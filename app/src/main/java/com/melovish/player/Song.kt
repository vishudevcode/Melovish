package com.melovish.player

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,          // Raw audio file name
    val artist: String,         // Metadata artist (empty if absent)
    val album: String,
    val duration: Long,
    val size: Long,             // In bytes
    val uri: Uri,
    val path: String,
    val folderName: String,     // Immediate directory name or "Storage"
    var playCount: Int = 0,
    var lastPlayed: Long = 0L
)
