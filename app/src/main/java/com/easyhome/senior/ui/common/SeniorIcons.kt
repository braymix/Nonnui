package com.easyhome.senior.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** Icona batteria disegnata a mano: nessuna dipendenza da material-icons-extended. */
@Composable
fun BatteryIcon(percent: Int, isCharging: Boolean, color: Color, fillColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.height * 0.1f
        val nubWidth = size.width * 0.08f
        val bodyWidth = size.width - nubWidth
        val radius = CornerRadius(size.height * 0.15f)
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = radius,
            style = Stroke(width = stroke),
        )
        drawRect(
            color = color,
            topLeft = Offset(bodyWidth, size.height * 0.3f),
            size = Size(nubWidth, size.height * 0.4f),
        )
        val inset = stroke * 1.6f
        val innerWidth = (bodyWidth - inset * 2) * (percent.coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(inset, inset),
            size = Size(innerWidth, size.height - inset * 2),
            cornerRadius = CornerRadius(size.height * 0.06f),
        )
        if (isCharging) {
            val w = bodyWidth
            val h = size.height
            val bolt = Path().apply {
                moveTo(w * 0.55f, h * 0.08f)
                lineTo(w * 0.30f, h * 0.55f)
                lineTo(w * 0.48f, h * 0.55f)
                lineTo(w * 0.42f, h * 0.92f)
                lineTo(w * 0.70f, h * 0.42f)
                lineTo(w * 0.52f, h * 0.42f)
                close()
            }
            drawPath(bolt, color = Color.Black)
            drawPath(bolt, color = Color(0xFFFFEB3B), style = Stroke(width = stroke * 0.5f))
        }
    }
}

/** Icona videocamera per la videochiamata WhatsApp. */
@Composable
fun VideoCallIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.22f),
            size = Size(w * 0.64f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.08f),
        )
        val lens = Path().apply {
            moveTo(w * 0.68f, h * 0.42f)
            lineTo(w, h * 0.22f)
            lineTo(w, h * 0.78f)
            lineTo(w * 0.68f, h * 0.58f)
            close()
        }
        drawPath(lens, color = color)
    }
}
