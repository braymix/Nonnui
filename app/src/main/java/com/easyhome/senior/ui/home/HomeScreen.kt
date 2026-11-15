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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
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
import com.easyhome.senior.system.ActionLauncher
import com.easyhome.senior.system.ContactResolver
import com.easyhome.senior.ui.common.AutoSizeText
import com.easyhome.senior.ui.common.VideoCallIcon
import com.easyhome.senior.ui.common.rememberAppIcon
import com.easyhome.senior.ui.common.seniorTap
import com.easyhome.senior.ui.theme.SeniorTheme
import com.easyhome.senior.ui.theme.homeButtonColors

/** Carica l'icona di un'app o, se shortcutId non è null, di una sua scorciatoia. */
typealias IconLoader = suspend (packageName: String, shortcutId: String?, sizePx: Int) -> Bitmap?

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
    onPageChange: (Int) -> Unit = {},
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
                columns = state.config.columns,
                rowsPerPage = state.config.rowsPerPage,
                page = state.page,
                onPageChange = onPageChange,
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

/**
 * Lista (1 colonna) o griglia (2-3 colonne), senza limite di pulsanti.
 *  - Se tutti i pulsanti entrano in una schermata, si dividono il 100% dello spazio
 *    (1 pulsante = schermo intero; l'ultima riga incompleta si allarga).
 *  - Altrimenti vengono mostrati a pagine di [rowsPerPage] righe, con i grandi tasti
 *    INDIETRO / AVANTI (niente swipe: nessun gesto accidentale).
 */
@Composable
fun HomeButtonsGrid(
    buttons: List<HomeButton>,
    columns: Int,
    rowsPerPage: Int,
    page: Int,
    onPageChange: (Int) -> Unit,
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
    val cols = columns.coerceAtLeast(1)
    val pageSize = cols * rowsPerPage.coerceAtLeast(1)
    val pageCount = (buttons.size + pageSize - 1) / pageSize
    val currentPage = page.coerceIn(0, pageCount - 1)
    val singlePage = pageCount == 1
    val pageButtons = buttons.drop(currentPage * pageSize).take(pageSize)
    val rows = pageButtons.chunked(cols)
    // Su più pagine ogni pagina ha lo stesso numero di righe: i pulsanti non cambiano dimensione.
    val rowCount = if (singlePage) rows.size else rowsPerPage
    // Con 3 colonne il testo deve stare in celle strette: si riduce in proporzione.
    val cellFontSp = when (cols) {
        1 -> fontSizeSp
        2 -> (fontSizeSp * 0.8f).toInt()
        else -> (fontSizeSp * 0.62f).toInt()
    }

    Column(modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (rowIndex in 0 until rowCount) {
                val rowButtons = rows.getOrNull(rowIndex).orEmpty()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowButtons.forEach { button ->
                        BigHomeButton(
                            button = button,
                            fontSizeSp = cellFontSp,
                            iconSizeDp = iconSizeDp,
                            interactive = interactive,
                            iconLoader = iconLoader,
                            onClick = { onButtonClick(button) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    // Celle vuote solo con più pagine, per mantenere la griglia regolare.
                    if (!singlePage) {
                        repeat(cols - rowButtons.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (!singlePage) {
            Spacer(Modifier.height(10.dp))
            PageNavigation(
                page = currentPage,
                pageCount = pageCount,
                interactive = interactive,
                onPageChange = onPageChange,
            )
        }
    }
}

@Composable
private fun PageNavigation(page: Int, pageCount: Int, interactive: Boolean, onPageChange: (Int) -> Unit) {
    val colors = SeniorTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PageButton(
            text = "◀ INDIETRO",
            enabled = interactive && page > 0,
            visible = page > 0,
            onClick = { onPageChange(page - 1) },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${page + 1} / $pageCount",
            color = colors.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(72.dp)
                .semantics { contentDescription = "Pagina ${page + 1} di $pageCount" },
        )
        PageButton(
            text = "AVANTI ▶",
            enabled = interactive && page < pageCount - 1,
            visible = page < pageCount - 1,
            onClick = { onPageChange(page + 1) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PageButton(text: String, enabled: Boolean, visible: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = SeniorTheme.colors
    val shape = RoundedCornerShape(20.dp)
    if (!visible) {
        Spacer(modifier)
        return
    }
    var pressed by remember { mutableStateOf(false) }
    val bg = if (colors.isHighContrast) Color.Black else colors.topBar
    val fg = if (colors.isHighContrast) Color(0xFFFFFF00) else colors.onTopBar
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (pressed) fg else bg)
            .border(4.dp, colors.border, shape)
            .semantics {
                role = Role.Button
                contentDescription = text.replace("◀", "").replace("▶", "").trim()
            }
            .seniorTap(enabled = enabled, onPressedChange = { pressed = it }, onTap = onClick),
    ) {
        AutoSizeText(
            text = text,
            maxFontSize = 22.sp,
            minFontSize = 12.sp,
            color = if (pressed) bg else fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
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
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Celle strette (griglia a 3): meno margine interno, così le parole non si spezzano.
                val innerPadding = if (this.maxWidth < 140.dp) 6.dp else 16.dp
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Celle alte o strette (griglia): icona sopra, testo sotto.
                    val vertical = maxHeight >= 200.dp || maxWidth < 240.dp
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
                            Spacer(Modifier.height(if (innerPadding < 16.dp) 6.dp else 12.dp))
                            AutoSizeText(
                                text = button.label,
                                maxFontSize = fontSizeSp.sp,
                                minFontSize = 11.sp,
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
}

@Composable
private fun ButtonIcon(button: HomeButton, size: Dp, tint: Color, iconLoader: IconLoader) {
    when (button.type) {
        ActionType.PHONE_CALL, ActionType.WHATSAPP_VOICE ->
            Icon(Icons.Filled.Call, contentDescription = null, tint = tint, modifier = Modifier.size(size))
        ActionType.WHATSAPP_VIDEO ->
            VideoCallIcon(color = tint, modifier = Modifier.size(size))
        else -> {
            // Icona dell'app (o della scorciatoia): il volto dell'app è ciò che l'anziano riconosce.
            val pkg = when (button.type) {
                ActionType.WHATSAPP_CHAT -> ContactResolver.WHATSAPP_PACKAGE
                ActionType.TELEGRAM_CHAT -> ContactResolver.TELEGRAM_PACKAGES.first()
                ActionType.LINK -> button.packageName ?: button.link?.let { ActionLauncher.packageForLink(it) }
                else -> button.packageName
            }.orEmpty()
            val shortcutId = if (button.type == ActionType.SHORTCUT) button.shortcutId else null
            val sizePx = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
            val bmp = rememberAppIcon(pkg, shortcutId, sizePx, iconLoader)
            when {
                bmp != null -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size))
                button.type == ActionType.LINK ->
                    Icon(Icons.Filled.Share, contentDescription = null, tint = tint, modifier = Modifier.size(size))
                button.type == ActionType.APP || button.type == ActionType.SHORTCUT ->
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(size))
                else -> Icon(Icons.Filled.Email, contentDescription = null, tint = tint, modifier = Modifier.size(size))
            }
        }
    }
}
