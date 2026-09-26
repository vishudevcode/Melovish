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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

@Composable
fun GlassBackButton(isDark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9))
            .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CurvedBackArrowIcon(tint = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.size(20.dp))
    }
}

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
                quadraticBezierTo(w * 0.88f, h * 0.28f, w * 0.88f, h * 0.35f)
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
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DefaultProfileAvatar(modifier: Modifier = Modifier, backgroundColor: Color = Color(0xFF030712)) {
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
                drawCircle(color = backgroundColor, radius = w / 2f, center = Offset(w / 2f, h / 2f))
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

// Full Player Sheet: 120 FPS Pager with Instant Single-Step Track Switching & Dynamic Queue Sheet
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
        snapshotFlow { pagerState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling && !isProgrammaticScroll) {
                val settledPage = pagerState.currentPage
                if (settledPage in currentQueue.indices) {
                    val targetSong = currentQueue[settledPage]
                    if (targetSong.id != manager.currentSong?.id) {
                        manager.playSong(targetSong, currentQueue, manager.currentSectionName)
                    }
                }
            }
        }
    }

    // Modal Sheet & Dialog States
    var showMenuModal by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Prioritized BackHandler for 1-Step Back Navigation
    BackHandler(enabled = true) {
        when {
            showQueueSheet -> showQueueSheet = false
            showEqualizerSheet -> showEqualizerSheet = false
            showSpeedDialog -> showSpeedDialog = false
            showSleepDialog -> showSleepDialog = false
            showTagEditorDialog -> showTagEditorDialog = false
            showLyricsDialog -> showLyricsDialog = false
            showAddToPlaylistDialog -> showAddToPlaylistDialog = false
            showMenuModal -> showMenuModal = false
            else -> onDismiss()
        }
    }

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

    val activeSong = manager.currentSong ?: currentQueue[pagerState.currentPage]
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

    val animBgTop by animateColorAsState(targetPalette.bgTop, tween(350, easing = FastOutSlowInEasing), label = "bgTop")
    val animBgBottom by animateColorAsState(targetPalette.bgBottom, tween(350, easing = FastOutSlowInEasing), label = "bgBottom")
    val animSurface by animateColorAsState(targetPalette.surface, tween(350, easing = FastOutSlowInEasing), label = "surface")
    val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(350, easing = FastOutSlowInEasing), label = "textPrimary")
    val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(350, easing = FastOutSlowInEasing), label = "textSecondary")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animBgTop, animBgBottom)))
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 38f) {
                        onDismiss()
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(stiffness = 550f, dampingRatio = 0.82f)
            ),
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageSong = currentQueue[pageIndex]

            var pageBmp by remember(pageSong.id) { mutableStateOf(manager.getCachedAlbumArt(pageSong.id)) }
            LaunchedEffect(pageSong.id) {
                if (pageBmp == null) pageBmp = manager.loadAlbumArtAsync(pageSong)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
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
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurvedBackArrowIcon(
                        tint = animTextPrimary,
                        modifier = Modifier.size(24.dp).clickable { onDismiss() }
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

                // Upper Section: Album Artwork
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

                // Lower Section: Metadata, Scrubber, Playback Controls, & Dock
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
                    IsolatedScrubberLeaf(
                        currentPositionMs = if (isCurrentActiveTrack) manager.currentPosition else 0L,
                        durationMs = if (isCurrentActiveTrack) manager.duration else pageSong.duration,
                        accentColor = userAccent,
                        textColor = animTextSecondary,
                        isDark = isDark,
                        onSeek = { manager.seekTo(it) }
                    )

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White else Color(0xFF0F172A))
                                .clickable {
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

                    // Bottom Dock: Swipe Up anywhere on the dock to reveal Queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(animSurface)
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(26.dp))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -32f) {
                                        showQueueSheet = true
                                    }
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 24.dp),
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

                        val isTrackFavorite = manager.allSongs.find { it.id == pageSong.id }?.isFavorite
                            ?: (pageSong.id == manager.currentSong?.id && manager.currentSong?.isFavorite == true)

                        HeartIconVector(
                            isFavorite = isTrackFavorite,
                            defaultTint = animTextPrimary,
                            modifier = Modifier.clickable {
                                val currentTrackInList = manager.allSongs.find { it.id == pageSong.id } ?: pageSong
                                manager.toggleFavorite(currentTrackInList)
                            }
                        )

                        Text("≡♪", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable { showQueueSheet = true })

                        Text("•••", fontSize = 24.sp, fontWeight = FontWeight.Black, color = animTextPrimary, modifier = Modifier.clickable { showMenuModal = true })
                    }
                }
            }
        }

        // Action Menu Dialog
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
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                        .clickable(enabled = false) {}
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${activeSong.title} - ${if (activeSong.artist.isNotBlank()) activeSong.artist else "Unknown"}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
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
                        MenuRow("⏱️", "Playback Speed", isDark) { showMenuModal = false; showSpeedDialog = true }
                        MenuRow("🎚️", "Equalizer", isDark) { showMenuModal = false; showEqualizerSheet = true }
                        MenuRow("🏷️", "Tag Editor", isDark) { showMenuModal = false; showTagEditorDialog = true }
                        MenuRow("➕", "Add to Playlist", isDark) { showMenuModal = false; showAddToPlaylistDialog = true }
                        MenuRow("🗑️", "Delete from Device", isDark, isDanger = true) {
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
                            Text("Cancel", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Shared Unified Modal Sheets
        if (showEqualizerSheet) EqualizerSheet(manager = manager, onDismiss = { showEqualizerSheet = false })
        if (showSpeedDialog) MagneticSpeedDialog(manager = manager, onDismiss = { showSpeedDialog = false })
        if (showSleepDialog) SleepTimerDialog(manager = manager, onDismiss = { showSleepDialog = false })

        // Dynamic Material You Styled Queue Sheet (Opens via Swipe Up / Button)
        AnimatedVisibility(
            visible = showQueueSheet,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(320, easing = FastOutSlowInEasing))
        ) {
            QueueSheet(
                manager = manager,
                palette = targetPalette,
                onDismiss = { showQueueSheet = false }
            )
        }

        if (showTagEditorDialog) TagEditorDialog(manager = manager, song = activeSong, onDismiss = { showTagEditorDialog = false })
        if (showLyricsDialog) LyricsDialog(song = activeSong, isDark = isDark, onDismiss = { showLyricsDialog = false })
        if (showAddToPlaylistDialog) AddToPlaylistDialog(manager = manager, song = activeSong, onDismiss = { showAddToPlaylistDialog = false })
    }
}

// Unified Equalizer Modal Sheet Shared by Settings Page & Music Player
@Composable
fun EqualizerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

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
                .fillMaxHeight(0.88f)
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
                        Text("Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(manager.selectedEqPreset, fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
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
                                val isSel = manager.selectedEqPreset == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) accent else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                                        .clickable { manager.applyEqPreset(preset) }
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
                                    val level = manager.eqBandLevels[i] ?: 0
                                    val levelDb = level / 100

                                    VerticalBandFader(
                                        level = level,
                                        minLevel = manager.eqMinLevel,
                                        maxLevel = manager.eqMaxLevel,
                                        dbLabel = if (levelDb > 0) "+$levelDb" else "$levelDb",
                                        freqLabel = freqLabel,
                                        accentColor = accent,
                                        isDark = isDark,
                                        onLevelChange = { newLevel ->
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
                            Text("Bass Boost: ${manager.bassBoostPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = manager.bassBoostPercent.toFloat(),
                                onValueChange = { manager.setBassBoost(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("3D Surround (Virtualizer): ${manager.virtualizerPercent}%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = manager.virtualizerPercent.toFloat(),
                                onValueChange = { manager.setVirtualizer(it.toInt()) },
                                valueRange = 0f..100f,
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
                                    onCheckedChange = { manager.toggleRemoveVocals(it) }
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
                                    onCheckedChange = { manager.toggleStopBass(it) }
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

@UnstableApi
@Composable
fun MiniPlayerDock(manager: MusicManager, onClick: () -> Unit) {
    val song = manager.currentSong ?: return
    val accent = manager.accentColor
    val isDark = manager.isDarkMode

    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xE60A0F1D) else Color(0xF2FFFFFF))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -24f) {
                        onClick()
                    }
                }
            }
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
                Text(song.title, color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}", color = accent, fontSize = 11.sp, maxLines = 1)
            }
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(accent).clickable { manager.togglePlayPause() }, contentAlignment = Alignment.Center) {
                Text(if (manager.isPlaying) "❚❚" else "▶", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Full-Page Playing Queue Sheet: Material You Design, Touch-and-Hold Drag Reorder, & Swipe-Down Dismiss
@UnstableApi
@Composable
fun QueueSheet(
    manager: MusicManager,
    palette: MaterialYouPalette? = null,
    onDismiss: () -> Unit
) {
    val isDark = manager.isDarkMode
    val bgTop = palette?.bgTop ?: if (isDark) Color(0xFF0A0F1D) else Color(0xFFF8F9FA)
    val bgBottom = palette?.bgBottom ?: if (isDark) Color(0xFF030712) else Color(0xFFEDEFEF)
    val surfaceColor = palette?.surface ?: if (isDark) Color(0x22FFFFFF) else Color.White
    val textPrimary = palette?.textPrimary ?: if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = palette?.textSecondary ?: Color(0xFF64748B)
    val accent = palette?.primaryAccent ?: manager.accentColor

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .statusBarsPadding()
            // Swipe Down to Dismiss Queue
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 36f) {
                        onDismiss()
                    }
                }
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag Handle Bar on top
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(textSecondary.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassBackButton(isDark = isDark, onClick = onDismiss)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Playing Queue (${manager.playbackQueue.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Touch and hold three lines to drag & reorder",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
                ) {
                    Text("Close", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Queue List with Touch-and-Hold Drag Reordering
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = manager.playbackQueue,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "queue_item_row" }
                ) { index, song ->
                    val isCur = song.id == manager.currentSong?.id
                    val isBeingDragged = draggingIndex == index

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (isBeingDragged) {
                                    translationY = dragDeltaY
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 18.dp.toPx()
                                }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isBeingDragged) accent.copy(alpha = 0.25f)
                                else if (isCur) accent.copy(alpha = 0.16f)
                                else surfaceColor
                            )
                            .border(
                                width = if (isCur || isBeingDragged) 1.5.dp else 1.dp,
                                color = if (isCur || isBeingDragged) accent else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { manager.playSong(song, manager.playbackQueue, manager.currentSectionName) }
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isCur) "▶" else "•", color = accent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}",
                                color = textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Wide, bold, finger-optimized drag handle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(index) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragDeltaY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDeltaY += dragAmount.y
                                            val threshold = 52.dp.toPx()
                                            if (dragDeltaY > threshold && index < manager.playbackQueue.size - 1) {
                                                manager.moveQueueItem(index, index + 1)
                                                draggingIndex = index + 1
                                                dragDeltaY -= threshold
                                            } else if (dragDeltaY < -threshold && index > 0) {
                                                manager.moveQueueItem(index, index - 1)
                                                draggingIndex = index - 1
                                                dragDeltaY += threshold
                                            }
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragDeltaY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragDeltaY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "≡",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isBeingDragged) accent else textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = textPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Sleep Timer Dialog
@UnstableApi
@Composable
fun SleepTimerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(15) }

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
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
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
                            Button(onClick = { manager.endSleepTimer(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(10.dp)) {
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
                            modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.setSleepTimer(mins); onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins min", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.setSleepTimerToEndOfTrack(); onDismiss() },
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
                        Slider(value = customHours.toFloat(), onValueChange = { customHours = it.toInt() }, valueRange = 0f..24f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minutes: $customMinutes", color = Color(0xFF64748B), fontSize = 12.sp)
                        Slider(value = customMinutes.toFloat(), onValueChange = { customMinutes = it.toInt() }, valueRange = 1f..60f, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    }
                    Button(onClick = { manager.setSleepTimer((customHours * 60) + customMinutes); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
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

// Circular Rainbow Color Picker
@Composable
fun CircularColorPickerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    val currentColor = remember(hue, sat, value) { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom Accent Picker", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("✕", fontSize = 18.sp, color = Color(0xFF64748B), modifier = Modifier.clickable { onDismiss() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
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
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { manager.updateAccent(currentColor); onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = currentColor)) {
                    Text("Apply Accent", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Tag Editor Dialog
@Composable
fun TagEditorDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    var editTitle by remember { mutableStateOf(song.title) }
    var editArtist by remember { mutableStateOf(song.artist) }
    var editAlbum by remember { mutableStateOf(song.album) }
    var editDate by remember { mutableStateOf(song.releaseDate) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> if (uri != null) selectedCoverUri = uri }
    val isDark = manager.isDarkMode
    val accent = manager.accentColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Edit Audio Tags", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { photoPickerLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(10.dp)) {
                    Text(if (selectedCoverUri != null) "Artwork Picked ✓" else "Change Artwork", color = Color.White, fontSize = 12.sp)
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
                Button(onClick = { manager.updateSongMetadata(song, editTitle, editArtist, editAlbum, editDate, selectedCoverUri); onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Add to Playlist Dialog
@Composable
fun AddToPlaylistDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
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
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).clickable(enabled = false) {}.padding(22.dp)) {
            Column {
                Text("Add to Playlist", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = manager.customPlaylists,
                        key = { it.id },
                        contentType = { "playlist_picker_row" }
                    ) { pl ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)).clickable { manager.addSongToPlaylist(song.id, pl); onDismiss() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlassmorphicFolderIcon(folderColor = Color(pl.iconColorHex), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(pl.name, color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)), shape = RoundedCornerShape(12.dp)) {
                    Text("Cancel", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Magnetic Speed Dialog
@Composable
fun MagneticSpeedDialog(manager: MusicManager, onDismiss: () -> Unit) {
    var speed by remember { mutableFloatStateOf(manager.playbackSpeed) }
    val isDark = manager.isDarkMode
    val accent = manager.accentColor

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
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playback Speed: ${String.format(Locale.US, "%.2fx", speed)}", color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = speed,
                    onValueChange = { raw ->
                        val snapped = manager.setMagneticSpeed(raw)
                        speed = snapped
                    },
                    valueRange = 0.25f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Lyrics Dialog
@Composable
fun LyricsDialog(song: Song, isDark: Boolean, onDismiss: () -> Unit) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
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
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
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

// Vector Heart Icon: Solid Glowing Red when Favorited
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
