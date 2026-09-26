package com.melovish.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
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
import java.util.Locale
import kotlin.math.pow

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
    val prefs: SharedPreferences = context.getSharedPreferences("melovish_prefs_v12", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Hardware Audio Processor for Mono Audio & Center-Vocal Cancellation
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

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }

    var mediaSession: MediaSession? = null
        private set

    // Hardware Audio Effects
    private var boundAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // Playback States
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

    // Collections
    val allSongs = mutableStateListOf<Song>()
    val rawStorageSongs = mutableStateListOf<Song>()
    val playbackQueue = mutableStateListOf<Song>()
    val historySongs = mutableStateListOf<Song>()
    val customPlaylists = mutableStateListOf<Playlist>()
    val hiddenFolders = mutableStateListOf<String>()
    val hiddenAudioIds = mutableStateListOf<Long>()
    val folderColors = mutableStateMapOf<String, Long>()
    val parsedArtistsList = mutableStateListOf<ArtistItem>()

    // Preferences & Theme
    var themeMode by mutableStateOf(prefs.getString("theme_mode", "System") ?: "System")
    var isDarkMode by mutableStateOf(false)
    var accentColor by mutableStateOf(Color(prefs.getInt("accent_color", 0xFF00B4D8.toInt())))
    val userSavedColorPresets = mutableStateListOf<Color>()

    // Selected Transition Effect for Full Screen Gesture Pager
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

    // Sorting
    var currentSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("saved_song_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )
    var currentFolderSortOrder by mutableStateOf(
        try {
            FolderSortOrder.valueOf(prefs.getString("saved_folder_sort", FolderSortOrder.A_TO_Z.name) ?: FolderSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            FolderSortOrder.A_TO_Z
        }
    )
    var folderInnerSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("folder_inner_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )
    var folderInnerIsCardView by mutableStateOf(prefs.getBoolean("folder_inner_card_view", false))
    var playlistInnerSortOrder by mutableStateOf(
        try {
            SongSortOrder.valueOf(prefs.getString("playlist_inner_sort", SongSortOrder.A_TO_Z.name) ?: SongSortOrder.A_TO_Z.name)
        } catch (_: Exception) {
            SongSortOrder.A_TO_Z
        }
    )
    var playlistInnerIsCardView by mutableStateOf(prefs.getBoolean("playlist_inner_card_view", false))

    // Player Settings Features
    var isColorfulPlayer by mutableStateOf(prefs.getBoolean("colorful_player", true))
    var isResumeFirstOnly by mutableStateOf(prefs.getBoolean("resume_first", false))
    var isFadeOnStart by mutableStateOf(prefs.getBoolean("fade_start", false))
    var isGaplessEnabled by mutableStateOf(prefs.getBoolean("gapless", true))
    var isCrossfadeEnabled by mutableStateOf(prefs.getBoolean("crossfade_enabled", false))
    var crossfadeDuration by mutableFloatStateOf(prefs.getFloat("crossfade_duration", 2.0f))

    // Audio Features
    var isLosslessEnabled by mutableStateOf(prefs.getBoolean("lossless", true))
    var isVolumeNormalized by mutableStateOf(prefs.getBoolean("vol_norm", false))
    var volumeBoostLevel by mutableFloatStateOf(prefs.getFloat("vol_boost", 100f))
    var isMonoAudio by mutableStateOf(prefs.getBoolean("mono", false))
    var selectedAudioOutput by mutableStateOf(prefs.getString("audio_output", "Phone") ?: "Phone")

    // Equalizer & Audio FX
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
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            syncDeviceVolume()
        }
    }

    init {
        activeInstance = this
        initDefaultEqualizerState()
        initMediaSession()
        setupBroadcastReceiver()
        loadPreferences()
        loadColorPresets()
        setupPlayerListener()
        startPositionTracker()
        syncDeviceVolume()
        applyChannelMixing()
        registerAudioDeviceCallback()
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
        try {
            when (output) {
                "Phone" -> audioManager.isSpeakerphoneOn = false
                "Speaker" -> audioManager.isSpeakerphoneOn = true
                "Buds" -> {
                    audioManager.isSpeakerphoneOn = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val devices = audioManager.availableCommunicationDevices
                        val btDevice = devices.find {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_HEARING_AID
                        }
                        if (btDevice != null) audioManager.setCommunicationDevice(btDevice)
                    }
                }
            }
        } catch (_: Exception) {}
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

    fun scanStorage() {
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
                        val customDate = prefs.getString("custom_date_$id", dateAdded.toString()) ?: ""

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
            }
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

    /**
     * Smooth, Zero-Flicker playback transition.
     * When switching between tracks in the active queue, performs an instant seek
     * rather than rebuilding the entire ExoPlayer playlist pipeline.
     */
    fun playSong(song: Song, queue: List<Song>, section: String, initialPositionMs: Long = 0L) {
        val isSameQueue = currentSectionName == section &&
                playbackQueue.size == queue.size &&
                player.mediaItemCount == queue.size

        currentSectionName = section
        currentSong = song

        val targetIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        if (isSameQueue && targetIndex in 0 until player.mediaItemCount) {
            // Instant seamless transition without rebuilding ExoPlayer queue
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
            return
        }

        // Full queue replacement only when entering a new list/playlist
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
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (repeatModeState == Player.REPEAT_MODE_ALL && playbackQueue.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
    }

    fun playPrevious() {
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
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

    fun updateSongMetadata(song: Song, newTitle: String, newArtist: String, newAlbum: String, newDate: String, customCoverUri: Uri?) {
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
                customCoverPath = coverPath
            )

            withContext(Dispatchers.Main.immediate) {
                val index = allSongs.indexOfFirst { it.id == song.id }
                if (index != -1) allSongs[index] = updated
                val rawIdx = rawStorageSongs.indexOfFirst { it.id == song.id }
                if (rawIdx != -1) rawStorageSongs[rawIdx] = updated
                if (currentSong?.id == song.id) currentSong = updated
                updateNotification()
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

    fun setPersistentFolderInnerCardView(isCard: Boolean) {
        folderInnerIsCardView = isCard
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("folder_inner_card_view", isCard).apply()
        }
    }

    fun setPersistentPlaylistInnerSort(order: SongSortOrder) {
        playlistInnerSortOrder = order
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putString("playlist_inner_sort", order.name).apply()
        }
    }

    fun setPersistentPlaylistInnerCardView(isCard: Boolean) {
        playlistInnerIsCardView = isCard
        managerScope.launch(Dispatchers.IO) {
            prefs.edit().putBoolean("playlist_inner_card_view", isCard).apply()
        }
    }

    fun movePlaylistItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in customPlaylists.indices && toIndex in customPlaylists.indices && fromIndex != toIndex) {
            val moved = customPlaylists.removeAt(fromIndex)
            customPlaylists.add(toIndex, moved)
            savePlaylists()
        }
    }

    fun getCachedAlbumArt(songId: Long): Bitmap? = memoryCache.get(songId)

    suspend fun loadAlbumArtAsync(song: Song): Bitmap? = withContext(Dispatchers.IO) {
        val cached = memoryCache.get(song.id)
        if (cached != null) return@withContext cached

        var resultBitmap: Bitmap? = null

        if (song.customCoverPath != null) {
            val file = File(song.customCoverPath)
            if (file.exists()) {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.absolutePath, this)
                    inSampleSize = calculateInSampleSize(this, 256, 256)
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                resultBitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
            }
        }

        if (resultBitmap == null) {
            try {
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(sArtworkUri, song.albumId)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    resultBitmap = BitmapFactory.decodeStream(stream, null, opts)
                }
            } catch (_: Exception) {}
        }

        if (resultBitmap == null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, this)
                        inSampleSize = calculateInSampleSize(this, 256, 256)
                        inJustDecodeBounds = false
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    resultBitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
                }
                retriever.release()
            } catch (_: Exception) {}
        }

        val finalBmp = resultBitmap
        if (finalBmp != null) {
            memoryCache.put(song.id, finalBmp)
            return@withContext finalBmp
        }
        null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun updateNotification() {
        val song = currentSong ?: return
        val art = getCachedAlbumArt(song.id)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val prevIntent = PendingIntent.getBroadcast(
            context, 1, Intent("ACTION_PREV"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            context, 2, Intent(if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getBroadcast(
            context, 3, Intent("ACTION_NEXT"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, MediaPlaybackService.NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}")
            .setContentIntent(contentPendingIntent)
            .setColor(accentColor.toArgb())
            .setColorized(true)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)

        if (art != null) {
            builder.setLargeIcon(art)
        }

        try {
            notificationManager.notify(MediaPlaybackService.NOTIF_ID, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPreferences() {
        hiddenFolders.clear()
        hiddenFolders.addAll(prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet())
        hiddenAudioIds.clear()
        hiddenAudioIds.addAll(
            prefs.getStringSet("hidden_audio", emptySet())?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        )

        val plJson = prefs.getString("custom_playlists", null)
        customPlaylists.clear()
        if (plJson != null) {
            try {
                val arr = JSONArray(plJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val isFolder = obj.optBoolean("isFolder", false)
                    val folderName = obj.optString("folderName", null)
                    val icon = obj.optString("icon", "📁")
                    val iconColorHex = obj.optLong("iconColorHex", 0xFFF59E0B)
                    val idsArr = obj.getJSONArray("songIds")
                    val songIds = ArrayList<Long>()
                    for (j in 0 until idsArr.length()) songIds.add(idsArr.getLong(j))
                    customPlaylists.add(
                        Playlist(
                            id = id,
                            name = name,
                            songIds = songIds.toImmutableList(),
                            isFolderPinned = isFolder,
                            folderName = folderName,
                            icon = icon,
                            iconColorHex = iconColorHex
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
