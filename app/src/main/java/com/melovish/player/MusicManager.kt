package com.melovish.player

import android.app.Notification
import android.app.NotificationChannel
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
import android.graphics.drawable.Icon
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MusicManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("melovish_prefs_v3", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
    }

    private var mediaSession: MediaSession? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val NOTIF_CHANNEL_ID = "melovish_playback_channel"
    private val NOTIF_ID = 1001

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // Playback State
    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var repeatModeState by mutableIntStateOf(Player.REPEAT_MODE_ALL)
    var isShuffleOn by mutableStateOf(false)
    var currentSectionName by mutableStateOf("All Songs")
    var currentVolume by mutableFloatStateOf(0.7f)

    // Volume Edge-Swipe HUD Overlay
    var isVolumeOverlayVisible by mutableStateOf(false)
    var volumeOverlayPercent by mutableIntStateOf(70)
    private var volumeOverlayJob: Job? = null

    // Sleep Timer
    var sleepTimerRemainingSeconds by mutableIntStateOf(0)
    private var sleepTimerJob: Job? = null

    // Lists & Sorting
    val allSongs = mutableStateListOf<Song>()
    val playbackQueue = mutableStateListOf<Song>()
    val historySongs = mutableStateListOf<Song>()
    val customPlaylists = mutableStateListOf<Playlist>()
    val hiddenFolders = mutableStateListOf<String>()
    val hiddenAudioIds = mutableStateListOf<Long>()
    var currentSortOrder by mutableStateOf(SongSortOrder.A_TO_Z)

    // Artwork Cache
    private val artworkCache = mutableMapOf<Long, Bitmap?>()

    // Dual Liquid Glass Theming
    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", false))
    var accentColor by mutableStateOf(Color(prefs.getInt("accent_color", 0xFFFF3B30.toInt())))
    var isColorfulPlayer by mutableStateOf(prefs.getBoolean("colorful_player", true))

    // Player Settings
    var isResumeFirstOnly by mutableStateOf(prefs.getBoolean("resume_first", false))
    var isFadeOnStart by mutableStateOf(prefs.getBoolean("fade_start", false))
    var isGaplessEnabled by mutableStateOf(prefs.getBoolean("gapless", true))
    var crossfadeDuration by mutableFloatStateOf(prefs.getFloat("crossfade", 0f))

    // Audio Settings
    var isLosslessEnabled by mutableStateOf(prefs.getBoolean("lossless", true))
    var isVolumeNormalized by mutableStateOf(prefs.getBoolean("vol_norm", false))
    var volumeBoostLevel by mutableFloatStateOf(prefs.getFloat("vol_boost", 100f))
    var isMonoAudio by mutableStateOf(prefs.getBoolean("mono", false))
    var selectedAudioOutput by mutableStateOf(prefs.getString("audio_output", "Phone") ?: "Phone")

    // Equalizer Options
    var isEqEnabled by mutableStateOf(prefs.getBoolean("eq_enabled", false))
    var bassBoostPercent by mutableIntStateOf(prefs.getInt("bass_boost", 100))
    var isStopBass by mutableStateOf(prefs.getBoolean("stop_bass", false))
    var isRemoveVocals by mutableStateOf(prefs.getBoolean("remove_vocals", false))

    // Profile Info & Permanent Disk Avatar
    var profileName by mutableStateOf(prefs.getString("prof_name", "Bharat Bhushan") ?: "Bharat Bhushan")
    var profileEmail by mutableStateOf(prefs.getString("prof_email", "") ?: "")
    var profileImagePath by mutableStateOf(prefs.getString("prof_image_path", null))

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        initMediaSession()
        createNotificationChannel()
        setupBroadcastReceiver()
        loadPreferences()
        setupPlayerListener()
        startPositionTracker()
        syncDeviceVolume()
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSession(context, "MelovishMediaSession").apply {
                isActive = true
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() { togglePlayPause() }
                    override fun onPause() { togglePlayPause() }
                    override fun onSkipToNext() { playNext() }
                    override fun onSkipToPrevious() { playPrevious() }
                    override fun onSeekTo(pos: Long) { seekTo(pos) }
                })
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification and lock screen media player"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
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
        } catch (_: Exception) {}
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updateMediaSessionState()
                updateNotification()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    attachAudioEffects()
                    updateMediaSessionMetadata()
                    updateMediaSessionState()
                    updateNotification()
                } else if (state == Player.STATE_ENDED) {
                    playNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentUri = mediaItem?.localConfiguration?.uri
                val song = allSongs.find { it.uri == currentUri }
                if (song != null) {
                    currentSong = song
                    recordSongPlayed(song)
                    updateMediaSessionMetadata()
                    updateMediaSessionState()
                    updateNotification()
                }
            }
        })
    }

    private fun updateMediaSessionState() {
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(state, player.currentPosition, playbackSpeed)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata() {
        val song = currentSong ?: return
        val art = getAlbumArt(song)
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, if (song.artist.isNotBlank()) song.artist else "Unknown Artist")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

        if (art != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, art)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, art)
        }
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun extractNotificationColor(art: Bitmap?): Int {
        if (art == null) return accentColor.toArgb()
        return try {
            val scaled = Bitmap.createScaledBitmap(art, 20, 20, false)
            var maxSaturation = -1f
            var vibrantColor = accentColor.toArgb()
            val hsv = FloatArray(3)

            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val pixel = scaled.getPixel(x, y)
                    android.graphics.Color.colorToHSV(pixel, hsv)
                    if (hsv[1] > maxSaturation && hsv[2] > 0.25f && hsv[2] < 0.95f) {
                        maxSaturation = hsv[1]
                        vibrantColor = pixel
                    }
                }
            }
            vibrantColor
        } catch (_: Exception) {
            accentColor.toArgb()
        }
    }

    fun updateNotification() {
        val song = currentSong ?: return
        val art = getAlbumArt(song)
        val token = mediaSession?.sessionToken ?: return
        val dynamicColor = extractNotificationColor(art)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_media_previous),
            "Previous",
            PendingIntent.getBroadcast(context, 1, Intent("ACTION_PREV"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(context, playPauseIcon),
            playPauseTitle,
            PendingIntent.getBroadcast(context, 2, Intent(if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_media_next),
            "Next",
            PendingIntent.getBroadcast(context, 3, Intent("ACTION_NEXT"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        ).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, NOTIF_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText("${formatFileSize(song.size)} • ${if (song.artist.isNotBlank()) song.artist else "Melovish"}")
            .setContentIntent(contentPendingIntent)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setColor(dynamicColor)
            .setColorized(true)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)

        if (art != null) {
            builder.setLargeIcon(art)
        }

        try {
            val notification = builder.build()
            notificationManager.notify(NOTIF_ID, notification)
            MediaPlaybackService.attachNotification(notification)
        } catch (_: Exception) {}
    }

    fun attachAudioEffects() {
        try {
            val audioSessionId = player.audioSessionId
            if (audioSessionId != 0) {
                if (equalizer == null) {
                    equalizer = Equalizer(0, audioSessionId).apply {
                        enabled = isEqEnabled
                    }
                }
                equalizer?.enabled = isEqEnabled

                if (bassBoost == null) {
                    bassBoost = BassBoost(0, audioSessionId)
                }
                bassBoost?.enabled = !isStopBass && isEqEnabled
                bassBoost?.setStrength((bassBoostPercent * 10).toShort().coerceIn(0, 1000))

                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                }
                val boostGainMb = ((volumeBoostLevel - 100f) * 30f).toInt()
                loudnessEnhancer?.setTargetGain(boostGainMb.coerceAtLeast(0))
                loudnessEnhancer?.enabled = volumeBoostLevel > 100f

                if (isStopBass && equalizer != null) {
                    val numBands = equalizer!!.numberOfBands
                    if (numBands > 0) {
                        equalizer!!.setBandLevel(0, equalizer!!.bandLevelRange[0])
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun setAudioOutputRouting(output: String) {
        selectedAudioOutput = output
        prefs.edit().putString("audio_output", output).apply()
        try {
            when (output) {
                "Phone" -> {
                    audioManager.isSpeakerphoneOn = false
                }
                "Speaker" -> {
                    audioManager.isSpeakerphoneOn = true
                }
                "Buds" -> {
                    audioManager.isSpeakerphoneOn = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val devices = audioManager.availableCommunicationDevices
                        val btDevice = devices.find {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_HEARING_AID
                        }
                        if (btDevice != null) {
                            audioManager.setCommunicationDevice(btDevice)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun startPositionTracker() {
        scope.launch {
            while (true) {
                if (isPlaying) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                }
                delay(300)
            }
        }
    }

    private fun syncDeviceVolume() {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            currentVolume = (cur / max).coerceIn(0f, 1f)
            volumeOverlayPercent = (currentVolume * 100).toInt()
        } catch (_: Exception) {
            currentVolume = 0.7f
            volumeOverlayPercent = 70
        }
    }

    fun adjustVolumeByDelta(deltaPercent: Float) {
        val newVol = (currentVolume + (deltaPercent / 100f)).coerceIn(0f, 1f)
        currentVolume = newVol
        volumeOverlayPercent = (newVol * 100).toInt()
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (newVol * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {}

        isVolumeOverlayVisible = true
        volumeOverlayJob?.cancel()
        volumeOverlayJob = scope.launch {
            delay(1200)
            isVolumeOverlayVisible = false
        }
    }

    fun scanStorage() {
        val songList = mutableListOf<Song>()
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
                            customCoverPath = customCover
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        allSongs.clear()
        val visibleSongs = songList.filter { it.folderName !in hiddenFolders && it.id !in hiddenAudioIds }
        allSongs.addAll(visibleSongs)
        refreshHistory()
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

    fun getAlbumArt(song: Song): Bitmap? {
        if (song.customCoverPath != null) {
            val file = File(song.customCoverPath!!)
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            }
        }
        if (artworkCache.containsKey(song.id)) {
            return artworkCache[song.id]
        }
        var bitmap: Bitmap? = null
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.uri)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            }
            retriever.release()
        } catch (_: Exception) {
            try {
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(sArtworkUri, song.albumId)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
        artworkCache[song.id] = bitmap
        return bitmap
    }

    fun playSong(song: Song, queue: List<Song>, section: String) {
        currentSectionName = section
        currentSong = song
        playbackQueue.clear()
        playbackQueue.addAll(queue)

        val mediaItems = queue.map { MediaItem.fromUri(it.uri) }
        player.setMediaItems(mediaItems)
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.seekTo(index, 0L)
        player.prepare()
        player.play()
        recordSongPlayed(song)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
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
        currentPosition = positionMs
        player.seekTo(positionMs)
        updateMediaSessionState()
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
        song.isFavorite = !song.isFavorite
        prefs.edit().putBoolean("fav_${song.id}", song.isFavorite).apply()

        var favList = customPlaylists.find { it.name == "Favorites" }
        if (favList == null) {
            favList = Playlist(id = "fav_01", name = "Favorites", songIds = mutableListOf())
            customPlaylists.add(favList)
        }
        if (song.isFavorite) {
            if (song.id !in favList.songIds) favList.songIds.add(song.id)
        } else {
            favList.songIds.remove(song.id)
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
        sleepTimerJob = scope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000)
                sleepTimerRemainingSeconds--
            }
            player.pause()
        }
    }

    fun setSleepTimerToEndOfTrack() {
        val remainingMs = (duration - currentPosition).coerceAtLeast(0L)
        val remainingSecs = (remainingMs / 1000).toInt()
        sleepTimerJob?.cancel()
        sleepTimerRemainingSeconds = remainingSecs
        sleepTimerJob = scope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000)
                sleepTimerRemainingSeconds--
            }
            player.pause()
        }
    }

    fun resumeLastPlayed(preferredSection: String? = null) {
        val candidate = historySongs.firstOrNull() ?: allSongs.firstOrNull()
        if (candidate != null) {
            val listToPlay = if (preferredSection != null && preferredSection == candidate.folderName) {
                allSongs.filter { it.folderName == preferredSection }
            } else {
                allSongs
            }
            playSong(candidate, listToPlay, preferredSection ?: "Storage")
        }
    }

    fun createPlaylist(name: String) {
        val newPl = Playlist(id = System.currentTimeMillis().toString(), name = name)
        customPlaylists.add(newPl)
        savePlaylists()
    }

    fun pinFolderAsPlaylist(folderName: String) {
        if (customPlaylists.any { it.folderName == folderName }) return
        val songsInFolder = allSongs.filter { it.folderName == folderName }.map { it.id }
        val newPl = Playlist(
            id = "folder_${System.currentTimeMillis()}",
            name = folderName,
            songIds = songsInFolder.toMutableList(),
            isFolderPinned = true,
            folderName = folderName
        )
        customPlaylists.add(newPl)
        savePlaylists()
    }

    fun addSongToPlaylist(songId: Long, playlist: Playlist) {
        if (songId !in playlist.songIds) {
            playlist.songIds.add(songId)
            savePlaylists()
        }
    }

    fun removeSongFromPlaylist(songId: Long, playlist: Playlist) {
        playlist.songIds.remove(songId)
        savePlaylists()
    }

    fun reorderPlaylistSongs(playlist: Playlist, fromIndex: Int, toIndex: Int) {
        if (fromIndex in playlist.songIds.indices && toIndex in playlist.songIds.indices) {
            val item = playlist.songIds.removeAt(fromIndex)
            playlist.songIds.add(toIndex, item)
            savePlaylists()
        }
    }

    fun shufflePlaylist(playlist: Playlist) {
        val songs = allSongs.filter { it.id in playlist.songIds }.shuffled()
        if (songs.isNotEmpty()) {
            playSong(songs.first(), songs, playlist.name)
        }
    }

    private fun savePlaylists() {
        val arr = JSONArray()
        for (pl in customPlaylists) {
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("isFolder", pl.isFolderPinned)
                put("folderName", pl.folderName ?: "")
                val idsArr = JSONArray()
                pl.songIds.forEach { idsArr.put(it) }
                put("songIds", idsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("custom_playlists", arr.toString()).apply()
    }

    fun updateSongMetadata(song: Song, newTitle: String, newArtist: String, newAlbum: String, newDate: String, customCoverUri: Uri?) {
        song.title = newTitle
        song.artist = newArtist
        song.album = newAlbum
        song.releaseDate = newDate

        prefs.edit()
            .putString("custom_title_${song.id}", newTitle)
            .putString("custom_artist_${song.id}", newArtist)
            .putString("custom_album_${song.id}", newAlbum)
            .putString("custom_date_${song.id}", newDate)
            .apply()

        if (customCoverUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(customCoverUri)
                val targetFile = File(context.filesDir, "cover_${song.id}.jpg")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                song.customCoverPath = targetFile.absolutePath
                artworkCache[song.id] = BitmapFactory.decodeFile(targetFile.absolutePath)
                prefs.edit().putString("custom_cover_${song.id}", targetFile.absolutePath).apply()
            } catch (_: Exception) {}
        }
        updateMediaSessionMetadata()
        updateNotification()
    }

    private fun recordSongPlayed(song: Song) {
        song.playCount += 1
        song.lastPlayed = System.currentTimeMillis()
        prefs.edit()
            .putInt("play_count_${song.id}", song.playCount)
            .putLong("last_played_${song.id}", song.lastPlayed)
            .apply()
        refreshHistory()
    }

    private fun refreshHistory() {
        historySongs.clear()
        historySongs.addAll(
            allSongs.filter { it.lastPlayed > 0L }
                .sortedByDescending { it.lastPlayed }
                .take(100)
        )
    }

    fun getMostPlayedSongs(): List<Song> {
        return allSongs.filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(100)
    }

    fun savePermanentProfile(name: String, email: String, imageUri: Uri?) {
        profileName = name
        profileEmail = email
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

    fun updateAccent(color: Color) {
        accentColor = color
        prefs.edit().putInt("accent_color", color.toArgb()).apply()
    }

    fun toggleDarkMode(dark: Boolean) {
        isDarkMode = dark
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    fun toggleHideFolder(folder: String) {
        if (folder in hiddenFolders) hiddenFolders.remove(folder) else hiddenFolders.add(folder)
        prefs.edit().putStringSet("hidden_folders", hiddenFolders.toSet()).apply()
        scanStorage()
    }

    fun toggleHideAudio(songId: Long) {
        if (songId in hiddenAudioIds) hiddenAudioIds.remove(songId) else hiddenAudioIds.add(songId)
        prefs.edit().putStringSet("hidden_audio", hiddenAudioIds.map { it.toString() }.toSet()).apply()
        scanStorage()
    }

    fun deleteSongFromDevice(song: Song): Boolean {
        return try {
            val file = File(song.path)
            if (file.exists()) file.delete()
            context.contentResolver.delete(song.uri, null, null)
            allSongs.remove(song)
            playbackQueue.remove(song)
            historySongs.remove(song)
            true
        } catch (_: Exception) {
            false
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
                    val idsArr = obj.getJSONArray("songIds")
                    val songIds = mutableListOf<Long>()
                    for (j in 0 until idsArr.length()) songIds.add(idsArr.getLong(j))
                    customPlaylists.add(Playlist(id, name, songIds, isFolder, folderName))
                }
            } catch (_: Exception) {}
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
