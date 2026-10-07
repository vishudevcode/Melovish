package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// =========================================================================
// 📌 SMART MOOD DATA MODELS
// =========================================================================

enum class AudioMood(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val colorHex: Long
) {
    PARTY(
        id = "mood_party",
        title = "Party & Dance",
        subtitle = "Upbeat dance tracks, heavy rhythm & club beats.",
        emoji = "🎉",
        colorHex = 0xFFFF2A85
    ),
    WORKOUT(
        id = "mood_workout",
        title = "Workout",
        subtitle = "High BPM, explosive rhythm & raw power.",
        emoji = "⚡",
        colorHex = 0xFFFF453A
    ),
    NINETIES(
        id = "mood_nineties",
        title = "90's Old Songs",
        subtitle = "Evergreen 90s melodies, golden era duets & sweet nostalgia.",
        emoji = "📻",
        colorHex = 0xFFF59E0B
    ),
    RETRO_SIXTIES(
        id = "mood_retro_sixties",
        title = "60's Old Songs",
        subtitle = "Timeless vintage classics, retro instruments & soul legends.",
        emoji = "🎙️",
        colorHex = 0xFFD97706
    ),
    ROMANTIC(
        id = "mood_romantic",
        title = "Romantic",
        subtitle = "Warm acoustic tones, sweet melodies & love notes.",
        emoji = "💖",
        colorHex = 0xFFFF69B4
    ),
    SAD(
        id = "mood_sad",
        title = "Sad & Melancholy",
        subtitle = "Deep emotional slow tracks for quiet reflections.",
        emoji = "💔",
        colorHex = 0xFF60A5FA
    ),
    STUDY(
        id = "mood_study",
        title = "Study & Focus",
        subtitle = "Consistent, ambient & calm tracks for deep focus.",
        emoji = "📚",
        colorHex = 0xFF10B981
    ),
    CHILL(
        id = "mood_chill",
        title = "Late Night Chill",
        subtitle = "Relaxed tempo, mellow vibes & peaceful tones.",
        emoji = "🌙",
        colorHex = 0xFF8B5CF6
    )
}

data class AudioAcousticProfile(
    val songId: Long,
    val energyRms: Float,
    val spectralBrightness: Float,
    val zeroCrossingRate: Float,
    val estimatedBpm: Int,
    val primaryMood: AudioMood
)

// =========================================================================
// 📌 OFFLINE DSP & ACOUSTIC FEATURE EXTRACTION ENGINE
// =========================================================================

object SmartMoodClassifier {
    private const val PREFS_NAME = "melovish_smart_moods_cache_v2"
    private var prefs: SharedPreferences? = null

    val profilesCache = mutableStateMapOf<Long, AudioAcousticProfile>()
    var classificationVersion by mutableIntStateOf(0)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadCache()
    }

    private fun loadCache() {
        val p = prefs ?: return
        profilesCache.clear()
        p.all.forEach { (key, value) ->
            if (value is String) {
                try {
                    val songId = key.toLong()
                    val obj = JSONObject(value)
                    val moodId = obj.optString("mood", AudioMood.CHILL.id)
                    val mood = AudioMood.values().find { it.id == moodId } ?: AudioMood.CHILL
                    val profile = AudioAcousticProfile(
                        songId = songId,
                        energyRms = obj.optDouble("energy", 0.5).toFloat(),
                        spectralBrightness = obj.optDouble("brightness", 0.5).toFloat(),
                        zeroCrossingRate = obj.optDouble("zcr", 0.1).toFloat(),
                        estimatedBpm = obj.optInt("bpm", 100),
                        primaryMood = mood
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
            val obj = JSONObject().apply {
                put("energy", profile.energyRms.toDouble())
                put("brightness", profile.spectralBrightness.toDouble())
                put("zcr", profile.zeroCrossingRate.toDouble())
                put("bpm", profile.estimatedBpm)
                put("mood", profile.primaryMood.id)
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

            // Strategy 1: Direct file path
            if (song.path.isNotBlank() && File(song.path).exists()) {
                try {
                    extractor.setDataSource(song.path)
                    dataSourceSet = true
                } catch (_: Exception) {}
            }

            // Strategy 2: FileDescriptor via ContentResolver
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

                // Sample chorus/drop at 35% position
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

        // =========================================================================
        // 🔬 ACOUSTIC DSP CALCULATIONS
        // =========================================================================

        val samplesCount = pcmFloats.size
        var energyRms = 0.5f
        var zeroCrossingRate = 0.1f
        var spectralBrightness = 0.5f
        var estimatedBpm = 100

        if (samplesCount > 1024) {
            var sumSquares = 0.0
            for (i in 0 until samplesCount) {
                val s = pcmFloats[i]
                sumSquares += (s * s)
            }
            energyRms = (sqrt(sumSquares / samplesCount).toFloat() * 3.5f).coerceIn(0.05f, 1.0f)

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
            spectralBrightness = (sqrt(highFreqEnergy / samplesCount).toFloat() * 4.0f).coerceIn(0.05f, 1.0f)

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
                    // Normalize octave tempo error (e.g. 60bpm -> 120bpm)
                    if (calcBpm in 60..75 && energyRms > 0.45f) calcBpm *= 2
                    estimatedBpm = calcBpm.coerceIn(65, 185)
                }
            }
        }

        // =========================================================================
        // 🧠 HYBRID MULTI-VECTOR DECISION ENGINE (DSP + METADATA + FOLDERS)
        // =========================================================================

        val cleanTitle = song.title.lowercase(Locale.getDefault())
        val cleanFolder = song.folderName.lowercase(Locale.getDefault())
        val cleanArtist = song.artist.lowercase(Locale.getDefault())
        val cleanAlbum = song.album.lowercase(Locale.getDefault())
        val cleanPath = song.path.lowercase(Locale.getDefault())
        val releaseYear = song.releaseDate.toIntOrNull() ?: 0

        // Combined string for comprehensive pattern matching
        val metadataBlob = "$cleanTitle $cleanFolder $cleanArtist $cleanAlbum $cleanPath"

        // 1. ERA & RETRO HEURISTICS (60s & 90s)
        val is60sRetroSemantic = cleanFolder.contains("60s") || cleanFolder.contains("70s") || cleanFolder.contains("50s") ||
                cleanFolder.contains("retro") || cleanFolder.contains("purane") || cleanFolder.contains("golden") ||
                cleanFolder.contains("evergreen") || (cleanFolder.contains("old") && !cleanFolder.contains("90")) ||
                cleanTitle.contains("retro") || cleanTitle.contains("60s") || cleanTitle.contains("70s") ||
                cleanArtist.contains("kishore kumar") || cleanArtist.contains("mohammed rafi") || cleanArtist.contains("lata mangeshkar") ||
                cleanArtist.contains("mukesh") || cleanArtist.contains("asha bhosle") || cleanArtist.contains("r.d. burman") ||
                cleanArtist.contains("rd burman") || cleanArtist.contains("manna dey") || cleanArtist.contains("hemant kumar") ||
                (releaseYear in 1950..1979)

        val is90sSemantic = cleanFolder.contains("90s") || cleanFolder.contains("90's") || cleanFolder.contains("nineties") ||
                cleanTitle.contains("90s") || cleanTitle.contains("90's") ||
                cleanArtist.contains("kumar sanu") || cleanArtist.contains("alka yagnik") || cleanArtist.contains("udit narayan") ||
                cleanArtist.contains("anuradha paudwal") || cleanArtist.contains("abhijeet") || cleanArtist.contains("sonu nigam") ||
                cleanArtist.contains("kavita krishnamurthy") || cleanArtist.contains("bappi lahiri") || cleanArtist.contains("nadeem shravan") ||
                cleanArtist.contains("jatin lalit") || (releaseYear in 1980..1999)

        // 2. FOLDER-PRIORITIZED MOODS (Guarantees entire folders like "Party" or "Workout" match)
        val isPartyFolder = cleanFolder.contains("party") || cleanFolder.contains("dance") || cleanFolder.contains("club") ||
                cleanFolder.contains("dj") || cleanFolder.contains("remix") || cleanFolder.contains("edm") || cleanFolder.contains("bhangra")
        val isWorkoutFolder = cleanFolder.contains("workout") || cleanFolder.contains("gym") || cleanFolder.contains("fitness") || cleanFolder.contains("power")
        val isRomanticFolder = cleanFolder.contains("romantic") || cleanFolder.contains("love") || cleanFolder.contains("couple") || cleanFolder.contains("valentine")
        val isSadFolder = cleanFolder.contains("sad") || cleanFolder.contains("dard") || cleanFolder.contains("breakup") || cleanFolder.contains("heartbreak") || cleanFolder.contains("alone")
        val isStudyFolder = cleanFolder.contains("study") || cleanFolder.contains("focus") || cleanFolder.contains("instrumental") || cleanFolder.contains("ambient") || cleanFolder.contains("meditation")
        val isChillFolder = cleanFolder.contains("chill") || cleanFolder.contains("relax") || cleanFolder.contains("peace") || cleanFolder.contains("sleep") || cleanFolder.contains("lofi") || cleanFolder.contains("lo-fi")

        // 3. TITLE & CONTENT KEYWORDS
        val isPartyKeyword = metadataBlob.contains("party") || metadataBlob.contains("dance") || metadataBlob.contains("club") ||
                metadataBlob.contains("remix") || metadataBlob.contains("dj") || metadataBlob.contains("mashup") ||
                metadataBlob.contains("dhol") || metadataBlob.contains("bass") || metadataBlob.contains("beat") ||
                metadataBlob.contains("nach") || metadataBlob.contains("thumka") || metadataBlob.contains("electronic")
        val isWorkoutKeyword = metadataBlob.contains("workout") || metadataBlob.contains("gym") || metadataBlob.contains("motivation") ||
                metadataBlob.contains("fit") || metadataBlob.contains("beast") || metadataBlob.contains("trap") || metadataBlob.contains("power")
        val isRomanticKeyword = metadataBlob.contains("love") || metadataBlob.contains("ishq") || metadataBlob.contains("dil") ||
                metadataBlob.contains("pyaar") || metadataBlob.contains("mohabbat") || metadataBlob.contains("deewana") ||
                metadataBlob.contains("romantic") || metadataBlob.contains("sanam") || metadataBlob.contains("humsafar") || metadataBlob.contains("jaan")
        val isSadKeyword = metadataBlob.contains("sad") || metadataBlob.contains("dard") || metadataBlob.contains("juda") ||
                metadataBlob.contains("bewafa") || metadataBlob.contains("rooth") || metadataBlob.contains("tanha") ||
                metadataBlob.contains("alone") || metadataBlob.contains("cry") || metadataBlob.contains("broken") || metadataBlob.contains("tears")
        val isStudyKeyword = metadataBlob.contains("study") || metadataBlob.contains("focus") || metadataBlob.contains("piano") ||
                metadataBlob.contains("acoustic guitar") || metadataBlob.contains("calm") || metadataBlob.contains("instrumental")
        val isChillKeyword = metadataBlob.contains("lofi") || metadataBlob.contains("lo-fi") || metadataBlob.contains("chill") ||
                metadataBlob.contains("relax") || metadataBlob.contains("rain") || metadataBlob.contains("night") || metadataBlob.contains("slowed")

        // 4. DECISION HIERARCHY
        val classifiedMood: AudioMood = when {
            // Priority 1: Era Specific Matching
            is60sRetroSemantic -> {
                AudioMood.RETRO_SIXTIES
            }
            is90sSemantic -> {
                AudioMood.NINETIES
            }

            // Priority 2: Direct Folder Ownership
            isPartyFolder -> {
                AudioMood.PARTY
            }
            isWorkoutFolder -> {
                AudioMood.WORKOUT
            }
            isRomanticFolder -> {
                AudioMood.ROMANTIC
            }
            isSadFolder -> {
                AudioMood.SAD
            }
            isStudyFolder -> {
                AudioMood.STUDY
            }
            isChillFolder -> {
                AudioMood.CHILL
            }

            // Priority 3: Keyword + Acoustic Hybrid
            isPartyKeyword || (energyRms >= 0.44f && estimatedBpm >= 110) || (energyRms >= 0.52f && spectralBrightness >= 0.45f) -> {
                AudioMood.PARTY
            }
            isWorkoutKeyword || (energyRms >= 0.50f && estimatedBpm >= 120 && zeroCrossingRate >= 0.07f) -> {
                AudioMood.WORKOUT
            }
            isSadKeyword || (energyRms <= 0.35f && estimatedBpm <= 96 && spectralBrightness <= 0.38f) -> {
                AudioMood.SAD
            }
            isRomanticKeyword || (estimatedBpm in 70..112 && energyRms in 0.28f..0.65f) -> {
                AudioMood.ROMANTIC
            }
            isStudyKeyword || (energyRms <= 0.34f && spectralBrightness <= 0.38f && zeroCrossingRate <= 0.06f) -> {
                AudioMood.STUDY
            }
            isChillKeyword || (energyRms <= 0.42f && estimatedBpm in 65..95) -> {
                AudioMood.CHILL
            }

            // Fallback: Energy-based distribution
            energyRms >= 0.48f -> {
                AudioMood.PARTY
            }
            else -> {
                AudioMood.CHILL
            }
        }

        val profile = AudioAcousticProfile(
            songId = song.id,
            energyRms = energyRms,
            spectralBrightness = spectralBrightness,
            zeroCrossingRate = zeroCrossingRate,
            estimatedBpm = estimatedBpm,
            primaryMood = classifiedMood
        )

        saveProfile(profile)
        return@withContext profile
    }

    fun getSongsForMood(mood: AudioMood, allSongs: List<Song>): List<Song> {
        val matchingSongIds = profilesCache.filterValues { it.primaryMood == mood }.keys.toSet()
        val matchingSongs = allSongs.filter { matchingSongIds.contains(it.id) }

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
