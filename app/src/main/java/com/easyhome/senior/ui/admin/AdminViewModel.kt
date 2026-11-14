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
import com.easyhome.senior.domain.model.AppTheme
import com.easyhome.senior.domain.model.ButtonPalette
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.OverlayPosition
import com.easyhome.senior.domain.model.VibrationIntensity
import com.easyhome.senior.system.BackupSharer
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
import java.util.Locale
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
            var dataId: Long? = null
            if (type == ActionType.WHATSAPP_VIDEO) {
                dataId = withContext(Dispatchers.IO) { resolver.findWhatsAppVideoDataId(picked.contactId) }
                if (dataId == null) {
                    showMessage(
                        if (resolver.hasContactsPermission()) {
                            "${picked.name} non risulta su WhatsApp: il pulsante aprirà la sua chat."
                        } else {
                            "Permesso Contatti mancante: il pulsante aprirà la chat WhatsApp."
                        },
                    )
                }
            }
            val prefix = if (type == ActionType.WHATSAPP_VIDEO) "VIDEO" else "CHIAMA"
            addButton { config ->
                HomeButton(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    label = LauncherConfig.normalizeLabel("$prefix ${picked.name}"),
                    colorArgb = ButtonPalette.next(config.buttons),
                    contactName = picked.name,
                    phoneNumber = picked.phoneNumber,
                    whatsappDataId = dataId,
                )
            }
        }
    }

    private fun addButton(factory: (LauncherConfig) -> HomeButton) {
        viewModelScope.launch {
            if (configRepository.config.first().isFull) {
                showMessage("Massimo ${LauncherConfig.MAX_BUTTONS} pulsanti: eliminane uno per aggiungerne un altro.")
                return@launch
            }
            configRepository.update { c ->
                if (c.isFull) c else c.copy(buttons = c.buttons + factory(c))
            }
        }
    }

    fun updateLabel(id: String, label: String) = updateButton(id) { it.copy(label = label) }

    fun updateColor(id: String, color: Long) = updateButton(id) { it.copy(colorArgb = color) }

    fun removeButton(id: String) = viewModelScope.launch {
        configRepository.update { c -> c.copy(buttons = c.buttons.filterNot { it.id == id }) }
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
                        if (b.type == ActionType.WHATSAPP_VIDEO) {
                            b.copy(whatsappDataId = b.phoneNumber?.let { resolver.findWhatsAppVideoDataIdByNumber(it) })
                        } else {
                            b
                        }
                    },
                )
            }
            configRepository.replace(remapped)
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

        fun describe(button: HomeButton, apps: List<InstalledApp>): String = when (button.type) {
            ActionType.APP -> {
                val name = apps.firstOrNull { it.packageName == button.packageName }?.label
                "Apre l'app ${name ?: button.packageName}"
            }
            ActionType.PHONE_CALL -> "Chiama ${button.contactName ?: ""} (${button.phoneNumber})"
            ActionType.WHATSAPP_VIDEO -> "Videochiamata WhatsApp a ${button.contactName ?: ""} (${button.phoneNumber})"
        }.replace("  ", " ").trim().let { it.replaceFirstChar { c -> c.titlecase(Locale.ITALIAN) } }
    }
}
