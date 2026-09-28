package com.melovish.player

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class GridViewMode {
    LIST, GRID_2, GRID_3, GRID_4, HERO_GRID
}

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val size: Long,
    val uri: Uri,
    val path: String,
    val folderName: String,
    val releaseDate: String = "",
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val customCoverPath: String? = null,
    val audioFormat: String = "MP3",
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val replayGainTrackDb: Float = 0.0f,
    val replayGainAlbumDb: Float = 0.0f,
    val formattedDuration: String = formatTime(duration),
    val formattedSize: String = formatFileSize(size),
    val displayArtist: String = if (artist.isNotBlank() && !artist.equals("<unknown>", ignoreCase = true)) artist else "Unknown Artist"
) {
    val isHiRes: Boolean
        get() = sampleRateHz >= 96000 || bitDepth >= 24 || audioFormat.equals("DSD", ignoreCase = true)
}

@Immutable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: ImmutableList<Long> = persistentListOf(),
    val isFolderPinned: Boolean = false,
    val folderName: String? = null,
    val icon: String = "📁",
    val iconColorHex: Long = 0xFFF59E0B
)

@Immutable
data class ArtistItem(
    val name: String,
    val songs: ImmutableList<Song> = persistentListOf(),
    val isPinned: Boolean = false
)

@Immutable
data class AlbumItem(
    val name: String,
    val artist: String,
    val albumId: Long,
    val songs: ImmutableList<Song> = persistentListOf()
)

@Immutable
data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: Int = 2,
    val isShuffleOn: Boolean = false,
    val effectiveOutput: String = "Phone",
    val isScanning: Boolean = false,
    val isInitialLoading: Boolean = true
)

enum class SongSortOrder {
    A_TO_Z,
    Z_TO_A,
    NEWEST,
    OLDEST,
    ARTIST,
    DURATION,
    FILE_SIZE
}

enum class FolderSortOrder {
    A_TO_Z,
    Z_TO_A,
    LATEST,
    OLDEST,
    MOST_PLAYED,
    LARGEST_SIZE,
    MOST_SONGS
}

enum class ArtistSortOrder {
    NAME_A_TO_Z,
    NAME_Z_TO_A,
    MOST_TRACKS,
    FEWEST_TRACKS
}

enum class ArtistSongSortOrder {
    TITLE_A_TO_Z,
    DURATION,
    FILE_SIZE,
    NEWEST
}

enum class ReplayGainMode {
    OFF,
    TRACK,
    ALBUM
}

enum class ReverbPresetMode(val label: String) {
    NONE("None"),
    SMALL_ROOM("Small Room"),
    MEDIUM_ROOM("Medium Room"),
    LARGE_ROOM("Large Room"),
    MEDIUM_HALL("Medium Hall"),
    LARGE_HALL("Large Hall"),
    PLATE("Plate")
}

enum class PagerTransitionEffect(val label: String) {
    SLIDE("Slide"),
    CASCADE("Cascade"),
    CROSSFADE("Crossfade"),
    ROTATE("Rotate"),
    TUMBLE("Tumble"),
    PAGE("Page")
}

val EQUALIZER_32_BANDS: List<String> = listOf(
    "20 Hz", "25 Hz", "31.5 Hz", "40 Hz", "50 Hz", "63 Hz", "80 Hz", "100 Hz",
    "125 Hz", "160 Hz", "200 Hz", "250 Hz", "315 Hz", "400 Hz", "500 Hz", "630 Hz",
    "800 Hz", "1 kHz", "1.25 kHz", "1.6 kHz", "2 kHz", "2.5 kHz", "3.15 kHz", "4 kHz",
    "5 kHz", "6.3 kHz", "8 kHz", "10 kHz", "12.5 kHz", "16 kHz", "18 kHz", "20 kHz"
)

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val remSecs = totalSeconds % 3600L
    val minutes = remSecs / 60L
    val seconds = remSecs % 60L

    val minTens = (minutes / 10L)
    val minOnes = (minutes % 10L)
    val secTens = (seconds / 10L)
    val secOnes = (seconds % 10L)

    return if (hours > 0L) {
        StringBuilder(10)
            .append(hours)
            .append(':')
            .append(minTens)
            .append(minOnes)
            .append(':')
            .append(secTens)
            .append(secOnes)
            .toString()
    } else {
        StringBuilder(6)
            .append(minTens)
            .append(minOnes)
            .append(':')
            .append(secTens)
            .append(secOnes)
            .toString()
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val kilo = 1024L
    val mega = kilo * 1024L
    val giga = mega * 1024L

    return when {
        bytes >= giga -> {
            val whole = bytes / giga
            val fraction = ((bytes % giga) * 100L) / giga
            val fracTens = fraction / 10L
            val fracOnes = fraction % 10L
            "$whole.$fracTens$fracOnes GB"
        }
        bytes >= mega -> {
            val whole = bytes / mega
            val fraction = ((bytes % mega) * 10L) / mega
            "$whole.$fraction MB"
        }
        bytes >= kilo -> {
            val whole = bytes / kilo
            val fraction = ((bytes % kilo) * 10L) / kilo
            "$whole.$fraction KB"
        }
        else -> "$bytes B"
    }
}
