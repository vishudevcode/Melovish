package com.melovish.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Locale
import kotlin.math.pow

enum class GridViewMode {
    LIST, GRID_2, GRID_3, GRID_4, HERO_GRID
}

enum class ArtistSortOrder {
    A_TO_Z, Z_TO_A, MOST_SONGS, MOST_PLAYED
}

@UnstableApi
class MusicManager(private val context: Context) {

    companion object {
        @Volatile
        var activeInstance: MusicManager? = null
            private set
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler)
    val prefs: SharedPreferences = context.getSharedPreferences("melovish_prefs_v13", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val channelMixingAudioProcessor = ChannelMixingAudioProcessor()

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(channelMixingAudioProcessor))
                .build()
        }
    }

    var isAlwaysPlay by mutableStateOf(prefs.getBoolean("always_play_enabled", false))

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            !isAlwaysPlay
        )
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }

    var mediaSession: MediaSession? = null
        private set

    private var boundAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var repeatModeState by mutableIntStateOf(Player.REPEAT_MODE_ALL)
    var isShuffleOn by mutableStateOf(false)
    var currentSectionName by mutableStateOf("All Songs")
    var currentVolume by mutableFloatStateOf(0.7f)

    var sleepTimerRemainingSeconds by mutableIntStateOf(0)
    private var sleepTimerJob: Job? = null
    private var fadeJob: Job? = null
    private var isFadingOutForCrossfade = false

    var isScanningStorage by mutableStateOf(false)

    val allSongs = mutableStateListOf<Song>()
    val rawStorageSongs = mutableStateListOf<Song>()
    val playbackQueue = mutableStateListOf<Song>()
    val historySongs = mutableStateListOf<Song>()
    val customPlaylists = mutableStateListOf<Playlist>()
    val hiddenFolders = mutableStateListOf<String>()
    val hiddenAudioIds = mutableStateListOf<Long>()
    val folderColors = mutableStateMapOf<String, Long>()
    val parsedArtistsList = mutableStateListOf<ArtistItem>()

    // ==========================================
    // SEPARATE VIEW MODES WITH PER-SCREEN MEMORY
    // ==========================================
    private fun parseGridViewMode(saved: String?): GridViewMode {
        return try {
            if (saved == null || saved == "DETAILED_LIST") GridViewMode.LIST
            else GridViewMode.valueOf(saved)
        } catch (_: Exception) {
            GridViewMode.LIST
        }
    }

    // 1. Home Screen (All Songs) View Mode
    var homeViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_home", GridViewMode.LIST.name)))

    // 2. Library (Folders Root) View Mode
    var libraryFoldersViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_library_folders", GridViewMode.LIST.name)))

    // 3. Inside Any Folder View Mode
    var folderInnerViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_folder_inner", GridViewMode.LIST.name)))

    // 4. Artists Section Root View Mode
    var artistsViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_artists", GridViewMode.LIST.name)))

    // 5. Inside Artist Detail View Mode
    var artistInnerViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_artist_inner", GridViewMode.LIST.name)))

    // 6. Inside Any Playlist View Mode
    var playlistInnerViewMode by mutableStateOf(parseGridViewMode(prefs.getString("pref_view_playlist_inner", GridViewMode.LIST.name)))

    // ==========================================
    // SEPARATE SORT ORDERS WITH PER-SCREEN MEMORY
    // ==========================================
    // 1. Home Song Sort
    var currentSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("saved_song_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )

    // 2. Library Folders Sort
    var currentFolderSortOrder by mutableStateOf(
        try {
            FolderSortOrder.valueOf(prefs.getString("saved_folder_sort", FolderSortOrder.A_TO_Z.name) ?: FolderSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            FolderSortOrder.A_TO_Z
        }
    )

    // 3. Inside Folder Sort
    var folderInnerSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("folder_inner_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )

    // 4. Artists Root Sort
    var artistsSortOrder by mutableStateOf(
        try {
            ArtistSortOrder.valueOf(prefs.getString("pref_sort_artists", ArtistSortOrder.A_TO_Z.name) ?: ArtistSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            ArtistSortOrder.A_TO_Z
        }
    )

    // 5. Inside Artist Detail Sort
    var artistInnerSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("pref_sort_artist_inner", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )

    // 6. Inside Playlist Sort
    var playlistInnerSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("playlist_inner_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )

    var themeMode by mutableStateOf(prefs.getString("theme_mode", "System") ?: "System")
    var isDarkMode by mutableStateOf(false)
    var accentColor by mutableStateOf(Color(prefs.getInt("accent_color", 0xFF00B4D8.toInt())))
    val userSavedColorPresets = mutableStateListOf<Color>()

    var pagerTransitionEffect by mutableStateOf(
        try {
            PagerTransitionEffect.valueOf(
                prefs.getString("pager_transition_effect", PagerTransitionEffect.CASCADE.name)
                    ?: PagerTransitionEffect.CASCADE.name
            )
        } catch (_: Exception) {
            PagerTransitionEffect.CASCADE
        }
    )

    var isColorfulPlayer by mutableStateOf(prefs.getBoolean("colorful_player", true))
    var isResumeFirstOnly by mutableStateOf(prefs.getBoolean("resume_first", false))
    var isFadeOnStart by mutableStateOf(prefs.getBoolean("fade_start", false))
    var isGaplessEnabled by mutableStateOf(prefs.getBoolean("gapless", true))
    var isCrossfadeEnabled by mutableStateOf(prefs.getBoolean("crossfade_enabled", false))
    var crossfadeDuration by mutableFloatStateOf(prefs.getFloat("crossfade_duration", 2.0f))

    var isLosslessEnabled by mutableStateOf(prefs.getBoolean("lossless", true))
    var isVolumeNormalized by mutableStateOf(prefs.getBoolean("vol_norm", false))
    var volumeBoostLevel by mutableFloatStateOf(prefs.getFloat("vol_boost", 100f))
    var isMonoAudio by mutableStateOf(prefs.getBoolean("mono", false))
    var selectedAudioOutput by mutableStateOf(prefs.getString("audio_output", "Phone") ?: "Phone")

    var isEqEnabled by mutableStateOf(prefs.getBoolean("eq_enabled", true))
    var eqBandsCount by mutableIntStateOf(5)
    val eqBandLevels = mutableStateMapOf<Int, Int>()
    val eqCenterFreqs = mutableStateMapOf<Int, Int>()
    var eqMinLevel by mutableIntStateOf(-1500)
    var eqMaxLevel by mutableIntStateOf(1500)
    val eqPresetNames = mutableStateListOf<String>()
    var selectedEqPreset by mutableStateOf(prefs.getString("selected_eq_preset", "Original") ?: "Original")

    val standardPresetCurves = mapOf(
        "Original" to listOf(0, 0, 0, 0, 0),
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Rock" to listOf(500, 300, -100, 200, 500),
        "Pop" to listOf(-100, 200, 500, 200, -100),
        "Jazz" to listOf(400, 200, -200, 200, 400),
        "Classical" to listOf(500, 300, -100, 200, 400),
        "Hip Hop" to listOf(600, 400, 0, 200, 400),
        "Dance" to listOf(500, 100, 200, 400, 200),
        "Bass Boost" to listOf(800, 500, 200, 0, 0),
        "Vocal Boost" to listOf(-300, 100, 600, 300, -200)
    )

    var bassBoostPercent by mutableIntStateOf(prefs.getInt("bass_boost", 50))
    var virtualizerPercent by mutableIntStateOf(prefs.getInt("virtualizer", 30))
    var isStopBass by mutableStateOf(prefs.getBoolean("stop_bass", false))
    var isRemoveVocals by mutableStateOf(prefs.getBoolean("remove_vocals", false))

    var profileName by mutableStateOf(prefs.getString("prof_name", "Bharat Bhushan") ?: "Bharat Bhushan")
    var profileEmail by mutableStateOf(prefs.getString("prof_email", "") ?: "")
    var profileImagePath by mutableStateOf(prefs.getString("prof_image_path", null))

    private val maxCacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(1024 * 16)
    private val memoryCache = object : LruCache<Long, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: Long, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            syncDeviceVolume()
            applyHardwareAudioRouting(selectedAudioOutput)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            syncDeviceVolume()
            applyHardwareAudioRouting(selectedAudioOutput)
        }
    }

    init {
        activeInstance = this
        initDefaultEqualizerState()
        initMediaSession()
        createNotificationChannel()
        setupBroadcastReceiver()
        loadPreferences()
        loadColorPresets()
        loadInstantCache()
        setupPlayerListener()
        startPositionTracker()
        syncDeviceVolume()
        applyChannelMixing()
        registerAudioDeviceCallback()
        applyHardwareAudioRouting(selectedAudioOutput)
    }

    private fun initDefaultEqualizerState() {
        eqPresetNames.clear()
        eqPresetNames.addAll(standardPresetCurves.keys)

        eqBandsCount = 5
        eqCenterFreqs[0] = 60
        eqCenterFreqs[1] = 230
        eqCenterFreqs[2] = 910
        eqCenterFreqs[3] = 3600
        eqCenterFreqs[4] = 14000

        for (i in 0 until 5) {
            eqBandLevels[i] = prefs.getInt("eq_band_$i", 0)
        }
    }

    private fun initMediaSession() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(context, player)
                .setSessionActivity(pendingIntent)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MediaPlaybackService.NOTIF_CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls and lockscreen carousel"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerAudioDeviceCallback() {
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBroadcastReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    "ACTION_PLAY", "ACTION_PAUSE" -> togglePlayPause()
                    "ACTION_NEXT" -> playNext()
                    "ACTION_PREV" -> playPrevious()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("ACTION_PLAY")
            addAction("ACTION_PAUSE")
            addAction("ACTION_NEXT")
            addAction("ACTION_PREV")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    bindHardwareAudioEffects(audioSessionId)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updateNotification()
                if (playing && isFadeOnStart && !isCrossfadeEnabled) {
                    triggerFadeIn(1000L)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                        bindHardwareAudioEffects(sessionId)
                    }
                    updateNotification()
                    applyHardwareAudioRouting(selectedAudioOutput)
                } else if (state == Player.STATE_ENDED) {
                    if (isGaplessEnabled) {
                        playNext()
                    } else {
                        managerScope.launch {
                            player.pause()
                            delay(500)
                            playNext()
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentUri = mediaItem?.localConfiguration?.uri
                val song = allSongs.find { it.uri == currentUri }
                if (song != null) {
                    currentSong = song
                    recordSongPlayed(song)
                    updateNotification()
                    applyVolumeNormalization()
                    applyHardwareAudioRouting(selectedAudioOutput)

                    isFadingOutForCrossfade = false
                    if (isCrossfadeEnabled) {
                        val fadeMs = (crossfadeDuration * 1000).toLong() / 2
                        triggerFadeIn(fadeMs.coerceAtLeast(400L))
                    } else if (isFadeOnStart) {
                        triggerFadeIn(1200L)
                    } else {
                        player.volume = 1.0f
                    }

                    if (!isGaplessEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        managerScope.launch {
                            player.pause()
                            delay(500)
                            player.play()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                managerScope.launch(Dispatchers.Main.immediate) {
                    Toast.makeText(context, "Track unplayable, skipping...", Toast.LENGTH_SHORT).show()
                    playNext()
                }
            }
        })
    }

    fun bindHardwareAudioEffects(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
        if (boundAudioSessionId == sessionId && equalizer != null) {
            updateAudioEffectsState()
            return
        }

        try {
            releaseAudioEffects()
            boundAudioSessionId = sessionId

            equalizer = Equalizer(0, sessionId).apply {
                enabled = isEqEnabled
                val range = bandLevelRange
                if (range != null && range.size >= 2) {
                    eqMinLevel = range[0].toInt()
                    eqMaxLevel = range[1].toInt()
                }
                eqBandsCount = numberOfBands.toInt().coerceAtLeast(1)
                for (i in 0 until eqBandsCount) {
                    val freq = getCenterFreq(i.toShort()) / 1000
                    if (freq > 0) eqCenterFreqs[i] = freq
                    val level = eqBandLevels[i] ?: 0
                    setBandLevel(i.toShort(), level.coerceIn(eqMinLevel, eqMaxLevel).toShort())
                }
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = !isStopBass && isEqEnabled
                setStrength((bassBoostPercent * 10).toShort().coerceIn(0, 1000))
            }

            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = isEqEnabled
                setStrength((virtualizerPercent * 10).toShort().coerceIn(0, 1000))
            }

            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                val boostGainMb = (((volumeBoostLevel - 100f) / 100f) * 2000f).toInt().coerceIn(0, 3000)
                setTargetGain(boostGainMb)
                enabled = volumeBoostLevel > 100f || isVolumeNormalized
            }

            applyVolumeNormalization()
            applyChannelMixing()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateAudioEffectsState() {
        try {
            equalizer?.enabled = isEqEnabled
            for (i in 0 until eqBandsCount) {
                val lvl = eqBandLevels[i] ?: 0
                equalizer?.setBandLevel(i.toShort(), lvl.coerceIn(eqMinLevel, eqMaxLevel).toShort())
            }

            bassBoost?.enabled = !isStopBass && isEqEnabled
            bassBoost?.setStrength((bassBoostPercent * 10).toShort().coerceIn(0, 1000))

            virtualizer?.enabled = isEqEnabled
            virtualizer?.setStrength((virtualizerPercent * 10).toShort().coerceIn(0, 1000))

            val boostGainMb = (((volumeBoostLevel - 100f) / 100f) * 2000f).toInt().coerceIn(0, 3000)
            loudnessEnhancer?.setTargetGain(boostGainMb)
            loudnessEnhancer?.enabled = volumeBoostLevel > 100f || isVolumeNormalized

            applyVolumeNormalization()
            applyChannelMixing()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseAudioEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }

    fun attachAudioEffects() {
        val sessionId = if (boundAudioSessionId != C.AUDIO_SESSION_ID_UNSET && boundAudioSessionId != 0) {
            boundAudioSessionId
        } else {
            player.audioSessionId
        }
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
            bindHardwareAudioEffects(sessionId)
        }
    }

    fun setEqBandLevel(band: Int, level: Int) {
        eqBandLevels[band] = level
        selectedEqPreset = "Custom"
        if (!isEqEnabled) {
            isEqEnabled = true
            prefs.edit().putBoolean("eq_enabled", true).apply()
        }
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putInt("eq_band_$band", level).putString("selected_eq_preset", "Custom").apply()
        }
        try {
            equalizer?.enabled = true
            equalizer?.setBandLevel(band.toShort(), level.coerceIn(eqMinLevel, eqMaxLevel).toShort())
        } catch (_: Exception) {}
    }

    fun applyEqPreset(presetName: String) {
        selectedEqPreset = presetName
        if (!isEqEnabled) {
            isEqEnabled = true
            prefs.edit().putBoolean("eq_enabled", true).apply()
        }
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("selected_eq_preset", presetName).apply()
        }

        val curve = standardPresetCurves[presetName] ?: standardPresetCurves["Flat"] ?: listOf(0, 0, 0, 0, 0)
        for (i in 0 until eqBandsCount) {
            val lvl = if (i < curve.size) curve[i] else 0
            eqBandLevels[i] = lvl
            prefs.edit().putInt("eq_band_$i", lvl).apply()
            try {
                equalizer?.enabled = true
                equalizer?.setBandLevel(i.toShort(), lvl.coerceIn(eqMinLevel, eqMaxLevel).toShort())
            } catch (_: Exception) {}
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        isEqEnabled = enabled
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("eq_enabled", enabled).apply()
        }
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = !isStopBass && enabled
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setBassBoost(percent: Int) {
        bassBoostPercent = percent
        if (!isEqEnabled) {
            isEqEnabled = true
            prefs.edit().putBoolean("eq_enabled", true).apply()
        }
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putInt("bass_boost", percent).apply()
        }
        try {
            bassBoost?.enabled = !isStopBass
            bassBoost?.setStrength((percent * 10).toShort().coerceIn(0, 1000))
        } catch (_: Exception) {}
    }

    fun setVirtualizer(percent: Int) {
        virtualizerPercent = percent
        if (!isEqEnabled) {
            isEqEnabled = true
            prefs.edit().putBoolean("eq_enabled", true).apply()
        }
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putInt("virtualizer", percent).apply()
        }
        try {
            virtualizer?.enabled = true
            virtualizer?.setStrength((percent * 10).toShort().coerceIn(0, 1000))
        } catch (_: Exception) {}
    }

    fun toggleStopBass(stop: Boolean) {
        isStopBass = stop
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("stop_bass", stop).apply()
        }
        try {
            bassBoost?.enabled = !stop && isEqEnabled
            if (stop && eqBandsCount > 0) {
                equalizer?.setBandLevel(0.toShort(), eqMinLevel.toShort())
                eqBandLevels[0] = eqMinLevel
            } else if (!stop && eqBandsCount > 0) {
                val saved = prefs.getInt("eq_band_0", 0)
                equalizer?.setBandLevel(0.toShort(), saved.toShort())
                eqBandLevels[0] = saved
            }
        } catch (_: Exception) {}
    }

    fun toggleRemoveVocals(remove: Boolean) {
        isRemoveVocals = remove
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("remove_vocals", remove).apply()
        }
        applyChannelMixing()

        if (remove && isEqEnabled && eqBandsCount >= 3) {
            val midBand = eqBandsCount / 2
            try {
                equalizer?.setBandLevel(midBand.toShort(), (eqMinLevel * 0.7f).toInt().toShort())
            } catch (_: Exception) {}
        } else if (!remove && isEqEnabled && eqBandsCount >= 3) {
            val midBand = eqBandsCount / 2
            val saved = eqBandLevels[midBand] ?: 0
            try {
                equalizer?.setBandLevel(midBand.toShort(), saved.toShort())
            } catch (_: Exception) {}
        }
    }

    fun setVolumeBoost(level: Float) {
        volumeBoostLevel = level
        val sessionId = if (boundAudioSessionId != C.AUDIO_SESSION_ID_UNSET && boundAudioSessionId != 0) {
            boundAudioSessionId
        } else {
            player.audioSessionId
        }

        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(sessionId)
                }
                val boostGainMb = (((level - 100f) / 100f) * 2000f).toInt().coerceIn(0, 3000)
                loudnessEnhancer?.setTargetGain(boostGainMb)
                loudnessEnhancer?.enabled = level > 100f || isVolumeNormalized
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putFloat("vol_boost", level).apply()
        }
    }

    fun toggleVolumeNormalization(enabled: Boolean) {
        isVolumeNormalized = enabled
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("vol_norm", enabled).apply()
        }
        applyVolumeNormalization()
    }

    private fun applyVolumeNormalization() {
        val song = currentSong
        if (!isVolumeNormalized || song == null) {
            if (volumeBoostLevel <= 100f) {
                try { loudnessEnhancer?.enabled = false } catch (_: Exception) {}
            }
            player.volume = 1.0f
            return
        }

        val targetDb = if (song.replayGainTrackDb != 0.0f) song.replayGainTrackDb else -3.5f
        if (targetDb < 0) {
            val linear = 10f.pow(targetDb / 20f).coerceIn(0.2f, 1.0f)
            player.volume = linear
            try {
                if (volumeBoostLevel <= 100f) loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {}
        } else {
            player.volume = 1.0f
            val baseGain = (targetDb * 100).toInt().coerceIn(0, 800)
            val boostGain = (((volumeBoostLevel - 100f) / 100f) * 2000f).toInt().coerceIn(0, 3000)
            try {
                loudnessEnhancer?.setTargetGain(baseGain + boostGain)
                loudnessEnhancer?.enabled = true
            } catch (_: Exception) {}
        }
    }

    fun toggleMonoAudio(enabled: Boolean) {
        isMonoAudio = enabled
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("mono", enabled).apply()
        }
        applyChannelMixing()
    }

    private fun applyChannelMixing() {
        try {
            val matrix = when {
                isRemoveVocals -> {
                    ChannelMixingMatrix(2, 2, floatArrayOf(0.707f, -0.707f, -0.707f, 0.707f))
                }
                isMonoAudio -> {
                    ChannelMixingMatrix(2, 2, floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f))
                }
                else -> {
                    ChannelMixingMatrix(2, 2, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f))
                }
            }
            channelMixingAudioProcessor.putChannelMixingMatrix(matrix)
        } catch (_: Exception) {}
    }

    fun setAudioOutputRouting(output: String) {
        selectedAudioOutput = output
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("audio_output", output).apply()
        }
        applyHardwareAudioRouting(output)
    }

    fun applyHardwareAudioRouting(output: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val targetDevice: AudioDeviceInfo? = when (output) {
                    "Phone" -> {
                        devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    }
                    "Speaker" -> {
                        devices.find {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                            it.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                            it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                        } ?: devices.find { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
                    }
                    "Buds" -> {
                        devices.find {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        }
                    }
                    else -> null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (targetDevice != null) {
                        audioManager.setCommunicationDevice(targetDevice)
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = (output == "Phone")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleAlwaysPlay(enabled: Boolean) {
        isAlwaysPlay = enabled
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("always_play_enabled", enabled).apply()
        }
    }

    // ========================================================
    // PER-SCREEN INDEPENDENT VIEW MODE PERSISTENCE METHODS
    // ========================================================
    fun updateHomeViewMode(mode: GridViewMode) {
        homeViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_home", mode.name).apply()
        }
    }

    fun cycleNextHomeViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (homeViewMode.ordinal + 1) % modes.size
        updateHomeViewMode(modes[nextIdx])
    }

    fun updateLibraryFoldersViewMode(mode: GridViewMode) {
        libraryFoldersViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_library_folders", mode.name).apply()
        }
    }

    fun cycleNextLibraryFoldersViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (libraryFoldersViewMode.ordinal + 1) % modes.size
        updateLibraryFoldersViewMode(modes[nextIdx])
    }

    fun updateFolderInnerViewMode(mode: GridViewMode) {
        folderInnerViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_folder_inner", mode.name).apply()
        }
    }

    fun cycleNextFolderInnerViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (folderInnerViewMode.ordinal + 1) % modes.size
        updateFolderInnerViewMode(modes[nextIdx])
    }

    fun updateArtistsViewMode(mode: GridViewMode) {
        artistsViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_artists", mode.name).apply()
        }
    }

    fun cycleNextArtistsViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (artistsViewMode.ordinal + 1) % modes.size
        updateArtistsViewMode(modes[nextIdx])
    }

    fun updateArtistInnerViewMode(mode: GridViewMode) {
        artistInnerViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_artist_inner", mode.name).apply()
        }
    }

    fun cycleNextArtistInnerViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (artistInnerViewMode.ordinal + 1) % modes.size
        updateArtistInnerViewMode(modes[nextIdx])
    }

    fun updatePlaylistInnerViewMode(mode: GridViewMode) {
        playlistInnerViewMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_view_playlist_inner", mode.name).apply()
        }
    }

    fun cycleNextPlaylistInnerViewMode() {
        val modes = GridViewMode.values()
        val nextIdx = (playlistInnerViewMode.ordinal + 1) % modes.size
        updatePlaylistInnerViewMode(modes[nextIdx])
    }

    // ========================================================
    // PER-SCREEN INDEPENDENT SORT ORDER PERSISTENCE METHODS
    // ========================================================
    fun setPersistentSongSort(order: SongSortOrder) {
        currentSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("saved_song_sort", order.name).apply()
        }
    }

    fun setPersistentFolderSort(order: FolderSortOrder) {
        currentFolderSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("saved_folder_sort", order.name).apply()
        }
    }

    fun setPersistentFolderInnerSort(order: SongSortOrder) {
        folderInnerSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("folder_inner_sort", order.name).apply()
        }
    }

    fun setPersistentArtistsSort(order: ArtistSortOrder) {
        artistsSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_sort_artists", order.name).apply()
        }
    }

    fun setPersistentArtistInnerSort(order: SongSortOrder) {
        artistInnerSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pref_sort_artist_inner", order.name).apply()
        }
    }

    fun setPersistentPlaylistInnerSort(order: SongSortOrder) {
        playlistInnerSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("playlist_inner_sort", order.name).apply()
        }
    }

    fun getSortedArtists(): List<ArtistItem> {
        return when (artistsSortOrder) {
            ArtistSortOrder.A_TO_Z -> parsedArtistsList.sortedBy { it.name.lowercase(Locale.getDefault()) }
            ArtistSortOrder.Z_TO_A -> parsedArtistsList.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            ArtistSortOrder.MOST_SONGS -> parsedArtistsList.sortedByDescending { it.songs.size }
            ArtistSortOrder.MOST_PLAYED -> parsedArtistsList.sortedByDescending { item -> item.songs.sumOf { it.playCount } }
        }
    }

    fun setPagerTransition(effect: PagerTransitionEffect) {
        pagerTransitionEffect = effect
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("pager_transition_effect", effect.name).apply()
        }
    }

    fun triggerFadeIn(durationMs: Long = 1000L) {
        fadeJob?.cancel()
        fadeJob = managerScope.launch {
            player.volume = 0f
            val steps = 25
            val stepDelay = (durationMs / steps).coerceAtLeast(15L)
            for (i in 1..steps) {
                delay(stepDelay)
                player.volume = i.toFloat() / steps
            }
            player.volume = 1.0f
        }
    }

    private fun startCrossfadeOut(fadeDurationMs: Long) {
        fadeJob?.cancel()
        fadeJob = managerScope.launch {
            val steps = 20
            val stepDelay = (fadeDurationMs / steps).coerceAtLeast(20L)
            val initialVol = player.volume
            for (i in steps downTo 0) {
                player.volume = initialVol * (i.toFloat() / steps)
                delay(stepDelay)
            }
            player.volume = 0f
        }
    }

    private fun startPositionTracker() {
        managerScope.launch {
            while (true) {
                if (isPlaying) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                    currentSong?.let { song ->
                        saveSongPosition(song.id, currentPosition)
                    }

                    if (isCrossfadeEnabled && duration > 0L) {
                        val remainingMs = duration - currentPosition
                        val fadeWindowMs = (crossfadeDuration * 1000).toLong().coerceIn(1000L, 12000L)
                        if (remainingMs in 1..fadeWindowMs && !isFadingOutForCrossfade) {
                            isFadingOutForCrossfade = true
                            startCrossfadeOut(fadeWindowMs)
                        }
                        if (remainingMs <= 350L && player.hasNextMediaItem()) {
                            playNext()
                        }
                    }
                }
                delay(250)
            }
        }
    }

    private fun syncDeviceVolume() {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            currentVolume = (cur / max).coerceIn(0f, 1f)
        } catch (_: Exception) {
            currentVolume = 0.7f
        }
    }

    fun setHardwareVolume(volFraction: Float) {
        currentVolume = volFraction.coerceIn(0f, 1f)
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (currentVolume * max).toInt(), 0)
        } catch (_: Exception) {}
    }

    private fun loadInstantCache() {
        val cachedJson = prefs.getString("cached_songs_catalog", null) ?: return
        try {
            val arr = JSONArray(cachedJson)
            val list = ArrayList<Song>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Song(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        artist = o.getString("artist"),
                        album = o.getString("album"),
                        albumId = o.getLong("albumId"),
                        duration = o.getLong("duration"),
                        size = o.getLong("size"),
                        uri = Uri.parse(o.getString("uri")),
                        path = o.getString("path"),
                        folderName = o.getString("folderName"),
                        releaseDate = o.optString("releaseDate", ""),
                        playCount = o.optInt("playCount", 0),
                        lastPlayed = o.optLong("lastPlayed", 0L),
                        isFavorite = o.optBoolean("isFavorite", false),
                        customCoverPath = o.optString("customCoverPath", null),
                        audioFormat = o.optString("audioFormat", "MP3"),
                        sampleRateHz = o.optInt("sampleRateHz", 44100),
                        bitDepth = o.optInt("bitDepth", 16),
                        replayGainTrackDb = -3.0f,
                        replayGainAlbumDb = -3.0f
                    )
                )
            }
            if (list.isNotEmpty()) {
                allSongs.clear()
                allSongs.addAll(list)
                rawStorageSongs.clear()
                rawStorageSongs.addAll(list)
                refreshHistory()
            }
        } catch (_: Exception) {}
    }

    private fun persistSongsCache(songs: List<Song>) {
        managerScope.launch(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                songs.forEach { s ->
                    val o = JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("artist", s.artist)
                        put("album", s.album)
                        put("albumId", s.albumId)
                        put("duration", s.duration)
                        put("size", s.size)
                        put("uri", s.uri.toString())
                        put("path", s.path)
                        put("folderName", s.folderName)
                        put("releaseDate", s.releaseDate)
                        put("playCount", s.playCount)
                        put("lastPlayed", s.lastPlayed)
                        put("isFavorite", s.isFavorite)
                        put("customCoverPath", s.customCoverPath ?: "")
                        put("audioFormat", s.audioFormat)
                        put("sampleRateHz", s.sampleRateHz)
                        put("bitDepth", s.bitDepth)
                    }
                    arr.put(o)
                }
                prefs.edit().putString("cached_songs_catalog", arr.toString()).apply()
            } catch (_: Exception) {}
        }
    }

    fun scanStorage() {
        if (isScanningStorage) return
        isScanningStorage = true

        managerScope.launch(Dispatchers.IO) {
            val songList = ArrayList<Song>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} COLLATE NOCASE ASC"

            try {
                context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawName = cursor.getString(displayCol) ?: "Unknown Track"
                        val cleanTitle = rawName.substringBeforeLast(".")
                        val rawArtist = cursor.getString(artistCol)
                        val artist = if (rawArtist.isNullOrBlank() || rawArtist.contains("unknown", ignoreCase = true)) "" else rawArtist
                        val album = cursor.getString(albumCol) ?: "Single"
                        val albumId = cursor.getLong(albumIdCol)
                        val durationMs = cursor.getLong(durationCol)
                        val fileSizeBytes = cursor.getLong(sizeCol)
                        val fullPath = cursor.getString(dataCol) ?: ""
                        val dateAdded = cursor.getLong(dateCol)

                        val file = File(fullPath)
                        val parentFolder = file.parentFile?.name ?: "Storage"
                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        val playCount = prefs.getInt("play_count_$id", 0)
                        val lastPlayed = prefs.getLong("last_played_$id", 0L)
                        val isFav = prefs.getBoolean("fav_$id", false)
                        val customCover = prefs.getString("custom_cover_$id", null)

                        val customTitle = prefs.getString("custom_title_$id", cleanTitle) ?: cleanTitle
                        val customArtist = prefs.getString("custom_artist_$id", artist) ?: artist
                        val customAlbum = prefs.getString("custom_album_$id", album) ?: album
                        val customDate = prefs.getString("custom_date_$id", dateAdded.toString()) ?: dateAdded.toString()

                        val ext = file.extension.uppercase(Locale.getDefault())
                        val format = when (ext) {
                            "FLAC" -> "FLAC"
                            "WAV" -> "WAV"
                            "M4A", "ALAC" -> "ALAC"
                            "DSF", "DFF" -> "DSD"
                            else -> "MP3"
                        }
                        val sampleRate = if (format in listOf("FLAC", "WAV", "DSD")) 96000 else 44100
                        val bitDepth = if (format in listOf("FLAC", "WAV", "DSD")) 24 else 16

                        songList.add(
                            Song(
                                id = id,
                                title = customTitle,
                                artist = customArtist,
                                album = customAlbum,
                                albumId = albumId,
                                duration = durationMs,
                                size = fileSizeBytes,
                                uri = uri,
                                path = fullPath,
                                folderName = parentFolder,
                                releaseDate = customDate,
                                playCount = playCount,
                                lastPlayed = lastPlayed,
                                isFavorite = isFav,
                                customCoverPath = customCover,
                                audioFormat = format,
                                sampleRateHz = sampleRate,
                                bitDepth = bitDepth,
                                replayGainTrackDb = -3.0f,
                                replayGainAlbumDb = -3.0f
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val hiddenFoldersSet = hiddenFolders.toHashSet()
            val hiddenAudioSet = hiddenAudioIds.toHashSet()
            val visibleSongs = songList.filter { it.folderName !in hiddenFoldersSet && it.id !in hiddenAudioSet }

            val parsed = withContext(Dispatchers.Default) {
                ArtistParsingEngine.parseAndGroupArtists(visibleSongs)
            }

            withContext(Dispatchers.Main.immediate) {
                rawStorageSongs.clear()
                rawStorageSongs.addAll(songList)
                allSongs.clear()
                allSongs.addAll(visibleSongs)
                refreshHistory()
                parsedArtistsList.clear()
                parsedArtistsList.addAll(parsed)
                isScanningStorage = false
            }
            persistSongsCache(visibleSongs)
        }
    }

    fun getSortedSongs(): List<Song> {
        return when (currentSortOrder) {
            SongSortOrder.A_TO_Z -> allSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> allSongs.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.NEWEST -> allSongs.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> allSongs.sortedBy { it.id }
            SongSortOrder.ARTIST -> allSongs.sortedBy { it.artist.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> allSongs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> allSongs.sortedByDescending { it.size }
        }
    }

    fun getSortedFolders(): List<String> {
        val grouped = allSongs.groupBy { it.folderName }
        return when (currentFolderSortOrder) {
            FolderSortOrder.A_TO_Z -> grouped.keys.sortedBy { it.lowercase(Locale.getDefault()) }
            FolderSortOrder.Z_TO_A -> grouped.keys.sortedByDescending { it.lowercase(Locale.getDefault()) }
            FolderSortOrder.LATEST -> grouped.keys.sortedByDescending { folder -> grouped[folder]?.maxOfOrNull { it.id } ?: 0L }
            FolderSortOrder.OLDEST -> grouped.keys.sortedBy { folder -> grouped[folder]?.minOfOrNull { it.id } ?: 0L }
            FolderSortOrder.MOST_PLAYED -> grouped.keys.sortedByDescending { folder -> grouped[folder]?.sumOf { it.playCount } ?: 0 }
            FolderSortOrder.LARGEST_SIZE -> grouped.keys.sortedByDescending { folder -> grouped[folder]?.sumOf { it.size } ?: 0L }
            FolderSortOrder.MOST_SONGS -> grouped.keys.sortedByDescending { folder -> grouped[folder]?.size ?: 0 }
        }
    }

    fun playSong(song: Song, queue: List<Song>, section: String, initialPositionMs: Long = 0L) {
        val isSameQueue = currentSectionName == section &&
                playbackQueue.size == queue.size &&
                player.mediaItemCount == queue.size

        currentSectionName = section
        currentSong = song

        val targetIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        if (isSameQueue && targetIndex in 0 until player.mediaItemCount) {
            if (initialPositionMs > 0L) {
                player.seekTo(targetIndex, initialPositionMs)
            } else {
                player.seekToDefaultPosition(targetIndex)
            }
            if (!player.isPlaying) {
                player.play()
            }
            recordSongPlayed(song)
            applyVolumeNormalization()
            updateNotification()
            return
        }

        playbackQueue.clear()
        playbackQueue.addAll(queue)

        managerScope.launch(Dispatchers.Default) {
            val mediaItems = queue.map { s ->
                MediaItem.Builder()
                    .setUri(s.uri)
                    .setMediaId(s.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .build()
                    )
                    .build()
            }

            withContext(Dispatchers.Main.immediate) {
                player.setMediaItems(mediaItems, targetIndex, initialPositionMs)
                player.prepare()
                if (isFadeOnStart) {
                    triggerFadeIn(1200L)
                } else {
                    player.volume = 1.0f
                }
                player.play()
                recordSongPlayed(song)
                applyVolumeNormalization()
                updateNotification()
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in playbackQueue.indices && toIndex in playbackQueue.indices && fromIndex != toIndex) {
            val moved = playbackQueue.removeAt(fromIndex)
            playbackQueue.add(toIndex, moved)
            try {
                player.moveMediaItem(fromIndex, toIndex)
            } catch (_: Exception) {}
        }
    }

    fun playNextInQueue(song: Song) {
        if (playbackQueue.isEmpty()) {
            playSong(song, listOf(song), currentSectionName)
            Toast.makeText(context, "Playing ${song.title}", Toast.LENGTH_SHORT).show()
            return
        }
        val currentIndex = player.currentMediaItemIndex
        val targetIndex = (currentIndex + 1).coerceAtMost(playbackQueue.size)
        playbackQueue.add(targetIndex, song)
        val mediaItem = MediaItem.Builder().setUri(song.uri).setMediaId(song.id.toString()).build()
        player.addMediaItem(targetIndex, mediaItem)
        Toast.makeText(context, "Will play next: ${song.title}", Toast.LENGTH_SHORT).show()
    }

    fun setAsRingtone(song: Song) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Toast.makeText(context, "Please allow permission to write system settings", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, song.uri)
            Toast.makeText(context, "Set as default ringtone!", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Could not set ringtone", Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            currentSong?.let { saveSongPosition(it.id, player.currentPosition) }
            player.pause()
        } else {
            if (isFadeOnStart) triggerFadeIn(1000L)
            player.play()
        }
        updateNotification()
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (repeatModeState == Player.REPEAT_MODE_ALL && playbackQueue.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
        updateNotification()
    }

    fun playPrevious() {
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
        updateNotification()
    }

    fun seekTo(positionMs: Long) {
        currentPosition = positionMs.coerceIn(0L, duration)
        player.seekTo(currentPosition)
        currentSong?.let { saveSongPosition(it.id, currentPosition) }
    }

    fun toggleRepeat() {
        repeatModeState = when (repeatModeState) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
        player.repeatMode = repeatModeState
    }

    fun toggleShuffle() {
        isShuffleOn = !isShuffleOn
        player.shuffleModeEnabled = isShuffleOn
    }

    fun toggleFavorite(song: Song) {
        val updatedFav = !song.isFavorite
        val updatedSong = song.copy(isFavorite = updatedFav)

        val songIdx = allSongs.indexOfFirst { it.id == song.id }
        if (songIdx != -1) allSongs[songIdx] = updatedSong

        if (currentSong?.id == song.id) currentSong = updatedSong

        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("fav_${song.id}", updatedFav).apply()
        }

        val favIndex = customPlaylists.indexOfFirst { it.name == "Favorites" }
        if (favIndex == -1) {
            val newFav = Playlist(
                id = "fav_01",
                name = "Favorites",
                songIds = if (updatedFav) listOf(song.id).toImmutableList() else emptyList<Long>().toImmutableList()
            )
            customPlaylists.add(newFav)
        } else {
            val existing = customPlaylists[favIndex]
            val mutableIds = existing.songIds.toMutableList()
            if (updatedFav) {
                if (song.id !in mutableIds) mutableIds.add(song.id)
            } else {
                mutableIds.remove(song.id)
            }
            customPlaylists[favIndex] = existing.copy(songIds = mutableIds.toImmutableList())
        }
        savePlaylists()
    }

    fun setMagneticSpeed(targetSpeed: Float): Float {
        val anchors = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
        val snapThreshold = 0.05f
        var finalSpeed = targetSpeed

        for (anchor in anchors) {
            if (Math.abs(targetSpeed - anchor) <= snapThreshold) {
                finalSpeed = anchor
                break
            }
        }
        playbackSpeed = finalSpeed
        player.playbackParameters = PlaybackParameters(finalSpeed)
        return finalSpeed
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            sleepTimerRemainingSeconds = 0
            return
        }
        sleepTimerRemainingSeconds = minutes * 60
        sleepTimerJob = managerScope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000)
                sleepTimerRemainingSeconds--
            }
            player.pause()
        }
    }

    fun endSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerRemainingSeconds = 0
        Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
    }

    fun setSleepTimerToEndOfTrack() {
        val remainingMs = (duration - currentPosition).coerceAtLeast(0L)
        val remainingSecs = (remainingMs / 1000).toInt()
        sleepTimerJob?.cancel()
        sleepTimerRemainingSeconds = remainingSecs
        sleepTimerJob = managerScope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000)
                sleepTimerRemainingSeconds--
            }
            player.pause()
        }
    }

    fun saveSongPosition(songId: Long, positionMs: Long) {
        prefs.edit().putLong("last_pos_$songId", positionMs).putLong("last_active_song_id", songId).apply()
    }

    fun getSavedPosition(songId: Long): Long {
        return prefs.getLong("last_pos_$songId", 0L)
    }

    fun resumeLastPlayed() {
        val lastSavedId = prefs.getLong("last_active_song_id", -1L)
        val candidate = allSongs.find { it.id == lastSavedId } ?: historySongs.firstOrNull() ?: allSongs.firstOrNull() ?: return

        val startPosition = if (isResumeFirstOnly) {
            getSavedPosition(candidate.id)
        } else {
            0L
        }

        playSong(candidate, allSongs, "Storage", initialPositionMs = startPosition)
    }

    fun createPlaylist(name: String) {
        val newPl = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name,
            songIds = emptyList<Long>().toImmutableList()
        )
        customPlaylists.add(newPl)
        savePlaylists()
    }

    fun pinFolderAsPlaylist(folderName: String) {
        if (customPlaylists.any { it.folderName == folderName }) return
        val songsInFolder = allSongs.filter { it.folderName == folderName }.map { it.id }.toImmutableList()
        val newPl = Playlist(
            id = "folder_${System.currentTimeMillis()}",
            name = folderName,
            songIds = songsInFolder,
            isFolderPinned = true,
            folderName = folderName,
            icon = "📁",
            iconColorHex = getFolderColor(folderName).toArgb().toLong()
        )
        customPlaylists.add(newPl)
        savePlaylists()
    }

    fun removePlaylist(playlist: Playlist) {
        customPlaylists.removeAll { it.id == playlist.id }
        savePlaylists()
    }

    fun addSongToPlaylist(songId: Long, playlist: Playlist) {
        val index = customPlaylists.indexOfFirst { it.id == playlist.id }
        if (index != -1 && songId !in playlist.songIds) {
            val updated = playlist.copy(songIds = (playlist.songIds + songId).toImmutableList())
            customPlaylists[index] = updated
            savePlaylists()
        }
    }

    fun removeSongFromPlaylist(songId: Long, playlist: Playlist) {
        val index = customPlaylists.indexOfFirst { it.id == playlist.id }
        if (index != -1 && songId in playlist.songIds) {
            val updated = playlist.copy(songIds = (playlist.songIds - songId).toImmutableList())
            customPlaylists[index] = updated
            savePlaylists()
        }
    }

    fun shufflePlaylist(playlist: Playlist) {
        val songIdSet = playlist.songIds.toHashSet()
        val songs = allSongs.filter { it.id in songIdSet }.shuffled()
        if (songs.isNotEmpty()) {
            playSong(songs.first(), songs, playlist.name)
        }
    }

    fun updateFolderColorOnly(folderName: String, colorHex: Long) {
        folderColors[folderName] = colorHex
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putLong("folder_color_$folderName", colorHex).apply()
        }
    }

    fun updatePlaylistColorOnly(playlist: Playlist, colorHex: Long) {
        val index = customPlaylists.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            customPlaylists[index] = playlist.copy(iconColorHex = colorHex)
            savePlaylists()
        }
    }

    fun getFolderColor(folderName: String): Color {
        val defaultMustard = 0xFFF59E0B
        val hex = folderColors[folderName] ?: prefs.getLong("folder_color_$folderName", defaultMustard)
        return Color(hex)
    }

    fun savePlaylists() {
        managerScope.launch(Dispatchers.IO) {
            val arr = JSONArray()
            for (pl in customPlaylists) {
                val obj = JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("isFolder", pl.isFolderPinned)
                    put("folderName", pl.folderName ?: "")
                    put("icon", pl.icon)
                    put("iconColorHex", pl.iconColorHex)
                    val idsArr = JSONArray()
                    pl.songIds.forEach { idsArr.put(it) }
                    put("songIds", idsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString("custom_playlists", arr.toString()).apply()
        }
    }

    fun reorderCustomPlaylists(newList: List<Playlist>) {
        customPlaylists.clear()
        customPlaylists.addAll(newList)
        savePlaylists()
    }

    fun updateSongMetadata(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newDate: String,
        customCoverUri: Uri?
    ) {
        managerScope.launch(Dispatchers.IO) {
            var coverPath = song.customCoverPath

            if (customCoverUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(customCoverUri)
                    val targetFile = File(context.filesDir, "cover_${song.id}.jpg")
                    val outputStream = FileOutputStream(targetFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    coverPath = targetFile.absolutePath

                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val bmp = BitmapFactory.decodeFile(targetFile.absolutePath, opts)
                    if (bmp != null) memoryCache.put(song.id, bmp)
                    prefs.edit().putString("custom_cover_${song.id}", targetFile.absolutePath).apply()
                } catch (_: Exception) {}
            }

            var finalPath = song.path
            try {
                val origFile = File(song.path)
                if (origFile.exists() && origFile.canWrite()) {
                    val ext = origFile.extension
                    val sanitizedTitle = newTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                    val newFileName = if (ext.isNotBlank()) "$sanitizedTitle.$ext" else sanitizedTitle
                    val renamedFile = File(origFile.parentFile, newFileName)

                    if (origFile.name != newFileName && origFile.renameTo(renamedFile)) {
                        finalPath = renamedFile.absolutePath
                    }

                    if (ext.equals("mp3", ignoreCase = true)) {
                        try {
                            RandomAccessFile(renamedFile, "rw").use { raf ->
                                if (raf.length() > 128) {
                                    raf.seek(raf.length() - 128)
                                    val tagBytes = ByteArray(3)
                                    raf.read(tagBytes)
                                    val hasTag = String(tagBytes) == "TAG"
                                    if (!hasTag) {
                                        raf.seek(raf.length())
                                        raf.write("TAG".toByteArray(Charsets.ISO_8859_1))
                                    } else {
                                        raf.seek(raf.length() - 125)
                                    }
                                    val titleBytes = ByteArray(30)
                                    val tB = newTitle.toByteArray(Charsets.ISO_8859_1)
                                    System.arraycopy(tB, 0, titleBytes, 0, tB.size.coerceAtMost(30))
                                    raf.write(titleBytes)

                                    val artistBytes = ByteArray(30)
                                    val aB = newArtist.toByteArray(Charsets.ISO_8859_1)
                                    System.arraycopy(aB, 0, artistBytes, 0, aB.size.coerceAtMost(30))
                                    raf.write(artistBytes)

                                    val albumBytes = ByteArray(30)
                                    val alB = newAlbum.toByteArray(Charsets.ISO_8859_1)
                                    System.arraycopy(alB, 0, albumBytes, 0, alB.size.coerceAtMost(30))
                                    raf.write(albumBytes)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val cv = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, newTitle)
                    put(MediaStore.Audio.Media.ARTIST, newArtist)
                    put(MediaStore.Audio.Media.ALBUM, newAlbum)
                    if (finalPath != song.path) {
                        put(MediaStore.Audio.Media.DATA, finalPath)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, File(finalPath).name)
                    }
                }
                context.contentResolver.update(song.uri, cv, null, null)
                MediaScannerConnection.scanFile(context, arrayOf(finalPath), null, null)
            } catch (_: Exception) {}

            prefs.edit()
                .putString("custom_title_${song.id}", newTitle)
                .putString("custom_artist_${song.id}", newArtist)
                .putString("custom_album_${song.id}", newAlbum)
                .putString("custom_date_${song.id}", newDate)
                .apply()

            val updated = song.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                releaseDate = newDate,
                path = finalPath,
                customCoverPath = coverPath
            )

            withContext(Dispatchers.Main.immediate) {
                val index = allSongs.indexOfFirst { it.id == song.id }
                if (index != -1) allSongs[index] = updated
                val rawIdx = rawStorageSongs.indexOfFirst { it.id == song.id }
                if (rawIdx != -1) rawStorageSongs[rawIdx] = updated
                val qIdx = playbackQueue.indexOfFirst { it.id == song.id }
                if (qIdx != -1) playbackQueue[qIdx] = updated
                if (currentSong?.id == song.id) currentSong = updated
                updateNotification()
                Toast.makeText(context, "Saved & updated file tags", Toast.LENGTH_SHORT).show()
            }

            val parsed = withContext(Dispatchers.Default) {
                ArtistParsingEngine.parseAndGroupArtists(allSongs)
            }
            withContext(Dispatchers.Main.immediate) {
                parsedArtistsList.clear()
                parsedArtistsList.addAll(parsed)
            }
        }
    }

    private fun recordSongPlayed(song: Song) {
        val updatedCount = song.playCount + 1
        val updatedTime = System.currentTimeMillis()
        val updated = song.copy(playCount = updatedCount, lastPlayed = updatedTime)

        val idx = allSongs.indexOfFirst { it.id == song.id }
        if (idx != -1) allSongs[idx] = updated
        val rawIdx = rawStorageSongs.indexOfFirst { it.id == song.id }
        if (rawIdx != -1) rawStorageSongs[rawIdx] = updated

        managerScope.launch(Dispatchers.IO) {
            prefs.edit()
                .putInt("play_count_${song.id}", updatedCount)
                .putLong("last_played_${song.id}", updatedTime)
                .apply()
        }
        refreshHistory()
    }

    private fun refreshHistory() {
        historySongs.clear()
        historySongs.addAll(
            allSongs.filter { it.lastPlayed > 0L }
                .sortedByDescending { it.lastPlayed }
                .take(300)
        )
    }

    fun clearHistory() {
        managerScope.launch(Dispatchers.IO) {
            historySongs.forEach { song ->
                prefs.edit().remove("last_played_${song.id}").apply()
            }
            withContext(Dispatchers.Main.immediate) {
                val reset = allSongs.map { it.copy(lastPlayed = 0L) }
                allSongs.clear()
                allSongs.addAll(reset)
                historySongs.clear()
                Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getMostPlayedSongs(): List<Song> {
        return allSongs.filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(300)
    }

    fun savePermanentProfile(name: String, email: String, imageUri: Uri?) {
        profileName = name
        profileEmail = email
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("prof_name", name).putString("prof_email", email).apply()

            if (imageUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val targetFile = File(context.filesDir, "user_avatar.jpg")
                    val outputStream = FileOutputStream(targetFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    profileImagePath = targetFile.absolutePath
                    prefs.edit().putString("prof_image_path", targetFile.absolutePath).apply()
                } catch (_: Exception) {}
            }
        }
    }

    fun updateAccent(color: Color) {
        accentColor = color
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putInt("accent_color", color.toArgb()).apply()
        }
    }

    fun addColorPreset(color: Color) {
        if (userSavedColorPresets.size >= 10) userSavedColorPresets.removeAt(0)
        if (color !in userSavedColorPresets) {
            userSavedColorPresets.add(color)
            saveColorPresets()
        }
    }

    private fun saveColorPresets() {
        managerScope.launch(Dispatchers.IO) {
            val arr = JSONArray()
            userSavedColorPresets.forEach { arr.put(it.toArgb()) }
            prefs.edit().putString("user_color_presets", arr.toString()).apply()
        }
    }

    private fun loadColorPresets() {
        val str = prefs.getString("user_color_presets", null)
        userSavedColorPresets.clear()
        if (str != null) {
            try {
                val arr = JSONArray(str)
                for (i in 0 until arr.length()) userSavedColorPresets.add(Color(arr.getInt(i)))
            } catch (_: Exception) {}
        }
        if (userSavedColorPresets.isEmpty()) {
            listOf(
                Color(0xFFF59E0B), Color(0xFF00B4D8), Color(0xFF10B981), Color(0xFF39FF14), Color(0xFFFF2A85),
                Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFFF6B35), Color(0xFFFFCC00)
            ).forEach { userSavedColorPresets.add(it) }
        }
    }

    fun setTheme(mode: String) {
        themeMode = mode
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("theme_mode", mode).apply()
        }
    }

    fun toggleHideFolder(folder: String) {
        if (folder in hiddenFolders) hiddenFolders.remove(folder) else hiddenFolders.add(folder)
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putStringSet("hidden_folders", hiddenFolders.toSet()).apply()
            scanStorage()
        }
    }

    fun toggleHideAudio(songId: Long) {
        if (songId in hiddenAudioIds) {
            hiddenAudioIds.remove(songId)
            rawStorageSongs.find { it.id == songId }?.let { restoredSong ->
                if (restoredSong.folderName !in hiddenFolders && allSongs.none { it.id == songId }) {
                    allSongs.add(restoredSong)
                }
            }
        } else {
            hiddenAudioIds.add(songId)
            allSongs.removeAll { it.id == songId }
        }
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putStringSet("hidden_audio", hiddenAudioIds.map { it.toString() }.toSet()).apply()
            scanStorage()
        }
    }

    fun deleteSongFromDevice(song: Song): Boolean {
        return try {
            val file = File(song.path)
            if (file.exists()) file.delete()
            context.contentResolver.delete(song.uri, null, null)
            allSongs.removeAll { it.id == song.id }
            rawStorageSongs.removeAll { it.id == song.id }
            playbackQueue.removeAll { it.id == song.id }
            historySongs.removeAll { it.id == song.id }

            managerScope.launch(Dispatchers.Default) {
                val parsed = ArtistParsingEngine.parseAndGroupArtists(allSongs)
                withContext(Dispatchers.Main.immediate) {
                    parsedArtistsList.clear()
                    parsedArtistsList.addAll(parsed)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
