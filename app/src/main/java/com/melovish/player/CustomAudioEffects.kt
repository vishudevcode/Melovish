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
import androidx.media3.common.PlaybackParameters
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
import kotlin.math.tanh

// =========================================================================
// 📌 1. THREAD-SAFE AUDIO EFFECTS STATE CONTROLLER
// =========================================================================

object AudioEffectsManager {
    private const val PREFS_NAME = "melovish_custom_audio_effects_prefs_v2"
    private const val KEY_3D_SPATIAL = "key_3d_spatial_enabled"
    private const val KEY_LOFI = "key_lofi_enabled"

    private val _is3DSpatialEnabled = MutableStateFlow(false)
    val is3DSpatialEnabled: StateFlow<Boolean> = _is3DSpatialEnabled.asStateFlow()

    private val _isLofiEnabled = MutableStateFlow(false)
    val isLofiEnabled: StateFlow<Boolean> = _isLofiEnabled.asStateFlow()

    val spatialProcessor = Spatial3DAudioProcessor()
    val lofiProcessor = LofiAudioProcessor()

    var onPlaybackSpeedChangeRequested: ((speed: Float, pitch: Float) -> Unit)? = null

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

        if (enabled) {
            onPlaybackSpeedChangeRequested?.invoke(0.83f, 0.88f)
        } else {
            onPlaybackSpeedChangeRequested?.invoke(1.0f, 1.0f)
        }
    }
}

// =========================================================================
// 📌 2. TRUE 3D BINAURAL HRTF & 360° SPATIAL SPHERE PROCESSOR
// =========================================================================

@UnstableApi
class Spatial3DAudioProcessor : BaseAudioProcessor() {

    private var azimuthPhase = 0.0
    private var elevationPhase = 0.0
    private val azimuthSpeedHz = 1.0 / 14.0
    private val elevationSpeedHz = 1.0 / 9.0

    private var delayBufferL = FloatArray(0)
    private var delayBufferR = FloatArray(0)
    private var delayWriteIndex = 0

    private var headShadowL = 0.0f
    private var headShadowR = 0.0f
    private var pinnaNotchL_x1 = 0.0f; private var pinnaNotchL_y1 = 0.0f
    private var pinnaNotchR_x1 = 0.0f; private var pinnaNotchR_y1 = 0.0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate
        val maxDelaySamples = (sampleRate * 0.010f).toInt()
        delayBufferL = FloatArray(maxDelaySamples)
        delayBufferR = FloatArray(maxDelaySamples)
        delayWriteIndex = 0

        headShadowL = 0f; headShadowR = 0f
        pinnaNotchL_x1 = 0f; pinnaNotchL_y1 = 0f
        pinnaNotchR_x1 = 0f; pinnaNotchR_y1 = 0f
        azimuthPhase = 0.0
        elevationPhase = 0.0

        return AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val isEnabled = AudioEffectsManager.is3DSpatialEnabled.value
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!isEnabled || inputAudioFormat.channelCount != 2) {
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val sampleRate = inputAudioFormat.sampleRate.toDouble()
        val azimuthInc = (2.0 * PI * azimuthSpeedHz) / sampleRate
        val elevationInc = (2.0 * PI * elevationSpeedHz) / sampleRate
        val maxItdSamples = (inputAudioFormat.sampleRate * 0.00075f)
        val bufferCapacity = delayBufferL.size

        val output = replaceOutputBuffer(remaining)

        while (inputBuffer.remaining() >= 4) {
            val s16L = inputBuffer.short
            val s16R = inputBuffer.short

            val inL = s16L / 32768.0f
            val inR = s16R / 32768.0f

            azimuthPhase += azimuthInc
            if (azimuthPhase >= 2.0 * PI) azimuthPhase -= 2.0 * PI

            elevationPhase += elevationInc
            if (elevationPhase >= 2.0 * PI) elevationPhase -= 2.0 * PI

            val x = sin(azimuthPhase)
            val y = cos(azimuthPhase)
            val z = sin(elevationPhase) * 0.45

            val distance = (1.0 + 0.20 * y).toFloat()

            val panAngle = (x + 1.0) * 0.5
            val ildL = (cos(panAngle * (PI / 2.0)) * (1.15 / distance)).toFloat()
            val ildR = (sin(panAngle * (PI / 2.0)) * (1.15 / distance)).toFloat()

            val delayL = if (x > 0) (x * maxItdSamples).toFloat() else 0.0f
            val delayR = if (x < 0) (-x * maxItdSamples).toFloat() else 0.0f

            delayBufferL[delayWriteIndex] = inL
            delayBufferR[delayWriteIndex] = inR

            val readPosL = (delayWriteIndex - delayL + bufferCapacity) % bufferCapacity
            val idxL0 = readPosL.toInt()
            val fracL = readPosL - idxL0
            val idxL1 = (idxL0 + 1) % bufferCapacity
            val delayedSampleL = delayBufferL[idxL0] * (1.0f - fracL) + delayBufferL[idxL1] * fracL

            val readPosR = (delayWriteIndex - delayR + bufferCapacity) % bufferCapacity
            val idxR0 = readPosR.toInt()
            val fracR = readPosR - idxR0
            val idxR1 = (idxR0 + 1) % bufferCapacity
            val delayedSampleR = delayBufferR[idxR0] * (1.0f - fracR) + delayBufferR[idxR1] * fracR

            delayWriteIndex = (delayWriteIndex + 1) % bufferCapacity

            val shadowCoeffL = (0.28f + 0.72f * (1.0f - max(0.0f, x.toFloat()))).coerceIn(0.12f, 1.0f)
            val shadowCoeffR = (0.28f + 0.72f * (1.0f - max(0.0f, -x.toFloat()))).coerceIn(0.12f, 1.0f)

            headShadowL += shadowCoeffL * (delayedSampleL - headShadowL)
            headShadowR += shadowCoeffR * (delayedSampleR - headShadowR)

            val pinnaDepth = ((1.0f - y.toFloat()) * 0.5f + (z.toFloat() * 0.5f)).coerceIn(0.0f, 1.0f)
            val notchAlpha = 0.40f * pinnaDepth

            val notchedL = headShadowL - notchAlpha * pinnaNotchL_x1
            pinnaNotchL_x1 = headShadowL
            val notchedR = headShadowR - notchAlpha * pinnaNotchR_x1
            pinnaNotchR_x1 = headShadowR

            val crossfeedGain = 0.18f
            val binauralL = (notchedL * ildL) + (notchedR * ildR * crossfeedGain)
            val binauralR = (notchedR * ildR) + (notchedL * ildL * crossfeedGain)

            val outL = (binauralL * 1.12f + inL * 0.22f).coerceIn(-1.0f, 1.0f)
            val outR = (binauralR * 1.12f + inR * 0.22f).coerceIn(-1.0f, 1.0f)

            output.putShort((outL * 32767.0f).toInt().toShort())
            output.putShort((outR * 32767.0f).toInt().toShort())
        }
        output.flip()
    }
}

// =========================================================================
// 📌 3. MELODIOUS LO-FI TAPE, REVERB DIFFUSION & WARMTH PROCESSOR
// =========================================================================

@UnstableApi
class LofiAudioProcessor : BaseAudioProcessor() {

    private var lpf_b0 = 0.0f; private var lpf_b1 = 0.0f; private var lpf_b2 = 0.0f
    private var lpf_a1 = 0.0f; private var lpf_a2 = 0.0f
    private var lpfL_x1 = 0.0f; private var lpfL_x2 = 0.0f
    private var lpfL_y1 = 0.0f; private var lpfL_y2 = 0.0f
    private var lpfR_x1 = 0.0f; private var lpfR_x2 = 0.0f
    private var lpfR_y1 = 0.0f; private var lpfR_y2 = 0.0f

    private var mid_b0 = 0.0f; private var mid_b1 = 0.0f; private var mid_b2 = 0.0f
    private var mid_a1 = 0.0f; private var mid_a2 = 0.0f
    private var midL_x1 = 0f; private var midL_x2 = 0f; private var midL_y1 = 0f; private var midL_y2 = 0f
    private var midR_x1 = 0f; private var midR_x2 = 0f; private var midR_y1 = 0f; private var midR_y2 = 0f

    private var tapeBufferL = FloatArray(0)
    private var tapeBufferR = FloatArray(0)
    private var tapeWriteIndex = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0

    private var combBuffer1 = FloatArray(0)
    private var combBuffer2 = FloatArray(0)
    private var combBuffer3 = FloatArray(0)
    private var combBuffer4 = FloatArray(0)
    private var combIdx1 = 0; private var combIdx2 = 0; private var combIdx3 = 0; private var combIdx4 = 0

    private var crackleHold = 0
    private var crackleAmp = 0.0f
    private var pseudoRandomSeed = 987654321L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate

        val maxTapeBuffer = (sampleRate * 0.040f).toInt()
        tapeBufferL = FloatArray(maxTapeBuffer)
        tapeBufferR = FloatArray(maxTapeBuffer)
        tapeWriteIndex = 0

        combBuffer1 = FloatArray((sampleRate * 0.0297f).toInt())
        combBuffer2 = FloatArray((sampleRate * 0.0371f).toInt())
        combBuffer3 = FloatArray((sampleRate * 0.0411f).toInt())
        combBuffer4 = FloatArray((sampleRate * 0.0437f).toInt())
        combIdx1 = 0; combIdx2 = 0; combIdx3 = 0; combIdx4 = 0

        computeBiquadLowPass(cutoffHz = 5500.0f, q = 0.65f, sampleRate = sampleRate.toFloat())
        computeBiquadPeak(centerHz = 350.0f, gainDb = 2.8f, q = 1.0f, sampleRate = sampleRate.toFloat())

        lpfL_x1 = 0f; lpfL_x2 = 0f; lpfL_y1 = 0f; lpfL_y2 = 0f
        lpfR_x1 = 0f; lpfR_x2 = 0f; lpfR_y1 = 0f; lpfR_y2 = 0f
        midL_x1 = 0f; midL_x2 = 0f; midL_y1 = 0f; midL_y2 = 0f
        midR_x1 = 0f; midR_x2 = 0f; midR_y1 = 0f; midR_y2 = 0f
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

    private fun computeBiquadPeak(centerHz: Float, gainDb: Float, q: Float, sampleRate: Float) {
        val a = sqrt(Math.pow(10.0, (gainDb / 20.0).toDouble())).toFloat()
        val omega = (2.0 * PI * centerHz / sampleRate).toFloat()
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0f * q)

        val a0 = 1.0f + (alpha / a)
        mid_b0 = (1.0f + alpha * a) / a0
        mid_b1 = (-2.0f * cs) / a0
        mid_b2 = (1.0f - alpha * a) / a0
        mid_a1 = (-2.0f * cs) / a0
        mid_a2 = (1.0f - (alpha / a)) / a0
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
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val sampleRate = inputAudioFormat.sampleRate.toDouble()
        val bufferCapacity = tapeBufferL.size
        val output = replaceOutputBuffer(remaining)

        val wowInc = (2.0 * PI * 0.42) / sampleRate
        val flutterInc = (2.0 * PI * 4.80) / sampleRate
        val baseDelay = (inputAudioFormat.sampleRate * 0.010f)

        while (inputBuffer.remaining() >= 4) {
            val s16L = inputBuffer.short
            val s16R = inputBuffer.short

            val inL = s16L / 32768.0f
            val inR = s16R / 32768.0f

            tapeBufferL[tapeWriteIndex] = inL
            tapeBufferR[tapeWriteIndex] = inR

            val modL = (sin(wowPhase) * 12.0 + sin(flutterPhase) * 2.2).toFloat()
            val modR = (sin(wowPhase + 0.65) * 12.0 + sin(flutterPhase + 0.85) * 2.2).toFloat()

            wowPhase += wowInc
            if (wowPhase >= 2.0 * PI) wowPhase -= 2.0 * PI
            flutterPhase += flutterInc
            if (flutterPhase >= 2.0 * PI) flutterPhase -= 2.0 * PI

            val readPosL = (tapeWriteIndex - (baseDelay + modL) + bufferCapacity) % bufferCapacity
            val idxL0 = readPosL.toInt()
            val fracL = readPosL - idxL0
            val idxL1 = (idxL0 + 1) % bufferCapacity
            val wowL = tapeBufferL[idxL0] * (1.0f - fracL) + tapeBufferL[idxL1] * fracL

            val readPosR = (tapeWriteIndex - (baseDelay + modR) + bufferCapacity) % bufferCapacity
            val idxR0 = readPosR.toInt()
            val fracR = readPosR - idxR0
            val idxR1 = (idxR0 + 1) % bufferCapacity
            val wowR = tapeBufferR[idxR0] * (1.0f - fracR) + tapeBufferR[idxR1] * fracR

            tapeWriteIndex = (tapeWriteIndex + 1) % bufferCapacity

            val midL = mid_b0 * wowL + mid_b1 * midL_x1 + mid_b2 * midL_x2 - mid_a1 * midL_y1 - mid_a2 * midL_y2
            midL_x2 = midL_x1; midL_x1 = wowL; midL_y2 = midL_y1; midL_y1 = midL

            val midR = mid_b0 * wowR + mid_b1 * midR_x1 + mid_b2 * midR_x2 - mid_a1 * midR_y1 - mid_a2 * midR_y2
            midR_x2 = midR_x1; midR_x1 = wowR; midR_y2 = midR_y1; midR_y1 = midR

            val warmL = lpf_b0 * midL + lpf_b1 * lpfL_x1 + lpf_b2 * lpfL_x2 - lpf_a1 * lpfL_y1 - lpf_a2 * lpfL_y2
            lpfL_x2 = lpfL_x1; lpfL_x1 = midL; lpfL_y2 = lpfL_y1; lpfL_y1 = warmL

            val warmR = lpf_b0 * midR + lpf_b1 * lpfR_x1 + lpf_b2 * lpfR_x2 - lpf_a1 * lpfR_y1 - lpf_a2 * lpfR_y2
            lpfR_x2 = lpfR_x1; lpfR_x1 = midR; lpfR_y2 = lpfR_y1; lpfR_y1 = warmR

            val saturatedL = tanh((warmL * 1.15f).toDouble()).toFloat()
            val saturatedR = tanh((warmR * 1.15f).toDouble()).toFloat()

            val monoReverbIn = (saturatedL + saturatedR) * 0.45f
            val fb = 0.76f

            val c1 = combBuffer1[combIdx1]
            combBuffer1[combIdx1] = monoReverbIn + c1 * fb
            combIdx1 = (combIdx1 + 1) % combBuffer1.size

            val c2 = combBuffer2[combIdx2]
            combBuffer2[combIdx2] = monoReverbIn + c2 * fb
            combIdx2 = (combIdx2 + 1) % combBuffer2.size

            val c3 = combBuffer3[combIdx3]
            combBuffer3[combIdx3] = monoReverbIn + c3 * fb
            combIdx3 = (combIdx3 + 1) % combBuffer3.size

            val c4 = combBuffer4[combIdx4]
            combBuffer4[combIdx4] = monoReverbIn + c4 * fb
            combIdx4 = (combIdx4 + 1) % combBuffer4.size

            val reverbTailL = (c1 - c2 + c3) * 0.18f
            val reverbTailR = (c2 - c3 + c4) * 0.18f

            var vinylNoise = nextRandom() * 0.0012f
            if (crackleHold <= 0) {
                if (nextRandom() > 0.9991f) {
                    crackleAmp = nextRandom() * 0.015f
                    crackleHold = (sampleRate * 0.0012).toInt()
                }
            } else {
                vinylNoise += crackleAmp
                crackleHold--
            }

            val outL = (saturatedL * 0.82f + reverbTailL + vinylNoise).coerceIn(-1.0f, 1.0f)
            val outR = (saturatedR * 0.82f + reverbTailR + vinylNoise).coerceIn(-1.0f, 1.0f)

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
                    text = "Real-time 360° binaural sphere & melodious Lo-Fi engine",
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
                Text("PRO DSP", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3D Spatial Audio Orbit Toggle
        AudioEffectToggleRow(
            icon = "🌐",
            title = "3D Spatial Audio",
            description = "Simulates a rotating 360° sound",
            isChecked = isSpatialActive,
            accentColor = accent,
            textColor = textColor,
            onCheckedChange = {
                manager.triggerHapticFeedback(false)
                AudioEffectsManager.set3DSpatialEnabled(it, manager.context)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lo-Fi Vintage Tape Filter Toggle
        AudioEffectToggleRow(
            icon = "📻",
            title = "Lofi Audio",
            description = "Warm nostalgic tape hiss vinyl old sound",
            isChecked = isLofiActive,
            accentColor = accent,
            textColor = textColor,
            onCheckedChange = {
                manager.triggerHapticFeedback(false)
                AudioEffectsManager.setLofiEnabled(it, manager.context)
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
