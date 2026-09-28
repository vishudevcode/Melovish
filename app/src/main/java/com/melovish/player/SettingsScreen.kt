package com.melovish.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
                    .clickable {
                        manager.triggerHapticFeedback(false)
                        onOpenProfile()
                    }
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
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.setTheme(mode)
                                },
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
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.updateAccent(col)
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                manager.triggerHapticFeedback(false)
                                showCircularPicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }

        // 3. Player Settings Section
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
                Text("Configure playback behavior.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow(
                    icon = "🔄",
                    title = "Always play",
                    subtitle = "Always play audio in background",
                    checked = manager.isAlwaysPlay,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.toggleAlwaysPlay(it)
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "🎨",
                    title = "Colourful Player",
                    subtitle = "Player background adapts to album art.",
                    checked = manager.isColorfulPlayer,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.isColorfulPlayer = it
                    manager.prefs.edit().putBoolean("colorful_player", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "⏯️",
                    title = "Resume only the first file",
                    subtitle = "Playback will only resume for the first track.",
                    checked = manager.isResumeFirstOnly,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
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
                    manager.triggerHapticFeedback(false)
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
                    manager.triggerHapticFeedback(false)
                    manager.isGaplessEnabled = it
                    manager.prefs.edit().putBoolean("gapless", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "↔️",
                    title = "Crossfade",
                    subtitle = "Adjust the fade duration between tracks.",
                    checked = manager.isCrossfadeEnabled,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.isCrossfadeEnabled = it
                    manager.prefs.edit().putBoolean("crossfade_enabled", it).apply()
                }

                if (manager.isCrossfadeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duration", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${manager.crossfadeDuration.toInt()}s", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = manager.crossfadeDuration,
                            onValueChange = {
                                val rounded = it.toInt()
                                if (rounded.toFloat() != manager.crossfadeDuration) {
                                    manager.triggerHapticFeedback(false)
                                    manager.crossfadeDuration = rounded.toFloat()
                                    manager.prefs.edit().putFloat("crossfade_duration", rounded.toFloat()).apply()
                                }
                            },
                            valueRange = 1f..12f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Swipe Transition Effect", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Select full-screen horizontal track change animation.", color = Color(0xFF64748B), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PagerTransitionEffect.values()) { effect ->
                        val isSel = manager.pagerTransitionEffect == effect
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.setPagerTransition(effect)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = effect.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSel) Color.White else textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Audio Section Directly On Page
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

                SettingSwitchRow(
                    icon = "📶",
                    title = "Lossless Audio",
                    subtitle = "Use Dolby Atmos and Hi-Res Audio.",
                    checked = manager.isLosslessEnabled,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.isLosslessEnabled = it
                    manager.prefs.edit().putBoolean("lossless", it).apply()
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "🔉",
                    title = "Volume Normalization",
                    subtitle = "Set the same loudness level for all tracks.",
                    checked = manager.isVolumeNormalized,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.toggleVolumeNormalization(it)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Volume Boost", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Increase the maximum volume without muffled clipping (${manager.volumeBoostLevel.toInt()}%).", color = Color(0xFF64748B), fontSize = 12.sp)

                Slider(
                    value = manager.volumeBoostLevel,
                    onValueChange = { liveLevel ->
                        val rounded = liveLevel.toInt()
                        if (rounded != manager.volumeBoostLevel.toInt()) {
                            if (rounded % 10 == 0) manager.triggerHapticFeedback(false)
                            manager.setVolumeBoost(liveLevel)
                        }
                    },
                    valueRange = 100f..200f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    icon = "🎚️",
                    title = "Mono Audio",
                    subtitle = "Combine left and right channels.",
                    checked = manager.isMonoAudio,
                    textColor = textColor
                ) {
                    manager.triggerHapticFeedback(false)
                    manager.toggleMonoAudio(it)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Audio Output", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Phone", "📱  Phone", "Built-in Speaker"),
                        Triple("Speaker", "🔊  Speaker", "Ext / BT Speaker"),
                        Triple("Buds", "🎧  Buds", "Earphones / BT")
                    ).forEach { (outputKey, label, _) ->
                        val isHighlighted = manager.effectiveAudioOutput == outputKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isHighlighted) accent.copy(alpha = 0.16f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                                .border(1.5.dp, if (isHighlighted) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    manager.triggerHapticFeedback(true)
                                    if (manager.userSelectedAudioOutput == outputKey) {
                                        manager.setAudioOutputRouting("Auto")
                                    } else {
                                        manager.setAudioOutputRouting(outputKey)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isHighlighted) accent else textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizer", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(if (manager.isEqEnabled) manager.selectedEqPreset else "Off", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                manager.triggerHapticFeedback(false)
                                onOpenEqualizer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Adjust", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Switch(
                            checked = manager.isEqEnabled,
                            onCheckedChange = {
                                manager.triggerHapticFeedback(false)
                                manager.toggleEqualizer(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Folders", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Exclude specific folders from library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            activeSubScreen = "hide_folders"
                        },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Audio", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hide specific audio files from library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            activeSubScreen = "hide_audio"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Haptics & Feedback Section (Placed right below Content Manager)
        item(key = "haptics_section", contentType = "haptics_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Haptics & Feedback", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Configure tactile vibration responses for sliders and controls.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow(
                    icon = "📳",
                    title = "Haptic Feedback",
                    subtitle = "Vibrate when adjusting sliders, switches, and scrubbers",
                    checked = manager.isHapticsEnabled,
                    textColor = textColor
                ) {
                    manager.toggleHaptics(it)
                }
            }
        }
    }

    if (showCircularPicker) {
        CircularColorPickerDialog(
            currentColor = accent,
            isDark = isDark,
            onColorSelected = { col ->
                manager.triggerHapticFeedback(true)
                manager.updateAccent(col)
                showCircularPicker = false
            },
            onDismiss = { showCircularPicker = false }
        )
    }
}

@Composable
fun SettingSwitchRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    textColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Full Screen Manage Folders with matching GlassmorphicFolderIcon and row heights
@UnstableApi
@Composable
fun ManageHiddenFoldersFullScreen(
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val allFolders = remember(manager.rawStorageSongs.size) {
        manager.rawStorageSongs.map { it.folderName }.distinct().sorted()
    }

    val hiddenList = allFolders.filter { it in manager.hiddenFolders }
    val visibleList = allFolders.filter { it !in manager.hiddenFolders }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Manage Folders", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Tap hidden folder to unhide, or visible to hide", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hiddenList.isNotEmpty()) {
                item {
                    Text(
                        text = "Hidden Folders (${hiddenList.size}) — Tap to Restore",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(items = hiddenList, key = { "hidden_$it" }) { folder ->
                    val fColor = manager.getFolderColor(folder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0x1AE53935) else Color.White)
                            .border(1.2.dp, Color(0x66E53935), RoundedCornerShape(18.dp))
                            .clickable {
                                manager.triggerHapticFeedback(false)
                                manager.toggleHideFolder(folder)
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            GlassmorphicFolderIcon(folderColor = fColor, modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(folder, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Excluded • Tap to unhide", color = Color(0xFFE57373), fontSize = 12.sp)
                            }
                        }
                        Text("👁️", fontSize = 18.sp)
                    }
                }
            }

            item {
                Text(
                    text = "Visible Folders (${visibleList.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(items = visibleList, key = { "visible_$it" }) { folder ->
                val fColor = manager.getFolderColor(folder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                        .clickable {
                            manager.triggerHapticFeedback(false)
                            manager.toggleHideFolder(folder)
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        GlassmorphicFolderIcon(folderColor = fColor, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(folder, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Active • Tap to hide", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                    Text("✓", color = Color(0xFFE91E63), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Full Screen Manage Audio Files with matching card aesthetics
@UnstableApi
@Composable
fun ManageHiddenAudioFullScreen(
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var query by remember { mutableStateOf("") }

    val rawSongs = manager.rawStorageSongs
    val hiddenSongs = rawSongs.filter { it.id in manager.hiddenAudioIds }
    val visibleSongs = rawSongs.filter { it.id !in manager.hiddenAudioIds }

    val filteredHidden = remember(query, hiddenSongs) {
        val q = query.trim()
        if (q.isEmpty()) hiddenSongs
        else hiddenSongs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
    }

    val filteredVisible = remember(query, visibleSongs) {
        val q = query.trim()
        if (q.isEmpty()) visibleSongs
        else visibleSongs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Manage Audio Files", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Search and tap files to hide or unhide", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs to hide or unhide...", color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedIndicatorColor = manager.accentColor,
                unfocusedIndicatorColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredHidden.isNotEmpty()) {
                item {
                    Text(
                        text = "Hidden Audio (${filteredHidden.size}) — Tap to Restore",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(items = filteredHidden, key = { "hidden_${it.id}" }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0x1AE53935) else Color.White)
                            .border(1.2.dp, Color(0x66E53935), RoundedCornerShape(18.dp))
                            .clickable {
                                manager.triggerHapticFeedback(false)
                                manager.toggleHideAudio(song.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("🎵", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(song.title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"} • Hidden", color = Color(0xFFE57373), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        Text("👁️", fontSize = 18.sp)
                    }
                }
            }

            item {
                Text(
                    text = "Visible Audio (${filteredVisible.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(items = filteredVisible, key = { "visible_${it.id}" }) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                        .clickable {
                            manager.triggerHapticFeedback(false)
                            manager.toggleHideAudio(song.id)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🎵", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(song.title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    Text("✓", color = Color(0xFFE91E63), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CircularColorPickerDialog(
    currentColor: Color,
    isDark: Boolean,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    val selectedColor = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Accent Color", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("✕", fontSize = 18.sp, color = Color(0xFF64748B), modifier = Modifier.clickable { onDismiss() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change: PointerInputChange, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touch = change.position
                                    val dx = (touch.x - center.x).toDouble()
                                    val dy = (touch.y - center.y).toDouble()
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val radius = (size.width.toFloat() / 2f).toDouble()
                                    if (dist >= radius * 0.65) {
                                        var angle = Math.toDegrees(atan2(dy, dx)).toFloat()
                                        if (angle < 0f) angle += 360f
                                        hue = angle
                                    } else {
                                        val halfInner = (radius * 0.55).toFloat()
                                        val normX = ((touch.x - (center.x - halfInner)) / (halfInner * 2f)).coerceIn(0f, 1f)
                                        val normY = ((touch.y - (center.y - halfInner)) / (halfInner * 2f)).coerceIn(0f, 1f)
                                        sat = normX
                                        value = 1f - normY
                                    }
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f
                        val ringThickness = radius * 0.28f
                        val sweepColors = (0..360 step 30).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
                        drawCircle(brush = Brush.sweepGradient(sweepColors, center), radius = radius - (ringThickness / 2f), style = Stroke(width = ringThickness))

                        val thumbRad = Math.toRadians(hue.toDouble())
                        val thumbDist = (radius - (ringThickness / 2f)).toDouble()
                        val thumbPos = Offset((center.x.toDouble() + (thumbDist * cos(thumbRad))).toFloat(), (center.y.toDouble() + (thumbDist * sin(thumbRad))).toFloat())
                        drawCircle(Color.White, radius = 12.dp.toPx(), center = thumbPos, style = Stroke(3.dp.toPx()))
                        drawCircle(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), radius = 9.dp.toPx(), center = thumbPos)

                        val halfBox = (radius * 0.55f)
                        val boxTopLeft = Offset(center.x - halfBox, center.y - halfBox)
                        val boxSize = Size(halfBox * 2f, halfBox * 2f)
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))), topLeft = boxTopLeft, size = boxSize)
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), topLeft = boxTopLeft, size = boxSize)
                        val targetPos = Offset(boxTopLeft.x + (sat * boxSize.width), boxTopLeft.y + ((1f - value) * boxSize.height))
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = targetPos, style = Stroke(2.5f.dp.toPx()))
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
