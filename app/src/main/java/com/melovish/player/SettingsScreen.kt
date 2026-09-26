package com.melovish.player

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.io.File
import java.util.Locale

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
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    when (activeSubScreen) {
        "audio_manager" -> {
            AudioManagerSubScreen(
                manager = manager,
                isDark = isDark,
                onBack = { activeSubScreen = null },
                onOpen32BandEq = { activeSubScreen = "eq_32_band" }
            )
            return
        }
        "eq_32_band" -> {
            Equalizer32BandScreen(manager = manager, isDark = isDark, onBack = { activeSubScreen = "audio_manager" })
            return
        }
        "hide_folders" -> {
            ManageHiddenFoldersFullScreen(manager = manager, isDark = isDark, onBack = { activeSubScreen = null })
            return
        }
        "hide_audio" -> {
            ManageHiddenAudioFullScreen(manager = manager, isDark = isDark, onBack = { activeSubScreen = null })
            return
        }
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

        // Profile Section
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

        // Dedicated Audio Manager Hub Card
        item(key = "audio_manager_section", contentType = "hub_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.2.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .clickable { activeSubScreen = "audio_manager" }
                    .padding(18.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎛️", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Manager", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("32-Band EQ, Bit-Perfect DAC, Gapless & ReplayGain Normalisation", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("›", fontSize = 24.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Appearance Section
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

        // Fully Functional Player Settings Card
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

                // 1. Colourful Player Toggle
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

                // 2. Fade on start Toggle
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

                // 3. Gapless Playback Toggle
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

                // 4. Crossfade Card: Toggle on Right Side, Duration Slider & Track Below
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

        // Content Manager Section
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
}

// -----------------------------------------------------------------------------------------
// Sub-Screen: Audio Manager (32-Band EQ, Hi-Res DAC, ReplayGain Normalisation, Reverb)
// -----------------------------------------------------------------------------------------

@UnstableApi
@Composable
fun AudioManagerSubScreen(
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit,
    onOpen32BandEq: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val accent = manager.accentColor
    var showReverbMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Audio Manager", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("32-Band EQ, External DAC & ReplayGain Normalisation", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Equalizer and Sound Effects
            item(key = "eq_and_sound_fx") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("Equalizer and Sound Effects", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Multi-band graphic EQ (32-band EQ), bass boost, virtualizer, and reverb.", color = Color(0xFF64748B), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("32-Band Equalizer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("20 Hz to 20 kHz ISO 1/3 Octave precision", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Button(
                            onClick = onOpen32BandEq,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("Open 32-Band EQ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Environmental Reverb", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Acoustic simulation: ${manager.selectedReverbPreset.label}", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box {
                            Button(
                                onClick = { showReverbMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Preset ▾", color = textColor, fontSize = 12.sp)
                            }
                            DropdownMenu(expanded = showReverbMenu, onDismissRequest = { showReverbMenu = false }) {
                                ReverbPresetMode.values().forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.label) },
                                        onClick = {
                                            manager.applyReverbPreset(preset)
                                            showReverbMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Bass Boost: ${manager.bassBoostPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = manager.bassBoostPercent.toFloat(),
                        onValueChange = { manager.setBassBoost(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("3D Virtualizer: ${manager.virtualizerPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = manager.virtualizerPercent.toFloat(),
                        onValueChange = { manager.setVirtualizer(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            }

            // 2. High-Resolution Audio Support & External DAC
            item(key = "hi_res_dac_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("High-Resolution Audio Support", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Bit-perfect playback, external DAC integration, and support for FLAC, ALAC, WAV, and DSD.", color = Color(0xFF64748B), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (manager.isExternalDacConnected) Color(0x1A10B981) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (manager.isExternalDacConnected) "🔌" else "🎧", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (manager.isExternalDacConnected) "External DAC Connected" else "Internal Output Device",
                                color = if (manager.isExternalDacConnected) Color(0xFF10B981) else textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = manager.connectedDacName ?: "Standard Hardware Audio Subsystem",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = "⚡",
                        title = "Bit-Perfect Playback",
                        subtitle = "Bypasses system float resampling directly to external USB DACs.",
                        checked = manager.isBitPerfectEnabled,
                        textColor = textColor
                    ) {
                        manager.toggleBitPerfect(it)
                    }
                }
            }

            // 3. Gapless Playback
            item(key = "audio_manager_gapless") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("Gapless Playback", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Eliminates silent gaps between consecutive tracks for live albums and mixes.", color = Color(0xFF64748B), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "♾️",
                        title = "Gapless Playback",
                        subtitle = "Seamless contiguous track stitching without pauses.",
                        checked = manager.isGaplessEnabled,
                        textColor = textColor
                    ) {
                        manager.isGaplessEnabled = it
                        manager.prefs.edit().putBoolean("gapless", it).apply()
                    }
                }
            }

            // 4. Volume Normalisation (ReplayGain Loudness Mapping)
            item(key = "replay_gain_volume_norm") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("ReplayGain Loudness Mapping", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Automatically adjusts playback volume so all songs play at consistent loudness.", color = Color(0xFF64748B), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "🔉",
                        title = "Volume Normalisation",
                        subtitle = "Enable ReplayGain target loudness matching across all songs.",
                        checked = manager.isVolumeNormalized,
                        textColor = textColor
                    ) {
                        manager.toggleVolumeNormalization(it)
                    }

                    if (manager.isVolumeNormalized) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ReplayGainMode.OFF to "Off",
                                ReplayGainMode.TRACK to "Track Gain",
                                ReplayGainMode.ALBUM to "Album Gain"
                            ).forEach { (mode, label) ->
                                val isSel = manager.replayGainMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) accent.copy(alpha = 0.15f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                                        .border(1.2.dp, if (isSel) accent else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { manager.setReplayGainSettings(mode, manager.replayGainPreampDb) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSel) accent else textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Preamp Gain Adjustment", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.US, "%.1f dB", manager.replayGainPreampDb), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = manager.replayGainPreampDb,
                            onValueChange = { manager.setReplayGainSettings(manager.replayGainMode, it) },
                            valueRange = -6.0f..6.0f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Sub-Screen: 32-Band Equalizer Frequency Editor
// -----------------------------------------------------------------------------------------

@Composable
fun Equalizer32BandScreen(manager: MusicManager, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("32-Band Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("20 Hz to 20 kHz Precision Response", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Button(
                onClick = {
                    for (i in 0 until 32) {
                        manager.set32BandLevel(i, 0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reset", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = EQUALIZER_32_BANDS,
                key = { index, _ -> index },
                contentType = { _, _ -> "eq_32_band_row" }
            ) { index, bandFreq ->
                val level = manager.eq32BandLevels[index] ?: 0
                val levelDb = level / 100
                val levelText = if (levelDb > 0) "+$levelDb dB" else "$levelDb dB"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF131B2E) else Color.White)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bandFreq, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(levelText, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { manager.set32BandLevel(index, it.toInt()) },
                        valueRange = -1500f..1500f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            }
        }
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

// Fixed Destructuring Syntax in ManageHiddenAudioFullScreen
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
