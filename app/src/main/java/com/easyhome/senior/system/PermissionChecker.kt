package com.easyhome.senior.system

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val isDefaultLauncher: Boolean = false,
    val isKeyboardEnabled: Boolean = false,
    val isKeyboardSelected: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasCallPermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val canWriteSystemSettings: Boolean = false,
) {
    val isKeyboardReady: Boolean get() = isKeyboardEnabled && isKeyboardSelected
}

/** Diagnostica del "semaforo permessi" e Intent per correggere ciascuna voce. */
class PermissionChecker(private val context: Context) {

    private val packageName = context.packageName

    fun currentStatus(): PermissionStatus = PermissionStatus(
        isDefaultLauncher = isDefaultLauncher(),
        isKeyboardEnabled = isKeyboardEnabled(),
        isKeyboardSelected = isKeyboardSelected(),
        canDrawOverlays = Settings.canDrawOverlays(context),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
        hasCallPermission = isGranted(Manifest.permission.CALL_PHONE),
        hasContactsPermission = isGranted(Manifest.permission.READ_CONTACTS),
        canWriteSystemSettings = Settings.System.canWrite(context),
    )

    fun isDefaultLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    fun isKeyboardEnabled(): Boolean {
        val imm = context.getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    fun isKeyboardSelected(): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?: return false
        return ComponentName.unflattenFromString(current)?.packageName == packageName
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // ---- Intent di configurazione -------------------------------------------------------

    fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)

    fun inputMethodSettingsIntent(): Intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)

    fun overlaySettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))

    @Suppress("BatteryLife")
    fun batteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))

    fun batteryOptimizationFallbackIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    fun showInputMethodPicker() {
        context.getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }
}

/** Avvia un Intent di sistema con ripiego, senza mai far crashare il configuratore. */
fun Context.safeStartActivity(intent: Intent, fallback: Intent? = null): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        fallback?.let { safeStartActivity(it, null) } ?: false
    } catch (_: SecurityException) {
        fallback?.let { safeStartActivity(it, null) } ?: false
    }
}
