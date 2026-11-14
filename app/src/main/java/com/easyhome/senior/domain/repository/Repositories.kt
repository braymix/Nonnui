package com.easyhome.senior.domain.repository

import android.graphics.Bitmap
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.model.LauncherConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ConfigRepository {
    val config: Flow<LauncherConfig>

    /** Aggiornamento atomico: la trasformazione riceve la configurazione corrente. */
    suspend fun update(transform: (LauncherConfig) -> LauncherConfig)

    suspend fun replace(config: LauncherConfig)
}

interface AppsRepository {
    suspend fun getLaunchableApps(): List<InstalledApp>
    suspend fun loadIcon(packageName: String, sizePx: Int): Bitmap?
    fun isInstalled(packageName: String): Boolean
}

interface DeviceStatusRepository {
    val battery: Flow<BatteryStatus>
    val isOnline: Flow<Boolean>
    /** Emette l'ora corrente troncata al minuto, solo quando cambia. */
    val clock: Flow<LocalDateTime>
}
