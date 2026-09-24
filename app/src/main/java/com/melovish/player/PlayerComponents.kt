package com.melovish.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }

    val accentGold = Color(0xFFD4AF37)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (manager.isColorfulPlayer) Color(0xFF1E293B) else Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onDismiss()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -35f) manager.playNext()
                    if (dragAmount > 35f) manager.playPrevious()
                }
            }
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pull Down Handle
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x66FFFFFF))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PLAYING FROM: ${manager.currentSectionName.uppercase()}",
                color = accentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Album Art
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.5.dp, Color(0x33D4AF37), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", fontSize = 90.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = song.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (song.artist.isNotBlank()) song.artist else "Unknown Artist",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = manager.currentPosition.toFloat(),
                onValueChange = { manager.seekTo(it.toLong()) },
                valueRange = 0f..(manager.duration.toFloat().coerceAtLeast(1f)),
                colors = SliderDefaults.colors(
                    thumbColor = accentGold,
                    activeTrackColor = accentGold,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(manager.currentPosition), color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(formatTime(manager.duration), color = Color(0xFF94A3B8), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val repeatColor = if (manager.repeatModeState != Player.REPEAT_MODE_OFF) accentGold else Color(0x66FFFFFF)
                Box(modifier = Modifier.clickable { manager.toggleRepeat() }.padding(8.dp)) {
                    Text(
                        text = when (manager.repeatModeState) {
                            Player.REPEAT_MODE_ONE -> "🔂"
                            Player.REPEAT_MODE_ALL -> "🔁"
                            else -> "↺"
                        },
                        fontSize = 20.sp,
                        color = repeatColor
                    )
                }

                Box(modifier = Modifier.clickable { manager.playPrevious() }.padding(8.dp)) {
                    Text("⏮", fontSize = 28.sp, color = Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(accentGold)
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (manager.isPlaying) "❚❚" else "▶",
                        color = Color(0xFF0F172A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(modifier = Modifier.clickable { manager.playNext() }.padding(8.dp)) {
                    Text("⏭", fontSize = 28.sp, color = Color.White)
                }

                val shuffleColor = if (manager.isShuffleOn) accentGold else Color(0x66FFFFFF)
                Box(modifier = Modifier.clickable { manager.toggleShuffle() }.padding(8.dp)) {
                    Text("🔀", fontSize = 20.sp, color = shuffleColor)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Secondary Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎚", fontSize = 18.sp, modifier = Modifier.clickable { showEqualizerSheet = true })
                Text("${manager.playbackSpeed}x", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showSpeedDialog = true })
                Text(
                    text = if (manager.sleepTimerRemainingSeconds > 0) "${manager.sleepTimerRemainingSeconds / 60}m" else "⏱",
                    fontSize = 14.sp,
                    color = if (manager.sleepTimerRemainingSeconds > 0) accentGold else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showSleepDialog = true }
                )
                Text("📋", fontSize = 18.sp, modifier = Modifier.clickable { showQueueSheet = true })
                Text("🏷", fontSize = 18.sp, modifier = Modifier.clickable { showTagEditorDialog = true })
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showEqualizerSheet) {
            EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        }

        if (showSpeedDialog) {
            SpeedDialog(
                currentSpeed = manager.playbackSpeed,
                onSpeedChange = { manager.setSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showSleepDialog) {
            SleepTimerDialog(
                onSetTimer = { manager.setSleepTimer(it) },
                onDismiss = { showSleepDialog = false }
            )
        }

        if (showQueueSheet) {
            QueueSheet(manager = manager, onDismiss = { showQueueSheet = false })
        }

        if (showTagEditorDialog) {
            TagEditorDialog(song = song, onDismiss = { showTagEditorDialog = false })
        }
    }
}

@Composable
fun MiniPlayerDock(
    manager: MusicManager,
    onClick: () -> Unit
) {
    val song = manager.currentSong ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xE60A0F1D))
            .border(1.5.dp, Color(0x33D4AF37), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                Text("🎵", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (song.artist.isNotBlank()) song.artist else "Melovish", color = Color(0xFFD4AF37), fontSize = 12.sp, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4AF37))
                    .clickable { manager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (manager.isPlaying) "❚❚" else "▶", color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Equalizer", color = Color.White, fontSize = 15.sp)
                Switch(checked = manager.isEqEnabled, onCheckedChange = { manager.isEqEnabled = it })
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("Bass Boost: ${manager.bassBoostPercent}%", color = Color.White, fontSize = 14.sp)
            Slider(
                value = manager.bassBoostPercent.toFloat(),
                onValueChange = { manager.bassBoostPercent = it.toInt() },
                valueRange = 0f..500f
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stop Bass (Full Cut)", color = Color.White, fontSize = 14.sp)
                Switch(checked = manager.isStopBass, onCheckedChange = { manager.isStopBass = it })
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remove Vocals (Center Cut)", color = Color.White, fontSize = 14.sp)
                Switch(checked = manager.isRemoveVocals, onCheckedChange = { manager.isRemoveVocals = it })
            }
        }
    }
}

@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    val speedLabel = String.format(Locale.US, "%.1fx", speed)

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
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playback Speed: $speedLabel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.1f..2.0f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onSpeedChange(speed)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply")
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
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                listOf(15, 30, 60).forEach { mins ->
                    Button(
                        onClick = {
                            onSetTimer(mins)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("$mins Minutes", color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        onSetTimer(0)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
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
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Close") }
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
                        Text(if (isCur) "▶" else "•", color = Color(0xFFD4AF37), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(song.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun TagEditorDialog(
    song: Song,
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
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Text("Metadata Tag Editor", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("File Name: ${song.title}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("Path: ${song.path}", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
