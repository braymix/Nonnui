package com.easyhome.senior.ui.pin

import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.ButtonPalette
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.ui.theme.AdminTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Riceve le richieste "Aggiungi scorciatoia / Aggiungi alla schermata Home" inviate dalle app
 * al launcher predefinito (es. una chat o un gruppo WhatsApp/Telegram) e le trasforma in
 * un pulsante gigante della Home, previa conferma.
 */
class ConfirmPinShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = launcherApps?.getPinItemRequest(intent)
        val info = request?.shortcutInfo
        if (request == null || !request.isValid || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT || info == null) {
            finish()
            return
        }
        val label = (info.longLabel ?: info.shortLabel ?: "Scorciatoia").toString()
        val appLabel = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(info.`package`, 0)).toString()
        } catch (_: Exception) {
            info.`package`
        }

        setContent {
            AdminTheme {
                var working by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text("Aggiungere alla Home?", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("da $appLabel", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Comparirà come nuovo pulsante gigante nella schermata principale.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                    confirmButton = {
                        Button(enabled = !working, onClick = {
                            working = true
                            lifecycleScope.launch {
                                addToHome(request, info.`package`, info.id, label)
                                finish()
                            }
                        }) { Text("AGGIUNGI") }
                    },
                    dismissButton = { TextButton(onClick = { finish() }) { Text("ANNULLA") } },
                )
            }
        }
    }

    private suspend fun addToHome(request: LauncherApps.PinItemRequest, pkg: String, shortcutId: String, label: String) {
        val repository = appContainer.configRepository
        if (repository.config.first().isFull) return
        if (!request.isValid || !request.accept()) return
        repository.update { config ->
            if (config.isFull ||
                config.buttons.any { it.type == ActionType.SHORTCUT && it.packageName == pkg && it.shortcutId == shortcutId }
            ) {
                config
            } else {
                config.copy(
                    buttons = config.buttons + HomeButton(
                        id = UUID.randomUUID().toString(),
                        type = ActionType.SHORTCUT,
                        label = LauncherConfig.normalizeLabel(label),
                        colorArgb = ButtonPalette.next(config.buttons),
                        packageName = pkg,
                        shortcutId = shortcutId,
                    ),
                )
            }
        }
    }
}
