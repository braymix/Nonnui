package com.easyhome.senior.ui.admin

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.service.OverlayService
import com.easyhome.senior.system.safeStartActivity

private data class PermissionAction(val label: String, val primary: Boolean, val onClick: () -> Unit)

/** "Semaforo" dei permessi: si aggiorna da solo ogni volta che si torna sulla schermata. */
@Composable
fun PermissionsStep(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    val checker = remember(context) { context.appContainer.permissionChecker }
    val p = state.permissions

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }
    LaunchedEffect(p.canDrawOverlays) {
        if (p.canDrawOverlays) OverlayService.startIfPermitted(context)
    }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Tutte le voci devono essere VERDI perché il telefono funzioni al meglio.",
            style = MaterialTheme.typography.bodyLarge,
        )
        PermissionCard(
            title = "1. Launcher predefinito",
            description = "EasyHome deve essere la schermata Home: il tasto Home riporta sempre qui. " +
                "Serve anche per le scorciatoie di chat e gruppi WhatsApp/Telegram.",
            granted = p.isDefaultLauncher,
            actions = listOf(
                PermissionAction("CONFIGURA ORA", true) {
                    context.safeStartActivity(checker.homeSettingsIntent(), fallback = checker.appDetailsIntent())
                },
            ),
        )
        PermissionCard(
            title = "2. Tastiera ABCD",
            description = "Abilitata: ${yesNo(p.isKeyboardEnabled)} · Selezionata: ${yesNo(p.isKeyboardSelected)}.\n" +
                "Prima abilitala nelle impostazioni, poi sceglila come tastiera attiva.",
            granted = p.isKeyboardReady,
            actions = listOf(
                PermissionAction("CONFIGURA ORA", !p.isKeyboardEnabled) {
                    context.safeStartActivity(checker.inputMethodSettingsIntent())
                },
                PermissionAction("SCEGLI TASTIERA", p.isKeyboardEnabled && !p.isKeyboardSelected) {
                    checker.showInputMethodPicker()
                },
            ),
        )
        PermissionCard(
            title = "3. Tasto \"Torna all'inizio\"",
            description = "Permesso di mostrare il pulsante sopra le altre app (es. dentro WhatsApp).",
            granted = p.canDrawOverlays,
            actions = listOf(
                PermissionAction("CONFIGURA ORA", true) {
                    context.safeStartActivity(checker.overlaySettingsIntent(), fallback = checker.appDetailsIntent())
                },
            ),
        )
        PermissionCard(
            title = "4. Esclusione risparmio energetico",
            description = "Evita che il sistema chiuda il tasto \"Torna all'inizio\" in background.",
            granted = p.isIgnoringBatteryOptimizations,
            actions = listOf(
                PermissionAction("CONFIGURA ORA", true) {
                    context.safeStartActivity(
                        checker.batteryOptimizationIntent(),
                        fallback = checker.batteryOptimizationFallbackIntent(),
                    )
                },
            ),
        )
        PermissionCard(
            title = "5. Chiamate dirette (consigliato)",
            description = "Senza questo permesso, i pulsanti \"Chiama\" aprono solo il tastierino con il numero.",
            granted = p.hasCallPermission,
            actions = listOf(
                PermissionAction("CONCEDI", true) { requestPermission.launch(Manifest.permission.CALL_PHONE) },
                PermissionAction("IMPOSTAZIONI APP", false) { context.safeStartActivity(checker.appDetailsIntent()) },
            ),
        )
        PermissionCard(
            title = "6. Luminosità (impostazioni di sistema)",
            description = "Serve solo se al passo 2 hai scelto luminosità ADATTIVA o FISSA.",
            granted = p.canWriteSystemSettings,
            actions = listOf(
                PermissionAction("CONFIGURA ORA", true) {
                    context.safeStartActivity(
                        context.appContainer.brightnessController.writeSettingsIntent(),
                        fallback = checker.appDetailsIntent(),
                    )
                },
            ),
        )
        PermissionCard(
            title = "7. Contatti (chiamate e chat WhatsApp dirette)",
            description = "Serve per avviare la videochiamata WhatsApp direttamente, senza passare dalla chat.",
            granted = p.hasContactsPermission,
            actions = listOf(
                PermissionAction("CONCEDI", true) { requestPermission.launch(Manifest.permission.READ_CONTACTS) },
                PermissionAction("IMPOSTAZIONI APP", false) { context.safeStartActivity(checker.appDetailsIntent()) },
            ),
        )
    }
}

private fun yesNo(value: Boolean) = if (value) "SÌ" else "NO"

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actions: List<PermissionAction>,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(granted)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (!granted) {
                actions.forEach { action ->
                    if (action.primary) {
                        Button(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) { Text(action.label) }
                    } else {
                        OutlinedButton(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) { Text(action.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(granted: Boolean) {
    val (bg, text) = if (granted) Color(0xFF1B5E20) to "ATTIVO" else Color(0xFFB71C1C) to "DA ATTIVARE"
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier.semantics { contentDescription = "Stato: $text" },
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
