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
    WORKOUT(
        id = "mood_workout",
        title = "Workout",
        subtitle = "High BPM, explosive rhythm & raw power.",
        emoji = "⚡",
        colorHex = 0xFFFF453A
    ),
    PARTY(
        id = "mood_party",
        title = "Party & Dance",
        subtitle = "Upbeat dance tracks, heavy rhythm & club beats.",
        emoji = "🎉",
        colorHex = 0xFFFF2A85
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
    private const val PREFS_NAME = "melovish_smart_moods_cache_v1"
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
            val uri = Uri.parse(song.uri)
            try {
                // Fixed: Explicit null cast for Map<String, String>? to prevent overload ambiguity
                extractor.setDataSource(context, uri, null as Map<String, String>?)
            } catch (_: Exception) {
                if (song.path.isNotBlank()) {
                    extractor.setDataSource(song.path)
                }
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

                val seekPositionUs = (song.duration * 0.30 * 1000).toLong().coerceAtLeast(0L)
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
                    val calcBpm = ((windowsPerSec * 60f) / bestLag).toInt()
                    estimatedBpm = calcBpm.coerceIn(65, 185)
                }
            }
        }

        // =========================================================================
        // 🧠 ACOUSTIC + SEMANTIC MULTI-VECTOR DECISION TREE
        // =========================================================================

        val cleanTitle = song.title.lowercase(Locale.getDefault())

        val isRomanticSemantic = cleanTitle.contains("love") || cleanTitle.contains("ishq") || cleanTitle.contains("dil") ||
                cleanTitle.contains("romantic") || cleanTitle.contains("pyaar") || cleanTitle.contains("sanam")
        val isSadSemantic = cleanTitle.contains("sad") || cleanTitle.contains("juda") || cleanTitle.contains("dard") ||
                cleanTitle.contains("alone") || cleanTitle.contains("cry") || cleanTitle.contains("broken")
        val isWorkoutSemantic = cleanTitle.contains("gym") || cleanTitle.contains("workout") || cleanTitle.contains("fit") ||
                cleanTitle.contains("power") || cleanTitle.contains("hard") || cleanTitle.contains("motivation")
        val isPartySemantic = cleanTitle.contains("remix") || cleanTitle.contains("club") || cleanTitle.contains("party") ||
                cleanTitle.contains("dance") || cleanTitle.contains("dj") || cleanTitle.contains("bass")

        val classifiedMood: AudioMood = when {
            isWorkoutSemantic || (energyRms > 0.65f && estimatedBpm >= 122 && zeroCrossingRate > 0.08f) -> {
                AudioMood.WORKOUT
            }
            isPartySemantic || (energyRms > 0.58f && spectralBrightness > 0.55f && estimatedBpm >= 115) -> {
                AudioMood.PARTY
            }
            isSadSemantic || (energyRms < 0.32f && estimatedBpm <= 95 && spectralBrightness < 0.35f) -> {
                AudioMood.SAD
            }
            isRomanticSemantic || (estimatedBpm in 72..108 && energyRms in 0.30f..0.62f && spectralBrightness in 0.28f..0.65f) -> {
                AudioMood.ROMANTIC
            }
            (energyRms < 0.38f && spectralBrightness < 0.40f && zeroCrossingRate < 0.06f) -> {
                AudioMood.STUDY
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
            AudioMood.WORKOUT -> matchingSongs.sortedByDescending { profilesCache[it.id]?.energyRms ?: 0f }
            AudioMood.PARTY -> matchingSongs.sortedByDescending { profilesCache[it.id]?.estimatedBpm ?: 0 }
            AudioMood.SAD -> matchingSongs.sortedBy { profilesCache[it.id]?.energyRms ?: 1f }
            AudioMood.ROMANTIC -> matchingSongs.sortedByDescending { it.playCount }
            AudioMood.STUDY -> matchingSongs.sortedBy { profilesCache[it.id]?.zeroCrossingRate ?: 1f }
            AudioMood.CHILL -> matchingSongs.sortedByDescending { it.duration }
        }
    }
}
