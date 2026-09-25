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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
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
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) manager.scanStorage()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) manager.scanStorage() else launcher.launch(permission)
    }

    var activeScreen by remember { mutableStateOf("home") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isSettingsEqOpen by remember { mutableStateOf(false) }

    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var activeTagEditSong by remember { mutableStateOf<Song?>(null) }
    var activeSongInfo by remember { mutableStateOf<Song?>(null) }
    var activeAddToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    val homeListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    val bg = if (isDark) Color(0xFF0A0F1D) else Color(0xFFF8F9FA)

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
                            ArtistDetailScreen(
                                artistItem = selectedArtist!!,
                                manager = manager,
                                isDark = isDark,
                                onBack = { selectedArtist = null },
                                onSongMenuClick = { activeSongForMenu = it }
                            )
                        }
                        selectedAlbum != null -> {
                            FilteredSongsScreen(
                                title = "Album: ${selectedAlbum!!}",
                                songs = manager.allSongs.filter { it.album.equals(selectedAlbum, ignoreCase = true) },
                                manager = manager,
                                isDark = isDark,
                                onBack = { selectedAlbum = null },
                                onSongMenuClick = { activeSongForMenu = it }
                            )
                        }
                        selectedPlaylist != null -> {
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist!!,
                                manager = manager,
                                isDark = isDark,
                                onBack = { selectedPlaylist = null },
                                onSongMenuClick = { activeSongForMenu = it },
                                onFolderClick = { folder -> selectedFolder = folder }
                            )
                        }
                        selectedFolder != null -> {
                            FolderSongsScreen(
                                folderName = selectedFolder!!,
                                manager = manager,
                                isDark = isDark,
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
                        activeScreen == "artists" -> ArtistsScreen(
                            manager = manager,
                            listState = artistsListState,
                            onArtistClick = { artist -> selectedArtist = artist }
                        )
                        activeScreen == "search" -> SearchScreen(
                            manager = manager,
                            listState = searchListState,
                            onSongMenuClick = { activeSongForMenu = it }
                        )
                        activeScreen == "library" -> LibraryScreen(
                            manager = manager,
                            listState = libraryListState,
                            onFolderClick = { folder -> selectedFolder = folder }
                        )
                        else -> HomeScreen(
                            manager = manager,
                            listState = homeListState,
                            onPlaylistClick = { pl -> selectedPlaylist = pl },
                            onResumeClick = { manager.resumeLastPlayed() },
                            onSongMenuClick = { activeSongForMenu = it }
                        )
                    }
                }

                if (manager.currentSong != null && !isPlayerExpanded) {
                    MiniPlayerDock(manager = manager, onClick = { isPlayerExpanded = true })
                }

                if (activeScreen in listOf("home", "library", "artists", "search") && selectedFolder == null && selectedPlaylist == null && selectedArtist == null && selectedAlbum == null) {
                    BottomNavBar(
                        manager = manager,
                        activeTab = activeScreen,
                        onTabSelected = { tab ->
                            if (activeScreen == tab) {
                                coroutineScope.launch {
                                    when (tab) {
                                        "home" -> homeListState.animateScrollToItem(0)
                                        "library" -> libraryListState.animateScrollToItem(0)
                                        "artists" -> artistsListState.animateScrollToItem(0)
                                        "search" -> searchListState.animateScrollToItem(0)
                                    }
                                }
                            } else {
                                activeScreen = tab
                            }
                        }
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
                        val matchingArtist = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                            .find { it.name.equals(s.artist, ignoreCase = true) }
                            ?: ArtistItem(name = s.artist, songs = mutableListOf(s))
                        selectedArtist = matchingArtist
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

@Composable
fun RecentlyPlayedCard(song: Song, manager: MusicManager, onClick: () -> Unit) {
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .width(116.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
    ) {
        if (albumArtBitmap != null) {
            Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🎵", fontSize = 36.sp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x88000000), Color(0xDE000000))))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = song.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LiveMechanicalGearIcon(isDark: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotation")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "gearAngle")

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

            val path = Path().apply {
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

                    if (i == 0) moveTo(p1.x, p1.y) else lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                }
                close()
            }
            drawPath(path, gearColor)

            val cavityRadius = innerRingRadius * 0.72f
            drawCircle(color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0), radius = cavityRadius, center = center)
            val hubRadius = innerRingRadius * 0.32f
            drawCircle(color = gearColor, radius = hubRadius, center = center)

            val spokeStroke = Stroke(width = 2.5f.dp.toPx())
            for (s in 0 until 3) {
                val spAngle = (s * 2.0 * Math.PI / 3.0).toFloat()
                val spokeEnd = Offset(center.x + (cavityRadius * cos(spAngle)), center.y + (cavityRadius * sin(spAngle)))
                drawLine(color = gearColor, start = center, end = spokeEnd, strokeWidth = spokeStroke.width)
            }
        }
    }
}

@Composable
fun TopBar(manager: MusicManager, onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) BitmapFactory.decodeFile(avatarFile.absolutePath) else null

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
                .background(Color(0xFF030712))
                .border(2.dp, manager.accentColor, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
            } else {
                DefaultProfileAvatar(modifier = Modifier.fillMaxSize())
            }
        }

        Text(text = "Melovish", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

        LiveMechanicalGearIcon(isDark = isDark, modifier = Modifier.clickable { onSettingsClick() })
    }
}

@Composable
fun UniversalSongRow(song: Song, manager: MusicManager, isDark: Boolean, onPlay: () -> Unit, onMenuClick: () -> Unit) {
    val isPlayingThis = manager.currentSong?.id == song.id
    val accent = manager.accentColor

    val textColor = if (isPlayingThis) accent else if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isPlayingThis) accent.copy(alpha = 0.08f) else if (isDark) Color(0xFF131B2E) else Color.White
    val borderColor = if (isPlayingThis) accent.copy(alpha = 0.6f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFECEFF3)

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
            if (albumArtBitmap != null) {
                Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
            } else {
                Text("🎵", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = textColor, fontSize = 14.sp, fontWeight = if (isPlayingThis) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1)
        }

        if (isPlayingThis) {
            LiveAudioWaveEqualizer(isAnimating = manager.isPlaying, accentColor = accent)
        } else {
            Text(formatTime(song.duration), color = Color(0xFF94A3B8), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onMenuClick() }, contentAlignment = Alignment.Center) {
            Text("⋮", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// Compact Song Card with Centered Alignment Everywhere
@Composable
fun UniversalSongCard(song: Song, manager: MusicManager, isDark: Boolean, onPlay: () -> Unit, onMenuClick: () -> Unit) {
    val isPlayingThis = manager.currentSong?.id == song.id
    val accent = manager.accentColor
    val cardBg = if (isPlayingThis) accent.copy(alpha = 0.12f) else if (isDark) Color(0xFF131B2E) else Color.White
    val textColor = if (isPlayingThis) accent else if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, if (isPlayingThis) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
            .clickable { onPlay() }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎵", fontSize = 30.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(song.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(if (song.artist.isNotBlank()) song.artist else "Unknown", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).clickable { onMenuClick() }) {
            Text("⋮", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Home Screen: Exactly 4 Square Cards in a Row with Large Centered Folders & Touch-Hold Rearrange
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    manager: MusicManager,
    listState: LazyListState,
    onPlaylistClick: (Playlist) -> Unit,
    onResumeClick: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var customizingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showRainbowWheelForPl by remember { mutableStateOf(false) }

    val sortedSongs = manager.getSortedSongs()
    val recents = manager.historySongs.take(30)

    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp - 32 - (3 * 8)) / 4).coerceAtLeast(76).dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = "Recently Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(10.dp))

                if (recents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)), contentAlignment = Alignment.Center) {
                        Text("No recently played tracks yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recents, key = { it.id }) { song ->
                            RecentlyPlayedCard(
                                song = song,
                                manager = manager,
                                onClick = { manager.playSong(song, manager.historySongs, "Recently Played") }
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Favourite Playlists", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showCreatePlaylistDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor), shape = RoundedCornerShape(12.dp)) {
                        Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (manager.customPlaylists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)), contentAlignment = Alignment.Center) {
                        Text("No playlists yet. Tap '+ New' to create one.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    // Exactly 4 in a row, fully square, centered icons, and direct touch-hold drag-and-drop to reorder
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(manager.customPlaylists, key = { _, pl -> pl.id }) { index, pl ->
                            Box(
                                modifier = Modifier
                                    .size(cardWidth)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onPlaylistClick(pl) },
                                        onLongClick = { customizingPlaylist = pl }
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    GlassmorphicFolderIcon(
                                        folderColor = Color(pl.iconColorHex),
                                        modifier = Modifier.size(46.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        pl.name,
                                        color = textColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${pl.songIds.size}",
                                        color = Color(pl.iconColorHex),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("All Songs", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("${manager.allSongs.size} Songs", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)).clickable { showSortMenu = true }, contentAlignment = Alignment.Center) {
                                Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.setPersistentSongSort(SongSortOrder.A_TO_Z); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.setPersistentSongSort(SongSortOrder.Z_TO_A); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Newest First") }, onClick = { manager.setPersistentSongSort(SongSortOrder.NEWEST); showSortMenu = false })
                                DropdownMenuItem(text = { Text("Oldest First") }, onClick = { manager.setPersistentSongSort(SongSortOrder.OLDEST); showSortMenu = false })
                                DropdownMenuItem(text = { Text("By Artist") }, onClick = { manager.setPersistentSongSort(SongSortOrder.ARTIST); showSortMenu = false })
                                DropdownMenuItem(text = { Text("By File Size") }, onClick = { manager.setPersistentSongSort(SongSortOrder.FILE_SIZE); showSortMenu = false })
                                DropdownMenuItem(text = { Text("By Duration") }, onClick = { manager.setPersistentSongSort(SongSortOrder.DURATION); showSortMenu = false })
                            }
                        }
                    }
                }
            }

            items(sortedSongs, key = { it.id }) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, "All Songs") }, onMenuClick = { onSongMenuClick(song) })
            }
        }

        if (manager.currentSong == null) {
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
    }

    if (showCreatePlaylistDialog) CreatePlaylistDialog(manager = manager, onDismiss = { showCreatePlaylistDialog = false })

    if (customizingPlaylist != null) {
        val pl = customizingPlaylist!!
        FolderColorDialog(
            folderName = pl.name,
            currentColor = Color(pl.iconColorHex),
            isDark = isDark,
            onColorSelected = { newColor -> manager.updatePlaylistColorOnly(pl, newColor.toArgb().toLong()) },
            onOpenRainbowPicker = { showRainbowWheelForPl = true },
            onDismiss = { customizingPlaylist = null }
        )
    }

    if (showRainbowWheelForPl) CircularColorPickerDialog(manager = manager, onDismiss = { showRainbowWheelForPl = false })
}

// Library Screen: Integrated Cards/Lines View Switcher
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(manager: MusicManager, listState: LazyListState, onFolderClick: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var showFolderSortMenu by remember { mutableStateOf(false) }
    var customizingFolder by remember { mutableStateOf<String?>(null) }
    var showRainbowWheelForFolder by remember { mutableStateOf(false) }

    val sortedFolders = manager.getSortedFolders()
    val folderMap = manager.allSongs.groupBy { it.folderName }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Folders & Storage", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Main Library View Switcher (Cards vs Lines)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable { manager.setPersistentFolderInnerCardView(!manager.folderInnerIsCardView) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (manager.folderInnerIsCardView) "☰" else "⊞", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    Button(
                        onClick = { showFolderSortMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⇅ Sort", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showFolderSortMenu, onDismissRequest = { showFolderSortMenu = false }) {
                        DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.A_TO_Z); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.Z_TO_A); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Latest Added") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.LATEST); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Oldest Added") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.OLDEST); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Most Played") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.MOST_PLAYED); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Largest Size") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.LARGEST_SIZE); showFolderSortMenu = false })
                        DropdownMenuItem(text = { Text("Most Songs") }, onClick = { manager.setPersistentFolderSort(FolderSortOrder.MOST_SONGS); showFolderSortMenu = false })
                    }
                }
            }
        }

        if (manager.folderInnerIsCardView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedFolders, key = { it }) { folderName ->
                    val songs = folderMap[folderName] ?: emptyList()
                    val totalSize = songs.sumOf { it.size }
                    val fColor = manager.getFolderColor(folderName)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                            .combinedClickable(
                                onClick = { onFolderClick(folderName) },
                                onLongClick = { customizingFolder = folderName }
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            GlassmorphicFolderIcon(folderColor = fColor, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(folderName, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${songs.size} songs • ${formatFileSize(totalSize)}", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedFolders, key = { it }) { folderName ->
                    val songs = folderMap[folderName] ?: emptyList()
                    val totalSize = songs.sumOf { it.size }
                    val fColor = manager.getFolderColor(folderName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                            .combinedClickable(onClick = { onFolderClick(folderName) }, onLongClick = { customizingFolder = folderName })
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassmorphicFolderIcon(folderColor = fColor, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folderName, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${formatFileSize(totalSize)} • ${songs.size} songs", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("›", color = fColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (customizingFolder != null) {
        val folder = customizingFolder!!
        FolderColorDialog(
            folderName = folder,
            currentColor = manager.getFolderColor(folder),
            isDark = isDark,
            onColorSelected = { newColor -> manager.updateFolderColorOnly(folder, newColor.toArgb().toLong()) },
            onOpenRainbowPicker = { showRainbowWheelForFolder = true },
            onDismiss = { customizingFolder = null }
        )
    }

    if (showRainbowWheelForFolder) CircularColorPickerDialog(manager = manager, onDismiss = { showRainbowWheelForFolder = false })
}

@Composable
fun SearchScreen(manager: MusicManager, listState: LazyListState, onSongMenuClick: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val filtered = manager.allSongs.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) || it.folderName.contains(query, ignoreCase = true)
    }

    val isDark = manager.isDarkMode

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, or folders...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, filtered, "Search Results") }, onMenuClick = { onSongMenuClick(song) })
            }
        }
    }
}

// Symmetrical Top Alignment + Inside Sort & View Switcher (Cards vs Lines) for Playlists
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onFolderClick: (String) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var showAddSongsSearchPicker by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val rawSongsInPlaylist = playlist.songIds.mapNotNull { id -> manager.allSongs.find { it.id == id } }

    val sortedSongs = remember(rawSongsInPlaylist, manager.playlistInnerSortOrder) {
        when (manager.playlistInnerSortOrder) {
            SongSortOrder.A_TO_Z -> rawSongsInPlaylist.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> rawSongsInPlaylist.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> rawSongsInPlaylist.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> rawSongsInPlaylist.sortedByDescending { it.size }
            SongSortOrder.NEWEST -> rawSongsInPlaylist.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> rawSongsInPlaylist.sortedBy { it.id }
            SongSortOrder.ARTIST -> rawSongsInPlaylist.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${sortedSongs.size} songs", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable { manager.setPersistentPlaylistInnerCardView(!manager.playlistInnerIsCardView) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (manager.playlistInnerIsCardView) "☰" else "⊞", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.setPersistentPlaylistInnerSort(SongSortOrder.A_TO_Z); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.setPersistentPlaylistInnerSort(SongSortOrder.Z_TO_A); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Duration") }, onClick = { manager.setPersistentPlaylistInnerSort(SongSortOrder.DURATION); showSortMenu = false })
                        DropdownMenuItem(text = { Text("File Size") }, onClick = { manager.setPersistentPlaylistInnerSort(SongSortOrder.FILE_SIZE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Newest First") }, onClick = { manager.setPersistentPlaylistInnerSort(SongSortOrder.NEWEST); showSortMenu = false })
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(onClick = { showAddSongsSearchPicker = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                    Text("+ Add", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(onClick = { manager.shufflePlaylist(playlist) }, colors = ButtonDefaults.buttonColors(containerColor = Color(playlist.iconColorHex)), shape = RoundedCornerShape(10.dp)) {
                    Text("🔀", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sortedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist is empty. Tap '+ Add' to search and add tracks.", color = Color(0xFF64748B))
            }
        } else {
            if (manager.playlistInnerIsCardView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedSongs, key = { it.id }) { song ->
                        UniversalSongCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sortedSongs, key = { it.id }) { song ->
                        UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                    }
                }
            }
        }
    }

    if (showAddSongsSearchPicker) {
        PlaylistAddSearchDialog(playlist = playlist, manager = manager, onDismiss = { showAddSongsSearchPicker = false }, onNavigateToFolder = { folder ->
            showAddSongsSearchPicker = false
            onFolderClick(folder)
        })
    }
}

@Composable
fun PlaylistAddSearchDialog(playlist: Playlist, manager: MusicManager, onDismiss: () -> Unit, onNavigateToFolder: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = manager.allSongs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    val folders = manager.allSongs.map { it.folderName }.distinct()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(600.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).padding(20.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Songs to ${playlist.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Search All", modifier = Modifier.padding(10.dp), fontSize = 13.sp) }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("By Folder", modifier = Modifier.padding(10.dp), fontSize = 13.sp) }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Playlists", modifier = Modifier.padding(10.dp), fontSize = 13.sp) }
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
                                        if (isAdded) manager.removeSongFromPlaylist(s.id, playlist) else manager.addSongToPlaylist(s.id, playlist)
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
                                    Text("${pl.name} (${pl.songIds.size} songs)", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

// Symmetrical Top Alignment
@Composable
fun FilteredSongsScreen(title: String, songs: List<Song>, manager: MusicManager, isDark: Boolean, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
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

// Symmetrical Top Alignment + Inside Sort & View Switcher (Cards vs Lines) for Library Folders
@Composable
fun FolderSongsScreen(folderName: String, manager: MusicManager, isDark: Boolean, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
    val rawSongs = manager.allSongs.filter { it.folderName == folderName }
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedSongs = remember(rawSongs, manager.folderInnerSortOrder) {
        when (manager.folderInnerSortOrder) {
            SongSortOrder.A_TO_Z -> rawSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> rawSongs.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> rawSongs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> rawSongs.sortedByDescending { it.size }
            SongSortOrder.NEWEST -> rawSongs.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> rawSongs.sortedBy { it.id }
            SongSortOrder.ARTIST -> rawSongs.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(folderName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${sortedSongs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // View Mode Switcher (Cards vs Lines)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable { manager.setPersistentFolderInnerCardView(!manager.folderInnerIsCardView) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (manager.folderInnerIsCardView) "☰" else "⊞", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.setPersistentFolderInnerSort(SongSortOrder.A_TO_Z); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.setPersistentFolderInnerSort(SongSortOrder.Z_TO_A); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Duration") }, onClick = { manager.setPersistentFolderInnerSort(SongSortOrder.DURATION); showSortMenu = false })
                        DropdownMenuItem(text = { Text("File Size") }, onClick = { manager.setPersistentFolderInnerSort(SongSortOrder.FILE_SIZE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Newest First") }, onClick = { manager.setPersistentFolderInnerSort(SongSortOrder.NEWEST); showSortMenu = false })
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        val shuffled = sortedSongs.shuffled()
                        if (shuffled.isNotEmpty()) manager.playSong(shuffled.first(), shuffled, folderName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔀", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sortedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Folder is empty.", color = Color(0xFF64748B))
            }
        } else {
            if (manager.folderInnerIsCardView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedSongs, key = { it.id }) { song ->
                        UniversalSongCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sortedSongs, key = { it.id }) { song ->
                        UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                    }
                }
            }
        }
    }
}

// Full-Width Action Sheet (Zero Dimming on Outside Click)
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
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${song.title} - ${if (song.artist.isNotBlank()) song.artist else "Unknown"}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                ModalActionRow("▶≡", "Play next", textColor) { onPlayNext() }
                ModalActionRow("≡+", "Add to playlist", textColor) { onAddToPlaylist() }
                ModalActionRow("◎", "Go to album", textColor) { onGoToAlbum() }
                ModalActionRow("👤", "Go to artist", textColor) { onGoToArtist() }
                ModalActionRow("🔗", "Share", textColor) { onShare() }
                ModalActionRow("🏷️", "Tag editor", textColor) { onTagEditor() }
                ModalActionRow("ℹ️", "Details", textColor) { onDetails() }
                ModalActionRow("🔔", "Set as ringtone", textColor) { onSetRingtone() }
                ModalActionRow("🗑️", "Delete from device", Color(0xFFEF4444)) { onDelete() }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModalActionRow(icon: String, title: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp, color = color, modifier = Modifier.width(30.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun SongInfoDialog(song: Song, isDark: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(22.dp)) {
            Column {
                Text("Details", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Title: ${song.title}", color = textColor, fontSize = 14.sp)
                Text("Artist: ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", color = Color(0xFF64748B), fontSize = 13.sp)
                Text("Album: ${song.album}", color = Color(0xFF64748B), fontSize = 13.sp)
                Text("Size: ${formatFileSize(song.size)}", color = Color(0xFF64748B), fontSize = 13.sp)
                Text("Duration: ${formatTime(song.duration)}", color = Color(0xFF64748B), fontSize = 13.sp)
                Text("Path: ${song.path}", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(manager: MusicManager, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val folders = manager.allSongs.map { it.folderName }.distinct()
    var selectedFolderToPin by remember { mutableStateOf<String?>(null) }
    val isDark = manager.isDarkMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(24.dp)) {
            Column {
                Text("Create New Playlist", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                Text("Or Pin an Entire Device Folder:", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders, key = { it }) { folder ->
                        val isSel = selectedFolderToPin == folder
                        val fColor = manager.getFolderColor(folder)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) manager.accentColor else if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    selectedFolderToPin = folder
                                    name = folder
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassmorphicFolderIcon(folderColor = fColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(folder, color = if (isSel) Color.White else if (isDark) Color.White else Color(0xFF0F172A), fontSize = 12.sp)
                            }
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor)
                ) {
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 4-Tab Bottom Navigation Bar: Home, Library, Artists, Search
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
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("home", "Home", "🏠"),
            Triple("library", "Library", "📚"),
            Triple("artists", "Artists", "🎙️"),
            Triple("search", "Search", "🔍")
        ).forEach { (key, label, icon) ->
            val isSel = activeTab == key
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(key) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(icon, fontSize = 20.sp)
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isSel) accent else Color(0xFF64748B),
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
