package com.melovish.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
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
    val accent = manager.accentColor
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val borderColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFECEFF3)

    var showHideFoldersDialog by remember { mutableStateOf(false) }
    var showHideAudioDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Curved Glass Back Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(isDark = isDark, onClick = onBackClick)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Profile Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onOpenProfile() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF030712))
                        .border(2.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!manager.profileImagePath.isNullOrBlank() && File(manager.profileImagePath!!).exists()) {
                        AsyncImage(
                            model = File(manager.profileImagePath!!),
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
                    Text(
                        text = manager.profileName,
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (manager.profileEmail.isNotBlank()) manager.profileEmail else "Personal Account & Preferences",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Theme & Accent Color Palette
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Appearance & Theme",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Theme Mode Selector: System, Light, Dark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("System", "Light", "Dark").forEach { mode ->
                        val isSel = manager.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accent else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable { manager.setTheme(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSel) Color.White else textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Accent Color",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // 10 Color Presets + Rainbow Custom Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    manager.userSavedColorPresets.take(5).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (accent == color) Color.White else Color.Transparent, CircleShape)
                                .clickable { manager.updateAccent(color) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showColorPickerDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }

        // Full Screen Pager Transition Effect
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Player Transition Effect",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Full screen album card swipe animation style",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )

                var showTransitionMenu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showTransitionMenu = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = manager.pagerTransitionEffect.name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() },
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("▼", color = accent, fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = showTransitionMenu,
                        onDismissRequest = { showTransitionMenu = false }
                    ) {
                        PagerTransitionEffect.values().forEach { effect ->
                            DropdownMenuItem(
                                text = { Text(effect.name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    manager.setPagerTransition(effect)
                                    showTransitionMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Player Settings Section: "Always play" Integrated at the Top
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Player Settings",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Feature: Always play (Placed Above Colourful Player)[span_0](start_span)[span_0](end_span)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Always play",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Always play audio in background regardless of anything else being played",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isAlwaysPlay,
                        onCheckedChange = { manager.toggleAlwaysPlay(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Colourful Player (Material You)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Colourful Player",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Extract dynamic colors from album artwork",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isColorfulPlayer,
                        onCheckedChange = {
                            manager.isColorfulPlayer = it
                            manager.prefs.edit().putBoolean("colorful_player", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Fade In on Start
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fade on Play",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smoothly fade in volume when playback begins",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isFadeOnStart,
                        onCheckedChange = {
                            manager.isFadeOnStart = it
                            manager.prefs.edit().putBoolean("fade_start", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Gapless Playback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gapless Playback",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Continuous music stream without track silence",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isGaplessEnabled,
                        onCheckedChange = {
                            manager.isGaplessEnabled = it
                            manager.prefs.edit().putBoolean("gapless", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Crossfade
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Crossfade",
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Blend tracks into each other (${String.format(Locale.US, "%.1fs", manager.crossfadeDuration)})",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = manager.isCrossfadeEnabled,
                            onCheckedChange = {
                                manager.isCrossfadeEnabled = it
                                manager.prefs.edit().putBoolean("crossfade_enabled", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent
                            )
                        )
                    }
                    if (manager.isCrossfadeEnabled) {
                        Slider(
                            value = manager.crossfadeDuration,
                            onValueChange = {
                                manager.crossfadeDuration = it
                                manager.prefs.edit().putFloat("crossfade_duration", it).apply()
                            },
                            valueRange = 1.0f..12.0f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }
            }
        }

        // Audio & Hardware Routing Section[span_1](start_span)[span_1](end_span)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Audio & Output Routing",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Lossless Audio Decoding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lossless Audio Decoding",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hi-Res FLAC, WAV, ALAC 24-bit 96kHz playback",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isLosslessEnabled,
                        onCheckedChange = {
                            manager.isLosslessEnabled = it
                            manager.prefs.edit().putBoolean("lossless", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Volume Normalization (ReplayGain)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Volume Normalization",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Equalize loudness levels across different songs",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isVolumeNormalized,
                        onCheckedChange = { manager.toggleVolumeNormalization(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Volume Boost
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Volume Boost: ${manager.volumeBoostLevel.toInt()}%",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = manager.volumeBoostLevel,
                        onValueChange = { manager.setVolumeBoost(it) },
                        valueRange = 100f..200f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                // Mono Audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mono Audio",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Combine left and right audio channels",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = manager.isMonoAudio,
                        onCheckedChange = { manager.toggleMonoAudio(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                // Audio Output Hardware Routing[span_2](start_span)[span_2](end_span)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Audio Output",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Phone", "📱 Phone", "Built-in Speaker"),
                            Triple("Speaker", "🔊 Speaker", "Ext / BT Speaker"),
                            Triple("Buds", "🎧 Buds", "Earphones / BT")
                        ).forEach { (outputKey, label, _) ->
                            val isSel = manager.selectedAudioOutput == outputKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) accent.copy(alpha = 0.15f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isSel) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { manager.setAudioOutputRouting(outputKey) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) accent else textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Equalizer Access Row[span_3](start_span)[span_3](end_span)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable { onOpenEqualizer() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Equalizer & Audio FX", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(if (manager.isEqEnabled) manager.selectedEqPreset else "Off", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onOpenEqualizer,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Adjust", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Content Manager: Hide Folders & Hide Audio[span_4](start_span)[span_4](end_span)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Content Manager",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Folders", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Exclude specific folders from library", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showHideFoldersDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Audio", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Hide specific audio tracks from library", color = Color(0xFF64748B), fontSize = 12.sp)
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

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showHideFoldersDialog) {
        HideFoldersManagementDialog(manager = manager, isDark = isDark, onDismiss = { showHideFoldersDialog = false })
    }

    if (showHideAudioDialog) {
        HideAudioManagementDialog(manager = manager, isDark = isDark, onDismiss = { showHideAudioDialog = false })
    }

    if (showColorPickerDialog) {
        CircularColorPickerDialog(manager = manager, onDismiss = { showColorPickerDialog = false })
    }
}

// Dialog: Hide Folders Management
@Composable
fun HideFoldersManagementDialog(manager: MusicManager, isDark: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val allDeviceFolders = remember {
        manager.rawStorageSongs.map { it.folderName }.distinct().sorted()
    }

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
                .fillMaxHeight(0.70f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Exclude Folders", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Select folders to hide from playback", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Done")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allDeviceFolders, key = { it }) { folder ->
                        val isHidden = folder in manager.hiddenFolders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable { manager.toggleHideFolder(folder) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📁 $folder", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isHidden) "Hidden ✕" else "Visible ✓",
                                color = if (isHidden) Color(0xFFEF4444) else manager.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Hide Audio Files Management
@Composable
fun HideAudioManagementDialog(manager: MusicManager, isDark: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var searchAudio by remember { mutableStateOf("") }
    val candidateSongs = remember(searchAudio, manager.rawStorageSongs.size) {
        val q = searchAudio.trim()
        if (q.isEmpty()) manager.rawStorageSongs.take(100)
        else manager.rawStorageSongs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
    }

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
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hide Specific Audio", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Tap track to hide or restore", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Done")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchAudio,
                    onValueChange = { searchAudio = it },
                    placeholder = { Text("Search songs...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(candidateSongs, key = { it.id }) { song ->
                        val isHidden = song.id in manager.hiddenAudioIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable { manager.toggleHideAudio(song.id) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text(
                                text = if (isHidden) "Hidden ✕" else "Visible ✓",
                                color = if (isHidden) Color(0xFFEF4444) else manager.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
