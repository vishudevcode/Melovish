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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsCard(
    isDark: Boolean,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(14.dp))
            content()
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
    var showColorWheelDialog by remember { mutableStateOf(false) }
    var showHideFolderDialog by remember { mutableStateOf(false) }
    var showHideAudioDialog by remember { mutableStateOf(false) }

    val isDark = manager.isDarkMode
    val bg = if (isDark) Color(0xFF030712) else Color(0xFFFAF8F5)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val accent = manager.accentColor

    if (showHideFolderDialog) {
        ManageHiddenFoldersSheet(manager = manager, onDismiss = { showHideFolderDialog = false })
        return
    }

    if (showHideAudioDialog) {
        ManageHiddenAudioSheet(manager = manager, onDismiss = { showHideAudioDialog = false })
        return
    }

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
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }

        // Profile Summary
        item {
            SettingsCard(isDark, "Profile", "Manage your profile identity.") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { onOpenProfile() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                            Text("👤", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(manager.profileName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    }
                    Text("›", fontSize = 22.sp, color = Color(0xFF64748B))
                }
            }
        }

        // Appearance: Dual Liquid Glass & Single Line Color Palette (5 + 1 Custom)
        item {
            SettingsCard(isDark, "Appearance", "Liquid Glass iOS Theming & Accent Palettes.") {
                Text("Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Light Cream Liquid Glass
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isDark) Color(0x22D4AF37) else Color.Transparent)
                            .border(1.5.dp, if (!isDark) accent else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { manager.toggleDarkMode(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☼ Warm Cream", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Pitch Black Obsidian Glass
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x22D4AF37) else Color.Transparent)
                            .border(1.5.dp, if (isDark) accent else Color(0x22000000), RoundedCornerShape(14.dp))
                            .clickable { manager.toggleDarkMode(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☾ Pitch Obsidian", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Accent Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                val presetColors = listOf(
                    Color(0xFFD4AF37), // Signature Gold
                    Color(0xFF10B981), // Emerald
                    Color(0xFF06B6D4), // Cyan
                    Color(0xFFEC4899), // Neon Pink
                    Color(0xFF8B5CF6)  // Royal Purple
                )

                // Single Horizontal Line: 5 Presets + 6th Custom Circle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetColors.forEach { col ->
                        val isSelected = manager.accentColor == col
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (isSelected) 3.dp else 0.dp, textColor, CircleShape)
                                .clickable { manager.updateAccent(col) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 6th Custom Color Circle (Wheel Gradient)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(1.5.dp, textColor, CircleShape)
                            .clickable { showColorWheelDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎨", fontSize = 16.sp)
                    }
                }
            }
        }

        // Equalizer & Audio
        item {
            SettingsCard(isDark, "Audio & Equalizer", "Audio effects and volume settings.") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equalizer Modal", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Button(onClick = { onOpenEqualizer() }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Adjust", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Volume Normalization", color = textColor, fontSize = 14.sp)
                    Switch(checked = manager.isVolumeNormalized, onCheckedChange = { manager.isVolumeNormalized = it })
                }
            }
        }

        // Content
        item {
            SettingsCard(isDark, "Content & Folders", "Exclude audio directories from storage scan.") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exclude Specific Folders", color = textColor, fontSize = 14.sp)
                    Button(onClick = { showHideFolderDialog = true }, shape = RoundedCornerShape(10.dp)) {
                        Text("Manage")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Modern Circular Color Wheel Dialog
    if (showColorWheelDialog) {
        ModernColorWheelDialog(
            currentColor = manager.accentColor,
            onColorSelect = {
                manager.updateAccent(it)
                showColorWheelDialog = false
            },
            onDismiss = { showColorWheelDialog = false }
        )
    }
}

@Composable
fun ModernColorWheelDialog(
    currentColor: Color,
    onColorSelect: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val spectrum = listOf(
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
        Color(0xFF34C759), Color(0xFF00C7BE), Color(0xFF30B0C7),
        Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFAF52DE),
        Color(0xFFFF2D55), Color(0xFFA2845E), Color(0xFFD4AF37)
    )

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Accent Color", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                // Modern 12-Color Circular Spectrum Matrix
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    spectrum.take(6).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { onColorSelect(col) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    spectrum.drop(6).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { onColorSelect(col) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ManageHiddenFoldersSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val folders = manager.allSongs.map { it.folderName }.distinct()
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).statusBarsPadding().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exclude Folders", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders) { folder ->
                val isHidden = folder in manager.hiddenFolders
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1AFFFFFF)).clickable { manager.toggleHideFolder(folder) }.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(folder, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Switch(checked = !isHidden, onCheckedChange = { manager.toggleHideFolder(folder) })
                }
            }
        }
    }
}

@Composable
fun ManageHiddenAudioSheet(manager: MusicManager, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).statusBarsPadding().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hide Audio Tracks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(manager.allSongs) { song ->
                val isHidden = song.id in manager.hiddenAudioIds
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1AFFFFFF)).clickable { manager.toggleHideAudio(song.id) }.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                        Text(song.folderName, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = !isHidden, onCheckedChange = { manager.toggleHideAudio(song.id) })
                }
            }
        }
    }
}
