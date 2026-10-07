package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import java.io.File
import java.nio.ByteOrder
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

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
        defaultMinBpm = 118,
        defaultMaxBpm = 185,
        defaultMinEnergy = 52,
        defaultMaxEnergy = 100
    ),
    WORKOUT(
        id = "mood_workout",
        title = "Workout",
        subtitle = "Explosive rhythm, fast cadence & peak energy.",
        emoji = "⚡",
        colorHex = 0xFFFF453A,
        defaultMinBpm = 125,
        defaultMaxBpm = 190,
        defaultMinEnergy = 60,
        defaultMaxEnergy = 100
    ),
    NINETIES(
        id = "mood_nineties",
        title = "90's Old Songs",
        subtitle = "1980–1999 golden melodies & memorable duets.",
        emoji = "📻",
        colorHex = 0xFFF59E0B,
        defaultMinBpm = 65,
        defaultMaxBpm = 145,
        defaultMinEnergy = 20,
        defaultMaxEnergy = 72
    ),
    RETRO_SIXTIES(
        id = "mood_retro_sixties",
        title = "60's Old Songs",
        subtitle = "1950–1979 vintage acoustic tracks & legends.",
        emoji = "🎙️",
        colorHex = 0xFFD97706,
        defaultMinBpm = 55,
        defaultMaxBpm = 135,
        defaultMinEnergy = 10,
        defaultMaxEnergy = 58
    ),
    ROMANTIC(
        id = "mood_romantic",
        title = "Romantic",
        subtitle = "Warm acoustic strings, sweet vocals & love notes.",
        emoji = "💖",
        colorHex = 0xFFFF69B4,
        defaultMinBpm = 68,
        defaultMaxBpm = 112,
        defaultMinEnergy = 22,
        defaultMaxEnergy = 60
    ),
    SAD(
        id = "mood_sad",
        title = "Sad & Melancholy",
        subtitle = "Deep emotional slow tracks for quiet reflections.",
        emoji = "💔",
        colorHex = 0xFF60A5FA,
        defaultMinBpm = 50,
        defaultMaxBpm = 95,
        defaultMinEnergy = 5,
        defaultMaxEnergy = 40
    ),
    STUDY(
        id = "mood_study",
        title = "Study & Focus",
        subtitle = "Calm tempos & ambient acoustic textures.",
        emoji = "📚",
        colorHex = 0xFF10B981,
        defaultMinBpm = 55,
        defaultMaxBpm = 110,
        defaultMinEnergy = 5,
        defaultMaxEnergy = 38
    ),
    CHILL(
        id = "mood_chill",
        title = "Late Night Chill",
        subtitle = "Laid-back vibes, lofi & relaxing tones.",
        emoji = "🌙",
        colorHex = 0xFF8B5CF6,
        defaultMinBpm = 60,
        defaultMaxBpm = 102,
        defaultMinEnergy = 15,
        defaultMaxEnergy = 48
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
// 📌 OFFLINE DSP & ACOUSTIC FEATURE EXTRACTION ENGINE (V4 CALIBRATED)
// =========================================================================

object SmartMoodClassifier {
    private const val PREFS_NAME = "melovish_smart_moods_cache_v4"
    private const val PREFS_FILTERS_NAME = "melovish_mood_filters_memory_v2"
    private var prefs: SharedPreferences? = null
    private var filterPrefs: SharedPreferences? = null

    val profilesCache = mutableStateMapOf<Long, AudioAcousticProfile>()
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

    // --- Persistent Acoustic Filter Bounds Helpers ---
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
        p.all.forEach { (key, value) ->
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

    private fun saveProfile(profile: AudioAcousticProfile) {
        profilesCache[profile.songId] = profile
        val p = prefs ?: return
        try {
            val moodsArr = JSONArray()
            profile.matchedMoods.forEach { moodsArr.put(it.id) }

            val obj = JSONObject().apply {
                put("energy", profile.energyRms.toDouble())
                put("brightness", profile.spectralBrightness.toDouble())
                put("zcr", profile.zeroCrossingRate.toDouble())
                put("bpm", profile.estimatedBpm)
                put("mood", profile.primaryMood.id)
                put("moods", moodsArr)
            }
            p.edit().putString(profile.songId.toString(), obj.toString()).apply()
            classificationVersion++
        } catch (_: Exception) {}
    }

    suspend fun analyzeAudioTrack(context: Context, song: Song): AudioAcousticProfile = withContext(Dispatchers.IO) {
        profilesCache[song.id]?.let { return@withContext it }

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null

        val pcmFloats = ArrayList<Float>(44100 * 6)
        var sampleRate = 44100
        var channels = 2

        try {
            extractor = MediaExtractor()
            var dataSourceSet = false

            if (song.path.isNotBlank() && File(song.path).exists()) {
                try {
                    extractor.setDataSource(song.path)
                    dataSourceSet = true
                } catch (_: Exception) {}
            }

            if (!dataSourceSet && song.uri.toString().isNotBlank()) {
                try {
                    val uri = if (song.uri is Uri) song.uri else Uri.parse(song.uri.toString())
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        extractor.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                    }
                } catch (_: Exception) {}
            }

            if (!dataSourceSet && song.path.isNotBlank()) {
                try {
                    extractor.setDataSource(song.path)
                    dataSourceSet = true
                } catch (_: Exception) {}
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex != -1 && audioFormat != null) {
                sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else 44100

                channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else 2

                val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(audioFormat, null, null, 0)
                decoder.start()

                extractor.selectTrack(audioTrackIndex)

                val seekPositionUs = (song.duration * 0.35 * 1000).toLong().coerceAtLeast(0L)
                extractor.seekTo(seekPositionUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val bufferInfo = MediaCodec.BufferInfo()
                val targetSampleLimit = sampleRate * 6
                var totalSamplesCollected = 0
                var isEos = false
                val timeoutUs = 5000L

                while (!isEos && totalSamplesCollected < targetSampleLimit) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEos = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outIndex >= 0) {
                        val outBuffer = decoder.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                            val shortBuffer = outBuffer.asShortBuffer()
                            while (shortBuffer.hasRemaining() && totalSamplesCollected < targetSampleLimit) {
                                val sample = shortBuffer.get() / 32768.0f
                                if (channels > 1 && shortBuffer.hasRemaining()) {
                                    for (c in 1 until channels) {
                                        if (shortBuffer.hasRemaining()) shortBuffer.get()
                                    }
                                }
                                pcmFloats.add(sample)
                                totalSamplesCollected++
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = decoder.outputFormat
                        if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }

        val samplesCount = pcmFloats.size
        var energyRms = 0.45f
        var zeroCrossingRate = 0.08f
        var spectralBrightness = 0.45f
        var estimatedBpm = 95

        if (samplesCount > 1024) {
            var sumSquares = 0.0
            for (i in 0 until samplesCount) {
                val s = pcmFloats[i]
                sumSquares += (s * s)
            }
            energyRms = (sqrt(sumSquares / samplesCount).toFloat() * 3.2f).coerceIn(0.05f, 1.0f)

            var zeroCrossings = 0
            for (i in 1 until samplesCount) {
                if ((pcmFloats[i] >= 0f && pcmFloats[i - 1] < 0f) || (pcmFloats[i] < 0f && pcmFloats[i - 1] >= 0f)) {
                    zeroCrossings++
                }
            }
            zeroCrossingRate = (zeroCrossings.toFloat() / samplesCount).coerceIn(0.01f, 0.45f)

            var highFreqEnergy = 0.0
            for (i in 1 until samplesCount) {
                val diff = pcmFloats[i] - pcmFloats[i - 1]
                highFreqEnergy += (diff * diff)
            }
            spectralBrightness = (sqrt(highFreqEnergy / samplesCount).toFloat() * 3.6f).coerceIn(0.05f, 1.0f)

            val windowSize = (sampleRate * 0.05).toInt()
            val numWindows = samplesCount / windowSize
            if (numWindows > 16) {
                val envelope = FloatArray(numWindows)
                for (w in 0 until numWindows) {
                    var winEnergy = 0.0
                    val offset = w * windowSize
                    for (j in 0 until windowSize) {
                        val s = pcmFloats[offset + j]
                        winEnergy += abs(s)
                    }
                    envelope[w] = (winEnergy / windowSize).toFloat()
                }

                val windowsPerSec = sampleRate.toFloat() / windowSize
                val minLag = (windowsPerSec * 60f / 185f).toInt()
                val maxLag = (windowsPerSec * 60f / 65f).toInt()

                var bestCorr = 0f
                var bestLag = (minLag + maxLag) / 2

                for (lag in minLag..maxLag) {
                    var corr = 0f
                    for (k in 0 until (numWindows - lag)) {
                        corr += envelope[k] * envelope[k + lag]
                    }
                    if (corr > bestCorr) {
                        bestCorr = corr
                        bestLag = lag
                    }
                }

                if (bestLag > 0) {
                    var calcBpm = ((windowsPerSec * 60f) / bestLag).toInt()
                    if (calcBpm in 55..70 && energyRms > 0.50f) calcBpm *= 2
                    if (calcBpm > 175 && energyRms < 0.40f) calcBpm /= 2
                    estimatedBpm = calcBpm.coerceIn(55, 185)
                }
            }
        }

        val cleanTitle = song.title.lowercase(Locale.getDefault())
        val cleanFolder = song.folderName.lowercase(Locale.getDefault())
        val cleanArtist = song.artist.lowercase(Locale.getDefault())
        val cleanAlbum = song.album.lowercase(Locale.getDefault())
        val releaseYear = song.releaseDate.toIntOrNull() ?: 0

        fun containsWord(text: String, vararg words: String): Boolean {
            return words.any { word ->
                Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(text)
            }
        }

        val folderAndTitle = "$cleanFolder $cleanTitle"
        val metaString = "$cleanTitle $cleanArtist $cleanAlbum"

        // 1. 60's & RETRO GOLDEN ERA
        val is60sArtist = cleanArtist.contains("kishore kumar") || cleanArtist.contains("mohammed rafi") ||
                cleanArtist.contains("lata mangeshkar") || cleanArtist.contains("mukesh") ||
                cleanArtist.contains("asha bhosle") || cleanArtist.contains("rd burman") ||
                cleanArtist.contains("r.d. burman") || cleanArtist.contains("manna dey") ||
                cleanArtist.contains("talat mahmood") || cleanArtist.contains("hemant kumar")

        val is60sFolder = containsWord(cleanFolder, "60s", "70s", "50s", "golden", "evergreen", "purane") ||
                (containsWord(cleanFolder, "retro", "old") && !cleanFolder.contains("90"))

        val is60sYear = releaseYear in 1950..1979

        val qualifies60s = (is60sArtist || is60sFolder || is60sYear || containsWord(cleanTitle, "60s", "70s")) &&
                (energyRms <= 0.60f && estimatedBpm <= 145)

        // 2. 90's NOSTALGIA ERA
        val is90sArtist = cleanArtist.contains("kumar sanu") || cleanArtist.contains("alka yagnik") ||
                cleanArtist.contains("udit narayan") || cleanArtist.contains("anuradha paudwal") ||
                cleanArtist.contains("abhijeet") || cleanArtist.contains("sonu nigam") ||
                cleanArtist.contains("kavita krishnamurthy") || cleanArtist.contains("nadeem shravan") ||
                cleanArtist.contains("jatin lalit") || cleanArtist.contains("bappi lahiri")

        val is90sFolder = containsWord(cleanFolder, "90s", "nineties", "90's")
        val is90sYear = releaseYear in 1980..1999

        val qualifies90s = (is90sArtist || is90sFolder || is90sYear || containsWord(cleanTitle, "90s", "90's")) &&
                !qualifies60s && (energyRms <= 0.75f)

        // 3. PARTY & DANCE
        val isPartyFolder = containsWord(cleanFolder, "party", "dance", "club", "dj", "remix", "edm", "bhangra", "pub")
        val isPartyWord = containsWord(folderAndTitle, "party", "dance", "club", "remix", "dhol", "bhangra", "mashup", "dj", "bass drop")

        val qualifiesParty = when {
            isPartyFolder -> (energyRms >= 0.40f && estimatedBpm >= 105)
            isPartyWord -> (energyRms >= 0.45f && estimatedBpm >= 110)
            else -> (energyRms >= 0.54f && estimatedBpm >= 118 && spectralBrightness >= 0.46f)
        }

        // 4. WORKOUT
        val isWorkoutFolder = containsWord(cleanFolder, "workout", "gym", "fitness", "crossfit", "running")
        val isWorkoutWord = containsWord(folderAndTitle, "workout", "gym", "motivation", "beast", "hardstyle")

        val qualifiesWorkout = when {
            isWorkoutFolder -> (energyRms >= 0.45f && estimatedBpm >= 115)
            isWorkoutWord -> (energyRms >= 0.50f && estimatedBpm >= 120)
            else -> (energyRms >= 0.60f && estimatedBpm >= 125 && zeroCrossingRate >= 0.06f)
        }

        // 5. ROMANTIC
        val isRomanticWord = containsWord(metaString, "love", "ishq", "dil", "pyaar", "mohabbat", "romantic", "sanam", "humsafar")
        val isRomanticFolder = containsWord(cleanFolder, "romantic", "love", "couple", "valentine")

        val qualifiesRomantic = (isRomanticFolder || isRomanticWord || (estimatedBpm in 68..112 && energyRms in 0.22f..0.60f)) &&
                !qualifiesParty && !qualifiesWorkout

        // 6. SAD & MELANCHOLY
        val isSadWord = containsWord(metaString, "sad", "dard", "juda", "bewafa", "alone", "cry", "broken", "tears", "tanha")
        val isSadFolder = containsWord(cleanFolder, "sad", "dard", "breakup", "heartbreak")

        val qualifiesSad = (isSadFolder || isSadWord || (energyRms <= 0.38f && estimatedBpm <= 95 && spectralBrightness <= 0.38f)) &&
                !qualifiesParty && !qualifiesWorkout

        // 7. STUDY & FOCUS
        val isStudyWord = containsWord(folderAndTitle, "study", "focus", "piano", "calm", "meditation", "classical")
        val isStudyFolder = containsWord(cleanFolder, "study", "focus", "instrumental", "ambient")

        val qualifiesStudy = (isStudyFolder || isStudyWord || (energyRms <= 0.36f && spectralBrightness <= 0.38f && zeroCrossingRate <= 0.06f)) &&
                !qualifiesParty && !qualifiesWorkout

        // 8. CHILL & RELAX
        val isChillWord = containsWord(folderAndTitle, "chill", "lofi", "lo-fi", "relax", "peace", "night", "rain", "slowed")
        val isChillFolder = containsWord(cleanFolder, "chill", "relax", "lofi", "sleep")

        val qualifiesChill = (isChillFolder || isChillWord || (energyRms in 0.15f..0.48f && estimatedBpm in 60..102)) &&
                !qualifiesParty && !qualifiesWorkout

        val assignedMoods = mutableSetOf<AudioMood>()

        if (qualifiesParty) assignedMoods.add(AudioMood.PARTY)
        if (qualifiesWorkout) assignedMoods.add(AudioMood.WORKOUT)
        if (qualifies60s) assignedMoods.add(AudioMood.RETRO_SIXTIES)
        if (qualifies90s) assignedMoods.add(AudioMood.NINETIES)
        if (qualifiesRomantic) assignedMoods.add(AudioMood.ROMANTIC)
        if (qualifiesSad) assignedMoods.add(AudioMood.SAD)
        if (qualifiesStudy) assignedMoods.add(AudioMood.STUDY)
        if (qualifiesChill) assignedMoods.add(AudioMood.CHILL)

        if (assignedMoods.isEmpty()) {
            assignedMoods.add(if (energyRms > 0.50f) AudioMood.PARTY else AudioMood.CHILL)
        }

        val finalMoods = assignedMoods.take(2).toSet()

        val profile = AudioAcousticProfile(
            songId = song.id,
            energyRms = energyRms,
            spectralBrightness = spectralBrightness,
            zeroCrossingRate = zeroCrossingRate,
            estimatedBpm = estimatedBpm,
            matchedMoods = finalMoods,
            primaryMood = finalMoods.first()
        )

        saveProfile(profile)
        return@withContext profile
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
            val profile = profilesCache[song.id] ?: return@filter false
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
        // 1. BPM & Energy Filter Button (Far Left)
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

        // 2. View Mode Button
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

        // 3. Sort Menu Button
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

        // 4. Shuffle Button (Far Right)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cardBg)
                .border(1.2.dp, accent.copy(alpha = 0.55f), CircleShape)
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
                        valueRange = 50f..190f,
                        steps = 27,
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
