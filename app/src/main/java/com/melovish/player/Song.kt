package com.melovish.player

import android.net.Uri

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
    var icon: String = "📑",
    var iconColorHex: Long = 0xFF00B4D8
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
