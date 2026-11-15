package com.easyhome.senior.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.AgendaEvent
import com.easyhome.senior.ui.common.AutoSizeText
import com.easyhome.senior.ui.common.horizontalSwipe
import com.easyhome.senior.ui.common.seniorTap
import com.easyhome.senior.ui.theme.SeniorTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Agenda semplificata: impegni dei prossimi 7 giorni presi dal calendario di sistema,
 * a caratteri giganti. Si chiude con il tasto grande, con Indietro o con lo swipe opposto.
 */
@Composable
fun AgendaScreen(
    state: AgendaUiState,
    fontSizeSp: Int,
    closeTowardRight: Boolean,
    onOpenSystemCalendar: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SeniorTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .horizontalSwipe(enabled = true, towardRight = closeTowardRight, onSwipe = onClose),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.topBar)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "CALENDARIO",
                color = colors.onTopBar,
                fontSize = (fontSizeSp * 1.1f).sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                state.today.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN)).uppercase(Locale.ITALIAN),
                color = colors.onTopBar,
                fontSize = (fontSizeSp * 0.68f).sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.onBackground)
                state.needsPermission -> CenteredMessage(
                    "IMPEGNI NON DISPONIBILI",
                    "Per i familiari: concedere il permesso Calendario nella configurazione (passo 3).",
                    fontSizeSp,
                )
                state.events.isEmpty() -> CenteredMessage(
                    "NESSUN IMPEGNO",
                    "Nei prossimi ${HomeViewModel.AGENDA_DAYS} giorni non ci sono appuntamenti.",
                    fontSizeSp,
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AgendaFormatter.groupByDay(state.events).forEach { (day, events) ->
                        item(key = "day-$day") {
                            val label = AgendaFormatter.dayLabel(day, state.today)
                            Text(
                                label,
                                color = if (day == state.today) colors.danger else colors.onBackground,
                                fontSize = (fontSizeSp * 0.85f).sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .semantics { heading() },
                            )
                        }
                        items(events, key = { "${it.date}-${it.startTime}-${it.title}-${it.location}" }) { event ->
                            EventCard(event, fontSizeSp)
                        }
                    }
                }
            }
        }

        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AgendaButton(
                text = "CALENDARIO DEL TELEFONO",
                background = colors.topBar,
                foreground = colors.onTopBar,
                onClick = onOpenSystemCalendar,
            )
            AgendaButton(
                text = "🏠 TORNA ALL'INIZIO",
                background = colors.success,
                foreground = colors.onSuccess,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun CenteredMessage(title: String, subtitle: String, fontSizeSp: Int) {
    val colors = SeniorTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = colors.onBackground, fontSize = fontSizeSp.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(subtitle, color = colors.onBackground, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EventCard(event: AgendaEvent, fontSizeSp: Int) {
    val colors = SeniorTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val time = AgendaFormatter.timeLabel(event)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (colors.isDark) Color.Black else Color.White)
            .border(3.dp, colors.border, shape)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$time, ${event.title}" + (event.location?.let { ", a $it" } ?: "")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            time,
            color = colors.onBackground,
            fontSize = if (event.startTime == null) (fontSizeSp * 0.5f).sp else (fontSizeSp * 0.85f).sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.title.uppercase(Locale.ITALIAN),
                color = colors.onBackground,
                fontSize = (fontSizeSp * 0.75f).sp,
                lineHeight = (fontSizeSp * 0.85f).sp,
                fontWeight = FontWeight.Bold,
            )
            event.location?.let {
                Text(it, color = colors.onBackground, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun AgendaButton(text: String, background: Color, foreground: Color, onClick: () -> Unit) {
    val colors = SeniorTheme.colors
    val shape = RoundedCornerShape(20.dp)
    var pressed by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(if (pressed) foreground else background)
            .border(4.dp, colors.border, shape)
            .semantics {
                role = Role.Button
                contentDescription = text.replace("🏠", "").trim()
            }
            .seniorTap(onPressedChange = { pressed = it }, onTap = onClick),
    ) {
        AutoSizeText(
            text = text,
            maxFontSize = 24.sp,
            color = if (pressed) background else foreground,
            maxLines = 1,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
        )
    }
}
