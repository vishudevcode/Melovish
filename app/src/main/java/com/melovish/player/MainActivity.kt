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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File

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

    // Active Context Menu State for Song Actions
    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var activeTagEditSong by remember { mutableStateOf<Song?>(null) }
    var activeSongInfo by remember { mutableStateOf<Song?>(null) }
    var activeAddToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    BackHandler(enabled = isPlayerExpanded || selectedArtist != null || selectedAlbum != null || selectedPlaylist != null || selectedFolder != null || activeScreen != "home") {
        when {
            isPlayerExpanded -> isPlayerExpanded = false
            selectedArtist != null -> selectedArtist = null
            selectedAlbum != null -> selectedAlbum = null
            selectedPlaylist != null -> selectedPlaylist = null
            selectedFolder != null -> selectedFolder = null
            else -> activeScreen = "home"
        }
    }

    val isDark = manager.isDarkMode
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
                                onSongMenuClick = { activeSongForMenu = it }
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
                            onOpenEqualizer = { isPlayerExpanded = true }
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

            // Universal 9-Option Song Action Modal
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

// 9-Option Action Sheet with Icons Matching Reference
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = song.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 1. Play next
                ModalActionRow("▶≡", "Play next", textColor) { onPlayNext() }
                // 2. Add to playlist
                ModalActionRow("≡+", "Add to playlist", textColor) { onAddToPlaylist() }
                // 3. Go to album
                ModalActionRow("◎", "Go to album", textColor) { onGoToAlbum() }
                // 4. Go to artist
                ModalActionRow("👤", "Go to artist", textColor) { onGoToArtist() }
                // 5. Share
                ModalActionRow("🔗", "Share", textColor) { onShare() }
                // 6. Tag editor
                ModalActionRow("🏷️", "Tag editor", textColor) { onTagEditor() }
                // 7. Details
                ModalActionRow("ℹ️", "Details", textColor) { onDetails() }
                // 8. Set as ringtone
                ModalActionRow("🔔", "Set as ringtone", textColor) { onSetRingtone() }
                // 9. Delete from device (Preserved)
                ModalActionRow("🗑️", "Delete from device", Color(0xFFEF4444)) { onDelete() }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)),
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp, color = color, modifier = Modifier.width(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
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

        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", color = textColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
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
    val art = manager.getAlbumArt(song)

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
            if (art != null) {
                Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
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

        // Extreme Right Three Dots
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Recently Played (Top 25%)
            item {
                Text("Recently Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(10.dp))

                val recents = manager.historySongs.take(30)
                if (recents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recently played tracks yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recents) { song ->
                            val art = manager.getAlbumArt(song)
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                    .clickable { manager.playSong(song, recents, "Recent Tracks") }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)),
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
                                    Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}", color = manager.accentColor, fontSize = 10.sp, maxLines = 1)
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
                        modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists yet. Tap '+ New' to create one.", color = Color(0xFF64748B), fontSize = 13.sp)
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

            // 3. All Songs Header with Total Count and Sort Action
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
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
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

            // Every song has the three-dot menu at extreme right
            items(sortedSongs) { song ->
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

// Filtered Songs Drilldown (for "Go to artist" and "Go to album")
@Composable
fun FilteredSongsScreen(
    title: String,
    songs: List<Song>,
    manager: MusicManager,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
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

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching tracks found.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(songs) { song ->
                    UniversalSongRow(
                        song = song,
                        manager = manager,
                        isDark = isDark,
                        onPlay = { manager.playSong(song, songs, title) },
                        onMenuClick = { onSongMenuClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    manager: MusicManager,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    val songsInPlaylist = playlist.songIds.mapNotNull { id -> manager.allSongs.find { it.id == id } }
    var showAddSongsPicker by remember { mutableStateOf(false) }

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
                    onClick = { showAddSongsPicker = true },
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
                Text("Playlist is empty. Tap '+ Add' to select songs.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(songsInPlaylist) { song ->
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

    if (showAddSongsPicker) {
        AddSongsToPlaylistPicker(
            playlist = playlist,
            manager = manager,
            onDismiss = { showAddSongsPicker = false }
        )
    }
}

@Composable
fun AddSongsToPlaylistPicker(
    playlist: Playlist,
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val isDark = manager.isDarkMode
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(550.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Songs", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
                }
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(manager.allSongs) { song ->
                        val isAdded = song.id in playlist.songIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    if (isAdded) manager.removeSongFromPlaylist(song.id, playlist)
                                    else manager.addSongToPlaylist(song.id, playlist)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text(if (isAdded) "✓ Added" else "+ Add", color = if (isAdded) manager.accentColor else Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Text("Create New Playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Or Pin an Entire Device Folder:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
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
                    Text("${formatFileSize(totalSize)} • ${songs.size} songs", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                Text("›", color = manager.accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FolderSongsScreen(
    folderName: String,
    manager: MusicManager,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
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
            items(songs) { song ->
                UniversalSongRow(
                    song = song,
                    manager = manager,
                    isDark = isDark,
                    onPlay = { manager.playSong(song, songs, folderName) },
                    onMenuClick = { onSongMenuClick(song) }
                )
            }
        }
    }
}

@Composable
fun SearchScreen(
    manager: MusicManager,
    onSongMenuClick: (Song) -> Unit
) {
    var query by remember { mutableStateOf("") }
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { song ->
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
fun SongInfoDialog(song: Song, isDark: Boolean, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
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
