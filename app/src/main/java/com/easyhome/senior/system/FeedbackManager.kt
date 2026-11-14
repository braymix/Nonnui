package com.easyhome.senior.system

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.VibrationIntensity

/** Feedback aptico e sonoro "deciso", pensato per utenti con sensibilità ridotta. */
class FeedbackManager(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val toneLock = Any()
    private var toneGenerator: ToneGenerator? = null

    /** Tocco su un pulsante gigante della Home o sul tasto "Torna all'inizio". */
    fun buttonFeedback(config: LauncherConfig) {
        vibrate(config.vibration, short = false)
        if (config.soundEnabled) beep()
    }

    /** Tocco su un tasto della tastiera ABCD. */
    fun keyFeedback(config: LauncherConfig) {
        vibrate(config.vibration, short = true)
        if (config.soundEnabled) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
        }
    }

    fun vibrate(intensity: VibrationIntensity, short: Boolean) {
        val v = vibrator ?: return
        if (intensity == VibrationIntensity.OFF || !v.hasVibrator()) return
        val (durationMs, amplitude) = when (intensity) {
            VibrationIntensity.LOW -> 30L to 90
            VibrationIntensity.MEDIUM -> 50L to 170
            VibrationIntensity.HIGH -> 80L to 255
            VibrationIntensity.OFF -> return
        }
        val duration = if (short) (durationMs * 0.6f).toLong().coerceAtLeast(20L) else durationMs
        val amp = if (v.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        try {
            v.vibrate(VibrationEffect.createOneShot(duration, amp))
        } catch (_: SecurityException) {
            // Permesso VIBRATE revocato da policy: il feedback aptico è opzionale.
        }
    }

    fun beep() {
        synchronized(toneLock) {
            val generator = toneGenerator ?: try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).also { toneGenerator = it }
            } catch (_: RuntimeException) {
                null // Alcuni dispositivi esauriscono le risorse audio: si prosegue senza suono.
            }
            generator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        }
    }
}
