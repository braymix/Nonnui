package com.easyhome.senior.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.repository.AppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerAppsRepository(context: Context) : AppsRepository {

    private val packageManager: PackageManager = context.packageManager
    private val ownPackage: String = context.packageName
    private val iconCache = LruCache<String, Bitmap>(48)

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

    override suspend fun loadIcon(packageName: String, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$packageName@$sizePx"
        iconCache.get(key) ?: try {
            packageManager.getApplicationIcon(packageName)
                .toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                .also { iconCache.put(key, it) }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun isInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null
}
