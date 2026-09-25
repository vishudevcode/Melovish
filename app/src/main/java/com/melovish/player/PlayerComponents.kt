package com.melovish.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GlassBackButton(isDark: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "‹",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            modifier = Modifier.offset(y = (-1).dp)
        )
    }
}

@Composable
fun GlassmorphicFolderIcon(folderColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val tabPath = Path().apply {
            moveTo(w * 0.08f, h * 0.28f)
            lineTo(w * 0.08f, h * 0.18f)
            quadraticBezierTo(w * 0.08f, h * 0.12f, w * 0.16f, h * 0.12f)
            lineTo(w * 0.40f, h * 0.12f)
            quadraticBezierTo(w * 0.46f, h * 0.12f, w * 0.52f, h * 0.20f)
            lineTo(w * 0.58f, h * 0.28f)
            close()
        }
        drawPath(tabPath, folderColor.copy(alpha = 0.85f))

        drawRoundRect(
            color = folderColor.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.06f, h * 0.24f),
            size = Size(w * 0.88f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(folderColor.copy(alpha = 0.95f), folderColor.copy(alpha = 0.70f))
            ),
            topLeft = Offset(w * 0.06f, h * 0.32f),
            size = Size(w * 0.88f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.06f, h * 0.32f),
            size = Size(w * 0.88f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun DefaultProfileAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF0F172A))),
            radius = w / 2f
        )
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.20f, center = Offset(w / 2f, h * 0.38f))
        val bodyPath = Path().apply {
            moveTo(w * 0.22f, h * 0.88f)
            quadraticBezierTo(w * 0.22f, h * 0.62f, w * 0.50f, h * 0.62f)
            quadraticBezierTo(w * 0.78f, h * 0.62f, w * 0.78f, h * 0.88f)
            close()
        }
        drawPath(bodyPath, Color.White.copy(alpha = 0.85f))
    }
}

@Composable
fun LiveAudioWaveEqualizer(isAnimating: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val bar1 by infiniteTransition.animateFloat(4f, 16f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(16f, 6f, infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(8f, 18f, infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "b3")
    val bar4 by infiniteTransition.animateFloat(14f, 5f, infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse), label = "b4")

    Row(
        modifier = Modifier.height(18.dp).width(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(bar1, bar2, bar3, bar4)
        bars.forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(if (isAnimating) heightVal.dp else 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun RotaryAudioKnobControl(
    value: Int,
    range: ClosedRange<Int> = 0..100,
    label: String,
    accentColor: Color,
    isDark: Boolean,
    onValueChange: (Int) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var angle by remember(value) {
        mutableFloatStateOf(((value - range.start).toFloat() / (range.endInclusive - range.start)) * 260f - 130f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y * 1.5f
                        val newAngle = (angle + delta).coerceIn(-130f, 130f)
                        angle = newAngle
                        val fraction = (newAngle + 130f) / 260f
                        val newValue = (range.start + fraction * (range.endInclusive - range.start)).roundToInt()
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.width / 2f - 6.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawArc(
                    color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                val sweep = (angle + 130f).coerceIn(0f, 260f)
                drawArc(
                    brush = Brush.sweepGradient(listOf(accentColor.copy(alpha = 0.6f), accentColor)),
                    startAngle = 140f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                drawCircle(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                    radius = radius - 8.dp.toPx(),
                    center = center
                )

                val rad = Math.toRadians((angle - 90f).toDouble())
                val dotRadius = radius - 16.dp.toPx()
                val dotX = (center.x + dotRadius * cos(rad)).toFloat()
                val dotY = (center.y + dotRadius * sin(rad)).toFloat()
                drawCircle(color = accentColor, radius = 3.5.dp.toPx(), center = Offset(dotX, dotY))
            }
            Text("$value%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
    }
}

@Composable
fun CircularSpeedometerControl(
    currentSpeed: Float,
    accentColor: Color,
    isDark: Boolean,
    onSpeedChange: (Float) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Playback Speed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(String.format(Locale.getDefault(), "%.2fx", currentSpeed), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = currentSpeed,
            onValueChange = onSpeedChange,
            valueRange = 0.25f..3.0f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f).forEach { s ->
                Text(
                    text = "${s}x",
                    fontSize = 10.sp,
                    color = if (currentSpeed == s) accentColor else Color(0xFF64748B),
                    fontWeight = if (currentSpeed == s) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onSpeedChange(s) }
                )
            }
        }
    }
}

@Composable
fun SleepTimerModalSheet(
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

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
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sleep Timer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    if (manager.sleepTimerRemainingSeconds > 0) {
                        Text(
                            "${manager.sleepTimerRemainingSeconds / 60}m ${manager.sleepTimerRemainingSeconds % 60}s left",
                            color = manager.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                manager.setSleepTimer(mins)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("$mins Minutes", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            manager.setSleepTimerToEndOfTrack()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("End of Current Track", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                if (manager.sleepTimerRemainingSeconds > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                manager.endSleepTimer()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❌", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Turn Off Timer", color = Color(0xFFEF4444), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Mini Player Dock
@Composable
fun MiniPlayerDock(manager: MusicManager, onClick: () -> Unit) {
    val song = manager.currentSong ?: return
    val isDark = manager.isDarkMode
    val accent = manager.accentColor
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xFF131B2E) else Color.White)
            .border(1.2.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(
                        bitmap = albumArtBitmap!!.asImageBitmap(),
                        contentDescription = song.title,
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
                    song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (song.artist.isNotBlank()) song.artist else "Unknown Artist",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable { manager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (manager.isPlaying) "⏸" else "▶",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Full Player Sheet with Swipe-Up YouTube Music Queue Activation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val song = manager.currentSong ?: return
    val isDark = manager.isDarkMode
    val accent = manager.accentColor
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) albumArtBitmap = manager.loadAlbumArtAsync(song)
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var isLyricsMode by remember { mutableStateOf(false) }

    val bgGradient = if (manager.isColorfulPlayer && albumArtBitmap != null) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF030712)))
    } else {
        Brush.verticalGradient(
            if (isDark) listOf(Color(0xFF0A0F1D), Color(0xFF030712))
            else listOf(Color(0xFFF8FAFC), Color(0xFFEDF2F7))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < -24f) {
                            showQueueSheet = true
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(isDark = isDark, onClick = onDismiss)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    Text(manager.currentSectionName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { showSleepTimer = true }, contentAlignment = Alignment.Center) {
                        Text("⏱️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { manager.toggleFavorite(song) }, contentAlignment = Alignment.Center) {
                        Text(if (song.isFavorite) "❤️" else "🤍", fontSize = 20.sp)
                    }
                }
            }

            // Big Artwork or Interactive Lyrics View
            if (isLyricsMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .clickable { isLyricsMode = false }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lyrics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "♪ ${song.title} ♪\n\nEnjoy pure offline music on Melovish.\nNo distractions, pure acoustic playback.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Tap to return to artwork", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { isLyricsMode = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtBitmap != null) {
                        Image(
                            bitmap = albumArtBitmap!!.asImageBitmap(),
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("🎵", fontSize = 80.sp)
                    }
                }
            }

            // Song Titles
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    song.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (song.artist.isNotBlank()) song.artist else "Unknown Artist",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            // Concentric Slider & Times
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = manager.currentPosition.toFloat(),
                    onValueChange = { manager.seekTo(it.toLong()) },
                    valueRange = 0f..manager.duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(manager.currentPosition), fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(formatTime(manager.duration), fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            // Media Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔀",
                    fontSize = 22.sp,
                    color = if (manager.isShuffleOn) accent else Color(0xFF64748B),
                    modifier = Modifier.clickable { manager.toggleShuffle() }
                )
                Text(
                    "⏮",
                    fontSize = 34.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.clickable { manager.playPrevious() }
                )
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (manager.isPlaying) "⏸" else "▶", fontSize = 28.sp, color = Color.White)
                }
                Text(
                    "⏭",
                    fontSize = 34.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.clickable { manager.playNext() }
                )
                Text(
                    "🔁",
                    fontSize = 22.sp,
                    color = if (manager.repeatModeState != Player.REPEAT_MODE_OFF) accent else Color(0xFF64748B),
                    modifier = Modifier.clickable { manager.toggleRepeat() }
                )
            }

            // Quick Pill Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x14000000))
                        .clickable { showSpeedDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("⚡ ${String.format(Locale.getDefault(), "%.2fx", manager.playbackSpeed)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x14000000))
                        .clickable { isLyricsMode = !isLyricsMode }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("📜 Lyrics", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLyricsMode) accent else if (isDark) Color.White else Color(0xFF0F172A))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x14000000))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▲ Queue (${manager.playbackQueue.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }
        }

        // Speed Controller Modal Sheet
        if (showSpeedDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSpeedDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularSpeedometerControl(
                            currentSpeed = manager.playbackSpeed,
                            accentColor = accent,
                            isDark = isDark,
                            onSpeedChange = { manager.setMagneticSpeed(it) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { showSpeedDialog = false }, shape = RoundedCornerShape(10.dp)) {
                            Text("Done")
                        }
                    }
                }
            }
        }

        // Sleep Timer Sheet
        if (showSleepTimer) {
            SleepTimerModalSheet(manager = manager, isDark = isDark, onDismiss = { showSleepTimer = false })
        }

        // YouTube Music Dynamic Queue Sheet
        AnimatedVisibility(
            visible = showQueueSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            YouTubeMusicQueueSheet(
                manager = manager,
                isDark = isDark,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

// YouTube Music-style Queue with Drag Handle and Dynamic Theme
@Composable
fun YouTubeMusicQueueSheet(
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val accent = manager.accentColor
    val sheetBg = if (isDark) Color(0xF20F172A) else Color(0xF2F8FAFC)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sheetBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF64748B).copy(alpha = 0.6f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Playing Queue", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    Text(manager.currentSectionName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Close", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(manager.playbackQueue, key = { _, s -> s.id }) { index, song ->
                    val isPlayingThis = manager.currentSong?.id == song.id
                    val isDragging = draggedIndex == index

                    var itemArt by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }
                    LaunchedEffect(song.id) {
                        if (itemArt == null) itemArt = manager.loadAlbumArtAsync(song)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 2f else 1f)
                            .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDragging) accent.copy(alpha = 0.25f)
                                else if (isPlayingThis) accent.copy(alpha = 0.12f)
                                else if (isDark) Color(0x14FFFFFF)
                                else Color.White
                            )
                            .border(
                                1.dp,
                                if (isPlayingThis) accent.copy(alpha = 0.5f)
                                else if (isDark) Color(0x10FFFFFF)
                                else Color(0xFFECEFF3),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                manager.playSong(song, manager.playbackQueue, manager.currentSectionName)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (itemArt != null) {
                                Image(bitmap = itemArt!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text("🎵", fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                color = if (isPlayingThis) accent else textColor,
                                fontSize = 14.sp,
                                fontWeight = if (isPlayingThis) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Unknown"}",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val threshold = 70f
                                            val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            if (dragOffsetY > threshold && currentIdx < manager.playbackQueue.size - 1) {
                                                manager.moveQueueItem(currentIdx, currentIdx + 1)
                                                draggedIndex = currentIdx + 1
                                                dragOffsetY = 0f
                                            } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                manager.moveQueueItem(currentIdx, currentIdx - 1)
                                                draggedIndex = currentIdx - 1
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("≡", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isDragging) accent else Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Equalizer Sheet with Rotary Controls & Band Sliders
@Composable
fun EqualizerSheet(manager: MusicManager, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    var selectedPresetTab by remember { mutableStateOf(manager.selectedEqPreset) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equalizer & Sound Effects", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Done") }
                }
                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(manager.eqPresetNames) { preset ->
                        val isSel = selectedPresetTab == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accent else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    selectedPresetTab = preset
                                    manager.applyEqPreset(preset)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(preset, color = if (isSel) Color.White else textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until manager.eqBandsCount) {
                        val level = manager.eqBandLevels[i] ?: 0
                        val freq = manager.eqCenterFreqs[i] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${freq}Hz", fontSize = 10.sp, color = Color(0xFF64748B))
                            Slider(
                                value = level.toFloat(),
                                onValueChange = {
                                    manager.setEqBandLevel(i, it.toInt())
                                    selectedPresetTab = "Custom"
                                },
                                valueRange = manager.eqMinLevel.toFloat()..manager.eqMaxLevel.toFloat(),
                                modifier = Modifier.height(140.dp).width(36.dp),
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                            Text("${level / 100}dB", fontSize = 9.sp, color = textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RotaryAudioKnobControl(
                        value = manager.bassBoostPercent,
                        label = "Bass Boost",
                        accentColor = accent,
                        isDark = isDark,
                        onValueChange = { manager.setBassBoost(it) }
                    )
                    RotaryAudioKnobControl(
                        value = manager.virtualizerPercent,
                        label = "3D Surround",
                        accentColor = accent,
                        isDark = isDark,
                        onValueChange = { manager.setVirtualizer(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sub-Bass Cutoff", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Remove muddy low-end frequencies", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = manager.isStopBass,
                        onCheckedChange = { manager.toggleStopBass(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vocal Enhancer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("Isolate & enhance vocal mid frequencies", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = manager.isRemoveVocals,
                        onCheckedChange = { manager.toggleRemoveVocals(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                    )
                }
            }
        }
    }
}

// Dialog: Circular Color Picker with HSV Palette Swatches
@Composable
fun CircularColorPickerDialog(manager: MusicManager, onDismiss: () -> Unit) {
    var selectedColor by remember { mutableStateOf(manager.accentColor) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (manager.isDarkMode) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Theme Color", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (manager.isDarkMode) Color.White else Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(3.dp, Color.White, CircleShape)
                        .shadow(8.dp, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val presetColors = listOf(
                    Color(0xFF00B4D8), Color(0xFF2EC4B6), Color(0xFF39FF14),
                    Color(0xFFFF2A85), Color(0xFFFF3B30), Color(0xFFF59E0B),
                    Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFFF6B35), Color(0xFFFFCC00)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { col ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(2.dp, if (selectedColor == col) Color.White else Color.Transparent, CircleShape)
                                .clickable { selectedColor = col }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        manager.updateAccent(selectedColor)
                        manager.addColorPreset(selectedColor)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Color", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Dialog: Folder Color Selector
@Composable
fun FolderColorDialog(
    folderName: String,
    currentColor: Color,
    isDark: Boolean,
    onColorSelected: (Color) -> Unit,
    onOpenRainbowPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val presets = listOf(Color(0xFFF59E0B), Color(0xFF00B4D8), Color(0xFF10B981), Color(0xFFFF2A85), Color(0xFFEF4444), Color(0xFF8B5CF6))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
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
            Column {
                Text("Customize '$folderName' Color", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    presets.forEach { color ->
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
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onOpenRainbowPicker()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pick Custom Color 🌈", color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Dialog: Add to Playlist
@Composable
fun AddToPlaylistDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
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
            Column {
                Text("Add to Playlist", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))
                if (manager.customPlaylists.isEmpty()) {
                    Text("No playlists found. Create one from the Home screen.", color = Color(0xFF64748B), fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(manager.customPlaylists, key = { it.id }) { pl ->
                            val isAdded = song.id in pl.songIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        if (isAdded) manager.removeSongFromPlaylist(song.id, pl)
                                        else manager.addSongToPlaylist(song.id, pl)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pl.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (isAdded) "✓ Added" else "+ Add", color = if (isAdded) manager.accentColor else Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Done")
                }
            }
        }
    }
}

// Dialog: Tag Editor with Image Selection & Metadata Writing
@Composable
fun TagEditorDialog(manager: MusicManager, song: Song, onDismiss: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var editTitle by remember { mutableStateOf(song.title) }
    var editArtist by remember { mutableStateOf(song.artist) }
    var editAlbum by remember { mutableStateOf(song.album) }
    var editDate by remember { mutableStateOf(song.releaseDate) }
    var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pickedCoverUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
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
            Column {
                Text("Edit Tags & Metadata", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📷\nArt", fontSize = 12.sp, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Album Cover Art", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (pickedCoverUri != null) "New Image Picked!" else "Tap icon to change cover", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editDate,
                    onValueChange = { editDate = it },
                    label = { Text("Year / Date Added") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        manager.updateSongMetadata(song, editTitle, editArtist, editAlbum, editDate, pickedCoverUri)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
