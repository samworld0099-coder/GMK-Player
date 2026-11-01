package com.example.player.video

enum class ResizeMode {
    FIT,
    FILL,
    ZOOM,
    FIXED_WIDTH,
    FIXED_HEIGHT,
    ORIGINAL
}

data class TrackInfo(
    val id: String,
    val index: Int,
    val groupIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

data class DoubleTapIndicatorState(
    val isForward: Boolean,
    val text: String,
    val id: Long = System.currentTimeMillis()
)

data class SwipeSeekState(
    val isDragging: Boolean = false,
    val targetPositionMs: Long = 0L,
    val deltaMs: Long = 0L,
    val formattedTarget: String = "",
    val formattedDelta: String = ""
)

data class VideoResolutionInfo(
    val width: Int = 0,
    val height: Int = 0,
    val label: String = ""
)

data class VideoPlayerUiState(
    val title: String = "",
    val uriString: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val rotationDegrees: Int = 0,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val gestureIndicatorText: String? = null,
    val doubleTapIndicator: DoubleTapIndicatorState? = null,
    val swipeSeekState: SwipeSeekState? = null,
    val resolutionInfo: VideoResolutionInfo? = null,
    val playbackError: String? = null,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val isSubtitleEnabled: Boolean = true,
    val subtitleTextSizeSp: Float = 18f,
    val subtitleBgOpacity: Float = 0.5f,
    val subtitleDelayMs: Long = 0L,
    val showSpeedDialog: Boolean = false,
    val showAudioDialog: Boolean = false,
    val showSubtitleDialog: Boolean = false,
    val showResizeDialog: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val showJumpToTimeDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,
    val isRepeatEnabled: Boolean = false,
    val showThreeDotMenu: Boolean = false,
    val showAudioSubtitlePanel: Boolean = false,
    val isHardwareDecoding: Boolean = true,
    val decodingFallbackAttempted: Boolean = false,
    val failedCodecName: String? = null,
    val isAudioOnlyFallback: Boolean = false,
    val mediaMimeType: String? = null,
    val showResumePrompt: Boolean = false,
    val savedResumePositionMs: Long = 0L,
    val playerSessionId: Long = 0L
)
