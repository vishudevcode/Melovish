package com.melovish.player

import android.net.Uri
import java.util.Locale

data class Song(
    val id: Long,
    var title: String,
    var artist: String,
    var album: String,
    val albumId: Long,
    val duration: Long,
    val size: Long,
    val uri: Uri,
    val path: String,
    val folderName: String,
    var releaseDate: String = "",
    var playCount: Int = 0,
    var lastPlayed: Long = 0L,
    var isFavorite: Boolean = false,
    var customCoverPath: String? = null
)

data class Playlist(
    val id: String,
    var name: String,
    val songIds: MutableList<Long> = mutableListOf(),
    val isFolderPinned: Boolean = false,
    val folderName: String? = null,
    var icon: String = "📁",
    var iconColorHex: Long = 0xFFF59E0B
)

enum class SongSortOrder {
    A_TO_Z,
    Z_TO_A,
    NEWEST,
    OLDEST,
    ARTIST,
    FILE_SIZE,
    DURATION
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

// Universal formatting utilities accessible across the entire project
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
