package com.easyhome.senior.domain.model

import java.util.Locale

/** Tipo di azione eseguita da un pulsante della Home. */
enum class ActionType { APP, PHONE_CALL, WHATSAPP_VIDEO }

/** Temi disponibili per la Home e la tastiera. */
enum class AppTheme { LIGHT, DARK, HIGH_CONTRAST }

/** Intensità del feedback aptico. */
enum class VibrationIntensity { OFF, LOW, MEDIUM, HIGH }

/** Posizione del pulsante galleggiante "Torna all'inizio". */
enum class OverlayPosition { BOTTOM, TOP }

/** Un pulsante gigante della Home. Modello immutabile. */
data class HomeButton(
    val id: String,
    val type: ActionType,
    val label: String,
    val colorArgb: Long,
    val packageName: String? = null,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    /** ID della riga ContactsContract.Data con mimetype WhatsApp video (specifico del dispositivo). */
    val whatsappDataId: Long? = null,
)

/** Configurazione completa del launcher, persistita in DataStore. */
data class LauncherConfig(
    val buttons: List<HomeButton> = emptyList(),
    val theme: AppTheme = AppTheme.LIGHT,
    val fontSizeSp: Int = DEFAULT_FONT_SP,
    val iconSizeDp: Int = DEFAULT_ICON_DP,
    val soundEnabled: Boolean = true,
    val vibration: VibrationIntensity = VibrationIntensity.HIGH,
    val overlayPosition: OverlayPosition = OverlayPosition.BOTTOM,
) {
    val isFull: Boolean get() = buttons.size >= MAX_BUTTONS

    /** Riporta qualsiasi valore (es. da import JSON) entro i limiti ammessi. */
    fun sanitized(): LauncherConfig = copy(
        buttons = buttons
            .distinctBy { it.id }
            .take(MAX_BUTTONS)
            .map { it.copy(label = normalizeLabel(it.label)) },
        fontSizeSp = fontSizeSp.coerceIn(MIN_FONT_SP, MAX_FONT_SP),
        iconSizeDp = iconSizeDp.coerceIn(MIN_ICON_DP, MAX_ICON_DP),
    )

    companion object {
        const val MAX_BUTTONS = 4
        const val MIN_FONT_SP = 24
        const val MAX_FONT_SP = 48
        const val DEFAULT_FONT_SP = 32
        const val MIN_ICON_DP = 48
        const val MAX_ICON_DP = 160
        const val DEFAULT_ICON_DP = 96
        const val MAX_LABEL_LENGTH = 24

        fun normalizeLabel(raw: String): String =
            raw.replace('\n', ' ').uppercase(Locale.ITALIAN).take(MAX_LABEL_LENGTH)
    }
}

/** Colori ad alto contrasto selezionabili per i pulsanti (nome visualizzato -> ARGB). */
object ButtonPalette {
    val colors: List<Pair<String, Long>> = listOf(
        "VERDE" to 0xFF1B5E20,
        "BLU" to 0xFF0D47A1,
        "ROSSO" to 0xFFB71C1C,
        "ARANCIO" to 0xFFBF360C,
        "VIOLA" to 0xFF4A148C,
        "MARRONE" to 0xFF3E2723,
        "PETROLIO" to 0xFF006064,
        "GRIGIO" to 0xFF212121,
        "GIALLO" to 0xFFFFD600,
    )

    fun next(used: List<HomeButton>): Long {
        val usedColors = used.map { it.colorArgb }.toSet()
        return colors.firstOrNull { it.second !in usedColors }?.second ?: colors.first().second
    }
}
