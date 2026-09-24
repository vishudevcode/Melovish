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
import androidx.compose.ui.draw.shadow
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
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    var isEditMode by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(manager.profileName) }
    var editEmail by remember { mutableStateOf(manager.profileEmail) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showBigPicturePreview by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) pickedImageUri = uri
    }

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) {
        BitmapFactory.decodeFile(avatarFile.absolutePath)
    } else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                    Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("My Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }

        // Profile Info Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profile Info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Button(
                        onClick = {
                            if (isEditMode) {
                                manager.savePermanentProfile(editName, editEmail, pickedImageUri)
                                isEditMode = false
                            } else {
                                isEditMode = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEditMode) "Save" else "Edit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, manager.accentColor, CircleShape)
                            .clickable {
                                if (isEditMode) {
                                    photoPicker.launch("image/*")
                                } else {
                                    showBigPicturePreview = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("👤", fontSize = 36.sp)
                        }

                        // Camera icon only in Edit Mode
                        if (isEditMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(manager.accentColor)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📷", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    if (isEditMode) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Your Name") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email (Optional)") }, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Column {
                            Text(manager.profileName, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (manager.profileEmail.isNotBlank()) {
                                Text(manager.profileEmail, color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Most Played Section
        item {
            Text("Most Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val mostPlayed = manager.getMostPlayedSongs().take(15)
            if (mostPlayed.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0x14000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No played tracks yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mostPlayed.forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .clickable { manager.playSong(song, mostPlayed, "Most Played") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎧", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${song.playCount} plays • ${formatFileSize(song.size)}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text("▶", color = manager.accentColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Big Picture Preview Dialog on Tap
    if (showBigPicturePreview) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xEE000000)).clickable { showBigPicturePreview = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Big Avatar", modifier = Modifier.fillMaxSize())
                } else {
                    Text("👤", fontSize = 120.sp)
                }
            }
        }
    }
}
