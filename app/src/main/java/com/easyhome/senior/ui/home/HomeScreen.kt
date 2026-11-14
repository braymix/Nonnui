package com.easyhome.senior.ui.home

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.ui.common.AutoSizeText
import com.easyhome.senior.ui.common.VideoCallIcon
import com.easyhome.senior.ui.common.rememberAppIcon
import com.easyhome.senior.ui.common.seniorTap
import com.easyhome.senior.ui.theme.SeniorTheme
import com.easyhome.senior.ui.theme.homeButtonColors

typealias IconLoader = suspend (packageName: String, sizePx: Int) -> Bitmap?

/**
 * Home dell'anziano. Stateless: riceve lo stato e i callback, così da poter essere
 * riusata identica come anteprima dal vivo nel configuratore ([interactive] = false).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    iconLoader: IconLoader,
    onButtonClick: (HomeButton) -> Unit,
    onAdminHoldCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val colors = SeniorTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        if (!state.isLoaded) return@Box
        Column(Modifier.fillMaxSize()) {
            StatusTopBar(
                timeText = state.timeText,
                dateText = state.dateText,
                battery = state.battery,
                fontSizeSp = state.config.fontSizeSp,
                interactive = interactive,
                onAdminHoldCompleted = onAdminHoldCompleted,
            )
            if (state.battery.isLow) {
                LowBatteryBanner(fontSizeSp = state.config.fontSizeSp)
            }
            if (!state.isOnline) {
                WarningBanner(
                    text = "NESSUNA CONNESSIONE INTERNET",
                    background = colors.warning,
                    foreground = colors.onWarning,
                    fontSizeSp = state.config.fontSizeSp,
                )
            }
            HomeButtonsGrid(
                buttons = state.config.buttons,
                fontSizeSp = state.config.fontSizeSp,
                iconSizeDp = state.config.iconSizeDp,
                interactive = interactive,
                iconLoader = iconLoader,
                onButtonClick = onButtonClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        state.message?.let { msg ->
            MessageCard(
                text = msg,
                fontSizeSp = state.config.fontSizeSp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun LowBatteryBanner(fontSizeSp: Int) {
    val colors = SeniorTheme.colors
    // Lampeggio lento (1 Hz): ben visibile ma sicuro per persone fotosensibili.
    val transition = rememberInfiniteTransition(label = "battery-blink")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_000), RepeatMode.Reverse),
        label = "battery-phase",
    )
    val background = lerp(colors.danger, Color(0xFFFFD600), phase)
    val foreground = if (phase < 0.5f) colors.onDanger else Color.Black
    WarningBanner(
        text = "BATTERIA SCARICA - METTERE IN CARICA",
        background = background,
        foreground = foreground,
        fontSizeSp = fontSizeSp,
    )
}

@Composable
private fun WarningBanner(text: String, background: Color, foreground: Color, fontSizeSp: Int) {
    Text(
        text = text,
        color = foreground,
        fontSize = (fontSizeSp * 0.75f).sp,
        lineHeight = (fontSizeSp * 0.85f).sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .border(3.dp, Color.Black)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
private fun MessageCard(text: String, fontSizeSp: Int, modifier: Modifier = Modifier) {
    val colors = SeniorTheme.colors
    Text(
        text = text,
        color = colors.onDanger,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.15f).sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.danger)
            .border(4.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
fun HomeButtonsGrid(
    buttons: List<HomeButton>,
    fontSizeSp: Int,
    iconSizeDp: Int,
    interactive: Boolean,
    iconLoader: IconLoader,
    onButtonClick: (HomeButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (buttons.isEmpty()) {
        EmptyHome(fontSizeSp, modifier)
        return
    }
    // 1 pulsante = schermo intero; N pulsanti = N rettangoli impilati che si dividono lo spazio.
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        buttons.forEach { button ->
            BigHomeButton(
                button = button,
                fontSizeSp = fontSizeSp,
                iconSizeDp = iconSizeDp,
                interactive = interactive,
                iconLoader = iconLoader,
                onClick = { onButtonClick(button) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyHome(fontSizeSp: Int, modifier: Modifier) {
    val colors = SeniorTheme.colors
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "NESSUN PULSANTE CONFIGURATO",
            color = colors.onBackground,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.15f).sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Per i familiari: tenere premuto l'orologio per 5 secondi per aprire la configurazione.",
            color = colors.onBackground,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BigHomeButton(
    button: HomeButton,
    fontSizeSp: Int,
    iconSizeDp: Int,
    interactive: Boolean,
    iconLoader: IconLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SeniorTheme.colors
    val (background, foreground) = homeButtonColors(button.colorArgb)
    val shape = RoundedCornerShape(28.dp)
    val shadowOffset = 6.dp
    var pressed by remember { mutableStateOf(false) }
    // Effetto "tasto fisico": premendo, il pulsante scende sulla propria ombra netta.
    val pressOffset = if (pressed) shadowOffset else 0.dp

    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = shadowOffset, top = shadowOffset)
                .clip(shape)
                .background(colors.shadow),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = shadowOffset, bottom = shadowOffset)
                .offset(x = pressOffset, y = pressOffset)
                .clip(shape)
                .background(if (pressed) lerp(background, Color.Black, 0.25f) else background)
                .border(4.dp, colors.border, shape)
                .semantics {
                    role = Role.Button
                    contentDescription = button.label
                }
                .seniorTap(
                    enabled = interactive,
                    onPressedChange = { pressed = it },
                    onTap = onClick,
                ),
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                val vertical = maxHeight >= 200.dp
                val iconSize = min(
                    iconSizeDp.dp,
                    if (vertical) maxHeight * 0.5f else min(maxHeight * 0.9f, maxWidth * 0.35f),
                )
                if (vertical) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ButtonIcon(button, iconSize, foreground, iconLoader)
                        Spacer(Modifier.height(12.dp))
                        AutoSizeText(
                            text = button.label,
                            maxFontSize = fontSizeSp.sp,
                            color = foreground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ButtonIcon(button, iconSize, foreground, iconLoader)
                        Spacer(Modifier.width(16.dp))
                        AutoSizeText(
                            text = button.label,
                            maxFontSize = fontSizeSp.sp,
                            color = foreground,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonIcon(button: HomeButton, size: Dp, tint: Color, iconLoader: IconLoader) {
    when (button.type) {
        ActionType.APP -> {
            val sizePx = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
            val pkg = button.packageName.orEmpty()
            val bmp = rememberAppIcon(pkg, sizePx, iconLoader)
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(size),
                )
            } else {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(size))
            }
        }
        ActionType.PHONE_CALL ->
            Icon(Icons.Filled.Call, contentDescription = null, tint = tint, modifier = Modifier.size(size))
        ActionType.WHATSAPP_VIDEO ->
            VideoCallIcon(color = tint, modifier = Modifier.size(size))
    }
}
