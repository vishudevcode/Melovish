package com.melovish.player

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

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
    val replayGainAlbumDb: Float = 0.0f
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

enum class DarkThemeSubStyle {
    BLUISH,
    AMOLED_BLACK,
    CUSTOM
}

enum class LightThemeSubStyle {
    WHITE,
    CREAM,
    CUSTOM
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

// 6 Preset Pager Horizontal Transitions
enum class PagerTransitionEffect(val label: String) {
    SLIDE("Slide"),
    CASCADE("Cascade"),
    CROSSFADE("Crossfade"),
    ROTATE("Rotate"),
    TUMBLE("Tumble"),
    PAGE("Page")
}

val EQUALIZER_32_BANDS = listOf(
    "20 Hz", "25 Hz", "31.5 Hz", "40 Hz", "50 Hz", "63 Hz", "80 Hz", "100 Hz",
    "125 Hz", "160 Hz", "200 Hz", "250 Hz", "315 Hz", "400 Hz", "500 Hz", "630 Hz",
    "800 Hz", "1 kHz", "1.25 kHz", "1.6 kHz", "2 kHz", "2.5 kHz", "3.15 kHz", "4 kHz",
    "5 kHz", "6.3 kHz", "8 kHz", "10 kHz", "12.5 kHz", "16 kHz", "18 kHz", "20 kHz"
)

/**
 * Millisecond Time Formatter (mm:ss or h:mm:ss)
 * Displays track durations under an hour as mm:ss and longer audio tracks as h:mm:ss.
 */
fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Adaptive file size formatter.
 * Automatically displays in GB for sizes >= 1024 MB, in MB for sizes >= 1 MB, and in KB for smaller files.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val kilo = 1024.0
    val mega = kilo * 1024.0
    val giga = mega * 1024.0

    return when {
        bytes >= giga -> {
            val gb = bytes / giga
            String.format(Locale.US, "%.2f GB", gb)
        }
        bytes >= mega -> {
            val mb = bytes / mega
            String.format(Locale.US, "%.1f MB", mb)
        }
        bytes >= kilo -> {
            val kb = bytes / kilo
            String.format(Locale.US, "%.1f KB", kb)
        }
        else -> "$bytes B"
    }
}
