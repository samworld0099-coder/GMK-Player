package com.example.player.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.abs

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

enum class DragGestureMode {
    UNDECIDED,
    HORIZONTAL_SEEK,
    BRIGHTNESS,
    VOLUME
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUri: Uri,
    title: String,
    viewModel: VideoPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.uiState.collectAsState()

    // Subtitle file picker launcher
    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.addExternalSubtitle(uri)
        }
    }

    // Keep screen on during playback
    DisposableEffect(videoUri) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.initialize(videoUri, title)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.pausePlayback()
            viewModel.saveHistory()
        }
    }

    // Window brightness sync
    LaunchedEffect(uiState.brightness) {
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = uiState.brightness
            win.attributes = lp
        }
    }

    BackHandler {
        viewModel.pausePlayback()
        viewModel.saveHistory()
        onBack()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        val density = LocalDensity.current
        val touchSlopPx = remember { with(density) { 14.dp.toPx() } }

        var dragMode by remember { mutableStateOf(DragGestureMode.UNDECIDED) }
        var dragStartMs by remember { mutableLongStateOf(0L) }
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var totalDragY by remember { mutableFloatStateOf(0f) }

        // Video Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(uiState.rotationDegrees.toFloat()),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.getPlayer()
                        useController = false // Custom VLC-style Compose HUD
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { playerView ->
                    val currentPlayer = viewModel.getPlayer()
                    if (playerView.player != currentPlayer) {
                        playerView.player = currentPlayer
                    }
                    val targetMode = when (uiState.resizeMode) {
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        ResizeMode.FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        ResizeMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    if (playerView.resizeMode != targetMode) {
                        playerView.resizeMode = targetMode
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // VLC Gesture Detector Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    if (uiState.isLocked) {
                        detectTapGestures(onTap = { viewModel.toggleControls() })
                    } else {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    viewModel.doubleTapSeekBackward()
                                } else {
                                    viewModel.doubleTapSeekForward()
                                }
                            }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragMode = DragGestureMode.UNDECIDED
                                dragStartMs = uiState.currentPositionMs
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragEnd = {
                                if (dragMode == DragGestureMode.HORIZONTAL_SEEK) {
                                    viewModel.onSwipeSeekCommit()
                                }
                                dragMode = DragGestureMode.UNDECIDED
                            },
                            onDragCancel = {
                                viewModel.onSwipeSeekCancel()
                                dragMode = DragGestureMode.UNDECIDED
                            },
                            onDrag = { change, dragAmount ->
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y

                                if (dragMode == DragGestureMode.UNDECIDED) {
                                    val absX = abs(totalDragX)
                                    val absY = abs(totalDragY)
                                    if (absX > touchSlopPx && absX > absY * 1.2f) {
                                        dragMode = DragGestureMode.HORIZONTAL_SEEK
                                    } else if (absY > touchSlopPx && absY > absX * 1.2f) {
                                        dragMode = if (change.position.x < size.width / 2) {
                                            DragGestureMode.BRIGHTNESS
                                        } else {
                                            DragGestureMode.VOLUME
                                        }
                                    }
                                }

                                when (dragMode) {
                                    DragGestureMode.HORIZONTAL_SEEK -> {
                                        change.consume()
                                        viewModel.onSwipeSeekUpdate(dragStartMs, totalDragX, size.width.toFloat())
                                    }
                                    DragGestureMode.BRIGHTNESS -> {
                                        change.consume()
                                        val delta = -dragAmount.y / size.height
                                        viewModel.setBrightnessDelta(delta * 0.7f)
                                    }
                                    DragGestureMode.VOLUME -> {
                                        change.consume()
                                        val delta = -dragAmount.y / size.height
                                        viewModel.setVolumeDelta(delta * 0.7f)
                                    }
                                    DragGestureMode.UNDECIDED -> {}
                                }
                            }
                        )
                    }
                }
        )

        // Gesture HUD Overlay (Brightness/Volume Indicator)
        uiState.gestureIndicatorText?.let { text ->
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    val isBrightness = text.startsWith("Brightness")
                    val isVolume = text.startsWith("Volume")
                    if (isBrightness) {
                        Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (isVolume) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Double-Tap Animated Indicator (+10 seconds / −10 seconds)
        uiState.doubleTapIndicator?.let { indicator ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = if (indicator.isForward) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    shape = CircleShape,
                    modifier = Modifier.size(92.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (indicator.isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                            contentDescription = indicator.text,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = indicator.text,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Horizontal Swipe Seek HUD Overlay
        uiState.swipeSeekState?.let { seek ->
            if (seek.isDragging) {
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = seek.formattedTarget,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${formatTime(uiState.durationMs)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (seek.deltaMs >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = seek.formattedDelta,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // VLC Fullscreen Controls Overlay
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.pausePlayback()
                            viewModel.saveHistory()
                            onBack()
                        },
                        modifier = Modifier.testTag("video_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.resolutionInfo?.let { res ->
                            Text(
                                text = "${res.label} • Hardware Accelerated",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    if (!uiState.isLocked) {
                        // Aspect Ratio / Screen adjustment
                        IconButton(onClick = { viewModel.openDialog("resize") }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Screen Fit",
                                tint = Color.White
                            )
                        }

                        // Subtitles
                        IconButton(onClick = { viewModel.openDialog("subtitle") }) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (uiState.isSubtitleEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Audio Track
                        IconButton(onClick = { viewModel.openDialog("audio") }) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Track",
                                tint = Color.White
                            )
                        }

                        // Playback Speed
                        IconButton(onClick = { viewModel.openDialog("speed") }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = Color.White
                            )
                        }

                        // Rotate 90
                        IconButton(onClick = { viewModel.rotate90(clockwise = true) }) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate",
                                tint = Color.White
                            )
                        }

                        // Picture in Picture
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                activity?.enterPictureInPictureMode()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "PiP",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Lock button (always visible)
                    IconButton(
                        onClick = { viewModel.toggleLock() },
                        modifier = Modifier.testTag("lock_controls_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (uiState.isLocked) "Unlock" else "Lock",
                            tint = if (uiState.isLocked) MaterialTheme.colorScheme.error else Color.White
                        )
                    }
                }

                // Center Controls (when unlocked)
                if (!uiState.isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.doubleTapSeekBackward() },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { viewModel.togglePlayPause() }
                                .testTag("play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.doubleTapSeekForward() },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Controls (when unlocked)
                if (!uiState.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(uiState.currentPositionMs),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatTime(uiState.durationMs),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }

                        Slider(
                            value = uiState.currentPositionMs.toFloat(),
                            onValueChange = { pos -> viewModel.seekTo(pos.toLong()) },
                            valueRange = 0f..uiState.durationMs.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_seek_slider")
                        )
                    }
                }
            }
        }

        // Resume Playback Prompt
        if (uiState.showResumePrompt) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Resume playback?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Continue at ${formatTime(uiState.savedResumePositionMs)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { viewModel.dismissResumePrompt() }) {
                        Text("Start Over")
                    }
                    TextButton(onClick = { viewModel.resumeFromSavedPosition() }) {
                        Text("Resume", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Playback Error Alert Dialog
        uiState.playbackError?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playback Notice", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(errorMsg, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GMK Player supports MP4, MKV, WebM, MOV, AVI, TS, and MPEG with hardware acceleration. If hardware decoding fails, software fallback can be tried.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.retryWithFallback() }) {
                        Text("Retry with Fallback")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.dismissError()
                        onBack()
                    }) {
                        Text("Back")
                    }
                }
            )
        }

        // Speed Dialog
        if (uiState.showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = uiState.playbackSpeed,
                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { viewModel.closeDialogs() }
            )
        }

        // Aspect Ratio Dialog
        if (uiState.showResizeDialog) {
            ResizeModeDialog(
                currentMode = uiState.resizeMode,
                onSelectMode = { viewModel.setResizeMode(it) },
                onResetRotation = { viewModel.resetRotation() },
                onDismiss = { viewModel.closeDialogs() }
            )
        }

        // Subtitle Selection & Customization Dialog
        if (uiState.showSubtitleDialog) {
            SubtitleSelectionDialog(
                tracks = uiState.subtitleTracks,
                isSubtitleEnabled = uiState.isSubtitleEnabled,
                textSizeSp = uiState.subtitleTextSizeSp,
                subtitleDelayMs = uiState.subtitleDelayMs,
                onSelectTrack = { viewModel.selectSubtitleTrack(it) },
                onTextSizeChange = { viewModel.setSubtitleSize(it) },
                onSubtitleDelayChange = { viewModel.setSubtitleDelay(it) },
                onPickExternal = {
                    subtitlePicker.launch(arrayOf("text/plain", "application/x-subrip", "*/*"))
                },
                onDismiss = { viewModel.closeDialogs() }
            )
        }

        // Audio Track Selection Dialog
        if (uiState.showAudioDialog) {
            AudioTrackDialog(
                tracks = uiState.audioTracks,
                onSelectTrack = { viewModel.selectAudioTrack(it) },
                onDismiss = { viewModel.closeDialogs() }
            )
        }
    }
}

@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSelectSpeed(speed) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${speed}x", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ResizeModeDialog(
    currentMode: ResizeMode,
    onSelectMode: (ResizeMode) -> Unit,
    onResetRotation: () -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        ResizeMode.FIT to "Fit Screen (Recommended)",
        ResizeMode.FILL to "Stretch to Fill",
        ResizeMode.ZOOM to "Zoom / Crop",
        ResizeMode.FIXED_WIDTH to "Fit Width",
        ResizeMode.FIXED_HEIGHT to "Fit Height",
        ResizeMode.ORIGINAL to "100% Original Size"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen Adjustment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                modes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onSelectMode(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onResetRotation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Rotation to 0°")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun SubtitleSelectionDialog(
    tracks: List<TrackInfo>,
    isSubtitleEnabled: Boolean,
    textSizeSp: Float,
    subtitleDelayMs: Long,
    onSelectTrack: (TrackInfo?) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onPickExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitles", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTrack(null) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isSubtitleEnabled,
                        onClick = { onSelectTrack(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Subtitles")
                }

                tracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSubtitleEnabled && track.isSelected,
                            onClick = { onSelectTrack(track) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(track.label, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onPickExternal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Select External Subtitle File (.srt, .vtt)")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Font Size: ${textSizeSp.toInt()}sp", fontSize = 13.sp)
                Slider(
                    value = textSizeSp,
                    onValueChange = onTextSizeChange,
                    valueRange = 12f..32f,
                    steps = 10
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Subtitle Sync Delay: ${subtitleDelayMs}ms", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-500L, -250L, 0L, 250L, 500L).forEach { delta ->
                        val label = if (delta == 0L) "0ms" else if (delta > 0) "+${delta}ms" else "${delta}ms"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (subtitleDelayMs == delta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (delta == 0L) onSubtitleDelayChange(0L)
                                    else onSubtitleDelayChange(subtitleDelayMs + delta)
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = if (subtitleDelayMs == delta) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AudioTrackDialog(
    tracks: List<TrackInfo>,
    onSelectTrack: (TrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Track Selection", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (tracks.isEmpty()) {
                    Text("No alternate audio tracks available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    tracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTrack(track) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = track.isSelected,
                                onClick = { onSelectTrack(track) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(track.label, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
