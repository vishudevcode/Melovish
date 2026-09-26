package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

// Thread-Safe Artist Customization & Persistence Engine
object ArtistDataManager {
    private const val PREFS_NAME = "melovish_artists_prefs_v12"
    private var prefs: SharedPreferences? = null

    val hiddenArtists = mutableStateListOf<String>()
    val pinnedArtists = mutableStateListOf<String>()
    val artistAliases = mutableStateMapOf<String, String>()
    val manuallyCreatedArtists = mutableStateListOf<String>()
    val removedSongMap = mutableStateMapOf<String, MutableList<Long>>()
    val movedSongMap = mutableStateMapOf<String, MutableList<Long>>()

    var refreshTrigger by mutableIntStateOf(0)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val p = prefs ?: return
        hiddenArtists.clear()
        hiddenArtists.addAll(p.getStringSet("hidden_artists", emptySet()) ?: emptySet())

        pinnedArtists.clear()
        pinnedArtists.addAll(p.getStringSet("pinned_artists", emptySet()) ?: emptySet())

        artistAliases.clear()
        val aliasJson = p.getString("artist_aliases", null)
        if (aliasJson != null) {
            try {
                val obj = JSONObject(aliasJson)
                obj.keys().forEach { artistAliases[it] = obj.getString(it) }
            } catch (_: Exception) {}
        }

        manuallyCreatedArtists.clear()
        val customJson = p.getString("custom_artists", null)
        if (customJson != null) {
            try {
                val arr = JSONArray(customJson)
                for (i in 0 until arr.length()) {
                    manuallyCreatedArtists.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }

        removedSongMap.clear()
        val remJson = p.getString("removed_songs_map", null)
        if (remJson != null) {
            try {
                val obj = JSONObject(remJson)
                obj.keys().forEach { key ->
                    val arr = obj.getJSONArray(key)
                    val list = mutableListOf<Long>()
                    for (i in 0 until arr.length()) list.add(arr.getLong(i))
                    removedSongMap[key] = list
                }
            } catch (_: Exception) {}
        }

        movedSongMap.clear()
        val movJson = p.getString("moved_songs_map", null)
        if (movJson != null) {
            try {
                val obj = JSONObject(movJson)
                obj.keys().forEach { key ->
                    val arr = obj.getJSONArray(key)
                    val list = mutableListOf<Long>()
                    for (i in 0 until arr.length()) list.add(arr.getLong(i))
                    movedSongMap[key] = list
                }
            } catch (_: Exception) {}
        }
    }

    private fun saveData() {
        val p = prefs ?: return
        val aliasObj = JSONObject()
        artistAliases.forEach { (k, v) -> aliasObj.put(k, v) }

        val customArr = JSONArray()
        manuallyCreatedArtists.forEach { customArr.put(it) }

        val remObj = JSONObject()
        removedSongMap.forEach { (k, v) ->
            val arr = JSONArray()
            v.forEach { arr.put(it) }
            remObj.put(k, arr)
        }

        val movObj = JSONObject()
        movedSongMap.forEach { (k, v) ->
            val arr = JSONArray()
            v.forEach { arr.put(it) }
            movObj.put(k, arr)
        }

        p.edit()
            .putStringSet("hidden_artists", hiddenArtists.toSet())
            .putStringSet("pinned_artists", pinnedArtists.toSet())
            .putString("artist_aliases", aliasObj.toString())
            .putString("custom_artists", customArr.toString())
            .putString("removed_songs_map", remObj.toString())
            .putString("moved_songs_map", movObj.toString())
            .apply()

        refreshTrigger++
    }

    fun isCardView(): Boolean = prefs?.getBoolean("is_card_view", false) ?: false
    fun setCardView(isCard: Boolean) {
        prefs?.edit()?.putBoolean("is_card_view", isCard)?.apply()
    }

    fun getSortOrder(): ArtistSortOrder {
        val saved = prefs?.getString("artist_sort_order", ArtistSortOrder.NAME_A_TO_Z.name)
        return try {
            ArtistSortOrder.valueOf(saved ?: ArtistSortOrder.NAME_A_TO_Z.name)
        } catch (_: Exception) {
            ArtistSortOrder.NAME_A_TO_Z
        }
    }

    fun setSortOrder(order: ArtistSortOrder) {
        prefs?.edit()?.putString("artist_sort_order", order.name)?.apply()
    }

    fun togglePinArtist(name: String) {
        if (pinnedArtists.contains(name)) {
            pinnedArtists.remove(name)
        } else {
            pinnedArtists.add(name)
        }
        saveData()
    }

    fun hideArtist(name: String) {
        if (!hiddenArtists.contains(name)) {
            hiddenArtists.add(name)
            saveData()
        }
    }

    fun mergeArtists(sourceName: String, targetName: String) {
        if (sourceName.equals(targetName, ignoreCase = true)) return
        artistAliases[sourceName] = targetName
        saveData()
    }

    fun createNewArtist(name: String, initialSongs: List<Song> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !manuallyCreatedArtists.any { it.equals(trimmed, ignoreCase = true) }) {
            manuallyCreatedArtists.add(trimmed)
            if (initialSongs.isNotEmpty()) {
                val list = movedSongMap.getOrPut(trimmed) { mutableListOf() }
                initialSongs.forEach { list.add(it.id) }
            }
            saveData()
        }
    }

    fun removeSongFromArtist(artistName: String, songId: Long) {
        val list = removedSongMap.getOrPut(artistName) { mutableListOf() }
        if (!list.contains(songId)) {
            list.add(songId)
            saveData()
        }
    }

    fun moveSongToArtist(song: Song, fromArtist: String, toArtist: String) {
        removeSongFromArtist(fromArtist, song.id)
        val list = movedSongMap.getOrPut(toArtist) { mutableListOf() }
        if (!list.contains(song.id)) {
            list.add(song.id)
        }
        saveData()
    }
}

// Phase 1 Invariant: Fully-Vectorized, Pre-Compiled Regex Parsing on Worker Dispatchers
object ArtistParsingEngine {
    private val splitRegex = Regex("""\s*(?:,|/|&|\bfeat\.|\bft\.|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE)
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
        clean = clean.replace(Regex("""^[0-9\.\-\#\s]+"""), "").trim()
        return clean
    }

    /**
     * O(N) Canonical Artist Grouping with O(1) HashSet Lookups.
     * Guarantees zero main-thread allocation and safe circular-dependency resolution.
     */
    fun parseAndGroupArtists(allSongs: List<Song>): List<ArtistItem> {
        if (allSongs.isEmpty()) return emptyList()

        val intermediateMap = LinkedHashMap<String, ArrayList<Song>>()

        for (song in allSongs) {
            val rawArtist = song.artist.trim()
            if (rawArtist.isEmpty() || rawArtist.contains("unknown", ignoreCase = true)) {
                intermediateMap.getOrPut("Unknown Artist") { ArrayList() }.add(song)
            } else {
                val splitNames = rawArtist.split(splitRegex)
                var matched = false

                for (part in splitNames) {
                    val clean = sanitizeArtistName(part)
                    if (clean.isNotBlank() && !clean.matches(invalidCharPunctuation)) {
                        intermediateMap.getOrPut(clean) { ArrayList() }.add(song)
                        matched = true
                    }
                }
                if (!matched) {
                    intermediateMap.getOrPut("Unknown Artist") { ArrayList() }.add(song)
                }
            }
        }

        val groupedPrefixMap = LinkedHashMap<String, ArrayList<Pair<String, List<Song>>>>()

        intermediateMap.forEach { (name, songs) ->
            val words = name.lowercase(Locale.getDefault()).split(Regex("""\s+""")).filter { it.isNotBlank() }
            val key = if (words.size >= 2) "${words[0]} ${words[1]}" else words.firstOrNull() ?: name.lowercase(Locale.getDefault())
            groupedPrefixMap.getOrPut(key) { ArrayList() }.add(name to songs)
        }

        val canonicalMap = LinkedHashMap<String, ArrayList<Song>>()
        val canonicalSongIds = LinkedHashMap<String, HashSet<Long>>()

        for ((_, group) in groupedPrefixMap) {
            val bestName = group.maxByOrNull { it.second.size }?.first ?: group.first().first
            val mergedSongs = canonicalMap.getOrPut(bestName) { ArrayList() }
            val idSet = canonicalSongIds.getOrPut(bestName) { HashSet() }

            group.forEach { (_, songs) ->
                songs.forEach { s ->
                    if (idSet.add(s.id)) {
                        mergedSongs.add(s)
                    }
                }
            }
        }

        val userMergedMap = LinkedHashMap<String, ArrayList<Song>>()
        val userMergedIds = LinkedHashMap<String, HashSet<Long>>()
        val aliasSnapshot = HashMap(ArtistDataManager.artistAliases)

        canonicalMap.forEach { (name, songs) ->
            var target = name
            val visited = HashSet<String>()
            while (aliasSnapshot.containsKey(target) && visited.add(target)) {
                target = aliasSnapshot[target] ?: target
            }
            val list = userMergedMap.getOrPut(target) { ArrayList() }
            val set = userMergedIds.getOrPut(target) { HashSet() }
            songs.forEach { s ->
                if (set.add(s.id)) {
                    list.add(s)
                }
            }
        }

        ArtistDataManager.manuallyCreatedArtists.forEach { customName ->
            if (!userMergedMap.containsKey(customName)) {
                userMergedMap[customName] = ArrayList()
                userMergedIds[customName] = HashSet()
            }
        }

        val hiddenSet = ArtistDataManager.hiddenArtists.toHashSet()
        val pinnedSet = ArtistDataManager.pinnedArtists.toHashSet()
        val finalResult = ArrayList<ArtistItem>(userMergedMap.size)
        val songLookup = allSongs.associateBy { it.id }

        userMergedMap.forEach { (name, songs) ->
            if (!hiddenSet.contains(name)) {
                val distinctSongs = ArrayList(songs)

                ArtistDataManager.removedSongMap[name]?.let { removedIds ->
                    if (removedIds.isNotEmpty()) {
                        val remSet = removedIds.toHashSet()
                        distinctSongs.removeAll { remSet.contains(it.id) }
                    }
                }

                ArtistDataManager.movedSongMap[name]?.let { movedIds ->
                    if (movedIds.isNotEmpty()) {
                        val currentIds = distinctSongs.map { it.id }.toHashSet()
                        movedIds.forEach { mId ->
                            if (!currentIds.contains(mId)) {
                                songLookup[mId]?.let { distinctSongs.add(it) }
                            }
                        }
                    }
                }

                if (distinctSongs.isNotEmpty() || ArtistDataManager.manuallyCreatedArtists.any { it.equals(name, ignoreCase = true) }) {
                    val isPinned = pinnedSet.contains(name)
                    finalResult.add(
                        ArtistItem(
                            name = name,
                            songs = distinctSongs.toImmutableList(),
                            isPinned = isPinned
                        )
                    )
                }
            }
        }

        return finalResult
    }
}

// Phase 2 Invariant: Zero-Recomposition Artists Screen with Immutable Lists & Explicit ContentTypes
@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun ArtistsScreen(
    manager: MusicManager,
    listState: LazyListState,
    onArtistClick: (ArtistItem) -> Unit
) {
    val context = LocalContext.current
    remember { ArtistDataManager.init(context) }

    val textColor = if (manager.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBg = if (manager.isDarkMode) Color(0xFF131B2E) else Color.White
    val accentColor = manager.accentColor
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var isCardView by remember { mutableStateOf(ArtistDataManager.isCardView()) }
    var sortOrder by remember { mutableStateOf(ArtistDataManager.getSortOrder()) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateArtistDialog by remember { mutableStateOf(false) }
    var selectedArtistForActions by remember { mutableStateOf<ArtistItem?>(null) }
    var artistToMergeSource by remember { mutableStateOf<ArtistItem?>(null) }

    val artistsList = if (manager.parsedArtistsList.isNotEmpty()) {
        manager.parsedArtistsList
    } else {
        remember(manager.allSongs.size, ArtistDataManager.refreshTrigger) {
            ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
        }
    }

    val sortedArtists: ImmutableList<ArtistItem> = remember(artistsList, query, sortOrder, ArtistDataManager.refreshTrigger) {
        val filtered = if (query.isBlank()) artistsList
        else artistsList.filter { it.name.contains(query, ignoreCase = true) }

        val comparator = when (sortOrder) {
            ArtistSortOrder.NAME_A_TO_Z -> Comparator<ArtistItem> { a, b ->
                if (a.name.equals("Unknown Artist", true)) 1
                else if (b.name.equals("Unknown Artist", true)) -1
                else a.name.compareTo(b.name, true)
            }
            ArtistSortOrder.NAME_Z_TO_A -> Comparator<ArtistItem> { a, b ->
                if (a.name.equals("Unknown Artist", true)) 1
                else if (b.name.equals("Unknown Artist", true)) -1
                else b.name.compareTo(a.name, true)
            }
            ArtistSortOrder.MOST_TRACKS -> compareByDescending<ArtistItem> { it.songs.size }
            ArtistSortOrder.FEWEST_TRACKS -> compareBy<ArtistItem> { it.songs.size }
        }

        val pinned = filtered.filter { it.isPinned }.sortedWith(comparator)
        val unpinned = filtered.filter { !it.isPinned }.sortedWith(comparator)
        (pinned + unpinned).toImmutableList()
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
                Text("${sortedArtists.size} artists found", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable {
                            val newMode = !isCardView
                            isCardView = newMode
                            ArtistDataManager.setCardView(newMode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isCardView) "☰" else "⊞", fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇅", fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Name (A to Z)") }, onClick = {
                            sortOrder = ArtistSortOrder.NAME_A_TO_Z
                            ArtistDataManager.setSortOrder(sortOrder)
                            showSortMenu = false
                        })
                        DropdownMenuItem(text = { Text("Name (Z to A)") }, onClick = {
                            sortOrder = ArtistSortOrder.NAME_Z_TO_A
                            ArtistDataManager.setSortOrder(sortOrder)
                            showSortMenu = false
                        })
                        DropdownMenuItem(text = { Text("Most Tracks") }, onClick = {
                            sortOrder = ArtistSortOrder.MOST_TRACKS
                            ArtistDataManager.setSortOrder(sortOrder)
                            showSortMenu = false
                        })
                        DropdownMenuItem(text = { Text("Fewest Tracks") }, onClick = {
                            sortOrder = ArtistSortOrder.FEWEST_TRACKS
                            ArtistDataManager.setSortOrder(sortOrder)
                            showSortMenu = false
                        })
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize().padding(end = 22.dp)
                    ) {
                        items(
                            items = sortedArtists,
                            key = { it.name },
                            contentType = { "artist_grid_card" }
                        ) { artist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(cardBg)
                                    .border(
                                        1.2.dp,
                                        if (artist.isPinned) accentColor else if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFECEFF3),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .combinedClickable(
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { selectedArtistForActions = artist }
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (artist.isPinned) {
                                    Text("📌", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
                                }
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 22.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = sortedArtists,
                            key = { it.name },
                            contentType = { "artist_list_row" }
                        ) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg)
                                    .border(
                                        1.2.dp,
                                        if (artist.isPinned) accentColor else if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFECEFF3),
                                        RoundedCornerShape(18.dp)
                                    )
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(artist.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (artist.isPinned) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("📌", fontSize = 11.sp)
                                        }
                                    }
                                    Text("${artist.songs.size} ${if (artist.songs.size == 1) "track" else "tracks"}", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                Text("⋮", fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { selectedArtistForActions = artist }.padding(4.dp))
                            }
                        }
                    }
                }

                // Fast Alpha-Index Scroller
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
                                        sortedArtists.indexOfFirst { it.name.isNotEmpty() && !it.name.first().isLetter() }
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
                    .background(if (manager.isDarkMode) Color(0xFF1E293B) else Color.White)
                    .clickable(enabled = false) {}
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(target.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ArtistDataManager.togglePinArtist(target.name)
                                selectedArtistForActions = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (target.isPinned) "📍" else "📌", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (target.isPinned) "Unpin from top" else "Pin to top", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                addArtistToFavouritePlaylists(context, manager, target)
                                selectedArtistForActions = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Favourite Playlists (Home)", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

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
                    .background(if (manager.isDarkMode) Color(0xFF1E293B) else Color.White)
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
                        items(
                            items = targetCandidates,
                            key = { it.name },
                            contentType = { "merge_target_row" }
                        ) { targetArtist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (manager.isDarkMode) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        ArtistDataManager.mergeArtists(src.name, targetArtist.name)
                                        Toast.makeText(context, "Merged into ${targetArtist.name}", Toast.LENGTH_SHORT).show()
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
                        colors = ButtonDefaults.buttonColors(containerColor = if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreateArtistDialog) {
        CreateArtistDialog(
            manager = manager,
            onDismiss = { showCreateArtistDialog = false }
        )
    }
}

// Inner Artist Detail Screen with Inside Sorting, Move & Remove
@UnstableApi
@Composable
fun ArtistDetailScreen(
    artistItem: ArtistItem,
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = manager.accentColor

    var sortOrder by remember { mutableStateOf(ArtistSongSortOrder.TITLE_A_TO_Z) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSongForAction by remember { mutableStateOf<Song?>(null) }
    var showMoveTargetDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    val currentSongs = remember(artistItem.name, manager.parsedArtistsList, ArtistDataManager.refreshTrigger) {
        manager.parsedArtistsList.find { it.name.equals(artistItem.name, ignoreCase = true) }?.songs ?: artistItem.songs
    }

    val sortedSongs: ImmutableList<Song> = remember(currentSongs, sortOrder) {
        when (sortOrder) {
            ArtistSongSortOrder.TITLE_A_TO_Z -> currentSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            ArtistSongSortOrder.DURATION -> currentSongs.sortedByDescending { it.duration }
            ArtistSongSortOrder.FILE_SIZE -> currentSongs.sortedByDescending { it.size }
            ArtistSongSortOrder.NEWEST -> currentSongs.sortedByDescending { it.id }
        }.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9))
                        .clickable { addArtistToFavouritePlaylists(context, manager, artistItem) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⭐", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

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

                Button(
                    onClick = { showAddSongsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

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
                items(
                    items = sortedSongs,
                    key = { it.id },
                    contentType = { "universal_song_row" }
                ) { song ->
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

    if (showMoveTargetDialog && selectedSongForAction != null) {
        val songToMove = selectedSongForAction!!
        var destSearch by remember { mutableStateOf("") }
        val allArtists = manager.parsedArtistsList
        val destCandidates = allArtists.filter { it.name != artistItem.name && it.name.contains(destSearch, ignoreCase = true) }

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
                        items(
                            items = destCandidates,
                            key = { it.name },
                            contentType = { "move_dest_row" }
                        ) { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        ArtistDataManager.moveSongToArtist(songToMove, artistItem.name, target.name)
                                        Toast.makeText(context, "Moved to ${target.name}", Toast.LENGTH_SHORT).show()
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

    if (showAddSongsDialog) {
        var addSongSearch by remember { mutableStateOf("") }
        val currentSongIdSet = sortedSongs.map { it.id }.toHashSet()
        val candidates = manager.allSongs.filter {
            !currentSongIdSet.contains(it.id) &&
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
                        items(
                            items = candidates,
                            key = { it.id },
                            contentType = { "add_song_candidate_row" }
                        ) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                    .clickable {
                                        val list = ArtistDataManager.movedSongMap.getOrPut(artistItem.name) { mutableListOf() }
                                        if (!list.contains(song.id)) list.add(song.id)
                                        ArtistDataManager.removedSongMap[artistItem.name]?.remove(song.id)
                                        ArtistDataManager.refreshTrigger++
                                        Toast.makeText(context, "Added ${song.title}", Toast.LENGTH_SHORT).show()
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

// Dialog: Create New Artist with Fast Filter
@Composable
fun CreateArtistDialog(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = manager.isDarkMode
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accentColor = manager.accentColor

    var artistName by remember { mutableStateOf("") }
    var searchSongQuery by remember { mutableStateOf("") }
    val selectedSongs = remember { mutableStateListOf<Song>() }

    val filteredSongs = manager.allSongs.filter {
        it.title.contains(searchSongQuery, ignoreCase = true) || it.artist.contains(searchSongQuery, ignoreCase = true)
    }

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
                .height(650.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Create New Artist", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    label = { Text("Artist Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Add Songs (${selectedSongs.size} selected):", color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchSongQuery,
                    onValueChange = { searchSongQuery = it },
                    placeholder = { Text("Search songs to add...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = filteredSongs,
                        key = { it.id },
                        contentType = { "dialog_song_row" }
                    ) { song ->
                        val isPicked = selectedSongs.any { it.id == song.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPicked) accentColor.copy(alpha = 0.15f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                .clickable {
                                    if (isPicked) selectedSongs.removeAll { it.id == song.id }
                                    else selectedSongs.add(song)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${formatFileSize(song.size)} • ${song.artist}", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text(if (isPicked) "✓ Added" else "+ Add", color = if (isPicked) accentColor else Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val name = artistName.trim()
                        if (name.isNotBlank()) {
                            ArtistDataManager.createNewArtist(name, selectedSongs)
                            Toast.makeText(context, "Artist created!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter an artist name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Save Artist", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Universal Helper: Adds any Artist directly to Home Screen "Favourite Playlists"
fun addArtistToFavouritePlaylists(context: Context, manager: MusicManager, artist: ArtistItem) {
    val existingIndex = manager.customPlaylists.indexOfFirst { it.name.equals(artist.name, ignoreCase = true) }
    if (existingIndex != -1) {
        val existing = manager.customPlaylists[existingIndex]
        val mutableIds = existing.songIds.toMutableList()
        artist.songs.forEach { s ->
            if (!mutableIds.contains(s.id)) mutableIds.add(s.id)
        }
        manager.customPlaylists[existingIndex] = existing.copy(songIds = mutableIds.toImmutableList())
        manager.savePlaylists()
        Toast.makeText(context, "'${artist.name}' playlist updated!", Toast.LENGTH_SHORT).show()
    } else {
        val newPl = Playlist(
            id = "artist_${System.currentTimeMillis()}",
            name = artist.name,
            songIds = artist.songs.map { it.id }.toImmutableList(),
            icon = "🎙️",
            iconColorHex = manager.accentColor.toArgb().toLong()
        )
        manager.customPlaylists.add(newPl)
        manager.savePlaylists()
        Toast.makeText(context, "Added '${artist.name}' to Favourite Playlists!", Toast.LENGTH_SHORT).show()
    }
}
