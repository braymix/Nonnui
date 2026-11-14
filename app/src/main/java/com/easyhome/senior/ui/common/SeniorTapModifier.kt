package com.easyhome.senior.ui.common

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Rilevatore di tocco tollerante per utenti anziani:
 *  - i micro-trascinamenti vengono ignorati: il tocco è valido se il dito viene sollevato
 *    dentro il pulsante, allargato di [slop] (molto più del touch-slop di sistema);
 *  - l'azione parte sia su tap rapido sia su pressione prolungata accidentale (al rilascio);
 *  - i doppi tocchi involontari (tremore) entro [debounceMs] vengono scartati.
 */
fun Modifier.seniorTap(
    enabled: Boolean = true,
    slop: Dp = 48.dp,
    debounceMs: Long = 700L,
    onPressedChange: (Boolean) -> Unit = {},
    onTap: () -> Unit,
): Modifier = composed {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnPressed by rememberUpdatedState(onPressedChange)
    val lastTapAt = remember { longArrayOf(0L) }

    pointerInput(enabled, slop, debounceMs) {
        if (!enabled) return@pointerInput
        val slopPx = slop.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            currentOnPressed(true)
            var lastPosition: Offset = down.position
            var released = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull { it.id == down.id }?.let { lastPosition = it.position }
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) {
                        released = true
                        break
                    }
                }
            } finally {
                currentOnPressed(false)
            }
            if (!released) return@awaitEachGesture
            val insideTolerantBounds =
                lastPosition.x >= -slopPx && lastPosition.x <= size.width + slopPx &&
                    lastPosition.y >= -slopPx && lastPosition.y <= size.height + slopPx
            val now = SystemClock.uptimeMillis()
            if (insideTolerantBounds && now - lastTapAt[0] >= debounceMs) {
                lastTapAt[0] = now
                currentOnTap()
            }
        }
    }
}

/**
 * Pressione con ripetizione automatica (tasto CANCELLA): un'azione subito al tocco,
 * poi ripetizioni continue finché il dito resta appoggiato.
 */
fun Modifier.repeatingPress(
    initialDelayMs: Long = 500L,
    repeatIntervalMs: Long = 90L,
    onPressedChange: (Boolean) -> Unit = {},
    onAction: () -> Unit,
): Modifier = composed {
    val currentAction by rememberUpdatedState(onAction)
    val currentOnPressed by rememberUpdatedState(onPressedChange)

    pointerInput(initialDelayMs, repeatIntervalMs) {
        coroutineScope {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                currentOnPressed(true)
                currentAction()
                val repeatJob = launch {
                    delay(initialDelayMs)
                    while (isActive) {
                        currentAction()
                        delay(repeatIntervalMs)
                    }
                }
                try {
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                } finally {
                    repeatJob.cancel()
                    currentOnPressed(false)
                }
            }
        }
    }
}
