package com.easyhome.senior.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import com.easyhome.senior.domain.model.BrightnessMode
import com.easyhome.senior.domain.model.LauncherConfig

/**
 * Luminosità: adattiva (sensore di luce) o fissa, applicata a tutto il telefono tramite
 * Settings.System (permesso "Modifica impostazioni di sistema"). Senza permesso, la luminosità
 * fissa viene comunque applicata almeno alla schermata Home.
 */
class BrightnessController(private val context: Context) {

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun writeSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** @return true se l'impostazione di sistema è stata applicata. */
    fun applySystem(config: LauncherConfig): Boolean {
        if (config.brightnessMode == BrightnessMode.SYSTEM || !canWriteSystemSettings()) return false
        val resolver = context.contentResolver
        return try {
            when (config.brightnessMode) {
                BrightnessMode.AUTO -> Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                )
                BrightnessMode.FIXED -> {
                    Settings.System.putInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    )
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, toSystemValue(config.brightnessLevel))
                }
                BrightnessMode.SYSTEM -> false
            }
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /** Luminosità della sola finestra: usata quando il permesso di sistema manca. */
    fun applyToWindow(activity: Activity, config: LauncherConfig) {
        val attributes = activity.window.attributes
        val target = if (config.brightnessMode == BrightnessMode.FIXED && !canWriteSystemSettings()) {
            config.brightnessLevel / 100f
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        if (attributes.screenBrightness != target) {
            attributes.screenBrightness = target
            activity.window.attributes = attributes
        }
    }

    private fun toSystemValue(percent: Int): Int =
        (percent.coerceIn(LauncherConfig.MIN_BRIGHTNESS, LauncherConfig.MAX_BRIGHTNESS) * 255 / 100).coerceIn(1, 255)
}
