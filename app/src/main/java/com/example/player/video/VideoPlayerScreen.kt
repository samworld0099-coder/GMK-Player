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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKSurfaceDark
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
            key(uiState.playerSessionId) {
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

        // Fullscreen Controls Overlay matching Phase 2 Reference Specifications
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
            ) {
                // Top Bar: Back | Title (Clean original filename) | Volume | Audio/Subtitles | 3-Dot More
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

                    // Original filename text
                    Text(
                        text = uiState.title.ifEmpty { "Video" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    if (!uiState.isLocked) {
                        // Volume Icon
                        IconButton(
                            onClick = {
                                viewModel.showGestureIndicator("Volume: ${(uiState.volume * 100).toInt()}%")
                            },
                            modifier = Modifier.testTag("volume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White
                            )
                        }

                        // Audio & Subtitles Combined Icon (Opens Section 4 Panel)
                        IconButton(
                            onClick = { viewModel.toggleAudioSubtitlePanel() },
                            modifier = Modifier.testTag("audio_subtitles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SpeakerNotes,
                                contentDescription = "Audio and Subtitles",
                                tint = if (uiState.showAudioSubtitlePanel) GMKElectricBlue else Color.White
                            )
                        }

                        // Three-Dot Menu (Opens Section 3 Menu)
                        IconButton(
                            onClick = { viewModel.toggleThreeDotMenu() },
                            modifier = Modifier.testTag("more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = if (uiState.showThreeDotMenu) GMKElectricBlue else Color.White
                            )
                        }
                    } else {
                        // When locked: prominent unlock button in top right
                        IconButton(
                            onClick = { viewModel.toggleLock() },
                            modifier = Modifier.testTag("lock_controls_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock screen",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Audio Only Fallback indicator banner
                if (uiState.isAudioOnlyFallback) {
                    Surface(
                        color = Color(0xD9101622),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, GMKElectricBlue),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = GMKElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audio-Only Mode (Decoder Fallback)", color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(onClick = { viewModel.resumeVideoRendering() }) {
                                Text("Resume Video", color = GMKElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section 3: Three Dot Menu Component (Floating top-right)
                if (uiState.showThreeDotMenu && !uiState.isLocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xF2101520),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 48.dp, end = 12.dp)
                            .width(220.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            // 1. Lock
                            ThreeDotMenuItem(
                                icon = Icons.Default.Lock,
                                label = "Lock",
                                onClick = {
                                    viewModel.closeDialogs()
                                    viewModel.toggleLock()
                                }
                            )
                            // 2. Sleep Timer
                            ThreeDotMenuItem(
                                icon = Icons.Default.Timer,
                                label = "Sleep timer",
                                onClick = { viewModel.openDialog("sleep") }
                            )
                            // 3. Playback Speed
                            ThreeDotMenuItem(
                                icon = Icons.Default.Speed,
                                label = "Playback speed",
                                onClick = { viewModel.openDialog("speed") }
                            )
                            // 4. Jump to Time
                            ThreeDotMenuItem(
                                icon = Icons.Default.Schedule,
                                label = "Jump to Time",
                                onClick = { viewModel.openDialog("jump") }
                            )
                            // 5. Equalizer
                            ThreeDotMenuItem(
                                icon = Icons.Default.Tune,
                                label = "Equalizer",
                                onClick = { viewModel.openDialog("equalizer") }
                            )
                            // 6. Play as audio
                            ThreeDotMenuItem(
                                icon = Icons.Default.Audiotrack,
                                label = "Play as audio",
                                onClick = {
                                    viewModel.closeDialogs()
                                    viewModel.playAudioOnly()
                                }
                            )
                            // 7. Pop-Up player
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ThreeDotMenuItem(
                                    icon = Icons.Default.PictureInPictureAlt,
                                    label = "Pop-Up player",
                                    onClick = {
                                        viewModel.closeDialogs()
                                        activity?.enterPictureInPictureMode()
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 4: Audio & Subtitles Selection Panel (Floating dual-column card)
                if (uiState.showAudioSubtitlePanel && !uiState.isLocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xF2101520),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 48.dp, end = 12.dp)
                            .fillMaxWidth(0.70f)
                            .height(280.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            // Column 1: Audio Tracks
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        val isNoneSelected = uiState.audioTracks.none { it.isSelected }
                                        TrackSelectionRow(
                                            label = "Disable track",
                                            isSelected = isNoneSelected,
                                            onClick = { viewModel.selectAudioTrack(null) }
                                        )
                                    }
                                    if (uiState.audioTracks.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Default audio track",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    } else {
                                        items(uiState.audioTracks) { track ->
                                            TrackSelectionRow(
                                                label = track.label,
                                                isSelected = track.isSelected,
                                                onClick = { viewModel.selectAudioTrack(track) }
                                            )
                                        }
                                    }
                                }
                            }

                            VerticalDivider(
                                color = Color(0x33FFFFFF),
                                modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight()
                            )

                            // Column 2: Subtitle Tracks
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Subtitles", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    item {
                                        TrackSelectionRow(
                                            label = "Disable track",
                                            isSelected = !uiState.isSubtitleEnabled,
                                            onClick = { viewModel.selectSubtitleTrack(null) }
                                        )
                                    }
                                    if (uiState.subtitleTracks.isEmpty()) {
                                        item {
                                            Text(
                                                text = "No embedded subtitles",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    } else {
                                        items(uiState.subtitleTracks) { track ->
                                            TrackSelectionRow(
                                                label = track.label,
                                                isSelected = track.isSelected && uiState.isSubtitleEnabled,
                                                onClick = { viewModel.selectSubtitleTrack(track) }
                                            )
                                        }
                                    }
                                }

                                // Load External Subtitle Button
                                TextButton(
                                    onClick = {
                                        subtitlePicker.launch(arrayOf("text/plain", "application/x-subrip", "*/*"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "+ Load external subtitle (.srt, .ass, .vtt)",
                                        fontSize = 11.sp,
                                        color = GMKElectricBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Timeline & Controls (Section 1 & 2 Matching Layout)
                if (!uiState.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        // Timeline & Seekbar Row (Current Time -- Orange Dot Seekbar -- Remaining Time)
                        val remainingMs = (uiState.durationMs - uiState.currentPositionMs).coerceAtLeast(0L)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(uiState.currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = uiState.currentPositionMs.toFloat(),
                                onValueChange = { pos -> viewModel.seekTo(pos.toLong()) },
                                valueRange = 0f..uiState.durationMs.coerceAtLeast(1L).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF5722), // Orange/coral thumb dot from reference image
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .testTag("video_seek_slider")
                            )

                            Text(
                                text = "- ${formatTime(remainingMs)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bottom Controls Row: [Subtitles] [Repeat] --- ( ▶/|| ) in Center --- [Fullscreen] [...]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lower Left Group: Subtitles and Aspect/Repeat
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleAudioSubtitlePanel() }) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Subtitles",
                                        tint = if (uiState.isSubtitleEnabled) Color.White else Color(0x88FFFFFF)
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleRepeat() }) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Loop playback",
                                        tint = if (uiState.isRepeatEnabled) GMKElectricBlue else Color.White
                                    )
                                }
                            }

                            // Center Play/Pause circular button (Matching Section 1 & 2 exact placement)
                            Surface(
                                shape = CircleShape,
                                color = Color(0x66000000),
                                border = BorderStroke(1.5.dp, Color.White),
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable { viewModel.togglePlayPause() }
                                    .testTag("play_pause_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Lower Right Group: Fullscreen and More (...)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    viewModel.setResizeMode(
                                        if (uiState.resizeMode == ResizeMode.FIT) ResizeMode.ZOOM else ResizeMode.FIT
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Toggle screen scale",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleThreeDotMenu() }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "More options",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
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

        // Priority 1: Playback Error Alert Dialog with Real Software Fallback and Non-Looping Logic
        uiState.playbackError?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.decodingFallbackAttempted) "Format Unsupported" else "Decoder Notice",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(errorMsg, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!uiState.decodingFallbackAttempted) {
                            Text(
                                text = "GMK Player attempted hardware decoding first. You can switch to the Android Software Decoder to play high-profile or 10-bit streams.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Neither hardware nor software decoders on this device support this video stream profile. You can play the audio track in background or choose another video file.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Row {
                        if (!uiState.decodingFallbackAttempted) {
                            TextButton(onClick = { viewModel.retryWithFallback() }) {
                                Text("Retry with Software Decoder", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { viewModel.playAudioOnly() }) {
                                Text("Play Audio Only", fontWeight = FontWeight.Bold)
                            }
                        }
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

        // Sleep Timer Dialog
        if (uiState.showSleepTimerDialog) {
            SleepTimerDialog(
                currentTimer = uiState.sleepTimerMinutesRemaining,
                onSelectMinutes = { viewModel.setSleepTimer(it) },
                onDismiss = { viewModel.closeDialogs() }
            )
        }

        // Jump to Time Dialog
        if (uiState.showJumpToTimeDialog) {
            JumpToTimeDialog(
                currentMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onJump = { m, s -> viewModel.jumpToTime(m, s) },
                onDismiss = { viewModel.closeDialogs() }
            )
        }

        // Equalizer Dialog
        if (uiState.showEqualizerDialog) {
            VideoEqualizerDialog(
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

@Composable
fun SleepTimerDialog(
    currentTimer: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        null to "Off (Cancel Timer)",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes",
        90 to "90 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                options.forEach { (mins, label) ->
                    val isSelected = currentTimer == mins
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMinutes(mins) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMinutes(mins) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
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
fun JumpToTimeDialog(
    currentMs: Long,
    durationMs: Long,
    onJump: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var targetSec by remember { mutableLongStateOf((currentMs / 1000L).coerceIn(0L, (durationMs / 1000L).coerceAtLeast(1L))) }
    val maxSec = (durationMs / 1000L).coerceAtLeast(1L)

    val currentMinutes = targetSec / 60
    val currentSeconds = targetSec % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jump to Time", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d:%02d / %s", currentMinutes, currentSeconds, formatTime(durationMs)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GMKElectricBlue
                )
                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = targetSec.toFloat(),
                    onValueChange = { targetSec = it.toLong().coerceIn(0L, maxSec) },
                    valueRange = 0f..maxSec.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = GMKElectricBlue,
                        activeTrackColor = GMKElectricBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { targetSec = (targetSec - 60).coerceAtLeast(0L) }) { Text("-1m") }
                    TextButton(onClick = { targetSec = (targetSec - 300).coerceAtLeast(0L) }) { Text("-5m") }
                    TextButton(onClick = { targetSec = (targetSec + 60).coerceAtMost(maxSec) }) { Text("+1m") }
                    TextButton(onClick = { targetSec = (targetSec + 300).coerceAtMost(maxSec) }) { Text("+5m") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onJump(currentMinutes, currentSeconds) }) {
                Text("Jump", fontWeight = FontWeight.Bold, color = GMKElectricBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VideoEqualizerDialog(
    onDismiss: () -> Unit
) {
    val presets = listOf("Flat", "Bass Boost", "Vocal", "Movie", "Rock", "Pop")
    var selectedPreset by remember { mutableStateOf("Flat") }
    var bassLevel by remember { mutableFloatStateOf(0.5f) }
    var trebleLevel by remember { mutableFloatStateOf(0.5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Equalizer & Sound FX", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Audio Presets", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GMKElectricBlue else Color(0xFF1E2638),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPreset = preset
                                    when (preset) {
                                        "Flat" -> { bassLevel = 0.5f; trebleLevel = 0.5f }
                                        "Bass Boost" -> { bassLevel = 0.85f; trebleLevel = 0.45f }
                                        "Vocal" -> { bassLevel = 0.35f; trebleLevel = 0.75f }
                                    }
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.drop(3).forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GMKElectricBlue else Color(0xFF1E2638),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPreset = preset
                                    when (preset) {
                                        "Movie" -> { bassLevel = 0.7f; trebleLevel = 0.65f }
                                        "Rock" -> { bassLevel = 0.75f; trebleLevel = 0.7f }
                                        "Pop" -> { bassLevel = 0.6f; trebleLevel = 0.6f }
                                    }
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Bass Boost: ${(bassLevel * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = bassLevel,
                    onValueChange = { bassLevel = it; selectedPreset = "Custom" },
                    colors = SliderDefaults.colors(activeTrackColor = GMKElectricBlue, thumbColor = GMKElectricBlue)
                )

                Text("Treble: ${(trebleLevel * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = trebleLevel,
                    onValueChange = { trebleLevel = it; selectedPreset = "Custom" },
                    colors = SliderDefaults.colors(activeTrackColor = GMKElectricBlue, thumbColor = GMKElectricBlue)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Apply", fontWeight = FontWeight.Bold, color = GMKElectricBlue) }
        }
    )
}

@Composable
private fun ThreeDotMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun TrackSelectionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
