package com.melovish.player

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

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
    val context = LocalContext.current

    var showHiddenAudioDialog by remember { mutableStateOf(false) }
    var showHiddenFoldersDialog by remember { mutableStateOf(false) }

    val avatarFile = manager.profileImagePath?.let { File(it) }
    val avatarBitmap = if (avatarFile != null && avatarFile.exists()) BitmapFactory.decodeFile(avatarFile.absolutePath) else null

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBackButton(isDark = isDark, onClick = onBackClick)
            Spacer(modifier = Modifier.width(14.dp))
            Text("Settings & Sound Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Preview
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .clickable { onOpenProfile() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF030712))
                            .border(2.dp, accent, CircleShape),
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
                        Text(if (manager.profileEmail.isNotBlank()) manager.profileEmail else "Personal Profile & Statistics", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Player Live Visualizer Settings Integration
            item {
                Text("Audio Visualizer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text("Live Player Visualizer", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Real-time audio reaction between track details and seekbar", color = Color(0xFF64748B), fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Off", "Waveform", "Dotted Equalizer").forEach { mode ->
                            val isSel = manager.visualizerMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) accent else if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable { manager.setVisualizerPreference(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSel) Color.White else textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Sound & Audio Effects
            item {
                Text("Sound & Audio Effects", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenEqualizer() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Equalizer & Audio DSP", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Hardware Effect: ${manager.selectedEqPreset}", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lossless Engine (Hi-Res Audio)", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Bit-perfect decoding up to 24-bit 192kHz", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Switch(
                            checked = manager.isLosslessEnabled,
                            onCheckedChange = {
                                manager.isLosslessEnabled = it
                                manager.prefs.edit().putBoolean("lossless", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gapless Playback", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Seamless transitions with zero delay between tracks", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Switch(
                            checked = manager.isGaplessEnabled,
                            onCheckedChange = {
                                manager.isGaplessEnabled = it
                                manager.prefs.edit().putBoolean("gapless", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                        )
                    }

                    // Crossfade Engine
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Crossfade", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.getDefault(), "%.1f", manager.crossfadeDuration)}s overlap between tracks", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                            Switch(
                                checked = manager.isCrossfadeEnabled,
                                onCheckedChange = {
                                    manager.isCrossfadeEnabled = it
                                    manager.prefs.edit().putBoolean("crossfade_enabled", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                            )
                        }
                        if (manager.isCrossfadeEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = manager.crossfadeDuration,
                                onValueChange = {
                                    manager.crossfadeDuration = it
                                    manager.prefs.edit().putFloat("crossfade_duration", it).apply()
                                },
                                valueRange = 1f..10f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }

                    // Volume Normalization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volume Normalization", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Consistent track volume balancing (ReplayGain)", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Switch(
                            checked = manager.isVolumeNormalized,
                            onCheckedChange = {
                                manager.isVolumeNormalized = it
                                manager.prefs.edit().putBoolean("vol_norm", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                        )
                    }

                    // Audio Output Routing Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Audio Output Routing", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Phone", "Speaker", "Buds").forEach { out ->
                                val isSel = manager.selectedAudioOutput == out
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) accent else if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                        .clickable { manager.setAudioOutputRouting(out) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(out, color = if (isSel) Color.White else textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Mono Audio Downmixer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mono Audio", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Combine stereo left & right channels into mono", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Switch(
                            checked = manager.isMonoAudio,
                            onCheckedChange = {
                                manager.isMonoAudio = it
                                manager.prefs.edit().putBoolean("mono", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                        )
                    }
                }
            }

            // Theme & Visual Style
            item {
                Text("Theme & Visual Style", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("App Mode", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("System", "Light", "Dark").forEach { mode ->
                            val isSel = manager.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) accent else if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable { manager.setTheme(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mode, color = if (isSel) Color.White else textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Primary Accent Colors", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(manager.userSavedColorPresets) { col ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(2.dp, if (manager.accentColor == col) Color.White else Color.Transparent, CircleShape)
                                    .clickable { manager.updateAccent(col) }
                            )
                        }
                    }
                }
            }

            // Library Management & Storage (Content Manager)
            item {
                Text("Library Management & Storage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardBg).border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showHiddenFoldersDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Excluded Folders (${manager.hiddenFolders.size})", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("View and unhide excluded directories", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showHiddenAudioDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hidden Tracks Manager (${manager.hiddenAudioIds.size})", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("View hidden audio files at the top and unhide by tapping", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { manager.scanStorage() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔄 Rescan Storage Files", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Excluded Folders Dialog
    if (showHiddenFoldersDialog) {
        HiddenFoldersManagerDialog(
            manager = manager,
            isDark = isDark,
            onDismiss = { showHiddenFoldersDialog = false }
        )
    }

    // Hidden Audio Tracks Dialog
    if (showHiddenAudioDialog) {
        HiddenAudioManagerDialog(
            manager = manager,
            isDark = isDark,
            onDismiss = { showHiddenAudioDialog = false }
        )
    }
}

// Full Excluded Folders Manager
@Composable
fun HiddenFoldersManagerDialog(
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var folderSearch by remember { mutableStateOf("") }

    val allDiscoveredFolders = remember(manager.allSongs.size) {
        (manager.allSongs.map { it.folderName } + manager.hiddenFolders).distinct()
    }

    val filteredFolders = remember(allDiscoveredFolders, folderSearch, manager.hiddenFolders.size) {
        val list = if (folderSearch.isBlank()) allDiscoveredFolders
        else allDiscoveredFolders.filter { it.contains(folderSearch, ignoreCase = true) }

        val hidden = list.filter { manager.hiddenFolders.contains(it) }.sorted()
        val visible = list.filter { !manager.hiddenFolders.contains(it) }.sorted()
        hidden + visible
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Excluded Folders", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("Tap any folder to hide or unhide it", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Done") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = folderSearch,
                    onValueChange = { folderSearch = it },
                    placeholder = { Text("Search folders...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFolders, key = { it }) { folder ->
                        val isHidden = manager.hiddenFolders.contains(folder)
                        val folderBg = if (isHidden) {
                            if (isDark) Color(0x33EF4444) else Color(0x1FEF4444)
                        } else {
                            if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(folderBg)
                                .border(1.dp, if (isHidden) Color(0xFFEF4444).copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable {
                                    manager.toggleHideFolder(folder)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                GlassmorphicFolderIcon(
                                    folderColor = if (isHidden) Color(0xFF94A3B8) else manager.getFolderColor(folder),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = folder,
                                        color = if (isHidden) textColor.copy(alpha = 0.6f) else textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isHidden) "Excluded from library (Tap to unhide)" else "Included in library (Tap to hide)",
                                        color = if (isHidden) Color(0xFFEF4444) else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = if (isHidden) "Unhide" else "Hide",
                                color = if (isHidden) accent else Color(0xFF64748B),
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

// Full Hidden Audio Manager
@Composable
fun HiddenAudioManagerDialog(
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var audioSearch by remember { mutableStateOf("") }

    val allSongsList = remember(manager.allSongs.size, manager.hiddenAudioIds.size) {
        manager.allSongs.toList()
    }

    val filteredSongs = remember(allSongsList, audioSearch, manager.hiddenAudioIds.size) {
        val list = if (audioSearch.isBlank()) allSongsList
        else allSongsList.filter {
            it.title.contains(audioSearch, ignoreCase = true) || it.artist.contains(audioSearch, ignoreCase = true)
        }

        val hidden = list.filter { manager.hiddenAudioIds.contains(it.id) }
        val normal = list.filter { !manager.hiddenAudioIds.contains(it.id) }
        hidden + normal
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hidden Tracks Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("Hidden tracks shown on top with dimmed styling", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Done") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = audioSearch,
                    onValueChange = { audioSearch = it },
                    placeholder = { Text("Search tracks by title or artist...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tracks found.", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isHidden = manager.hiddenAudioIds.contains(song.id)
                            val rowBg = if (isHidden) {
                                if (isDark) Color(0x33EF4444) else Color(0x1FEF4444)
                            } else {
                                if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(rowBg)
                                    .border(1.dp, if (isHidden) Color(0xFFEF4444).copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable {
                                        manager.toggleHideAudio(song.id)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isHidden) textColor.copy(alpha = 0.55f) else textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isHidden) "Hidden from player • Tap to Unhide" else "${formatFileSize(song.size)} • ${song.artist} • Tap to Hide",
                                        color = if (isHidden) Color(0xFFEF4444) else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = if (isHidden) "Unhide" else "Hide",
                                    color = if (isHidden) accent else Color(0xFF64748B),
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
}
