package com.melovish.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    val customArtistImages = mutableStateMapOf<String, String>()

    var refreshTrigger by mutableIntStateOf(0)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
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

        customArtistImages.clear()
        val imgJson = p.getString("custom_artist_images", null)
        if (imgJson != null) {
            try {
                val obj = JSONObject(imgJson)
                obj.keys().forEach { customArtistImages[it] = obj.getString(it) }
            } catch (_: Exception) {}
        }
    }

    fun saveData() {
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

        val imgObj = JSONObject()
        customArtistImages.forEach { (k, v) -> imgObj.put(k, v) }

        p.edit()
            .putStringSet("hidden_artists", hiddenArtists.toSet())
            .putStringSet("pinned_artists", pinnedArtists.toSet())
            .putString("artist_aliases", aliasObj.toString())
            .putString("custom_artists", customArr.toString())
            .putString("removed_songs_map", remObj.toString())
            .putString("moved_songs_map", movObj.toString())
            .putString("custom_artist_images", imgObj.toString())
            .commit()

        refreshTrigger++
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

    fun setArtistCustomImage(context: Context, artistName: String, uri: Uri) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return
            val sanitized = artistName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val targetFile = File(context.filesDir, "artist_$sanitized.jpg")
            val output = FileOutputStream(targetFile)
            input.copyTo(output)
            input.close()
            output.close()
            customArtistImages[artistName] = targetFile.absolutePath
            saveData()
        } catch (_: Exception) {}
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

    // 🚀 Robust, permanent track addition
    fun addSongToArtist(artistName: String, songId: Long) {
        removedSongMap[artistName]?.remove(songId)
        val list = movedSongMap.getOrPut(artistName) { mutableListOf() }
        if (!list.contains(songId)) {
            list.add(songId)
        }
        saveData()
    }
}

// Regex-Optimized Canonical Artist Parsing Engine
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

    fun parseAndGroupArtists(allSongs: List<Song>): List<ArtistItem> {
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

        // Guarantee manually created artists always exist in the map
        ArtistDataManager.manuallyCreatedArtists.forEach { customName ->
            var target = customName
            val visited = HashSet<String>()
            while (aliasSnapshot.containsKey(target) && visited.add(target)) {
                target = aliasSnapshot[target] ?: target
            }
            if (!userMergedMap.containsKey(target)) {
                userMergedMap[target] = ArrayList()
                userMergedIds[target] = HashSet()
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

                val isCustomCreated = ArtistDataManager.manuallyCreatedArtists.any { it.equals(name, ignoreCase = true) }
                if (distinctSongs.isNotEmpty() || isCustomCreated) {
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

// Root Artist Action Modal
@UnstableApi
@Composable
fun RootArtistActionModal(
    artist: ArtistItem,
    manager: MusicManager,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val textColor = manager.getCurrentTextColor()
    val accentColor = manager.accentColor
    var artistToMergeSource by remember { mutableStateOf<ArtistItem?>(null) }
    var artistForCustomImage by remember { mutableStateOf<ArtistItem?>(null) }

    val modalScrollInterceptor = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
        }
    }

    val dialogSurface = manager.getCurrentDialogColor()
    val glassBorder = manager.getGlassBorderBrush()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && artistForCustomImage != null) {
            ArtistDataManager.setArtistCustomImage(context, artistForCustomImage!!.name, uri)
            val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
            manager.parsedArtistsList.clear()
            manager.parsedArtistsList.addAll(updated)
            Toast.makeText(context, "Artist image updated!", Toast.LENGTH_SHORT).show()
        }
        artistForCustomImage = null
        onDismiss()
    }

    if (artistToMergeSource != null) {
        val src = artistToMergeSource!!
        var mergeSearchQuery by remember { mutableStateOf("") }
        val artistsList = manager.parsedArtistsList
        val targetCandidates = artistsList.filter { it.name != src.name && it.name.contains(mergeSearchQuery, ignoreCase = true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x77000000))
                .nestedScroll(modalScrollInterceptor)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onDismiss()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(dialogSurface)
                    .border(1.5.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = manager.getCurrentSurfaceColor(),
                            unfocusedContainerColor = manager.getCurrentSurfaceColor(),
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = manager.getCurrentBorderColor(),
                            cursorColor = accentColor
                        )
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
                                        val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                                        manager.parsedArtistsList.clear()
                                        manager.parsedArtistsList.addAll(updated)
                                        Toast.makeText(context, "Merged into ${targetArtist.name}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎙", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(targetArtist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (manager.isDarkMode) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .nestedScroll(modalScrollInterceptor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(dialogSurface)
                .border(1.5.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(artist.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            ArtistDataManager.togglePinArtist(artist.name)
                            val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                            manager.parsedArtistsList.clear()
                            manager.parsedArtistsList.addAll(updated)
                            onDismiss()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (artist.isPinned) "📍" else "📌", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Pin / Unpin from top", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            artistForCustomImage = artist
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🖼", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Change artist photo...", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            addArtistToFavouritePlaylists(context, manager, artist)
                            onDismiss()
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
                            artistToMergeSource = artist
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
                            ArtistDataManager.hideArtist(artist.name)
                            val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                            manager.parsedArtistsList.clear()
                            manager.parsedArtistsList.addAll(updated)
                            onDismiss()
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
                    onClick = onDismiss,
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

// 1:1 Dynamic Square Artist Card with Fast Hardware Image Loading
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistSquareCard(
    artist: ArtistItem,
    manager: MusicManager,
    isDark: Boolean,
    cardBg: Color,
    glassBorderBrush: Brush,
    accentColor: Color,
    textColor: Color,
    isHero: Boolean,
    gridColumns: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val customImgPath = ArtistDataManager.customArtistImages[artist.name]
    val firstSong = artist.songs.firstOrNull()
    val artUri = remember(firstSong?.id, customImgPath) {
        if (!customImgPath.isNullOrBlank()) Uri.fromFile(File(customImgPath))
        else firstSong?.let { manager.getAlbumArtUri(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width = if (artist.isPinned) 1.6.dp else 1.2.dp,
                brush = if (artist.isPinned) Brush.linearGradient(listOf(accentColor, accentColor)) else glassBorderBrush,
                shape = RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artist.isPinned) {
            Text("📌", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).zIndex(2f))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (gridColumns == 4) 24.dp else if (isHero) 32.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                val badgeFraction = when (gridColumns) {
                    2 -> if (isHero) 0.58f else 0.54f
                    3 -> 0.52f
                    else -> 0.48f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(badgeFraction)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artUri)
                            .size(140, 140)
                            .precision(Precision.INEXACT)
                            .crossfade(80)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                val badgeFraction = when (gridColumns) {
                    2 -> if (isHero) 0.56f else 0.52f
                    3 -> 0.50f
                    else -> 0.48f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(badgeFraction)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎙️",
                        fontSize = when (gridColumns) {
                            2 -> if (isHero) 32.sp else 28.sp
                            3 -> 22.sp
                            else -> 17.sp
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = if (gridColumns == 4) 6.dp else 10.dp,
                    end = if (gridColumns == 4) 6.dp else 10.dp,
                    bottom = if (gridColumns == 4) 8.dp else if (isHero) 14.dp else 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = artist.name,
                    color = textColor,
                    fontSize = when (gridColumns) {
                        2 -> if (isHero) 14.5.sp else 13.5.sp
                        3 -> 12.sp
                        else -> 10.5.sp
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!isHero && gridColumns < 4) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${artist.songs.size} tracks",
                        color = Color(0xFF64748B),
                        fontSize = if (gridColumns == 2) 10.sp else 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 📌 ARTISTS SCREEN
// =========================================================================

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun ArtistsScreen(
    manager: MusicManager,
    listState: LazyListState,
    onArtistClick: (ArtistItem) -> Unit,
    onArtistLongClick: (ArtistItem) -> Unit,
    onOpenCreateArtist: () -> Unit = {},
    onOpenGridSizeDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    remember { ArtistDataManager.init(context) }

    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val accentColor = manager.accentColor
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    val artistsList = remember(manager.allSongs.size, ArtistDataManager.refreshTrigger) {
        val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
        manager.parsedArtistsList.clear()
        manager.parsedArtistsList.addAll(updated)
        updated
    }

    val sortedArtists: ImmutableList<ArtistItem> = remember(artistsList, query, manager.artistsSortOrder, ArtistDataManager.refreshTrigger) {
        val filtered = if (query.isBlank()) artistsList
        else artistsList.filter { it.name.contains(query, ignoreCase = true) }

        val comparator = when (manager.artistsSortOrder) {
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
    var activeBubbleChar by remember { mutableStateOf<Char?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
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
                            .background(cardBg)
                            .border(1.2.dp, glassBorderBrush, CircleShape)
                            .combinedClickable(
                                onClick = { manager.cycleNextArtistsViewMode() },
                                onLongClick = onOpenGridSizeDialog
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GridViewModeVectorIcon(mode = manager.artistsViewMode, tint = accentColor, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                                .border(1.2.dp, glassBorderBrush, CircleShape)
                                .clickable { showSortMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            SortListVector(tint = accentColor, modifier = Modifier.size(19.dp))
                        }

                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Name (A to Z)") }, onClick = {
                                manager.setPersistentArtistsSort(ArtistSortOrder.NAME_A_TO_Z)
                                showSortMenu = false
                            })
                            DropdownMenuItem(text = { Text("Name (Z to A)") }, onClick = {
                                manager.setPersistentArtistsSort(ArtistSortOrder.NAME_Z_TO_A)
                                showSortMenu = false
                            })
                            DropdownMenuItem(text = { Text("Most Tracks") }, onClick = {
                                manager.setPersistentArtistsSort(ArtistSortOrder.MOST_TRACKS)
                                showSortMenu = false
                            })
                            DropdownMenuItem(text = { Text("Fewest Tracks") }, onClick = {
                                manager.setPersistentArtistsSort(ArtistSortOrder.FEWEST_TRACKS)
                                showSortMenu = false
                            })
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenCreateArtist,
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = manager.getCurrentBorderColor(),
                    cursorColor = accentColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedArtists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No artists found.", color = Color(0xFF64748B))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (manager.artistsViewMode) {
                        GridViewMode.LIST -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(end = 24.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = sortedArtists,
                                    key = { it.name },
                                    contentType = { "artist_list_row" }
                                ) { artist ->
                                    val customImg = ArtistDataManager.customArtistImages[artist.name]
                                    val firstSong = artist.songs.firstOrNull()
                                    val artUri = remember(firstSong?.id, customImg) {
                                        if (!customImg.isNullOrBlank()) Uri.fromFile(File(customImg))
                                        else firstSong?.let { manager.getAlbumArtUri(it) }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(cardBg)
                                            .border(
                                                width = if (artist.isPinned) 1.6.dp else 1.2.dp,
                                                brush = if (artist.isPinned) Brush.linearGradient(listOf(accentColor, accentColor)) else glassBorderBrush,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .combinedClickable(
                                                onClick = { onArtistClick(artist) },
                                                onLongClick = { onArtistLongClick(artist) }
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
                                            if (artUri != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(artUri)
                                                        .size(100, 100)
                                                        .precision(Precision.INEXACT)
                                                        .crossfade(80)
                                                        .build(),
                                                    contentDescription = artist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text("🎙", fontSize = 20.sp)
                                            }
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
                                        Text("⋮", fontSize = 20.sp, color = textColor, modifier = Modifier.clickable { onArtistLongClick(artist) }.padding(4.dp))
                                    }
                                }
                            }
                        }
                        GridViewMode.GRID_2 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                            ) {
                                items(
                                    items = sortedArtists,
                                    key = { it.name },
                                    contentType = { "artist_grid_card_2" }
                                ) { artist ->
                                    ArtistSquareCard(
                                        artist = artist,
                                        manager = manager,
                                        isDark = manager.isDarkMode,
                                        cardBg = cardBg,
                                        glassBorderBrush = glassBorderBrush,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        isHero = false,
                                        gridColumns = 2,
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { onArtistLongClick(artist) }
                                    )
                                }
                            }
                        }
                        GridViewMode.GRID_3 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                            ) {
                                items(
                                    items = sortedArtists,
                                    key = { it.name },
                                    contentType = { "artist_grid_card_3" }
                                ) { artist ->
                                    ArtistSquareCard(
                                        artist = artist,
                                        manager = manager,
                                        isDark = manager.isDarkMode,
                                        cardBg = cardBg,
                                        glassBorderBrush = glassBorderBrush,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        isHero = false,
                                        gridColumns = 3,
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { onArtistLongClick(artist) }
                                    )
                                }
                            }
                        }
                        GridViewMode.GRID_4 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                            ) {
                                items(
                                    items = sortedArtists,
                                    key = { it.name },
                                    contentType = { "artist_grid_card_4" }
                                ) { artist ->
                                    ArtistSquareCard(
                                        artist = artist,
                                        manager = manager,
                                        isDark = manager.isDarkMode,
                                        cardBg = cardBg,
                                        glassBorderBrush = glassBorderBrush,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        isHero = false,
                                        gridColumns = 4,
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { onArtistLongClick(artist) }
                                    )
                                }
                            }
                        }
                        GridViewMode.HERO_GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                            ) {
                                items(
                                    items = sortedArtists,
                                    key = { it.name },
                                    contentType = { "artist_grid_hero" }
                                ) { artist ->
                                    ArtistSquareCard(
                                        artist = artist,
                                        manager = manager,
                                        isDark = manager.isDarkMode,
                                        cardBg = cardBg,
                                        glassBorderBrush = glassBorderBrush,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        isHero = true,
                                        gridColumns = 2,
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { onArtistLongClick(artist) }
                                    )
                                }
                            }
                        }
                    }

                    // Alphabet Fast-Scroll Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 80.dp)
                            .pointerInput(sortedArtists) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        val total = alphabet.size
                                        val index = ((offset.y / size.height) * total).toInt().coerceIn(0, total - 1)
                                        val char = alphabet[index]
                                        activeBubbleChar = char
                                        val targetIndex = if (char == '#') {
                                            sortedArtists.indexOfFirst { it.name.isNotEmpty() && !it.name.first().isLetter() }
                                        } else {
                                            sortedArtists.indexOfFirst { it.name.startsWith(char, ignoreCase = true) }
                                        }
                                        if (targetIndex != -1) {
                                            coroutineScope.launch { listState.scrollToItem(targetIndex) }
                                        }
                                    },
                                    onDragEnd = { activeBubbleChar = null },
                                    onDragCancel = { activeBubbleChar = null },
                                    onVerticalDrag = { change, _ ->
                                        val total = alphabet.size
                                        val index = ((change.position.y / size.height) * total).toInt().coerceIn(0, total - 1)
                                        val char = alphabet[index]
                                        activeBubbleChar = char
                                        val targetIndex = if (char == '#') {
                                            sortedArtists.indexOfFirst { it.name.isNotEmpty() && !it.name.first().isLetter() }
                                        } else {
                                            sortedArtists.indexOfFirst { it.name.startsWith(char, ignoreCase = true) }
                                        }
                                        if (targetIndex != -1) {
                                            coroutineScope.launch { listState.scrollToItem(targetIndex) }
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        alphabet.forEach { char ->
                            Text(
                                text = char.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeBubbleChar == char) accentColor else Color(0xFF94A3B8),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val targetIndex = if (char == '#') {
                                            sortedArtists.indexOfFirst { it.name.isNotEmpty() && !it.name.first().isLetter() }
                                        } else {
                                            sortedArtists.indexOfFirst { it.name.startsWith(char, ignoreCase = true) }
                                        }
                                        if (targetIndex != -1) {
                                            coroutineScope.launch { listState.scrollToItem(targetIndex) }
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
                            )
                        }
                    }

                    if (activeBubbleChar != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(76.dp)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeBubbleChar.toString(),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 📌 ARTIST DETAIL SCREEN
// =========================================================================

@UnstableApi
@Composable
fun ArtistDetailScreen(
    artistItem: ArtistItem,
    manager: MusicManager,
    isDark: Boolean,
    onBack: () -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onOpenGridSizeDialog: () -> Unit = {},
    onOpenAddSongsDialog: () -> Unit = {}
) {
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val accent = manager.accentColor

    var showSortMenu by remember { mutableStateOf(false) }

    // 🚀 Reactive hook: automatically updates song list when tracks are added or moved
    val currentSongs = remember(artistItem.name, manager.parsedArtistsList, ArtistDataManager.refreshTrigger) {
        val updatedGroup = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
        updatedGroup.find { it.name.equals(artistItem.name, ignoreCase = true) }?.songs ?: artistItem.songs
    }

    val sortedSongs: ImmutableList<Song> = remember(currentSongs, manager.artistInnerSortOrder) {
        when (manager.artistInnerSortOrder) {
            SongSortOrder.A_TO_Z -> currentSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.Z_TO_A -> currentSongs.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SongSortOrder.DURATION -> currentSongs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE -> currentSongs.sortedByDescending { it.size }
            SongSortOrder.NEWEST -> currentSongs.sortedByDescending { it.id }
            SongSortOrder.OLDEST -> currentSongs.sortedBy { it.id }
            SongSortOrder.ARTIST -> currentSongs.sortedBy { it.artist.lowercase(Locale.getDefault()) }
        }.toImmutableList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    GlassBackButton(isDark = isDark, onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
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
                            .background(cardBg)
                            .border(1.2.dp, glassBorderBrush, CircleShape)
                            .clickable { onOpenAddSongsDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        AddTrackActionVector(tint = accent, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.2.dp, glassBorderBrush, CircleShape)
                            .combinedClickable(
                                onClick = { manager.cycleNextArtistInnerViewMode() },
                                onLongClick = onOpenGridSizeDialog
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GridViewModeVectorIcon(mode = manager.artistInnerViewMode, tint = accent, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                                .border(1.2.dp, glassBorderBrush, CircleShape)
                                .clickable { showSortMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            SortListVector(tint = accent, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("A to Z") }, onClick = { manager.setPersistentArtistInnerSort(SongSortOrder.A_TO_Z); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Z to A") }, onClick = { manager.setPersistentArtistInnerSort(SongSortOrder.Z_TO_A); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Duration") }, onClick = { manager.setPersistentArtistInnerSort(SongSortOrder.DURATION); showSortMenu = false })
                            DropdownMenuItem(text = { Text("File Size") }, onClick = { manager.setPersistentArtistInnerSort(SongSortOrder.FILE_SIZE); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Newest First") }, onClick = { manager.setPersistentArtistInnerSort(SongSortOrder.NEWEST); showSortMenu = false })
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.2.dp, accent.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                val shuffled = sortedSongs.shuffled()
                                if (shuffled.isNotEmpty()) manager.playSong(shuffled.first(), shuffled, artistItem.name)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShuffleActionVector(tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sortedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tracks available for this artist.", color = Color(0xFF64748B))
                }
            } else {
                when (manager.artistInnerViewMode) {
                    GridViewMode.LIST -> {
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
                                    onMenuClick = { onSongMenuClick(song) }
                                )
                            }
                        }
                    }
                    GridViewMode.GRID_2 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = sortedSongs,
                                key = { it.id },
                                contentType = { "artist_inner_card_2" }
                            ) { song ->
                                SquareAlbumOverlayCard(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    onPlay = { manager.playSong(song, sortedSongs, artistItem.name) },
                                    onMenuClick = { onSongMenuClick(song) }
                                )
                            }
                        }
                    }
                    GridViewMode.GRID_3 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = sortedSongs,
                                key = { it.id },
                                contentType = { "artist_inner_card_3" }
                            ) { song ->
                                SquareAlbumOverlayCard(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    onPlay = { manager.playSong(song, sortedSongs, artistItem.name) },
                                    onMenuClick = { onSongMenuClick(song) }
                                )
                            }
                        }
                    }
                    GridViewMode.GRID_4 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = sortedSongs,
                                key = { it.id },
                                contentType = { "artist_inner_card_4" }
                            ) { song ->
                                SquareAlbumOverlayCard(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    onPlay = { manager.playSong(song, sortedSongs, artistItem.name) },
                                    onMenuClick = { onSongMenuClick(song) }
                                )
                            }
                        }
                    }
                    GridViewMode.HERO_GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = sortedSongs,
                                key = { it.id },
                                contentType = { "artist_inner_card_hero" }
                            ) { song ->
                                HeroAlbumCard(
                                    song = song,
                                    manager = manager,
                                    isDark = isDark,
                                    onPlay = { manager.playSong(song, sortedSongs, artistItem.name) },
                                    onMenuClick = { onSongMenuClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 📌 ARTIST ADD SONGS DIALOG (Rendered directly at the root level)
// =========================================================================

@Composable
fun ArtistAddSongsDialog(
    artistName: String,
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val accent = manager.accentColor

    val modalScrollInterceptor = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
        }
    }

    val currentArtist = remember(artistName, manager.parsedArtistsList, ArtistDataManager.refreshTrigger) {
        val updatedGroup = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
        updatedGroup.find { it.name.equals(artistName, ignoreCase = true) }
    }
    val currentSongIdSet = remember(currentArtist) {
        currentArtist?.songs?.map { it.id }?.toHashSet() ?: hashSetOf()
    }

    var addSongSearch by remember { mutableStateOf("") }
    val candidates = remember(addSongSearch, currentSongIdSet, manager.allSongs.size) {
        manager.allSongs.filter {
            !currentSongIdSet.contains(it.id) &&
            (it.title.contains(addSongSearch, ignoreCase = true) || it.artist.contains(addSongSearch, ignoreCase = true))
        }
    }

    val dialogSurface = manager.getCurrentDialogColor()
    val glassBorder = manager.getGlassBorderBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .nestedScroll(modalScrollInterceptor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(dialogSurface)
                .border(1.5.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Songs to $artistName", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Button(onClick = { onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Done") }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = addSongSearch,
                    onValueChange = { addSongSearch = it },
                    placeholder = { Text("Search songs to add...", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = manager.getCurrentBorderColor(),
                        cursorColor = accent
                    )
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
                                    // 🚀 Permanently saves to disk and updates reactive lists immediately
                                    ArtistDataManager.addSongToArtist(artistName, song.id)
                                    val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                                    manager.parsedArtistsList.clear()
                                    manager.parsedArtistsList.addAll(updated)
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

@Composable
fun CreateArtistDialog(
    manager: MusicManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = manager.isDarkMode
    val textColor = manager.getCurrentTextColor()
    val cardBg = manager.getCurrentSurfaceColor()
    val accentColor = manager.accentColor

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val bottomInsetPadding = if (isImeVisible) {
        WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

    val dialogScrollInterceptor = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
        }
    }

    val dialogSurface = manager.getCurrentDialogColor()
    val glassBorder = manager.getGlassBorderBrush()

    var artistName by remember { mutableStateOf("") }
    var searchSongQuery by remember { mutableStateOf("") }
    val selectedSongs = remember { mutableStateListOf<Song>() }

    val filteredSongs = manager.allSongs.filter {
        it.title.contains(searchSongQuery, ignoreCase = true) || it.artist.contains(searchSongQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .nestedScroll(dialogScrollInterceptor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomInsetPadding)
                .height(650.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(dialogSurface)
                .border(1.5.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = manager.getCurrentBorderColor(),
                        cursorColor = accentColor,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Add Songs (${selectedSongs.size} selected):", color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchSongQuery,
                    onValueChange = { searchSongQuery = it },
                    placeholder = { Text("Search songs to add...", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = manager.getCurrentBorderColor(),
                        cursorColor = accentColor
                    )
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
                            val updated = ArtistParsingEngine.parseAndGroupArtists(manager.allSongs)
                            manager.parsedArtistsList.clear()
                            manager.parsedArtistsList.addAll(updated)
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
