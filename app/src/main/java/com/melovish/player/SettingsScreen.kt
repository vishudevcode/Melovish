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
fun SettingsToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
        )
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

        // 1. Profile Card
        item {
            SettingsCard(isDark, "Profile", "Manage your profile information and preferences.") {
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

        // 2. Appearance: Theme + Single-line Color Palette (5 + 1 Custom)
        item {
            SettingsCard(isDark, "Appearance", "Customize the look and feel of your music player.") {
                Text("Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("System", "💻", false),
                        Triple("Light", "☼", !isDark),
                        Triple("Dark", "☾", isDark)
                    ).forEach { (theme, icon, isSel) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(if (isSel) 1.5.dp else 1.dp, if (isSel) accent else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0x15FF3B30) else Color.Transparent)
                                .clickable {
                                    if (theme == "Light") manager.toggleDarkMode(false)
                                    if (theme == "Dark") manager.toggleDarkMode(true)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(icon, fontSize = 14.sp)
                                Text(theme, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Color Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                val presetColors = listOf(
                    Color(0xFF10B981), // Emerald
                    Color(0xFF06B6D4), // Cyan
                    Color(0xFFEC4899), // Neon Pink
                    Color(0xFFFF9500), // Orange
                    Color(0xFF8B5CF6)  // Purple
                )

                // Single Line: 5 Presets + 6th Custom Color Picker
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

                    // 6th Custom Color Circle
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

        // 3. Player Settings
        item {
            SettingsCard(isDark, "Player Settings", "Configure playback behavior.") {
                SettingsToggleRow("🎨", "Colourful Player", "Player background adapts to album art.", manager.isColorfulPlayer, isDark, accent) {
                    manager.isColorfulPlayer = it
                }
                SettingsToggleRow("⏯", "Resume only the first file", "Playback will only resume for the first track.", manager.isResumeFirstOnly, isDark, accent) {
                    manager.isResumeFirstOnly = it
                }
                SettingsToggleRow("🔉", "Fade on start", "Gently fade in audio on playback.", manager.isFadeOnStart, isDark, accent) {
                    manager.isFadeOnStart = it
                }
                SettingsToggleRow("♾", "Gapless Playback", "Removes pauses between tracks.", manager.isGaplessEnabled, isDark, accent) {
                    manager.isGaplessEnabled = it
                }
                SettingsToggleRow("⇹", "Crossfade", "Adjust the fade duration between tracks.", manager.crossfadeDuration > 0f, isDark, accent) {
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
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${manager.crossfadeDuration.toInt()}s", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        // 4. Audio Settings
        item {
            SettingsCard(isDark, "Audio", "Adjust audio playback settings.") {
                SettingsToggleRow("📶", "Lossless Audio", "Use Dolby Atmos and Hi-Res Audio.", manager.isLosslessEnabled, isDark, accent) {
                    manager.isLosslessEnabled = it
                }
                SettingsToggleRow("🔊", "Volume Normalization", "Set the same loudness level for all tracks.", manager.isVolumeNormalized, isDark, accent) {
                    manager.isVolumeNormalized = it
                }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Text("Volume Boost", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    Text("Increase the maximum volume.", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = manager.volumeBoostLevel,
                            onValueChange = {
                                manager.volumeBoostLevel = it
                                manager.attachAudioEffects()
                            },
                            valueRange = 100f..200f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${manager.volumeBoostLevel.toInt()}%", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                SettingsToggleRow("🎚", "Mono Audio", "Combine left and right channels.", manager.isMonoAudio, isDark, accent) {
                    manager.isMonoAudio = it
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Audio Output", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))

                // Audio Output Routing Pills: Phone | Speaker | Buds
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
                                    color = if (isSelected) accent else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) Color(0x15FF3B30) else Color.Transparent)
                                .clickable { manager.setAudioOutputRouting(output) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(output, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
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
                        Text("Equalizer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Fine-tune your audio experience.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = manager.isEqEnabled,
                        onCheckedChange = {
                            manager.isEqEnabled = it
                            manager.attachAudioEffects()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
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
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adjust", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Content Management: Hide Folders & Hide Audio
        item {
            SettingsCard(isDark, "Content", "Manage what music appears in your library.") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Folders", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Exclude specific folders from your library.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = { showHideFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Audio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Hide specific audio files from your library.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = { showHideAudioDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

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
                        Text("${formatFileSize(song.size)} • ${song.folderName}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = !isHidden, onCheckedChange = { manager.toggleHideAudio(song.id) })
                }
            }
        }
    }
}
