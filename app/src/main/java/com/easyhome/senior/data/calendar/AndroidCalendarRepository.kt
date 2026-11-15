package com.easyhome.senior.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.AgendaEvent
import com.easyhome.senior.domain.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Legge gli impegni dal calendario di sistema (CalendarContract), tutti gli account del telefono. */
class AndroidCalendarRepository(context: Context) : CalendarRepository {

    private val appContext = context.applicationContext

    override suspend fun upcomingEvents(from: LocalDate, days: Int): List<AgendaEvent>? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        val zone = ZoneId.systemDefault()
        val lastDay = from.plusDays(days.toLong() - 1)
        val startMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = lastDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, startMillis)
            ContentUris.appendId(it, endMillis)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
        )
        val events = mutableListOf<AgendaEvent>()
        try {
            appContext.contentResolver.query(
                uri,
                projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.trim().orEmpty().ifEmpty { "(senza titolo)" }
                    val begin = Instant.ofEpochMilli(c.getLong(1))
                    val end = Instant.ofEpochMilli(c.getLong(2))
                    val allDay = c.getInt(3) == 1
                    val location = c.getString(4)?.trim()?.takeIf { it.isNotEmpty() }
                    if (allDay) {
                        // Gli eventi "tutto il giorno" sono memorizzati in UTC: si mostra ogni giorno coperto.
                        val firstDay = begin.atZone(ZoneOffset.UTC).toLocalDate()
                        val lastCovered = end.atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
                        var day = maxOf(firstDay, from)
                        while (!day.isAfter(minOf(lastCovered, lastDay))) {
                            events += AgendaEvent(title, day, null, null, location)
                            day = day.plusDays(1)
                        }
                    } else {
                        val start = begin.atZone(zone)
                        val finish = end.atZone(zone)
                        // Un impegno iniziato prima di oggi e ancora in corso compare da oggi.
                        val date = maxOf(start.toLocalDate(), from)
                        events += AgendaEvent(title, date, start.toLocalTime(), finish.toLocalTime(), location)
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext null
        }
        events.sortedWith(compareBy({ it.date }, { it.startTime != null }, { it.startTime }))
    }
}
