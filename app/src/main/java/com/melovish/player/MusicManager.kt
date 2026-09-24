package com.melovish.player

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MusicManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("melovish_prefs", Context.MODE_PRIVATE)

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // Playback State
    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var repeatModeState by mutableIntStateOf(Player.REPEAT_MODE_ALL) // 0: OFF, 1: ONE, 2: ALL
    var isShuffleOn by mutableStateOf(false)
    var currentSectionName by mutableStateOf("All Songs")

    // Sleep Timer
    var sleepTimerRemainingSeconds by mutableIntStateOf(0)
    private var sleepTimerJob: Job? = null

    // Lists
    val allSongs = mutableStateListOf<Song>()
    val playbackQueue = mutableStateListOf<Song>()
    val historySongs = mutableStateListOf<Song>()
    val hiddenFolders = mutableStateListOf<String>()
    val hiddenAudioIds = mutableStateListOf<Long>()

    // Settings
    var selectedTheme by mutableStateOf(prefs.getString("theme", "System") ?: "System")
    var selectedPaletteIndex by mutableIntStateOf(prefs.getInt("palette_idx", 0))
    var isColorfulPlayer by mutableStateOf(prefs.getBoolean("colorful_player", true))
    var isResumeFirstOnly by mutableStateOf(prefs.getBoolean("resume_first", false))
    var isFadeOnStart by mutableStateOf(prefs.getBoolean("fade_start", false))
    var isGaplessEnabled by mutableStateOf(prefs.getBoolean("gapless", true))
    var crossfadeDuration by mutableFloatStateOf(prefs.getFloat("crossfade", 0f))
    var isLosslessEnabled by mutableStateOf(prefs.getBoolean("lossless", true))
    var isVolumeNormalized by mutableStateOf(prefs.getBoolean("vol_norm", false))
    var volumeBoostLevel by mutableFloatStateOf(prefs.getFloat("vol_boost", 100f))
    var isMonoAudio by mutableStateOf(prefs.getBoolean("mono", false))
    var selectedAudioOutput by mutableStateOf(prefs.getString("audio_output", "Phone") ?: "Phone")

    // Equalizer Options
    var isEqEnabled by mutableStateOf(false)
    var bassBoostPercent by mutableIntStateOf(100)
    var isStopBass by mutableStateOf(false)
    var isRemoveVocals by mutableStateOf(false)

    // User Profile
    var profileName by mutableStateOf(prefs.getString("prof_name", "Bharat Bhushan") ?: "Bharat Bhushan")
    var profileEmail by mutableStateOf(prefs.getString("prof_email", "bharatbhushan@bharat") ?: "bharatbhushan@bharat")
    var profileImageUri by mutableStateOf(prefs.getString("prof_image", null))

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        loadHiddenPreferences()
        setupPlayerListener()
        startPositionTracker()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                    attachAudioEffects()
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
                }
            }
        })
    }

    private fun attachAudioEffects() {
        try {
            val audioSessionId = player.audioSessionId
            if (audioSessionId != 0) {
                if (equalizer == null) {
                    equalizer = Equalizer(0, audioSessionId).apply { enabled = isEqEnabled }
                }
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, audioSessionId).apply {
                        enabled = !isStopBass
                        setStrength((bassBoostPercent * 10).toShort().coerceIn(0, 1000))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun startPositionTracker() {
        scope.launch {
            while (isActive) {
                if (isPlaying) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                }
                delay(500)
            }
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
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawName = cursor.getString(displayCol) ?: "Unknown Track"
                    val cleanTitle = rawName.substringBeforeLast(".")
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist.contains("unknown", ignoreCase = true)) "" else rawArtist
                    val album = cursor.getString(albumCol) ?: "Single"
                    val durationMs = cursor.getLong(durationCol)
                    val fileSizeBytes = cursor.getLong(sizeCol)
                    val fullPath = cursor.getString(dataCol) ?: ""

                    val file = File(fullPath)
                    val parentFolder = file.parentFile?.name ?: "Storage"
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val playCount = prefs.getInt("play_count_$id", 0)
                    val lastPlayed = prefs.getLong("last_played_$id", 0L)

                    songList.add(
                        Song(
                            id = id,
                            title = cleanTitle,
                            artist = artist,
                            album = album,
                            duration = durationMs,
                            size = fileSizeBytes,
                            uri = uri,
                            path = fullPath,
                            folderName = parentFolder,
                            playCount = playCount,
                            lastPlayed = lastPlayed
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
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
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

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        player.playbackParameters = PlaybackParameters(speed)
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

    fun saveProfile(name: String, email: String, imageUri: String?) {
        profileName = name
        profileEmail = email
        profileImageUri = imageUri
        prefs.edit()
            .putString("prof_name", name)
            .putString("prof_email", email)
            .putString("prof_image", imageUri)
            .apply()
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

    private fun loadHiddenPreferences() {
        hiddenFolders.clear()
        hiddenFolders.addAll(prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet())
        hiddenAudioIds.clear()
        hiddenAudioIds.addAll(
            prefs.getStringSet("hidden_audio", emptySet())?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        )
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
