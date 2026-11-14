package com.easyhome.senior.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.AppTheme
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.OverlayPosition
import com.easyhome.senior.domain.model.VibrationIntensity
import com.easyhome.senior.ui.home.IconLoader
import com.easyhome.senior.ui.theme.seniorColorsFor
import kotlin.math.roundToInt

@Composable
fun AppearanceStep(state: AdminUiState, viewModel: AdminViewModel, iconLoader: IconLoader) {
    val config = state.config
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LivePreview(config, iconLoader, Modifier.align(Alignment.CenterHorizontally))

        SectionTitle("Tema")
        AppTheme.entries.forEach { theme ->
            ThemeOption(theme = theme, selected = config.theme == theme, onClick = { viewModel.setTheme(theme) })
        }

        HorizontalDivider()
        SectionTitle("Dimensione testo: ${config.fontSizeSp} sp")
        SteppedSlider(
            value = config.fontSizeSp,
            range = LauncherConfig.MIN_FONT_SP..LauncherConfig.MAX_FONT_SP,
            step = 2,
            onChange = viewModel::setFontSize,
        )

        SectionTitle("Dimensione icone: ${config.iconSizeDp} dp")
        SteppedSlider(
            value = config.iconSizeDp,
            range = LauncherConfig.MIN_ICON_DP..LauncherConfig.MAX_ICON_DP,
            step = 8,
            onChange = viewModel::setIconSize,
        )

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle("Suono alla pressione")
                Text("Un \"bip\" conferma ogni tocco valido.", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = config.soundEnabled, onCheckedChange = viewModel::setSound)
        }

        SectionTitle("Intensità vibrazione")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VibrationIntensity.entries.forEach { intensity ->
                ToggleButton(
                    text = when (intensity) {
                        VibrationIntensity.OFF -> "NO"
                        VibrationIntensity.LOW -> "BASSA"
                        VibrationIntensity.MEDIUM -> "MEDIA"
                        VibrationIntensity.HIGH -> "ALTA"
                    },
                    selected = config.vibration == intensity,
                    onClick = { viewModel.setVibration(intensity) },
                )
            }
        }
        OutlinedButton(onClick = viewModel::testFeedback, modifier = Modifier.fillMaxWidth()) {
            Text("PROVA SUONO E VIBRAZIONE")
        }

        HorizontalDivider()
        SectionTitle("Posizione tasto \"Torna all'inizio\"")
        Text(
            "In basso è più facile da trovare; in alto non copre la barra di scrittura delle app.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleButton("IN BASSO", config.overlayPosition == OverlayPosition.BOTTOM) {
                viewModel.setOverlayPosition(OverlayPosition.BOTTOM)
            }
            ToggleButton("IN ALTO", config.overlayPosition == OverlayPosition.TOP) {
                viewModel.setOverlayPosition(OverlayPosition.TOP)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

/** Slider a scatti: salva in DataStore solo quando il valore arrotondato cambia. */
@Composable
private fun SteppedSlider(value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val intervals = (range.last - range.first) / step
    Slider(
        value = local,
        onValueChange = { raw ->
            local = raw
            val snapped = (range.first + ((raw - range.first) / step).roundToInt() * step)
                .coerceIn(range.first, range.last)
            if (snapped != value) onChange(snapped)
        },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (intervals - 1).coerceAtLeast(0),
    )
}

@Composable
private fun ThemeOption(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val palette = seniorColorsFor(theme)
    val name = when (theme) {
        AppTheme.LIGHT -> "CHIARO"
        AppTheme.DARK -> "SCURO"
        AppTheme.HIGH_CONTRAST -> "ALTO CONTRASTO (giallo su nero)"
    }
    val shape = RoundedCornerShape(12.dp)
    val content: @Composable RowScope.() -> Unit = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(palette.background, shape)
                .border(2.dp, palette.border, shape),
        ) {
            Text("Aa", color = palette.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(16.dp))
        Text(name, modifier = Modifier.weight(1f))
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), content = content)
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), content = content)
    }
}

@Composable
private fun RowScope.ToggleButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.weight(1f)
    val pad = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = pad) { Text(text, maxLines = 1) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = pad) {
            Text(text, maxLines = 1, color = Color.DarkGray)
        }
    }
}

