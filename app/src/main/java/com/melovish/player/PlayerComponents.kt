package com.melovish.player

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MaterialYouPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val primaryAccent: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

fun extractMaterialYouPalette(bitmap: Bitmap?, isDarkMode: Boolean, fallbackAccent: Color): MaterialYouPalette {
    if (bitmap == null) {
        return if (isDarkMode) {
            MaterialYouPalette(Color(0xFF1E1F28), Color(0xFF0B0C10), fallbackAccent, Color(0x33FFFFFF), Color.White, Color(0xFF94A3B8))
        } else {
            MaterialYouPalette(Color(0xFFFAF7F2), Color(0xFFEBE5DB), fallbackAccent, Color(0x66FFFFFF), Color(0xFF0F172A), Color(0xFF475569))
        }
    }
    return try {
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, false)
        var totalR = 0L; var totalG = 0L; var totalB = 0L; var count = 0
        var maxSat = -1f; var vibrantColor = android.graphics.Color.WHITE
        val hsv = FloatArray(3)

        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val p = scaled.getPixel(x, y)
                totalR += android.graphics.Color.red(p)
                totalG += android.graphics.Color.green(p)
                totalB += android.graphics.Color.blue(p)
                count++
                android.graphics.Color.colorToHSV(p, hsv)
                if (hsv[1] > maxSat && hsv[2] > 0.22f && hsv[2] < 0.95f) {
                    maxSat = hsv[1]
                    vibrantColor = p
                }
            }
        }
        val dom = if (maxSat > 0.28f) vibrantColor else android.graphics.Color.rgb((totalR/count).toInt(), (totalG/count).toInt(), (totalB/count).toInt())
        android.graphics.Color.colorToHSV(dom, hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.35f, 0.85f)

        if (isDarkMode) {
            val top = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.65f, 0.20f)))
            val bot = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 18f) % 360f, sat * 0.8f, 0.08f)))
            val acc = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.9f, 0.90f)))
            val surf = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.35f, 0.28f))).copy(alpha = 0.55f)
            MaterialYouPalette(top, bot, acc, surf, Color(0xFFF8FAFC), Color(0xFFCBD5E1))
        } else {
            val top = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.25f, 0.97f)))
            val bot = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 16f) % 360f, sat * 0.40f, 0.88f)))
            val acc = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.55f)))
            val surf = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat * 0.12f, 0.99f))).copy(alpha = 0.75f)
            MaterialYouPalette(top, bot, acc, surf, Color(0xFF0F172A), Color(0xFF334155))
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

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(manager.currentPosition) {
        if (!isDraggingSlider) {
            dragProgressMs = manager.currentPosition.toFloat()
        }
    }

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    val targetPalette = remember(song.id, albumArtBitmap, isDark, manager.accentColor) {
        extractMaterialYouPalette(albumArtBitmap, isDark, manager.accentColor)
    }

    val animBgTop by animateColorAsState(targetPalette.bgTop, tween(650, easing = FastOutSlowInEasing), label = "bgTop")
    val animBgBottom by animateColorAsState(targetPalette.bgBottom, tween(650, easing = FastOutSlowInEasing), label = "bgBottom")
    val animAccent by animateColorAsState(targetPalette.primaryAccent, tween(650, easing = FastOutSlowInEasing), label = "accent")
    val animSurface by animateColorAsState(targetPalette.surface, tween(650, easing = FastOutSlowInEasing), label = "surface")
    val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(650, easing = FastOutSlowInEasing), label = "textPrimary")
    val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(650, easing = FastOutSlowInEasing), label = "textSecondary")

    val monoColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animBgTop, animBgBottom)))
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Artwork with Horizontal Swipe (Left = Prev, Right = Next)
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = animAccent)
                    .clip(RoundedCornerShape(32.dp))
                    .background(animSurface)
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                    .pointerInput(song.id) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount < -35f) {
                                manager.playNext()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragAmount > 35f) {
                                manager.playPrevious()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎵", fontSize = 96.sp)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Seek Bar
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

            // Title and Subtitle
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

            // Top Playback Row: [⇄ Repeat] [⏮ Prev] [▶ Play/Pause] [⏭ Next] [🔀 Shuffle]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRepeatActive = manager.repeatModeState != Player.REPEAT_MODE_OFF
                RepeatVectorIcon(
                    isActive = isRepeatActive,
                    isRepeatOne = manager.repeatModeState == Player.REPEAT_MODE_ONE,
                    monoColor = monoColor,
                    modifier = Modifier.clickable { manager.toggleRepeat() }.padding(8.dp)
                )

                Text(
                    text = "⏮",
                    fontSize = 32.sp,
                    color = monoColor,
                    modifier = Modifier.clickable { manager.playPrevious() }.padding(8.dp)
                )

                Text(
                    text = if (manager.isPlaying) "❚❚" else "▶",
                    color = monoColor,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { manager.togglePlayPause() }.padding(8.dp)
                )

                Text(
                    text = "⏭",
                    fontSize = 32.sp,
                    color = monoColor,
                    modifier = Modifier.clickable { manager.playNext() }.padding(8.dp)
                )

                ShuffleVectorIcon(
                    isActive = manager.isShuffleOn,
                    monoColor = monoColor,
                    modifier = Modifier.clickable { manager.toggleShuffle() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Dedicated Volume Slider Row: [🔈] ----•---- [🔊]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔈", fontSize = 16.sp)
                Slider(
                    value = manager.currentVolume,
                    onValueChange = { manager.setHardwareVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = animAccent,
                        activeTrackColor = animAccent,
                        inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text("🔊", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Utility Bar: [❝≡] [☾] [♡] [≡♪] [•••]
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
        }

        // More Modal Sheet
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
                        .fillMaxWidth(0.75f)
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

        if (showEqualizerSheet) EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        if (showSpeedDialog) MagneticSpeedDialog(manager = manager, onDismiss = { showSpeedDialog = false })
        if (showSleepDialog) SleepTimerDialog(manager = manager, onDismiss = { showSleepDialog = false })
        if (showQueueSheet) QueueSheet(manager = manager, onDismiss = { showQueueSheet = false })
        if (showTagEditorDialog) TagEditorDialog(manager = manager, song = song, onDismiss = { showTagEditorDialog = false })
        if (showLyricsDialog) LyricsDialog(song = song, isDark = isDark, onDismiss = { showLyricsDialog = false })
        if (showAddToPlaylistDialog) AddToPlaylistDialog(manager = manager, song = song, onDismiss = { showAddToPlaylistDialog = false })
    }
}

// Pro-Grade Functional Equalizer & Audio FX Sheet
@Composable
fun EqualizerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val accent = manager.accentColor

    Box(modifier = Modifier.fillMaxSize().background(Color(0xF50A0F1D)).statusBarsPadding().padding(20.dp)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Equalizer & Audio FX", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(onClick = { onDismiss() }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Master Equalizer Switch
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AFFFFFF)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Master Equalizer", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (manager.isEqEnabled) "Hardware audio processor enabled" else "Processor bypassed", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Switch(checked = manager.isEqEnabled, onCheckedChange = { manager.toggleEqualizer(it) })
                }
            }

            // Presets Scrollable Row
            item {
                Text("Sound Presets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(manager.eqPresetNames) { preset ->
                        val isSel = manager.selectedEqPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accent else Color(0x22FFFFFF))
                                .clickable { manager.applyEqPreset(preset) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(preset, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Graphic Equalizer Frequency Bands
            item {
                Text("Frequency Response (-15dB to +15dB)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x14FFFFFF)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in 0 until manager.eqBandsCount) {
                        val freq = manager.eqCenterFreqs[i] ?: (60 * (i + 1) * (i + 1))
                        val label = if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz"
                        val level = manager.eqBandLevels[i] ?: 0
                        val levelDb = level / 100

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${if (levelDb > 0) "+$levelDb" else "$levelDb"} dB", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { manager.setEqBandLevel(i, it.toInt()) },
                                valueRange = manager.eqMinLevel.toFloat()..manager.eqMaxLevel.toFloat(),
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }
                }
            }

            // Acoustic FX: Bass Boost & 3D Surround Virtualizer
            item {
                Text("Acoustics & Depth", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x14FFFFFF)).padding(16.dp)) {
                    Text("Bass Boost: ${manager.bassBoostPercent}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = manager.bassBoostPercent.toFloat(),
                        onValueChange = { manager.setBassBoost(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("3D Surround (Virtualizer): ${manager.virtualizerPercent}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = manager.virtualizerPercent.toFloat(),
                        onValueChange = { manager.setVirtualizer(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            }

            // Real-Time Vocal & Bass Cut Filters
            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x14FFFFFF)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Stop Bass (Full Cut)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Completely cuts sub-bass frequencies", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(checked = manager.isStopBass, onCheckedChange = { manager.toggleStopBass(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Remove Vocals (Center Cut)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Attenuates vocal center frequency bands", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(checked = manager.isRemoveVocals, onCheckedChange = { manager.toggleRemoveVocals(it) })
                    }
                }
            }
        }
    }
}

// Vector Repeat and Shuffle
@Composable
fun RepeatVectorIcon(isActive: Boolean, isRepeatOne: Boolean, monoColor: Color, modifier: Modifier = Modifier) {
    val strokeWidth = if (isActive) 3.8f else 2.0f
    val alpha = if (isActive) 1.0f else 0.38f

    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = monoColor.copy(alpha = alpha)
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = strokeWidth * density, cap = StrokeCap.Round, join = StrokeJoin.Round)

            val topPath = Path().apply {
                moveTo(w * 0.2f, h * 0.55f)
                lineTo(w * 0.2f, h * 0.35f)
                quadraticBezierTo(w * 0.2f, h * 0.25f, w * 0.35f, h * 0.25f)
                lineTo(w * 0.75f, h * 0.25f)
            }
            drawPath(topPath, color, style = stroke)

            val topArrow = Path().apply {
                moveTo(w * 0.65f, h * 0.15f)
                lineTo(w * 0.82f, h * 0.25f)
                lineTo(w * 0.65f, h * 0.35f)
            }
            drawPath(topArrow, color, style = stroke)

            val botPath = Path().apply {
                moveTo(w * 0.8f, h * 0.45f)
                lineTo(w * 0.8f, h * 0.65f)
                quadraticBezierTo(w * 0.8f, h * 0.75f, w * 0.65f, h * 0.75f)
                lineTo(w * 0.25f, h * 0.75f)
            }
            drawPath(botPath, color, style = stroke)

            val botArrow = Path().apply {
                moveTo(w * 0.35f, h * 0.65f)
                lineTo(w * 0.18f, h * 0.75f)
                lineTo(w * 0.35f, h * 0.85f)
            }
            drawPath(botArrow, color, style = stroke)
        }

        if (isRepeatOne) {
            Text("1", color = monoColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ShuffleVectorIcon(isActive: Boolean, monoColor: Color, modifier: Modifier = Modifier) {
    val strokeWidth = if (isActive) 3.8f else 2.0f
    val alpha = if (isActive) 1.0f else 0.38f

    Canvas(modifier = modifier.size(24.dp)) {
        val color = monoColor.copy(alpha = alpha)
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = strokeWidth * density, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val p1 = Path().apply {
            moveTo(w * 0.18f, h * 0.32f)
            cubicTo(w * 0.42f, h * 0.32f, w * 0.58f, h * 0.68f, w * 0.80f, h * 0.68f)
        }
        drawPath(p1, color, style = stroke)

        val a1 = Path().apply {
            moveTo(w * 0.70f, h * 0.58f)
            lineTo(w * 0.84f, h * 0.68f)
            lineTo(w * 0.70f, h * 0.78f)
        }
        drawPath(a1, color, style = stroke)

        val p2 = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            cubicTo(w * 0.38f, h * 0.68f, w * 0.45f, h * 0.56f, w * 0.50f, h * 0.50f)
        }
        val p2b = Path().apply {
            moveTo(w * 0.58f, h * 0.42f)
            cubicTo(w * 0.64f, h * 0.32f, w * 0.72f, h * 0.32f, w * 0.80f, h * 0.32f)
        }
        drawPath(p2, color, style = stroke)
        drawPath(p2b, color, style = stroke)

        val a2 = Path().apply {
            moveTo(w * 0.70f, h * 0.22f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.70f, h * 0.42f)
        }
        drawPath(a2, color, style = stroke)
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
fun MiniPlayerDock(manager: MusicManager, onClick: () -> Unit) {
    val song = manager.currentSong ?: return
    val accent = manager.accentColor
    val isDark = manager.isDarkMode

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xE60A0F1D) else Color(0xF2FFFFFF))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                if (albumArtBitmap != null) {
                    Image(bitmap = albumArtBitmap!!.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎵", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}", color = accent, fontSize = 11.sp, maxLines = 1)
            }
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(accent).clickable { manager.togglePlayPause() }, contentAlignment = Alignment.Center) {
                Text(if (manager.isPlaying) "❚❚" else "▶", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SleepTimerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val accent = manager.accentColor

    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(15) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(cardBg).clickable(enabled = false) {}.padding(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Sleep Timer", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically stop playback after a set time.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("✕", color = Color(0xFF64748B), fontSize = 20.sp, modifier = Modifier.clickable { onDismiss() })
                }

                if (manager.sleepTimerRemainingSeconds > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AEF4444)).border(1.dp, Color(0x33EF4444), RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("⏳ Timer Active", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${manager.sleepTimerRemainingSeconds / 60}m ${manager.sleepTimerRemainingSeconds % 60}s remaining", color = textColor, fontSize = 12.sp)
                            }
                            Button(onClick = { manager.endSleepTimer(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(10.dp)) {
                                Text("End Timer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Presets", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Box(modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.setSleepTimer(mins); onDismiss() }, contentAlignment = Alignment.Center) {
                            Text("$mins min", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.setSleepTimerToEndOfTrack(); onDismiss() }, contentAlignment = Alignment.Center) {
                    Text("End of current track", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Custom Timer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hours: $customHours", color = Color(0xFF64748B), fontSize = 12.sp)
                        Slider(value = customHours.toFloat(), onValueChange = { customHours = it.toInt() }, valueRange = 0f..24f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minutes: $customMinutes", color = Color(0xFF64748B), fontSize = 12.sp)
                        Slider(value = customMinutes.toFloat(), onValueChange = { customMinutes = it.toInt() }, valueRange = 1f..60f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    }
                    Button(onClick = { manager.setSleepTimer((customHours * 60) + customMinutes); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(50.dp)) {
                        Text("Set", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CircularColorPickerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    val currentColor = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(28.dp)).background(if (isDark) Color(0xFF0F172A) else Color.White).clickable(enabled = false) {}.padding(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom Accent Picker", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("✕", fontSize = 18.sp, modifier = Modifier.clickable { onDismiss() })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touch = change.position
                                    val dist = sqrt((touch.x - center.x) * (touch.x - center.x) + (touch.y - center.y) * (touch.y - center.y))
                                    val radius = size.width / 2f

                                    if (dist >= radius * 0.65f) {
                                        var angle = Math.toDegrees(atan2(touch.y - center.y, touch.x - center.x).toDouble()).toFloat()
                                        if (angle < 0) angle += 360f
                                        hue = angle
                                    } else {
                                        val halfInner = (radius * 0.55f)
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
                        val thumbDist = radius - (ringThickness / 2f)
                        val thumbPos = Offset(center.x + (thumbDist * cos(thumbRad)).toFloat(), center.y + (thumbDist * sin(thumbRad)).toFloat())
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

                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(currentColor).border(2.dp, Color.White, CircleShape))
                    Button(onClick = { manager.addColorPreset(currentColor) }, colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(10.dp)) {
                        Text("+ Add to Presets", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Saved Presets (10 Slots)", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    manager.userSavedColorPresets.take(5).forEach { color ->
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(1.5.dp, if (manager.accentColor == color) Color.White else Color.Transparent, CircleShape).clickable { manager.updateAccent(color); onDismiss() })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    manager.userSavedColorPresets.drop(5).take(5).forEach { color ->
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(1.5.dp, if (manager.accentColor == color) Color.White else Color.Transparent, CircleShape).clickable { manager.updateAccent(color); onDismiss() })
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { manager.updateAccent(currentColor); onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = currentColor)) {
                    Text("Apply Accent", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TagEditorDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Edit Audio Tags", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { photoPickerLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor), shape = RoundedCornerShape(10.dp)) {
                        Text(if (selectedCoverUri != null) "Artwork Picked ✓" else "Change Artwork", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Song Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artist Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editAlbum, onValueChange = { editAlbum = it }, label = { Text("Album Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editDate, onValueChange = { editDate = it }, label = { Text("Date & Time / Year") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { manager.updateSongMetadata(song, editTitle, editArtist, editAlbum, editDate, selectedCoverUri); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = manager.accentColor)) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(20.dp)) {
            Column {
                Text("Add to Playlist", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(manager.customPlaylists) { pl ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.addSongToPlaylist(song.id, pl); onDismiss() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("📑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(pl.name, color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
fun MagneticSpeedDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var speed by remember { mutableFloatStateOf(manager.playbackSpeed) }
    val formattedSpeed = String.format(Locale.US, "%.2fx", speed)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E293B)).padding(24.dp)) {
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
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
fun QueueSheet(manager: MusicManager, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xF00F172A)).padding(20.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Playing Queue (${manager.playbackQueue.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { onDismiss() }, shape = RoundedCornerShape(10.dp)) { Text("Close") }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(manager.playbackQueue, key = { it.id }) { song ->
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
                            Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsDialog(song: Song, isDark: Boolean, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).height(400.dp).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).padding(24.dp)) {
            Column {
                Text("Lyrics", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("No synchronized lyrics found for \"${song.title}\".", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}
