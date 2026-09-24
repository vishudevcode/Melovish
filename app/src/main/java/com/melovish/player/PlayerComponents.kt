package com.melovish.player

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 4-Bar Animated Live Audio Equalizer for Playing Song Rows
@Composable
fun LiveAudioWaveEqualizer(
    isAnimating: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.70f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse), label = "h4"
    )

    Row(
        modifier = modifier.height(16.dp).width(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val heights = if (isAnimating) listOf(h1, h2, h3, h4) else listOf(0.3f, 0.4f, 0.35f, 0.25f)
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

// Curved Back Arrow Icon
@Composable
fun CurvedBackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.50f)
            lineTo(w * 0.48f, h * 0.20f)
            lineTo(w * 0.48f, h * 0.38f)
            cubicTo(w * 0.65f, h * 0.39f, w * 0.85f, h * 0.54f, w * 0.94f, h * 0.80f)
            cubicTo(w * 0.74f, h * 0.63f, w * 0.58f, h * 0.62f, w * 0.48f, h * 0.62f)
            lineTo(w * 0.48f, h * 0.80f)
            close()
        }
        drawPath(path, color = tint)
    }
}

// Circular Embossed Glass Back Button Container
@Composable
fun GlassBackButton(
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9)
    val ringColor = if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1)
    val iconColor = if (isDark) Color.White else Color(0xFF0F172A)

    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(buttonBg)
            .border(1.2.dp, ringColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CurvedBackArrowIcon(
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Glassmorphic Full-Color Folder Icon
@Composable
fun GlassmorphicFolderIcon(
    folderColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val backPath = Path().apply {
            moveTo(w * 0.12f, h * 0.22f)
            quadraticBezierTo(w * 0.12f, h * 0.14f, w * 0.20f, h * 0.14f)
            lineTo(w * 0.42f, h * 0.14f)
            quadraticBezierTo(w * 0.48f, h * 0.14f, w * 0.52f, h * 0.22f)
            lineTo(w * 0.56f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.28f)
            quadraticBezierTo(w * 0.88f, h * 0.28f, w * 0.88f, h * 0.35f)
            lineTo(w * 0.88f, h * 0.82f)
            quadraticBezierTo(w * 0.88f, h * 0.88f, w * 0.80f, h * 0.88f)
            lineTo(w * 0.18f, h * 0.88f)
            quadraticBezierTo(w * 0.12f, h * 0.88f, w * 0.12f, h * 0.82f)
            close()
        }

        drawPath(
            backPath,
            brush = Brush.verticalGradient(listOf(folderColor, folderColor.copy(alpha = 0.85f)))
        )

        val glassPath = Path().apply {
            moveTo(w * 0.15f, h * 0.38f)
            quadraticBezierTo(w * 0.13f, h * 0.38f, w * 0.18f, h * 0.38f)
            lineTo(w * 0.87f, h * 0.38f)
            quadraticBezierTo(w * 0.93f, h * 0.38f, w * 0.91f, h * 0.46f)
            lineTo(w * 0.83f, h * 0.88f)
            quadraticBezierTo(w * 0.81f, h * 0.92f, w * 0.75f, h * 0.92f)
            lineTo(w * 0.15f, h * 0.92f)
            quadraticBezierTo(w * 0.10f, h * 0.92f, w * 0.12f, h * 0.86f)
            close()
        }

        drawPath(
            glassPath,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.50f), folderColor.copy(alpha = 0.58f))
            )
        )

        drawPath(
            glassPath,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f))
            ),
            style = Stroke(width = 1.6f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// 9 Preset Colors + 10th Rainbow Circular Picker Dialog
@Composable
fun FolderColorDialog(
    folderName: String,
    currentColor: Color,
    isDark: Boolean,
    onColorSelected: (Color) -> Unit,
    onOpenRainbowPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val preset9Colors = listOf(
        Color(0xFFF59E0B), // 1. Mustard / Gold (Default)
        Color(0xFF00B4D8), // 2. Teal Blue
        Color(0xFF10B981), // 3. Mint / Emerald
        Color(0xFF39FF14), // 4. Bright Neon Green
        Color(0xFFFF2A85), // 5. Pink / Rose
        Color(0xFFEF4444), // 6. Crimson Red
        Color(0xFF8B5CF6), // 7. Purple / Violet
        Color(0xFF3B82F6), // 8. Electric Blue
        Color(0xFFFF6B35)  // 9. Coral Orange
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Folder Color: $folderName",
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicFolderIcon(folderColor = currentColor, modifier = Modifier.size(56.dp))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Pick Preset or Open Wheel",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        preset9Colors.take(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (currentColor == color) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        onColorSelected(color)
                                        onDismiss()
                                    }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        preset9Colors.drop(5).take(4).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (currentColor == color) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        onColorSelected(color)
                                        onDismiss()
                                    }
                            )
                        }

                        // 10th Slot: Circular Rainbow Wheel
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    onDismiss()
                                    onOpenRainbowPicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = if (isDark) Color.White else Color(0xFF0F172A))
                }
            }
        }
    }
}

// Minimalist Vector Avatar (Soft Sky-Blue to Periwinkle Gradient)
@Composable
fun DefaultProfileAvatar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF030712)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = backgroundColor,
            radius = w / 2f,
            center = Offset(w / 2f, h / 2f)
        )

        val bodyBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFCBD6FF), Color(0xFF5B82FC)),
            startY = h * 0.48f, endY = h * 0.95f
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.16f, h * 0.88f)
            cubicTo(w * 0.16f, h * 0.66f, w * 0.28f, h * 0.49f, w * 0.50f, h * 0.49f)
            cubicTo(w * 0.72f, h * 0.49f, w * 0.84f, h * 0.66f, w * 0.84f, h * 0.88f)
            cubicTo(w * 0.76f, h * 0.95f, w * 0.24f, h * 0.95f, w * 0.16f, h * 0.88f)
            close()
        }
        drawPath(bodyPath, brush = bodyBrush)

        val headRadius = w * 0.235f
        val headCenter = Offset(w * 0.5f, h * 0.285f)
        val headBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE2F1FE), Color(0xFF7FA8FE)),
            startY = headCenter.y - headRadius, endY = headCenter.y + headRadius
        )
        drawCircle(brush = headBrush, radius = headRadius, center = headCenter)
    }
}

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

// Full Player Sheet with Full-Screen Swipe, Double-Tap 10s Seek, and Reference Controls
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

    // Double-tap visual animation ripple states
    var showSeekLeftAnim by remember { mutableStateOf(false) }
    var showSeekRightAnim by remember { mutableStateOf(false) }

    LaunchedEffect(showSeekLeftAnim) {
        if (showSeekLeftAnim) {
            delay(550)
            showSeekLeftAnim = false
        }
    }
    LaunchedEffect(showSeekRightAnim) {
        if (showSeekRightAnim) {
            delay(550)
            showSeekRightAnim = false
        }
    }

    // High performance RenderNode alpha animation for seek arcs
    val leftSeekAlpha by animateFloatAsState(
        targetValue = if (showSeekLeftAnim) 1f else 0f,
        animationSpec = tween(durationMillis = if (showSeekLeftAnim) 80 else 380),
        label = "leftSeekAlpha"
    )
    val rightSeekAlpha by animateFloatAsState(
        targetValue = if (showSeekRightAnim) 1f else 0f,
        animationSpec = tween(durationMillis = if (showSeekRightAnim) 80 else 380),
        label = "rightSeekAlpha"
    )

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
    val animSurface by animateColorAsState(targetPalette.surface, tween(650, easing = FastOutSlowInEasing), label = "surface")
    val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(650, easing = FastOutSlowInEasing), label = "textPrimary")
    val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(650, easing = FastOutSlowInEasing), label = "textSecondary")

    val monoColor = if (isDark) Color.White else Color(0xFF0F172A)
    val userAccent = manager.accentColor

    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animBgTop, animBgBottom)))
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragX < -60f) {
                            manager.playNext()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (totalDragX > 60f) {
                            manager.playPrevious()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Big Album Art (Dominating Upper Half, Double-Tap to Seek)
            Box(
                modifier = Modifier
                    .size(335.dp)
                    .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = userAccent)
                    .clip(RoundedCornerShape(32.dp))
                    .background(animSurface)
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                    .pointerInput(song.id) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2f) {
                                    showSeekLeftAnim = true
                                    manager.seekTo((manager.currentPosition - 10000L).coerceAtLeast(0L))
                                } else {
                                    showSeekRightAnim = true
                                    manager.seekTo((manager.currentPosition + 10000L).coerceAtMost(manager.duration))
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(
                        bitmap = albumArtBitmap!!.asImageBitmap(),
                        contentDescription = "Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎵", fontSize = 110.sp)
                }

                // Left Ripple Overlay: « 10s (Receiver-Safe & Smooth)
                if (leftSeekAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .align(Alignment.CenterStart)
                            .background(Color(0x66000000).copy(alpha = 0.45f * leftSeekAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer { alpha = leftSeekAlpha }
                        ) {
                            Text("«", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Black)
                            Text("10s", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Right Ripple Overlay: 10s » (Receiver-Safe & Smooth)
                if (rightSeekAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .align(Alignment.CenterEnd)
                            .background(Color(0x66000000).copy(alpha = 0.45f * rightSeekAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer { alpha = rightSeekAlpha }
                        ) {
                            Text("»", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Black)
                            Text("10s", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 2. Song Name & Metadata
            Text(
                text = song.title,
                color = animTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
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

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Song Progress Bar (Driven by User Selected Accent Color)
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
                    thumbColor = userAccent,
                    activeTrackColor = userAccent,
                    inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(dragProgressMs.toLong()), color = animTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(formatTime(manager.duration), color = animTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Primary Playback Controls Row (Shifted Just Above Bottom Bar)
            // Order: [Repeat] [Prev] [Play/Pause] [Next] [Shuffle]
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RepeatControlIcon(
                    repeatMode = manager.repeatModeState,
                    tint = monoColor,
                    modifier = Modifier.clickable { manager.toggleRepeat() }.padding(8.dp)
                )

                PreviousControlIcon(
                    tint = monoColor,
                    modifier = Modifier.clickable { manager.playPrevious() }.padding(8.dp)
                )

                // Center: High-Contrast Solid Circular Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White else Color(0xFF0F172A))
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    val iconTint = if (isDark) Color(0xFF0F172A) else Color.White
                    if (manager.isPlaying) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp, 22.dp).clip(RoundedCornerShape(3.dp)).background(iconTint))
                            Box(modifier = Modifier.size(6.dp, 22.dp).clip(RoundedCornerShape(3.dp)).background(iconTint))
                        }
                    } else {
                        Canvas(modifier = Modifier.size(24.dp).padding(start = 3.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.15f, size.height * 0.10f)
                                lineTo(size.width * 0.90f, size.height * 0.50f)
                                lineTo(size.width * 0.15f, size.height * 0.90f)
                                close()
                            }
                            drawPath(path, color = iconTint)
                        }
                    }
                }

                NextControlIcon(
                    tint = monoColor,
                    modifier = Modifier.clickable { manager.playNext() }.padding(8.dp)
                )

                ShuffleControlIcon(
                    isShuffleOn = manager.isShuffleOn,
                    tint = monoColor,
                    modifier = Modifier.clickable { manager.toggleShuffle() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Bottom Utility Dock (Enlarged Bold Icons, Consistent Heart Shape)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(animSurface)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(26.dp))
                    .padding(vertical = 14.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❝≡", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable { showLyricsDialog = true })

                Text(
                    text = if (manager.sleepTimerRemainingSeconds > 0) "${manager.sleepTimerRemainingSeconds / 60}m" else "☾",
                    fontSize = 22.sp,
                    color = if (manager.sleepTimerRemainingSeconds > 0) userAccent else animTextPrimary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { showSleepDialog = true }
                )

                HeartIconVector(
                    isFavorite = song.isFavorite,
                    defaultTint = animTextPrimary,
                    modifier = Modifier.clickable { manager.toggleFavorite(song) }
                )

                Text("≡♪", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable { showQueueSheet = true })

                Text("•••", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable { showMenuModal = true })
            }
        }

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

// Consistent Heart Shape (Vibrant Red When Favorited)
@Composable
fun HeartIconVector(
    isFavorite: Boolean,
    defaultTint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.60f, 0f, h * 0.38f, 0f, h * 0.22f)
            cubicTo(0f, h * 0.08f, w * 0.18f, 0f, w * 0.36f, 0f)
            cubicTo(w * 0.44f, 0f, w * 0.5f, h * 0.08f, w * 0.5f, h * 0.12f)
            cubicTo(w * 0.5f, h * 0.08f, w * 0.56f, 0f, w * 0.64f, 0f)
            cubicTo(w * 0.82f, 0f, w, h * 0.08f, w, h * 0.22f)
            cubicTo(w, h * 0.38f, w * 0.85f, h * 0.60f, w * 0.5f, h * 0.85f)
            close()
        }

        if (isFavorite) {
            drawPath(path, color = Color(0xFFEF4444))
        } else {
            drawPath(
                path,
                color = defaultTint,
                style = Stroke(width = 2.2f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// Sleek Clean Controls Vector Set
@Composable
fun RepeatControlIcon(repeatMode: Int, tint: Color, modifier: Modifier = Modifier) {
    val isActive = repeatMode != Player.REPEAT_MODE_OFF
    val alpha = if (isActive) 1f else 0.4f
    Box(modifier = modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = size.width
            val h = size.height
            val color = tint.copy(alpha = alpha)

            val p1 = Path().apply {
                moveTo(w * 0.22f, h * 0.55f)
                lineTo(w * 0.22f, h * 0.30f)
                lineTo(w * 0.78f, h * 0.30f)
            }
            drawPath(p1, color, style = stroke)

            val a1 = Path().apply {
                moveTo(w * 0.66f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.30f)
                lineTo(w * 0.66f, h * 0.42f)
            }
            drawPath(a1, color, style = stroke)

            val p2 = Path().apply {
                moveTo(w * 0.78f, h * 0.45f)
                lineTo(w * 0.78f, h * 0.70f)
                lineTo(w * 0.22f, h * 0.70f)
            }
            drawPath(p2, color, style = stroke)

            val a2 = Path().apply {
                moveTo(w * 0.34f, h * 0.58f)
                lineTo(w * 0.18f, h * 0.70f)
                lineTo(w * 0.34f, h * 0.82f)
            }
            drawPath(a2, color, style = stroke)
        }
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            Text("1", color = tint, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PreviousControlIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.20f),
            end = Offset(w * 0.22f, h * 0.80f),
            strokeWidth = 3.5f.dp.toPx(),
            cap = StrokeCap.Round
        )

        val tri = Path().apply {
            moveTo(w * 0.80f, h * 0.20f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.80f, h * 0.80f)
            close()
        }
        drawPath(tri, color = tint)
    }
}

@Composable
fun NextControlIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        val tri = Path().apply {
            moveTo(w * 0.20f, h * 0.20f)
            lineTo(w * 0.66f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.80f)
            close()
        }
        drawPath(tri, color = tint)

        drawLine(
            color = tint,
            start = Offset(w * 0.78f, h * 0.20f),
            end = Offset(w * 0.78f, h * 0.80f),
            strokeWidth = 3.5f.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ShuffleControlIcon(isShuffleOn: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val alpha = if (isShuffleOn) 1f else 0.4f
    Canvas(modifier = modifier.size(26.dp)) {
        val stroke = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val color = tint.copy(alpha = alpha)

        val p1 = Path().apply {
            moveTo(w * 0.18f, h * 0.30f)
            cubicTo(w * 0.45f, h * 0.30f, w * 0.55f, h * 0.70f, w * 0.80f, h * 0.70f)
        }
        drawPath(p1, color, style = stroke)

        val a1 = Path().apply {
            moveTo(w * 0.68f, h * 0.60f)
            lineTo(w * 0.82f, h * 0.70f)
            lineTo(w * 0.68f, h * 0.80f)
        }
        drawPath(a1, color, style = stroke)

        val p2 = Path().apply {
            moveTo(w * 0.18f, h * 0.70f)
            cubicTo(w * 0.38f, h * 0.70f, w * 0.44f, h * 0.58f, w * 0.50f, h * 0.50f)
        }
        val p2b = Path().apply {
            moveTo(w * 0.58f, h * 0.42f)
            cubicTo(w * 0.64f, h * 0.30f, w * 0.72f, h * 0.30f, w * 0.80f, h * 0.30f)
        }
        drawPath(p2, color, style = stroke)
        drawPath(p2b, color, style = stroke)

        val a2 = Path().apply {
            moveTo(w * 0.68f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.30f)
            lineTo(w * 0.68f, h * 0.40f)
        }
        drawPath(a2, color, style = stroke)
    }
}
