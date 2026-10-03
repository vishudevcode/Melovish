package com.melovish.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.io.File

@UnstableApi
@Composable
fun ProfileScreen(manager: MusicManager, onBackClick: () -> Unit) {
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val accent = manager.accentColor

    var isEditMode by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(manager.profileName) }
    var editEmail by remember { mutableStateOf(manager.profileEmail) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showBigPicturePreview by remember { mutableStateOf(false) }
    var viewingAllType by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = viewingAllType != null || showBigPicturePreview) {
        if (showBigPicturePreview) {
            showBigPicturePreview = false
        } else {
            viewingAllType = null
        }
    }

    val photoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedImageUri = uri
        }
    }

    val savedAvatarPath = manager.profileImagePath
    val avatarModel: Any? = pickedImageUri ?: if (!savedAvatarPath.isNullOrBlank() && File(savedAvatarPath).exists()) {
        File(savedAvatarPath)
    } else {
        null
    }

    val mostPlayed: ImmutableList<Song> = remember(manager.allSongs.size, manager.historySongs.size) {
        manager.getMostPlayedSongs().toImmutableList()
    }
    val historyList: ImmutableList<Song> = remember(manager.historySongs.size, manager.historySongs.toList()) {
        manager.historySongs.toImmutableList()
    }

    val isAnyProfileDialogOpen = showBigPicturePreview

    val animatedProfileBlur by animateDpAsState(
        targetValue = if (isAnyProfileDialogOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "profileBlurAnim"
    )

    if (viewingAllType != null) {
        val isMostPlayedView = viewingAllType == "most_played"
        val fullList = if (isMostPlayedView) mostPlayed else historyList

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassBackButton(
                        isDark = isDark,
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            viewingAllType = null
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isMostPlayedView) "Most Played" else "History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text("${fullList.size} tracks available", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                if (!isMostPlayedView && fullList.isNotEmpty()) {
                    Button(
                        onClick = {
                            manager.triggerHapticFeedback(true)
                            manager.clearHistory()
                            viewingAllType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (fullList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tracks found.", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = fullList,
                        key = { _, s -> s.id },
                        contentType = { _, _ -> "profile_expanded_song_row" }
                    ) { index, song ->
                        ProfileSongRow(
                            song = song,
                            manager = manager,
                            isDark = isDark,
                            accent = accent,
                            showPlayCount = isMostPlayedView,
                            rank = if (isMostPlayedView) index + 1 else null,
                            onClick = {
                                manager.playSong(song, fullList, if (isMostPlayedView) "Most Played" else "History")
                            }
                        )
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(animatedProfileBlur)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "profile_top_bar", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(
                        isDark = isDark,
                        onClick = {
                            manager.triggerHapticFeedback(false)
                            onBackClick()
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "My Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                }
            }

            item(key = "profile_info_card", contentType = "user_info_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile Info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Button(
                            onClick = {
                                manager.triggerHapticFeedback(false)
                                if (isEditMode) {
                                    manager.savePermanentProfile(editName, editEmail, pickedImageUri)
                                    isEditMode = false
                                } else {
                                    isEditMode = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEditMode) "Save" else "Edit", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Option A Dynamic Frosted Glass Avatar Container
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(6.dp, CircleShape, spotColor = accent)
                                .clip(CircleShape)
                                .background(manager.getCurrentSurfaceColor())
                                .border(2.dp, accent, CircleShape)
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    if (isEditMode) photoPicker.launch("image/*") else showBigPicturePreview = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarModel != null) {
                                AsyncImage(
                                    model = avatarModel,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                DefaultProfileAvatar(
                                    modifier = Modifier.fillMaxSize(),
                                    backgroundColor = Color.Transparent
                                )
                            }

                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        if (isEditMode) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Your Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        focusedContainerColor = cardBg,
                                        unfocusedContainerColor = cardBg,
                                        focusedBorderColor = accent,
                                        unfocusedBorderColor = manager.getCurrentBorderColor(),
                                        cursorColor = accent,
                                        focusedLabelColor = accent,
                                        unfocusedLabelColor = Color(0xFF64748B)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editEmail,
                                    onValueChange = { editEmail = it },
                                    label = { Text("Email (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        focusedContainerColor = cardBg,
                                        unfocusedContainerColor = cardBg,
                                        focusedBorderColor = accent,
                                        unfocusedBorderColor = manager.getCurrentBorderColor(),
                                        cursorColor = accent,
                                        focusedLabelColor = accent,
                                        unfocusedLabelColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        } else {
                            Column {
                                Text(manager.profileName, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (manager.profileEmail.isNotBlank()) {
                                    Text(manager.profileEmail, color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "profile_most_played", contentType = "analytics_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Most Played", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Your all-time favorite songs.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    viewingAllType = "most_played"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View all ›", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (mostPlayed.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                            Text("No songs played yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            mostPlayed.take(5).forEach { song ->
                                ProfileSongRow(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    accent = accent,
                                    showPlayCount = true,
                                    onClick = { manager.playSong(song, mostPlayed, "Most Played") }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "profile_history", contentType = "analytics_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.2.dp, glassBorderBrush, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("History", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Your recently played songs.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    manager.triggerHapticFeedback(false)
                                    viewingAllType = "history"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View all ›", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (historyList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                            Text("No playback history yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            historyList.take(5).forEach { song ->
                                ProfileSongRow(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    accent = accent,
                                    showPlayCount = false,
                                    onClick = { manager.playSong(song, historyList, "History") }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showBigPicturePreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0x66000000) else Color(0x40000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showBigPicturePreview = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(manager.getCurrentDialogColor())
                        .border(1.5.dp, manager.getGlassBorderBrush(), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarModel != null) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "Big Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DefaultProfileAvatar(
                            modifier = Modifier.size(240.dp),
                            backgroundColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun ProfileSongRow(
    song: Song,
    manager: MusicManager,
    isDark: Boolean,
    accent: Color,
    showPlayCount: Boolean,
    rank: Int? = null,
    onClick: () -> Unit
) {
    val isPlayingThis = manager.currentSong?.id == song.id
    val textColor = if (isPlayingThis) accent else manager.getCurrentTextColor()
    val cardBg = if (isPlayingThis) accent.copy(alpha = 0.12f) else manager.getCurrentSurfaceColor()
    val glassBorderBrush = if (isPlayingThis) Brush.linearGradient(listOf(accent, accent)) else manager.getGlassBorderBrush()
    var albumArtBitmap by remember(song.id) { mutableStateOf(manager.getCachedAlbumArt(song.id)) }

    LaunchedEffect(song.id) {
        if (albumArtBitmap == null) {
            albumArtBitmap = manager.loadAlbumArtAsync(song)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.2.dp, glassBorderBrush, RoundedCornerShape(14.dp))
            .clickable {
                manager.triggerHapticFeedback(false)
                onClick()
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                text = "#$rank",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(30.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B)),
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
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isPlayingThis) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (song.artist.isNotBlank()) song.artist else "Unknown Artist",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isPlayingThis) {
            LiveAudioWaveEqualizer(isAnimating = manager.isPlaying, accentColor = accent)
        } else if (showPlayCount) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${song.playCount}",
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Text(
                text = formatTime(song.duration),
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
