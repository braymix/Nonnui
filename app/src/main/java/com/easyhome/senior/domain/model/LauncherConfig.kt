package com.easyhome.senior.domain.model

import java.util.Locale

/** Tipo di azione eseguita da un pulsante della Home. */
enum class ActionType {
    /** Apre un'app installata. */
    APP,

    /** Chiamata telefonica diretta a un contatto. */
    PHONE_CALL,

    /** Videochiamata WhatsApp a un contatto. */
    WHATSAPP_VIDEO,

    /** Chiamata vocale WhatsApp a un contatto. */
    WHATSAPP_VOICE,

    /** Apre la chat WhatsApp di un contatto. */
    WHATSAPP_CHAT,

    /** Apre la chat Telegram di un contatto (tramite numero di telefono). */
    TELEGRAM_CHAT,

    /** Apre un link: invito a gruppo WhatsApp (chat.whatsapp.com), t.me/..., sito web. */
    LINK,

    /** Scorciatoia pubblicata da un'app (es. chat o gruppo WhatsApp/Telegram "Aggiungi a Home"). */
    SHORTCUT,
}

/** Temi disponibili per la Home e la tastiera. */
enum class AppTheme { LIGHT, DARK, HIGH_CONTRAST }

/** Intensità del feedback aptico. */
enum class VibrationIntensity { OFF, LOW, MEDIUM, HIGH }

/** Posizione del pulsante galleggiante "Torna all'inizio". */
enum class OverlayPosition { BOTTOM, TOP }

/** Gestione della luminosità dello schermo. */
enum class BrightnessMode {
    /** Non tocca la luminosità: decide il telefono. */
    SYSTEM,

    /** Luminosità adattiva (sensore di luce) attivata a livello di sistema. */
    AUTO,

    /** Luminosità fissa al livello scelto. */
    FIXED,
}

/** Swipe orizzontale sulla Home che apre il calendario. */
enum class CalendarSwipe {
    OFF,

    /** Il calendario entra da sinistra: si scorre il dito verso destra. */
    FROM_LEFT,

    /** Il calendario entra da destra: si scorre il dito verso sinistra. */
    FROM_RIGHT,
}

/** Cosa apre lo swipe del calendario. */
enum class CalendarTarget {
    /** L'app calendario del telefono (Google Calendar, Samsung Calendar, ...). */
    SYSTEM_APP,

    /** Agenda semplificata a caratteri giganti con gli impegni del calendario di sistema. */
    AGENDA,
}

/** Un pulsante gigante della Home. Modello immutabile. */
data class HomeButton(
    val id: String,
    val type: ActionType,
    val label: String,
    val colorArgb: Long,
    /** App da aprire (APP), app proprietaria della scorciatoia (SHORTCUT) o app preferita per un LINK. */
    val packageName: String? = null,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    /** ID della riga ContactsContract.Data di WhatsApp (specifico del dispositivo). */
    val whatsappDataId: Long? = null,
    /** URL per il tipo LINK. */
    val link: String? = null,
    /** ID della scorciatoia per il tipo SHORTCUT. */
    val shortcutId: String? = null,
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
    /** 1 = lista (pulsanti impilati), 2-3 = griglia. */
    val columns: Int = DEFAULT_COLUMNS,
    /** Righe visibili in ciascuna schermata: oltre, compaiono i tasti AVANTI/INDIETRO. */
    val rowsPerPage: Int = DEFAULT_ROWS,
    val brightnessMode: BrightnessMode = BrightnessMode.SYSTEM,
    /** Percentuale 10-100, usata con [BrightnessMode.FIXED]. */
    val brightnessLevel: Int = DEFAULT_BRIGHTNESS,
    val calendarSwipe: CalendarSwipe = CalendarSwipe.OFF,
    val calendarTarget: CalendarTarget = CalendarTarget.SYSTEM_APP,
) {
    val isFull: Boolean get() = buttons.size >= MAX_BUTTONS
    val pageSize: Int get() = columns * rowsPerPage
    val pageCount: Int get() = if (buttons.isEmpty()) 1 else (buttons.size + pageSize - 1) / pageSize

    /** Riporta qualsiasi valore (es. da import JSON) entro i limiti ammessi. */
    fun sanitized(): LauncherConfig = copy(
        buttons = buttons
            .distinctBy { it.id }
            .take(MAX_BUTTONS)
            .map { it.copy(label = normalizeLabel(it.label)) },
        fontSizeSp = fontSizeSp.coerceIn(MIN_FONT_SP, MAX_FONT_SP),
        iconSizeDp = iconSizeDp.coerceIn(MIN_ICON_DP, MAX_ICON_DP),
        columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
        rowsPerPage = rowsPerPage.coerceIn(MIN_ROWS, MAX_ROWS),
        brightnessLevel = brightnessLevel.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS),
    )

    companion object {
        /** Nessun limite pratico: è solo una protezione contro backup anomali. */
        const val MAX_BUTTONS = 60
        const val MIN_FONT_SP = 24
        const val MAX_FONT_SP = 48
        const val DEFAULT_FONT_SP = 32
        const val MIN_ICON_DP = 48
        const val MAX_ICON_DP = 160
        const val DEFAULT_ICON_DP = 96
        const val MAX_LABEL_LENGTH = 24
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 3
        const val DEFAULT_COLUMNS = 1
        const val MIN_ROWS = 1
        const val MAX_ROWS = 6
        const val DEFAULT_ROWS = 4
        const val MIN_BRIGHTNESS = 10
        const val MAX_BRIGHTNESS = 100
        const val DEFAULT_BRIGHTNESS = 80

        fun normalizeLabel(raw: String): String =
            raw.replace('\n', ' ').uppercase(Locale.ITALIAN).take(MAX_LABEL_LENGTH)
    }
}

/** Colori pieni e saturi selezionabili per i pulsanti (nome visualizzato -> ARGB). */
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
        return colors.firstOrNull { it.second !in usedColors }?.second
            ?: colors[used.size % colors.size].second
    }
}
