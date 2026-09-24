package com.melovish.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MediaPlaybackService.start(this)
        val manager = MusicManager(this)
        setContent {
            MelovishRootApp(manager)
        }
    }
}

@Composable
fun MelovishRootApp(manager: MusicManager) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val isDark = when (manager.themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> systemDark
    }

    LaunchedEffect(isDark) {
        manager.isDarkMode = isDark
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) manager.scanStorage()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) manager.scanStorage() else launcher.launch(permission)
    }

    var activeScreen by remember { mutableStateOf("home") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isSettingsEqOpen by remember { mutableStateOf(false) }

    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var activeTagEditSong by remember { mutableStateOf<Song?>(null) }
    var activeSongInfo by remember { mutableStateOf<Song?>(null) }
    var activeAddToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    BackHandler(enabled = isSettingsEqOpen || isPlayerExpanded || selectedArtist != null || selectedAlbum != null || selectedPlaylist != null || selectedFolder != null || activeScreen != "home") {
        when {
            isSettingsEqOpen -> isSettingsEqOpen = false
            isPlayerExpanded -> isPlayerExpanded = false
            selectedArtist != null -> selectedArtist = null
            selectedAlbum != null -> selectedAlbum = null
            selectedPlaylist != null -> selectedPlaylist = null
            selectedFolder != null -> selectedFolder = null
            else -> activeScreen = "home"
        }
    }

    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize().background(bg)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    manager = manager,
                    onProfileClick = { activeScreen = "profile" },
                    onSettingsClick = { activeScreen = "settings" }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        selectedArtist != null -> {
                            FilteredSongsScreen(
                                title = "Artist: ${selectedArtist!!}",
                                songs = manager.allSongs.filter { it.artist.equals(selectedArtist, ignoreCase = true) },
                                manager = manager,
                                onBack = { selectedArtist = null },
                                onSongMenuClick = { activeSongForMenu = it }
                            )
                        }
                        selectedAlbum != null -> {
                            FilteredSongsScreen(
                                title = "Album: ${selectedAlbum!!}",
                                songs = manager.allSongs.filter { it.album.equals(selectedAlbum, ignoreCase = true) },
                                manager = manager,
                                onBack = { selectedAlbum = null },
                                onSongMenuClick = { activeSongForMenu = it }
                            )
                        }
                        selectedPlaylist != null -> {
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist!!,
                                manager = manager,
                                onBack = { selectedPlaylist = null },
                                onSongMenuClick = { activeSongForMenu = it },
                                onFolderClick = { folder -> selectedFolder = folder }
                            )
                        }
                        selectedFolder != null -> {
                            FolderSongsScreen(
                                folderName = selectedFolder!!,
                                manager = manager,
                                onBack = { selectedFolder = null },
                                onSongMenuClick = { activeSongForMenu = it }
                            )
                        }
                        activeScreen == "settings" -> SettingsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" },
                            onOpenProfile = { activeScreen = "profile" },
                            onOpenEqualizer = { isSettingsEqOpen = true }
                        )
                        activeScreen == "profile" -> ProfileScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" }
                        )
                        activeScreen == "search" -> SearchScreen(
                            manager = manager,
                            onSongMenuClick = { activeSongForMenu = it }
                        )
                        activeScreen == "library" -> LibraryScreen(
                            manager = manager,
                            onFolderClick = { folder -> selectedFolder = folder }
                        )
                        else -> HomeScreen(
                            manager = manager,
                            onPlaylistClick = { pl -> selectedPlaylist = pl },
                            onResumeClick = { manager.resumeLastPlayed() },
                            onSongMenuClick = { activeSongForMenu = it }
                        )
                    }
                }

                if (manager.currentSong != null && !isPlayerExpanded) {
                    MiniPlayerDock(manager = manager, onClick = { isPlayerExpanded = true })
                }

                if (activeScreen in listOf("home", "search", "library") && selectedFolder == null && selectedPlaylist == null && selectedArtist == null && selectedAlbum == null) {
                    BottomNavBar(
                        manager = manager,
                        activeTab = activeScreen,
                        onTabSelected = { activeScreen = it }
                    )
                }
            }

            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullPlayerSheet(manager = manager, onDismiss = { isPlayerExpanded = false })
            }

            if (isSettingsEqOpen) {
                EqualizerSheet(manager = manager, onDismiss = { isSettingsEqOpen = false })
            }

            // Universal 9-Option Action Sheet
            if (activeSongForMenu != null) {
                val s = activeSongForMenu!!
                SongItemActionModal(
                    song = s,
                    isDark = isDark,
                    onDismiss = { activeSongForMenu = null },
                    onPlayNext = {
                        manager.playNextInQueue(s)
                        activeSongForMenu = null
                    },
                    onAddToPlaylist = {
                        activeAddToPlaylistSong = s
                        activeSongForMenu = null
                    },
                    onGoToAlbum = {
                        selectedAlbum = s.album
                        activeSongForMenu = null
                    },
                    onGoToArtist = {
                        selectedArtist = s.artist
                        activeSongForMenu = null
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, s.uri)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${s.title}"))
                        activeSongForMenu = null
                    },
                    onTagEditor = {
                        activeTagEditSong = s
                        activeSongForMenu = null
                    },
                    onDetails = {
                        activeSongInfo = s
                        activeSongForMenu = null
                    },
                    onSetRingtone = {
                        manager.setAsRingtone(s)
                        activeSongForMenu = null
                    },
                    onDelete = {
                        manager.deleteSongFromDevice(s)
                        activeSongForMenu = null
                    }
                )
            }

            if (activeTagEditSong != null) {
                TagEditorDialog(manager = manager, song = activeTagEditSong!!, onDismiss = { activeTagEditSong = null })
            }

            if (activeAddToPlaylistSong != null) {
                AddToPlaylistDialog(manager = manager, song = activeAddToPlaylistSong!!, onDismiss = { activeAddToPlaylistSong = null })
            }

            if (activeSongInfo != null) {
                val s = activeSongInfo!!
                SongInfoDialog(song = s, isDark = isDark, onDismiss = { activeSongInfo = null })
            }
        }
    }
}

// 3-Layout Recently Played Cover Card with Bottom Translucent Scrim
@Composable
fun RecentlyPlayedCard(
    song: Song,
    manager: MusicManager,
    onClick: () -> Unit
) {
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    Box(
        modifier = Modifier
            .width(116.dp)
            .height(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
    ) {
        if (albumArtBitmap != null) {
            Image(
                bitmap = albumArtBitmap!!.asImageBitmap(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🎵", fontSize = 36.sp)
            }
        }

        // Bottom Translucent Opaque Blur-Gradient Area
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x88000000),
                            Color(0xDE000000)
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LiveMechanicalGearIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(12000, easing = LinearEasing)),
        label = "gearAngle"
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .shadow(6.dp, CircleShape)
            .background(if (isDark) Color(0x33FFFFFF) else Color(0x1A000000))
            .border(1.5.dp, if (isDark) Color(0x44FFFFFF) else Color(0x22000000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp).rotate(rotation)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val toothDepth = outerRadius * 0.22f
            val innerRingRadius = outerRadius - toothDepth
            val teethCount = 12

            val gearColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

            val path = Path()
            for (i in 0 until teethCount) {
                val angleStep = (2.0 * Math.PI / teethCount).toFloat()
                val a1 = i * angleStep
                val a2 = a1 + (angleStep * 0.35f)
                val a3 = a1 + (angleStep * 0.65f)
                val a4 = (i + 1) * angleStep

                val p1 = Offset(center.x + (innerRingRadius * cos(a1)), center.y + (innerRingRadius * sin(a1)))
                val p2 = Offset(center.x + (outerRadius * cos(a2)), center.y + (outerRadius * sin(a2)))
                val p3 = Offset(center.x + (outerRadius * cos(a3)), center.y + (outerRadius * sin(a3)))
                val p4 = Offset(center.x + (innerRingRadius * cos(a4)), center.y + (innerRingRadius * sin(a4)))

                if (i == 0) path.moveTo(p1.x, p1.y) else path.lineTo(p1.x, p1.y)
                path.lineTo(p2.x, p2.y)
                path.lineTo(p3.x, p3.y)
                path.lineTo(p4.x, p4.y)
            }
            path.close()
            drawPath(path, gearColor)

            val cavityRadius = innerRingRadius * 0.72f
            drawCircle(
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                radius = cavityRadius,
                center = center
            )

            val hubRadius = innerRingRadius * 0.32f
            drawCircle(color = gearColor, radius = hubRadius, center = center)

            val spokeStroke = Stroke(width = 2.5f.dp.toPx())
            for (s in 0 until 3) {
                val spAngle = (s * 2.0 * Math.PI / 3.0).toFloat()
                val spokeEnd = Offset(
                    center.x + (cavityRadius * cos(spAngle)),
                    center.y + (cavityRadius * sin(spAngle))
                )
                drawLine(
                    color = gearColor,
                    start = center,
                    end = spokeEnd,
                    strokeWidth = spokeStroke.width
                )
            }
        }
    }
}

@Composable
fun TopBar(
    manager: MusicManager,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) {
        BitmapFactory.decodeFile(avatarFile.absolutePath)
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isDark) Color(0x33FFFFFF) else Color(0x14000000))
                .border(2.dp, manager.accentColor, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
            } else {
                Text("👤", fontSize = 20.sp)
            }
        }

        Text(
            text = "Melovish",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        LiveMechanicalGearIcon(
            isDark = isDark,
            modifier = Modifier.clickable { onSettingsClick() }
        )
    }
}

@Composable
fun UniversalSongRow(
    song: Song,
    manager: MusicManager,
    isDark: Boolean,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtBitmap != null) {
                Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
            } else {
                Text("🎵", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1)
        }
        Text(formatTime(song.duration), color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("⋮", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun HomeScreen(
    manager: MusicManager,
    onPlaylistClick: (Playlist) -> Unit,
    onResumeClick: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val sortedSongs = manager.getSortedSongs()
    val recents = manager.historySongs.take(30)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Recently Played Header with Right-Side Play Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(manager.accentColor)
                            .clickable {
                                if (recents.isNotEmpty()) {
                                    manager.playSong(recents.first(), recents, "Recently Played")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3-Card Scrollable Row Matching Reference
                if (recents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recently played tracks yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recents, key = { it.id }) { song ->
                            RecentlyPlayedCard(
                                song = song,
                                manager = manager,
                                onClick = { manager.playSong(song, recents, "Recent Tracks") }
                            )
                        }
                    }
                }
            }

            // 2. Favourite Playlists
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favourite Playlists", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (manager.customPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists yet. Tap '+ New' to create one.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(manager.customPlaylists, key = { it.id }) { pl ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                    .clickable { onPlaylistClick(pl) }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(if (pl.isFolderPinned) "📁" else "📑", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${pl.songIds.size} songs", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. All Songs Header with Sort Dropdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Songs", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${manager.allSongs.size} Songs", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable { showSortMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.currentSortOrder = SongSortOrder.A_TO_Z; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.currentSortOrder = SongSortOrder.Z_TO_A; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Newest First") }, onClick = { manager.currentSortOrder = SongSortOrder.NEWEST; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Oldest First") }, onClick = { manager.currentSortOrder = SongSortOrder.OLDEST; showSortMenu = false })
                                DropdownMenuItem(text = { Text("By Artist") }, onClick = { manager.currentSortOrder = SongSortOrder.ARTIST; showSortMenu = false })
                                DropdownMenuItem(text = { Text("By File Size") }, onClick = { manager.currentSortOrder = SongSortOrder.FILE_SIZE; showSortMenu = false })
                                DropdownMenuItem(text = { Text("By Duration") }, onClick = { manager.currentSortOrder = SongSortOrder.DURATION; showSortMenu = false })
                            }
                        }
                    }
                }
            }

            items(sortedSongs, key = { it.id }) { song ->
                UniversalSongRow(
                    song = song,
                    manager = manager,
                    isDark = isDark,
                    onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                    onMenuClick = { onSongMenuClick(song) }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(manager.accentColor)
                .clickable { onResumeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(manager = manager, onDismiss = { showCreatePlaylistDialog = false })
    }
}

@Composable
fun LibraryScreen(manager: MusicManager, onFolderClick: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    var showFolderSortMenu by remember { mutableStateOf(false) }

    val sortedFolders = manager.getSortedFolders()
    val folderMap = manager.allSongs.groupBy { it.folderName }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Folders & Storage", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box {
                    Button(onClick = { showFolderSortMenu = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                        Text("⇅ Sort", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showFolderSortMenu, onDismissRequest = { showFolderSortMenu = false }) {
                        DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.A_TO_Z; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.Z_TO_A; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Latest Added") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.LATEST; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Oldest Added") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.OLDEST; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Most Played") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.MOST_PLAYED; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Largest Size") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.LARGEST_SIZE; showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Most Songs") }, onClick = { manager.currentFolderSortOrder = FolderSortOrder.MOST_SONGS; showFolderSortMenu = false })
                    }
                }
            }
        }

        items(sortedFolders, key = { it }) { folderName ->
            val songs = folderMap[folderName] ?: emptyList()
            val totalSize = songs.sumOf { it.size }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                    .clickable { onFolderClick(folderName) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📁", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folderName, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${formatFileSize(totalSize)} • ${songs.size} songs", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                Text("›", color = manager.accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchScreen(manager: MusicManager, onSongMenuClick: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val filtered = manager.allSongs.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.artist.contains(query, ignoreCase = true) ||
        it.folderName.contains(query, ignoreCase = true)
    }

    val isDark = manager.isDarkMode

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, or folders...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { song ->
                UniversalSongRow(
                    song = song,
                    manager = manager,
                    isDark = isDark,
                    onPlay = { manager.playSong(song, filtered, "Search Results") },
                    onMenuClick = { onSongMenuClick(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    manager: MusicManager,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onFolderClick: (String) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    val songsInPlaylist = playlist.songIds.mapNotNull { id -> manager.allSongs.find { it.id == id } }
    var showAddSongsSearchPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("${songsInPlaylist.size} songs", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row {
                Button(
                    onClick = { manager.shufflePlaylist(playlist) },
                    colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔀 Shuffle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddSongsSearchPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (songsInPlaylist.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist is empty. Tap '+ Add' to search and add tracks.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(songsInPlaylist, key = { it.id }) { song ->
                    UniversalSongRow(
                        song = song,
                        manager = manager,
                        isDark = isDark,
                        onPlay = { manager.playSong(song, songsInPlaylist, playlist.name) },
                        onMenuClick = { onSongMenuClick(song) }
                    )
                }
            }
        }
    }

    if (showAddSongsSearchPicker) {
        PlaylistAddSearchDialog(
            playlist = playlist,
            manager = manager,
            onDismiss = { showAddSongsSearchPicker = false },
            onNavigateToFolder = { folder ->
                showAddSongsSearchPicker = false
                onFolderClick(folder)
            }
        )
    }
}

@Composable
fun PlaylistAddSearchDialog(
    playlist: Playlist,
    manager: MusicManager,
    onDismiss: () -> Unit,
    onNavigateToFolder: (String) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = manager.allSongs.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.artist.contains(searchQuery, ignoreCase = true)
    }

    val folders = manager.allSongs.map { it.folderName }.distinct()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).height(600.dp).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF0F172A) else Color.White).padding(20.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Songs to ${playlist.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Search All", modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("By Folder", modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("Playlists", modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search songs or artists...") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredSongs, key = { it.id }) { s ->
                                val isAdded = s.id in playlist.songIds
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable {
                                        if (isAdded) manager.removeSongFromPlaylist(s.id, playlist)
                                        else manager.addSongToPlaylist(s.id, playlist)
                                    }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${formatFileSize(s.size)} • ${s.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                    Text(if (isAdded) "✓ Added" else "+ Add", color = if (isAdded) manager.accentColor else Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(folders, key = { it }) { folder ->
                                val count = manager.allSongs.count { it.folderName == folder }
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.clickable { onNavigateToFolder(folder) }) {
                                        Text("📁 $folder", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("$count songs • Tap to view", color = manager.accentColor, fontSize = 11.sp)
                                    }
                                    Button(onClick = {
                                        val folderSongIds = manager.allSongs.filter { it.folderName == folder }.map { it.id }
                                        folderSongIds.forEach { manager.addSongToPlaylist(it, playlist) }
                                    }, shape = RoundedCornerShape(8.dp)) {
                                        Text("Add All", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(manager.customPlaylists.filter { it.id != playlist.id }, key = { it.id }) { pl ->
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📑 ${pl.name} (${pl.songIds.size} songs)", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Button(onClick = { pl.songIds.forEach { manager.addSongToPlaylist(it, playlist) } }, shape = RoundedCornerShape(8.dp)) {
                                        Text("Copy All", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredSongsScreen(title: String, songs: List<Song>, manager: MusicManager, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${songs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(songs, key = { it.id }) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, songs, title) }, onMenuClick = { onSongMenuClick(song) })
            }
        }
    }
}

@Composable
fun FolderSongsScreen(folderName: String, manager: MusicManager, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
    val songs = manager.allSongs.filter { it.folderName == folderName }
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(folderName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(songs, key = { it.id }) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, songs, folderName) }, onMenuClick = { onSongMenuClick(song) })
            }
        }
    }
}

@Composable
fun SongItemActionModal(
    song: Song,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onTagEditor: () -> Unit,
    onDetails: () -> Unit,
    onSetRingtone: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(cardBg).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(song.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))

                ModalActionRow("▶≡", "Play next", textColor) { onPlayNext() }
                ModalActionRow("≡+", "Add to playlist", textColor) { onAddToPlaylist() }
                ModalActionRow("◎", "Go to album", textColor) { onGoToAlbum() }
                ModalActionRow("👤", "Go to artist", textColor) { onGoToArtist() }
                ModalActionRow("🔗", "Share", textColor) { onShare() }
                ModalActionRow("🏷️", "Tag editor", textColor) { onTagEditor() }
                ModalActionRow("ℹ️", "Details", textColor) { onDetails() }
                ModalActionRow("🔔", "Set as ringtone", textColor) { onSetRingtone() }
                ModalActionRow("🗑️", "Delete from device", Color(0xFFEF4444)) { onDelete() }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(12.dp)) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModalActionRow(icon: String, title: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp, color = color, modifier = Modifier.width(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun SongInfoDialog(song: Song, isDark: Boolean, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(20.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).padding(20.dp)) {
            Column {
                Text("Details", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Title: ${song.title}", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                Text("Artist: ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("Album: ${song.album}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("Size: ${formatFileSize(song.size)}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("Duration: ${formatTime(song.duration)}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("Path: ${song.path}", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(manager: MusicManager, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val folders = manager.allSongs.map { it.folderName }.distinct()
    var selectedFolderToPin by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E293B)).padding(24.dp)) {
            Column {
                Text("Create New Playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                Text("Or Pin an Entire Device Folder:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders, key = { it }) { folder ->
                        val isSel = selectedFolderToPin == folder
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) manager.accentColor else Color(0x33FFFFFF))
                                .clickable {
                                    selectedFolderToPin = folder
                                    name = folder
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("📁 $folder", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (selectedFolderToPin != null) {
                            manager.pinFolderAsPlaylist(selectedFolderToPin!!)
                        } else if (name.isNotBlank()) {
                            manager.createPlaylist(name)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor)
                ) {
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(manager: MusicManager, activeTab: String, onTabSelected: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val navBg = if (isDark) Color(0xE60A0F1D) else Color(0xF2FFFFFF)
    val accent = manager.accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBg)
            .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("home", "Home", "🏠"),
            Triple("library", "Library", "📚"),
            Triple("search", "Search", "🔍")
        ).forEach { (key, label, icon) ->
            val isSel = activeTab == key
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTabSelected(key) }) {
                Text(icon, fontSize = 20.sp)
                Text(text = label, fontSize = 11.sp, color = if (isSel) accent else Color(0xFF64748B), fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
