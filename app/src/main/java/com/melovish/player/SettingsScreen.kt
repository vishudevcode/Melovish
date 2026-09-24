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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1010B981)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
        )
    }
}

@Composable
fun ColorPaletteCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val emerald = Color(0xFF10B981)
    var showHideFolderDialog by remember { mutableStateOf(false) }
    var showHideAudioDialog by remember { mutableStateOf(false) }

    if (showHideFolderDialog) {
        ManageHiddenFoldersSheet(
            manager = manager,
            onDismiss = { showHideFolderDialog = false }
        )
        return
    }

    if (showHideAudioDialog) {
        ManageHiddenAudioSheet(
            manager = manager,
            onDismiss = { showHideAudioDialog = false }
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
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
        }

        // 1. Profile Summary Card
        item {
            SettingsCard(title = "Profile", subtitle = "Manage your profile information and preferences.") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { onOpenProfile() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = manager.profileName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text("›", fontSize = 22.sp, color = Color(0xFF64748B))
                }
            }
        }

        // 2. Appearance Card
        item {
            SettingsCard(title = "Appearance", subtitle = "Customize the look and feel of your music player.") {
                Text("Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("System", "💻", manager.selectedTheme == "System"),
                        Triple("Light", "☼", manager.selectedTheme == "Light"),
                        Triple("Dark", "☾", manager.selectedTheme == "Dark")
                    ).forEach { (theme, icon, isSelected) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) emerald else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) Color(0x1010B981) else Color.Transparent)
                                .clickable { manager.selectedTheme = theme },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(icon, fontSize = 14.sp, color = if (isSelected) emerald else Color(0xFF64748B))
                                Text(theme, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Color Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))

                val palettes = listOf(
                    Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFFEC4899),
                    Color(0xFF0EA5E9), Color(0xFFF97316), Color(0xFF84CC16),
                    Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFF14B8A6)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    palettes.take(5).forEachIndexed { index, color ->
                        ColorPaletteCircle(
                            color = color,
                            isSelected = manager.selectedPaletteIndex == index,
                            onClick = { manager.selectedPaletteIndex = index }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    palettes.drop(5).forEachIndexed { index, color ->
                        val realIndex = index + 5
                        ColorPaletteCircle(
                            color = color,
                            isSelected = manager.selectedPaletteIndex == realIndex,
                            onClick = { manager.selectedPaletteIndex = realIndex }
                        )
                    }
                }
            }
        }

        // 3. Player Settings Card
        item {
            SettingsCard(title = "Player Settings", subtitle = "Configure playback behavior.") {
                SettingsToggleRow("🎨", "Colourful Player", "Player background adapts to album art.", manager.isColorfulPlayer) { manager.isColorfulPlayer = it }
                SettingsToggleRow("⏯", "Resume only the first file", "Playback will only resume for the first track.", manager.isResumeFirstOnly) { manager.isResumeFirstOnly = it }
                SettingsToggleRow("🔉", "Fade on start", "Gently fade in audio on playback.", manager.isFadeOnStart) { manager.isFadeOnStart = it }
                SettingsToggleRow("♾", "Gapless Playback", "Removes pauses between tracks.", manager.isGaplessEnabled) { manager.isGaplessEnabled = it }
                SettingsToggleRow("⇹", "Crossfade", "Adjust the fade duration between tracks.", manager.crossfadeDuration > 0f) {
                    manager.crossfadeDuration = if (it) 3f else 0f
                }
                if (manager.crossfadeDuration > 0f) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = manager.crossfadeDuration,
                            onValueChange = { manager.crossfadeDuration = it },
                            valueRange = 0f..12f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = emerald, activeTrackColor = emerald)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${manager.crossfadeDuration.toInt()}s", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        // 4. Audio Settings Card
        item {
            SettingsCard(title = "Audio", subtitle = "Adjust audio playback settings.") {
                SettingsToggleRow("📶", "Lossless Audio", "Use Dolby Atmos and Hi-Res Audio.", manager.isLosslessEnabled) { manager.isLosslessEnabled = it }
                SettingsToggleRow("🔊", "Volume Normalization", "Set the same loudness level for all tracks.", manager.isVolumeNormalized) { manager.isVolumeNormalized = it }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Text("Volume Boost", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    Text("Increase the maximum volume.", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = manager.volumeBoostLevel,
                            onValueChange = { manager.volumeBoostLevel = it },
                            valueRange = 100f..200f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = emerald, activeTrackColor = emerald)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${manager.volumeBoostLevel.toInt()}%", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                SettingsToggleRow("🎚", "Mono Audio", "Combine left and right channels.", manager.isMonoAudio) { manager.isMonoAudio = it }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Audio Output", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(Pair("Phone", "📱"), Pair("Speaker", "🔊"), Pair("Buds", "🎧")).forEach { (output, icon) ->
                        val isSelected = manager.selectedAudioOutput == output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) emerald else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) Color(0x1010B981) else Color.Transparent)
                                .clickable { manager.selectedAudioOutput = output },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(output, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text("Fine-tune your audio experience.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = manager.isEqEnabled,
                        onCheckedChange = { manager.isEqEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = emerald)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original", fontSize = 12.sp, color = Color(0xFF64748B))
                    Button(
                        onClick = { onOpenEqualizer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adjust", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // 5. Content Management Card
        item {
            SettingsCard(title = "Content", subtitle = "Manage what music appears in your library.") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Folders", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text("Exclude specific folders from your library.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = { showHideFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Audio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text("Hide specific audio files from your library.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = { showHideAudioDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun ManageHiddenFoldersSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val folders = manager.allSongs.map { it.folderName }.distinct()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exclude Folders", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) {
                Text("Done")
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders) { folder ->
                val isHidden = folder in manager.hiddenFolders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { manager.toggleHideFolder(folder) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(folder, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = !isHidden, onCheckedChange = { manager.toggleHideFolder(folder) })
                }
            }
        }
    }
}

@Composable
fun ManageHiddenAudioSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hide Audio Tracks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) {
                Text("Done")
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(manager.allSongs) { song ->
                val isHidden = song.id in manager.hiddenAudioIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { manager.toggleHideAudio(song.id) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(song.folderName, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = !isHidden, onCheckedChange = { manager.toggleHideAudio(song.id) })
                }
            }
        }
    }
}
