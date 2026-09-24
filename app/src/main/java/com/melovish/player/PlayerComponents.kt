package com.melovish.player

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

// Material You Dynamic Palette Data Class
data class MaterialYouPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val primaryAccent: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

// Real-Time Material You HSV Palette Extractor
fun extractMaterialYouPalette(bitmap: Bitmap?, isDarkMode: Boolean, fallbackAccent: Color): MaterialYouPalette {
    if (bitmap == null) {
        return if (isDarkMode) {
            MaterialYouPalette(
                bgTop = Color(0xFF1E1F28),
                bgBottom = Color(0xFF0B0C10),
                primaryAccent = fallbackAccent,
                surface = Color(0x33FFFFFF),
                textPrimary = Color.White,
                textSecondary = Color(0xFF94A3B8)
            )
        } else {
            MaterialYouPalette(
                bgTop = Color(0xFFFAF7F2),
                bgBottom = Color(0xFFEBE5DB),
                primaryAccent = fallbackAccent,
                surface = Color(0x66FFFFFF),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569)
            )
        }
    }

    return try {
        val scaled = Bitmap.createScaledBitmap(bitmap, 20, 20, false)
        var totalR = 0L; var totalG = 0L; var totalB = 0L; var count = 0
        var maxSaturation = -1f
        var vibrantColorInt = android.graphics.Color.WHITE
        val hsv = FloatArray(3)

        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                totalR += android.graphics.Color.red(pixel)
                totalG += android.graphics.Color.green(pixel)
                totalB += android.graphics.Color.blue(pixel)
                count++

                android.graphics.Color.colorToHSV(pixel, hsv)
                if (hsv[1] > maxSaturation && hsv[2] > 0.22f && hsv[2] < 0.95f) {
                    maxSaturation = hsv[1]
                    vibrantColorInt = pixel
                }
            }
        }

        val dominantColorInt = if (maxSaturation > 0.28f) {
            vibrantColorInt
        } else {
            android.graphics.Color.rgb((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
        }

        android.graphics.Color.colorToHSV(dominantColorInt, hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.35f, 0.85f)

        if (isDarkMode) {
            val top = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.65f, 0.20f)))
            val bottom = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 18f) % 360f, sat * 0.8f, 0.08f)))
            val accent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.9f, 0.90f)))
            val surface = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.35f, 0.28f))).copy(alpha = 0.55f)
            MaterialYouPalette(top, bottom, accent, surface, Color(0xFFF8FAFC), Color(0xFFCBD5E1))
        } else {
            val top = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.25f, 0.97f)))
            val bottom = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 16f) % 360f, sat * 0.40f, 0.88f)))
            val accent = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.55f)))
            val surface = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.12f, 0.99f))).copy(alpha = 0.75f)
            MaterialYouPalette(top, bottom, accent, surface, Color(0xFF0F172A), Color(0xFF334155))
        }
    } catch (_: Exception) {
        MaterialYouPalette(
            bgTop = if (isDarkMode) Color(0xFF1E1F28) else Color(0xFFFAF7F2),
            bgBottom = if (isDarkMode) Color(0xFF0B0C10) else Color(0xFFEBE5DB),
            primaryAccent = fallbackAccent,
            surface = if (isDarkMode) Color(0x33FFFFFF) else Color(0x66FFFFFF),
            textPrimary = if (isDarkMode) Color.White else Color(0xFF0F172A),
            textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
        )
    }
}

@Composable
fun FullPlayerSheet(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val song = manager.currentSong ?: return
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isDark = manager.isDarkMode

    var showMenuModal by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Live Scrubbing State
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(manager.currentPosition) {
        if (!isDraggingSlider) {
            dragProgressMs = manager.currentPosition.toFloat()
        }
    }

    val albumArt = manager.getAlbumArt(song)

    // Compute Dynamic Material You Palette from Album Art
    val targetPalette = remember(song.id, albumArt, isDark, manager.accentColor) {
        extractMaterialYouPalette(albumArt, isDark, manager.accentColor)
    }

    // Smooth Google-Style 650ms Morphing Transition
    val animBgTop by animateColorAsState(targetPalette.bgTop, tween(650, easing = FastOutSlowInEasing), label = "bgTop")
    val animBgBottom by animateColorAsState(targetPalette.bgBottom, tween(650, easing = FastOutSlowInEasing), label = "bgBottom")
    val animAccent by animateColorAsState(targetPalette.primaryAccent, tween(650, easing = FastOutSlowInEasing), label = "accent")
    val animSurface by animateColorAsState(targetPalette.surface, tween(650, easing = FastOutSlowInEasing), label = "surface")
    val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(650, easing = FastOutSlowInEasing), label = "textPrimary")
    val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(650, easing = FastOutSlowInEasing), label = "textSecondary")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animBgTop, animBgBottom)))
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 35f) onDismiss()
                }
            }
            .padding(horizontal = 24.dp, vertical = 10.dp)
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
                Text("⌵", fontSize = 28.sp, color = animTextPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Embedded Album Artwork with Edge-Swipe Volume Gesture
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = animAccent)
                    .clip(RoundedCornerShape(32.dp))
                    .background(animSurface)
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val touchX = change.position.x
                            val width = 310f * 2.7f
                            if (touchX < width * 0.28f || touchX > width * 0.72f) {
                                val delta = -dragAmount.y * 0.4f
                                manager.adjustVolumeByDelta(delta)
                            } else {
                                if (dragAmount.x < -35f) {
                                    manager.playNext()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (dragAmount.x > 35f) {
                                    manager.playPrevious()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
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

                // Volume Overlay HUD
                AnimatedVisibility(
                    visible = manager.isVolumeOverlayVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC000000))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔊", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${manager.volumeOverlayPercent}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

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
                    thumbColor = animAccent,
                    activeTrackColor = animAccent,
                    inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(dragProgressMs.toLong()), color = animTextSecondary, fontSize = 12.sp)
                Text(formatTime(manager.duration), color = animTextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Title and Subtitle ([File Size] • [Artist Name])
            Text(
                text = song.title,
                color = animTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"}",
                color = animTextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Controls: [⇄ Repeat] [⏮ Prev] [▶ Play/Pause] [⏭ Next] [🔀 Shuffle]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat
                val repColor = if (manager.repeatModeState != Player.REPEAT_MODE_OFF) animAccent else if (isDark) Color(0x66FFFFFF) else Color(0x66000000)
                Text(
                    text = when (manager.repeatModeState) {
                        Player.REPEAT_MODE_ONE -> "🔂"
                        Player.REPEAT_MODE_ALL -> "🔁"
                        else -> "⇄"
                    },
                    fontSize = 24.sp,
                    color = repColor,
                    modifier = Modifier.clickable { manager.toggleRepeat() }.padding(8.dp)
                )

                // Previous
                Text(
                    text = "⏮",
                    fontSize = 32.sp,
                    color = animTextPrimary,
                    modifier = Modifier.clickable { manager.playPrevious() }.padding(8.dp)
                )

                // Play / Pause Material You Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape, spotColor = animAccent)
                        .clip(CircleShape)
                        .background(animAccent)
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (manager.isPlaying) "❚❚" else "▶",
                        color = if (isDark) Color(0xFF0F172A) else Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Next
                Text(
                    text = "⏭",
                    fontSize = 32.sp,
                    color = animTextPrimary,
                    modifier = Modifier.clickable { manager.playNext() }.padding(8.dp)
                )

                // Shuffle
                val shuffleColor = if (manager.isShuffleOn) animAccent else if (isDark) Color(0x66FFFFFF) else Color(0x66000000)
                Text(
                    text = "🔀",
                    fontSize = 22.sp,
                    color = shuffleColor,
                    modifier = Modifier.clickable { manager.toggleShuffle() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom 5 Utility Icons: [❝≡] [☾] [♡] [≡♪] [•••]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(animSurface)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❝≡", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = animTextPrimary, modifier = Modifier.clickable { showLyricsDialog = true })
                Text(
                    text = if (manager.sleepTimerRemainingSeconds > 0) "${manager.sleepTimerRemainingSeconds / 60}m" else "☾",
                    fontSize = 20.sp,
                    color = if (manager.sleepTimerRemainingSeconds > 0) animAccent else animTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { showSleepDialog = true }
                )
                Text(
                    text = if (song.isFavorite) "♥" else "♡",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (song.isFavorite) Color(0xFFEF4444) else animTextPrimary,
                    modifier = Modifier.clickable { manager.toggleFavorite(song) }
                )
                Text("≡♪", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = animTextPrimary, modifier = Modifier.clickable { showQueueSheet = true })
                Text("•••", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = animTextPrimary, modifier = Modifier.clickable { showMenuModal = true })
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Ordered Three-Dot Popup Menu
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
                        .fillMaxWidth(0.74f)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color.White)
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MenuRow("👤", "Artist: ${if (song.artist.isNotBlank()) song.artist else "Unknown"}", isDark) { showMenuModal = false }
                        MenuRow("📜", "Lyrics", isDark) { showMenuModal = false; showLyricsDialog = true }
                        MenuRow("🔗", "Share", isDark) {
                            showMenuModal = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, song.uri)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                        MenuRow("⏱️", "Playback Speed", isDark) { showMenuModal = false; showSpeedDialog = true }
                        MenuRow("🎚️", "Equalizer", isDark) { showMenuModal = false; showEqualizerSheet = true }
                        MenuRow("🏷️", "Tag Editor", isDark) { showMenuModal = false; showTagEditorDialog = true }
                        MenuRow("➕", "Add to Playlist", isDark) { showMenuModal = false; showAddToPlaylistDialog = true }
                        MenuRow("🗑️", "Delete from Device", isDark, isDanger = true) {
                            showMenuModal = false
                            manager.deleteSongFromDevice(song)
                            onDismiss()
                        }
                    }
                }
            }
        }

        // Attached Dialogs
        if (showEqualizerSheet) EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        if (showSpeedDialog) MagneticSpeedDialog(manager = manager, onDismiss = { showSpeedDialog = false })
        if (showSleepDialog) SleepTimerDialog(manager = manager, onDismiss = { showSleepDialog = false })
        if (showQueueSheet) QueueSheet(manager = manager, onDismiss = { showQueueSheet = false })
        if (showTagEditorDialog) TagEditorDialog(manager = manager, song = song, onDismiss = { showTagEditorDialog = false })
        if (showLyricsDialog) LyricsDialog(song = song, isDark = isDark, onDismiss = { showLyricsDialog = false })
        if (showAddToPlaylistDialog) AddToPlaylistDialog(manager = manager, song = song, onDismiss = { showAddToPlaylistDialog = false })
    }
}

@Composable
fun MenuRow(icon: String, text: String, isDark: Boolean, isDanger: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDanger) Color(0xFFEF4444) else if (isDark) Color.White else Color(0xFF0F172A)
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val isDark = manager.isDarkMode

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xE60A0F1D) else Color(0xF2FFFFFF))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(22.dp))
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (art != null) {
                    Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎵", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}", color = accent, fontSize = 11.sp, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable { manager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (manager.isPlaying) "❚❚" else "▶", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Sleep Timer Dialog with Presets, End of Track, and Interactive Vertical Sliders
@Composable
fun SleepTimerDialog(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val isDark = manager.isDarkMode
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val accent = manager.accentColor

    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(15) }
    var showHourSlider by remember { mutableStateOf(false) }
    var showMinuteSlider by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sleep Timer", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically stop playback after a set time.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("✕", color = Color(0xFF64748B), fontSize = 20.sp, modifier = Modifier.clickable { onDismiss() })
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Presets", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.setSleepTimer(mins)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins min", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable {
                            manager.setSleepTimerToEndOfTrack()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("End of current track", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Custom Timer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hours", color = Color(0xFF64748B), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .clickable { showHourSlider = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$customHours", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minutes", color = Color(0xFF64748B), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .clickable { showMinuteSlider = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$customMinutes", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val totalMins = (customHours * 60) + customMinutes
                            manager.setSleepTimer(totalMins)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 18.dp).height(50.dp)
                    ) {
                        Text("Set", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showHourSlider) {
        VerticalPickerPopup(
            title = "Select Hours (0 - 24)",
            value = customHours,
            range = 0..24,
            isDark = isDark,
            accent = accent,
            onValueSelected = { customHours = it; showHourSlider = false },
            onDismiss = { showHourSlider = false }
        )
    }

    if (showMinuteSlider) {
        VerticalPickerPopup(
            title = "Select Minutes (1 - 60)",
            value = customMinutes,
            range = 1..60,
            isDark = isDark,
            accent = accent,
            onValueSelected = { customMinutes = it; showMinuteSlider = false },
            onDismiss = { showMinuteSlider = false }
        )
    }
}

@Composable
fun VerticalPickerPopup(
    title: String,
    value: Int,
    range: IntRange,
    isDark: Boolean,
    accent: Color,
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableIntStateOf(value) }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("$selected", color = accent, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = selected.toFloat(),
                    onValueChange = { selected = it.toInt() },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onValueSelected(selected) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Complete Tag Editor Dialog
@Composable
fun TagEditorDialog(
    manager: MusicManager,
    song: Song,
    onDismiss: () -> Unit
) {
    var editTitle by remember { mutableStateOf(song.title) }
    var editArtist by remember { mutableStateOf(song.artist) }
    var editAlbum by remember { mutableStateOf(song.album) }
    var editDate by remember { mutableStateOf(song.releaseDate) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedCoverUri = uri
    }

    val isDark = manager.isDarkMode
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Edit Audio Tags", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🖼️", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (selectedCoverUri != null) "Artwork Picked ✓" else "Change Artwork", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Song Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text("Artist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Album Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editDate,
                    onValueChange = { editDate = it },
                    label = { Text("Date & Time / Year") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        manager.updateSongMetadata(song, editTitle, editArtist, editAlbum, editDate, selectedCoverUri)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Add to Playlist Dialog
@Composable
fun AddToPlaylistDialog(
    manager: MusicManager,
    song: Song,
    onDismiss: () -> Unit
) {
    val isDark = manager.isDarkMode
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Text("Add to Playlist", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(manager.customPlaylists) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.addSongToPlaylist(song.id, pl)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun MagneticSpeedDialog(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var speed by remember { mutableFloatStateOf(manager.playbackSpeed) }
    val formattedSpeed = String.format(Locale.US, "%.2fx", speed)

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
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
                    listOf("0.25x", "0.5x", "1.0x", "1.5x", "2.0x", "3.0x").forEach { spd ->
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
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxSize().background(Color(0xF00A0F1D)).padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer & Audio FX", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Done") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Equalizer", color = Color.White, fontSize = 16.sp)
                Switch(
                    checked = manager.isEqEnabled,
                    onCheckedChange = {
                        manager.isEqEnabled = it
                        manager.attachAudioEffects()
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Bass Boost: ${manager.bassBoostPercent}%", color = Color.White, fontSize = 14.sp)
            Slider(
                value = manager.bassBoostPercent.toFloat(),
                onValueChange = {
                    manager.bassBoostPercent = it.toInt()
                    manager.attachAudioEffects()
                },
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
                Switch(
                    checked = manager.isStopBass,
                    onCheckedChange = {
                        manager.isStopBass = it
                        manager.attachAudioEffects()
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remove Vocals (Center Cut)", color = Color.White, fontSize = 15.sp)
                Switch(
                    checked = manager.isRemoveVocals,
                    onCheckedChange = {
                        manager.isRemoveVocals = it
                        manager.attachAudioEffects()
                    }
                )
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
        modifier = Modifier.fillMaxSize().background(Color(0xF00F172A)).padding(20.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsDialog(
    song: Song,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.85f).height(400.dp).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).padding(24.dp)
        ) {
            Column {
                Text("Lyrics", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("No synchronized lyrics found for \"${song.title}\".", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}
