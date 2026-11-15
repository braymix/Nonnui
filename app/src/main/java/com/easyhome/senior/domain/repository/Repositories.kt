package com.easyhome.senior.domain.repository

import android.graphics.Bitmap
import com.easyhome.senior.domain.model.AgendaEvent
import com.easyhome.senior.domain.model.AppShortcut
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.model.LauncherConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface ConfigRepository {
    val config: Flow<LauncherConfig>

    /** Aggiornamento atomico: la trasformazione riceve la configurazione corrente. */
    suspend fun update(transform: (LauncherConfig) -> LauncherConfig)

    suspend fun replace(config: LauncherConfig)
}

interface AppsRepository {
    suspend fun getLaunchableApps(): List<InstalledApp>

    /** Icona dell'app o, se [shortcutId] è valorizzato, della scorciatoia. */
    suspend fun loadIcon(packageName: String, shortcutId: String?, sizePx: Int): Bitmap?
    fun isInstalled(packageName: String): Boolean

    /** True se il launcher può leggere/avviare le scorciatoie (cioè è il launcher predefinito). */
    fun canUseShortcuts(): Boolean

    /** Scorciatoie disponibili (chat recenti, gruppi, azioni rapide) di tutte le app installate. */
    suspend fun getShortcuts(): List<AppShortcut>

    /** Mantiene "fissate" le scorciatoie usate dai pulsanti, così non spariscono dalla lista dell'app. */
    suspend fun setPinnedShortcuts(packageName: String, shortcutIds: List<String>)
}

interface DeviceStatusRepository {
    val battery: Flow<BatteryStatus>
    val isOnline: Flow<Boolean>
    /** Emette l'ora corrente troncata al minuto, solo quando cambia. */
    val clock: Flow<LocalDateTime>
}

interface CalendarRepository {
    /** Impegni da [from] per [days] giorni, in ordine; null se manca il permesso Calendario. */
    suspend fun upcomingEvents(from: LocalDate, days: Int): List<AgendaEvent>?
}
