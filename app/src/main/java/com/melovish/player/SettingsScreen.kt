package com.melovish.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.io.File

@UnstableApi
@Composable
fun SettingsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val accent = manager.accentColor

    var showCircularPicker by remember { mutableStateOf(false) }
    var showEqualizerModal by remember { mutableStateOf(false) }
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeSubScreen != null || showEqualizerModal) {
        if (showEqualizerModal) {
            showEqualizerModal = false
        } else {
            activeSubScreen = null
        }
    }

    if (activeSubScreen == "hide_folders") {
        ManageHiddenFoldersFullScreen(manager = manager, isDark = isDark, onBack = { activeSubScreen = null })
        return
    }

    if (activeSubScreen == "hide_audio") {
        ManageHiddenAudioFullScreen(manager = manager, isDark = isDark, onBack = { activeSubScreen = null })
        return
    }

    val avatarPath = manager.profileImagePath

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_header", contentType = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(isDark = isDark, onClick = onBackClick)
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = "Settings", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            }
        }

        // 1. Profile Section
        item(key = "profile_section", contentType = "profile_card") {
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
                            .background(Color(0xFF030712))
                            .border(2.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
                            AsyncImage(
                                model = File(avatarPath),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            DefaultProfileAvatar(modifier = Modifier.fillMaxSize())
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

        // 2. Appearance Section
        item(key = "appearance_section", contentType = "appearance_card") {
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
                                .background(if (isSel) accent.copy(alpha = 0.15f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
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
                    val colors = listOf(Color(0xFF00B4D8), Color(0xFF2EC4B6), Color(0xFF39FF14), Color(0xFFFF2A85), Color(0xFFFF3B30))
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

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showCircularPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }

        // 3. Player Settings Section (With Gapless Playback & Crossfade)
        item(key = "player_settings_section", contentType = "player_settings_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Player Settings", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Configure playback and transition behaviors.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow(
                    icon = "🎨",
                    title = "Colourful Player",
                    subtitle = "Toggles dynamic Material You colors extracted from album art.",
                    checked = manager.isColorfulPlayer,
                    textColor = textColor
                ) {
                    manager.isColorfulPlayer = it
                    manager.prefs.edit().putBoolean("colorful_player", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "⏯️",
                    title = "Resume only the first file",
                    subtitle = "Playback will only resume for the first track in storage.",
                    checked = manager.isResumeFirstOnly,
                    textColor = textColor
                ) {
                    manager.isResumeFirstOnly = it
                    manager.prefs.edit().putBoolean("resume_first", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "🔊",
                    title = "Fade on start",
                    subtitle = "Gently fades in audio when playback begins.",
                    checked = manager.isFadeOnStart,
                    textColor = textColor
                ) {
                    manager.isFadeOnStart = it
                    manager.prefs.edit().putBoolean("fade_start", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "♾️",
                    title = "Gapless Playback",
                    subtitle = "Removes silent pauses between consecutive tracks.",
                    checked = manager.isGaplessEnabled,
                    textColor = textColor
                ) {
                    manager.isGaplessEnabled = it
                    manager.prefs.edit().putBoolean("gapless", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("↔️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Crossfade", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Adjust the fade duration between tracks.", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = manager.isCrossfadeEnabled,
                            onCheckedChange = {
                                manager.isCrossfadeEnabled = it
                                manager.prefs.edit().putBoolean("crossfade_enabled", it).apply()
                            }
                        )
                    }

                    if (manager.isCrossfadeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duration", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${manager.crossfadeDuration.toInt()}s", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = manager.crossfadeDuration,
                            onValueChange = {
                                manager.crossfadeDuration = it
                                manager.prefs.edit().putFloat("crossfade_duration", it).apply()
                            },
                            valueRange = 1f..12f,
                            steps = 10,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }
            }
        }

        // 4. Audio Section Directly On Page (Matching 2nd Photo exactly)
        item(key = "audio_section_direct", contentType = "audio_card") {
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

                // Lossless Audio Toggle
                SettingSwitchRow(
                    icon = "📶",
                    title = "Lossless Audio",
                    subtitle = "Use Dolby Atmos and Hi-Res Audio.",
                    checked = manager.isLosslessEnabled,
                    textColor = textColor
                ) {
                    manager.isLosslessEnabled = it
                    manager.prefs.edit().putBoolean("lossless", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Volume Normalization (Mapped to ReplayGain)
                SettingSwitchRow(
                    icon = "🔉",
                    title = "Volume Normalization",
                    subtitle = "Set the same loudness level for all tracks.",
                    checked = manager.isVolumeNormalized,
                    textColor = textColor
                ) {
                    manager.toggleVolumeNormalization(it)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Volume Boost Slider
                Text("Volume Boost", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Increase the maximum volume (${manager.volumeBoostLevel.toInt()}%).", color = Color(0xFF64748B), fontSize = 12.sp)

                var boostSliderVal by remember(manager.volumeBoostLevel) { mutableFloatStateOf(manager.volumeBoostLevel) }
                Slider(
                    value = boostSliderVal,
                    onValueChange = { boostSliderVal = it },
                    onValueChangeFinished = {
                        manager.volumeBoostLevel = boostSliderVal
                        manager.attachAudioEffects()
                    },
                    valueRange = 100f..200f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mono Audio Toggle
                SettingSwitchRow(
                    icon = "🎚️",
                    title = "Mono Audio",
                    subtitle = "Combine left and right channels.",
                    checked = manager.isMonoAudio,
                    textColor = textColor
                ) {
                    manager.isMonoAudio = it
                    manager.prefs.edit().putBoolean("mono", it).apply()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Output Routing (Phone, Speaker, Buds)
                Text("Audio Output", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Phone", "📱", "Phone"),
                        Triple("Speaker", "🔊", "Speaker"),
                        Triple("Buds", "🎧", "Buds")
                    ).forEach { (key, icon, label) ->
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

                // Equalizer Row with Adjust Button and Switch Toggle
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
                            onClick = { showEqualizerModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Adjust", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = manager.isEqEnabled,
                            onCheckedChange = { manager.toggleEqualizer(it) }
                        )
                    }
                }
            }
        }

        // 5. Content Manager Section
        item(key = "content_manager_section", contentType = "content_manager_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Content Manager", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Manage what music appears in your library.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Hide Folders", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Exclude specific folders from library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { activeSubScreen = "hide_folders" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Hide Audio", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hide specific audio files from library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { activeSubScreen = "hide_audio" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCircularPicker) CircularColorPickerDialog(manager = manager, onDismiss = { showCircularPicker = false })

    // Equalizer Sheet with Vertical Sliders and Save Button
    if (showEqualizerModal) {
        VerticalLinesEqualizerSheet(manager = manager, onDismiss = { showEqualizerModal = false })
    }
}

// -----------------------------------------------------------------------------------------
// Equalizer Modal Sheet: Vertical Faders, Sound Presets, Bass Boost, 3D Virtualizer & Save
// -----------------------------------------------------------------------------------------

@Composable
fun VerticalLinesEqualizerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Save Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(manager.selectedEqPreset, fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sound Presets Row
                    item {
                        Text("Sound Presets", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(manager.eqPresetNames, key = { it }) { preset ->
                                val isSel = manager.selectedEqPreset == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                                        .clickable { manager.applyEqPreset(preset) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        color = if (isSel) Color.White else textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Multi-band Graphic EQ in Vertical Lines
                    item {
                        Text("Frequency Response (-15dB to +15dB)", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until manager.eqBandsCount) {
                                    val freq = manager.eqCenterFreqs[i] ?: (60 * (i + 1) * (i + 1))
                                    val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                                    val level = manager.eqBandLevels[i] ?: 0
                                    val levelDb = level / 100

                                    VerticalBandFader(
                                        level = level,
                                        minLevel = manager.eqMinLevel,
                                        maxLevel = manager.eqMaxLevel,
                                        dbLabel = if (levelDb > 0) "+$levelDb" else "$levelDb",
                                        freqLabel = freqLabel,
                                        accentColor = accent,
                                        isDark = isDark,
                                        onLevelChange = { newLevel ->
                                            manager.setEqBandLevel(i, newLevel)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bass Boost & Virtualizer
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .padding(16.dp)
                        ) {
                            Text("Bass Boost: ${manager.bassBoostPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = manager.bassBoostPercent.toFloat(),
                                onValueChange = { manager.setBassBoost(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("3D Surround (Virtualizer): ${manager.virtualizerPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = manager.virtualizerPercent.toFloat(),
                                onValueChange = { manager.setVirtualizer(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Vertical Line Mixer Slider for Hardware Frequency Bands
@Composable
fun VerticalBandFader(
    level: Int,
    minLevel: Int,
    maxLevel: Int,
    dbLabel: String,
    freqLabel: String,
    accentColor: Color,
    isDark: Boolean,
    onLevelChange: (Int) -> Unit
) {
    val totalRange = (maxLevel - minLevel).coerceAtLeast(1)
    val fraction = ((level - minLevel).toFloat() / totalRange.toFloat()).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(42.dp)
    ) {
        Text(
            text = dbLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(140.dp)
                .pointerInput(minLevel, maxLevel) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val trackHeight = size.height.toFloat()
                        val newFraction = 1f - (down.position.y / trackHeight).coerceIn(0f, 1f)
                        onLevelChange((minLevel + newFraction * totalRange).toInt())

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                val moveFraction = 1f - (change.position.y / trackHeight).coerceIn(0f, 1f)
                                onLevelChange((minLevel + moveFraction * totalRange).toInt())
                            } else {
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val topY = 8.dp.toPx()
                val bottomY = size.height - 8.dp.toPx()
                val trackHeight = bottomY - topY
                val knobY = bottomY - (fraction * trackHeight)

                // Background Vertical Track Line
                drawLine(
                    color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                    start = Offset(centerX, topY),
                    end = Offset(centerX, bottomY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active Vertical Track Line (From bottom up to knob)
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, bottomY),
                    end = Offset(centerX, knobY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Fader Knob Ball
                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, knobY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = freqLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

// -----------------------------------------------------------------------------------------
// Sub-Screens: Content Management (Hidden Folders and Hidden Audio)
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun ManageHiddenFoldersFullScreen(manager: MusicManager, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White

    val allFolders = remember(manager.allSongs.size) {
        manager.allSongs.map { it.folderName }.distinct()
    }
    val hiddenSet = remember(manager.hiddenFolders.size, manager.hiddenFolders.toList()) {
        manager.hiddenFolders.toHashSet()
    }
    val hiddenFolders: ImmutableList<String> = remember(hiddenSet) { hiddenSet.toList().toImmutableList() }
    val visibleFolders: ImmutableList<String> = remember(allFolders, hiddenSet) {
        allFolders.filter { it !in hiddenSet }.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Manage Folders", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("Tap hidden folder to unhide, or visible to hide", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hiddenFolders.isNotEmpty()) {
                item(key = "hidden_folders_header", contentType = "section_header") {
                    Text(
                        text = "Hidden Folders (${hiddenFolders.size}) — Tap to Restore",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(
                    items = hiddenFolders,
                    key = { "hidden_$it" },
                    contentType = { "hidden_folder_row" }
                ) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.5.dp, Color(0x66EF4444), RoundedCornerShape(16.dp))
                            .clickable { manager.toggleHideFolder(folder) }
                            .padding(14.dp)
                            .alpha(0.45f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassmorphicFolderIcon(folderColor = manager.getFolderColor(folder), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Excluded • Tap to unhide", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("👁️‍🗨️", fontSize = 18.sp)
                    }
                }
            }

            item(key = "visible_folders_header", contentType = "section_header") {
                Text(
                    text = "Visible Folders (${visibleFolders.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(
                items = visibleFolders,
                key = { "visible_$it" },
                contentType = { "visible_folder_row" }
            ) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(16.dp))
                        .clickable { manager.toggleHideFolder(folder) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicFolderIcon(folderColor = manager.getFolderColor(folder), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Active • Tap to hide", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Text("✓", color = manager.accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun ManageHiddenAudioFullScreen(manager: MusicManager, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var searchQuery by remember { mutableStateOf("") }

    val hiddenSet = remember(manager.hiddenAudioIds.size, manager.hiddenAudioIds.toList()) {
        manager.hiddenAudioIds.toHashSet()
    }

    val (hiddenSongs, visibleSongs) = remember(
        searchQuery,
        manager.allSongs.size,
        hiddenSet
    ) {
        val q = searchQuery.trim()
        val filtered = if (q.isEmpty()) {
            manager.allSongs
        } else {
            manager.allSongs.filter {
                it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
            }
        }
        val hidden = ArrayList<Song>()
        val visible = ArrayList<Song>()
        for (song in filtered) {
            if (hiddenSet.contains(song.id)) {
                hidden.add(song)
            } else {
                visible.add(song)
            }
        }
        hidden.toImmutableList() to visible.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Manage Audio Files", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("Search and tap files to hide or unhide", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs to hide or unhide...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hiddenSongs.isNotEmpty()) {
                item(key = "hidden_audio_header", contentType = "section_header") {
                    Text(
                        text = "Hidden Audio (${hiddenSongs.size}) — Tap to Restore",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(
                    items = hiddenSongs,
                    key = { "hidden_${it.id}" },
                    contentType = { "hidden_audio_row" }
                ) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(1.2.dp, Color(0x66EF4444), RoundedCornerShape(14.dp))
                            .clickable { manager.toggleHideAudio(song.id) }
                            .padding(12.dp)
                            .alpha(0.45f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚫", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatFileSize(song.size)} • ${song.artist} • Excluded", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }
            }

            item(key = "visible_audio_header", contentType = "section_header") {
                Text(
                    text = "Visible Audio (${visibleSongs.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(
                items = visibleSongs,
                key = { "visible_${it.id}" },
                contentType = { "visible_audio_row" }
            ) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(14.dp))
                        .clickable { manager.toggleHideAudio(song.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Text("✓", color = manager.accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(icon: String, title: String, subtitle: String, checked: Boolean, textColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
