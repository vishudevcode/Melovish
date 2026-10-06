package com.melovish.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun SettingsScreen(
    manager: MusicManager,
    onBackClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onDarkSubStyleClick: () -> Unit = {},
    onLightSubStyleClick: () -> Unit = {},
    onOpenAccentPicker: () -> Unit = {}
) {
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val accent = manager.accentColor

    var showCircularPicker by remember { mutableStateOf(false) }
    var isPickingDarkHue by remember { mutableStateOf(false) }
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    var isTransitionEnabled by remember {
        mutableStateOf(manager.prefs.getBoolean("pager_transition_enabled", true))
    }
    var rememberedCustomTransition by remember {
        mutableStateOf(
            try {
                val saved = manager.prefs.getString("pager_transition_custom_saved", PagerTransitionEffect.CASCADE.name)
                PagerTransitionEffect.valueOf(saved ?: PagerTransitionEffect.CASCADE.name)
            } catch (_: Exception) {
                PagerTransitionEffect.CASCADE
            }
        )
    }

    var isVolumeBoostEnabled by remember {
        mutableStateOf(manager.prefs.getBoolean("vol_boost_enabled", false))
    }
    var rememberedVolumeBoostLevel by remember {
        mutableFloatStateOf(
            manager.prefs.getFloat("saved_vol_boost_level", 120f).coerceIn(100f, 200f)
        )
    }

    BackHandler(enabled = activeSubScreen != null || showCircularPicker) {
        when {
            showCircularPicker -> showCircularPicker = false
            else -> activeSubScreen = null
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "settings_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(
                        isDark = isDark,
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            onBackClick()
                        }
                    )
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
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
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
                                .shadow(4.dp, CircleShape, spotColor = accent)
                                .clip(CircleShape)
                                .background(manager.getCurrentSurfaceColor())
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
                                DefaultProfileAvatar(
                                    modifier = Modifier.fillMaxSize(),
                                    backgroundColor = Color.Transparent
                                )
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
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Text("Appearance", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Customize theme, style variants, and accent colors.", color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hold Light / Dark for variants", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
                                    .combinedClickable(
                                        onClick = {
                                            manager.triggerHapticFeedback(false)
                                            manager.setTheme(mode)
                                        },
                                        onLongClick = {
                                            manager.triggerHapticFeedback(true)
                                            when (mode) {
                                                "Dark" -> onDarkSubStyleClick()
                                                "Light" -> onLightSubStyleClick()
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(mode, color = if (isSel) accent else textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (mode == "Dark" && isSel) {
                                        Text(
                                            when (manager.darkThemeSubStyle) {
                                                DarkThemeSubStyle.AMOLED_BLACK -> "BLACK"
                                                DarkThemeSubStyle.BLUISH -> "Slate"
                                                DarkThemeSubStyle.CUSTOM -> "Custom"
                                            },
                                            color = accent.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (mode == "Light" && isSel) {
                                        Text(
                                            when (manager.lightThemeSubStyle) {
                                                LightThemeSubStyle.WHITE -> "White"
                                                LightThemeSubStyle.CREAM -> "Cream"
                                                LightThemeSubStyle.CUSTOM -> "Custom"
                                            },
                                            color = accent.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
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
                                    onOpenAccentPicker()
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
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
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
                        textColor = textColor,
                        accentColor = accent
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
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.isColorfulPlayer = it
                        manager.managerScope.launch(Dispatchers.IO) {
                            manager.prefs.edit().putBoolean("colorful_player", it).apply()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "⏭️",
                        title = "Resume the First File",
                        subtitle = "Playback will only resume for the first track.",
                        checked = manager.isResumeFirstOnly,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.isResumeFirstOnly = it
                        manager.managerScope.launch(Dispatchers.IO) {
                            manager.prefs.edit().putBoolean("resume_first", it).apply()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "♾️",
                        title = "Silence Trimming",
                        subtitle = "Skips silent gaps at the end of tracks",
                        checked = manager.isSilenceTrimmingEnabled,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.isSilenceTrimmingEnabled = it
                        manager.managerScope.launch(Dispatchers.IO) {
                            manager.prefs.edit().putBoolean("silence_trimming", it).apply()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "🔊",
                        title = "Fade on start",
                        subtitle = "Gently fades in audio when playback begins.",
                        checked = manager.isFadeOnStart,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.isFadeOnStart = it
                        manager.managerScope.launch(Dispatchers.IO) {
                            manager.prefs.edit().putBoolean("fade_start", it).apply()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "↔️",
                        title = "Crossfade",
                        subtitle = "Adjust the fade duration between tracks.",
                        checked = manager.isCrossfadeEnabled,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.isCrossfadeEnabled = it
                        manager.managerScope.launch(Dispatchers.IO) {
                            manager.prefs.edit().putBoolean("crossfade_enabled", it).apply()
                        }
                    }

                    if (manager.isCrossfadeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Duration", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${manager.crossfadeDuration.toInt()}s", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = manager.crossfadeDuration,
                                onValueChange = { newValue ->
                                    val targetInt = newValue.roundToInt().coerceIn(1, 12)
                                    val targetFloat = targetInt.toFloat()
                                    if (targetFloat != manager.crossfadeDuration) {
                                        manager.triggerHapticFeedback(false)
                                        manager.crossfadeDuration = targetFloat
                                        manager.managerScope.launch(Dispatchers.IO) {
                                            manager.prefs.edit().putFloat("crossfade_duration", targetFloat).apply()
                                        }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("📲", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Swipe Transition Effect", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("Select track change animation", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = isTransitionEnabled,
                            onCheckedChange = { isEnabled ->
                                manager.triggerHapticFeedback(false)
                                isTransitionEnabled = isEnabled
                                manager.managerScope.launch(Dispatchers.IO) {
                                    manager.prefs.edit().putBoolean("pager_transition_enabled", isEnabled).apply()
                                }

                                if (isEnabled) {
                                    manager.setPagerTransition(rememberedCustomTransition)
                                } else {
                                    manager.setPagerTransition(PagerTransitionEffect.SLIDE)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = isTransitionEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PagerTransitionEffect.values()) { effect ->
                                    val isSel = manager.pagerTransitionEffect == effect
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                                            .clickable {
                                                manager.triggerHapticFeedback(false)
                                                rememberedCustomTransition = effect
                                                manager.managerScope.launch(Dispatchers.IO) {
                                                    manager.prefs.edit().putString("pager_transition_custom_saved", effect.name).apply()
                                                }
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
                }
            }

            // 4. Audio Section
            item(key = "audio_section_direct", contentType = "audio_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
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
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.toggleLosslessAudio(it)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "🔉",
                        title = "Volume Normalization",
                        subtitle = "Set the same loudness level for all tracks.",
                        checked = manager.isVolumeNormalized,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.toggleVolumeNormalization(it)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        icon = "🎚",
                        title = "Mono Audio",
                        subtitle = "Combine left and right channels.",
                        checked = manager.isMonoAudio,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(false)
                        manager.toggleMonoAudio(it)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("📢", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Volume Boost", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isVolumeBoostEnabled) {
                                        "Increase the maximum volume (${manager.volumeBoostLevel.toInt()}%)."
                                    } else {
                                        "Increase the maximum volume beyond 100%."
                                    },
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isVolumeBoostEnabled,
                            onCheckedChange = { isEnabled ->
                                manager.triggerHapticFeedback(false)
                                isVolumeBoostEnabled = isEnabled
                                manager.managerScope.launch(Dispatchers.IO) {
                                    manager.prefs.edit().putBoolean("vol_boost_enabled", isEnabled).apply()
                                }

                                if (isEnabled) {
                                    manager.setVolumeBoost(rememberedVolumeBoostLevel)
                                } else {
                                    manager.setVolumeBoost(100f)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = isVolumeBoostEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = manager.volumeBoostLevel,
                                onValueChange = { liveLevel ->
                                    val rounded = liveLevel.toInt()
                                    if (rounded != manager.volumeBoostLevel.toInt()) {
                                        if (rounded % 10 == 0) manager.triggerHapticFeedback(false)
                                        rememberedVolumeBoostLevel = liveLevel
                                        manager.managerScope.launch(Dispatchers.IO) {
                                            manager.prefs.edit().putFloat("saved_vol_boost_level", liveLevel).apply()
                                        }
                                        manager.setVolumeBoost(liveLevel)
                                    }
                                },
                                valueRange = 100f..200f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Audio Output", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accent
                                )
                            )
                        }
                    }
                }
            }

            // 5. 🌟 Custom Audio Effects Section (Directly Below Audio)
            item(key = "custom_audio_effects_section", contentType = "custom_dsp_card") {
                AudioEffectsSettingsSection(
                    manager = manager,
                    isDark = isDark
                )
            }

            // 6. Frosted Glass Styling Section (Updated Titles & Precision Ticked Slider)
            item(key = "frosted_glass_section", contentType = "frosted_glass_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    SettingSwitchRow(
                        icon = "✨",
                        title = "Frosted Glass Effect",
                        subtitle = "Enable glassmorphism with depth blur",
                        checked = manager.isFrostedGlassEnabled,
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.triggerHapticFeedback(true)
                        manager.toggleFrostedGlass(it)
                    }

                    AnimatedVisibility(
                        visible = manager.isFrostedGlassEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Glass Visibility", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                val label = when {
                                    manager.frostedGlassOpacity <= 0.25f -> "Clear"
                                    manager.frostedGlassOpacity <= 0.70f -> "Frosted"
                                    else -> "Opaque"
                                }
                                Text(
                                    "$label (${(manager.frostedGlassOpacity * 100).toInt()}%)",
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // High-Precision Slider with 5% dots and prominent 0%, 25%, 50%, 75%, 100% anchors
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .padding(horizontal = 10.dp)
                                ) {
                                    val centerY = size.height / 2f
                                    val trackWidth = size.width

                                    for (i in 0..20) {
                                        val frac = i / 20f
                                        val dotX = frac * trackWidth
                                        val isProminent = i == 0 || i == 5 || i == 10 || i == 15 || i == 20

                                        val dotRadius = if (isProminent) 3.5.dp.toPx() else 1.8.dp.toPx()
                                        val dotColor = when {
                                            frac <= manager.frostedGlassOpacity -> if (isProminent) Color.White.copy(alpha = 0.95f) else accent.copy(alpha = 0.70f)
                                            isProminent -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                            else -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                        }

                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(dotX, centerY)
                                        )
                                    }
                                }

                                Slider(
                                    value = manager.frostedGlassOpacity,
                                    onValueChange = { newOpacity ->
                                        val oldInt = (manager.frostedGlassOpacity * 100).toInt()
                                        val newInt = (newOpacity * 100).toInt()
                                        if (newInt != oldInt && newInt % 5 == 0) {
                                            manager.triggerHapticFeedback(false)
                                        }
                                        manager.updateFrostedGlassOpacity(newOpacity)
                                    },
                                    valueRange = 0.0f..1.0f,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = accent,
                                        activeTrackColor = accent.copy(alpha = 0.55f),
                                        inactiveTrackColor = Color.Transparent
                                    )
                                )
                            }

                            // Coordinate-locked bottom labels with 50% "Frosted" dead center
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )

                                Text(
                                    text = "Frosted",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Text(
                                    text = "Opaque",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Haptics & Feedback Section
            item(key = "haptics_section", contentType = "haptics_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
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
                        textColor = textColor,
                        accentColor = accent
                    ) {
                        manager.toggleHaptics(it)
                    }
                }
            }

            // 8. 📁 Content Manager Section (Relocated Below Haptics & Feedback)
            item(key = "content_manager_section", contentType = "content_manager_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
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
        }
    }

    if (showCircularPicker) {
        val initialPickerColor = if (isPickingDarkHue) {
            manager.customDarkFrostedHueColor
        } else {
            if (manager.lightThemeSubStyle == LightThemeSubStyle.CUSTOM) manager.customFrostedHueColor else accent
        }

        CircularColorPickerDialog(
            manager = manager,
            title = if (isPickingDarkHue) "Select Dark Mode Tint" else "Select Accent Color",
            currentColor = initialPickerColor,
            onColorSelected = { col ->
                manager.triggerHapticFeedback(true)
                if (isPickingDarkHue) {
                    manager.setCustomDarkFrostedHue(col)
                    manager.setTheme("Dark")
                } else {
                    manager.updateAccent(col)
                }
                showCircularPicker = false
            },
            onDismiss = { showCircularPicker = false }
        )
    }
}

@Composable
fun DarkThemeVariantDialog(
    manager: MusicManager,
    currentStyle: DarkThemeSubStyle,
    accent: Color,
    isDark: Boolean,
    onSelect: (DarkThemeSubStyle) -> Unit,
    onOpenColorWheel: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = manager.getCurrentTextColor()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Dark Mode Variant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Choose your preferred dark background aesthetic.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Pair(DarkThemeSubStyle.BLUISH, Pair("Midnight Bluish Slate", "Deep slate blue background")),
                    Pair(DarkThemeSubStyle.AMOLED_BLACK, Pair("Pure Pitch Black", "True pure black background"))
                ).forEach { (style, textPair) ->
                    val isSel = currentStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) accent.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.2.dp, if (isSel) accent else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onSelect(style) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(textPair.first, color = if (isSel) accent else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(textPair.second, color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        if (isSel) Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentStyle == DarkThemeSubStyle.CUSTOM) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.2.dp, if (currentStyle == DarkThemeSubStyle.CUSTOM) accent else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onOpenColorWheel() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Custom Dark Frosted Hue", color = if (currentStyle == DarkThemeSubStyle.CUSTOM) accent else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Pick any custom atmosphere colour tint", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                    if (currentStyle == DarkThemeSubStyle.CUSTOM) Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LightThemeVariantDialog(
    manager: MusicManager,
    currentStyle: LightThemeSubStyle,
    accent: Color,
    isDark: Boolean,
    onSelect: (LightThemeSubStyle) -> Unit,
    onOpenColorWheel: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = manager.getCurrentTextColor()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Light Mode Variant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Choose your preferred light background shade or tint.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Pair(LightThemeSubStyle.WHITE, Pair("Pure Porcelain White", "Clean modern white background")),
                    Pair(LightThemeSubStyle.CREAM, Pair("Pale Warm Cream", "Gentle white cream background"))
                ).forEach { (style, textPair) ->
                    val isSel = currentStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) accent.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.2.dp, if (isSel) accent else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onSelect(style) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(textPair.first, color = if (isSel) accent else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(textPair.second, color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        if (isSel) Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentStyle == LightThemeSubStyle.CUSTOM) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.2.dp, if (currentStyle == LightThemeSubStyle.CUSTOM) accent else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onOpenColorWheel() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Custom Frosted Hue", color = if (currentStyle == LightThemeSubStyle.CUSTOM) accent else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Pick any custom atmosphere colour tint", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                    if (currentStyle == LightThemeSubStyle.CUSTOM) Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    textColor: Color,
    accentColor: Color,
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}

@UnstableApi
@Composable
fun ManageHiddenFoldersFullScreen(
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
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
            GlassBackButton(
                isDark = isDark,
                onClick = {
                    manager.triggerHapticFeedback(false)
                    onBack()
                }
            )
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
                        Text("👁", fontSize = 18.sp)
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
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(18.dp))
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

@UnstableApi
@Composable
fun ManageHiddenAudioFullScreen(
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val accent = manager.accentColor
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
            GlassBackButton(
                isDark = isDark,
                onClick = {
                    manager.triggerHapticFeedback(false)
                    onBack()
                }
            )
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = accent,
                unfocusedBorderColor = manager.getCurrentBorderColor(),
                cursorColor = accent
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
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(18.dp))
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
    manager: MusicManager,
    title: String = "Select Accent / Glass Tint",
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    val selectedColor = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
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
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(28.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                    val radius = (size.width / 2f).toDouble()
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
