package com.melovish.player

import android.Manifest
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    val manager: MusicManager = MusicManager.activeInstance ?: MusicManager(application.applicationContext)
}

@UnstableApi
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MusicViewModel>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            viewModel.manager.scanStorage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val serviceIntent = Intent(this, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (_: Exception) {}

        requestRequiredPermissions()

        setContent {
            MelovishRootApp(viewModel.manager)
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            viewModel.manager.scanStorage()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            coil.Coil.imageLoader(this).memoryCache?.clear()
            System.gc()
        }
    }
}

@UnstableApi
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
    var previousActiveScreen by remember { mutableStateOf("home") }

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

    BackHandler(enabled = isSettingsEqOpen || isPlayerExpanded || activeScreen == "settings" || activeScreen == "profile" || selectedArtist != null || selectedAlbum != null || selectedPlaylist != null || selectedFolder != null || activeScreen != "home") {
        when {
            isSettingsEqOpen -> isSettingsEqOpen = false
            isPlayerExpanded -> isPlayerExpanded = false
            activeScreen == "settings" -> activeScreen = previousActiveScreen
            activeScreen == "profile" -> activeScreen = previousActiveScreen
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
                    onProfileClick = {
                        previousActiveScreen = activeScreen
                        activeScreen = "profile"
                    },
                    onSettingsClick = {
                        previousActiveScreen = activeScreen
                        activeScreen = "settings"
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        activeScreen == "settings" -> SettingsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = previousActiveScreen },
                            onOpenProfile = {
                                previousActiveScreen = activeScreen
                                activeScreen = "profile"
                            },
                            onOpenEqualizer = { isSettingsEqOpen = true }
                        )
                        activeScreen == "profile" -> ProfileScreen(
                            manager = manager,
                            onBackClick = { activeScreen = previousActiveScreen }
                        )
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
                                songs = manager.allSongs.filter { it.album.equals(selectedAlbum, ignoreCase = true) }.toImmutableList(),
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
                        val matchingArtist = manager.parsedArtistsList.find { it.name.equals(s.artist, ignoreCase = true) }
                            ?: ArtistItem(name = s.artist, songs = listOf(s).toImmutableList())
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
fun shimmerBrush(isDark: Boolean): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerFloat"
    )
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1E293B).copy(alpha = 0.6f),
            Color(0xFF334155).copy(alpha = 0.25f),
            Color(0xFF1E293B).copy(alpha = 0.6f)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0).copy(alpha = 0.6f),
            Color(0xFFF8FAFC).copy(alpha = 0.85f),
            Color(0xFFE2E8F0).copy(alpha = 0.6f)
        )
    }
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun ShimmerSkeletonRow(isDark: Boolean) {
    val brush = shimmerBrush(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF131B2E) else Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(brush))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.65f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(0.40f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(36.dp, 12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
    }
}

// 1:1 Standard Square Card with Full Information
@UnstableApi
@Composable
fun SquareAlbumOverlayCard(
    song: Song,
    manager: MusicManager,
    isDark: Boolean,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isPlayingThis = manager.currentSong?.id == song.id
    val accent = manager.accentColor
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isPlayingThis) 2.dp else 1.dp,
                color = if (isPlayingThis) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onPlay() }
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x88000000), Color(0xDE000000))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = if (isPlayingThis) accent else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (song.artist.isNotBlank()) song.artist else "Unknown",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(modifier = Modifier.clickable { onMenuClick() }.padding(start = 4.dp)) {
                    Text("⋮", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 5th View Mode: Hero Album Card (Strict 1:1 Aspect Ratio, Song Title Only, Zero Metadata)
@UnstableApi
@Composable
fun HeroAlbumCard(
    song: Song,
    manager: MusicManager,
    isDark: Boolean,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isPlayingThis = manager.currentSong?.id == song.id
    val accent = manager.accentColor
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isPlayingThis) 2.5.dp else 1.dp,
                color = if (isPlayingThis) accent else if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onPlay() }
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
                Text("🎵", fontSize = 48.sp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xA6000000), Color(0xF2000000))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = song.title,
                    color = if (isPlayingThis) accent else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.clickable { onMenuClick() }.padding(start = 6.dp)) {
                    Text("⋮", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// 5-Mode Vector Switcher Icon
@Composable
fun GridViewModeVectorIcon(mode: GridViewMode, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = 2.dp.toPx()
        when (mode) {
            GridViewMode.LIST -> {
                drawLine(tint, Offset(0f, h * 0.20f), Offset(w, h * 0.20f), s)
                drawLine(tint, Offset(0f, h * 0.50f), Offset(w, h * 0.50f), s)
                drawLine(tint, Offset(0f, h * 0.80f), Offset(w, h * 0.80f), s)
            }
            GridViewMode.GRID_2 -> {
                drawRoundRect(tint, Offset(0f, 0f), Size(w * 0.44f, h * 0.44f), CornerRadius(4f, 4f))
                drawRoundRect(tint, Offset(w * 0.56f, 0f), Size(w * 0.44f, h * 0.44f), CornerRadius(4f, 4f))
                drawRoundRect(tint, Offset(0f, h * 0.56f), Size(w * 0.44f, h * 0.44f), CornerRadius(4f, 4f))
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.56f), Size(w * 0.44f, h * 0.44f), CornerRadius(4f, 4f))
            }
            GridViewMode.GRID_3 -> {
                val step = w / 3.4f
                val radius = 2.5f.dp.toPx()
                for (r in 0..2) {
                    for (c in 0..2) {
                        drawCircle(tint, radius, Offset(c * step + step * 0.5f, r * step + step * 0.5f))
                    }
                }
            }
            GridViewMode.GRID_4 -> {
                val step = w / 4.4f
                val radius = 1.8f.dp.toPx()
                for (r in 0..3) {
                    for (c in 0..3) {
                        drawCircle(tint, radius, Offset(c * step + step * 0.5f, r * step + step * 0.5f))
                    }
                }
            }
            GridViewMode.HERO_GRID -> {
                drawRoundRect(tint, Offset(0f, 0f), Size(w * 0.46f, h), CornerRadius(5f, 5f))
                drawRoundRect(tint, Offset(w * 0.54f, 0f), Size(w * 0.46f, h), CornerRadius(5f, 5f))
            }
        }
    }
}

// 5-Mode Grid Size Dialog
@Composable
fun GridSizeDialog(
    currentMode: GridViewMode,
    isDark: Boolean,
    accent: Color,
    onSelectMode: (GridViewMode) -> Unit,
    onDismiss: () -> Unit
) {
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
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grid Size", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))

                listOf(
                    Pair(GridViewMode.LIST, "Standard List View"),
                    Pair(GridViewMode.GRID_2, "Big (2 Cards Grid)"),
                    Pair(GridViewMode.GRID_3, "Medium (3 Cards Grid)"),
                    Pair(GridViewMode.GRID_4, "Small (4 Cards Grid)"),
                    Pair(GridViewMode.HERO_GRID, "Hero Album Grid")
                ).forEach { (mode, name) ->
                    val isSel = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) accent.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GridViewModeVectorIcon(mode = mode, tint = if (isSel) accent else textColor, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(name, color = if (isSel) accent else textColor, fontSize = 14.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                        }
                        if (isSel) {
                            Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
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
fun ManagePlaylistsDialog(
    manager: MusicManager,
    isDark: Boolean,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

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
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Manage Playlists", color = textColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Add, remove or reorder favourite items", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onAddNew()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = manager.customPlaylists,
                        key = { _, pl -> pl.id }
                    ) { index, pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                GlassmorphicFolderIcon(folderColor = Color(pl.iconColorHex), modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${pl.songIds.size} songs", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1AEF4444))
                                        .clickable { manager.removePlaylist(pl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🗑️", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrangePlaylistsDialog(
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    val tempList = remember { manager.customPlaylists.toMutableList() }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 64.dp.toPx() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffsetPx by remember { mutableFloatStateOf(0f) }

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
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Arrange Playlists", color = textColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Hold & drag items to arrange sequence", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            manager.reorderCustomPlaylists(tempList)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = tempList,
                        key = { _, pl -> pl.id }
                    ) { index, pl ->
                        val isDragging = draggingIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(spring(stiffness = 550f, dampingRatio = 0.85f))
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = draggingOffsetPx
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDragging) accent.copy(alpha = 0.2f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                GlassmorphicFolderIcon(folderColor = Color(pl.iconColorHex), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .pointerInput(pl.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = index
                                                draggingOffsetPx = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                draggingOffsetPx += dragAmount.y
                                                val curIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                val threshold = itemHeightPx * 0.65f

                                                if (draggingOffsetPx > threshold && curIdx < tempList.size - 1) {
                                                    val item = tempList.removeAt(curIdx)
                                                    tempList.add(curIdx + 1, item)
                                                    draggingIndex = curIdx + 1
                                                    draggingOffsetPx -= itemHeightPx
                                                } else if (draggingOffsetPx < -threshold && curIdx > 0) {
                                                    val item = tempList.removeAt(curIdx)
                                                    tempList.add(curIdx - 1, item)
                                                    draggingIndex = curIdx - 1
                                                    draggingOffsetPx += itemHeightPx
                                                }
                                            },
                                            onDragEnd = {
                                                draggingIndex = null
                                                draggingOffsetPx = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                draggingOffsetPx = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ReorderDragHandle(tint = if (isDragging) accent else Color(0xFF64748B), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        manager.reorderCustomPlaylists(tempList)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Order", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FavouritePlaylistLongPressDialog(
    playlist: Playlist,
    manager: MusicManager,
    isDark: Boolean,
    onOpenRainbowPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val preset9Colors = remember {
        listOf(
            Color(0xFFF59E0B), Color(0xFF00B4D8), Color(0xFF10B981), Color(0xFF39FF14), Color(0xFFFF2A85),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFFF6B35)
        )
    }

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
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = playlist.name,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicFolderIcon(folderColor = Color(playlist.iconColorHex), modifier = Modifier.size(52.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        preset9Colors.take(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (Color(playlist.iconColorHex) == color) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        manager.updatePlaylistColorOnly(playlist, color.toArgb().toLong())
                                        onDismiss()
                                    }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        preset9Colors.drop(5).take(4).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (Color(playlist.iconColorHex) == color) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        manager.updatePlaylistColorOnly(playlist, color.toArgb().toLong())
                                        onDismiss()
                                    }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    onOpenRainbowPicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        manager.removePlaylist(playlist)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22EF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("🗑️ Remove from Favourites", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
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
    var showManagePlaylistsDialog by remember { mutableStateOf(false) }
    var showArrangePlaylistsDialog by remember { mutableStateOf(false) }
    var longPressPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showRainbowWheelForPl by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }

    val sortedSongs: ImmutableList<Song> = remember(manager.allSongs.toList(), manager.currentSortOrder) {
        manager.getSortedSongs().toImmutableList()
    }
    val recents: ImmutableList<Song> = remember(manager.historySongs.size, manager.historySongs.toList()) {
        manager.historySongs.take(30).toImmutableList()
    }

    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp - 32 - (3 * 8)) / 4).coerceAtLeast(76).dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "home_recents_section", contentType = "recents_carousel") {
                Text(text = "Recently Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(10.dp))

                if (manager.isScanningStorage && manager.allSongs.isEmpty()) {
                    val brush = shimmerBrush(isDark)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(3) {
                            Box(
                                modifier = Modifier
                                    .width(116.dp)
                                    .height(116.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(brush)
                            )
                        }
                    }
                } else if (recents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)), contentAlignment = Alignment.Center) {
                        Text("No recently played tracks yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = recents,
                            key = { it.id },
                            contentType = { "recent_song_card" }
                        ) { song ->
                            RecentlyPlayedCard(
                                song = song,
                                manager = manager,
                                onClick = { manager.playSong(song, manager.historySongs, "Recently Played") }
                            )
                        }
                    }
                }
            }

            item(key = "home_playlists_section", contentType = "playlists_carousel") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favourite Playlists", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showArrangePlaylistsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Arrange", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showManagePlaylistsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Manage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (manager.customPlaylists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)), contentAlignment = Alignment.Center) {
                        Text("No playlists yet. Tap 'Manage' to add.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            items = manager.customPlaylists,
                            key = { _, pl -> pl.id },
                            contentType = { _, _ -> "favourite_playlist_card" }
                        ) { _, pl ->
                            Box(
                                modifier = Modifier
                                    .size(cardWidth)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onPlaylistClick(pl) },
                                        onLongClick = { longPressPlaylist = pl }
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

            item(key = "home_all_songs_header", contentType = "all_songs_header") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("All Songs", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("${manager.allSongs.size} Songs", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                .combinedClickable(
                                    onClick = { manager.cycleNextHomeViewMode() },
                                    onLongClick = { showGridSizeDialog = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GridViewModeVectorIcon(mode = manager.homeViewMode, tint = textColor, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))
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

            if (manager.isScanningStorage && manager.allSongs.isEmpty()) {
                items(6) {
                    ShimmerSkeletonRow(isDark = isDark)
                }
            } else {
                when (manager.homeViewMode) {
                    GridViewMode.LIST -> {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "home_song_row" }
                        ) { song ->
                            UniversalSongRow(
                                song = song,
                                manager = manager,
                                isDark = isDark,
                                onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                                onMenuClick = { onSongMenuClick(song) }
                            )
                        }
                    }
                    GridViewMode.GRID_2 -> {
                        items(
                            items = sortedSongs.chunked(2),
                            key = { it.first().id }
                        ) { rowSongs ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowSongs.forEach { song ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SquareAlbumOverlayCard(
                                            song = song,
                                            manager = manager,
                                            isDark = isDark,
                                            onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                                            onMenuClick = { onSongMenuClick(song) }
                                        )
                                    }
                                }
                                if (rowSongs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    GridViewMode.GRID_3 -> {
                        items(
                            items = sortedSongs.chunked(3),
                            key = { it.first().id }
                        ) { rowSongs ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowSongs.forEach { song ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SquareAlbumOverlayCard(
                                            song = song,
                                            manager = manager,
                                            isDark = isDark,
                                            onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                                            onMenuClick = { onSongMenuClick(song) }
                                        )
                                    }
                                }
                                for (i in rowSongs.size until 3) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    GridViewMode.GRID_4 -> {
                        items(
                            items = sortedSongs.chunked(4),
                            key = { it.first().id }
                        ) { rowSongs ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowSongs.forEach { song ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SquareAlbumOverlayCard(
                                            song = song,
                                            manager = manager,
                                            isDark = isDark,
                                            onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                                            onMenuClick = { onSongMenuClick(song) }
                                        )
                                    }
                                }
                                for (i in rowSongs.size until 4) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    GridViewMode.HERO_GRID -> {
                        items(
                            items = sortedSongs.chunked(2),
                            key = { it.first().id }
                        ) { rowSongs ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowSongs.forEach { song ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        HeroAlbumCard(
                                            song = song,
                                            manager = manager,
                                            isDark = isDark,
                                            onPlay = { manager.playSong(song, sortedSongs, "All Songs") },
                                            onMenuClick = { onSongMenuClick(song) }
                                        )
                                    }
                                }
                                if (rowSongs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
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
    if (showManagePlaylistsDialog) ManagePlaylistsDialog(manager = manager, isDark = isDark, onAddNew = { showCreatePlaylistDialog = true }, onDismiss = { showManagePlaylistsDialog = false })
    if (showArrangePlaylistsDialog) ArrangePlaylistsDialog(manager = manager, isDark = isDark, onDismiss = { showArrangePlaylistsDialog = false })
    if (showGridSizeDialog) GridSizeDialog(
        currentMode = manager.homeViewMode,
        isDark = isDark,
        accent = manager.accentColor,
        onSelectMode = { manager.updateHomeViewMode(it) },
        onDismiss = { showGridSizeDialog = false }
    )

    if (longPressPlaylist != null && !showRainbowWheelForPl) {
        val pl = longPressPlaylist!!
        FavouritePlaylistLongPressDialog(
            playlist = pl,
            manager = manager,
            isDark = isDark,
            onOpenRainbowPicker = {
                showRainbowWheelForPl = true
            },
            onDismiss = { longPressPlaylist = null }
        )
    }

    if (showRainbowWheelForPl && longPressPlaylist != null) {
        val pl = longPressPlaylist!!
        FolderColourPickerDialog(
            title = "Colour Picker",
            onColorSelected = { newColor ->
                manager.updatePlaylistColorOnly(pl, newColor.toArgb().toLong())
                showRainbowWheelForPl = false
                longPressPlaylist = null
            },
            onDismiss = {
                showRainbowWheelForPl = false
                longPressPlaylist = null
            }
        )
    }
}

@Composable
fun FolderColourPickerDialog(
    title: String = "Colour Picker",
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    val currentColor = remember(hue, sat, value) { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("✕", fontSize = 18.sp, color = Color(0xFF64748B), modifier = Modifier.clickable { onDismiss() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change: PointerInputChange, _ ->
                                    val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                                    val touch = change.position
                                    val dx = (touch.x - center.x).toDouble()
                                    val dy = (touch.y - center.y).toDouble()
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val radius = (size.width.toFloat() / 2f).toDouble()
                                    if (dist >= radius * 0.65) {
                                        var angle = Math.toDegrees(atan2(dy, dx)).toFloat()
                                        if (angle < 0f) angle += 360f
                                        hue = angle
                                    } else {
                                        val halfInner = (radius * 0.55).toFloat()
                                        val normX = ((touch.x - (center.x - halfInner)) / (halfInner * 2f)).coerceIn(0f, 1f)
                                        val normY = ((touch.y - (center.y - halfInner)) / (halfInner * 2f)).coerceIn(0f, 1f)
                                        sat = normX
                                        value = 1f - normY
                                    }
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f
                        val ringThickness = radius * 0.28f
                        val sweepColors = (0..360 step 30).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
                        drawCircle(brush = Brush.sweepGradient(sweepColors, center), radius = radius - (ringThickness / 2f), style = Stroke(width = ringThickness))

                        val thumbRad = Math.toRadians(hue.toDouble())
                        val thumbDist = (radius - (ringThickness / 2f)).toDouble()
                        val thumbPos = Offset((center.x.toDouble() + (thumbDist * cos(thumbRad))).toFloat(), (center.y.toDouble() + (thumbDist * sin(thumbRad))).toFloat())
                        drawCircle(Color.White, radius = 12.dp.toPx(), center = thumbPos, style = Stroke(3.dp.toPx()))
                        drawCircle(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), radius = 9.dp.toPx(), center = thumbPos)

                        val halfBox = (radius * 0.55f)
                        val boxTopLeft = Offset(center.x - halfBox, center.y - halfBox)
                        val boxSize = Size(halfBox * 2f, halfBox * 2f)
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))), topLeft = boxTopLeft, size = boxSize)
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), topLeft = boxTopLeft, size = boxSize)
                        val targetPos = Offset(boxTopLeft.x + (sat * boxSize.width), boxTopLeft.y + ((1f - value) * boxSize.height))
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = targetPos, style = Stroke(2.5f.dp.toPx()))
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        onColorSelected(currentColor)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 1:1 Perfect Square Folder Card for Library Grid Modes
@Composable
fun LibraryFolderSquareCard(
    folderName: String,
    totalSize: Long,
    songCount: Int,
    folderColor: Color,
    isDark: Boolean,
    cardBg: Color,
    isHero: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            GlassmorphicFolderIcon(
                folderColor = folderColor,
                modifier = Modifier.size(if (isHero) 64.dp else 46.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folderName,
                color = textColor,
                fontSize = if (isHero) 15.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (!isHero) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${songCount} songs • ${formatFileSize(totalSize)}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Library Screen: Independent View Mode (5 modes) & Independent Sort Order with 1:1 Perfect Square Cards
@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun LibraryScreen(manager: MusicManager, listState: LazyListState, onFolderClick: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var showFolderSortMenu by remember { mutableStateOf(false) }
    var customizingFolder by remember { mutableStateOf<String?>(null) }
    var showRainbowWheelForFolder by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }

    val sortedFolders: ImmutableList<String> = remember(manager.allSongs.size, manager.currentFolderSortOrder) {
        manager.getSortedFolders().toImmutableList()
    }
    val folderMap = remember(manager.allSongs.size) { manager.allSongs.groupBy { it.folderName } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Folders & Storage", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Section-Specific 5-Mode View Switcher with independent memory
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .combinedClickable(
                            onClick = { manager.cycleNextLibraryFoldersViewMode() },
                            onLongClick = { showGridSizeDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GridViewModeVectorIcon(mode = manager.libraryFoldersViewMode, tint = textColor, modifier = Modifier.size(16.dp))
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

        when (manager.libraryFoldersViewMode) {
            GridViewMode.LIST -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = sortedFolders,
                        key = { it },
                        contentType = { "folder_list_row" }
                    ) { folderName ->
                        val songs = folderMap[folderName] ?: emptyList()
                        val totalSize = remember(songs) { songs.sumOf { it.size } }
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
            GridViewMode.GRID_2 -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedFolders,
                        key = { it },
                        contentType = { "folder_grid_card_2" }
                    ) { folderName ->
                        val songs = folderMap[folderName] ?: emptyList()
                        val totalSize = remember(songs) { songs.sumOf { it.size } }
                        val fColor = manager.getFolderColor(folderName)

                        LibraryFolderSquareCard(
                            folderName = folderName,
                            totalSize = totalSize,
                            songCount = songs.size,
                            folderColor = fColor,
                            isDark = isDark,
                            cardBg = cardBg,
                            isHero = false,
                            onClick = { onFolderClick(folderName) },
                            onLongClick = { customizingFolder = folderName }
                        )
                    }
                }
            }
            GridViewMode.GRID_3 -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedFolders,
                        key = { it },
                        contentType = { "folder_grid_card_3" }
                    ) { folderName ->
                        val songs = folderMap[folderName] ?: emptyList()
                        val totalSize = remember(songs) { songs.sumOf { it.size } }
                        val fColor = manager.getFolderColor(folderName)

                        LibraryFolderSquareCard(
                            folderName = folderName,
                            totalSize = totalSize,
                            songCount = songs.size,
                            folderColor = fColor,
                            isDark = isDark,
                            cardBg = cardBg,
                            isHero = false,
                            onClick = { onFolderClick(folderName) },
                            onLongClick = { customizingFolder = folderName }
                        )
                    }
                }
            }
            GridViewMode.GRID_4 -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedFolders,
                        key = { it },
                        contentType = { "folder_grid_card_4" }
                    ) { folderName ->
                        val songs = folderMap[folderName] ?: emptyList()
                        val totalSize = remember(songs) { songs.sumOf { it.size } }
                        val fColor = manager.getFolderColor(folderName)

                        LibraryFolderSquareCard(
                            folderName = folderName,
                            totalSize = totalSize,
                            songCount = songs.size,
                            folderColor = fColor,
                            isDark = isDark,
                            cardBg = cardBg,
                            isHero = false,
                            onClick = { onFolderClick(folderName) },
                            onLongClick = { customizingFolder = folderName }
                        )
                    }
                }
            }
            GridViewMode.HERO_GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedFolders,
                        key = { it },
                        contentType = { "folder_grid_hero" }
                    ) { folderName ->
                        val songs = folderMap[folderName] ?: emptyList()
                        val totalSize = remember(songs) { songs.sumOf { it.size } }
                        val fColor = manager.getFolderColor(folderName)

                        LibraryFolderSquareCard(
                            folderName = folderName,
                            totalSize = totalSize,
                            songCount = songs.size,
                            folderColor = fColor,
                            isDark = isDark,
                            cardBg = cardBg,
                            isHero = true,
                            onClick = { onFolderClick(folderName) },
                            onLongClick = { customizingFolder = folderName }
                        )
                    }
                }
            }
        }
    }

    if (showGridSizeDialog) {
        GridSizeDialog(
            currentMode = manager.libraryFoldersViewMode,
            isDark = isDark,
            accent = manager.accentColor,
            onSelectMode = { manager.updateLibraryFoldersViewMode(it) },
            onDismiss = { showGridSizeDialog = false }
        )
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

    if (showRainbowWheelForFolder && customizingFolder != null) {
        val folder = customizingFolder!!
        FolderColourPickerDialog(
            title = "Colour Picker",
            onColorSelected = { newColor ->
                manager.updateFolderColorOnly(folder, newColor.toArgb().toLong())
                showRainbowWheelForFolder = false
            },
            onDismiss = { showRainbowWheelForFolder = false }
        )
    }
}

@UnstableApi
@Composable
fun SearchScreen(manager: MusicManager, listState: LazyListState, onSongMenuClick: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val filtered: ImmutableList<Song> = remember(query, manager.allSongs.size) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList<Song>().toImmutableList()
        } else {
            manager.allSongs.filter {
                it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.folderName.contains(q, ignoreCase = true)
            }.toImmutableList()
        }
    }

    val isDark = manager.isDarkMode

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, or folders...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(
                items = filtered,
                key = { it.id },
                contentType = { "search_result_row" }
            ) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, filtered, "Search Results") }, onMenuClick = { onSongMenuClick(song) })
            }
        }
    }
}

// Playlist Detail Screen: Independent View Mode (5 modes) & Independent Sort Order with 1:1 Perfect Squares
@UnstableApi
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
    var showGridSizeDialog by remember { mutableStateOf(false) }

    val songLookup = remember(manager.allSongs.size) { manager.allSongs.associateBy { it.id } }
    val rawSongsInPlaylist = remember(playlist.songIds, songLookup) {
        playlist.songIds.mapNotNull { songLookup[it] }
    }

    val sortedSongs: ImmutableList<Song> = remember(rawSongsInPlaylist, manager.playlistInnerSortOrder) {
        when (manager.playlistInnerSortOrder) {
            SongSortOrder.A_TO_Z -> rawSongsInPlaylist.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> rawSongsInPlaylist.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> rawSongsInPlaylist.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> rawSongsInPlaylist.sortedByDescending { it.size }
            SongSortOrder.NEWEST -> rawSongsInPlaylist.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> rawSongsInPlaylist.sortedBy { it.id }
            SongSortOrder.ARTIST -> rawSongsInPlaylist.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        }.toImmutableList()
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
                // Section-Specific 5-Mode Grid Size Switcher with independent memory
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .combinedClickable(
                            onClick = { manager.cycleNextPlaylistInnerViewMode() },
                            onLongClick = { showGridSizeDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GridViewModeVectorIcon(mode = manager.playlistInnerViewMode, tint = textColor, modifier = Modifier.size(16.dp))
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
            when (manager.playlistInnerViewMode) {
                GridViewMode.LIST -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "playlist_song_row" }
                        ) { song ->
                            UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_2 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "playlist_song_card_2" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_3 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "playlist_song_card_3" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_4 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "playlist_song_card_4" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.HERO_GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "playlist_song_card_hero" }
                        ) { song ->
                            HeroAlbumCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, playlist.name) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
            }
        }
    }

    if (showGridSizeDialog) {
        GridSizeDialog(
            currentMode = manager.playlistInnerViewMode,
            isDark = isDark,
            accent = manager.accentColor,
            onSelectMode = { manager.updatePlaylistInnerViewMode(it) },
            onDismiss = { showGridSizeDialog = false }
        )
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

    val filteredSongs: ImmutableList<Song> = remember(searchQuery, manager.allSongs.size) {
        val q = searchQuery.trim()
        if (q.isEmpty()) manager.allSongs.toImmutableList()
        else manager.allSongs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }.toImmutableList()
    }
    val folders: ImmutableList<String> = remember(manager.allSongs.size) { manager.allSongs.map { it.folderName }.distinct().toImmutableList() }

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
                        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search songs or artists...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                items = filteredSongs,
                                key = { it.id },
                                contentType = { "dialog_search_row" }
                            ) { s ->
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
                            items(
                                items = folders,
                                key = { it },
                                contentType = { "dialog_folder_row" }
                            ) { folder ->
                                val count = remember(folder, manager.allSongs.size) { manager.allSongs.count { it.folderName == folder } }
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
                        val otherPlaylists: ImmutableList<Playlist> = remember(manager.customPlaylists.size) {
                            manager.customPlaylists.filter { it.id != playlist.id }.toImmutableList()
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                items = otherPlaylists,
                                key = { it.id },
                                contentType = { "dialog_copy_playlist_row" }
                            ) { pl ->
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

@UnstableApi
@Composable
fun FilteredSongsScreen(title: String, songs: ImmutableList<Song>, manager: MusicManager, isDark: Boolean, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
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
            items(
                items = songs,
                key = { it.id },
                contentType = { "filtered_song_row" }
            ) { song ->
                UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, songs, title) }, onMenuClick = { onSongMenuClick(song) })
            }
        }
    }
}

// Inside Folder Screen: Independent View Mode (5 modes) & Independent Sort Order with 1:1 Perfect Squares
@UnstableApi
@Composable
fun FolderSongsScreen(folderName: String, manager: MusicManager, isDark: Boolean, onBack: () -> Unit, onSongMenuClick: (Song) -> Unit) {
    val rawSongs = remember(folderName, manager.allSongs.size) { manager.allSongs.filter { it.folderName == folderName } }
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var showSortMenu by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }

    val sortedSongs: ImmutableList<Song> = remember(rawSongs, manager.folderInnerSortOrder) {
        when (manager.folderInnerSortOrder) {
            SongSortOrder.A_TO_Z -> rawSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> rawSongs.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> rawSongs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> rawSongs.sortedByDescending { it.size }
            SongSortOrder.NEWEST -> rawSongs.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> rawSongs.sortedBy { it.id }
            SongSortOrder.ARTIST -> rawSongs.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        }.toImmutableList()
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
                // Section-Specific 5-Mode Grid Size Switcher with independent memory
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .combinedClickable(
                            onClick = { manager.cycleNextFolderInnerViewMode() },
                            onLongClick = { showGridSizeDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GridViewModeVectorIcon(mode = manager.folderInnerViewMode, tint = textColor, modifier = Modifier.size(16.dp))
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
            when (manager.folderInnerViewMode) {
                GridViewMode.LIST -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "folder_inner_row" }
                        ) { song ->
                            UniversalSongRow(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_2 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "folder_inner_card_2" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_3 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "folder_inner_card_3" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.GRID_4 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "folder_inner_card_4" }
                        ) { song ->
                            SquareAlbumOverlayCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
                GridViewMode.HERO_GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = sortedSongs,
                            key = { it.id },
                            contentType = { "folder_inner_card_hero" }
                        ) { song ->
                            HeroAlbumCard(song = song, manager = manager, isDark = isDark, onPlay = { manager.playSong(song, sortedSongs, folderName) }, onMenuClick = { onSongMenuClick(song) })
                        }
                    }
                }
            }
        }
    }

    if (showGridSizeDialog) {
        GridSizeDialog(
            currentMode = manager.folderInnerViewMode,
            isDark = isDark,
            accent = manager.accentColor,
            onSelectMode = { manager.updateFolderInnerViewMode(it) },
            onDismiss = { showGridSizeDialog = false }
        )
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
    val folders: ImmutableList<String> = remember(manager.allSongs.size) { manager.allSongs.map { it.folderName }.distinct().toImmutableList() }
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Or Pin an Entire Device Folder:", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = folders,
                        key = { it },
                        contentType = { "dialog_folder_chip" }
                    ) { folder ->
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

@UnstableApi
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
        Spacer(
            modifier = Modifier
                .size(34.dp)
                .rotate(rotation)
                .drawWithCache {
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

                            val p1 = Offset((center.x.toDouble() + (innerRingRadius * cos(a1.toDouble()))).toFloat(), (center.y.toDouble() + (innerRingRadius * sin(a1.toDouble()))).toFloat())
                            val p2 = Offset((center.x.toDouble() + (outerRadius * cos(a2.toDouble()))).toFloat(), (center.y.toDouble() + (outerRadius * sin(a2.toDouble()))).toFloat())
                            val p3 = Offset((center.x.toDouble() + (outerRadius * cos(a3.toDouble()))).toFloat(), (center.y.toDouble() + (outerRadius * sin(a3.toDouble()))).toFloat())
                            val p4 = Offset((center.x.toDouble() + (innerRingRadius * cos(a4.toDouble()))).toFloat(), (center.y.toDouble() + (innerRingRadius * sin(a4.toDouble()))).toFloat())

                            if (i == 0) moveTo(p1.x, p1.y) else lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            lineTo(p4.x, p4.y)
                        }
                        close()
                    }

                    val cavityRadius = innerRingRadius * 0.72f
                    val hubRadius = innerRingRadius * 0.32f
                    val spokeStrokeWidth = 2.5f.dp.toPx()
                    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)

                    onDrawBehind {
                        drawPath(path, gearColor)
                        drawCircle(color = bgColor, radius = cavityRadius, center = center)
                        drawCircle(color = gearColor, radius = hubRadius, center = center)
                        for (s in 0 until 3) {
                            val spAngle = (s * 2.0 * Math.PI / 3.0).toFloat()
                            val spokeEnd = Offset((center.x.toDouble() + (cavityRadius * cos(spAngle.toDouble()))).toFloat(), (center.y.toDouble() + (cavityRadius * sin(spAngle.toDouble()))).toFloat())
                            drawLine(color = gearColor, start = center, end = spokeEnd, strokeWidth = spokeStrokeWidth)
                        }
                    }
                }
        )
    }
}

@Composable
fun TopBar(manager: MusicManager, onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val avatarPath = manager.profileImagePath

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
            if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
                AsyncImage(
                    model = File(avatarPath),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DefaultProfileAvatar(modifier = Modifier.fillMaxSize())
            }
        }

        Text(text = "Melovish", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

        LiveMechanicalGearIcon(isDark = isDark, modifier = Modifier.clickable { onSettingsClick() })
    }
}

@UnstableApi
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
                Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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

@UnstableApi
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
            .aspectRatio(1f)
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
