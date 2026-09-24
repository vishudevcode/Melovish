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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        if (hasPermission) manager.scanStorage()
    }

    var activeScreen by remember { mutableStateOf("home") } // "home", "search", "playlist", "settings", "profile", "all_songs"
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }

    // Per-page layout options
    var homeIsGrid by remember { mutableStateOf(false) }
    var homeSortCriteria by remember { mutableStateOf("A-Z") }
    var homeSortAsc by remember { mutableStateOf(true) }

    BackHandler(enabled = activeScreen != "home" || isPlayerExpanded) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else {
            activeScreen = "home"
        }
    }

    val darkGradient = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(modifier = Modifier.fillMaxSize().background(darkGradient)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopBar(
                    onProfileClick = { activeScreen = "profile" },
                    onSettingsClick = { activeScreen = "settings" },
                    onLayoutClick = { showLayoutDialog = true }
                )

                // Body based on active screen
                Box(modifier = Modifier.weight(1f)) {
                    when (activeScreen) {
                        "settings" -> SettingsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" },
                            onOpenProfile = { activeScreen = "profile" },
                            onOpenEqualizer = { isPlayerExpanded = true }
                        )
                        "profile" -> ProfileScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" }
                        )
                        "search" -> SearchScreen(manager = manager)
                        "playlist" -> PlaylistFolderScreen(
                            manager = manager,
                            onResumeClick = { manager.resumeLastPlayed("Storage") }
                        )
                        "all_songs" -> AllSongsScreen(
                            manager = manager,
                            onBackClick = { activeScreen = "home" }
                        )
                        else -> HomeScreen(
                            manager = manager,
                            isGrid = homeIsGrid,
                            onViewAllClick = { activeScreen = "all_songs" },
                            onResumeClick = { manager.resumeLastPlayed() }
                        )
                    }
                }

                // Mini Player Dock above Navigation Bar
                if (manager.currentSong != null && !isPlayerExpanded) {
                    MiniPlayerDock(
                        manager = manager,
                        onClick = { isPlayerExpanded = true }
                    )
                }

                // Bottom Navigation Bar
                if (activeScreen in listOf("home", "search", "playlist")) {
                    BottomNavBar(
                        activeTab = activeScreen,
                        onTabSelected = { activeScreen = it }
                    )
                }
            }

            // Full Player Screen Overlay
            if (isPlayerExpanded) {
                FullPlayerSheet(
                    manager = manager,
                    onDismiss = { isPlayerExpanded = false }
                )
            }

            // Layout & Sorting Dialog
            if (showLayoutDialog) {
                LayoutSortDialog(
                    isGrid = homeIsGrid,
                    criteria = homeSortCriteria,
                    isAsc = homeSortAsc,
                    onApply = { g, c, a ->
                        homeIsGrid = g
                        homeSortCriteria = c
                        homeSortAsc = a
                        showLayoutDialog = false
                    },
                    onDismiss = { showLayoutDialog = false }
                )
            }
        }
    }
}

@Composable
fun TopBar(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLayoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture Avatar Icon on LEFT
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 18.sp)
        }

        // App Logo / Name in Center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Melovish", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Liquid Glass Audio", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Sorting & Settings on RIGHT
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .clickable { onLayoutClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(
    manager: MusicManager,
    isGrid: Boolean,
    onViewAllClick: () -> Unit,
    onResumeClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Top 25%: Recent 50 tracks in horizontal carousel (3 visible)
            Text(
                text = "Recently Played",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
            )

            val recents = manager.historySongs.take(50)
            if (recents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0FFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent tracks played yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recents) { song ->
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                                .clickable { manager.playSong(song, recents, "Recent Tracks") }
                                .padding(10.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(song.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (song.artist.isNotBlank()) song.artist else "Audio", color = Color(0xFFD4AF37), fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lower 75%: All Songs Section with "View All" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All Songs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onViewAllClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AD4AF37)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("View All", color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(manager.allSongs) { song ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .clickable { manager.playSong(song, manager.allSongs, "All Songs") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 42.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(if (song.artist.isNotBlank()) song.artist else "Audio", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(manager.allSongs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .clickable { manager.playSong(song, manager.allSongs, "All Songs") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 22.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(formatTime(song.duration), color = Color.White, fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${if (song.artist.isNotBlank()) song.artist else "Audio"} • ${formatFileSize(song.size)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Resume Button in Bottom Right Corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4AF37))
                .clickable { onResumeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable { manager.playSong(song, filtered, "Search Results") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${song.folderName} • ${song.artist}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistFolderScreen(
    manager: MusicManager,
    onResumeClick: () -> Unit
) {
    val folderMap = manager.allSongs.groupBy { it.folderName }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Folders", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            items(folderMap.keys.toList()) { folderName ->
                val songs = folderMap[folderName] ?: emptyList()
                val totalSize = songs.sumOf { it.size }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(songs.first(), songs, folderName) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folderName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${songs.size} songs • ${formatFileSize(totalSize)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Text("▶", color = Color(0xFFD4AF37), fontSize = 16.sp)
                }
            }
        }

        // Floating Resume Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4AF37))
                .clickable { onResumeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun AllSongsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("All Songs (${manager.allSongs.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(manager.allSongs) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable { manager.playSong(song, manager.allSongs, "All Songs") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${if (song.artist.isNotBlank()) song.artist else "Audio"} • ${song.folderName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Text(formatTime(song.duration), color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE60A0F1D))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("home", "Home", "🏠"),
            Triple("search", "Search", "🔍"),
            Triple("playlist", "Playlist", "📑")
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
                    color = if (isSel) Color(0xFFD4AF37) else Color(0xFF64748B),
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LayoutSortDialog(
    isGrid: Boolean,
    criteria: String,
    isAsc: Boolean,
    onApply: (Boolean, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempGrid by remember { mutableStateOf(isGrid) }
    var tempCriteria by remember { mutableStateOf(criteria) }
    var tempAsc by remember { mutableStateOf(isAsc) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .padding(20.dp)
        ) {
            Column {
                Text("Page Layout & Sorting", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Layout View", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !tempGrid, onClick = { tempGrid = false })
                    Text("List View", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = tempGrid, onClick = { tempGrid = true })
                    Text("Grid View (2 Cols)", color = Color.White, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Sort Criteria", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                listOf("A-Z", "Date", "Status").forEach { crit ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tempCriteria == crit, onClick = { tempCriteria = crit })
                        Text(crit, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Order", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tempAsc, onClick = { tempAsc = true })
                    Text("Ascending", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !tempAsc, onClick = { tempAsc = false })
                    Text("Descending", color = Color.White, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { onApply(tempGrid, tempCriteria, tempAsc) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Settings")
                }
            }
        }
    }
}
