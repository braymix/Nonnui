package com.easyhome.senior.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Testo che riduce la propria dimensione finché entra nello spazio disponibile
 * senza mai spezzare una parola a metà (es. "MESSAG-GI").
 */
@Composable
fun AutoSizeText(
    text: String,
    maxFontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 14.sp,
    maxLines: Int = 2,
    fontWeight: FontWeight = FontWeight.Black,
    textAlign: TextAlign = TextAlign.Center,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        var fontSize by remember(text, maxFontSize, maxWidth, maxHeight) { mutableStateOf(maxFontSize) }
        var ready by remember(text, maxFontSize, maxWidth, maxHeight) { mutableStateOf(false) }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize * 1.1f,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = true,
            overflow = TextOverflow.Clip,
            modifier = Modifier.drawWithContent { if (ready) drawContent() },
            onTextLayout = { result ->
                val tooBig = result.hasVisualOverflow || result.breaksWord(text)
                if (tooBig && fontSize.value > minFontSize.value) {
                    fontSize = (fontSize.value * 0.9f).coerceAtLeast(minFontSize.value).sp
                } else {
                    ready = true
                }
            },
        )
    }
}

private fun TextLayoutResult.breaksWord(text: String): Boolean {
    for (line in 0 until lineCount - 1) {
        val end = getLineEnd(line)
        if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) {
            return true
        }
    }
    return false
}
