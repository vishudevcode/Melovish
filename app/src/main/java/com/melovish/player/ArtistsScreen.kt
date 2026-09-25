package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

// Data Models
data class ArtistItem(
    val name: String,
    val songs: MutableList<Song> = mutableListOf()
)

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

// Artist Customization & Persistence Controller
object ArtistDataManager {
    private const val PREFS_NAME = "melovish_artists_prefs"
    private var prefs: SharedPreferences? = null

    val hiddenArtists = mutableStateListOf<String>()
    val artistAliases = mutableStateMapOf<String, String>() // sourceArtist -> targetArtist
    val manuallyCreatedArtists = mutableStateListOf<ArtistItem>()
    val removedSongMap = mutableStateMapOf<String, MutableList<Long>>() // artistName -> list of removed song IDs
    val movedSongMap = mutableStateMapOf<String, MutableList<Long>>() // artistName -> list of added song IDs

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val p = prefs ?: return
        hiddenArtists.clear()
        hiddenArtists.addAll(p.getStringSet("hidden_artists", emptySet()) ?: emptySet())

        artistAliases.clear()
        val aliasJson = p.getString("artist_aliases", null)
        if (aliasJson != null) {
            try {
                val obj = JSONObject(aliasJson)
                obj.keys().forEach { artistAliases[it] = obj.getString(it) }
            } catch (_: Exception) {}
        }

        val customJson = p.getString("custom_artists", null)
        manuallyCreatedArtists.clear()
        if (customJson != null) {
            try {
                val arr = JSONArray(customJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("name")
                    manuallyCreatedArtists.add(ArtistItem(name, mutableListOf()))
                }
            } catch (_: Exception) {}
        }
    }

    private fun saveData() {
        val p = prefs ?: return
        val aliasObj = JSONObject()
        artistAliases.forEach { (k, v) -> aliasObj.put(k, v) }

        val customArr = JSONArray()
        manuallyCreatedArtists.forEach {
            customArr.put(JSONObject().put("name", it.name))
        }

        p.edit()
            .putStringSet("hidden_artists", hiddenArtists.toSet())
            .putString("artist_aliases", aliasObj.toString())
            .putString("custom_artists", customArr.toString())
            .apply()
    }

    fun hideArtist(name: String) {
        if (!hiddenArtists.contains(name)) {
            hiddenArtists.add(name)
            saveData()
        }
    }

    fun unhideArtist(name: String) {
        hiddenArtists.remove(name)
        saveData()
    }

    fun mergeArtists(sourceName: String, targetName: String) {
        if (sourceName.equals(targetName, ignoreCase = true)) return
        artistAliases[sourceName] = targetName
        saveData()
    }

    fun createNewArtist(name: String, initialSongs: List<Song> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && manuallyCreatedArtists.none { it.name.equals(trimmed, ignoreCase = true) }) {
            val item = ArtistItem(trimmed, initialSongs.toMutableList())
            manuallyCreatedArtists.add(item)
            val list = movedSongMap.getOrPut(trimmed) { mutableListOf() }
            initialSongs.forEach { list.add(it.id) }
            saveData()
        }
    }

    fun removeSongFromArtist(artistName: String, songId: Long) {
        val list = removedSongMap.getOrPut(artistName) { mutableListOf() }
        if (!list.contains(songId)) list.add(songId)
    }

    fun moveSongToArtist(song: Song, fromArtist: String, toArtist: String) {
        removeSongFromArtist(fromArtist, song.id)
        val list = movedSongMap.getOrPut(toArtist) { mutableListOf() }
        if (!list.contains(song.id)) list.add(song.id)
    }
}

// Standalone Multi-Artist Parser, Cleaner, and Grouping Engine
object ArtistParsingEngine {
    private val splitRegex = Regex("""\s*(?:,|/|&|\bfeat\.|\bft\.|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE)

    // Regex patterns to strip web promotions, junk domains, and HTML entities
    private val promoWebsitesRegex = Regex(
        """(?i)\s*[\(\[\-–]\s*(?:pagalworld|ghantalele|djmaza|songsmp3|mp3mad|mirchifun|hungama|wynk|jiosaavn|gaana|pendujatt|mr-jatt|naasongs)[\w\.\-]*\s*[\)\]]?"""
    )
    private val domainExtensionsRegex = Regex("""(?i)\b\w+\.(?:com|co|tv|pw|click|info|cool|in|org|net|xyz|me|club|top)\b""")
    private val htmlEntitiesRegex = Regex("""(?i)&?#\d+;?""")
    private val invalidCharPunctuation = Regex("""^[\.\,\#\-\_\:\;\/\s\d]+$""")

    fun sanitizeArtistName(raw: String): String {
        var clean = raw
        clean = htmlEntitiesRegex.replace(clean, " ")
        clean = promoWebsitesRegex.replace(clean, "")
        clean = domainExtensionsRegex.replace(clean, "")
        clean = clean.replace(Regex("""[\(\[\{\}\]\)]"""), "")
        clean = clean.trim()

        // Strip leading dots, numbers, or dashes (e.g. ". Leo" -> "Leo")
        clean = clean.replace(Regex("""^[0-9\.\-\#\s]+"""), "").trim()
        return clean
    }

    fun parseAndGroupArtists(allSongs: List<Song>): List<ArtistItem> {
        val intermediateMap = mutableMapOf<String, MutableList<Song>>()

        // 1. Split raw metadata and sanitize
        for (song in allSongs) {
            val rawArtist = song.artist.trim()
            if (rawArtist.isEmpty() || rawArtist.contains("unknown", ignoreCase = true)) {
                intermediateMap.getOrPut("Unknown Artist") { mutableListOf() }.add(song)
            } else {
                val splitNames = rawArtist.split(splitRegex)
                var matched = false

                for (part in splitNames) {
                    val clean = sanitizeArtistName(part)
                    // Discard purely numbers, symbols, or empty results (e.g. "#", "0", ". ")
                    if (clean.isNotBlank() && !clean.matches(invalidCharPunctuation)) {
                        intermediateMap.getOrPut(clean) { mutableListOf() }.add(song)
                        matched = true
                    }
                }
                if (!matched) {
                    intermediateMap.getOrPut("Unknown Artist") { mutableListOf() }.add(song)
                }
            }
        }

        // 2. Automated Prefix Merging (if the first two words match, merge into the one with more tracks)
        val groupedPrefixMap = mutableMapOf<String, MutableList<Pair<String, List<Song>>>>()

        intermediateMap.forEach { (name, songs) ->
            val words = name.lowercase(Locale.getDefault()).split(Regex("""\s+""")).filter { it.isNotBlank() }
            val key = if (words.size >= 2) "${words[0]} ${words[1]}" else words.firstOrNull() ?: name.lowercase()
            groupedPrefixMap.getOrPut(key) { mutableListOf() }.add(name to songs)
        }

        val canonicalMap = mutableMapOf<String, MutableList<Song>>()

        for ((_, group) in groupedPrefixMap) {
            // Sort by count descending so the name holding the most tracks becomes the primary entity
            val bestName = group.maxByOrNull { it.second.size }?.first ?: group.first().first
            val mergedSongs = canonicalMap.getOrPut(bestName) { mutableListOf() }
            group.forEach { (_, songs) ->
                songs.forEach { s ->
                    if (mergedSongs.none { it.id == s.id }) mergedSongs.add(s)
                }
            }
        }

        // 3. Apply User Manual Merges (Aliases)
        val userMergedMap = mutableMapOf<String, MutableList<Song>>()
        canonicalMap.forEach { (name, songs) ->
            val target = ArtistDataManager.artistAliases[name] ?: name
            userMergedMap.getOrPut(target) { mutableListOf() }.addAll(songs)
        }

        // 4. Inject Manually Created Artists
        ArtistDataManager.manuallyCreatedArtists.forEach { custom ->
            if (!userMergedMap.containsKey(custom.name)) {
                userMergedMap[custom.name] = mutableListOf()
            }
        }

        // 5. Apply Song Moves & Removals
        val finalResult = mutableListOf<ArtistItem>()

        userMergedMap.forEach { (name, songs) ->
            if (!ArtistDataManager.hiddenArtists.contains(name)) {
                val distinctSongs = songs.distinctBy { it.id }.toMutableList()

                // Apply removals
                ArtistDataManager.removedSongMap[name]?.let { removedIds ->
                    distinctSongs.removeAll { removedIds.contains(it.id) }
                }

                // Apply additions / moves
                ArtistDataManager.movedSongMap[name]?.let { movedIds ->
                    val added = allSongs.filter { movedIds.contains(it.id) && distinctSongs.none { d -> d.id == it.id } }
                    distinctSongs.addAll(added)
                }

                if (distinctSongs.isNotEmpty() || ArtistDataManager.manuallyCreatedArtists.any { it.name.equals(name, ignoreCase = true) }) {
                    finalResult.add(ArtistItem(name = name, songs = distinctSongs))
                }
            }
        }

        return finalResult
    }
}

// Main Artists Screen with Cards/Lines Switcher & Group Management
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsScreen(
    artistsList: List<ArtistItem>,
    accentColor: Color,
    isDark: Boolean,
    listState: LazyListState,
    onArtistClick: (ArtistItem) -> Unit
) {
    val context = LocalContext.current
    ArtistDataManager.init(context)

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var isCardView by remember { mutableStateOf(false) } // Lines view vs Card view
    var sortOrder by remember { mutableStateOf(ArtistSortOrder.NAME_A_TO_Z) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateArtistDialog by remember { mutableStateOf(false) }
    var selectedArtistForActions by remember { mutableStateOf<ArtistItem?>(null) }
    var artistToMergeSource by remember { mutableStateOf<ArtistItem?>(null) }

    // Filter and Sort
    val sortedArtists = remember(artistsList, query, sortOrder, ArtistDataManager.hiddenArtists.size, ArtistDataManager.artistAliases.size) {
        var list = if (query.isBlank()) artistsList
        else artistsList.filter { it.name.contains(query, ignoreCase = true) }

        when (sortOrder) {
            ArtistSortOrder.NAME_A_TO_Z -> list.sortedWith { a, b ->
                if (a.name.equals("Unknown Artist", true)) 1
                else if (b.name.equals("Unknown Artist", true)) -1
                else a.name.compareTo(b.name, true)
            }
            ArtistSortOrder.NAME_Z_TO_A -> list.sortedWith { a, b ->
                if (a.name.equals("Unknown Artist", true)) 1
                else if (b.name.equals("Unknown Artist", true)) -1
                else b.name.compareTo(a.name, true)
            }
            ArtistSortOrder.MOST_TRACKS -> list.sortedByDescending { it.songs.size }
            ArtistSortOrder.FEWEST_TRACKS -> list.sortedBy { it.songs.size }
        }
    }

    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Artists", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("${sortedArtists.size} artists found", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // View Switcher (Cards vs Lines)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable { isCardView = !isCardView },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isCardView) "☰" else "⊞", fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Name (A to Z)") }, onClick = { sortOrder = ArtistSortOrder.NAME_A_TO_Z; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Name (Z to A)") }, onClick = { sortOrder = ArtistSortOrder.NAME_Z_TO_A; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Most Tracks") }, onClick = { sortOrder = ArtistSortOrder.MOST_TRACKS; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Fewest Tracks") }, onClick = { sortOrder = ArtistSortOrder.FEWEST_TRACKS; showSortMenu = false })
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // New Artist Button
                Button(
                    onClick = { showCreateArtistDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search artists...", color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (sortedArtists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No artists found.", color = Color(0xFF64748B))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isCardView) {
                    // Card / Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize().padding(end = 22.dp)
                    ) {
                        items(sortedArtists, key = { it.name }) { artist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { selectedArtistForActions = artist }
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎙️", fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        artist.name,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${artist.songs.size} tracks",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Lines / List View
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 22.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedArtists, key = { it.name }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFECEFF3), RoundedCornerShape(18.dp))
                                    .combinedClickable(
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { selectedArtistForActions = artist }
                                    )
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
                                Text("⋮", fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { selectedArtistForActions = artist }.padding(4.dp))
                            }
                        }
                    }
                }

                // Fast-Scroll Alphabet Scroller
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
                                        sortedArtists.indexOfFirst { !it.name.first().isLetter() }
                                    } else {
                                        sortedArtists.indexOfFirst { it.name.startsWith(char, ignoreCase = true) }
                                    }
                                    if (targetIndex != -1) {
                                        coroutineScope.launch {
                                            if (!isCardView) listState.scrollToItem(targetIndex)
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

    // Artist Actions Sheet (Hide Artist / Merge Artist)
    if (selectedArtistForActions != null) {
        val target = selectedArtistForActions!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    selectedArtistForActions = null
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                artistToMergeSource = target
                                selectedArtistForActions = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔀", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Merge into another artist...", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ArtistDataManager.hideArtist(target.name)
                                selectedArtistForActions = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚫", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Hide artist group", fontSize = 15.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { selectedArtistForActions = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Merge Picker Dialog
    if (artistToMergeSource != null) {
        val src = artistToMergeSource!!
        var mergeSearchQuery by remember { mutableStateOf("") }
        val targetCandidates = artistsList.filter { it.name != src.name && it.name.contains(mergeSearchQuery, ignoreCase = true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    artistToMergeSource = null
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Column {
                    Text("Merge '${src.name}' into:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = mergeSearchQuery,
                        onValueChange = { mergeSearchQuery = it },
                        placeholder = { Text("Search target artist...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(targetCandidates, key = { it.name }) { targetArtist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        ArtistDataManager.mergeArtists(src.name, targetArtist.name)
                                        artistToMergeSource = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎙️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(targetArtist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { artistToMergeSource = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Create New Artist Dialog
    if (showCreateArtistDialog) {
        CreateArtistDialog(
            isDark = isDark,
            accentColor = accentColor,
            onDismiss = { showCreateArtistDialog = false }
        )
    }
}

// Inner Artist Detail Screen with Inside Sorting, Move & Remove
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

    var sortOrder by remember { mutableStateOf(ArtistSongSortOrder.TITLE_A_TO_Z) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSongForAction by remember { mutableStateOf<Song?>(null) }
    var showMoveTargetDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    val sortedSongs = remember(artistItem.songs, sortOrder, ArtistDataManager.removedSongMap[artistItem.name]?.size, ArtistDataManager.movedSongMap[artistItem.name]?.size) {
        val currentList = artistItem.songs.toMutableList()
        when (sortOrder) {
            ArtistSongSortOrder.TITLE_A_TO_Z -> currentList.sortedBy { it.title.lowercase(Locale.getDefault()) }
            ArtistSongSortOrder.DURATION -> currentList.sortedByDescending { it.duration }
            ArtistSongSortOrder.FILE_SIZE -> currentList.sortedByDescending { it.size }
            ArtistSongSortOrder.NEWEST -> currentList.sortedByDescending { it.id }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Symmetrical Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GlassBackButton(isDark = isDark, onClick = onBack)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(artistItem.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${sortedSongs.size} tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sort Tracks
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Title (A to Z)") }, onClick = { sortOrder = ArtistSongSortOrder.TITLE_A_TO_Z; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Duration") }, onClick = { sortOrder = ArtistSongSortOrder.DURATION; showSortMenu = false })
                        DropdownMenuItem(text = { Text("File Size") }, onClick = { sortOrder = ArtistSongSortOrder.FILE_SIZE; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Newest First") }, onClick = { sortOrder = ArtistSongSortOrder.NEWEST; showSortMenu = false })
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Add Songs to Artist
                Button(
                    onClick = { showAddSongsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Shuffle
                Button(
                    onClick = {
                        val shuffled = sortedSongs.shuffled()
                        if (shuffled.isNotEmpty()) manager.playSong(shuffled.first(), shuffled, artistItem.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔀", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sortedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks available for this artist.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedSongs, key = { it.id }) { song ->
                    UniversalSongRow(
                        song = song,
                        manager = manager,
                        isDark = isDark,
                        onPlay = { manager.playSong(song, sortedSongs, artistItem.name) },
                        onMenuClick = { selectedSongForAction = song }
                    )
                }
            }
        }
    }

    // Song Move / Remove Modal
    if (selectedSongForAction != null) {
        val s = selectedSongForAction!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    selectedSongForAction = null
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showMoveTargetDialog = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("➡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Move track to another artist...", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ArtistDataManager.removeSongFromArtist(artistItem.name, s.id)
                                artistItem.songs.removeAll { it.id == s.id }
                                selectedSongForAction = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🗑️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove track from this artist", fontSize = 15.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val target = s
                                selectedSongForAction = null
                                onSongMenuClick(target)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚙️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Other song options...", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { selectedSongForAction = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Move to Artist Picker Dialog
    if (showMoveTargetDialog && selectedSongForAction != null) {
        val songToMove = selectedSongForAction!!
        var destSearch by remember { mutableStateOf("") }
        val destCandidates = manager.parsedArtistsList.filter { it.name != artistItem.name && it.name.contains(destSearch, ignoreCase = true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    showMoveTargetDialog = false
                    selectedSongForAction = null
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Column {
                    Text("Move '${songToMove.title}' to:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = destSearch,
                        onValueChange = { destSearch = it },
                        placeholder = { Text("Search target artist...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(destCandidates, key = { it.name }) { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        ArtistDataManager.moveSongToArtist(songToMove, artistItem.name, target.name)
                                        artistItem.songs.removeAll { it.id == songToMove.id }
                                        target.songs.add(songToMove)
                                        showMoveTargetDialog = false
                                        selectedSongForAction = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎙️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(target.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            showMoveTargetDialog = false
                            selectedSongForAction = null
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add Songs to this Artist Dialog
    if (showAddSongsDialog) {
        var addSongSearch by remember { mutableStateOf("") }
        val candidates = manager.allSongs.filter {
            artistItem.songs.none { existing -> existing.id == it.id } &&
            (it.title.contains(addSongSearch, ignoreCase = true) || it.artist.contains(addSongSearch, ignoreCase = true))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    showAddSongsDialog = false
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Add Songs to ${artistItem.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Button(onClick = { showAddSongsDialog = false }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = addSongSearch,
                        onValueChange = { addSongSearch = it },
                        placeholder = { Text("Search songs to add...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(candidates, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        val list = ArtistDataManager.movedSongMap.getOrPut(artistItem.name) { mutableListOf() }
                                        if (!list.contains(song.id)) list.add(song.id)
                                        artistItem.songs.add(song)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                                Text("+ Add", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Create New Artist
@Composable
fun CreateArtistDialog(
    isDark: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var artistName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column {
                Text("Create New Artist", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    label = { Text("Artist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (artistName.isNotBlank()) {
                            ArtistDataManager.createNewArtist(artistName.trim())
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Create Artist", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
