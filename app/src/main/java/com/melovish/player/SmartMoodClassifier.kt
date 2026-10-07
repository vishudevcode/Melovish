package com.melovish.player

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
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
    val matchedMoods: Set<AudioMood>,
    val primaryMood: AudioMood = matchedMoods.firstOrNull() ?: AudioMood.CHILL
)

// =========================================================================
// 📌 OFFLINE DSP & MULTI-MOOD ACOUSTIC ENGINE
// =========================================================================

object SmartMoodClassifier {
    private const val PREFS_NAME = "melovish_smart_moods_cache_v3"
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

        // DSP Calculations
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
                    if (calcBpm in 60..75 && energyRms > 0.45f) calcBpm *= 2
                    estimatedBpm = calcBpm.coerceIn(65, 185)
                }
            }
        }

        // Multi-Criteria Scoring Vector
        val cleanTitle = song.title.lowercase(Locale.getDefault())
        val cleanFolder = song.folderName.lowercase(Locale.getDefault())
        val cleanArtist = song.artist.lowercase(Locale.getDefault())
        val cleanAlbum = song.album.lowercase(Locale.getDefault())
        val cleanPath = song.path.lowercase(Locale.getDefault())
        val releaseYear = song.releaseDate.toIntOrNull() ?: 0
        val metadataBlob = "$cleanTitle $cleanFolder $cleanArtist $cleanAlbum $cleanPath"

        val assignedMoods = mutableSetOf<AudioMood>()

        // 1. Vintage / Era Detection
        val is60sRetro = cleanFolder.contains("60s") || cleanFolder.contains("70s") || cleanFolder.contains("50s") ||
                cleanFolder.contains("retro") || cleanFolder.contains("purane") || cleanFolder.contains("golden") ||
                cleanFolder.contains("evergreen") || (cleanFolder.contains("old") && !cleanFolder.contains("90")) ||
                cleanTitle.contains("retro") || cleanTitle.contains("60s") || cleanTitle.contains("70s") ||
                cleanArtist.contains("kishore kumar") || cleanArtist.contains("mohammed rafi") || cleanArtist.contains("lata mangeshkar") ||
                cleanArtist.contains("mukesh") || cleanArtist.contains("asha bhosle") || cleanArtist.contains("rd burman") ||
                (releaseYear in 1950..1979)

        val is90s = cleanFolder.contains("90s") || cleanFolder.contains("90's") || cleanFolder.contains("nineties") ||
                cleanTitle.contains("90s") || cleanTitle.contains("90's") ||
                cleanArtist.contains("kumar sanu") || cleanArtist.contains("alka yagnik") || cleanArtist.contains("udit narayan") ||
                cleanArtist.contains("anuradha paudwal") || cleanArtist.contains("abhijeet") || cleanArtist.contains("sonu nigam") ||
                cleanArtist.contains("kavita krishnamurthy") || cleanArtist.contains("nadeem shravan") || (releaseYear in 1980..1999)

        if (is60sRetro) assignedMoods.add(AudioMood.RETRO_SIXTIES)
        if (is90s) assignedMoods.add(AudioMood.NINETIES)

        // 2. Party & Dance
        val isParty = cleanFolder.contains("party") || cleanFolder.contains("dance") || cleanFolder.contains("club") ||
                cleanFolder.contains("dj") || cleanFolder.contains("remix") || cleanFolder.contains("edm") ||
                metadataBlob.contains("party") || metadataBlob.contains("dance") || metadataBlob.contains("club") ||
                metadataBlob.contains("remix") || metadataBlob.contains("dj") || metadataBlob.contains("mashup") ||
                metadataBlob.contains("dhol") || metadataBlob.contains("bass") || metadataBlob.contains("nach") ||
                (energyRms >= 0.44f && estimatedBpm >= 110) || (energyRms >= 0.52f && spectralBrightness >= 0.45f)

        if (isParty) assignedMoods.add(AudioMood.PARTY)

        // 3. Workout
        val isWorkout = cleanFolder.contains("workout") || cleanFolder.contains("gym") || cleanFolder.contains("fitness") ||
                metadataBlob.contains("workout") || metadataBlob.contains("gym") || metadataBlob.contains("motivation") ||
                metadataBlob.contains("trap") || metadataBlob.contains("beast") ||
                (energyRms >= 0.50f && estimatedBpm >= 120 && zeroCrossingRate >= 0.07f) ||
                (isParty && energyRms >= 0.55f && estimatedBpm >= 125)

        if (isWorkout) assignedMoods.add(AudioMood.WORKOUT)

        // 4. Romantic
        val isRomantic = cleanFolder.contains("romantic") || cleanFolder.contains("love") || cleanFolder.contains("couple") ||
                metadataBlob.contains("love") || metadataBlob.contains("ishq") || metadataBlob.contains("dil") ||
                metadataBlob.contains("pyaar") || metadataBlob.contains("mohabbat") || metadataBlob.contains("romantic") ||
                metadataBlob.contains("sanam") || metadataBlob.contains("jaan") ||
                (estimatedBpm in 70..112 && energyRms in 0.28f..0.65f && !isParty)

        if (isRomantic) assignedMoods.add(AudioMood.ROMANTIC)

        // 5. Sad & Melancholy
        val isSad = cleanFolder.contains("sad") || cleanFolder.contains("dard") || cleanFolder.contains("breakup") ||
                metadataBlob.contains("sad") || metadataBlob.contains("dard") || metadataBlob.contains("juda") ||
                metadataBlob.contains("bewafa") || metadataBlob.contains("alone") || metadataBlob.contains("cry") ||
                (energyRms <= 0.35f && estimatedBpm <= 96 && spectralBrightness <= 0.38f)

        if (isSad) assignedMoods.add(AudioMood.SAD)

        // 6. Study & Focus
        val isStudy = cleanFolder.contains("study") || cleanFolder.contains("focus") || cleanFolder.contains("ambient") ||
                metadataBlob.contains("study") || metadataBlob.contains("focus") || metadataBlob.contains("piano") ||
                metadataBlob.contains("calm") || metadataBlob.contains("instrumental") ||
                (energyRms <= 0.34f && spectralBrightness <= 0.38f && zeroCrossingRate <= 0.06f)

        if (isStudy) assignedMoods.add(AudioMood.STUDY)

        // 7. Late Night Chill
        val isChill = cleanFolder.contains("chill") || cleanFolder.contains("relax") || cleanFolder.contains("sleep") ||
                metadataBlob.contains("lofi") || metadataBlob.contains("lo-fi") || metadataBlob.contains("chill") ||
                metadataBlob.contains("rain") || metadataBlob.contains("slowed") ||
                (energyRms <= 0.42f && estimatedBpm in 65..95)

        if (isChill || assignedMoods.isEmpty()) assignedMoods.add(AudioMood.CHILL)

        // Cap to top 3 matching moods to maintain playlists quality
        val finalMoods = assignedMoods.take(3).toSet()

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

    // Dynamic Time-of-Day Hero Mood (For Search Tab Highlighting)
    fun getCurrentHeroMood(): AudioMood {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> AudioMood.WORKOUT
            in 11..16 -> AudioMood.STUDY
            in 17..21 -> AudioMood.PARTY
            else -> AudioMood.CHILL
        }
    }

    // Vibe match calculation percentage (0% to 100%)
    fun calculateVibeMatchScore(songId: Long, mood: AudioMood): Int {
        val profile = profilesCache[songId] ?: return 85
        var score = 75

        if (profile.matchedMoods.contains(mood)) score += 15
        if (profile.primaryMood == mood) score += 5

        when (mood) {
            AudioMood.PARTY, AudioMood.WORKOUT -> {
                if (profile.estimatedBpm >= 120) score += 5
                if (profile.energyRms >= 0.50f) score += 5
            }
            AudioMood.ROMANTIC, AudioMood.NINETIES, AudioMood.RETRO_SIXTIES -> {
                if (profile.estimatedBpm in 70..115) score += 5
            }
            AudioMood.SAD, AudioMood.STUDY, AudioMood.CHILL -> {
                if (profile.energyRms <= 0.38f) score += 5
            }
        }
        return score.coerceIn(60, 99)
    }

    // Primary retrieval with multi-criteria fallback
    fun getSongsForMood(mood: AudioMood, allSongs: List<Song>): List<Song> {
        return getFilteredSongsForMood(mood, allSongs, minBpm = 0, minEnergyPercent = 0)
    }

    // Interactive slider-filtered query
    fun getFilteredSongsForMood(
        mood: AudioMood,
        allSongs: List<Song>,
        minBpm: Int = 0,
        minEnergyPercent: Int = 0
    ): List<Song> {
        val minEnergyFloat = (minEnergyPercent / 100f).coerceIn(0f, 1f)

        val matchingSongs = allSongs.filter { song ->
            val profile = profilesCache[song.id]
            val moodMatches = profile?.matchedMoods?.contains(mood) == true || profile?.primaryMood == mood

            if (!moodMatches) return@filter false

            val bpmPass = profile.estimatedBpm >= minBpm
            val energyPass = profile.energyRms >= minEnergyFloat
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
// 📌 SELF-CONTAINED FROSTED BPM & ENERGY CONTROLS (Zero MainActivity bloat)
// =========================================================================

@Composable
fun MoodHeaderActionButtons(
    accent: Color,
    cardBg: Color,
    glassBorder: androidx.compose.ui.graphics.Brush,
    isFilterActive: Boolean,
    onOpenFilterDialog: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // BPM & Energy Filter Button (Left to Shuffle)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isFilterActive) accent.copy(alpha = 0.22f) else cardBg)
                .border(1.2.dp, if (isFilterActive) accent else Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable { onOpenFilterDialog() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Shuffle Button (Far Right)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cardBg)
                .border(1.2.dp, accent.copy(alpha = 0.55f), CircleShape)
                .clickable { onShuffleClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val strokeW = w * 0.14f

                val p1 = Path().apply {
                    moveTo(w * 0.08f, h * 0.32f)
                    lineTo(w * 0.30f, h * 0.32f)
                    cubicTo(w * 0.46f, h * 0.32f, w * 0.54f, h * 0.68f, w * 0.70f, h * 0.68f)
                    lineTo(w * 0.82f, h * 0.68f)
                }
                drawPath(p1, accent, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val p2 = Path().apply {
                    moveTo(w * 0.08f, h * 0.68f)
                    lineTo(w * 0.30f, h * 0.68f)
                    cubicTo(w * 0.46f, h * 0.68f, w * 0.54f, h * 0.32f, w * 0.70f, h * 0.32f)
                    lineTo(w * 0.82f, h * 0.32f)
                }
                drawPath(p2, accent, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val arr1 = Path().apply {
                    moveTo(w * 0.94f, h * 0.32f)
                    lineTo(w * 0.70f, h * 0.12f)
                    lineTo(w * 0.70f, h * 0.52f)
                    close()
                }
                drawPath(arr1, accent)

                val arr2 = Path().apply {
                    moveTo(w * 0.94f, h * 0.68f)
                    lineTo(w * 0.70f, h * 0.48f)
                    lineTo(w * 0.70f, h * 0.88f)
                    close()
                }
                drawPath(arr2, accent)
            }
        }
    }
}

@Composable
fun MoodBpmEnergyFilterDialog(
    initialMinBpm: Int,
    initialMinEnergy: Int,
    isDark: Boolean,
    accent: Color,
    textColor: Color,
    dialogColor: Color,
    glassBorder: androidx.compose.ui.graphics.Brush,
    onApply: (minBpm: Int, minEnergy: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var minBpm by remember { mutableIntStateOf(initialMinBpm) }
    var minEnergy by remember { mutableIntStateOf(initialMinEnergy) }

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
                            text = "Acoustic DSP Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Filter playlist tracks by acoustic tempo & power",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "Reset",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                minBpm = 0
                                minEnergy = 0
                                onReset()
                            }
                            .padding(6.dp)
                    )
                }

                // Min BPM Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min Tempo (BPM)", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (minBpm > 0) "$minBpm BPM" else "Any Pace", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minBpm.toFloat(),
                        onValueChange = { minBpm = it.toInt() },
                        valueRange = 0f..180f,
                        steps = 17,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                // Min Energy Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min Acoustic Energy", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (minEnergy > 0) "$minEnergy%" else "Any Energy", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minEnergy.toFloat(),
                        onValueChange = { minEnergy = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 9,
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
                            onApply(minBpm, minEnergy)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
