package com.melovish.player

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Live Animated 4-Bar Equalizer
@Composable
fun LiveAudioWaveEqualizer(isAnimating: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val h1 by infiniteTransition.animateFloat(0.25f, 0.95f, infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "h1")
    val h2 by infiniteTransition.animateFloat(0.85f, 0.20f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "h2")
    val h3 by infiniteTransition.animateFloat(0.35f, 1.0f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h3")
    val h4 by infiniteTransition.animateFloat(0.70f, 0.30f, infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse), label = "h4")

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

// Vector Curved Back Arrow Icon
@Composable
fun CurvedBackArrowIcon(tint: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
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
            onDrawBehind {
                drawPath(path, color = tint)
            }
        }
    )
}

// Circular Embossed Glass Back Button with Haptic Response
@OptIn(UnstableApi::class)
@Composable
fun GlassBackButton(isDark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9))
            .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
            .clickable {
                MusicManager.activeInstance?.triggerHapticFeedback(false)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        CurvedBackArrowIcon(tint = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.size(20.dp))
    }
}

// Glassmorphic Folder Icon
@Composable
fun GlassmorphicFolderIcon(folderColor: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val backPath = Path().apply {
                moveTo(w * 0.12f, h * 0.22f)
                quadraticBezierTo(w * 0.12f, h * 0.14f, w * 0.20f, h * 0.14f)
                lineTo(w * 0.42f, h * 0.14f)
                quadraticBezierTo(w * 0.48f, h * 0.14f, w * 0.52f, h * 0.22f)
                lineTo(w * 0.56f, h * 0.28f)
                lineTo(w * 0.82f, h * 0.28f)
                quadraticBezierTo(w * 0.88f, h * 0.88f, w * 0.88f, h * 0.35f)
                lineTo(w * 0.88f, h * 0.82f)
                quadraticBezierTo(w * 0.88f, h * 0.88f, w * 0.80f, h * 0.88f)
                lineTo(w * 0.18f, h * 0.88f)
                quadraticBezierTo(w * 0.12f, h * 0.88f, w * 0.12f, h * 0.82f)
                close()
            }
            val glassPath = Path().apply {
                moveTo(w * 0.15f, h * 0.38f)
                quadraticBezierTo(w * 0.13f, h * 0.38f, w * 0.18f, h * 0.38f)
                lineTo(w * 0.87f, h * 0.38f)
                quadraticBezierTo(w * 0.93f, h * 0.46f, w * 0.91f, h * 0.46f)
                lineTo(w * 0.83f, h * 0.88f)
                quadraticBezierTo(w * 0.81f, h * 0.92f, w * 0.75f, h * 0.92f)
                lineTo(w * 0.15f, h * 0.92f)
                quadraticBezierTo(w * 0.10f, h * 0.92f, w * 0.12f, h * 0.86f)
                close()
            }
            val backBrush = Brush.verticalGradient(listOf(folderColor, folderColor.copy(alpha = 0.85f)))
            val glassBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.50f), folderColor.copy(alpha = 0.58f)))
            val strokeBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f)))
            val strokeStyle = Stroke(width = 1.6f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            onDrawBehind {
                drawPath(backPath, brush = backBrush)
                drawPath(glassPath, brush = glassBrush)
                drawPath(glassPath, brush = strokeBrush, style = strokeStyle)
            }
        }
    )
}

// Folder Color Dialog with Frosted Glass Styling
@Composable
fun FolderColorDialog(
    folderName: String,
    currentColor: Color,
    isDark: Boolean,
    onColorSelected: (Color) -> Unit,
    onOpenRainbowPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val preset9Colors = remember {
        listOf(
            Color(0xFFF59E0B), Color(0xFF00B4D8), Color(0xFF10B981), Color(0xFF39FF14), Color(0xFFFF2A85),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFFF6B35)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Folder Color: $folderName",
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicFolderIcon(folderColor = currentColor, modifier = Modifier.size(52.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { onOpenRainbowPicker() },
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
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Minimalist Vector Avatar Supporting Dynamic Theme Atmosphere Tinting
@Composable
fun DefaultProfileAvatar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val bodyBrush = Brush.verticalGradient(listOf(Color(0xFFCBD6FF), Color(0xFF5B82FC)), startY = h * 0.48f, endY = h * 0.95f)
            val bodyPath = Path().apply {
                moveTo(w * 0.16f, h * 0.88f)
                cubicTo(w * 0.16f, h * 0.66f, w * 0.28f, h * 0.49f, w * 0.50f, h * 0.49f)
                cubicTo(w * 0.72f, h * 0.49f, w * 0.84f, h * 0.66f, w * 0.84f, h * 0.88f)
                cubicTo(w * 0.76f, h * 0.95f, w * 0.24f, h * 0.95f, w * 0.16f, h * 0.88f)
                close()
            }
            val headRadius = w * 0.235f
            val headCenter = Offset(w * 0.5f, h * 0.285f)
            val headBrush = Brush.verticalGradient(listOf(Color(0xFFE2F1FE), Color(0xFF7FA8FE)), startY = headCenter.y - headRadius, endY = headCenter.y + headRadius)

            onDrawBehind {
                if (backgroundColor != Color.Transparent) {
                    drawCircle(color = backgroundColor, radius = w / 2f, center = Offset(w / 2f, h / 2f))
                }
                drawPath(bodyPath, brush = bodyBrush)
                drawCircle(brush = headBrush, radius = headRadius, center = headCenter)
            }
        }
    )
}

@Immutable
data class MaterialYouPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val primaryAccent: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

suspend fun extractMaterialYouPaletteAsync(bitmap: Bitmap?, isDarkMode: Boolean, fallbackAccent: Color): MaterialYouPalette = withContext(Dispatchers.Default) {
    if (bitmap == null) {
        return@withContext if (isDarkMode) {
            MaterialYouPalette(Color(0xFF1E1F28), Color(0xFF0B0C10), fallbackAccent, Color(0x33FFFFFF), Color(0xFFF8FAFC), Color(0xFF94A3B8))
        } else {
            MaterialYouPalette(Color(0xFFFAF7F2), Color(0xFFEBE5DB), fallbackAccent, Color(0x66FFFFFF), Color(0xFF0F172A), Color(0xFF475569))
        }
    }
    try {
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
        val dom = if (maxSat > 0.28f) vibrantColor else android.graphics.Color.rgb((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
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
            textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
        )
    }
}

// 100% Mathematically Centered Progress Bar
@Composable
fun IsolatedScrubberLeaf(
    currentPositionMs: Long,
    durationMs: Long,
    accentColor: Color,
    textColor: Color,
    isDark: Boolean,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val maxDuration = durationMs.coerceAtLeast(1L)
    val playbackFraction = (currentPositionMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
    val currentFraction = if (isDragging) dragFraction else playbackFraction
    val displayPos = if (isDragging) (dragFraction * maxDuration).toLong() else currentPositionMs
    val inactiveTrackColor = if (isDark) Color(0xFF475569) else Color(0xFFD1D5DB)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(maxDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        val thumbRadiusPx = 5.dp.toPx()
                        val trackStart = thumbRadiusPx
                        val trackEnd = size.width - thumbRadiusPx
                        val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)

                        dragFraction = ((down.position.x - trackStart) / trackWidth).coerceIn(0f, 1f)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                dragFraction = ((change.position.x - trackStart) / trackWidth).coerceIn(0f, 1f)
                            } else {
                                isDragging = false
                                onSeek((dragFraction * maxDuration).toLong())
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                val centerY = size.height / 2f
                val strokeWidthPx = 3.dp.toPx()
                val thumbRadiusPx = 5.dp.toPx()

                val trackStart = thumbRadiusPx
                val trackEnd = size.width - thumbRadiusPx
                val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                val thumbX = trackStart + currentFraction * trackWidth

                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(trackStart, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )

                if (thumbX > trackStart) {
                    drawLine(
                        color = accentColor,
                        start = Offset(trackStart, centerY),
                        end = Offset(thumbX, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = accentColor,
                    radius = thumbRadiusPx,
                    center = Offset(thumbX, centerY)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(displayPos),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTime(durationMs),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Vector Heart Icon
@Composable
fun HeartIconVector(isFavorite: Boolean, defaultTint: Color, modifier: Modifier = Modifier) {
    val heartColor = if (isFavorite) Color(0xFFFF2A55) else defaultTint

    Spacer(
        modifier = modifier.size(24.dp).drawWithCache {
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
            val strokeStyle = Stroke(width = 2.2f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            onDrawBehind {
                if (isFavorite) {
                    drawPath(path, color = Color(0x66FF2A55), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, color = heartColor, style = Fill)
                    drawPath(path, color = Color(0xFFFF4D79), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawPath(path, color = defaultTint, style = strokeStyle)
                }
            }
        }
    )
}

// Vector Bold Clock Icon with Top Timer Crown
@Composable
fun SleepTimerClockIcon(
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.size(24.dp).drawWithCache {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.54f)
            val radius = w * 0.40f
            val strokeW = 2.3f.dp.toPx()

            onDrawBehind {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.40f, h * 0.06f),
                    end = Offset(w * 0.60f, h * 0.06f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.06f),
                    end = Offset(w * 0.50f, h * 0.14f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeW)
                )
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x - radius * 0.42f, center.y - radius * 0.35f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x + radius * 0.52f, center.y - radius * 0.48f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = 2.4f.dp.toPx(),
                    center = center
                )
            }
        }
    )
}

// Vector 3-Sliders Equalizer Icon
@Composable
fun EqualizerSlidersIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2f.dp.toPx()
        val knobRadius = 3.2f.dp.toPx()

        val y1 = h * 0.22f
        val knobX1 = w * 0.30f
        drawLine(tint, Offset(w * 0.08f, y1), Offset(knobX1 - knobRadius, y1), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(knobX1 + knobRadius, y1), Offset(w * 0.92f, y1), strokeW, StrokeCap.Round)
        drawCircle(tint, knobRadius, Offset(knobX1, y1), style = Stroke(strokeW))

        val y2 = h * 0.50f
        val knobX2 = w * 0.70f
        drawLine(tint, Offset(w * 0.08f, y2), Offset(knobX2 - knobRadius, y2), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(knobX2 + knobRadius, y2), Offset(w * 0.92f, y2), strokeW, StrokeCap.Round)
        drawCircle(tint, knobRadius, Offset(knobX2, y2), style = Stroke(strokeW))

        val y3 = h * 0.78f
        val knobX3 = w * 0.45f
        drawLine(tint, Offset(w * 0.08f, y3), Offset(knobX3 - knobRadius, y3), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(knobX3 + knobRadius, y3), Offset(w * 0.92f, y3), strokeW, StrokeCap.Round)
        drawCircle(tint, knobRadius, Offset(knobX3, y3), style = Stroke(strokeW))
    }
}

// Triple-Line Drag Handle Icon
@Composable
fun ReorderDragHandle(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 3.2f.dp.toPx()

        drawLine(color = tint, start = Offset(w * 0.10f, h * 0.25f), end = Offset(w * 0.90f, h * 0.25f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.10f, h * 0.50f), end = Offset(w * 0.90f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.10f, h * 0.75f), end = Offset(w * 0.90f, h * 0.75f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

// Full Player Sheet with Restored Bold Center Controls & Dynamic Material You
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun FullPlayerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isDark = manager.isDarkMode
    val userAccent = manager.accentColor
    val monoColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val currentQueue = remember(manager.playbackQueue.size, manager.playbackQueue.toList()) {
        if (manager.playbackQueue.isNotEmpty()) manager.playbackQueue.toList()
        else if (manager.currentSong != null) listOf(manager.currentSong!!)
        else emptyList()
    }

    if (currentQueue.isEmpty()) return

    val currentSongIndex = currentQueue.indexOfFirst { it.id == manager.currentSong?.id }.coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = currentSongIndex,
        pageCount = { currentQueue.size }
    )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(manager.currentSong?.id) {
        val targetIdx = currentQueue.indexOfFirst { it.id == manager.currentSong?.id }
        if (targetIdx != -1 && targetIdx != pagerState.currentPage && !pagerState.isScrollInProgress) {
            isProgrammaticScroll = true
            pagerState.scrollToPage(targetIdx)
            isProgrammaticScroll = false
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress to pagerState.settledPage }.collect { (inProgress, settledPage) ->
            if (!inProgress && !isProgrammaticScroll && settledPage in currentQueue.indices) {
                val targetSong = currentQueue[settledPage]
                if (targetSong.id != manager.currentSong?.id) {
                    manager.playSong(targetSong, currentQueue, manager.currentSectionName)
                }
            }
        }
    }

    var showMenuModal by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var showSeekLeftAnim by remember { mutableStateOf(false) }
    var showSeekRightAnim by remember { mutableStateOf(false) }

    LaunchedEffect(showSeekLeftAnim) {
        if (showSeekLeftAnim) {
            delay(500)
            showSeekLeftAnim = false
        }
    }
    LaunchedEffect(showSeekRightAnim) {
        if (showSeekRightAnim) {
            delay(500)
            showSeekRightAnim = false
        }
    }

    val leftSeekAlpha by animateFloatAsState(if (showSeekLeftAnim) 1f else 0f, tween(if (showSeekLeftAnim) 80 else 350), label = "leftAlpha")
    val rightSeekAlpha by animateFloatAsState(if (showSeekRightAnim) 1f else 0f, tween(if (showSeekRightAnim) 80 else 350), label = "rightAlpha")

    val safeIndex = pagerState.settledPage.coerceIn(0, currentQueue.size - 1)
    val activeSong = currentQueue[safeIndex]
    var albumArtBitmap by remember(activeSong.id) { mutableStateOf(manager.getCachedAlbumArt(activeSong.id)) }
    LaunchedEffect(activeSong.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(activeSong)
    }

    val defaultDarkPalette = MaterialYouPalette(
        bgTop = Color(0xFF1E1F28),
        bgBottom = Color(0xFF0B0C10),
        primaryAccent = userAccent,
        surface = Color(0x33FFFFFF),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8)
    )

    val defaultLightPalette = MaterialYouPalette(
        bgTop = Color(0xFFFAF7F2),
        bgBottom = Color(0xFFEBE5DB),
        primaryAccent = userAccent,
        surface = Color(0x66FFFFFF),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569)
    )

    var targetPalette by remember(activeSong.id, isDark, userAccent) {
        mutableStateOf(if (isDark) defaultDarkPalette else defaultLightPalette)
    }

    LaunchedEffect(activeSong.id, albumArtBitmap, isDark, userAccent, manager.isColorfulPlayer) {
        targetPalette = if (manager.isColorfulPlayer) {
            extractMaterialYouPaletteAsync(albumArtBitmap, isDark, userAccent)
        } else {
            if (isDark) defaultDarkPalette else defaultLightPalette
        }
    }

    val animBgTop by animateColorAsState(targetPalette.bgTop, tween(300, easing = FastOutSlowInEasing), label = "bgTop")
    val animBgBottom by animateColorAsState(targetPalette.bgBottom, tween(300, easing = FastOutSlowInEasing), label = "bgBottom")
    val animSurface by animateColorAsState(targetPalette.surface, tween(300, easing = FastOutSlowInEasing), label = "surface")
    val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(300, easing = FastOutSlowInEasing), label = "textPrimary")
    val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(300, easing = FastOutSlowInEasing), label = "textSecondary")

    BackHandler(enabled = showQueueSheet || showMenuModal || showSpeedDialog || showSleepDialog || showEqualizerSheet || showTagEditorDialog || showLyricsDialog || showAddToPlaylistDialog) {
        when {
            showQueueSheet -> showQueueSheet = false
            showMenuModal -> showMenuModal = false
            showSpeedDialog -> showSpeedDialog = false
            showSleepDialog -> showSleepDialog = false
            showEqualizerSheet -> showEqualizerSheet = false
            showTagEditorDialog -> showTagEditorDialog = false
            showLyricsDialog -> showLyricsDialog = false
            showAddToPlaylistDialog -> showAddToPlaylistDialog = false
        }
    }

    val isAnyPlayerDialogOpen = showMenuModal || showSpeedDialog || showSleepDialog ||
            showEqualizerSheet || showTagEditorDialog || showLyricsDialog || showAddToPlaylistDialog || showQueueSheet

    val animatedPlayerBlur by animateDpAsState(
        targetValue = if (isAnyPlayerDialogOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "playerBlurAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animBgTop, animBgBottom)))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 38f && !isAnyPlayerDialogOpen && !showQueueSheet) {
                        onDismiss()
                    } else if (dragAmount < -38f && !isAnyPlayerDialogOpen && !showQueueSheet) {
                        showQueueSheet = true
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(animatedPlayerBlur)
        ) {
            val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTopPadding)
                    .background(animBgTop)
            )

            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = spring(stiffness = 700f, dampingRatio = 0.90f)
                ),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pageIndex ->
                val pageSong = currentQueue[pageIndex]

                var pageBmp by remember(pageSong.id) { mutableStateOf(manager.getCachedAlbumArt(pageSong.id)) }
                LaunchedEffect(pageSong.id) {
                    if (pageBmp == null) pageBmp = manager.loadAlbumArtAsync(pageSong)
                }

                val pageOffset by remember(pageIndex) {
                    derivedStateOf {
                        (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = pageOffset
                            when (manager.pagerTransitionEffect) {
                                PagerTransitionEffect.SLIDE -> {
                                    alpha = 1f
                                    translationX = 0f
                                }
                                PagerTransitionEffect.CASCADE -> {
                                    val scale = (1f - (abs(offset) * 0.08f)).coerceIn(0.92f, 1f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - (abs(offset) * 0.35f)).coerceIn(0.65f, 1f)
                                    translationX = offset * -size.width * 0.12f
                                }
                                PagerTransitionEffect.CROSSFADE -> {
                                    alpha = (1f - abs(offset)).coerceIn(0f, 1f)
                                }
                                PagerTransitionEffect.ROTATE -> {
                                    rotationY = (offset * 18f).coerceIn(-30f, 30f)
                                    cameraDistance = 14f * density
                                    alpha = (1f - (abs(offset) * 0.3f)).coerceIn(0.7f, 1f)
                                }
                                PagerTransitionEffect.TUMBLE -> {
                                    rotationZ = (offset * -12f).coerceIn(-18f, 18f)
                                    val scale = (1f - (abs(offset) * 0.10f)).coerceIn(0.90f, 1f)
                                    scaleX = scale
                                    scaleY = scale
                                }
                                PagerTransitionEffect.PAGE -> {
                                    if (offset < 0) {
                                        translationX = -offset * size.width * 0.45f
                                        val scale = (1f + offset * 0.12f).coerceIn(0.88f, 1f)
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurvedBackArrowIcon(
                            tint = animTextPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    onDismiss()
                                }
                        )
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(animTextSecondary.copy(alpha = 0.4f))
                                .clickable { onDismiss() }
                        )
                        Text(
                            text = "•••",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = animTextPrimary,
                            modifier = Modifier.clickable { showMenuModal = true }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.15f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = userAccent)
                                .clip(RoundedCornerShape(32.dp))
                                .background(animSurface)
                                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                                .pointerInput(pageSong.id) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            manager.triggerHapticFeedback(true)
                                            if (tapOffset.x < size.width / 2f) {
                                                showSeekLeftAnim = true
                                                manager.seekTo((manager.currentPosition - 10000L).coerceAtLeast(0L))
                                            } else {
                                                showSeekRightAnim = true
                                                manager.seekTo((manager.currentPosition + 10000L).coerceAtMost(manager.duration))
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageBmp != null) {
                                Image(
                                    bitmap = pageBmp!!.asImageBitmap(),
                                    contentDescription = "Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("🎵", fontSize = 110.sp)
                            }

                            if (leftSeekAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterStart).background(Color(0x66000000).copy(alpha = 0.45f * leftSeekAlpha)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = leftSeekAlpha }) {
                                        Text("«", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Black)
                                        Text("10s", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (rightSeekAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd).background(Color(0x66000000).copy(alpha = 0.45f * rightSeekAlpha)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = rightSeekAlpha }) {
                                        Text("»", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Black)
                                        Text("10s", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = pageSong.title,
                                color = animTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${formatFileSize(pageSong.size)} • ${if (pageSong.artist.isNotBlank()) pageSong.artist else "Unknown Artist"}",
                                color = animTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        val isCurrentActiveTrack = pageSong.id == manager.currentSong?.id
                        val displayedPos = if (isCurrentActiveTrack) manager.currentPosition else 0L
                        val displayedDuration = pageSong.duration.coerceAtLeast(1L)

                        IsolatedScrubberLeaf(
                            currentPositionMs = displayedPos,
                            durationMs = displayedDuration,
                            accentColor = userAccent,
                            textColor = animTextSecondary,
                            isDark = isDark,
                            onSeek = {
                                manager.triggerHapticFeedback(false)
                                manager.seekTo(it)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RepeatControlIcon(
                                repeatMode = manager.repeatModeState,
                                tint = monoColor,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.toggleRepeat()
                                }.padding(8.dp)
                            )

                            PreviousControlIcon(
                                tint = monoColor,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.playPrevious()
                                }.padding(8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color.White else Color(0xFF0F172A))
                                    .clickable {
                                        manager.triggerHapticFeedback(true)
                                        if (pageSong.id != manager.currentSong?.id) {
                                            manager.playSong(pageSong, currentQueue, manager.currentSectionName)
                                        } else {
                                            manager.togglePlayPause()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val isThisSongPlaying = manager.isPlaying && (pageSong.id == manager.currentSong?.id)
                                val iconTint = if (isDark) Color(0xFF0F172A) else Color.White
                                if (isThisSongPlaying) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Box(modifier = Modifier.size(7.5.dp, 28.dp).clip(RoundedCornerShape(4.dp)).background(iconTint))
                                        Box(modifier = Modifier.size(7.5.dp, 28.dp).clip(RoundedCornerShape(4.dp)).background(iconTint))
                                    }
                                } else {
                                    Canvas(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .offset(x = 2.dp)
                                    ) {
                                        val path = Path().apply {
                                            moveTo(size.width * 0.18f, size.height * 0.12f)
                                            lineTo(size.width * 0.88f, size.height * 0.50f)
                                            lineTo(size.width * 0.18f, size.height * 0.88f)
                                            close()
                                        }
                                        drawPath(path, color = iconTint)
                                    }
                                }
                            }

                            NextControlIcon(
                                tint = monoColor,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.playNext()
                                }.padding(8.dp)
                            )

                            ShuffleControlIcon(
                                isShuffleOn = manager.isShuffleOn,
                                tint = monoColor,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    manager.toggleShuffle()
                                }.padding(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(manager.getCurrentSurfaceColor())
                                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(26.dp))
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EqualizerSlidersIcon(
                                tint = animTextPrimary,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    showEqualizerSheet = true
                                }
                            )

                            val isTimerActive = manager.sleepTimerRemainingSeconds > 0
                            val timerTint = if (isTimerActive) userAccent else animTextPrimary

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(false)
                                    showSleepDialog = true
                                }
                            ) {
                                SleepTimerClockIcon(
                                    isActive = isTimerActive,
                                    tint = timerTint,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isTimerActive) {
                                    Text(
                                        text = "${manager.sleepTimerRemainingSeconds / 60}m",
                                        fontSize = 13.sp,
                                        color = userAccent,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            val isTrackFavorite = manager.allSongs.find { it.id == pageSong.id }?.isFavorite
                                ?: (pageSong.id == manager.currentSong?.id && manager.currentSong?.isFavorite == true)

                            HeartIconVector(
                                isFavorite = isTrackFavorite,
                                defaultTint = animTextPrimary,
                                modifier = Modifier.clickable {
                                    manager.triggerHapticFeedback(true)
                                    val currentTrackInList = manager.allSongs.find { it.id == pageSong.id } ?: pageSong
                                    manager.toggleFavorite(currentTrackInList)
                                }
                            )

                            Text("≡♪", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable {
                                manager.triggerHapticFeedback(false)
                                showQueueSheet = true
                            })

                            Text("•••", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable {
                                manager.triggerHapticFeedback(false)
                                showMenuModal = true
                            })
                        }
                    }
                }
            }
        }

        if (isAnyPlayerDialogOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            )
        }

        if (showMenuModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showMenuModal = false },
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${activeSong.title} - ${if (activeSong.artist.isNotBlank()) activeSong.artist else "Unknown"}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = manager.getCurrentTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        MenuRow("👤", "Artist: ${if (activeSong.artist.isNotBlank()) activeSong.artist else "Unknown"}", isDark) { showMenuModal = false }
                        MenuRow("📜", "Lyrics", isDark) { showMenuModal = false; showLyricsDialog = true }
                        MenuRow("🔗", "Share", isDark) {
                            showMenuModal = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, activeSong.uri)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                        MenuRow("🎚️", "Equalizer", isDark) { showMenuModal = false; showEqualizerSheet = true }
                        MenuRow("🏷", "Tag Editor", isDark) { showMenuModal = false; showTagEditorDialog = true }
                        MenuRow("➕", "Add to Playlist", isDark) { showMenuModal = false; showAddToPlaylistDialog = true }
                        MenuRow("⏱️", "Playback Speed", isDark) { showMenuModal = false; showSpeedDialog = true }

                        MenuRow("🗑", "Delete from Device", isDark, isDanger = true) {
                            showMenuModal = false
                            manager.deleteSongFromDevice(activeSong)
                            onDismiss()
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showMenuModal = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = manager.getCurrentTextColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showEqualizerSheet) EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        if (showSpeedDialog) MagneticSpeedDialog(manager = manager, onDismiss = { showSpeedDialog = false })
        if (showSleepDialog) SleepTimerDialog(manager = manager, onDismiss = { showSleepDialog = false })

        AnimatedVisibility(
            visible = showQueueSheet,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f)),
            modifier = Modifier.fillMaxSize().zIndex(20f)
        ) {
            QueueSheet(
                manager = manager,
                bgTop = animBgTop,
                bgBottom = animBgBottom,
                surfaceColor = animSurface,
                textColor = animTextPrimary,
                subTextColor = animTextSecondary,
                accent = userAccent,
                isDark = isDark,
                onDismiss = { showQueueSheet = false }
            )
        }

        if (showTagEditorDialog) {
            TagEditorDialog(manager = manager, song = activeSong, onDismiss = { showTagEditorDialog = false })
        }
        if (showLyricsDialog) LyricsDialog(song = activeSong, manager = manager, onDismiss = { showLyricsDialog = false })
        if (showAddToPlaylistDialog) AddToPlaylistDialog(manager = manager, song = activeSong, onDismiss = { showAddToPlaylistDialog = false })
    }
}

// Queue Sheet with 120Hz Floating Drag, Symmetrical 3-Zone Edge Auto-Scroll & Virtual Hover Swapping
@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun QueueSheet(
    manager: MusicManager,
    bgTop: Color,
    bgBottom: Color,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color,
    accent: Color,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val itemHeightPx = with(density) { 72.dp.toPx() }
    val edgeZonePx = with(density) { 70.dp.toPx() }
    val maxScrollSpeedPxPerSec = with(density) { 850.dp.toPx() }

    val tempQueue = remember { mutableStateListOf<Song>() }

    LaunchedEffect(manager.playbackQueue.toList()) {
        tempQueue.clear()
        tempQueue.addAll(manager.playbackQueue)
    }

    LaunchedEffect(Unit) {
        val curIdx = manager.playbackQueue.indexOfFirst { it.id == manager.currentSong?.id }
        if (curIdx != -1) {
            val targetScrollIdx = (curIdx - 1).coerceAtLeast(0)
            listState.scrollToItem(targetScrollIdx)
        }
    }

    var draggingSongId by remember { mutableStateOf<Long?>(null) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    var fingerYInList by remember { mutableFloatStateOf(-1f) }
    var grabOffsetY by remember { mutableFloatStateOf(itemHeightPx / 2f) }

    LaunchedEffect(draggingSongId) {
        if (draggingSongId != null) {
            var lastFrameTimeNanos = 0L

            while (draggingSongId != null) {
                withFrameNanos { currentFrameTimeNanos ->
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = currentFrameTimeNanos
                        return@withFrameNanos
                    }
                    val dt = ((currentFrameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    lastFrameTimeNanos = currentFrameTimeNanos

                    if (containerHeightPx > (edgeZonePx * 2f)) {
                        val bottomTriggerY = containerHeightPx - edgeZonePx

                        val scrollSpeed: Float = when {
                            fingerYInList <= edgeZonePx -> {
                                val ratio = (1f - (fingerYInList.coerceAtLeast(0f) / edgeZonePx)).coerceIn(0f, 1f)
                                -(maxScrollSpeedPxPerSec * (ratio * ratio))
                            }
                            fingerYInList >= bottomTriggerY -> {
                                val distFromBottom = (containerHeightPx - fingerYInList).coerceAtLeast(0f)
                                val ratio = (1f - (distFromBottom / edgeZonePx)).coerceIn(0f, 1f)
                                (maxScrollSpeedPxPerSec * (ratio * ratio))
                            }
                            else -> 0f
                        }

                        if (scrollSpeed != 0f) {
                            listState.dispatchRawDelta(scrollSpeed * dt)

                            val currentId = draggingSongId
                            if (currentId != null) {
                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                val fromIndex = tempQueue.indexOfFirst { it.id == currentId }

                                if (fromIndex != -1 && visibleItems.isNotEmpty()) {
                                    val targetIndex = when {
                                        fingerYInList <= edgeZonePx -> visibleItems.first().index
                                        fingerYInList >= bottomTriggerY -> visibleItems.last().index
                                        else -> visibleItems.find { item ->
                                            val itemCenter = item.offset + (item.size / 2f)
                                            abs(fingerYInList - itemCenter) < (item.size * 0.45f)
                                        }?.index
                                    }

                                    if (targetIndex != null) {
                                        val clampedTarget = targetIndex.coerceIn(0, tempQueue.size - 1)
                                        if (clampedTarget != fromIndex) {
                                            manager.triggerHapticFeedback(false)
                                            val moved = tempQueue.removeAt(fromIndex)
                                            tempQueue.add(clampedTarget, moved)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 && draggingSongId == null) {
                    if (available.y > 20f) {
                        onDismiss()
                        return available
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 15f && draggingSongId == null) {
                    onDismiss()
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y > 350f && listState.firstVisibleItemIndex == 0 && draggingSongId == null) {
                    onDismiss()
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (draggingSongId == null) onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .nestedScroll(nestedScrollConnection)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 26f && draggingSongId == null) {
                            onDismiss()
                        }
                    }
                }
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassBackButton(isDark = isDark, onClick = onDismiss)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Playing Queue (${tempQueue.size})",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = manager.getCurrentTextColor()
                            )
                            Text(
                                text = "Hold and drag bars to reorder tracks",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = manager.getCurrentSurfaceColor()),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, manager.getGlassBorderBrush())
                    ) {
                        Text("Close", color = manager.getCurrentTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            containerHeightPx = coordinates.size.height.toFloat()
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = tempQueue,
                            key = { _, song -> song.id },
                            contentType = { _, _ -> "queue_item_row" }
                        ) { _, song ->
                            val isCur = song.id == manager.currentSong?.id
                            val isBeingDragged = draggingSongId == song.id
                            var rowTopInParent by remember { mutableFloatStateOf(0f) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement(spring(stiffness = 650f, dampingRatio = 0.90f))
                                    .onGloballyPositioned { coordinates ->
                                        rowTopInParent = coordinates.positionInParent().y
                                    }
                                    .graphicsLayer {
                                        alpha = if (isBeingDragged) 0.20f else 1f
                                    }
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isBeingDragged) accent.copy(alpha = 0.12f)
                                        else if (isCur) accent.copy(alpha = 0.20f)
                                        else manager.getCurrentSurfaceColor()
                                    )
                                    .border(
                                        width = if (isBeingDragged || isCur) 1.5.dp else 1.2.dp,
                                        brush = if (isBeingDragged || isCur) Brush.linearGradient(listOf(accent, accent)) else manager.getGlassBorderBrush(),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        if (draggingSongId == null) {
                                            manager.triggerHapticFeedback(false)
                                            manager.playSong(song, manager.playbackQueue, manager.currentSectionName)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isCur) accent.copy(alpha = 0.25f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCur) "▶" else "•",
                                        color = if (isCur) accent else Color(0xFF64748B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCur) accent else manager.getCurrentTextColor(),
                                        fontSize = 15.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown Artist"}",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .pointerInput(song.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    manager.triggerHapticFeedback(true)
                                                    draggingSongId = song.id
                                                    grabOffsetY = offset.y
                                                    fingerYInList = rowTopInParent + offset.y
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    fingerYInList += dragAmount.y

                                                    val currentId = draggingSongId
                                                    val bottomTriggerY = containerHeightPx - edgeZonePx
                                                    if (currentId != null && fingerYInList > edgeZonePx && fingerYInList < bottomTriggerY) {
                                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                                        val hitItem = visibleItems.find { item ->
                                                            val itemCenter = item.offset + (item.size / 2f)
                                                            abs(fingerYInList - itemCenter) < (item.size * 0.45f)
                                                        }
                                                        if (hitItem != null && hitItem.key != currentId) {
                                                            val fromIndex = tempQueue.indexOfFirst { it.id == currentId }
                                                            val toIndex = hitItem.index.coerceIn(0, tempQueue.size - 1)
                                                            if (fromIndex != -1 && fromIndex != toIndex) {
                                                                manager.triggerHapticFeedback(false)
                                                                val movedSong = tempQueue.removeAt(fromIndex)
                                                                tempQueue.add(toIndex, movedSong)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    manager.triggerHapticFeedback(true)
                                                    val finalIndex = tempQueue.indexOfFirst { it.id == song.id }
                                                    val originalIndex = manager.playbackQueue.indexOfFirst { it.id == song.id }
                                                    if (finalIndex != -1 && originalIndex != -1 && finalIndex != originalIndex) {
                                                        manager.moveQueueItem(originalIndex, finalIndex)
                                                    }
                                                    draggingSongId = null
                                                    fingerYInList = -1f
                                                },
                                                onDragCancel = {
                                                    draggingSongId = null
                                                    fingerYInList = -1f
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ReorderDragHandle(
                                        tint = if (isCur) accent else Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (draggingSongId != null && fingerYInList >= -150f) {
                        val draggedSong = tempQueue.find { it.id == draggingSongId }
                        if (draggedSong != null) {
                            val floatingTop = (fingerYInList - grabOffsetY).coerceIn(
                                -20f,
                                (containerHeightPx - itemHeightPx + 40f).coerceAtLeast(0f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(0, floatingTop.roundToInt()) }
                                    .zIndex(100f)
                                    .graphicsLayer {
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                    }
                                    .shadow(24.dp, RoundedCornerShape(18.dp), spotColor = accent)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accent.copy(alpha = 0.28f))
                                    .background(manager.getCurrentDialogColor())
                                    .border(1.5.dp, accent, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "▶",
                                        color = accent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draggedSong.title,
                                        color = accent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatFileSize(draggedSong.size)} • ${if (draggedSong.artist.isNotBlank()) draggedSong.artist else "Unknown Artist"}",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ReorderDragHandle(
                                        tint = accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = manager.getCurrentSurfaceColor()),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, manager.getGlassBorderBrush()),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Close",
                        color = manager.getCurrentTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Equalizer Sheet with Frosted Glass Styling
@Composable
fun EqualizerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val accent = manager.accentColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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
                        Text("Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(if (manager.isEqEnabled) manager.selectedEqPreset else "Off (Original Audio)", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Sound Presets", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(manager.eqPresetNames, key = { it }) { preset ->
                                val isSel = manager.selectedEqPreset == preset && manager.isEqEnabled
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                                        .clickable {
                                            manager.triggerHapticFeedback(false)
                                            manager.applyEqPreset(preset)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        color = if (isSel) Color.White else textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Frequency Response (-15dB to +15dB)", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until manager.eqBandsCount) {
                                    val freq = manager.eqCenterFreqs[i] ?: (60 * (i + 1) * (i + 1))
                                    val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                                    val level = if (manager.isEqEnabled) (manager.eqBandLevels[i] ?: 0) else 0
                                    val levelDb = level / 100

                                    VerticalBandFader(
                                        level = level,
                                        minLevel = manager.eqMinLevel,
                                        maxLevel = manager.eqMaxLevel,
                                        dbLabel = if (levelDb > 0) "+$levelDb" else "$levelDb",
                                        freqLabel = freqLabel,
                                        accentColor = if (manager.isEqEnabled) accent else Color.Gray,
                                        isDark = isDark,
                                        onLevelChange = { newLevel ->
                                            manager.triggerHapticFeedback(false)
                                            manager.setEqBandLevel(i, newLevel)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .padding(16.dp)
                        ) {
                            Text("Bass Boost: ${if (manager.isEqEnabled) manager.bassBoostPercent else 0}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = if (manager.isEqEnabled) manager.bassBoostPercent.toFloat() else 0f,
                                onValueChange = {
                                    val rounded = it.toInt()
                                    if (rounded != manager.bassBoostPercent) {
                                        manager.triggerHapticFeedback(false)
                                        manager.setBassBoost(rounded)
                                    }
                                },
                                valueRange = 0f..100f,
                                enabled = manager.isEqEnabled,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("3D Surround (Virtualizer): ${if (manager.isEqEnabled) manager.virtualizerPercent else 0}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = if (manager.isEqEnabled) manager.virtualizerPercent.toFloat() else 0f,
                                onValueChange = {
                                    val rounded = it.toInt()
                                    if (rounded != manager.virtualizerPercent) {
                                        manager.triggerHapticFeedback(false)
                                        manager.setVirtualizer(rounded)
                                    }
                                },
                                valueRange = 0f..100f,
                                enabled = manager.isEqEnabled,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Remove Vocals", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Cancels center vocals so you only hear music.", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = manager.isRemoveVocals,
                                    onCheckedChange = {
                                        manager.triggerHapticFeedback(false)
                                        manager.toggleRemoveVocals(it)
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Stop Bass (Full Cut)", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Cuts sub-bass frequencies.", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = manager.isStopBass,
                                    onCheckedChange = {
                                        manager.triggerHapticFeedback(false)
                                        manager.toggleStopBass(it)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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

// Vertical Line Mixer Slider for Frequency Bands
@Composable
fun VerticalBandFader(
    level: Int,
    minLevel: Int,
    maxLevel: Int,
    dbLabel: String,
    freqLabel: String,
    accentColor: Color,
    isDark: Boolean,
    onLevelChange: (Int) -> Unit
) {
    val totalRange = (maxLevel - minLevel).coerceAtLeast(1)
    val fraction = ((level - minLevel).toFloat() / totalRange.toFloat()).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(42.dp)
    ) {
        Text(
            text = dbLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(140.dp)
                .pointerInput(minLevel, maxLevel) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val topY = 8.dp.toPx()
                        val bottomY = size.height - 8.dp.toPx()
                        val trackHeight = (bottomY - topY).coerceAtLeast(1f)

                        val newFraction = 1f - ((down.position.y - topY) / trackHeight).coerceIn(0f, 1f)
                        onLevelChange((minLevel + newFraction * totalRange).toInt())

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                val moveFraction = 1f - ((change.position.y - topY) / trackHeight).coerceIn(0f, 1f)
                                onLevelChange((minLevel + moveFraction * totalRange).toInt())
                            } else {
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val topY = 8.dp.toPx()
                val bottomY = size.height - 8.dp.toPx()
                val trackHeight = bottomY - topY
                val knobY = bottomY - (fraction * trackHeight)

                drawLine(
                    color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                    start = Offset(centerX, topY),
                    end = Offset(centerX, bottomY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = accentColor,
                    start = Offset(centerX, bottomY),
                    end = Offset(centerX, knobY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, knobY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = freqLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MenuRow(icon: String, text: String, isDark: Boolean, isDanger: Boolean = false, onClick: () -> Unit) {
    val textColor = when {
        isDanger -> Color(0xFFEF4444)
        isDark -> Color(0xFFF8FAFC)
        else -> Color(0xFF0F172A)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// 🌟 Mini Player Dock with Frosted Glass Surface, Optical Depth Blur & Swipe-to-Dismiss Gesture
@UnstableApi
@Composable
fun MiniPlayerDock(
    manager: MusicManager,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val song = manager.currentSong ?: return
    val accent = manager.accentColor
    val isDark = manager.isDarkMode

    // Dynamic frosted glass properties from settings
    val surfaceColor = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val isFrosted = manager.isFrostedGlassEnabled
    val opacity = manager.frostedGlassOpacity

    // Dynamic blur depth: higher blur when slider is transparent, gently softening as it becomes solid
    val blurRadius = if (isFrosted) {
        (26.dp * (1f - (opacity * 0.35f))).coerceAtLeast(8.dp)
    } else {
        0.dp
    }

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(
                elevation = if (isDark) 16.dp else 10.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = accent.copy(alpha = 0.40f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.75f) else Color(0x44000000)
            )
            .clip(RoundedCornerShape(22.dp))
            .border(1.2.dp, glassBorderBrush, RoundedCornerShape(22.dp))
            .pointerInput(manager.isPlaying) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -22f) {
                        onClick()
                    } else if (dragAmount > 32f && !manager.isPlaying) {
                        manager.triggerHapticFeedback(false)
                        onDismiss()
                    }
                }
            }
            .clickable { onClick() }
    ) {
        // 🔮 BACKDROP BLUR & FROSTED GLASS LAYER:
        // Diffuses background scrolling items into a smooth bokeh while applying theme tint
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier
                )
                .background(surfaceColor)
        )

        // Specular glass highlight reflection across the top
        if (isFrosted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 🎵 FOREGROUND CONTENT (Keeps album art, text labels, and buttons sharp and readable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(
                        bitmap = albumArtBitmap!!.asImageBitmap(),
                        contentDescription = "Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("🎵", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = manager.getCurrentTextColor(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}",
                    color = accent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = accent)
                    .clip(CircleShape)
                    .background(accent)
                    .border(1.2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        manager.triggerHapticFeedback(true)
                        manager.togglePlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (manager.isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(3.5.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                        Box(modifier = Modifier.size(3.5.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = 1.2.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width * 0.18f, size.height * 0.12f)
                            lineTo(size.width * 0.88f, size.height * 0.50f)
                            lineTo(size.width * 0.18f, size.height * 0.88f)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }
            }
        }
    }
}

// Sleep Timer Dialog with Frosted Glass Surface
@UnstableApi
@Composable
fun SleepTimerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val accent = manager.accentColor
    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(15) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Sleep Timer", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically stop playback after a set time.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Text("✕", color = Color(0xFF64748B), fontSize = 20.sp, modifier = Modifier.clickable { onDismiss() })
                }

                if (manager.sleepTimerRemainingSeconds > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1AEF4444)).padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("⏳ Timer Active", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${manager.sleepTimerRemainingSeconds / 60}m remaining", color = textColor, fontSize = 12.sp)
                            }
                            Button(onClick = {
                                manager.triggerHapticFeedback(true)
                                manager.endSleepTimer()
                                onDismiss()
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(10.dp)) {
                                Text("End", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Presets", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable {
                                manager.triggerHapticFeedback(false)
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
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable {
                        manager.triggerHapticFeedback(false)
                        manager.setSleepTimerToEndOfTrack()
                        onDismiss()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text("End of current track", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Custom Timer", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hours: $customHours", color = Color(0xFF64748B), fontSize = 12.sp)
                        Slider(
                            value = customHours.toFloat(),
                            onValueChange = {
                                val rounded = it.toInt()
                                if (rounded != customHours) {
                                    manager.triggerHapticFeedback(false)
                                    customHours = rounded
                                }
                            },
                            valueRange = 0f..24f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minutes: $customMinutes", color = Color(0xFF64748B), fontSize = 12.sp)
                        Slider(
                            value = customMinutes.toFloat(),
                            onValueChange = {
                                val rounded = it.toInt()
                                if (rounded != customMinutes) {
                                    manager.triggerHapticFeedback(false)
                                    customMinutes = rounded
                                }
                            },
                            valueRange = 1f..60f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                    Button(onClick = {
                        manager.triggerHapticFeedback(true)
                        manager.setSleepTimer((customHours * 60) + customMinutes)
                        onDismiss()
                    }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                        Text("Set", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
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

// Lyrics Dialog with Frosted Glass Surface
@Composable
fun LyricsDialog(song: Song, manager: MusicManager, onDismiss: () -> Unit) {
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
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column {
                Text("Lyrics", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("No synchronized lyrics found for \"${song.title}\".", color = Color(0xFF64748B), fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onDismiss() },
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

// Add to Playlist Dialog with Frosted Glass Surface & High Contrast Text Input
@Composable
fun AddToPlaylistDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val accent = manager.accentColor
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Playlist", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            isCreatingNew = !isCreatingNew
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isCreatingNew) "Cancel" else "+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isCreatingNew) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Playlist Name", color = Color(0xFF64748B)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = manager.getCurrentBorderColor(),
                                cursorColor = accent
                            )
                        )
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    manager.triggerHapticFeedback(true)
                                    manager.createPlaylistAndAddSong(newPlaylistName.trim(), song.id)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false).height(240.dp)) {
                    items(
                        items = manager.customPlaylists,
                        key = { it.id },
                        contentType = { "playlist_picker_row" }
                    ) { pl ->
                        val isAlreadyIn = song.id in pl.songIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    if (isAlreadyIn) {
                                        manager.removeSongFromPlaylist(song.id, pl)
                                    } else {
                                        manager.addSongToPlaylist(song.id, pl)
                                    }
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                GlassmorphicFolderIcon(folderColor = Color(pl.iconColorHex), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (isAlreadyIn) {
                                Text("✓ Added", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(12.dp)) {
                    Text("Close", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Magnetic Speed Dialog with Frosted Glass Surface
@Composable
fun MagneticSpeedDialog(manager: MusicManager, onDismiss: () -> Unit) {
    var speed by remember { mutableFloatStateOf(manager.playbackSpeed) }
    val isDark = manager.isDarkMode
    val accent = manager.accentColor
    val textColor = manager.getCurrentTextColor()
    val prominentSteps = remember {
        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 2.75f, 3.0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0x66000000) else Color(0x40000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(manager.getCurrentDialogColor())
                .border(1.2.dp, manager.getGlassBorderBrush(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Playback Speed: ${String.format(Locale.US, "%.2fx", speed)}",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(prominentSteps) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val horizontalPaddingPx = 16.dp.toPx()
                                val usableWidth = (size.width - (horizontalPaddingPx * 2)).coerceAtLeast(1f)

                                fun updateFromX(touchX: Float) {
                                    val frac = ((touchX - horizontalPaddingPx) / usableWidth).coerceIn(0f, 1f)
                                    val rawVal = 0.25f + (frac * (3.0f - 0.25f))
                                    var closest = prominentSteps.first()
                                    var minDiff = Float.MAX_VALUE
                                    for (st in prominentSteps) {
                                        val diff = abs(rawVal - st)
                                        if (diff < minDiff) {
                                            minDiff = diff
                                            closest = st
                                        }
                                    }
                                    if (closest != speed) {
                                        manager.triggerHapticFeedback(closest == 1.0f)
                                        speed = manager.setMagneticSpeed(closest)
                                    }
                                }

                                updateFromX(down.position.x)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        change.consume()
                                        updateFromX(change.position.x)
                                    } else {
                                        break
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        val centerY = size.height / 2f
                        val horizontalPaddingPx = 16.dp.toPx()
                        val usableWidth = size.width - (horizontalPaddingPx * 2)

                        val currentFraction = ((speed - 0.25f) / (3.0f - 0.25f)).coerceIn(0f, 1f)
                        val activeX = horizontalPaddingPx + (currentFraction * usableWidth)

                        drawRoundRect(
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            topLeft = Offset(horizontalPaddingPx, centerY - 2.5.dp.toPx()),
                            size = Size(usableWidth, 5.dp.toPx()),
                            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                        )

                        if (activeX > horizontalPaddingPx) {
                            drawRoundRect(
                                color = accent.copy(alpha = 0.5f),
                                topLeft = Offset(horizontalPaddingPx, centerY - 2.5.dp.toPx()),
                                size = Size(activeX - horizontalPaddingPx, 5.dp.toPx()),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                        }

                        prominentSteps.forEach { step ->
                            val stepFraction = (step - 0.25f) / (3.0f - 0.25f)
                            val dotX = horizontalPaddingPx + (stepFraction * usableWidth)
                            val is1X = (step == 1.0f)
                            val isSelected = (step == speed)

                            drawCircle(
                                color = when {
                                    isSelected -> accent
                                    is1X -> accent.copy(alpha = 0.85f)
                                    isDark -> Color(0xFF64748B)
                                    else -> Color(0xFF94A3B8)
                                },
                                radius = if (is1X || isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                                center = Offset(dotX, centerY)
                            )
                        }

                        val indicatorWidth = 5.dp.toPx()
                        val indicatorHeight = 26.dp.toPx()
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(activeX - (indicatorWidth / 2f), centerY - (indicatorHeight / 2f)),
                            size = Size(indicatorWidth, indicatorHeight),
                            cornerRadius = CornerRadius(indicatorWidth / 2f, indicatorWidth / 2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.25x", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("1.0x (Normal)", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("3.0x", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        manager.triggerHapticFeedback(true)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Repeat Control Icon
@Composable
fun RepeatControlIcon(repeatMode: Int, tint: Color, modifier: Modifier = Modifier) {
    val isActive = repeatMode != Player.REPEAT_MODE_OFF
    val alpha = if (isActive) 1f else 0.4f
    Box(modifier = modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier.fillMaxSize().drawWithCache {
                val stroke = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val w = size.width
                val h = size.height
                val color = tint.copy(alpha = alpha)

                val p1 = Path().apply {
                    moveTo(w * 0.22f, h * 0.55f)
                    lineTo(w * 0.22f, h * 0.30f)
                    lineTo(w * 0.78f, h * 0.30f)
                }
                val a1 = Path().apply {
                    moveTo(w * 0.66f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.30f)
                    lineTo(w * 0.66f, h * 0.42f)
                }
                val p2 = Path().apply {
                    moveTo(w * 0.78f, h * 0.45f)
                    lineTo(w * 0.78f, h * 0.70f)
                    lineTo(w * 0.22f, h * 0.70f)
                }
                val a2 = Path().apply {
                    moveTo(w * 0.34f, h * 0.58f)
                    lineTo(w * 0.18f, h * 0.70f)
                    lineTo(w * 0.34f, h * 0.82f)
                }
                onDrawBehind {
                    drawPath(p1, color, style = stroke)
                    drawPath(a1, color, style = stroke)
                    drawPath(p2, color, style = stroke)
                    drawPath(a2, color, style = stroke)
                }
            }
        )
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            Text("1", color = tint, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

// Previous Control Icon
@Composable
fun PreviousControlIcon(tint: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.size(28.dp).drawWithCache {
            val w = size.width
            val h = size.height
            val tri = Path().apply {
                moveTo(w * 0.80f, h * 0.20f)
                lineTo(w * 0.34f, h * 0.50f)
                lineTo(w * 0.80f, h * 0.80f)
                close()
            }
            val strokeW = 3.5f.dp.toPx()
            onDrawBehind {
                drawLine(color = tint, start = Offset(w * 0.22f, h * 0.20f), end = Offset(w * 0.22f, h * 0.80f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawPath(tri, color = tint)
            }
        }
    )
}

// Next Control Icon
@Composable
fun NextControlIcon(tint: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.size(28.dp).drawWithCache {
            val w = size.width
            val h = size.height
            val tri = Path().apply {
                moveTo(w * 0.20f, h * 0.20f)
                lineTo(w * 0.66f, h * 0.50f)
                lineTo(w * 0.20f, h * 0.80f)
                close()
            }
            val strokeW = 3.5f.dp.toPx()
            onDrawBehind {
                drawPath(tri, color = tint)
                drawLine(color = tint, start = Offset(w * 0.78f, h * 0.20f), end = Offset(w * 0.78f, h * 0.80f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
        }
    )
}

// Shuffle Control Icon
@Composable
fun ShuffleControlIcon(isShuffleOn: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val alpha = if (isShuffleOn) 1f else 0.4f
    Spacer(
        modifier = modifier.size(26.dp).drawWithCache {
            val stroke = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = size.width
            val h = size.height
            val color = tint.copy(alpha = alpha)

            val p1 = Path().apply {
                moveTo(w * 0.18f, h * 0.28f)
                lineTo(w * 0.36f, h * 0.28f)
                lineTo(w * 0.64f, h * 0.72f)
                lineTo(w * 0.82f, h * 0.72f)
            }
            val a1 = Path().apply {
                moveTo(w * 0.68f, h * 0.60f)
                lineTo(w * 0.84f, h * 0.72f)
                lineTo(w * 0.68f, h * 0.84f)
            }
            val p2 = Path().apply {
                moveTo(w * 0.18f, h * 0.72f)
                lineTo(w * 0.36f, h * 0.72f)
                lineTo(w * 0.46f, h * 0.56f)
            }
            val p2b = Path().apply {
                moveTo(w * 0.54f, h * 0.44f)
                lineTo(w * 0.64f, h * 0.28f)
                lineTo(w * 0.82f, h * 0.28f)
            }
            val a2 = Path().apply {
                moveTo(w * 0.68f, h * 0.16f)
                lineTo(w * 0.84f, h * 0.28f)
                lineTo(w * 0.68f, h * 0.40f)
            }
            onDrawBehind {
                drawPath(p1, color, style = stroke)
                drawPath(a1, color, style = stroke)
                drawPath(p2, color, style = stroke)
                drawPath(p2b, color, style = stroke)
                drawPath(a2, color, style = stroke)
            }
        }
    )
}
