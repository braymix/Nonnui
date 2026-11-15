package com.easyhome.senior.ui.home

import com.easyhome.senior.domain.model.AgendaEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Testi dell'agenda in italiano, senza abbreviazioni. */
object AgendaFormatter {
    private val DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)

    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "OGGI"
        today.plusDays(1) -> "DOMANI"
        else -> date.format(DAY).uppercase(Locale.ITALIAN)
    }

    fun timeLabel(event: AgendaEvent): String =
        event.startTime?.format(TIME) ?: "TUTTO IL GIORNO"

    /** Impegni raggruppati per giorno, nell'ordine in cui vanno mostrati. */
    fun groupByDay(events: List<AgendaEvent>): List<Pair<LocalDate, List<AgendaEvent>>> =
        events.groupBy { it.date }.toSortedMap().map { (day, list) -> day to list }
}
