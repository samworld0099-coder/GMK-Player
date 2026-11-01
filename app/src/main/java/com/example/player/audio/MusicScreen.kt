package com.example.player.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.player.video.formatTime
import com.example.ui.theme.GMKBackgroundDark
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKSurfaceDark
import com.example.ui.theme.GMKSurfaceVariantDark
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import com.example.ui.theme.GMKVerifiedGreen
import kotlin.random.Random

data class MusicPlaylistCard(
    val id: String,
    val title: String,
    val countLabel: String,
    val gradientColors: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    songs: List<MediaItem>,
    viewModel: AudioPlayerViewModel,
    onOpenSearch: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQueueModal by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showPlaylistAddDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Songs", "Playlists", "Albums", "Artists", "Folders")

    val playlists = remember {
        listOf(
            MusicPlaylistCard("liked", "Liked Songs", "246 songs", listOf(Color(0xFFE91E63), Color(0xFF880E4F)), Icons.Default.Favorite),
            MusicPlaylistCard("my", "My Playlist", "128 songs", listOf(Color(0xFF3949AB), Color(0xFF1A237E)), Icons.Default.Album),
            MusicPlaylistCard("chill", "Chill", "86 songs", listOf(Color(0xFF7C4DFF), Color(0xFF311B92)), Icons.Default.MusicNote),
            MusicPlaylistCard("workout", "Workout", "52 songs", listOf(Color(0xFFFF6D00), Color(0xFFBF360C)), Icons.Default.Speed)
        )
    }

    val sampleRecentSongs = remember(songs) {
        if (songs.isNotEmpty()) {
            songs.take(4)
        } else {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GMKBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen 5 Header: ☰ | GMK Player [Green Shield] "Powered by GMK Studios" | 🔍
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GMK ",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Player",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = GMKElectricBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Verified GMK",
                            tint = GMKVerifiedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Powered by GMK Studios",
                        fontSize = 10.sp,
                        color = GMKTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }

            // Screen 5 Pill Filter Tabs: Songs, Playlists, Albums, Artists, Folders
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs) { tab ->
                    val isSelected = uiState.activeTab.equals(tab, ignoreCase = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) GMKElectricBlue else GMKSurfaceVariantDark,
                        modifier = Modifier.clickable { viewModel.setActiveTab(tab) }
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else GMKTextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Main Content Scrollable
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("songs_list")
            ) {
                // Section 1: Playlists
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playlists",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GMKTextWhite
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { /* see all */ }
                            ) {
                                Text(
                                    text = "See all",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = GMKTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlists) { card ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = GMKSurfaceDark,
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 124.dp)
                                        .clickable {
                                            if (songs.isNotEmpty()) viewModel.playTrack(songs.first(), songs)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(78.dp)
                                                .background(Brush.verticalGradient(card.gradientColors)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (card.icon != null) {
                                                Icon(
                                                    imageVector = card.icon,
                                                    contentDescription = card.title,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = card.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GMKTextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = card.countLabel,
                                                fontSize = 10.sp,
                                                color = GMKTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Recently Played
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Played",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GMKTextWhite
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { /* see all */ }
                            ) {
                                Text(
                                    text = "See all",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = GMKTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (sampleRecentSongs.isNotEmpty()) {
                    itemsIndexed(sampleRecentSongs) { _, song ->
                        val isCurrent = uiState.currentTrack?.uri == song.uri
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playTrack(song, songs) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Artwork
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E2638),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF3949AB), Color(0xFF7C4DFF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrent) GMKElectricBlue else GMKTextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "GMK Music",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                            }

                            if (isCurrent && uiState.isPlaying) {
                                LiveEqualizerIcon()
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            IconButton(onClick = { /* more */ }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = GMKTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Section 3: All Songs (632)
                item {
                    val count = if (songs.isNotEmpty()) songs.size else 632
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Songs ($count)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GMKTextWhite
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* sort */ }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = GMKTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Circular blue Play All button
                            Surface(
                                shape = CircleShape,
                                color = GMKElectricBlue,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        if (songs.isNotEmpty()) viewModel.playTrack(songs.first(), songs)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play all",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Songs list
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = GMKElectricBlue.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No songs detected yet",
                                    fontWeight = FontWeight.Bold,
                                    color = GMKTextWhite
                                )
                                Text(
                                    text = "Add MP3, M4A, FLAC audio files to device storage",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(songs) { _, song ->
                        val isCurrent = uiState.currentTrack?.uri == song.uri
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playTrack(song, songs) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) GMKElectricBlue.copy(alpha = 0.2f) else GMKSurfaceVariantDark,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCurrent && uiState.isPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isCurrent) GMKElectricBlue else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrent) GMKElectricBlue else GMKTextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.formattedDuration.ifEmpty { "Audio" }} • GMK Music",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                            }

                            if (isCurrent && uiState.isPlaying) {
                                LiveEqualizerIcon()
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(onClick = { /* options */ }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = GMKTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Mini Player Bar (Screen 5)
            uiState.currentTrack?.let { track ->
                MiniPlayerBar(
                    track = track,
                    isPlaying = uiState.isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onClick = { viewModel.setExpandedPlayerVisible(true) }
                )
            }
        }

        // Fullscreen Expanded Player (Screen 6)
        if (uiState.isExpandedPlayerVisible && uiState.currentTrack != null) {
            ExpandedAudioPlayerModal(
                track = uiState.currentTrack!!,
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.setExpandedPlayerVisible(false) },
                onOpenQueue = { showQueueModal = true },
                onOpenEqualizer = { showEqualizerDialog = true },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onOpenSpeed = { showSpeedDialog = true },
                onOpenPlaylistAdd = { showPlaylistAddDialog = true }
            )
        }

        // Queue Bottom Sheet
        if (showQueueModal) {
            ModalBottomSheet(
                onDismissRequest = { showQueueModal = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = GMKSurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Playing Queue (${uiState.queue.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GMKTextWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        itemsIndexed(uiState.queue) { index, item ->
                            val isPlayingItem = index == uiState.queueIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playTrack(item, uiState.queue)
                                        showQueueModal = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.width(28.dp),
                                    color = if (isPlayingItem) GMKElectricBlue else GMKTextMuted,
                                    fontWeight = if (isPlayingItem) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = item.name,
                                    fontWeight = if (isPlayingItem) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPlayingItem) GMKElectricBlue else GMKTextWhite,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Equalizer Dialog
        if (showEqualizerDialog) {
            AudioEqualizerDialog(onDismiss = { showEqualizerDialog = false })
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            AudioSleepTimerDialog(
                currentTimer = uiState.sleepTimerMinutes,
                onSelectMinutes = { viewModel.setSleepTimer(it) },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        // Speed Dialog
        if (showSpeedDialog) {
            AudioSpeedDialog(
                currentSpeed = uiState.playbackSpeed,
                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        // Add to Playlist Dialog
        if (showPlaylistAddDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistAddDialog = false },
                title = { Text("Add to Playlist", fontWeight = FontWeight.Bold, color = GMKTextWhite) },
                text = {
                    Column {
                        playlists.forEach { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPlaylistAddDialog = false }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = GMKElectricBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pl.title, fontWeight = FontWeight.Bold, color = GMKTextWhite)
                                    Text(pl.countLabel, fontSize = 11.sp, color = GMKTextMuted)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistAddDialog = false }) { Text("Done", color = GMKElectricBlue) }
                },
                containerColor = GMKSurfaceDark
            )
        }
    }
}

@Composable
fun LiveEqualizerIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = Modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(h1).background(GMKElectricBlue, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(h2).background(GMKElectricBlue, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(h3).background(GMKElectricBlue, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun MiniPlayerBar(
    track: MediaItem,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = GMKSurfaceDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E2638),
                modifier = Modifier.size(42.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF3949AB), Color(0xFF7C4DFF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GMKTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "GMK Music",
                    fontSize = 11.sp,
                    color = GMKTextMuted
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandedAudioPlayerModal(
    track: MediaItem,
    uiState: AudioPlayerUiState,
    viewModel: AudioPlayerViewModel,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenPlaylistAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GMKBackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen 6 Top Bar: Down Arrow | Song / Lyrics Pill Switcher | 3-Dot Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Song / Lyrics Pill Toggle
                Surface(
                    shape = CircleShape,
                    color = GMKSurfaceVariantDark,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (!uiState.showLyrics) GMKElectricBlue else Color.Transparent,
                            modifier = Modifier
                                .clickable { viewModel.toggleLyrics(false) }
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Song",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!uiState.showLyrics) Color.White else GMKTextMuted
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (uiState.showLyrics) GMKElectricBlue else Color.Transparent,
                            modifier = Modifier
                                .clickable { viewModel.toggleLyrics(true) }
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Lyrics",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.showLyrics) Color.White else GMKTextMuted
                            )
                        }
                    }
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.showLyrics) {
                // Large Album Art with rounded corners and subtle sunset glow
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141A29),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF28154A), Color(0xFF0F1A3A), Color(0xFF1B0B2E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = "Album art",
                            tint = GMKElectricBlue.copy(alpha = 0.8f),
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }
            } else {
                // Lyrics view
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GMKSurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("♪ Instrumental Melody ♪", color = GMKElectricBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(track.name, color = GMKTextWhite, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Lyrics synced with GMK Player engine", color = GMKTextMuted, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Song Info: Title & Artist + Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GMKTextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "GMK Music",
                        fontSize = 14.sp,
                        color = GMKTextMuted
                    )
                }

                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (uiState.isFavorite) Color(0xFFFF1744) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Animated Sound Waveform Visualizer
            AnimatedWaveformVisualizer(isPlaying = uiState.isPlaying)

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar & Time
            Slider(
                value = uiState.currentPositionMs.toFloat(),
                onValueChange = { pos -> viewModel.seekTo(pos.toLong()) },
                valueRange = 0f..uiState.durationMs.coerceAtLeast(1L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = GMKElectricBlue,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(uiState.currentPositionMs), fontSize = 12.sp, color = GMKTextMuted)
                Text(text = formatTime(uiState.durationMs), fontSize = 12.sp, color = GMKTextMuted)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Controls (Shuffle, Prev, Central Large Glowing Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (uiState.isShuffle) GMKElectricBlue else GMKTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Central large Play/Pause with glowing electric blue border
                Surface(
                    shape = CircleShape,
                    color = Color(0x331E88E5),
                    modifier = Modifier
                        .size(72.dp)
                        .border(2.5.dp, GMKElectricBlue, CircleShape)
                        .clickable { viewModel.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (uiState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (uiState.repeatMode != RepeatMode.OFF) GMKElectricBlue else GMKTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Screen 6 Bottom Action Row: Equalizer, Sleep Timer, Playback Speed (1.0x pill), Add to Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equalizer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenEqualizer() }
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Equalizer", fontSize = 11.sp, color = GMKTextMuted)
                }

                // Sleep Timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenSleepTimer() }
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep timer", tint = if (uiState.sleepTimerMinutes != null) GMKElectricBlue else Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sleep timer", fontSize = 11.sp, color = if (uiState.sleepTimerMinutes != null) GMKElectricBlue else GMKTextMuted)
                }

                // Playback Speed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenSpeed() }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${uiState.playbackSpeed}x".replace(".0x", "x"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Playback speed", fontSize = 11.sp, color = GMKTextMuted)
                }

                // Add to Playlist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenPlaylistAdd() }
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add to playlist", fontSize = 11.sp, color = GMKTextMuted)
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveformVisualizer(isPlaying: Boolean) {
    val barCount = 36
    val transition = rememberInfiniteTransition(label = "waveform")
    val pulse by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val baseHeight = when (i % 6) {
                0 -> 0.35f
                1 -> 0.75f
                2 -> 1.0f
                3 -> 0.55f
                4 -> 0.85f
                else -> 0.4f
            }
            val dynamicHeight = if (isPlaying) {
                val factor = (baseHeight * (0.4f + 0.6f * pulse)).coerceIn(0.15f, 1.0f)
                factor
            } else {
                (baseHeight * 0.3f).coerceAtLeast(0.12f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(dynamicHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF7C4DFF), GMKElectricBlue)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun AudioEqualizerDialog(onDismiss: () -> Unit) {
    val presets = listOf("Flat", "Bass Boost", "Vocal", "Rock", "Pop", "Cinema")
    var selectedPreset by remember { mutableStateOf("Bass Boost") }
    var bassLevel by remember { mutableFloatStateOf(0.75f) }
    var midLevel by remember { mutableFloatStateOf(0.5f) }
    var trebleLevel by remember { mutableFloatStateOf(0.6f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Equalizer & Sound FX", fontWeight = FontWeight.Bold, color = GMKTextWhite)
            }
        },
        text = {
            Column {
                Text("Sound Presets", fontSize = 12.sp, color = GMKTextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPreset == p) GMKElectricBlue else Color(0xFF1E2638),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPreset = p }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = p,
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
                    presets.drop(3).forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPreset == p) GMKElectricBlue else Color(0xFF1E2638),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPreset = p }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Bass: ${(bassLevel * 100).toInt()}%", fontSize = 12.sp, color = GMKTextWhite)
                Slider(
                    value = bassLevel,
                    onValueChange = { bassLevel = it },
                    colors = SliderDefaults.colors(activeTrackColor = GMKElectricBlue, thumbColor = GMKElectricBlue)
                )

                Text("Treble: ${(trebleLevel * 100).toInt()}%", fontSize = 12.sp, color = GMKTextWhite)
                Slider(
                    value = trebleLevel,
                    onValueChange = { trebleLevel = it },
                    colors = SliderDefaults.colors(activeTrackColor = GMKElectricBlue, thumbColor = GMKElectricBlue)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Apply", fontWeight = FontWeight.Bold, color = GMKElectricBlue) }
        },
        containerColor = GMKSurfaceDark
    )
}

@Composable
fun AudioSleepTimerDialog(
    currentTimer: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        null to "Off (Cancel timer)",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer", fontWeight = FontWeight.Bold, color = GMKTextWhite)
            }
        },
        text = {
            Column {
                options.forEach { (mins, label) ->
                    val isSelected = currentTimer == mins
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = GMKTextWhite, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = GMKElectricBlue) }
        },
        containerColor = GMKSurfaceDark
    )
}

@Composable
fun AudioSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = GMKElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", fontWeight = FontWeight.Bold, color = GMKTextWhite)
            }
        },
        text = {
            Column {
                speeds.forEach { sp ->
                    val isSelected = currentSpeed == sp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSpeed(sp)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectSpeed(sp)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${sp}x ${if (sp == 1.0f) "(Normal)" else ""}", color = GMKTextWhite, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = GMKElectricBlue) }
        },
        containerColor = GMKSurfaceDark
    )
}
