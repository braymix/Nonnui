package com.easyhome.senior.ui.home

import com.easyhome.senior.domain.model.AgendaEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AgendaFormatterTest {

    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun `etichette dei giorni in italiano senza abbreviazioni`() {
        assertEquals("OGGI", AgendaFormatter.dayLabel(today, today))
        assertEquals("DOMANI", AgendaFormatter.dayLabel(today.plusDays(1), today))
        assertEquals("MARTEDÌ 17 NOVEMBRE", AgendaFormatter.dayLabel(today.plusDays(2), today))
    }

    @Test
    fun `orario o tutto il giorno`() {
        assertEquals("09:05", AgendaFormatter.timeLabel(AgendaEvent("Medico", today, LocalTime.of(9, 5), null, null)))
        assertEquals("TUTTO IL GIORNO", AgendaFormatter.timeLabel(AgendaEvent("Compleanno", today, null, null, null)))
    }

    @Test
    fun `raggruppa per giorno in ordine cronologico`() {
        val events = listOf(
            AgendaEvent("B", today.plusDays(2), LocalTime.NOON, null, null),
            AgendaEvent("A", today, LocalTime.NOON, null, null),
            AgendaEvent("C", today, null, null, null),
        )
        val groups = AgendaFormatter.groupByDay(events)
        assertEquals(listOf(today, today.plusDays(2)), groups.map { it.first })
        assertEquals(2, groups.first().second.size)
    }
}
