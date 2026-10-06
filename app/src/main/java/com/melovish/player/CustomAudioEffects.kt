package com.melovish.player

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// =========================================================================
// 📌 1. THREAD-SAFE AUDIO EFFECTS STATE CONTROLLER
// =========================================================================

object AudioEffectsManager {
    private const val PREFS_NAME = "melovish_custom_audio_effects_prefs"
    private const val KEY_3D_SPATIAL = "key_3d_spatial_enabled"
    private const val KEY_LOFI = "key_lofi_enabled"

    private val _is3DSpatialEnabled = MutableStateFlow(false)
    val is3DSpatialEnabled: StateFlow<Boolean> = _is3DSpatialEnabled.asStateFlow()

    private val _isLofiEnabled = MutableStateFlow(false)
    val isLofiEnabled: StateFlow<Boolean> = _isLofiEnabled.asStateFlow()

    val spatialProcessor = Spatial3DAudioProcessor()
    val lofiProcessor = LofiAudioProcessor()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val spatial = prefs.getBoolean(KEY_3D_SPATIAL, false)
        val lofi = prefs.getBoolean(KEY_LOFI, false)

        _is3DSpatialEnabled.value = spatial
        _isLofiEnabled.value = lofi
    }

    fun set3DSpatialEnabled(enabled: Boolean, context: Context? = null) {
        _is3DSpatialEnabled.value = enabled
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_3D_SPATIAL, enabled)?.apply()
    }

    fun setLofiEnabled(enabled: Boolean, context: Context? = null) {
        _isLofiEnabled.value = enabled
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_LOFI, enabled)?.apply()
    }
}

// =========================================================================
// 📌 2. REAL-TIME 3D SPATIAL ORBIT & BINAURAL AUDIO PROCESSOR
// =========================================================================

@UnstableApi
class Spatial3DAudioProcessor : BaseAudioProcessor() {

    // Orbit parameters
    private var orbitPhase = 0.0
    // Slower rotation: 1 full orbit around head every 12 seconds
    private val orbitSpeedHz = 1.0 / 12.0

    // Interaural Time Difference (ITD) delay buffer (max human ITD is ~0.7 ms, we allocate 4 ms)
    private var itdDelayBufferL = FloatArray(0)
    private var itdDelayBufferR = FloatArray(0)
    private var itdWriteIndex = 0

    // Head-Shadow IIR Low-Pass Filters (Frequency attenuation when sound is on opposite ear)
    private var filterL_x1 = 0.0f
    private var filterL_y1 = 0.0f
    private var filterR_x1 = 0.0f
    private var filterR_y1 = 0.0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate
        val maxDelaySamples = (sampleRate * 0.005f).toInt() // 5ms buffer
        itdDelayBufferL = FloatArray(maxDelaySamples)
        itdDelayBufferR = FloatArray(maxDelaySamples)
        itdWriteIndex = 0

        filterL_x1 = 0f; filterL_y1 = 0f
        filterR_x1 = 0f; filterR_y1 = 0f
        orbitPhase = 0.0

        return AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val isEnabled = AudioEffectsManager.is3DSpatialEnabled.value
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!isEnabled || inputAudioFormat.channelCount != 2) {
            // Bypass mode: Zero latency, zero allocation pass-through
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val sampleRate = inputAudioFormat.sampleRate.toDouble()
        val phaseIncrement = (2.0 * PI * orbitSpeedHz) / sampleRate
        val maxItdSamples = (inputAudioFormat.sampleRate * 0.0007).toFloat() // ~0.7ms maximum interaural time difference
        val bufferCapacity = itdDelayBufferL.size

        val output = replaceOutputBuffer(remaining)

        while (inputBuffer.remaining() >= 4) {
            val s16L = inputBuffer.short
            val s16R = inputBuffer.short

            val inL = s16L / 32768.0f
            val inR = s16R / 32768.0f

            // Mono center collapse for 3D trajectory placement
            val monoSignal = (inL + inR) * 0.5f

            // Current azimuth angle in orbit around the listener
            val azimuth = orbitPhase
            orbitPhase += phaseIncrement
            if (orbitPhase >= 2.0 * PI) orbitPhase -= 2.0 * PI

            val panX = sin(azimuth) // -1.0 (full left) to +1.0 (full right)
            val distance = (1.0 + 0.15 * cos(azimuth)).toFloat() // Elliptical front/back depth distance attenuation

            // Constant-Power Panning Law (ILD - Interaural Level Difference)
            val panNormalized = ((panX + 1.0) * 0.5).coerceIn(0.0, 1.0)
            val ildGainL = (cos(panNormalized * (PI / 2.0)) * (1.0 / distance)).toFloat()
            val ildGainR = (sin(panNormalized * (PI / 2.0)) * (1.0 / distance)).toFloat()

            // Interaural Time Difference (ITD) fractional delay
            val delayL = if (panX > 0) (panX * maxItdSamples).toFloat() else 0.0f
            val delayR = if (panX < 0) (-panX * maxItdSamples).toFloat() else 0.0f

            // Store current frame into delay line
            itdDelayBufferL[itdWriteIndex] = monoSignal
            itdDelayBufferR[itdWriteIndex] = monoSignal

            // Read with linear interpolation for fractional sample delays
            val readPosL = (itdWriteIndex - delayL + bufferCapacity) % bufferCapacity
            val idxL0 = readPosL.toInt()
            val fracL = readPosL - idxL0
            val idxL1 = (idxL0 + 1) % bufferCapacity
            val delayedSampleL = itdDelayBufferL[idxL0] * (1.0f - fracL) + itdDelayBufferL[idxL1] * fracL

            val readPosR = (itdWriteIndex - delayR + bufferCapacity) % bufferCapacity
            val idxR0 = readPosR.toInt()
            val fracR = readPosR - idxR0
            val idxR1 = (idxR0 + 1) % bufferCapacity
            val delayedSampleR = itdDelayBufferR[idxR0] * (1.0f - fracR) + itdDelayBufferR[idxR1] * fracR

            itdWriteIndex = (itdWriteIndex + 1) % bufferCapacity

            // Head-Shadow Spectral Filtering (High-frequency rolloff for the shadowed ear)
            // Cutoff is dynamically modulated by head occlusion
            val alphaL = (0.35f + 0.65f * (1.0f - max(0.0f, panX.toFloat()))).coerceIn(0.15f, 1.0f)
            val alphaR = (0.35f + 0.65f * (1.0f - max(0.0f, -panX.toFloat()))).coerceIn(0.15f, 1.0f)

            filterL_y1 += alphaL * (delayedSampleL - filterL_y1)
            filterR_y1 += alphaR * (delayedSampleR - filterR_y1)

            // Combine spatial cues with ambient preservation
            val outL = (filterL_y1 * ildGainL * 1.25f + inL * 0.15f).coerceIn(-1.0f, 1.0f)
            val outR = (filterR_y1 * ildGainR * 1.25f + inR * 0.15f).coerceIn(-1.0f, 1.0f)

            output.putShort((outL * 32767.0f).toInt().toShort())
            output.putShort((outR * 32767.0f).toInt().toShort())
        }
        output.flip()
    }
}

// =========================================================================
// 📌 3. REAL-TIME VINTAGE LO-FI TAPE & ANALOG VINYL PROCESSOR
// =========================================================================

@UnstableApi
class LofiAudioProcessor : BaseAudioProcessor() {

    // Biquad 2nd-order resonant low-pass filter states (L & R)
    private var lpf_b0 = 0.0f; private var lpf_b1 = 0.0f; private var lpf_b2 = 0.0f
    private var lpf_a1 = 0.0f; private var lpf_a2 = 0.0f
    private var lpfL_x1 = 0.0f; private var lpfL_x2 = 0.0f
    private var lpfL_y1 = 0.0f; private var lpfL_y2 = 0.0f
    private var lpfR_x1 = 0.0f; private var lpfR_x2 = 0.0f
    private var lpfR_y1 = 0.0f; private var lpfR_y2 = 0.0f

    // Wow & Flutter modulated fractional tape delay line
    private var tapeDelayBufferL = FloatArray(0)
    private var tapeDelayBufferR = FloatArray(0)
    private var tapeWriteIndex = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0

    // Vinyl crackle generator state
    private var crackleHold = 0
    private var crackleAmp = 0.0f
    private var pseudoRandomSeed = 123456789L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate

        // Pre-allocate 35ms fractional tape buffer for wow/flutter modulation
        val maxBuffer = (sampleRate * 0.035f).toInt()
        tapeDelayBufferL = FloatArray(maxBuffer)
        tapeDelayBufferR = FloatArray(maxBuffer)
        tapeWriteIndex = 0

        // Compute 2nd-Order Butterworth Low-Pass Filter @ 3800 Hz (Warm vintage tape ceiling)
        computeBiquadLowPass(cutoffHz = 3800.0f, q = 0.707f, sampleRate = sampleRate.toFloat())

        // Reset filter histories
        lpfL_x1 = 0f; lpfL_x2 = 0f; lpfL_y1 = 0f; lpfL_y2 = 0f
        lpfR_x1 = 0f; lpfR_x2 = 0f; lpfR_y1 = 0f; lpfR_y2 = 0f
        wowPhase = 0.0
        flutterPhase = 0.0

        return AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    private fun computeBiquadLowPass(cutoffHz: Float, q: Float, sampleRate: Float) {
        val omega = (2.0 * PI * cutoffHz / sampleRate).toFloat()
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0f * q)

        val a0 = 1.0f + alpha
        lpf_b0 = ((1.0f - cs) / 2.0f) / a0
        lpf_b1 = (1.0f - cs) / a0
        lpf_b2 = ((1.0f - cs) / 2.0f) / a0
        lpf_a1 = (-2.0f * cs) / a0
        lpf_a2 = (1.0f - alpha) / a0
    }

    private fun nextRandom(): Float {
        pseudoRandomSeed = (pseudoRandomSeed * 1103515245L + 12345L) and 0x7fffffffL
        return (pseudoRandomSeed.toFloat() / 0x7fffffffL) * 2.0f - 1.0f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val isEnabled = AudioEffectsManager.isLofiEnabled.value
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!isEnabled || inputAudioFormat.channelCount != 2) {
            // Bypass mode: Zero latency, zero allocation pass-through
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val sampleRate = inputAudioFormat.sampleRate.toDouble()
        val bufferCapacity = tapeDelayBufferL.size
        val output = replaceOutputBuffer(remaining)

        // Wow: 0.65 Hz pitch drift, Flutter: 6.2 Hz micro-vibrato
        val wowInc = (2.0 * PI * 0.65) / sampleRate
        val flutterInc = (2.0 * PI * 6.20) / sampleRate
        val baseDelaySamples = (inputAudioFormat.sampleRate * 0.012f) // 12ms nominal center

        while (inputBuffer.remaining() >= 4) {
            val s16L = inputBuffer.short
            val s16R = inputBuffer.short

            val inL = s16L / 32768.0f
            val inR = s16R / 32768.0f

            // 1. Wow & Flutter Fractional Delay Line
            tapeDelayBufferL[tapeWriteIndex] = inL
            tapeDelayBufferR[tapeWriteIndex] = inR

            val modDepth = (sin(wowPhase) * 18.0 + sin(flutterPhase) * 4.0).toFloat()
            wowPhase += wowInc
            if (wowPhase >= 2.0 * PI) wowPhase -= 2.0 * PI
            flutterPhase += flutterInc
            if (flutterPhase >= 2.0 * PI) flutterPhase -= 2.0 * PI

            val readPos = (tapeWriteIndex - (baseDelaySamples + modDepth) + bufferCapacity) % bufferCapacity
            val idx0 = readPos.toInt()
            val frac = readPos - idx0
            val idx1 = (idx0 + 1) % bufferCapacity

            val delayedL = tapeDelayBufferL[idx0] * (1.0f - frac) + tapeDelayBufferL[idx1] * frac
            val delayedR = tapeDelayBufferR[idx0] * (1.0f - frac) + tapeDelayBufferR[idx1] * frac

            tapeWriteIndex = (tapeWriteIndex + 1) % bufferCapacity

            // 2. Analog 2nd-Order Resonant Low-Pass Filtering
            val filteredL = lpf_b0 * delayedL + lpf_b1 * lpfL_x1 + lpf_b2 * lpfL_x2 - lpf_a1 * lpfL_y1 - lpf_a2 * lpfL_y2
            lpfL_x2 = lpfL_x1; lpfL_x1 = delayedL
            lpfL_y2 = lpfL_y1; lpfL_y1 = filteredL

            val filteredR = lpf_b0 * delayedR + lpf_b1 * lpfR_x1 + lpf_b2 * lpfR_x2 - lpf_a1 * lpfR_y1 - lpf_a2 * lpfR_y2
            lpfR_x2 = lpfR_x1; lpfR_x1 = delayedR
            lpfR_y2 = lpfR_y1; lpfR_y1 = filteredR

            // 3. Smooth Harmonic Soft-Clipping Saturation: f(x) = 1.5x - 0.5x^3
            val satInputL = (filteredL * 1.45f).coerceIn(-1.5f, 1.5f)
            val satInputR = (filteredR * 1.45f).coerceIn(-1.5f, 1.5f)

            val saturatedL = if (abs(satInputL) <= 1.0f) {
                1.5f * satInputL - 0.5f * (satInputL * satInputL * satInputL)
            } else {
                if (satInputL > 0f) 1.0f else -1.0f
            }

            val saturatedR = if (abs(satInputR) <= 1.0f) {
                1.5f * satInputR - 0.5f * (satInputR * satInputR * satInputR)
            } else {
                if (satInputR > 0f) 1.0f else -1.0f
            }

            // 4. Subtle Vinyl Static & Needle Noise
            var vinylNoise = nextRandom() * 0.0028f
            if (crackleHold <= 0) {
                if (nextRandom() > 0.9985f) { // Occasional vinyl dust pop
                    crackleAmp = nextRandom() * 0.024f
                    crackleHold = (sampleRate * 0.0015).toInt()
                }
            } else {
                vinylNoise += crackleAmp
                crackleHold--
            }

            val outL = ((saturatedL * 0.88f) + vinylNoise).coerceIn(-1.0f, 1.0f)
            val outR = ((saturatedR * 0.88f) + vinylNoise).coerceIn(-1.0f, 1.0f)

            output.putShort((outL * 32767.0f).toInt().toShort())
            output.putShort((outR * 32767.0f).toInt().toShort())
        }
        output.flip()
    }
}

// =========================================================================
// 📌 4. JETPACK COMPOSE SETTINGS UI SECTION
// =========================================================================

@Composable
fun AudioEffectsSettingsSection(
    manager: MusicManager,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isSpatialActive by AudioEffectsManager.is3DSpatialEnabled.collectAsState()
    val isLofiActive by AudioEffectsManager.isLofiEnabled.collectAsState()

    val surfaceBg = manager.getCurrentSurfaceColor()
    val glassBorderBrush = manager.getGlassBorderBrush()
    val textColor = manager.getCurrentTextColor()
    val accent = manager.accentColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceBg)
            .border(1.2.dp, glassBorderBrush, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Custom Audio Effects",
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline real-time DSP acoustic filters",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("DSP ENGINE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3D Spatial Audio Orbit Toggle
        AudioEffectToggleRow(
            icon = "🌐",
            title = "3D Spatial Audio",
            description = "Simulates a rotating 360° binaural orbit around your head with Interaural Time & Level Differences.",
            isChecked = isSpatialActive,
            accentColor = accent,
            textColor = textColor,
            onCheckedChange = {
                manager.triggerHapticFeedback(false)
                AudioEffectsManager.set3DSpatialEnabled(it)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lo-Fi Vintage Tape Filter Toggle
        AudioEffectToggleRow(
            icon = "📻",
            title = "Lo-Fi Tape & Vinyl",
            description = "Warm analog tape harmonics, 3.8 kHz biquad ceiling, wow/flutter pitch drift, and vinyl texture.",
            isChecked = isLofiActive,
            accentColor = accent,
            textColor = textColor,
            onCheckedChange = {
                manager.triggerHapticFeedback(false)
                AudioEffectsManager.setLofiEnabled(it)
            }
        )
    }
}

@Composable
private fun AudioEffectToggleRow(
    icon: String,
    title: String,
    description: String,
    isChecked: Boolean,
    accentColor: Color,
    textColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x3364748B)
            )
        )
    }
}
