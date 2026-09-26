package com.melovish.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi

@UnstableApi
class MainActivity : ComponentActivity() {

    private lateinit var musicManager: MusicManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            musicManager.scanStorage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicManager = MusicManager(applicationContext)

        checkAndRequestPermissions()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDark = when (musicManager.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> systemDark
            }
            musicManager.isDarkMode = isDark

            MelovishAppRoot(manager = musicManager)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            musicManager.scanStorage()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@UnstableApi
@Composable
fun MelovishAppRoot(manager: MusicManager) {
    val isDark = manager.isDarkMode
    val appBg = if (isDark) Color(0xFF0A0F1D) else Color(0xFFF8FAFC)

    var showSettings by remember { mutableStateOf(false) }
    var showPlayerFullScreen by remember { mutableStateOf(false) }
    var activeFolderView by remember { mutableStateOf<String?>(null) }
    var activePlaylistView by remember { mutableStateOf<Playlist?>(null) }
    var activeArtistView by remember { mutableStateOf<ArtistItem?>(null) }
    var showProfileModal by remember { mutableStateOf(false) }

    // 1-Step Back Navigation Management
    BackHandler(
        enabled = showSettings || showPlayerFullScreen || activeFolderView != null ||
                activePlaylistView != null || activeArtistView != null || showProfileModal
    ) {
        when {
            showProfileModal -> showProfileModal = false
            showSettings -> showSettings = false
            showPlayerFullScreen -> showPlayerFullScreen = false
            activeFolderView != null -> activeFolderView = null
            activePlaylistView != null -> activePlaylistView = null
            activeArtistView != null -> activeArtistView = null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg),
        color = appBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Home Screen
            MainHomeScreen(
                manager = manager,
                onOpenSettings = { showSettings = true },
                onOpenProfile = { showProfileModal = true },
                onOpenFolder = { activeFolderView = it },
                onOpenPlaylist = { activePlaylistView = it },
                onOpenArtist = { activeArtistView = it },
                onOpenPlayer = { showPlayerFullScreen = true }
            )

            // Overlays: Inner Folder View
            activeFolderView?.let { folderName ->
                FolderDetailScreen(
                    folderName = folderName,
                    manager = manager,
                    onBackClick = { activeFolderView = null },
                    onOpenPlayer = { showPlayerFullScreen = true }
                )
            }

            // Overlays: Inner Playlist View
            activePlaylistView?.let { playlist ->
                PlaylistDetailScreen(
                    playlist = playlist,
                    manager = manager,
                    onBackClick = { activePlaylistView = null },
                    onOpenPlayer = { showPlayerFullScreen = true }
                )
            }

            // Overlays: Inner Artist View
            activeArtistView?.let { artist ->
                ArtistDetailScreen(
                    artist = artist,
                    manager = manager,
                    onBackClick = { activeArtistView = null },
                    onOpenPlayer = { showPlayerFullScreen = true }
                )
            }

            // Settings is placed AFTER playlists/folders so it always opens in Foreground
            if (showSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBg)
                ) {
                    SettingsScreen(
                        manager = manager,
                        onBackClick = { showSettings = false },
                        onOpenProfile = { showProfileModal = true },
                        onOpenEqualizer = {}
                    )
                }
            }

            // Full Player Sheet (Slides up and covers foreground)
            AnimatedVisibility(
                visible = showPlayerFullScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullPlayerSheet(
                    manager = manager,
                    onDismiss = { showPlayerFullScreen = false }
                )
            }

            // Profile Modal
            if (showProfileModal) {
                ProfileDialog(
                    manager = manager,
                    onDismiss = { showProfileModal = false }
                )
            }
        }
    }
}
