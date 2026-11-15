package com.easyhome.senior.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class BatteryStatus(val percent: Int, val isCharging: Boolean) {
    val isLow: Boolean get() = percent < LOW_THRESHOLD && !isCharging

    companion object {
        const val LOW_THRESHOLD = 20
    }
}

data class InstalledApp(val packageName: String, val label: String)

data class PickedContact(val contactId: Long, val name: String, val phoneNumber: String)

/** Scorciatoia pubblicata da un'app e leggibile dal launcher predefinito (LauncherApps). */
data class AppShortcut(
    val packageName: String,
    val appLabel: String,
    val shortcutId: String,
    val label: String,
)

/** Un impegno del calendario di sistema, già riportato al giorno in cui mostrarlo. */
data class AgendaEvent(
    val title: String,
    val date: LocalDate,
    /** null = evento di tutto il giorno. */
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val location: String?,
)
