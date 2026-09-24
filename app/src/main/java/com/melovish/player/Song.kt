package com.melovish.player

import android.net.Uri

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
    var playCount: Int = 0,
    var lastPlayed: Long = 0L,
    var isFavorite: Boolean = false,
    var customCoverUri: String? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val songIds: MutableList<Long> = mutableListOf(),
    val isFolderPinned: Boolean = false,
    val folderName: String? = null
)
