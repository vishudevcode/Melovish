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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val emeraldAccent = Color(0xFF10B981)
    val cardBackground = Color(0xFFFFFFFF)
    val screenBackground = Color(0xFFF6F8FA)
    val textPrimary = Color(0xFF1E293B)
    val textSecondary = Color(0xFF64748B)

    var selectedTheme by remember { mutableStateOf("System") }
    var selectedPaletteIndex by remember { mutableIntStateOf(0) }

    // Player Settings States
    var colorfulPlayer by remember { mutableStateOf(false) }
    var resumeFirstFile by remember { mutableStateOf(false) }
    var fadeOnStart by remember { mutableStateOf(false) }
    var gaplessPlayback by remember { mutableStateOf(false) }
    var crossfadeEnabled by remember { mutableStateOf(false) }
    var crossfadeSeconds by remember { mutableFloatStateOf(0f) }

    // Audio Settings States
    var losslessAudio by remember { mutableStateOf(false) }
    var volumeNormalization by remember { mutableStateOf(false) }
    var volumeBoost by remember { mutableFloatStateOf(100f) }
    var monoAudio by remember { mutableStateOf(false) }
    var audioOutput by remember { mutableStateOf("Phone") }
    var equalizerEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 22.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
        }

        // 1. Profile Section
        item {
            SettingsCard(title = "Profile", subtitle = "Manage your profile information and preferences.") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Bharat Bhushan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                    }
                    Text("›", fontSize = 22.sp, color = textSecondary)
                }
            }
        }

        // 2. Appearance Section
        item {
            SettingsCard(title = "Appearance", subtitle = "Customize the look and feel of your music player.") {
                Text("Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("System", "💻", selectedTheme == "System"),
                        Triple("Light", "☼", selectedTheme == "Light"),
                        Triple("Dark", "☾", selectedTheme == "Dark")
                    ).forEach { (theme, icon, isSelected) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) emeraldAccent else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) Color(0x1010B981) else Color.Transparent)
                                .clickable { selectedTheme = theme },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(icon, fontSize = 14.sp, color = if (isSelected) emeraldAccent else textSecondary)
                                Text(theme, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Color Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette Grid
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
                            isSelected = selectedPaletteIndex == index,
                            onClick = { selectedPaletteIndex = index }
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
                            isSelected = selectedPaletteIndex == realIndex,
                            onClick = { selectedPaletteIndex = realIndex }
                        )
                    }
                }
            }
        }

        // 3. Player Settings Section
        item {
            SettingsCard(title = "Player Settings", subtitle = "Configure playback behavior.") {
                SettingsToggleRow("🎨", "Colourful Player", "Player background adapts to album art.", colorfulPlayer) { colorfulPlayer = it }
                SettingsToggleRow("⏯", "Resume only the first file", "Playback will only resume for the first track.", resumeFirstFile) { resumeFirstFile = it }
                SettingsToggleRow("🔉", "Fade on start", "Gently fade in audio on playback.", fadeOnStart) { fadeOnStart = it }
                SettingsToggleRow("♾", "Gapless Playback", "Removes pauses between tracks.", gaplessPlayback) { gaplessPlayback = it }

                SettingsToggleRow("⇹", "Crossfade", "Adjust the fade duration between tracks.", crossfadeEnabled) { crossfadeEnabled = it }
                if (crossfadeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = crossfadeSeconds,
                            onValueChange = { crossfadeSeconds = it },
                            valueRange = 0f..12f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = emeraldAccent, activeTrackColor = emeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${crossfadeSeconds.toInt()}s", fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }

        // 4. Audio Section
        item {
            SettingsCard(title = "Audio", subtitle = "Adjust audio playback settings.") {
                SettingsToggleRow("📶", "Lossless Audio", "Use Dolby Atmos and Hi-Res Audio.", losslessAudio) { losslessAudio = it }
                SettingsToggleRow("🔊", "Volume Normalization", "Set the same loudness level for all tracks.", volumeNormalization) { volumeNormalization = it }

                // Volume Boost
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Text("Volume Boost", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Text("Increase the maximum volume.", fontSize = 12.sp, color = textSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = volumeBoost,
                            onValueChange = { volumeBoost = it },
                            valueRange = 0f..150f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = emeraldAccent, activeTrackColor = emeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${volumeBoost.toInt()}%", fontSize = 12.sp, color = textSecondary)
                    }
                }

                SettingsToggleRow("🎚", "Mono Audio", "Combine left and right channels.", monoAudio) { monoAudio = it }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Audio Output", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Pair("Phone", "📱"),
                        Pair("Speaker", "🔊"),
                        Pair("Buds", "🎧")
                    ).forEach { (output, icon) ->
                        val isSelected = audioOutput == output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) emeraldAccent else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) Color(0x1010B981) else Color.Transparent)
                                .clickable { audioOutput = output },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(output, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Equalizer Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text("Fine-tune your audio experience.", fontSize = 12.sp, color = textSecondary)
                    }
                    Switch(
                        checked = equalizerEnabled,
                        onCheckedChange = { equalizerEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = emeraldAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original", fontSize = 12.sp, color = textSecondary)
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adjust", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // 5. Content Section
        item {
            SettingsCard(title = "Content", subtitle = "Manage what music appears in your library.") {
                ContentManageRow("Hide Folders", "Exclude specific folders from your library.")
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ContentManageRow("Hide Audio", "Hide specific audio files from your library.")
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

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
fun ContentManageRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Manage", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
