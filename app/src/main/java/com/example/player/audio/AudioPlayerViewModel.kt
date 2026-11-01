package com.example.player.audio

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.db.AppDatabase
import com.example.data.model.MediaItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode { OFF, ONE, ALL }

data class AudioPlayerUiState(
    val currentTrack: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<MediaItem> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isFavorite: Boolean = false,
    val isExpandedPlayerVisible: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int? = null,
    val showLyrics: Boolean = false,
    val activeTab: String = "Songs"
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(AppDatabase.getInstance(application))
    private val _uiState = MutableStateFlow(AudioPlayerUiState())
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null

    fun getPlayer(): ExoPlayer? {
        return player ?: createPlayer()?.also { player = it }
    }

    private fun createPlayer(): ExoPlayer? {
        return try {
            val exo = ExoPlayer.Builder(getApplication()).build()
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startProgressTracker() else stopProgressTracker()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _uiState.update {
                            it.copy(
                                durationMs = exo.duration.coerceAtLeast(0L),
                                currentPositionMs = exo.currentPosition.coerceAtLeast(0L)
                            )
                        }
                    } else if (state == Player.STATE_ENDED) {
                        handleTrackEnded()
                    }
                }
            })
            exo
        } catch (_: Throwable) {
            null
        }
    }

    fun playTrack(track: MediaItem, newQueue: List<MediaItem>? = null) {
        val q = newQueue ?: if (_uiState.value.queue.isEmpty()) listOf(track) else _uiState.value.queue
        val idx = q.indexOfFirst { it.uri == track.uri }.let { if (it >= 0) it else 0 }

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = q,
                queueIndex = idx
            )
        }

        getPlayer()?.let { exo ->
            exo.setMediaItem(ExoMediaItem.fromUri(track.uri))
            exo.prepare()
            exo.play()
        }

        viewModelScope.launch {
            repository.recordPlayback(
                uri = track.uri.toString(),
                title = track.name,
                path = track.path,
                durationMs = track.durationMs,
                positionMs = 0L,
                mediaType = "AUDIO"
            )
        }
    }

    fun togglePlayPause() {
        player?.let { exo ->
            if (exo.isPlaying) exo.pause() else exo.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun playNext() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val nextIdx = if (state.isShuffle) {
            (0 until state.queue.size).random()
        } else {
            (state.queueIndex + 1) % state.queue.size
        }
        playTrack(state.queue[nextIdx], state.queue)
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val prevIdx = if (state.queueIndex > 0) state.queueIndex - 1 else state.queue.size - 1
        playTrack(state.queue[prevIdx], state.queue)
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _uiState.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun toggleFavorite() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            repository.toggleFavorite(track)
            _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    fun setExpandedPlayerVisible(visible: Boolean) {
        _uiState.update { it.copy(isExpandedPlayerVisible = visible) }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    private var sleepTimerJob: Job? = null
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMinutes = minutes) }
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                player?.pause()
                _uiState.update { it.copy(sleepTimerMinutes = null) }
            }
        }
    }

    fun toggleLyrics(show: Boolean) {
        _uiState.update { it.copy(showLyrics = show) }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun handleTrackEnded() {
        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                player?.seekTo(0)
                player?.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (_uiState.value.queueIndex < _uiState.value.queue.size - 1) {
                    playNext()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                player?.let { exo ->
                    _uiState.update { it.copy(currentPositionMs = exo.currentPosition.coerceAtLeast(0L)) }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player?.release()
        player = null
    }
}
