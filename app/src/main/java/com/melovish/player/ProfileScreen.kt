package com.melovish.player

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    manager: MusicManager,
    onBackClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(manager.profileName) }
    var editEmail by remember { mutableStateOf(manager.profileEmail) }
    var fullViewMode by remember { mutableStateOf<String?>(null) } // "history" or "most_played"

    if (fullViewMode == "history") {
        FullHistoryListScreen(
            manager = manager,
            onBack = { fullViewMode = null }
        )
        return
    }

    if (fullViewMode == "most_played") {
        FullMostPlayedListScreen(
            manager = manager,
            onBack = { fullViewMode = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 22.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "My Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        // 1. Profile Info Card (At Top)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Button(
                            onClick = {
                                if (isEditing) {
                                    manager.saveProfile(editName, editEmail, manager.profileImageUri)
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isEditing) "Save" else "Edit", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎵", fontSize = 32.sp)
                            }
                            // Camera Icon Button
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📷", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = manager.profileName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = manager.profileEmail,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
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
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. Most Played Songs Section (Directly Below Profile)
        item {
            val mostPlayed = manager.getMostPlayedSongs().take(5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Most Played", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text("Your all-time favorite songs.", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = { fullViewMode = "most_played" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View All ›", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎧", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (song.artist.isNotBlank()) song.artist else "Unknown Artist", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                                }
                                Text("▶", color = Color(0xFF10B981), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. History Section (Below Most Played)
        item {
            val historyList = manager.historySongs.take(5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text("Your recently played songs.", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = { fullViewMode = "history" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View All ›", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (song.artist.isNotBlank()) song.artist else "Unknown Artist", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                                }
                                Text(formatTime(song.duration), fontSize = 12.sp, color = Color(0xFF94A3B8))
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
fun FullMostPlayedListScreen(
    manager: MusicManager,
    onBack: () -> Unit
) {
    val songs = manager.getMostPlayedSongs()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 22.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Most Played Songs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(songs) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, songs, "Most Played") }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ranking Number 1 to 100 on the left
                    Text(
                        text = "#${index + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.width(36.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (song.artist.isNotBlank()) song.artist else "Unknown Artist", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                    }

                    // 3D-styled play-count badge on the right
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
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
fun FullHistoryListScreen(
    manager: MusicManager,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 22.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Listening History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(manager.historySongs) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.playSong(song, manager.historySongs, "History") }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎵", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"} • ${formatFileSize(song.size)}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                    Text(formatTime(song.duration), fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}
