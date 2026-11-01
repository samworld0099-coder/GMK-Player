package com.example.player.video

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.data.db.AppDatabase
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(AppDatabase.getInstance(application))
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var hideControlsJob: Job? = null
    private var doubleTapIndicatorJob: Job? = null
    private var indicatorJob: Job? = null

    private val failedCodecs = mutableSetOf<String>()
    private var isSoftwareFallbackMode = false

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(forceSoftwareFallback: Boolean = false): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(getApplication()).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)

            // Dynamic MediaCodecSelector providing real hardware and software decoding paths
            val customSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val allDecoders = try {
                    MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                } catch (_: Exception) {
                    emptyList()
                }

                if (forceSoftwareFallback) {
                    // True software decoding fallback:
                    // 1. Exclude hardware decoders that previously failed
                    // 2. Strongly prioritize Google Codec2/OMX software decoders (c2.android.*, OMX.google.*, softwareOnly = true)
                    val available = allDecoders.filter { it.name !in failedCodecs }
                    available.sortedWith(
                        compareByDescending<MediaCodecInfo> { info ->
                            info.softwareOnly ||
                            !info.hardwareAccelerated ||
                            info.name.startsWith("c2.android.") ||
                            info.name.startsWith("OMX.google.") ||
                            info.name.contains(".sw.", ignoreCase = true) ||
                            info.name.contains("software", ignoreCase = true)
                        }
                    )
                } else {
                    // Standard hardware-accelerated playback:
                    // Filter out any decoders that have already failed in this session
                    val nonFailed = allDecoders.filter { it.name !in failedCodecs }
                    if (nonFailed.isNotEmpty()) nonFailed else allDecoders
                }
            }

            setMediaCodecSelector(customSelector)
        }

        val trackSelector = DefaultTrackSelector(getApplication()).apply {
            val params = buildUponParameters()
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)

            if (forceSoftwareFallback) {
                // In software fallback mode: disable hardware tunneling and display constraints
                params.setTunnelingEnabled(false)
            }
            setParameters(params)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 20_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10_000, true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(getApplication())
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    scheduleHideControls()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val w = videoSize.width
                    val h = videoSize.height
                    val label = when {
                        w >= 3840 || h >= 2160 -> "4K UHD (${w}×${h})"
                        w >= 2560 || h >= 1440 -> "2K QHD (${w}×${h})"
                        w >= 1920 || h >= 1080 -> "1080p FHD (${w}×${h})"
                        w >= 1280 || h >= 720 -> "720p HD (${w}×${h})"
                        else -> "${w}×${h}"
                    }
                    _uiState.update {
                        it.copy(
                            resolutionInfo = VideoResolutionInfo(
                                width = w,
                                height = h,
                                label = label
                            )
                        )
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update {
                        it.copy(
                            durationMs = player.duration.coerceAtLeast(0L),
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            playbackError = null
                        )
                    }
                    updateTracks(player)
                } else if (playbackState == Player.STATE_ENDED) {
                    saveHistory(completed = true)
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                updateTracks(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                var failedCodec: String? = null
                var cause: Throwable? = error.cause
                while (cause != null) {
                    if (cause is MediaCodecDecoderException) {
                        failedCodec = cause.codecInfo?.name
                        break
                    } else if (cause is MediaCodecRenderer.DecoderInitializationException) {
                        failedCodec = cause.codecInfo?.name
                        break
                    }
                    cause = cause.cause
                }
                if (failedCodec != null) {
                    failedCodecs.add(failedCodec)
                }

                val isDecoderFailure = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED

                val errorMessage: String
                if (isDecoderFailure) {
                    val codecTag = failedCodec?.let { " ($it)" } ?: ""
                    if (!_uiState.value.decodingFallbackAttempted) {
                        errorMessage = "Playback error: Stream decoding failed with hardware decoder$codecTag. Codec profile or bit depth may require software decoding."
                    } else {
                        errorMessage = "Playback error: Stream decoding failed$codecTag. Neither hardware nor software decoders on this device can decode this stream. You can play the audio track in background or choose another video."
                    }
                } else {
                    errorMessage = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                            "Media file was not found or is no longer accessible on storage."
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                            "Unsupported container format. GMK Player supports MP4, MKV, WebM, MOV, AVI, TS, and MPEG."
                        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                            "Playback buffer underrun. Re-buffering stream."
                        else ->
                            error.localizedMessage ?: "Playback encountered an unexpected error (${error.errorCodeName})"
                    }
                }

                _uiState.update {
                    it.copy(
                        playbackError = errorMessage,
                        isPlaying = false,
                        failedCodecName = failedCodec
                    )
                }
            }
        })
        return player
    }

    fun initialize(uri: Uri, title: String, startPositionMs: Long? = null) {
        if (!isSoftwareFallbackMode) {
            failedCodecs.clear()
            _uiState.update {
                it.copy(
                    isHardwareDecoding = true,
                    decodingFallbackAttempted = false,
                    failedCodecName = null,
                    isAudioOnlyFallback = false
                )
            }
        }

        val player = getPlayer()
        val uriStr = uri.toString()
        _uiState.update {
            it.copy(
                title = title,
                uriString = uriStr,
                playbackError = null,
                showResumePrompt = false,
                resolutionInfo = null,
                doubleTapIndicator = null,
                swipeSeekState = null
            )
        }

        viewModelScope.launch {
            val savedPos = startPositionMs ?: repository.getPlaybackPosition(uriStr)
            if (startPositionMs == null && savedPos > 3000L) {
                _uiState.update {
                    it.copy(
                        showResumePrompt = true,
                        savedResumePositionMs = savedPos
                    )
                }
            }

            val mediaItemBuilder = ExoMediaItem.Builder().setUri(uri)

            // Look for matching subtitle candidate in the same folder if file URI
            if (uri.scheme == "file") {
                uri.path?.let { p ->
                    val videoFile = File(p)
                    val parent = videoFile.parentFile
                    val baseName = videoFile.nameWithoutExtension
                    val srtFile = File(parent, "$baseName.srt")
                    if (srtFile.exists() && srtFile.canRead()) {
                        val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(Uri.fromFile(srtFile))
                            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                            .setLanguage("en")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                        mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
                    }
                }
            }

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            if (startPositionMs != null && startPositionMs > 0L) {
                player.seekTo(startPositionMs)
            }
            player.play()
        }
    }

    fun retryWithFallback() {
        val currentUriStr = _uiState.value.uriString
        val currentTitle = _uiState.value.title
        val savedPos = _uiState.value.currentPositionMs.coerceAtLeast(exoPlayer?.currentPosition ?: 0L)

        if (_uiState.value.decodingFallbackAttempted) {
            // Software fallback already attempted and failed. Do not loop.
            return
        }

        if (currentUriStr.isNotEmpty()) {
            isSoftwareFallbackMode = true
            val newSessionId = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isHardwareDecoding = false,
                    decodingFallbackAttempted = true,
                    playbackError = null,
                    playerSessionId = newSessionId
                )
            }
            try {
                exoPlayer?.stop()
                exoPlayer?.release()
            } catch (_: Exception) {}
            exoPlayer = createPlayer(forceSoftwareFallback = true)
            val uri = Uri.parse(currentUriStr)
            initialize(uri, currentTitle, startPositionMs = savedPos)
        }
    }

    fun playAudioOnly() {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        _uiState.update {
            it.copy(
                isAudioOnlyFallback = true,
                playbackError = null
            )
        }
        player.play()
    }

    fun resumeVideoRendering() {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
        _uiState.update { it.copy(isAudioOnlyFallback = false) }
        player.play()
    }

    fun dismissError() {
        _uiState.update { it.copy(playbackError = null) }
    }

    fun resumeFromSavedPosition() {
        val pos = _uiState.value.savedResumePositionMs
        exoPlayer?.seekTo(pos)
        _uiState.update { it.copy(showResumePrompt = false) }
    }

    fun dismissResumePrompt() {
        _uiState.update { it.copy(showResumePrompt = false) }
    }

    private fun updateTracks(player: ExoPlayer) {
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        val rawTitle = _uiState.value.title
        val cleanName = if (rawTitle.contains('.')) rawTitle.substringBeforeLast('.') else rawTitle

        val tracks = player.currentTracks
        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val lang = format.language?.let {
                    try { Locale(it).displayLanguage } catch (_: Exception) { it }
                } ?: format.language

                if (group.type == C.TRACK_TYPE_AUDIO) {
                    val langSuffix = if (!lang.isNullOrBlank()) " - [$lang]" else " - [Audio ${audioList.size + 1}]"
                    val role = when {
                        (format.roleFlags and C.ROLE_FLAG_DUB) != 0 -> "[Dubbed] "
                        (format.roleFlags and C.ROLE_FLAG_COMMENTARY) != 0 -> "[Commentary] "
                        else -> ""
                    }
                    val label = format.label ?: "$role$cleanName$langSuffix"
                    audioList.add(
                        TrackInfo(
                            id = "audio_${groupIndex}_$trackIndex",
                            index = trackIndex,
                            groupIndex = groupIndex,
                            label = label,
                            language = lang,
                            isSelected = isSelected
                        )
                    )
                } else if (group.type == C.TRACK_TYPE_TEXT) {
                    val tag = when {
                        (format.roleFlags and C.ROLE_FLAG_CAPTION) != 0 -> "[SDH] "
                        (format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0 -> "[Forced] "
                        else -> ""
                    }
                    val langSuffix = if (!lang.isNullOrBlank()) " - [$lang]" else " - [Subtitle ${subList.size + 1}]"
                    val label = format.label ?: "$tag$cleanName$langSuffix"
                    subList.add(
                        TrackInfo(
                            id = "sub_${groupIndex}_$trackIndex",
                            index = trackIndex,
                            groupIndex = groupIndex,
                            label = label,
                            language = lang,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }
        _uiState.update { it.copy(audioTracks = audioList, subtitleTracks = subList) }
    }

    fun selectAudioTrack(track: TrackInfo?) {
        val player = exoPlayer ?: return
        if (track == null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        } else {
            val tracks = player.currentTracks
            if (track.groupIndex < tracks.groups.size) {
                val group = tracks.groups[track.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, track.index)
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .addOverride(override)
                    .build()
            }
        }
    }

    fun selectSubtitleTrack(track: TrackInfo?) {
        val player = exoPlayer ?: return
        if (track == null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _uiState.update { it.copy(isSubtitleEnabled = false) }
        } else {
            val tracks = player.currentTracks
            if (track.groupIndex < tracks.groups.size) {
                val group = tracks.groups[track.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, track.index)
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .addOverride(override)
                    .build()
            }
            _uiState.update { it.copy(isSubtitleEnabled = true) }
        }
    }

    fun addExternalSubtitle(subUri: Uri) {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem ?: return
        val ext = subUri.path?.substringAfterLast('.', "") ?: "srt"
        val mime = if (ext.equals("vtt", ignoreCase = true)) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP

        val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(subUri)
            .setMimeType(mime)
            .setLanguage("External")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val pos = player.currentPosition
        val newMediaItem = currentMediaItem.buildUpon()
            .setSubtitleConfigurations(listOf(subConfig))
            .build()
        player.setMediaItem(newMediaItem)
        player.prepare()
        player.seekTo(pos)
        player.play()
        _uiState.update { it.copy(showSubtitleDialog = false, isSubtitleEnabled = true) }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            saveHistory(completed = false)
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val p = positionMs.coerceIn(0L, _uiState.value.durationMs)
        exoPlayer?.seekTo(p)
        _uiState.update { it.copy(currentPositionMs = p) }
    }

    fun seekBy(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + offsetMs)
        val text = if (offsetMs >= 0) "+${offsetMs / 1000}s" else "${offsetMs / 1000}s"
        showGestureIndicator(text)
    }

    fun doubleTapSeekForward() {
        val duration = _uiState.value.durationMs
        val current = exoPlayer?.currentPosition ?: _uiState.value.currentPositionMs
        val target = (current + 10_000L).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        exoPlayer?.seekTo(target)
        _uiState.update {
            it.copy(
                currentPositionMs = target,
                doubleTapIndicator = DoubleTapIndicatorState(isForward = true, text = "+10 seconds")
            )
        }
        scheduleClearDoubleTapIndicator()
    }

    fun doubleTapSeekBackward() {
        val current = exoPlayer?.currentPosition ?: _uiState.value.currentPositionMs
        val target = (current - 10_000L).coerceAtLeast(0L)
        exoPlayer?.seekTo(target)
        _uiState.update {
            it.copy(
                currentPositionMs = target,
                doubleTapIndicator = DoubleTapIndicatorState(isForward = false, text = "−10 seconds")
            )
        }
        scheduleClearDoubleTapIndicator()
    }

    private fun scheduleClearDoubleTapIndicator() {
        doubleTapIndicatorJob?.cancel()
        doubleTapIndicatorJob = viewModelScope.launch {
            delay(900)
            _uiState.update { it.copy(doubleTapIndicator = null) }
        }
    }

    fun onSwipeSeekUpdate(startPositionMs: Long, deltaX: Float, screenWidthPx: Float) {
        val duration = _uiState.value.durationMs
        if (duration <= 0L || screenWidthPx <= 0f) return

        val maxSwipeWindowMs = minOf(duration, maxOf(60_000L, duration / 4))
        val fraction = (deltaX / screenWidthPx).coerceIn(-1f, 1f)
        val deltaMs = (fraction * maxSwipeWindowMs).toLong()
        val targetMs = (startPositionMs + deltaMs).coerceIn(0L, duration)
        val deltaSec = (targetMs - startPositionMs) / 1000

        val deltaStr = if (deltaSec >= 0) "+${formatTimeHelper(deltaSec * 1000)}" else "-${formatTimeHelper(-deltaSec * 1000)}"
        val targetStr = formatTimeHelper(targetMs)

        _uiState.update {
            it.copy(
                swipeSeekState = SwipeSeekState(
                    isDragging = true,
                    targetPositionMs = targetMs,
                    deltaMs = deltaMs,
                    formattedTarget = targetStr,
                    formattedDelta = deltaStr
                )
            )
        }
    }

    fun onSwipeSeekCommit() {
        val target = _uiState.value.swipeSeekState?.targetPositionMs
        if (target != null) {
            seekTo(target)
        }
        _uiState.update { it.copy(swipeSeekState = null) }
    }

    fun onSwipeSeekCancel() {
        _uiState.update { it.copy(swipeSeekState = null) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
    }

    fun setResizeMode(mode: ResizeMode) {
        _uiState.update { it.copy(resizeMode = mode, showResizeDialog = false) }
    }

    fun rotate90(clockwise: Boolean) {
        _uiState.update {
            val delta = if (clockwise) 90 else -90
            val newDeg = (it.rotationDegrees + delta + 360) % 360
            it.copy(rotationDegrees = newDeg)
        }
    }

    fun resetRotation() {
        _uiState.update { it.copy(rotationDegrees = 0) }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleControls() {
        if (_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
            return
        }
        val newVisibility = !_uiState.value.isControlsVisible
        _uiState.update { it.copy(isControlsVisible = newVisibility) }
        if (newVisibility) {
            scheduleHideControls()
        }
    }

    fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(4000)
            if (_uiState.value.isPlaying) {
                _uiState.update { it.copy(isControlsVisible = false) }
            }
        }
    }

    fun setVolumeDelta(delta: Float) {
        val am = audioManager ?: return
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val step = (delta * maxVol).toInt()
        val newVol = (currentVol + step).coerceIn(0, maxVol)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)

        val fraction = newVol.toFloat() / maxVol
        _uiState.update { it.copy(volume = fraction) }
        showGestureIndicator("Volume: ${(fraction * 100).toInt()}%")
    }

    fun setBrightnessDelta(delta: Float) {
        val current = _uiState.value.brightness
        val newBright = (current + delta).coerceIn(0.01f, 1.0f)
        _uiState.update { it.copy(brightness = newBright) }
        showGestureIndicator("Brightness: ${(newBright * 100).toInt()}%")
    }

    fun showGestureIndicator(text: String) {
        _uiState.update { it.copy(gestureIndicatorText = text) }
        indicatorJob?.cancel()
        indicatorJob = viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(gestureIndicatorText = null) }
        }
    }

    fun setSubtitleSize(sp: Float) = _uiState.update { it.copy(subtitleTextSizeSp = sp) }
    fun setSubtitleBgOpacity(opacity: Float) = _uiState.update { it.copy(subtitleBgOpacity = opacity) }
    fun setSubtitleDelay(delayMs: Long) {
        _uiState.update { it.copy(subtitleDelayMs = delayMs) }
        val text = if (delayMs >= 0) "Subtitle delay: +${delayMs}ms" else "Subtitle delay: ${delayMs}ms"
        showGestureIndicator(text)
    }

    private var sleepTimerJob: Job? = null

    fun toggleRepeat() {
        val newRepeat = !_uiState.value.isRepeatEnabled
        exoPlayer?.repeatMode = if (newRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.update { it.copy(isRepeatEnabled = newRepeat) }
        showGestureIndicator(if (newRepeat) "Loop: ON" else "Loop: OFF")
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null, showSleepTimerDialog = false) }
            showGestureIndicator("Sleep timer off")
            return
        }
        _uiState.update { it.copy(sleepTimerMinutesRemaining = minutes, showSleepTimerDialog = false) }
        showGestureIndicator("Sleep timer: $minutes min")
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            pausePlayback()
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null) }
        }
    }

    fun jumpToTime(minutes: Long, seconds: Long) {
        val targetMs = (minutes * 60 + seconds) * 1000L
        seekTo(targetMs)
        _uiState.update { it.copy(showJumpToTimeDialog = false) }
    }

    fun toggleThreeDotMenu() {
        val current = _uiState.value.showThreeDotMenu
        _uiState.update { it.copy(showThreeDotMenu = !current, showAudioSubtitlePanel = false) }
    }

    fun toggleAudioSubtitlePanel() {
        val current = _uiState.value.showAudioSubtitlePanel
        _uiState.update { it.copy(showAudioSubtitlePanel = !current, showThreeDotMenu = false) }
    }

    fun openDialog(dialog: String) {
        _uiState.update {
            when (dialog) {
                "speed" -> it.copy(showSpeedDialog = true, showThreeDotMenu = false)
                "audio" -> it.copy(showAudioSubtitlePanel = true, showThreeDotMenu = false)
                "subtitle" -> it.copy(showAudioSubtitlePanel = true, showThreeDotMenu = false)
                "resize" -> it.copy(showResizeDialog = true, showThreeDotMenu = false)
                "sleep" -> it.copy(showSleepTimerDialog = true, showThreeDotMenu = false)
                "jump" -> it.copy(showJumpToTimeDialog = true, showThreeDotMenu = false)
                "equalizer" -> it.copy(showEqualizerDialog = true, showThreeDotMenu = false)
                else -> it
            }
        }
    }

    fun closeDialogs() {
        _uiState.update {
            it.copy(
                showSpeedDialog = false,
                showAudioDialog = false,
                showSubtitleDialog = false,
                showResizeDialog = false,
                showSleepTimerDialog = false,
                showJumpToTimeDialog = false,
                showEqualizerDialog = false,
                showThreeDotMenu = false,
                showAudioSubtitlePanel = false
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _uiState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun pausePlayback() {
        exoPlayer?.pause()
        _uiState.update { it.copy(isPlaying = false) }
        stopProgressTracker()
    }

    fun saveHistory(completed: Boolean = false) {
        val player = exoPlayer ?: return
        val pos = player.currentPosition
        val dur = player.duration
        val state = _uiState.value
        if (state.uriString.isNotEmpty() && dur > 0) {
            viewModelScope.launch {
                repository.recordPlayback(
                    uri = state.uriString,
                    title = state.title,
                    path = state.uriString,
                    durationMs = dur,
                    positionMs = pos,
                    mediaType = "VIDEO",
                    completed = completed
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveHistory()
        progressJob?.cancel()
        hideControlsJob?.cancel()
        doubleTapIndicatorJob?.cancel()
        indicatorJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        fun formatTimeHelper(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSec = ms / 1000
            val s = totalSec % 60
            val m = (totalSec / 60) % 60
            val h = totalSec / 3600
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }
    }
}
