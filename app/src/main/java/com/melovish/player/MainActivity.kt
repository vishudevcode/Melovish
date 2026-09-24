package com.melovish.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = MusicManager(this)
        setContent {
            MelovishRootApp(manager)
        }
    }
}

@Composable
fun MelovishRootApp(manager: MusicManager) {
    val context = LocalContext.current
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

    var activeScreen by remember { mutableStateOf("home") } // "home", "search", "library", "settings", "profile", "all_songs"
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isPlayerExpanded || selectedFolder != null || activeScreen != "home") {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (selectedFolder != null) {
            selectedFolder = null
        } else {
            activeScreen = "home"
        }
    }

    val isDark = manager.isDarkMode
    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize().background(bg)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar with Safe Inset Protection
                TopBar(
                    manager = manager,
                    onProfileClick = { activeScreen = "profile" },
                    onSettingsClick = { activeScreen = "settings" }
                )

                // Screen Switcher
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        selectedFolder != null -> {
                            FolderSongsScreen(
                                folderName = selectedFolder!!,
                                manager = manager,
                                onBack = { selectedFolder = null }
                            )
                        }
                        activeScreen == "settings" -> SettingsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" },
                            onOpenProfile = { activeScreen = "profile" },
                            onOpenEqualizer = { isPlayerExpanded = true }
                        )
                        activeScreen == "profile" -> ProfileScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" }
                        )
                        activeScreen == "search" -> SearchScreen(manager = manager)
                        activeScreen == "library" -> LibraryScreen(
                            manager = manager,
                            onFolderClick = { folder -> selectedFolder = folder }
                        )
                        activeScreen == "all_songs" -> AllSongsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" }
                        )
                        else -> HomeScreen(
                            manager = manager,
                            onViewAllClick = { activeScreen = "all_songs" },
                            onResumeClick = { manager.resumeLastPlayed() }
                        )
                    }
                }

                // Mini Player Dock
                if (manager.currentSong != null && !isPlayerExpanded) {
                    MiniPlayerDock(manager = manager, onClick = { isPlayerExpanded = true })
                }

                // Bottom Nav Bar (Home | Search | Library)
                if (activeScreen in listOf("home", "search", "library") && selectedFolder == null) {
                    BottomNavBar(
                        manager = manager,
                        activeTab = activeScreen,
                        onTabSelected = { activeScreen = it }
                    )
                }
            }

            // Animated Spring Full Player Sheet
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullPlayerSheet(manager = manager, onDismiss = { isPlayerExpanded = false })
            }
        }
    }
}

// Top Bar with Clean "Melovish" Only
@Composable
fun TopBar(
    manager: MusicManager,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x33FFFFFF) else Color(0x22000000))
                .border(1.5.dp, manager.accentColor, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 18.sp)
        }

        // Center: Melovish Only (Subtitle Removed)
        Text(
            text = "Melovish",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Right: Sort & Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", color = textColor, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(
    manager: MusicManager,
    onViewAllClick: () -> Unit,
    onResumeClick: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Recently Played (Top 25%)
            item {
                Text("Recently Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(10.dp))

                val recents = manager.historySongs.take(50)
                if (recents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent tracks played yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recents) { song ->
                            val art = manager.getAlbumArt(song)
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .clickable { manager.playSong(song, recents, "Recent Tracks") }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (art != null) {
                                            Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                                        } else {
                                            Text("🎵", fontSize = 28.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(song.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (song.artist.isNotBlank()) song.artist else "Melovish", color = manager.accentColor, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Favourite Playlists Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favourite Playlists", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { manager.createPlaylist("Playlist ${manager.customPlaylists.size + 1}") },
                        colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ New", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (manager.customPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists created yet. Tap '+ New' to create one.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(manager.customPlaylists) { pl ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(if (pl.isFolderPinned) "📁" else "📑", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${pl.songIds.size} songs", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. All Songs with "View All"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Songs (${manager.allSongs.size})", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onViewAllClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View All", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(manager.allSongs.take(25)) { song ->
                val art = manager.getAlbumArt(song)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, manager.allSongs, "All Songs") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (art != null) {
                            Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("🎵", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (song.artist.isNotBlank()) song.artist else "Unknown"} • ${formatFileSize(song.size)}", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1)
                    }
                    Text(formatTime(song.duration), color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }

        // Floating Resume Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(manager.accentColor)
                .clickable { onResumeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// Library Screen (Renamed from Playlist)
@Composable
fun LibraryScreen(
    manager: MusicManager,
    onFolderClick: (String) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val folderMap = manager.allSongs.groupBy { it.folderName }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Folders & Storage", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        items(folderMap.keys.toList()) { folderName ->
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
                    Text("${songs.size} songs • ${formatFileSize(totalSize)}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                Text("›", color = manager.accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Folder Drill-down with Song Three-Dot Actions
@Composable
fun FolderSongsScreen(
    folderName: String,
    manager: MusicManager,
    onBack: () -> Unit
) {
    val songs = manager.allSongs.filter { it.folderName == folderName }
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    var activeSongMenu by remember { mutableStateOf<Song?>(null) }
    var activeTagEditSong by remember { mutableStateOf<Song?>(null) }
    var activeSongInfo by remember { mutableStateOf<Song?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(folderName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(songs) { song ->
                val art = manager.getAlbumArt(song)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, songs, folderName) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                        if (art != null) {
                            Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("🎵", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (song.artist.isNotBlank()) song.artist else "Unknown"} • ${formatFileSize(song.size)}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }

                    // Three-dot button
                    Box(modifier = Modifier.size(36.dp).clickable { activeSongMenu = song }, contentAlignment = Alignment.Center) {
                        Text("⋮", color = textColor, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    // Three Dot Menu Dialog for Folder Song
    if (activeSongMenu != null) {
        val s = activeSongMenu!!
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { activeSongMenu = null },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.8f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E293B)).padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Song Options", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("ℹ Song Info", color = Color(0xFFE2E8F0), modifier = Modifier.clickable { activeSongInfo = s; activeSongMenu = null })
                    Text("🏷 Edit Metadata", color = Color(0xFFE2E8F0), modifier = Modifier.clickable { activeTagEditSong = s; activeSongMenu = null })
                    Text("🗑 Delete from Device", color = Color(0xFFEF4444), modifier = Modifier.clickable {
                        manager.deleteSongFromDevice(s)
                        activeSongMenu = null
                    })
                }
            }
        }
    }

    if (activeTagEditSong != null) {
        TagEditorDialog(song = activeTagEditSong!!, onDismiss = { activeTagEditSong = null })
    }

    if (activeSongInfo != null) {
        val s = activeSongInfo!!
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { activeSongInfo = null },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E293B)).padding(20.dp)
            ) {
                Column {
                    Text("Song Info", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Title: ${s.title}", color = Color.White, fontSize = 13.sp)
                    Text("Artist: ${if (s.artist.isNotBlank()) s.artist else "Unknown"}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Album: ${s.album}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Size: ${formatFileSize(s.size)}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Duration: ${formatTime(s.duration)}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Path: ${s.path}", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { activeSongInfo = null }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun AllSongsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("All Songs (${manager.allSongs.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(manager.allSongs) { song ->
                val art = manager.getAlbumArt(song)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(14.dp))
                        .clickable { manager.playSong(song, manager.allSongs, "All Songs") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                        if (art != null) {
                            Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("🎵", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${if (song.artist.isNotBlank()) song.artist else "Audio"} • ${song.folderName}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Text(formatTime(song.duration), color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SearchScreen(manager: MusicManager) {
    var query by remember { mutableStateOf("") }
    val filtered = manager.allSongs.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.artist.contains(query, ignoreCase = true) ||
        it.folderName.contains(query, ignoreCase = true)
    }

    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, or folders...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { song ->
                val art = manager.getAlbumArt(song)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(14.dp))
                        .clickable { manager.playSong(song, filtered, "Search Results") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                        if (art != null) {
                            Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("🎵", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${song.folderName} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Bottom Navigation: Home | Search | Library
@Composable
fun BottomNavBar(
    manager: MusicManager,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
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
            Triple("search", "Search", "🔍"),
            Triple("library", "Library", "📚")
        ).forEach { (key, label, icon) ->
            val isSel = activeTab == key
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabSelected(key) }
            ) {
                Text(icon, fontSize = 18.sp)
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
