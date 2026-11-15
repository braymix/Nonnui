package com.easyhome.senior.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.easyhome.senior.domain.model.AppShortcut
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.repository.AppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerAppsRepository(context: Context) : AppsRepository {

    private val packageManager: PackageManager = context.packageManager
    private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java)
    private val ownPackage: String = context.packageName
    private val densityDpi: Int = context.resources.displayMetrics.densityDpi
    private val iconCache = LruCache<String, Bitmap>(64)

    override suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        resolved
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.packageName != ownPackage }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override suspend fun loadIcon(packageName: String, shortcutId: String?, sizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "$packageName/${shortcutId.orEmpty()}@$sizePx"
            iconCache.get(key)?.let { return@withContext it }
            val drawable = shortcutId?.let { id ->
                findShortcut(packageName, id)?.let { info ->
                    try {
                        launcherApps?.getShortcutIconDrawable(info, densityDpi)
                    } catch (_: RuntimeException) {
                        null
                    }
                }
            } ?: try {
                packageManager.getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
            drawable?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)?.also { iconCache.put(key, it) }
        }

    override fun isInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    override fun canUseShortcuts(): Boolean = try {
        launcherApps?.hasShortcutHostPermission() == true
    } catch (_: RuntimeException) {
        false
    }

    override suspend fun getShortcuts(): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (!canUseShortcuts()) return@withContext emptyList()
        getLaunchableApps().flatMap { app ->
            queryShortcuts(app.packageName, null).map { info ->
                AppShortcut(
                    packageName = app.packageName,
                    appLabel = app.label,
                    shortcutId = info.id,
                    label = (info.longLabel ?: info.shortLabel ?: info.id).toString(),
                )
            }
        }.distinctBy { it.packageName to it.shortcutId }
    }

    override suspend fun setPinnedShortcuts(packageName: String, shortcutIds: List<String>) {
        withContext(Dispatchers.IO) {
            if (!canUseShortcuts()) return@withContext
            try {
                launcherApps?.pinShortcuts(packageName, shortcutIds.distinct(), Process.myUserHandle())
            } catch (_: RuntimeException) {
                // App disinstallata o scorciatoie disabilitate: il pulsante userà il ripiego.
            }
        }
    }

    private fun findShortcut(packageName: String, shortcutId: String): ShortcutInfo? =
        queryShortcuts(packageName, listOf(shortcutId)).firstOrNull()

    private fun queryShortcuts(packageName: String, ids: List<String>?): List<ShortcutInfo> {
        val apps = launcherApps ?: return emptyList()
        if (!canUseShortcuts()) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        if (ids != null) query.setShortcutIds(ids)
        return try {
            apps.getShortcuts(query, Process.myUserHandle()).orEmpty().filter { it.isEnabled }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }
}
