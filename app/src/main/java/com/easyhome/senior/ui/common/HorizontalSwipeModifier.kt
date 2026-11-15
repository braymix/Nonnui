package com.easyhome.senior.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Swipe orizzontale deliberato, osservato PRIMA dei figli (passaggio Initial):
 *  - scatta solo con uno spostamento ampio (30% della larghezza, minimo 110dp) e quasi orizzontale;
 *  - [towardRight] = true -> dito da sinistra verso destra;
 *  - una volta riconosciuto, consuma il gesto: il pulsante sotto al dito NON viene premuto.
 */
fun Modifier.horizontalSwipe(
    enabled: Boolean,
    towardRight: Boolean,
    onSwipe: () -> Unit,
): Modifier = composed {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    pointerInput(enabled, towardRight) {
        if (!enabled) return@pointerInput
        val threshold = maxOf(size.width * 0.30f, 110.dp.toPx())
        val direction = if (towardRight) 1f else -1f
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var triggered = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // Più dita = non è uno swipe intenzionale.
                if (event.changes.count { it.pressed } > 1 && !triggered) break
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!triggered && dx * direction > threshold && abs(dx) > 2f * abs(dy)) {
                    triggered = true
                    currentOnSwipe()
                }
                if (triggered) event.changes.forEach { it.consume() }
                if (!change.pressed) break
            }
        }
    }
}
