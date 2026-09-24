package com.melovish.player

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun ProfileScreen(
    manager: MusicManager,
    onBackClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(manager.profileName) }
    var editEmail by remember { mutableStateOf(manager.profileEmail) }
    var fullViewMode by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            manager.savePermanentProfile(manager.profileName, manager.profileEmail, uri)
        }
    }

    if (fullViewMode == "history") {
        FullHistoryListScreen(manager = manager, onBack = { fullViewMode = null })
        return
    }

    if (fullViewMode == "most_played") {
        FullMostPlayedListScreen(manager = manager, onBack = { fullViewMode = null })
        return
    }

    val isDark = manager.isDarkMode
    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) {
        BitmapFactory.decodeFile(avatarFile.absolutePath)
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("My Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }

        // Profile Card with Dynamic Center Alignment
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Button(
                            onClick = {
                                if (isEditing) {
                                    manager.savePermanentProfile(editName, editEmail, null)
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEditing) "Save" else "Edit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Permanent Avatar Picker
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
                                } else {
                                    Text("👤", fontSize = 36.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(manager.accentColor)
                                    .border(2.dp, cardBg, CircleShape)
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📷", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Dynamic Centered Name (If Email Is Missing, Centered In Alignment)
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = manager.profileName,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            if (manager.profileEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = manager.profileEmail,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    if (isEditing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Most Played (Top 5 preview)
        item {
            val mostPlayed = manager.getMostPlayedSongs().take(5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Most Played", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("Your all-time favorite songs.", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = { fullViewMode = "most_played" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View All ›", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (mostPlayed.isEmpty()) {
                        Text("No songs played yet.", color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        mostPlayed.forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { manager.playSong(song, manager.getMostPlayedSongs(), "Most Played") }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎧", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"}", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                                }
                                Text("▶", color = manager.accentColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // History
        item {
            val historyList = manager.historySongs.take(5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("Your recently played songs.", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = { fullViewMode = "history" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View All ›", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (historyList.isEmpty()) {
                        Text("No recent history.", color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        historyList.forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { manager.playSong(song, manager.historySongs, "History") }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"}", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                                }
                                Text(formatTime(song.duration), fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun FullMostPlayedListScreen(manager: MusicManager, onBack: () -> Unit) {
    val songs = manager.getMostPlayedSongs()
    val isDark = manager.isDarkMode
    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Most Played Ranking (1–100)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(songs) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, songs, "Most Played") }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = manager.accentColor, modifier = Modifier.width(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"}", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🔥 ${song.playCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }
            }
        }
    }
}

@Composable
fun FullHistoryListScreen(manager: MusicManager, onBack: () -> Unit) {
    val isDark = manager.isDarkMode
    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Listening History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(manager.historySongs) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, manager.historySongs, "History") }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                        Text("🎵", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                    }
                    Text(formatTime(song.duration), fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}
