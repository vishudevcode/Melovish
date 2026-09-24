package com.melovish.player

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import java.util.Locale

@Composable
fun FullPlayerSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val song = manager.currentSong ?: return
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showMenuModal by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }
    var showDriveMode by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }

    // Live Scrubbing State
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(manager.currentPosition) {
        if (!isDraggingSlider) {
            dragProgressMs = manager.currentPosition.toFloat()
        }
    }

    val albumArt = manager.getAlbumArt(song)
    val accent = manager.accentColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (manager.isDarkMode) Color(0xFF131720) else Color(0xFFF2ECE1),
                        if (manager.isDarkMode) Color(0xFF030712) else Color(0xFFE5DDD0)
                    )
                )
            )
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 35f) onDismiss()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -45f) {
                        manager.playNext()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (dragAmount > 45f) {
                        manager.playPrevious()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sleek Top Minimize Chevron
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌵", fontSize = 28.sp, color = if (manager.isDarkMode) Color.White else Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Embedded Album Artwork
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (manager.isDarkMode) Color(0x1AFFFFFF) else Color(0x33000000))
                    .border(
                        1.5.dp,
                        if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0x44FFFFFF),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎵", fontSize = 96.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Seek Bar with Timestamps
            Slider(
                value = dragProgressMs.coerceIn(0f, manager.duration.toFloat().coerceAtLeast(1f)),
                onValueChange = {
                    isDraggingSlider = true
                    dragProgressMs = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    manager.seekTo(dragProgressMs.toLong())
                },
                valueRange = 0f..(manager.duration.toFloat().coerceAtLeast(1f)),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = if (manager.isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(dragProgressMs.toLong()), color = if (manager.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B), fontSize = 12.sp)
                Text(formatTime(manager.duration), color = if (manager.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Title and Artist
            Text(
                text = song.title,
                color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (song.artist.isNotBlank()) song.artist else "Unknown Artist",
                color = if (manager.isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat
                val repColor = if (manager.repeatModeState != Player.REPEAT_MODE_OFF) accent else if (manager.isDarkMode) Color(0x66FFFFFF) else Color(0x66000000)
                Text(
                    text = when (manager.repeatModeState) {
                        Player.REPEAT_MODE_ONE -> "🔂"
                        Player.REPEAT_MODE_ALL -> "🔁"
                        else -> "⇄"
                    },
                    fontSize = 22.sp,
                    color = repColor,
                    modifier = Modifier.clickable { manager.toggleRepeat() }.padding(8.dp)
                )

                // Previous
                Text(
                    text = "⏮",
                    fontSize = 32.sp,
                    color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.clickable { manager.playPrevious() }.padding(8.dp)
                )

                // Play / Pause Solid Button
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (manager.isPlaying) "❚❚" else "▶",
                        color = Color(0xFF0A0F1D),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Next
                Text(
                    text = "⏭",
                    fontSize = 32.sp,
                    color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.clickable { manager.playNext() }.padding(8.dp)
                )

                // Three Dot Menu
                Text(
                    text = "•••",
                    fontSize = 24.sp,
                    color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.clickable { showMenuModal = true }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hardware-synced Volume Slider Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔉", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = manager.currentVolume,
                    onValueChange = { manager.setSystemVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = if (manager.isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🔊", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom 5 Utility Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (manager.isDarkMode) Color(0x1FFFFFFF) else Color(0x33FFFFFF))
                    .border(
                        1.dp,
                        if (manager.isDarkMode) Color(0x1AFFFFFF) else Color(0x44FFFFFF),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Lyrics
                Text("❝≡", fontSize = 20.sp, color = if (manager.isDarkMode) Color.White else Color(0xFF1E293B), modifier = Modifier.clickable { showLyricsDialog = true })
                // 2. Sleep Timer
                Text(
                    text = if (manager.sleepTimerRemainingSeconds > 0) "${manager.sleepTimerRemainingSeconds / 60}m" else "☾",
                    fontSize = 18.sp,
                    color = if (manager.sleepTimerRemainingSeconds > 0) accent else if (manager.isDarkMode) Color.White else Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showSleepDialog = true }
                )
                // 3. Favorite
                Text(
                    text = if (song.isFavorite) "♥" else "♡",
                    fontSize = 20.sp,
                    color = if (song.isFavorite) Color(0xFFEF4444) else if (manager.isDarkMode) Color.White else Color(0xFF1E293B),
                    modifier = Modifier.clickable { manager.toggleFavorite(song) }
                )
                // 4. Queue
                Text("≡♪", fontSize = 20.sp, color = if (manager.isDarkMode) Color.White else Color(0xFF1E293B), modifier = Modifier.clickable { showQueueSheet = true })
                // 5. Shuffle
                Text(
                    text = "🔀",
                    fontSize = 18.sp,
                    color = if (manager.isShuffleOn) accent else if (manager.isDarkMode) Color(0x66FFFFFF) else Color(0x66000000),
                    modifier = Modifier.clickable { manager.toggleShuffle() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Three Dot Popup Menu (From Screenshot 2)
        if (showMenuModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { showMenuModal = false },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(if (manager.isDarkMode) Color(0xFF0F172A) else Color(0xFFFAF8F5))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MenuRow("🏷️", "Tag editor") { showMenuModal = false; showTagEditorDialog = true }
                        MenuRow("🔗", "Share") {
                            showMenuModal = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, song.uri)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                        MenuRow("➕", "Add to playlist") { showMenuModal = false; manager.createPlaylist("Playlist ${manager.customPlaylists.size + 1}") }
                        MenuRow("👤", "Artist: ${if (song.artist.isNotBlank()) song.artist else "Unknown"}") { showMenuModal = false }
                        MenuRow("📜", "Lyrics") { showMenuModal = false; showLyricsDialog = true }
                        MenuRow("📡", "Cast") { showMenuModal = false }
                        MenuRow("⏱️", "Playback Speed") { showMenuModal = false; showSpeedDialog = true }
                        MenuRow("🚗", "Drive mode") { showMenuModal = false; showDriveMode = true }
                        MenuRow("🎚️", "Equalizer") { showMenuModal = false; showEqualizerSheet = true }
                        MenuRow("🗑️", "Delete from device", isDanger = true) {
                            showMenuModal = false
                            manager.deleteSongFromDevice(song)
                            onDismiss()
                        }
                    }
                }
            }
        }

        // Modals
        if (showEqualizerSheet) EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        if (showSpeedDialog) MagneticSpeedDialog(manager = manager, onDismiss = { showSpeedDialog = false })
        if (showSleepDialog) SleepTimerDialog(onSetTimer = { manager.setSleepTimer(it) }, onDismiss = { showSleepDialog = false })
        if (showQueueSheet) QueueSheet(manager = manager, onDismiss = { showQueueSheet = false })
        if (showTagEditorDialog) TagEditorDialog(song = song, onDismiss = { showTagEditorDialog = false })
        if (showDriveMode) DriveModeScreen(manager = manager, onDismiss = { showDriveMode = false })
        if (showLyricsDialog) LyricsDialog(song = song, onDismiss = { showLyricsDialog = false })
    }
}

@Composable
fun MenuRow(icon: String, text: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, fontSize = 14.sp, color = if (isDanger) Color(0xFFEF4444) else Color(0xFFE2E8F0), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MiniPlayerDock(
    manager: MusicManager,
    onClick: () -> Unit
) {
    val song = manager.currentSong ?: return
    val art = manager.getAlbumArt(song)
    val accent = manager.accentColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (manager.isDarkMode) Color(0xE60A0F1D) else Color(0xF2FFFFFF))
            .border(1.dp, if (manager.isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) onClick()
                }
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (art != null) {
                    Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎵", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (song.artist.isNotBlank()) song.artist else "Melovish", color = accent, fontSize = 12.sp, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable { manager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (manager.isPlaying) "❚❚" else "▶", color = Color(0xFF0A0F1D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Magnetic Speed Dialog (0.25x, 0.5x, 1x, 1.5x, 2x, 2.5x, 3x with haptics)
@Composable
fun MagneticSpeedDialog(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var speed by remember { mutableFloatStateOf(manager.playbackSpeed) }
    val formattedSpeed = String.format(Locale.US, "%.2fx", speed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
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
                Text("Playback Speed: $formattedSpeed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = speed,
                    onValueChange = { raw ->
                        val snapped = manager.setMagneticSpeed(raw)
                        if (snapped != speed) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        speed = snapped
                    },
                    valueRange = 0.25f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = manager.accentColor, activeTrackColor = manager.accentColor)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0.5x", "1.0x", "1.5x", "2.0x", "3.0x").forEach { spd ->
                        Text(spd, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor)
                ) {
                    Text("Done", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EqualizerSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00A0F1D))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Done") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Equalizer", color = Color.White, fontSize = 16.sp)
                Switch(checked = manager.isEqEnabled, onCheckedChange = { manager.isEqEnabled = it })
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Bass Boost: ${manager.bassBoostPercent}%", color = Color.White, fontSize = 14.sp)
            Slider(
                value = manager.bassBoostPercent.toFloat(),
                onValueChange = { manager.bassBoostPercent = it.toInt() },
                valueRange = 0f..500f,
                colors = SliderDefaults.colors(thumbColor = manager.accentColor, activeTrackColor = manager.accentColor)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stop Bass (Full Cut)", color = Color.White, fontSize = 15.sp)
                Switch(checked = manager.isStopBass, onCheckedChange = { manager.isStopBass = it })
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remove Vocals (Center Cut)", color = Color.White, fontSize = 15.sp)
                Switch(checked = manager.isRemoveVocals, onCheckedChange = { manager.isRemoveVocals = it })
            }
        }
    }
}

@Composable
fun TagEditorDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    var editTitle by remember { mutableStateOf(song.title) }
    var editArtist by remember { mutableStateOf(song.artist) }
    var editAlbum by remember { mutableStateOf(song.album) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Text("Edit Audio Tags", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text("Artist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
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
                Text("Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                listOf(15, 30, 60).forEach { mins ->
                    Button(
                        onClick = { onSetTimer(mins); onDismiss() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("$mins Minutes", color = Color.White)
                    }
                }
                Button(
                    onClick = { onSetTimer(0); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Turn Off Timer", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun QueueSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00F172A))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playing Queue (${manager.playbackQueue.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Close") }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(manager.playbackQueue) { song ->
                    val isCur = song.id == manager.currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCur) Color(0x33D4AF37) else Color(0x1AFFFFFF))
                            .clickable { manager.playSong(song, manager.playbackQueue, manager.currentSectionName) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isCur) "▶" else "•", color = manager.accentColor, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(song.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun DriveModeScreen(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))) {
                    Text("Exit Drive Mode")
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(manager.currentSong?.title ?: "No Song", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                Text(manager.currentSong?.artist ?: "", color = Color(0xFF94A3B8), fontSize = 18.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏮", fontSize = 54.sp, color = Color.White, modifier = Modifier.clickable { manager.playPrevious() })
                Box(
                    modifier = Modifier.size(90.dp).clip(CircleShape).background(manager.accentColor).clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (manager.isPlaying) "❚❚" else "▶", fontSize = 36.sp, color = Color.Black)
                }
                Text("⏭", fontSize = 54.sp, color = Color.White, modifier = Modifier.clickable { manager.playNext() })
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun LyricsDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.85f).height(400.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E293B)).padding(24.dp)
        ) {
            Column {
                Text("Lyrics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("No synchronized lyrics found for \"${song.title}\".", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}
