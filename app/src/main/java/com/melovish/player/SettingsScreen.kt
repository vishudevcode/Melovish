package com.melovish.player

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) BitmapFactory.decodeFile(avatarFile.absolutePath) else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(54.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color(0xFF030712)).border(2.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(bitmap = avatarBitmap.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize())
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

        // Appearance
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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

        // Player Settings
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

                SettingSwitchRow("🎨", "Colourful Player", "Player background adapts to album art.", manager.isColorfulPlayer, textColor) { manager.isColorfulPlayer = it }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("⏯️", "Resume only the first file", "Playback will only resume for the first track.", manager.isResumeFirstOnly, textColor) { manager.isResumeFirstOnly = it }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🔊", "Fade on start", "Gently fade in audio on playback.", manager.isFadeOnStart, textColor) { manager.isFadeOnStart = it }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("♾️", "Gapless Playback", "Removes pauses between tracks.", manager.isGaplessEnabled, textColor) { manager.isGaplessEnabled = it }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("↔️", "Crossfade", "Adjust the fade duration between tracks.", manager.isCrossfadeEnabled, textColor) { manager.isCrossfadeEnabled = it }

                if (manager.isCrossfadeEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Duration: ${manager.crossfadeDuration.toInt()}s", color = Color(0xFF64748B), fontSize = 12.sp)
                    Slider(value = manager.crossfadeDuration, onValueChange = { manager.crossfadeDuration = it }, valueRange = 1f..10f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                }
            }
        }

        // Audio Settings
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

                SettingSwitchRow("📶", "Lossless Audio", "Use Dolby Atmos and Hi-Res Audio.", manager.isLosslessEnabled, textColor) { manager.isLosslessEnabled = it }
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🔉", "Volume Normalization", "Set the same loudness level for all tracks.", manager.isVolumeNormalized, textColor) { manager.isVolumeNormalized = it }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Volume Boost", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Increase the maximum volume (${manager.volumeBoostLevel.toInt()}%).", color = Color(0xFF64748B), fontSize = 12.sp)
                Slider(value = manager.volumeBoostLevel, onValueChange = { manager.volumeBoostLevel = it; manager.attachAudioEffects() }, valueRange = 100f..200f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                Spacer(modifier = Modifier.height(14.dp))
                SettingSwitchRow("🎚️", "Mono Audio", "Combine left and right channels.", manager.isMonoAudio, textColor) { manager.isMonoAudio = it }

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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Equalizer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(manager.selectedEqPreset, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onOpenEqualizer() }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                            Text("Adjust", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(checked = manager.isEqEnabled, onCheckedChange = { manager.toggleEqualizer(it) })
                    }
                }
            }
        }

        // Content Manager
        item {
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
                    Button(onClick = { activeSubScreen = "hide_folders" }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Hide Audio", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hide specific audio files from library.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Button(onClick = { activeSubScreen = "hide_audio" }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                        Text("Manage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCircularPicker) CircularColorPickerDialog(manager = manager, onDismiss = { showCircularPicker = false })
}

// Full-Screen Manage Hidden Folders (Hidden at Top with Dimmed Opacity)
@Composable
fun ManageHiddenFoldersFullScreen(manager: MusicManager, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val allFolders = manager.allSongs.map { it.folderName }.distinct()
    val hiddenFolders = manager.hiddenFolders.toList()
    val visibleFolders = allFolders.filter { it !in hiddenFolders }

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
                item {
                    Text(
                        text = "Hidden Folders (${hiddenFolders.size}) — Tap to Restore",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(hiddenFolders, key = { "hidden_$it" }) { folder ->
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

            item {
                Text(
                    text = "Visible Folders (${visibleFolders.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(visibleFolders, key = { "visible_$it" }) { folder ->
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

// Full-Screen Manage Hidden Audio (Search Bar + Dimmed Hidden Tracks on Top)
@Composable
fun ManageHiddenAudioFullScreen(manager: MusicManager, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var searchQuery by remember { mutableStateOf("") }

    val filteredAll = manager.allSongs.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
    }

    val hiddenSongs = filteredAll.filter { it.id in manager.hiddenAudioIds }
    val visibleSongs = filteredAll.filter { it.id !in manager.hiddenAudioIds }

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
                item {
                    Text(
                        text = "Hidden Audio (${hiddenSongs.size}) — Tap to Restore",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(hiddenSongs, key = { "hidden_${it.id}" }) { song ->
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

            item {
                Text(
                    text = "Visible Audio (${visibleSongs.size}) — Tap to Exclude",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(visibleSongs, key = { "visible_${it.id}" }) { song ->
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
