package com.melovish.player

import android.graphics.BitmapFactory
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun SettingsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val accent = manager.accentColor

    var showCircularPicker by remember { mutableStateOf(false) }
    var showHideFoldersDialog by remember { mutableStateOf(false) }
    var showHideAudioDialog by remember { mutableStateOf(false) }

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) {
        BitmapFactory.decodeFile(avatarFile.absolutePath)
    } else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                    Text("←", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }

        // 1. Profile Section on Top (Matching Reference)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .clickable { onOpenProfile() }
                    .padding(18.dp)
            ) {
                Text("Profile", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Manage your profile information and preferences.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("👤", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(manager.profileName, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (manager.profileEmail.isNotBlank()) manager.profileEmail else "Personal Account", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("›", fontSize = 22.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Appearance Section (Themes & Palette)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Appearance", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Customize the look and feel of your music player.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Text("Theme", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("System", "Light", "Dark").forEach { mode ->
                        val isSel = manager.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0x1A00B4D8) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                                .border(1.5.dp, if (isSel) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { manager.setTheme(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode, color = if (isSel) accent else textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Color Palette", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        Color(0xFF00B4D8), // 1. Teal Blue
                        Color(0xFF2EC4B6), // 2. Mint Green
                        Color(0xFF39FF14), // 3. Bright Neon Green
                        Color(0xFFFF2A85), // 4. Pink
                        Color(0xFFFF3B30)  // 5. Red
                    )

                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(2.dp, if (manager.accentColor == col) Color.White else Color.Transparent, CircleShape)
                                .clickable { manager.updateAccent(col) }
                        )
                    }

                    // 6th Slot: Circular Wheel Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showCircularPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }

        // 3. Player Settings Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Player Settings", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Configure playback behavior.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow("🎨", "Colourful Player", "Player background adapts to album art.", manager.isColorfulPlayer, textColor) {
                    manager.isColorfulPlayer = it
                }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("⏯️", "Resume only the first file", "Playback will only resume for the first track.", manager.isResumeFirstOnly, textColor) {
                    manager.isResumeFirstOnly = it
                }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🔊", "Fade on start", "Gently fade in audio on playback.", manager.isFadeOnStart, textColor) {
                    manager.isFadeOnStart = it
                }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("♾️", "Gapless Playback", "Removes pauses between tracks.", manager.isGaplessEnabled, textColor) {
                    manager.isGaplessEnabled = it
                }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("↔️", "Crossfade", "Adjust the fade duration between tracks.", manager.isCrossfadeEnabled, textColor) {
                    manager.isCrossfadeEnabled = it
                }

                if (manager.isCrossfadeEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Duration: ${manager.crossfadeDuration.toInt()}s", color = Color(0xFF64748B), fontSize = 12.sp)
                    Slider(
                        value = manager.crossfadeDuration,
                        onValueChange = { manager.crossfadeDuration = it },
                        valueRange = 1f..10f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            }
        }

        // 4. Audio Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Audio", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Adjust audio playback settings.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow("📶", "Lossless Audio", "Use Dolby Atmos and Hi-Res Audio.", manager.isLosslessEnabled, textColor) {
                    manager.isLosslessEnabled = it
                }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🔉", "Volume Normalization", "Set the same loudness level for all tracks.", manager.isVolumeNormalized, textColor) {
                    manager.isVolumeNormalized = it
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Volume Boost", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Increase the maximum volume (${manager.volumeBoostLevel.toInt()}%).", color = Color(0xFF64748B), fontSize = 12.sp)
                Slider(
                    value = manager.volumeBoostLevel,
                    onValueChange = {
                        manager.volumeBoostLevel = it
                        manager.attachAudioEffects()
                    },
                    valueRange = 100f..200f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )

                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🎚️", "Mono Audio", "Combine left and right channels.", manager.isMonoAudio, textColor) {
                    manager.isMonoAudio = it
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Audio Output", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Triple("Phone", "📱", "Phone"), Triple("Speaker", "🔊", "Speaker"), Triple("Buds", "🎧", "Buds")).forEach { (key, icon, label) ->
                        val isSel = manager.selectedAudioOutput == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0x1A00B4D8) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                                .border(1.5.dp, if (isSel) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { manager.setAudioOutputRouting(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, color = if (isSel) accent else textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Equalizer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(manager.selectedEqPreset, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onOpenEqualizer() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Adjust", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(checked = manager.isEqEnabled, onCheckedChange = { manager.toggleEqualizer(it) })
                    }
                }
            }
        }

        // 5. Content Management (Hide Folders & Audio)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Content", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Manage what music appears in your library.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hide Folders", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Exclude specific folders from your library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showHideFoldersDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hide Audio", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hide specific audio files from your library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showHideAudioDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCircularPicker) {
        CircularColorPickerDialog(manager = manager, onDismiss = { showCircularPicker = false })
    }

    if (showHideFoldersDialog) {
        ManageHiddenFoldersDialog(manager = manager, onDismiss = { showHideFoldersDialog = false })
    }

    if (showHideAudioDialog) {
        ManageHiddenAudioDialog(manager = manager, onDismiss = { showHideAudioDialog = false })
    }
}

@Composable
fun SettingSwitchRow(icon: String, title: String, subtitle: String, checked: Boolean, textColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF64748B), fontSize = 11.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ManageHiddenFoldersDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val folders = manager.allSongs.map { it.folderName }.distinct()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).height(500.dp).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF0F172A) else Color.White).padding(20.dp)) {
            Column {
                Text("Exclude Folders", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        val isHidden = folder in manager.hiddenFolders
                        Row(modifier = Modifier.fillMaxWidth().clickable { manager.toggleHideFolder(folder) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isHidden, onCheckedChange = { manager.toggleHideFolder(folder) }, colors = CheckboxDefaults.colors(checkedColor = manager.accentColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
fun ManageHiddenAudioDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).height(500.dp).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF0F172A) else Color.White).padding(20.dp)) {
            Column {
                Text("Exclude Audio Files", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(manager.allSongs, key = { it.id }) { song ->
                        val isHidden = song.id in manager.hiddenAudioIds
                        Row(modifier = Modifier.fillMaxWidth().clickable { manager.toggleHideAudio(song.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isHidden, onCheckedChange = { manager.toggleHideAudio(song.id) }, colors = CheckboxDefaults.colors(checkedColor = manager.accentColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(song.title, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 13.sp, maxLines = 1)
                                Text(song.artist, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
