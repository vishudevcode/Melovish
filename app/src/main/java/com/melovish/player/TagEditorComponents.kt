package com.melovish.player

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// =========================================================================
// 1. WHEEL ROLLER CODE-LOCK DATE PICKER (Matches Image 2)
// =========================================================================

@Composable
fun WheelRollerDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = remember {
        Calendar.getInstance().apply {
            if (initialDateMillis > 0L) timeInMillis = initialDateMillis
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-based
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    val daysInMonth by remember {
        derivedStateOf {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            c.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) selectedDay = daysInMonth
    }

    val headerCalendar by remember {
        derivedStateOf {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }
        }
    }

    val headerDayOfWeek = remember(headerCalendar) {
        SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH).format(headerCalendar.time)
    }

    val monthNames = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val yearsList = remember { (1950..2040).toList() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(enabled = false) {}
        ) {
            // Green Header (Matches Image 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = selectedYear.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headerDayOfWeek,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Wheel Roller Area (3 columns: Day, Month, Year)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Centered Selector Highlight Band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x15000000))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Roller Column
                    TumblerWheelColumn(
                        items = (1..daysInMonth).map { it.toString() },
                        selectedIndex = (selectedDay - 1).coerceAtLeast(0),
                        modifier = Modifier.weight(1f),
                        onItemSelected = { index -> selectedDay = index + 1 }
                    )

                    // Month Roller Column
                    TumblerWheelColumn(
                        items = monthNames,
                        selectedIndex = selectedMonth,
                        modifier = Modifier.weight(1.6f),
                        onItemSelected = { index -> selectedMonth = index }
                    )

                    // Year Roller Column
                    TumblerWheelColumn(
                        items = yearsList.map { it.toString() },
                        selectedIndex = yearsList.indexOf(selectedYear).coerceAtLeast(0),
                        modifier = Modifier.weight(1.2f),
                        onItemSelected = { index -> selectedYear = yearsList[index] }
                    )
                }

                // Top & Bottom Fade Gradient
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                )
            }

            // Action Buttons (CANCEL / OK)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CANCEL",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OK",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            onDateSelected(selectedYear, selectedMonth, selectedDay)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TumblerWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val currentCenteredIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > 55) (first + 1).coerceIn(0, items.size - 1) else first.coerceIn(0, items.size - 1)
        }
    }

    LaunchedEffect(currentCenteredIndex) {
        if (currentCenteredIndex in items.indices && currentCenteredIndex != selectedIndex) {
            onItemSelected(currentCenteredIndex)
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex.coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(180.dp),
        contentPadding = PaddingValues(vertical = 68.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = index == currentCenteredIndex
            Text(
                text = item,
                fontSize = if (isSelected) 18.sp else 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF212121) else Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable {
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

// =========================================================================
// 2. MATERIAL DESIGN ANALOG CLOCK TIME PICKER (Matches Images 3 & 4)
// =========================================================================

enum class ClockSelectionMode {
    HOUR, MINUTE
}

@Composable
fun MaterialAnalogClockPickerDialog(
    initialTimeMillis: Long,
    onTimeSelected: (hourOfDay: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = remember {
        Calendar.getInstance().apply {
            if (initialTimeMillis > 0L) timeInMillis = initialTimeMillis
        }
    }

    var isAm by remember { mutableStateOf(initialCal.get(Calendar.AM_PM) == Calendar.AM) }
    var selectedHour12 by remember {
        val h = initialCal.get(Calendar.HOUR)
        mutableIntStateOf(if (h == 0) 12 else h)
    }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var clockMode by remember { mutableStateOf(ClockSelectionMode.HOUR) }

    val view = LocalView.current
    val accent = Color(0xFF6750A4)
    val clockFaceBg = Color(0xFFECE6F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select time",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF49454F),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Digital Blocks Header with AM/PM toggle (Matches Images 3 & 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Digital Box
                Box(
                    modifier = Modifier
                        .size(96.dp, 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (clockMode == ClockSelectionMode.HOUR) accent.copy(alpha = 0.22f) else Color(0xFFF3EDF7))
                        .border(
                            2.dp,
                            if (clockMode == ClockSelectionMode.HOUR) accent else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { clockMode = ClockSelectionMode.HOUR },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", selectedHour12),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (clockMode == ClockSelectionMode.HOUR) accent else Color(0xFF1D1B20)
                    )
                }

                Text(
                    text = ":",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                // Minute Digital Box
                Box(
                    modifier = Modifier
                        .size(96.dp, 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (clockMode == ClockSelectionMode.MINUTE) accent.copy(alpha = 0.22f) else Color(0xFFF3EDF7))
                        .border(
                            2.dp,
                            if (clockMode == ClockSelectionMode.MINUTE) accent else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { clockMode = ClockSelectionMode.MINUTE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", selectedMinute),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (clockMode == ClockSelectionMode.MINUTE) accent else Color(0xFF1D1B20)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // AM / PM Toggle Column
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp, 39.dp)
                            .background(if (isAm) accent.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { isAm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAm) accent else Color(0xFF49454F)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp, 1.dp)
                            .background(Color(0xFFCAC4D0))
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp, 39.dp)
                            .background(if (!isAm) accent.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { isAm = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!isAm) accent else Color(0xFF49454F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Analog Dial Touch Canvas
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(CircleShape)
                    .background(clockFaceBg),
                contentAlignment = Alignment.Center
            ) {
                AnalogClockDial(
                    mode = clockMode,
                    selectedHour12 = selectedHour12,
                    selectedMinute = selectedMinute,
                    accentColor = accent,
                    onHourChanged = { h ->
                        if (selectedHour12 != h) {
                            selectedHour12 = h
                            try { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } catch (_: Exception) {}
                        }
                    },
                    onMinuteChanged = { m ->
                        if (selectedMinute != m) {
                            selectedMinute = m
                            try { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } catch (_: Exception) {}
                        }
                    },
                    onHourConfirmed = {
                        clockMode = ClockSelectionMode.MINUTE
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Cancel / Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val finalHour24 = when {
                            isAm && selectedHour12 == 12 -> 0
                            !isAm && selectedHour12 < 12 -> selectedHour12 + 12
                            else -> selectedHour12
                        }
                        onTimeSelected(finalHour24, selectedMinute)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnalogClockDial(
    mode: ClockSelectionMode,
    selectedHour12: Int,
    selectedMinute: Int,
    accentColor: Color,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onHourConfirmed: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(mode) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (angleDeg < 0f) angleDeg += 360f

                    if (mode == ClockSelectionMode.HOUR) {
                        var h = (angleDeg / 30f).roundToInt()
                        if (h == 0) h = 12
                        if (h > 12) h = 12
                        onHourChanged(h)
                        onHourConfirmed()
                    } else {
                        var m = (angleDeg / 6f).roundToInt()
                        if (m >= 60) m = 0
                        onMinuteChanged(m)
                    }
                }
            }
            .pointerInput(mode) {
                detectDragGestures(
                    onDragEnd = {
                        if (mode == ClockSelectionMode.HOUR) onHourConfirmed()
                    }
                ) { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (angleDeg < 0f) angleDeg += 360f

                    if (mode == ClockSelectionMode.HOUR) {
                        var h = (angleDeg / 30f).roundToInt()
                        if (h == 0) h = 12
                        if (h > 12) h = 12
                        onHourChanged(h)
                    } else {
                        var m = (angleDeg / 6f).roundToInt()
                        if (m >= 60) m = 0
                        onMinuteChanged(m)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f
        val pointerLength = radius * 0.72f

        val activeAngleDeg = if (mode == ClockSelectionMode.HOUR) {
            (selectedHour12 % 12) * 30f
        } else {
            selectedMinute * 6f
        }
        val rad = (activeAngleDeg - 90f) * (PI.toFloat() / 180f)
        val handThumb = Offset(center.x + (pointerLength * cos(rad)), center.y + (pointerLength * sin(rad)))

        // Draw Center Pivot
        drawCircle(color = accentColor, radius = 5.dp.toPx(), center = center)

        // Draw Clock Hand Line
        drawLine(
            color = accentColor,
            start = center,
            end = handThumb,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw Active Selector Circle
        drawCircle(
            color = accentColor,
            radius = 18.dp.toPx(),
            center = handThumb
        )
    }

    // Number overlay layer
    Box(modifier = Modifier.fillMaxSize()) {
        val count = 12
        for (i in 1..count) {
            val angleDeg = i * (360f / count)
            val rad = (angleDeg - 90f) * (PI.toFloat() / 180f)
            val textToDisplay = if (mode == ClockSelectionMode.HOUR) {
                i.toString()
            } else {
                val min = (i * 5) % 60
                String.format("%02d", min)
            }
            val isSelected = if (mode == ClockSelectionMode.HOUR) {
                selectedHour12 == i
            } else {
                selectedMinute == (i * 5) % 60
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp),
                contentAlignment = Alignment.Center
            ) {
                val xOffset = (100 * cos(rad)).dp
                val yOffset = (100 * sin(rad)).dp

                Text(
                    text = textToDisplay,
                    fontSize = if (mode == ClockSelectionMode.HOUR) 15.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF1D1B20),
                    modifier = Modifier.padding(start = xOffset * 2, top = yOffset * 2)
                )
            }
        }
    }
}

// =========================================================================
// 3. BEAUTIFUL TAG EDITOR DIALOG WITH LIVE FILE BADGE & TIME PICKER
// =========================================================================

@UnstableApi
@Composable
fun TagEditorDialog(
    manager: MusicManager,
    song: Song,
    onDismiss: () -> Unit
) {
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val accent = manager.accentColor

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var dateMillis by remember {
        val parsed = song.releaseDate.toLongOrNull() ?: System.currentTimeMillis()
        mutableStateOf(parsed)
    }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    var showWheelDatePicker by remember { mutableStateOf(false) }
    var showAnalogClockPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedCoverUri = uri
    }

    val formattedDateString = remember(dateMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(dateMillis)
    }

    var albumArtBitmap by remember(song.id, selectedCoverUri) {
        mutableStateOf(manager.getCachedAlbumArt(song.id))
    }
    LaunchedEffect(song.id, selectedCoverUri) {
        if (selectedCoverUri == null && albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
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
                .background(cardBg)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audio Tag Editor",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = "Direct storage file tag modification",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 14.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Cover Art Changer Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedCoverUri != null) {
                            AsyncImage(
                                model = selectedCoverUri,
                                contentDescription = "New Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (albumArtBitmap != null) {
                            Image(
                                bitmap = albumArtBitmap!!.asImageBitmap(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🎵", fontSize = 36.sp)
                        }

                        // Frosted Camera Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0x99000000))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📷 Edit", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Format: ${song.audioFormat} • ${formatFileSize(song.size)}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = File(song.path).parentFile?.name ?: "Storage",
                            fontSize = 11.sp,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Editable Fields
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Time Picker Trigger Section
                Text(
                    text = "Release Date & Time",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDateString,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showWheelDatePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("📅 Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { showAnalogClockPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("⏰ Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Save Button
                Button(
                    onClick = {
                        manager.updateSongMetadata(
                            song = song,
                            newTitle = title.trim(),
                            newArtist = artist.trim(),
                            newAlbum = album.trim(),
                            newDate = dateMillis.toString(),
                            customCoverUri = selectedCoverUri
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        text = "Save Changes to File",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showWheelDatePicker) {
        WheelRollerDatePickerDialog(
            initialDateMillis = dateMillis,
            onDateSelected = { y, m, d ->
                val c = Calendar.getInstance().apply {
                    timeInMillis = dateMillis
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                dateMillis = c.timeInMillis
            },
            onDismiss = { showWheelDatePicker = false }
        )
    }

    if (showAnalogClockPicker) {
        MaterialAnalogClockPickerDialog(
            initialTimeMillis = dateMillis,
            onTimeSelected = { h24, min ->
                val c = Calendar.getInstance().apply {
                    timeInMillis = dateMillis
                    set(Calendar.HOUR_OF_DAY, h24)
                    set(Calendar.MINUTE, min)
                }
                dateMillis = c.timeInMillis
            },
            onDismiss = { showAnalogClockPicker = false }
        )
    }
}
