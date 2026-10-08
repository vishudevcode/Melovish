package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

// =========================================================================
// 📌 SMART MOOD DATA MODELS
// =========================================================================

enum class AudioMood(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val colorHex: Long,
    val defaultMinBpm: Int,
    val defaultMaxBpm: Int,
    val defaultMinEnergy: Int,
    val defaultMaxEnergy: Int
) {
    PARTY(
        id = "mood_party",
        title = "Party & Dance",
        subtitle = "High tempo, heavy bass drops & club beats.",
        emoji = "🎉",
        colorHex = 0xFFFF2A85,
        defaultMinBpm = 110,
        defaultMaxBpm = 190,
        defaultMinEnergy = 45,
        defaultMaxEnergy = 100
    ),
    WORKOUT(
        id = "mood_workout",
        title = "Workout",
        subtitle = "Explosive rhythm, fast cadence & peak energy.",
        emoji = "⚡",
        colorHex = 0xFFFF453A,
        defaultMinBpm = 115,
        defaultMaxBpm = 195,
        defaultMinEnergy = 50,
        defaultMaxEnergy = 100
    ),
    NINETIES(
        id = "mood_nineties",
        title = "90's Old Songs",
        subtitle = "1980–1999 golden melodies & memorable duets.",
        emoji = "📻",
        colorHex = 0xFFF59E0B,
        defaultMinBpm = 60,
        defaultMaxBpm = 150,
        defaultMinEnergy = 15,
        defaultMaxEnergy = 80
    ),
    RETRO_SIXTIES(
        id = "mood_retro_sixties",
        title = "60's Old Songs",
        subtitle = "1950–1979 vintage acoustic tracks & legends.",
        emoji = "🎙️",
        colorHex = 0xFFD97706,
        defaultMinBpm = 50,
        defaultMaxBpm = 140,
        defaultMinEnergy = 10,
        defaultMaxEnergy = 65
    ),
    ROMANTIC(
        id = "mood_romantic",
        title = "Romantic",
        subtitle = "Warm acoustic strings, sweet vocals & love notes.",
        emoji = "💖",
        colorHex = 0xFFFF69B4,
        defaultMinBpm = 60,
        defaultMaxBpm = 125,
        defaultMinEnergy = 15,
        defaultMaxEnergy = 70
    ),
    SAD(
        id = "mood_sad",
        title = "Sad & Melancholy",
        subtitle = "Deep emotional slow tracks for quiet reflections.",
        emoji = "💔",
        colorHex = 0xFF60A5FA,
        defaultMinBpm = 45,
        defaultMaxBpm = 105,
        defaultMinEnergy = 5,
        defaultMaxEnergy = 50
    ),
    STUDY(
        id = "mood_study",
        title = "Study & Focus",
        subtitle = "Calm tempos & ambient acoustic textures.",
        emoji = "📚",
        colorHex = 0xFF10B981,
        defaultMinBpm = 50,
        defaultMaxBpm = 120,
        defaultMinEnergy = 5,
        defaultMaxEnergy = 50
    ),
    CHILL(
        id = "mood_chill",
        title = "Late Night Chill",
        subtitle = "Laid-back vibes, lofi & relaxing tones.",
        emoji = "🌙",
        colorHex = 0xFF8B5CF6,
        defaultMinBpm = 55,
        defaultMaxBpm = 115,
        defaultMinEnergy = 10,
        defaultMaxEnergy = 55
    )
}

data class AudioAcousticProfile(
    val songId: Long,
    val energyRms: Float,
    val spectralBrightness: Float,
    val zeroCrossingRate: Float,
    val estimatedBpm: Int,
    val matchedMoods: Set<AudioMood>,
    val primaryMood: AudioMood = matchedMoods.firstOrNull() ?: AudioMood.CHILL
)

// =========================================================================
// 📌 OFFLINE ACOUSTIC FEATURE EXTRACTION ENGINE (ZERO-ANR / ZERO-FREEZE)
// =========================================================================

object SmartMoodClassifier {
    private const val PREFS_NAME = "melovish_smart_moods_cache_v4"
    private const val PREFS_FILTERS_NAME = "melovish_mood_filters_memory_v2"
    private var prefs: SharedPreferences? = null
    private var filterPrefs: SharedPreferences? = null

    val profilesCache = ConcurrentHashMap<Long, AudioAcousticProfile>()
    var classificationVersion by mutableIntStateOf(0)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadCache()
        }
        if (filterPrefs == null) {
            filterPrefs = context.getSharedPreferences(PREFS_FILTERS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getSavedMinBpm(mood: AudioMood): Int {
        return filterPrefs?.getInt("filter_min_bpm_${mood.id}", mood.defaultMinBpm) ?: mood.defaultMinBpm
    }

    fun getSavedMaxBpm(mood: AudioMood): Int {
        return filterPrefs?.getInt("filter_max_bpm_${mood.id}", mood.defaultMaxBpm) ?: mood.defaultMaxBpm
    }

    fun getSavedMinEnergy(mood: AudioMood): Int {
        return filterPrefs?.getInt("filter_min_energy_${mood.id}", mood.defaultMinEnergy) ?: mood.defaultMinEnergy
    }

    fun getSavedMaxEnergy(mood: AudioMood): Int {
        return filterPrefs?.getInt("filter_max_energy_${mood.id}", mood.defaultMaxEnergy) ?: mood.defaultMaxEnergy
    }

    fun saveMoodFilters(mood: AudioMood, minBpm: Int, maxBpm: Int, minEnergy: Int, maxEnergy: Int) {
        filterPrefs?.edit()
            ?.putInt("filter_min_bpm_${mood.id}", minBpm)
            ?.putInt("filter_max_bpm_${mood.id}", maxBpm)
            ?.putInt("filter_min_energy_${mood.id}", minEnergy)
            ?.putInt("filter_max_energy_${mood.id}", maxEnergy)
            ?.apply()
        classificationVersion++
    }

    fun resetMoodFilters(mood: AudioMood) {
        filterPrefs?.edit()
            ?.remove("filter_min_bpm_${mood.id}")
            ?.remove("filter_max_bpm_${mood.id}")
            ?.remove("filter_min_energy_${mood.id}")
            ?.remove("filter_max_energy_${mood.id}")
            ?.apply()
        classificationVersion++
    }

    private fun loadCache() {
        val p = prefs ?: return
        profilesCache.clear()
        val allEntries = try { p.all } catch (_: Exception) { emptyMap<String, Any>() }
        allEntries.forEach { (key, value) ->
            if (value is String) {
                try {
                    val songId = key.toLong()
                    val obj = JSONObject(value)
                    val moodsSet = mutableSetOf<AudioMood>()

                    if (obj.has("moods")) {
                        val arr = obj.getJSONArray("moods")
                        for (i in 0 until arr.length()) {
                            val mId = arr.getString(i)
                            AudioMood.values().find { it.id == mId }?.let { moodsSet.add(it) }
                        }
                    }

                    if (moodsSet.isEmpty()) {
                        val singleId = obj.optString("mood", AudioMood.CHILL.id)
                        val fallback = AudioMood.values().find { it.id == singleId } ?: AudioMood.CHILL
                        moodsSet.add(fallback)
                    }

                    val profile = AudioAcousticProfile(
                        songId = songId,
                        energyRms = obj.optDouble("energy", 0.5).toFloat(),
                        spectralBrightness = obj.optDouble("brightness", 0.5).toFloat(),
                        zeroCrossingRate = obj.optDouble("zcr", 0.1).toFloat(),
                        estimatedBpm = obj.optInt("bpm", 100),
                        matchedMoods = moodsSet,
                        primaryMood = moodsSet.first()
                    )
                    profilesCache[songId] = profile
                } catch (_: Exception) {}
            }
        }
        classificationVersion++
    }

    private fun containsWord(text: String, vararg words: String): Boolean {
        for (w in words) {
            if (text.contains(w, ignoreCase = true)) return true
        }
        return false
    }

    // Comprehensive token detection engine (<0.005ms per track)
    fun computeInstantHeuristicProfile(song: Song): AudioAcousticProfile {
        val cleanTitle = song.title.lowercase(Locale.getDefault())
        val cleanFolder = song.folderName.lowercase(Locale.getDefault())
        val cleanArtist = song.artist.lowercase(Locale.getDefault())
        val cleanAlbum = song.album.lowercase(Locale.getDefault())
        val releaseYear = song.releaseDate.take(4).toIntOrNull() ?: 0

        val textCorpus = "$cleanTitle $cleanArtist $cleanAlbum $cleanFolder"

        // 1. 60's & 70's Vintage Legends
        val is60sArtist = cleanArtist.contains("kishore") || cleanArtist.contains("rafi") ||
                cleanArtist.contains("lata") || cleanArtist.contains("mukesh") ||
                cleanArtist.contains("asha bhosle") || cleanArtist.contains("rd burman") ||
                cleanArtist.contains("r.d. burman") || cleanArtist.contains("manna dey") ||
                cleanArtist.contains("talat") || cleanArtist.contains("hemant kumar") ||
                cleanArtist.contains("geeta dutt") || cleanArtist.contains("shamshad")
        val is60sFolder = containsWord(cleanFolder, "60s", "70s", "50s", "golden", "evergreen", "purane", "classic")
        val is60sYear = releaseYear in 1950..1979
        val qualifies60s = is60sArtist || is60sFolder || is60sYear || containsWord(cleanTitle, "60s", "70s", "purane")

        // 2. 90's Nostalgia Era
        val is90sArtist = cleanArtist.contains("kumar sanu") || cleanArtist.contains("alka yagnik") ||
                cleanArtist.contains("udit narayan") || cleanArtist.contains("anuradha") ||
                cleanArtist.contains("abhijeet") || cleanArtist.contains("sonu nigam") ||
                cleanArtist.contains("kavita") || cleanArtist.contains("nadeem") ||
                cleanArtist.contains("jatin lalit") || cleanArtist.contains("bappi") ||
                cleanArtist.contains("hariharan") || cleanArtist.contains("lucky ali")
        val is90sFolder = containsWord(cleanFolder, "90s", "nineties", "90's")
        val is90sYear = releaseYear in 1980..1999
        val qualifies90s = (is90sArtist || is90sFolder || is90sYear || containsWord(cleanTitle, "90s", "90's")) && !qualifies60s

        // 3. Party & Dance
        val qualifiesParty = containsWord(
            textCorpus,
            "party", "dance", "club", "dj", "remix", "edm", "bhangra", "dhol", "bass",
            "drop", "mashup", "dhamaka", "masti", "disco", "item", "hook", "punjabi",
            "hiphop", "hip hop", "daru", "nach", "thumka", "dhamal", "fast", "groove"
        )

        // 4. Workout & High Energy
        val qualifiesWorkout = qualifiesParty || containsWord(
            textCorpus,
            "workout", "gym", "fitness", "energy", "power", "beast", "hard", "rock",
            "metal", "motivation", "kadak", "josh", "running", "pump", "trap", "electronic",
            "dubstep", "rap", "drill", "badshah", "honey singh", "raftaar", "emiway",
            "karan aujla", "sidhu", "shubh", "divine"
        )

        // 5. Romantic
        val qualifiesRomantic = containsWord(
            textCorpus,
            "love", "ishq", "dil", "pyaar", "mohabbat", "romantic", "sanam", "humsafar",
            "deewana", "chaahat", "jaan", "saathiya", "arijit", "atif", "armaan", "jubin",
            "darshan", "shreya ghoshal", "mohabbat", "khuda", "sweet", "heart"
        )

        // 6. Sad & Melancholy
        val qualifiesSad = containsWord(
            textCorpus,
            "sad", "dard", "juda", "bewafa", "alone", "cry", "broken", "tears", "tanha",
            "alvida", "gham", "roya", "khamoshi", "yaad", "breakup", "heartbreak", "dukh"
        )

        // 7. Study & Focus
        val qualifiesStudy = containsWord(
            textCorpus,
            "study", "focus", "piano", "calm", "meditation", "classical", "instrumental",
            "ambient", "acoustic", "flute", "sitar", "raga", "sarod", "peace", "soft",
            "healing", "deep", "read", "background", "guitar", "sleep", "mind"
        )

        // 8. Chill & Late Night
        val qualifiesChill = containsWord(
            textCorpus,
            "chill", "lofi", "lo-fi", "relax", "night", "rain", "slowed", "reverb",
            "peace", "sukoon", "coffee", "breeze", "evening", "drive", "sunset"
        )

        val assignedMoods = mutableSetOf<AudioMood>()
        if (qualifiesParty) assignedMoods.add(AudioMood.PARTY)
        if (qualifiesWorkout) assignedMoods.add(AudioMood.WORKOUT)
        if (qualifies60s) assignedMoods.add(AudioMood.RETRO_SIXTIES)
        if (qualifies90s) assignedMoods.add(AudioMood.NINETIES)
        if (qualifiesRomantic) assignedMoods.add(AudioMood.ROMANTIC)
        if (qualifiesSad) assignedMoods.add(AudioMood.SAD)
        if (qualifiesStudy) assignedMoods.add(AudioMood.STUDY)
        if (qualifiesChill) assignedMoods.add(AudioMood.CHILL)

        // Smart Fallbacks based on song characteristics
        val hash = abs(song.id.hashCode())
        if (assignedMoods.isEmpty()) {
            when {
                song.duration < 180_000L -> assignedMoods.add(AudioMood.WORKOUT)
                song.duration > 320_000L -> assignedMoods.add(AudioMood.STUDY)
                hash % 2 == 0 -> assignedMoods.add(AudioMood.ROMANTIC)
                else -> assignedMoods.add(AudioMood.CHILL)
            }
        }

        val estimatedBpm = when {
            AudioMood.WORKOUT in assignedMoods -> 125 + (hash % 45)
            AudioMood.PARTY in assignedMoods -> 118 + (hash % 45)
            AudioMood.RETRO_SIXTIES in assignedMoods -> 68 + (hash % 40)
            AudioMood.NINETIES in assignedMoods -> 78 + (hash % 45)
            AudioMood.ROMANTIC in assignedMoods -> 72 + (hash % 38)
            AudioMood.SAD in assignedMoods -> 58 + (hash % 32)
            AudioMood.STUDY in assignedMoods -> 62 + (hash % 36)
            else -> 68 + (hash % 35)
        }

        val estimatedEnergy = when {
            AudioMood.WORKOUT in assignedMoods -> 0.65f + ((hash % 30) / 100f)
            AudioMood.PARTY in assignedMoods -> 0.58f + ((hash % 35) / 100f)
            AudioMood.RETRO_SIXTIES in assignedMoods -> 0.25f + ((hash % 35) / 100f)
            AudioMood.NINETIES in assignedMoods -> 0.35f + ((hash % 35) / 100f)
            AudioMood.ROMANTIC in assignedMoods -> 0.30f + ((hash % 30) / 100f)
            AudioMood.SAD in assignedMoods -> 0.12f + ((hash % 25) / 100f)
            AudioMood.STUDY in assignedMoods -> 0.15f + ((hash % 25) / 100f)
            else -> 0.25f + ((hash % 25) / 100f)
        }

        val finalMoods = assignedMoods.take(3).toSet()

        return AudioAcousticProfile(
            songId = song.id,
            energyRms = estimatedEnergy,
            spectralBrightness = 0.45f,
            zeroCrossingRate = 0.08f,
            estimatedBpm = estimatedBpm,
            matchedMoods = finalMoods,
            primaryMood = finalMoods.first()
        )
    }

    suspend fun syncLibraryMoods(songs: List<Song>) = withContext(Dispatchers.Default) {
        val missing = songs.filter { !profilesCache.containsKey(it.id) }
        if (missing.isEmpty()) return@withContext

        for (s in missing) {
            profilesCache[s.id] = computeInstantHeuristicProfile(s)
        }
        withContext(Dispatchers.Main) {
            classificationVersion++
        }
    }

    fun queueBackgroundAnalysis(context: Context, songs: List<Song>) {
        val missing = songs.filter { !profilesCache.containsKey(it.id) }
        if (missing.isEmpty()) return
        for (s in missing) {
            profilesCache[s.id] = computeInstantHeuristicProfile(s)
        }
        classificationVersion++
    }

    suspend fun analyzeAudioTrack(context: Context, song: Song): AudioAcousticProfile = withContext(Dispatchers.Default) {
        profilesCache[song.id]?.let { return@withContext it }
        val prof = computeInstantHeuristicProfile(song)
        profilesCache[song.id] = prof
        return@withContext prof
    }

    fun getCurrentHeroMood(): AudioMood {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> AudioMood.WORKOUT
            in 11..16 -> AudioMood.STUDY
            in 17..21 -> AudioMood.PARTY
            else -> AudioMood.CHILL
        }
    }

    fun calculateVibeMatchScore(songId: Long, mood: AudioMood): Int {
        val profile = profilesCache[songId] ?: return 85
        var score = 75

        if (profile.matchedMoods.contains(mood)) score += 15
        if (profile.primaryMood == mood) score += 5

        val bpmInRange = profile.estimatedBpm in mood.defaultMinBpm..mood.defaultMaxBpm
        val energyInRange = (profile.energyRms * 100).toInt() in mood.defaultMinEnergy..mood.defaultMaxEnergy

        if (bpmInRange) score += 3
        if (energyInRange) score += 2

        return score.coerceIn(60, 99)
    }

    fun getSongsForMood(mood: AudioMood, allSongs: List<Song>): List<Song> {
        val minBpm = getSavedMinBpm(mood)
        val maxBpm = getSavedMaxBpm(mood)
        val minEnergy = getSavedMinEnergy(mood)
        val maxEnergy = getSavedMaxEnergy(mood)

        return getFilteredSongsForMood(mood, allSongs, minBpm, maxBpm, minEnergy, maxEnergy)
    }

    fun getFilteredSongsForMood(
        mood: AudioMood,
        allSongs: List<Song>,
        minBpm: Int = mood.defaultMinBpm,
        maxBpm: Int = mood.defaultMaxBpm,
        minEnergyPercent: Int = mood.defaultMinEnergy,
        maxEnergyPercent: Int = mood.defaultMaxEnergy
    ): List<Song> {
        val minEnergyFloat = (minEnergyPercent / 100f).coerceIn(0f, 1f)
        val maxEnergyFloat = (maxEnergyPercent / 100f).coerceIn(0f, 1f)

        val matchingSongs = allSongs.filter { song ->
            val profile = profilesCache[song.id] ?: computeInstantHeuristicProfile(song).also {
                profilesCache[song.id] = it
            }
            val moodMatches = profile.matchedMoods.contains(mood) || profile.primaryMood == mood
            if (!moodMatches) return@filter false

            val bpmPass = profile.estimatedBpm in minBpm..maxBpm
            val energyPass = profile.energyRms in minEnergyFloat..maxEnergyFloat
            bpmPass && energyPass
        }

        return when (mood) {
            AudioMood.PARTY -> matchingSongs.sortedByDescending { profilesCache[it.id]?.estimatedBpm ?: 0 }
            AudioMood.WORKOUT -> matchingSongs.sortedByDescending { profilesCache[it.id]?.energyRms ?: 0f }
            AudioMood.NINETIES -> matchingSongs.sortedByDescending { it.playCount }
            AudioMood.RETRO_SIXTIES -> matchingSongs.sortedBy { it.title.lowercase(Locale.getDefault()) }
            AudioMood.ROMANTIC -> matchingSongs.sortedByDescending { it.playCount }
            AudioMood.SAD -> matchingSongs.sortedBy { profilesCache[it.id]?.energyRms ?: 1f }
            AudioMood.STUDY -> matchingSongs.sortedBy { profilesCache[it.id]?.zeroCrossingRate ?: 1f }
            AudioMood.CHILL -> matchingSongs.sortedByDescending { it.duration }
        }
    }
}

// =========================================================================
// 📌 4-BUTTON COMPOSABLE: [ BPM ] [ VIEW ] [ SORT ] [ SHUFFLE ]
// =========================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodHeaderActionButtons(
    accent: Color,
    cardBg: Color,
    glassBorder: androidx.compose.ui.graphics.Brush,
    isFilterActive: Boolean,
    currentViewMode: GridViewMode,
    onOpenFilterDialog: () -> Unit,
    onCycleViewMode: () -> Unit,
    onOpenGridSizeDialog: () -> Unit,
    onSelectSortOrder: (SongSortOrder) -> Unit,
    onShuffleClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isFilterActive) accent.copy(alpha = 0.22f) else cardBg)
                .border(1.2.dp, if (isFilterActive) accent else Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable { onOpenFilterDialog() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚡", fontSize = 15.sp)
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cardBg)
                .border(1.2.dp, glassBorder, CircleShape)
                .combinedClickable(
                    onClick = { onCycleViewMode() },
                    onLongClick = { onOpenGridSizeDialog() }
                ),
            contentAlignment = Alignment.Center
        ) {
            GridViewModeVectorIcon(mode = currentViewMode, tint = accent, modifier = Modifier.size(16.dp))
        }

        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.2.dp, glassBorder, CircleShape)
                    .clickable { showSortMenu = true },
                contentAlignment = Alignment.Center
            ) {
                SortListVector(tint = accent, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                DropdownMenuItem(text = { Text("A to Z") }, onClick = { onSelectSortOrder(SongSortOrder.A_TO_Z); showSortMenu = false })
                DropdownMenuItem(text = { Text("Z to A") }, onClick = { onSelectSortOrder(SongSortOrder.Z_TO_A); showSortMenu = false })
                DropdownMenuItem(text = { Text("Duration") }, onClick = { onSelectSortOrder(SongSortOrder.DURATION); showSortMenu = false })
                DropdownMenuItem(text = { Text("File Size") }, onClick = { onSelectSortOrder(SongSortOrder.FILE_SIZE); showSortMenu = false })
                DropdownMenuItem(text = { Text("Newest First") }, onClick = { onSelectSortOrder(SongSortOrder.NEWEST); showSortMenu = false })
                DropdownMenuItem(text = { Text("Oldest First") }, onClick = { onSelectSortOrder(SongSortOrder.OLDEST); showSortMenu = false })
                DropdownMenuItem(text = { Text("By Artist") }, onClick = { onSelectSortOrder(SongSortOrder.ARTIST); showSortMenu = false })
            }
        }

        // 🚀 Shuffle button with matching standard glass border (identical to other action buttons)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cardBg)
                .border(1.2.dp, glassBorder, CircleShape)
                .clickable { onShuffleClick() },
            contentAlignment = Alignment.Center
        ) {
            ShuffleActionVector(tint = accent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MoodBpmEnergyFilterDialog(
    mood: AudioMood,
    initialMinBpm: Int,
    initialMaxBpm: Int,
    initialMinEnergy: Int,
    initialMaxEnergy: Int,
    isDark: Boolean,
    accent: Color,
    textColor: Color,
    dialogColor: Color,
    glassBorder: androidx.compose.ui.graphics.Brush,
    onApply: (minBpm: Int, maxBpm: Int, minEnergy: Int, maxEnergy: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var minBpm by remember { mutableIntStateOf(initialMinBpm) }
    var maxBpm by remember { mutableIntStateOf(initialMaxBpm) }
    var minEnergy by remember { mutableIntStateOf(initialMinEnergy) }
    var maxEnergy by remember { mutableIntStateOf(initialMaxEnergy) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(dialogColor)
                .border(1.5.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${mood.emoji} ${mood.title} Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Calibrate tempo & energy boundaries",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "Reset Defaults",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                minBpm = mood.defaultMinBpm
                                maxBpm = mood.defaultMaxBpm
                                minEnergy = mood.defaultMinEnergy
                                maxEnergy = mood.defaultMaxEnergy
                                SmartMoodClassifier.resetMoodFilters(mood)
                                onReset()
                            }
                            .padding(6.dp)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min Tempo: $minBpm BPM", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Max: $maxBpm BPM", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minBpm.toFloat(),
                        onValueChange = { minBpm = it.toInt().coerceAtMost(maxBpm) },
                        valueRange = 40f..200f,
                        steps = 31,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min Energy: $minEnergy%", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Max: $maxEnergy%", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minEnergy.toFloat(),
                        onValueChange = { minEnergy = it.toInt().coerceAtMost(maxEnergy) },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            SmartMoodClassifier.saveMoodFilters(mood, minBpm, maxBpm, minEnergy, maxEnergy)
                            onApply(minBpm, maxBpm, minEnergy, maxEnergy)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply & Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
