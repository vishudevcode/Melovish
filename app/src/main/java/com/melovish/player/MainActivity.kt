package com.melovish.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale

@UnstableApi
class MainActivity : ComponentActivity() {

    private lateinit var musicManager: MusicManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            musicManager.scanStorage()
        } else {
            Toast.makeText(this, "Permission required to access media library", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicManager = MusicManager(this)

        checkPermissions()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (musicManager.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> systemDark
            }
            musicManager.isDarkMode = effectiveDark

            MainAppScaffold(manager = musicManager)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            musicManager.scanStorage()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        musicManager.scanStorage()
    }
}

@UnstableApi
@Composable
fun MainAppScaffold(manager: MusicManager) {
    val isDark = manager.isDarkMode
    val bgColor = if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC)
    val accent = manager.accentColor

    var activeTab by remember { mutableStateOf("home") }
    var currentScreen by remember { mutableStateOf<String?>(null) } // "settings", "profile", "equalizer_fullscreen"

    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var isPlayerMaximized by remember { mutableStateOf(false) }

    // Prioritized 1-Step Back Navigation Hierarchy
    BackHandler(enabled = true) {
        when {
            isPlayerMaximized -> {
                isPlayerMaximized = false
            }
            currentScreen != null -> {
                currentScreen = null
            }
            selectedPlaylist != null -> {
                selectedPlaylist = null
            }
            selectedArtist != null -> {
                selectedArtist = null
            }
            selectedFolder != null -> {
                selectedFolder = null
            }
            activeTab != "home" -> {
                activeTab = "home"
            }
            else -> {
                (manager.player.applicationLooper.thread.contextClassLoader as? ComponentActivity)?.finish()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
        bottomBar = {
            Column {
                if (!isPlayerMaximized && manager.currentSong != null) {
                    MiniPlayerDock(
                        manager = manager,
                        onClick = { isPlayerMaximized = true }
                    )
                }
                if (currentScreen == null) {
                    BottomNavBar(
                        manager = manager,
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Tab Views
            when (activeTab) {
                "home" -> HomeScreen(
                    manager = manager,
                    onOpenSettings = { currentScreen = "settings" },
                    onOpenProfile = { currentScreen = "profile" },
                    onOpenPlaylist = { pl -> selectedPlaylist = pl },
                    onOpenArtist = { art -> selectedArtist = art },
                    onOpenFolder = { f -> selectedFolder = f },
                    onExpandPlayer = { isPlayerMaximized = true }
                )
                "library" -> LibraryScreen(
                    manager = manager,
                    onOpenPlaylist = { pl -> selectedPlaylist = pl },
                    onOpenFolder = { f -> selectedFolder = f },
                    onOpenSettings = { currentScreen = "settings" }
                )
                "artists" -> ArtistsScreen(
                    manager = manager,
                    onOpenArtist = { art -> selectedArtist = art },
                    onOpenSettings = { currentScreen = "settings" }
                )
                "search" -> SearchScreen(
                    manager = manager,
                    onExpandPlayer = { isPlayerMaximized = true },
                    onOpenSettings = { currentScreen = "settings" }
                )
            }

            // Folder Detail View
            if (selectedFolder != null) {
                FolderDetailView(
                    manager = manager,
                    folderName = selectedFolder!!,
                    onBack = { selectedFolder = null },
                    onOpenSettings = { currentScreen = "settings" }
                )
            }

            // Artist Detail View
            if (selectedArtist != null) {
                ArtistDetailView(
                    manager = manager,
                    artist = selectedArtist!!,
                    onBack = { selectedArtist = null },
                    onOpenSettings = { currentScreen = "settings" }
                )
            }

            // Playlist Detail View
            if (selectedPlaylist != null) {
                PlaylistDetailView(
                    manager = manager,
                    playlist = selectedPlaylist!!,
                    onBack = { selectedPlaylist = null },
                    onOpenSettings = { currentScreen = "settings" }
                )
            }

            // Foreground Elevated Settings Overlay (Opens on foreground over anything clicked)
            if (currentScreen == "settings") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    SettingsScreen(
                        manager = manager,
                        onBackClick = { currentScreen = null },
                        onOpenProfile = { currentScreen = "profile" },
                        onOpenEqualizer = { currentScreen = "equalizer_fullscreen" }
                    )
                }
            }

            // Foreground Elevated Profile Edit Overlay
            if (currentScreen == "profile") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    ProfileEditScreen(
                        manager = manager,
                        onBack = { currentScreen = "settings" }
                    )
                }
            }

            // Full Player Screen (Elevated with Hardware Transform Animations)
            AnimatedVisibility(
                visible = isPlayerMaximized && manager.currentSong != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                )
            ) {
                FullPlayerSheet(
                    manager = manager,
                    onDismiss = { isPlayerMaximized = false }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. Home Screen
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun HomeScreen(
    manager: MusicManager,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenArtist: (ArtistItem) -> Unit,
    onOpenFolder: (String) -> Unit,
    onExpandPlayer: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    val avatarPath = manager.profileImagePath

    var activeSongFilter by remember { mutableStateOf("All Songs") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Multi-Selection State
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    // Context Menu Dialog State
    var contextMenuSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistModal by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header / Multi-Select Action Bar
            item(key = "home_header") {
                if (isMultiSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurvedBackArrowIcon(
                                tint = textColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        isMultiSelectMode = false
                                        selectedSongIds.clear()
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${selectedSongIds.size} Selected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Select All",
                                color = accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedSongIds.clear()
                                    selectedSongIds.addAll(manager.allSongs.map { it.id })
                                }
                            )
                            Text(
                                text = "Play",
                                color = accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    val selected = manager.allSongs.filter { it.id in selectedSongIds }
                                    if (selected.isNotEmpty()) {
                                        manager.playSong(selected.first(), selected, "Selected")
                                    }
                                    isMultiSelectMode = false
                                    selectedSongIds.clear()
                                }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF030712))
                                .border(2.dp, accent, CircleShape)
                                .clickable { onOpenProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
                                AsyncImage(
                                    model = File(avatarPath),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                DefaultProfileAvatar(modifier = Modifier.fillMaxSize())
                            }
                        }

                        Text(
                            text = "Melovish",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 20.sp)
                        }
                    }
                }
            }

            // Quick Playlists Carousel Row
            if (!isMultiSelectMode) {
                item(key = "quick_playlists") {
                    Column {
                        Text(
                            text = "Playlists",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(
                                items = manager.customPlaylists,
                                key = { it.id }
                            ) { playlist ->
                                QuickPlaylistCard(
                                    playlist = playlist,
                                    isDark = isDark,
                                    onClick = { onOpenPlaylist(playlist) }
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips & Sort Order Dropdown
            item(key = "filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf("All Songs", "Favorites", "Recent", "Most Played").forEach { filter ->
                            val isSel = activeSongFilter == filter
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) accent else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                        .clickable { activeSongFilter = filter }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable { showSortMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SongSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.replace("_", " ")) },
                                    onClick = {
                                        manager.setPersistentSongSort(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Active Songs List
            val displayedSongs = when (activeSongFilter) {
                "Favorites" -> manager.allSongs.filter { it.isFavorite }
                "Recent" -> manager.historySongs
                "Most Played" -> manager.getMostPlayedSongs()
                else -> manager.getSortedSongs()
            }

            itemsIndexed(
                items = displayedSongs,
                key = { _, song -> song.id },
                contentType = { _, _ -> "song_row" }
            ) { index, song ->
                val isSelected = song.id in selectedSongIds
                SongRowItem(
                    song = song,
                    isCurrent = song.id == manager.currentSong?.id,
                    isPlaying = manager.isPlaying && song.id == manager.currentSong?.id,
                    manager = manager,
                    isDark = isDark,
                    isSelected = isSelected,
                    isMultiSelectMode = isMultiSelectMode,
                    onClick = {
                        if (isMultiSelectMode) {
                            if (isSelected) selectedSongIds.remove(song.id) else selectedSongIds.add(song.id)
                        } else {
                            manager.playSong(song, displayedSongs, activeSongFilter)
                        }
                    },
                    onLongClick = {
                        if (!isMultiSelectMode) {
                            isMultiSelectMode = true
                            selectedSongIds.add(song.id)
                        }
                    },
                    onFavoriteToggle = {
                        manager.toggleFavorite(song)
                    },
                    onMoreOptionsClick = {
                        contextMenuSong = song
                    }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (manager.currentSong == null) {
                    manager.resumeLastPlayed()
                } else {
                    manager.togglePlayPause()
                }
            },
            containerColor = accent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape)
        ) {
            Text(
                text = if (manager.isPlaying) "❚❚" else "▶",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Song Context Menu Modal
    if (contextMenuSong != null) {
        val song = contextMenuSong!!
        SongContextMenuModal(
            song = song,
            manager = manager,
            isDark = isDark,
            onDismiss = { contextMenuSong = null },
            onAddToPlaylist = {
                showAddToPlaylistModal = song
                contextMenuSong = null
            },
            onDeleteSong = {
                showDeleteConfirmDialog = song
                contextMenuSong = null
            }
        )
    }

    // Add To Playlist Modal
    if (showAddToPlaylistModal != null) {
        AddToPlaylistDialog(
            manager = manager,
            song = showAddToPlaylistModal!!,
            onDismiss = { showAddToPlaylistModal = null }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        val s = showDeleteConfirmDialog!!
        ConfirmDeleteSongDialog(
            song = s,
            isDark = isDark,
            onConfirm = {
                manager.deleteSongFromDevice(s)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }
}

// -----------------------------------------------------------------------------------------
// 2. Library Screen
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun LibraryScreen(
    manager: MusicManager,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var newPlaylistDialog by remember { mutableStateOf(false) }

    var editingFolderColor by remember { mutableStateOf<String?>(null) }
    var showRainbowPicker by remember { mutableStateOf(false) }
    var showFolderSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Library", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { newPlaylistDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Playlist", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folders Header with Sorting Dropdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Music Folders", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)

                    Box {
                        Text(
                            text = "Sort ▾",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showFolderSortMenu = true }
                        )

                        DropdownMenu(
                            expanded = showFolderSortMenu,
                            onDismissRequest = { showFolderSortMenu = false }
                        ) {
                            FolderSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.replace("_", " ")) },
                                    onClick = {
                                        manager.setPersistentFolderSort(order)
                                        showFolderSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val folders = manager.getSortedFolders()
            items(folders, key = { it }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF131B2E) else Color.White)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .clickable { onOpenFolder(folder) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clickable { editingFolderColor = folder }) {
                        GlassmorphicFolderIcon(
                            folderColor = manager.getFolderColor(folder),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        val count = manager.allSongs.count { it.folderName == folder }
                        Text("$count songs", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("›", fontSize = 22.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }

            // Playlists Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Your Playlists", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(manager.customPlaylists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF131B2E) else Color.White)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .clickable { onOpenPlaylist(playlist) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicFolderIcon(
                        folderColor = Color(playlist.iconColorHex),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${playlist.songIds.size} tracks", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("›", fontSize = 22.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (newPlaylistDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { newPlaylistDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Column {
                    Text("Create Playlist", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = { Text("Playlist name...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { newPlaylistDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    manager.createPlaylist(playlistNameInput.trim())
                                    newPlaylistDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Folder Color Palette Dialog (Line 835 preserved)
    if (editingFolderColor != null) {
        val fName = editingFolderColor!!
        FolderColorDialog(
            folderName = fName,
            currentColor = manager.getFolderColor(fName),
            isDark = isDark,
            onColorSelected = { col ->
                manager.updateFolderColorOnly(fName, col.toArgb().toLong())
                editingFolderColor = null
            },
            onOpenRainbowPicker = {
                showRainbowPicker = true
            },
            onDismiss = { editingFolderColor = null }
        )
    }

    if (showRainbowPicker) {
        CircularColorPickerDialog(
            manager = manager,
            onDismiss = { showRainbowPicker = false }
        )
    }
}

// -----------------------------------------------------------------------------------------
// 3. Artists Screen
// -----------------------------------------------------------------------------------------

@Composable
fun ArtistsScreen(
    manager: MusicManager,
    onOpenArtist: (ArtistItem) -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Artists (${manager.parsedArtistsList.size})",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 18.sp)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(manager.parsedArtistsList, key = { it.name }) { artist ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0xFF131B2E) else Color.White)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .clickable { onOpenArtist(artist) }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎤", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = artist.name,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.songs.size} tracks",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. Search Screen
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun SearchScreen(
    manager: MusicManager,
    onExpandPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, manager.allSongs.size) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else manager.allSongs.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.artist.contains(q, ignoreCase = true) ||
            it.album.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Music",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 18.sp)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, albums...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrent = song.id == manager.currentSong?.id,
                    isPlaying = manager.isPlaying && song.id == manager.currentSong?.id,
                    manager = manager,
                    isDark = isDark,
                    isSelected = false,
                    isMultiSelectMode = false,
                    onClick = {
                        manager.playSong(song, filtered, "Search")
                    },
                    onLongClick = {},
                    onFavoriteToggle = {
                        manager.toggleFavorite(song)
                    },
                    onMoreOptionsClick = {}
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 5. Folder Detail View
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun FolderDetailView(
    manager: MusicManager,
    folderName: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    val songs = remember(manager.allSongs.size, folderName) {
        manager.allSongs.filter { it.folderName == folderName }
    }

    var editingFolderColor by remember { mutableStateOf<String?>(null) }
    var showRainbowPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(folderName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("${songs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable { editingFolderColor = folderName }) {
                    GlassmorphicFolderIcon(
                        folderColor = manager.getFolderColor(folderName),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrent = song.id == manager.currentSong?.id,
                    isPlaying = manager.isPlaying && song.id == manager.currentSong?.id,
                    manager = manager,
                    isDark = isDark,
                    isSelected = false,
                    isMultiSelectMode = false,
                    onClick = { manager.playSong(song, songs, folderName) },
                    onLongClick = {},
                    onFavoriteToggle = { manager.toggleFavorite(song) },
                    onMoreOptionsClick = {}
                )
            }
        }
    }

    // Line 990 preserved
    if (editingFolderColor != null) {
        val fName = editingFolderColor!!
        FolderColorDialog(
            folderName = fName,
            currentColor = manager.getFolderColor(fName),
            isDark = isDark,
            onColorSelected = { col ->
                manager.updateFolderColorOnly(fName, col.toArgb().toLong())
                editingFolderColor = null
            },
            onOpenRainbowPicker = { showRainbowPicker = true },
            onDismiss = { editingFolderColor = null }
        )
    }

    if (showRainbowPicker) {
        CircularColorPickerDialog(
            manager = manager,
            onDismiss = { showRainbowPicker = false }
        )
    }
}

// -----------------------------------------------------------------------------------------
// 6. Artist Detail View
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun ArtistDetailView(
    manager: MusicManager,
    artist: ArtistItem,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(artist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("${artist.songs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 18.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artist.songs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrent = song.id == manager.currentSong?.id,
                    isPlaying = manager.isPlaying && song.id == manager.currentSong?.id,
                    manager = manager,
                    isDark = isDark,
                    isSelected = false,
                    isMultiSelectMode = false,
                    onClick = { manager.playSong(song, artist.songs, artist.name) },
                    onLongClick = {},
                    onFavoriteToggle = { manager.toggleFavorite(song) },
                    onMoreOptionsClick = {}
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 7. Playlist Detail View
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun PlaylistDetailView(
    manager: MusicManager,
    playlist: Playlist,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    val songIdSet = remember(playlist.songIds) { playlist.songIds.toHashSet() }
    val songs = remember(manager.allSongs.size, songIdSet) {
        manager.allSongs.filter { it.id in songIdSet }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("${songs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { manager.shufflePlaylist(playlist) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Shuffle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrent = song.id == manager.currentSong?.id,
                    isPlaying = manager.isPlaying && song.id == manager.currentSong?.id,
                    manager = manager,
                    isDark = isDark,
                    isSelected = false,
                    isMultiSelectMode = false,
                    onClick = { manager.playSong(song, songs, playlist.name) },
                    onLongClick = {},
                    onFavoriteToggle = { manager.toggleFavorite(song) },
                    onMoreOptionsClick = {}
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 8. Profile Edit Screen
// -----------------------------------------------------------------------------------------

@Composable
fun ProfileEditScreen(manager: MusicManager, onBack: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

    var name by remember { mutableStateOf(manager.profileName) }
    var email by remember { mutableStateOf(manager.profileEmail) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.dp, accent, CircleShape)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!manager.profileImagePath.isNullOrBlank() && File(manager.profileImagePath).exists()) {
                    AsyncImage(
                        model = File(manager.profileImagePath),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultProfileAvatar(modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Tap to Change Picture", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                manager.savePermanentProfile(name.trim(), email.trim(), selectedImageUri)
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// -----------------------------------------------------------------------------------------
// 9. Song Row & Context Menu Modals
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    manager: MusicManager,
    isDark: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var songArt by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }

    LaunchedEffect(song.id) {
        if (songArt == null) songArt = manager.loadAlbumArtAsync(song)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.22f)
                else if (isCurrent) accent.copy(alpha = 0.12f)
                else if (isDark) Color(0xFF131B2E)
                else Color.White
            )
            .border(
                1.dp,
                if (isSelected) accent
                else if (isCurrent) accent
                else if (isDark) Color(0x14FFFFFF)
                else Color(0xFFECEFF3),
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else Color.Transparent)
                    .border(1.5.dp, if (isSelected) accent else Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            if (songArt != null) {
                Image(
                    bitmap = songArt!!.asImageBitmap(),
                    contentDescription = "Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("🎵", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrent && isPlaying) {
            LiveAudioWaveEqualizer(isAnimating = true, accentColor = accent)
            Spacer(modifier = Modifier.width(8.dp))
        }

        HeartIconVector(
            isFavorite = song.isFavorite,
            defaultTint = Color(0xFF64748B),
            modifier = Modifier.clickable { onFavoriteToggle() }
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "⋮",
            fontSize = 18.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onMoreOptionsClick() }
                .padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun SongContextMenuModal(
    song: Song,
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDeleteSong: () -> Unit
) {
    val context = LocalContext.current
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = song.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatFileSize(song.size)} • ${song.artist}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                MenuRow("▶", "Play Next in Queue", isDark) {
                    manager.playNextInQueue(song)
                    onDismiss()
                }
                MenuRow("➕", "Add to Playlist", isDark) {
                    onAddToPlaylist()
                }
                MenuRow("🔔", "Set as Ringtone", isDark) {
                    manager.setAsRingtone(song)
                    onDismiss()
                }
                MenuRow("🔗", "Share Audio", isDark) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, song.uri)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    onDismiss()
                }
                MenuRow("🗑️", "Delete File", isDark, isDanger = true) {
                    onDeleteSong()
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
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
fun ConfirmDeleteSongDialog(
    song: Song,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Text("Delete Audio?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Are you sure you want to permanently delete \"${song.title}\" from device storage? This cannot be undone.",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPlaylistCard(playlist: Playlist, isDark: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF131B2E) else Color.White)
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassmorphicFolderIcon(
            folderColor = Color(playlist.iconColorHex),
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songIds.size} songs",
            color = Color(0xFF64748B),
            fontSize = 10.sp
        )
    }
}

// Bottom Navigation Bar
@Composable
fun BottomNavBar(manager: MusicManager, activeTab: String, onTabSelected: (String) -> Unit) {
    val isDark = manager.isDarkMode
    val navBg = if (isDark) Color(0xE60A0F1D) else Color(0xF2FFFFFF)
    val accent = manager.accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBg)
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("home", "Home", "🏠"),
            Triple("library", "Library", "📁"),
            Triple("artists", "Artists", "🎤"),
            Triple("search", "Search", "🔍")
        ).forEach { (key, label, icon) ->
            val isSel = activeTab == key
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(key) }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(icon, fontSize = 20.sp)
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isSel) accent else Color(0xFF64748B),
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
