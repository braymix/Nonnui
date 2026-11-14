package com.easyhome.senior.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.ui.common.BatteryIcon
import com.easyhome.senior.ui.theme.SeniorTheme
import kotlinx.coroutines.launch

private const val ADMIN_HOLD_MS = 5_000

@Composable
fun StatusTopBar(
    timeText: String,
    dateText: String,
    battery: BatteryStatus,
    fontSizeSp: Int,
    interactive: Boolean,
    onAdminHoldCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SeniorTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdminHoldClock(
            timeText = timeText,
            dateText = dateText,
            fontSizeSp = fontSizeSp,
            enabled = interactive,
            onHoldCompleted = onAdminHoldCompleted,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        BatteryIndicator(battery = battery, fontSizeSp = fontSizeSp)
    }
}

/**
 * Orologio che, tenuto premuto per 5 secondi consecutivi, apre l'accesso ai familiari.
 * Durante la pressione compare un piccolo anello di avanzamento.
 */
@Composable
private fun AdminHoldClock(
    timeText: String,
    dateText: String,
    fontSizeSp: Int,
    enabled: Boolean,
    onHoldCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SeniorTheme.colors
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnHoldCompleted by rememberUpdatedState(onHoldCompleted)

    Box(
        modifier = modifier
            .semantics { contentDescription = "Ore $timeText, $dateText" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val holdJob = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(ADMIN_HOLD_MS, easing = LinearEasing))
                        currentOnHoldCompleted()
                    }
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    holdJob.cancel()
                    scope.launch { progress.snapTo(0f) }
                }
            },
    ) {
        Column {
            Text(
                text = timeText,
                color = colors.onTopBar,
                fontSize = (fontSizeSp * 1.7f).sp,
                lineHeight = (fontSizeSp * 1.8f).sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = dateText,
                color = colors.onTopBar,
                fontSize = (fontSizeSp * 0.68f).sp,
                lineHeight = (fontSizeSp * 0.8f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (progress.value > 0f) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
                color = colors.onTopBar.copy(alpha = 0.6f),
                strokeWidth = 3.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun BatteryIndicator(battery: BatteryStatus, fontSizeSp: Int) {
    val colors = SeniorTheme.colors
    val fill = when {
        battery.isLow -> colors.danger
        battery.percent < 50 -> colors.warning
        else -> colors.success
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.semantics {
            contentDescription = "Batteria al ${battery.percent} per cento" + if (battery.isCharging) ", in carica" else ""
        },
    ) {
        BatteryIcon(
            percent = battery.percent,
            isCharging = battery.isCharging,
            color = colors.onTopBar,
            fillColor = fill,
            modifier = Modifier
                .width(72.dp)
                .height(36.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${battery.percent}%",
            color = colors.onTopBar,
            fontSize = (fontSizeSp * 0.85f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}
