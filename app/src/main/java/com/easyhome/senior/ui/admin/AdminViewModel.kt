package com.easyhome.senior.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.easyhome.senior.data.config.ConfigJsonSerializer
import com.easyhome.senior.di.AppContainer
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.AppShortcut
import com.easyhome.senior.domain.model.BrightnessMode
import com.easyhome.senior.domain.model.CalendarSwipe
import com.easyhome.senior.domain.model.CalendarTarget
import com.easyhome.senior.domain.model.AppTheme
import com.easyhome.senior.domain.model.ButtonPalette
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.OverlayPosition
import com.easyhome.senior.domain.model.VibrationIntensity
import com.easyhome.senior.system.ActionLauncher
import com.easyhome.senior.system.BackupSharer
import com.easyhome.senior.system.ContactResolver
import com.easyhome.senior.system.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class WizardStep(val title: String) {
    BUTTONS("Pulsanti e app"),
    APPEARANCE("Aspetto e accessibilità"),
    PERMISSIONS("Permessi e sistema"),
    BACKUP("Backup configurazione"),
}

/** Stato immutabile del configuratore. La configurazione arriva sempre da DataStore. */
data class AdminUiState(
    val config: LauncherConfig = LauncherConfig(),
    val isConfigLoaded: Boolean = false,
    val step: WizardStep = WizardStep.BUTTONS,
    val apps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = true,
    val permissions: PermissionStatus = PermissionStatus(),
    val message: String? = null,
    val pendingImport: LauncherConfig? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val shortcutsLoading: Boolean = false,
    val canUseShortcuts: Boolean = false,
) {
    val installedPackages: Set<String> get() = apps.mapTo(HashSet()) { it.packageName }
    val exportJson: String get() = ConfigJsonSerializer.toJson(config)
}

private data class LocalState(
    val step: WizardStep = WizardStep.BUTTONS,
    val apps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = true,
    val permissions: PermissionStatus = PermissionStatus(),
    val message: String? = null,
    val pendingImport: LauncherConfig? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val shortcutsLoading: Boolean = false,
    val canUseShortcuts: Boolean = false,
)

/**
 * Ogni modifica viene salvata subito in DataStore ("salvataggio automatico"):
 * un familiare può uscire in qualsiasi momento senza perdere il lavoro.
 */
class AdminViewModel(private val container: AppContainer) : ViewModel() {

    private val configRepository = container.configRepository
    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<AdminUiState> = combine(configRepository.config, local) { config, l ->
        AdminUiState(
            config = config,
            isConfigLoaded = true,
            step = l.step,
            apps = l.apps,
            appsLoading = l.appsLoading,
            permissions = l.permissions,
            message = l.message,
            pendingImport = l.pendingImport,
            shortcuts = l.shortcuts,
            shortcutsLoading = l.shortcutsLoading,
            canUseShortcuts = l.canUseShortcuts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    init {
        loadApps()
        refreshPermissions()
    }

    // ---- Navigazione wizard ---------------------------------------------------------------

    fun goTo(step: WizardStep) = local.update { it.copy(step = step) }

    fun next() = local.update { s ->
        s.copy(step = WizardStep.entries.getOrElse(s.step.ordinal + 1) { s.step })
    }

    fun back() = local.update { s ->
        s.copy(step = WizardStep.entries.getOrElse(s.step.ordinal - 1) { s.step })
    }

    fun showMessage(text: String) = local.update { it.copy(message = text) }

    fun clearMessage() = local.update { it.copy(message = null) }

    // ---- Step 1: pulsanti -----------------------------------------------------------------

    fun loadApps() {
        viewModelScope.launch {
            local.update { it.copy(appsLoading = true) }
            val apps = container.appsRepository.getLaunchableApps()
            local.update { it.copy(apps = apps, appsLoading = false) }
        }
    }

    fun addApp(app: InstalledApp) {
        addButton { config ->
            HomeButton(
                id = UUID.randomUUID().toString(),
                type = ActionType.APP,
                label = LauncherConfig.normalizeLabel(app.label),
                colorArgb = ButtonPalette.next(config.buttons),
                packageName = app.packageName,
            )
        }
    }

    fun onContactPicked(uri: Uri, type: ActionType) {
        viewModelScope.launch {
            val resolver = container.contactResolver
            val picked = withContext(Dispatchers.IO) { resolver.readPickedPhone(uri) }
            if (picked == null) {
                showMessage("Impossibile leggere il contatto scelto.")
                return@launch
            }
            val mime = ContactResolver.whatsAppMimeFor(type)
            val dataId = mime?.let { withContext(Dispatchers.IO) { resolver.findWhatsAppDataId(picked.contactId, it) } }
            if (mime != null && dataId == null) {
                showMessage(
                    if (resolver.hasContactsPermission()) {
                        "${picked.name} non risulta su WhatsApp da questo telefono: il pulsante aprirà la chat tramite numero."
                    } else {
                        "Permesso Contatti mancante: il pulsante aprirà la chat WhatsApp tramite numero."
                    },
                )
            }
            if (type == ActionType.TELEGRAM_CHAT && container.actionLauncher.telegramPackage() == null) {
                showMessage("Attenzione: Telegram non è installato su questo telefono.")
            }
            val prefix = when (type) {
                ActionType.PHONE_CALL -> "CHIAMA"
                ActionType.WHATSAPP_VIDEO -> "VIDEO"
                ActionType.WHATSAPP_VOICE -> "VOCE"
                ActionType.WHATSAPP_CHAT -> "SCRIVI A"
                ActionType.TELEGRAM_CHAT -> "TELEGRAM"
                else -> ""
            }
            addButton { config ->
                HomeButton(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    label = LauncherConfig.normalizeLabel("$prefix ${picked.name}".trim()),
                    colorArgb = ButtonPalette.next(config.buttons),
                    contactName = picked.name,
                    phoneNumber = picked.phoneNumber,
                    whatsappDataId = dataId,
                )
            }
        }
    }

    /** Gruppo WhatsApp (link di invito), chat/gruppo/canale Telegram (t.me/...), sito web. */
    fun addLink(label: String, rawLink: String) {
        val link = ActionLauncher.normalizeLink(rawLink)
        val uri = android.net.Uri.parse(link)
        if (rawLink.isBlank() || uri.scheme.isNullOrBlank() || (uri.scheme in listOf("http", "https") && uri.host.isNullOrBlank())) {
            showMessage("Il collegamento non è valido. Esempio: https://chat.whatsapp.com/AbCdEf...")
            return
        }
        addButton { config ->
            HomeButton(
                id = UUID.randomUUID().toString(),
                type = ActionType.LINK,
                label = LauncherConfig.normalizeLabel(label.ifBlank { "APRI" }),
                colorArgb = ButtonPalette.next(config.buttons),
                packageName = ActionLauncher.packageForLink(link),
                link = link,
            )
        }
    }

    fun loadShortcuts() {
        viewModelScope.launch {
            val repo = container.appsRepository
            local.update { it.copy(shortcutsLoading = true, canUseShortcuts = repo.canUseShortcuts()) }
            val list = repo.getShortcuts()
            local.update { it.copy(shortcuts = list, shortcutsLoading = false, canUseShortcuts = repo.canUseShortcuts()) }
        }
    }

    fun addShortcut(shortcut: AppShortcut) {
        viewModelScope.launch {
            addButtonNow { config ->
                HomeButton(
                    id = UUID.randomUUID().toString(),
                    type = ActionType.SHORTCUT,
                    label = LauncherConfig.normalizeLabel(shortcut.label),
                    colorArgb = ButtonPalette.next(config.buttons),
                    packageName = shortcut.packageName,
                    shortcutId = shortcut.shortcutId,
                )
            }
            syncPinnedShortcuts(shortcut.packageName)
        }
    }

    /** Le scorciatoie usate restano "fissate": WhatsApp/Telegram non le possono togliere dalla lista. */
    private suspend fun syncPinnedShortcuts(packageName: String) {
        val ids = configRepository.config.first().buttons
            .filter { it.type == ActionType.SHORTCUT && it.packageName == packageName }
            .mapNotNull { it.shortcutId }
        container.appsRepository.setPinnedShortcuts(packageName, ids)
    }

    private fun addButton(factory: (LauncherConfig) -> HomeButton) {
        viewModelScope.launch { addButtonNow(factory) }
    }

    private suspend fun addButtonNow(factory: (LauncherConfig) -> HomeButton) {
        if (configRepository.config.first().isFull) {
            showMessage("Hai raggiunto il massimo tecnico di ${LauncherConfig.MAX_BUTTONS} pulsanti.")
            return
        }
        configRepository.update { c ->
            if (c.isFull) c else c.copy(buttons = c.buttons + factory(c))
        }
    }

    fun updateLabel(id: String, label: String) = updateButton(id) { it.copy(label = label) }

    fun updateColor(id: String, color: Long) = updateButton(id) { it.copy(colorArgb = color) }

    fun removeButton(id: String) = viewModelScope.launch {
        val removed = configRepository.config.first().buttons.firstOrNull { it.id == id }
        configRepository.update { c -> c.copy(buttons = c.buttons.filterNot { it.id == id }) }
        if (removed?.type == ActionType.SHORTCUT) removed.packageName?.let { syncPinnedShortcuts(it) }
    }

    fun moveButton(id: String, delta: Int) = viewModelScope.launch {
        configRepository.update { c ->
            val list = c.buttons.toMutableList()
            val from = list.indexOfFirst { it.id == id }
            val to = from + delta
            if (from < 0 || to !in list.indices) return@update c
            list.add(to, list.removeAt(from))
            c.copy(buttons = list)
        }
    }

    private fun updateButton(id: String, transform: (HomeButton) -> HomeButton) = viewModelScope.launch {
        configRepository.update { c -> c.copy(buttons = c.buttons.map { if (it.id == id) transform(it) else it }) }
    }

    // ---- Step 2: aspetto ------------------------------------------------------------------

    fun setTheme(theme: AppTheme) = updateConfig { it.copy(theme = theme) }
    fun setFontSize(sp: Int) = updateConfig { it.copy(fontSizeSp = sp) }
    fun setIconSize(dp: Int) = updateConfig { it.copy(iconSizeDp = dp) }
    fun setSound(enabled: Boolean) = updateConfig { it.copy(soundEnabled = enabled) }
    fun setOverlayPosition(position: OverlayPosition) = updateConfig { it.copy(overlayPosition = position) }
    fun setColumns(columns: Int) = updateConfig { it.copy(columns = columns) }
    fun setRowsPerPage(rows: Int) = updateConfig { it.copy(rowsPerPage = rows) }
    fun setCalendarSwipe(swipe: CalendarSwipe) = updateConfig { it.copy(calendarSwipe = swipe) }
    fun setCalendarTarget(target: CalendarTarget) = updateConfig { it.copy(calendarTarget = target) }

    fun setBrightnessMode(mode: BrightnessMode) = updateConfigAndApplyBrightness { it.copy(brightnessMode = mode) }
    fun setBrightnessLevel(level: Int) = updateConfigAndApplyBrightness { it.copy(brightnessLevel = level) }

    private fun updateConfigAndApplyBrightness(transform: (LauncherConfig) -> LauncherConfig) = viewModelScope.launch {
        configRepository.update(transform)
        container.brightnessController.applySystem(configRepository.config.first())
    }

    fun setVibration(intensity: VibrationIntensity) {
        updateConfig { it.copy(vibration = intensity) }
        container.feedbackManager.vibrate(intensity, short = false)
    }

    fun testFeedback() = container.feedbackManager.buttonFeedback(uiState.value.config)

    private fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) = viewModelScope.launch {
        configRepository.update(transform)
    }

    // ---- Step 3: permessi -----------------------------------------------------------------

    fun refreshPermissions() {
        local.update { it.copy(permissions = container.permissionChecker.currentStatus()) }
    }

    // ---- Step 4: backup -------------------------------------------------------------------

    fun prepareImportFromText(text: String) {
        if (text.isBlank()) {
            showMessage("Incolla prima il testo del backup.")
            return
        }
        try {
            val parsed = ConfigJsonSerializer.fromJson(text)
            local.update { it.copy(pendingImport = parsed) }
        } catch (e: IllegalArgumentException) {
            showMessage(e.message ?: "Backup non valido.")
        }
    }

    fun prepareImportFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { BackupSharer.readText(container.appContext, uri) }
                prepareImportFromText(text)
            } catch (e: IllegalArgumentException) {
                showMessage(e.message ?: "File non leggibile.")
            } catch (e: java.io.IOException) {
                showMessage("Errore di lettura del file: ${e.message}")
            } catch (e: SecurityException) {
                showMessage("Accesso al file negato.")
            }
        }
    }

    fun confirmImport() {
        val pending = local.value.pendingImport ?: return
        viewModelScope.launch {
            // Gli ID WhatsApp sono specifici del telefono di origine: si ricalcolano qui.
            val resolver = container.contactResolver
            val remapped = withContext(Dispatchers.IO) {
                pending.copy(
                    buttons = pending.buttons.map { b ->
                        val mime = ContactResolver.whatsAppMimeFor(b.type)
                        if (mime != null) {
                            b.copy(whatsappDataId = b.phoneNumber?.let { resolver.findWhatsAppDataIdByNumber(it, mime) })
                        } else {
                            b
                        }
                    },
                )
            }
            configRepository.replace(remapped)
            remapped.buttons.filter { it.type == ActionType.SHORTCUT }.mapNotNull { it.packageName }.distinct()
                .forEach { syncPinnedShortcuts(it) }
            val missing = remapped.buttons.count {
                it.type == ActionType.APP && it.packageName != null &&
                    !container.appsRepository.isInstalled(it.packageName)
            }
            local.update { it.copy(pendingImport = null) }
            showMessage(
                if (missing > 0) {
                    "Configurazione importata. Attenzione: $missing app non sono installate su questo telefono."
                } else {
                    "Configurazione importata correttamente."
                },
            )
        }
    }

    fun cancelImport() = local.update { it.copy(pendingImport = null) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminViewModel(container) }
        }

        fun describe(button: HomeButton, apps: List<InstalledApp>): String {
            val who = listOfNotNull(button.contactName, button.phoneNumber?.let { "($it)" }).joinToString(" ")
            fun appName(pkg: String?) = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg.orEmpty()
            return when (button.type) {
                ActionType.APP -> "Apre l'app ${appName(button.packageName)}"
                ActionType.PHONE_CALL -> "Chiamata telefonica a $who"
                ActionType.WHATSAPP_VIDEO -> "Videochiamata WhatsApp a $who"
                ActionType.WHATSAPP_VOICE -> "Chiamata vocale WhatsApp a $who"
                ActionType.WHATSAPP_CHAT -> "Apre la chat WhatsApp di $who"
                ActionType.TELEGRAM_CHAT -> "Apre la chat Telegram di $who"
                ActionType.LINK -> "Apre il collegamento ${button.link}"
                ActionType.SHORTCUT -> "Scorciatoia di ${appName(button.packageName)}"
            }
        }
    }
}
