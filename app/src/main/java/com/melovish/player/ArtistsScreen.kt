package com.melovish.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ArtistItem(
    val name: String,
    val songs: List<Song>
)

object ArtistParsingEngine {
    private val splitRegex = Regex("""\s*(?:,|/|&|\bfeat\.|\bft\.|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE)

    fun parseAndGroupArtists(songs: List<Song>): List<ArtistItem> {
        val artistMap = mutableMapOf<String, MutableList<Song>>()

        for (song in songs) {
            val rawArtist = song.artist.trim()
            if (rawArtist.isEmpty() || rawArtist.contains("unknown", ignoreCase = true)) {
                artistMap.getOrPut("Unknown Artist") { mutableListOf() }.add(song)
            } else {
                val artists = rawArtist.split(splitRegex)
                    .map { it.trim().trim('(', ')', '[', ']') }
                    .filter { it.isNotBlank() }

                if (artists.isEmpty()) {
                    artistMap.getOrPut("Unknown Artist") { mutableListOf() }.add(song)
                } else {
                    for (art in artists) {
                        artistMap.getOrPut(art) { mutableListOf() }.add(song)
                    }
                }
            }
        }

        return artistMap.entries
            .map { ArtistItem(name = it.key, songs = it.value.distinctBy { s -> s.id }) }
            .sortedWith { a, b ->
                if (a.name.equals("Unknown Artist", ignoreCase = true)) 1
                else if (b.name.equals("Unknown Artist", ignoreCase = true)) -1
                else a.name.compareTo(b.name, ignoreCase = true)
            }
    }
}

@Composable
fun ArtistsScreen(
    artistsList: List<ArtistItem>,
    accentColor: Color,
    isDark: Boolean,
    listState: LazyListState,
    onArtistClick: (ArtistItem) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    var query by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val filteredArtists = remember(query, artistsList.size) {
        if (query.isBlank()) artistsList
        else artistsList.filter { it.name.contains(query, ignoreCase = true) }
    }

    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Artists", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("${artistsList.size} artists discovered", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search artists...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredArtists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No artists found.", color = Color(0xFF64748B))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 22.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredArtists, key = { it.name }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cardBg)
                                .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                                .clickable { onArtistClick(artist) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎙️", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(artist.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${artist.songs.size} ${if (artist.songs.size == 1) "track" else "tracks"}", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                            Text("›", color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    alphabet.forEach { char ->
                        Text(
                            text = char.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val targetIndex = if (char == '#') {
                                        filteredArtists.indexOfFirst { !it.name.first().isLetter() }
                                    } else {
                                        filteredArtists.indexOfFirst { it.name.startsWith(char, ignoreCase = true) }
                                    }
                                    if (targetIndex != -1) {
                                        coroutineScope.launch {
                                            listState.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artistItem: ArtistItem,
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor
    val songs = artistItem.songs

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(artistItem.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${songs.size} tracks by artist", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row {
                Button(
                    onClick = {
                        val shuffled = songs.shuffled()
                        if (shuffled.isNotEmpty()) manager.playSong(shuffled.first(), shuffled, artistItem.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔀 Shuffle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) manager.playSong(songs.first(), songs, artistItem.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("▶ Play", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                UniversalSongRow(
                    song = song,
                    manager = manager,
                    isDark = isDark,
                    onPlay = { manager.playSong(song, songs, artistItem.name) },
                    onMenuClick = { onSongMenuClick(song) }
                )
            }
        }
    }
}
