package com.easyhome.senior.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.AppTheme
import com.easyhome.senior.system.BackupSharer

@Composable
fun BackupStep(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val json = state.exportJson
    var importText by rememberSaveable { mutableStateOf("") }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.prepareImportFromFile(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Esporta configurazione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Copia o condividi questo testo per clonare la configurazione su un altro telefono.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SelectionContainer {
            OutlinedTextField(
                value = json,
                onValueChange = {},
                readOnly = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp),
            )
        }
        Button(
            onClick = {
                clipboard.setText(AnnotatedString(json))
                viewModel.showMessage("Configurazione copiata negli appunti.")
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("COPIA TESTO") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { BackupSharer.shareAsText(context, json) }, modifier = Modifier.weight(1f)) {
                Text("CONDIVIDI TESTO")
            }
            OutlinedButton(
                onClick = {
                    if (!BackupSharer.shareAsFile(context, json)) {
                        viewModel.showMessage("Nessuna app disponibile per condividere il file.")
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("CONDIVIDI FILE") }
        }

        HorizontalDivider()

        Text("Importa configurazione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("Incolla qui il testo del backup") },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 220.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.prepareImportFromText(importText) }, modifier = Modifier.weight(1f)) {
                Text("IMPORTA TESTO")
            }
            OutlinedButton(
                onClick = { openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.weight(1f),
            ) { Text("APRI FILE") }
        }
    }

    state.pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Sostituire la configurazione?") },
            text = {
                Text(
                    "Il backup contiene ${pending.buttons.size} pulsanti " +
                        "(${pending.buttons.joinToString { it.label }}), tema ${themeName(pending.theme)}, " +
                        "testo ${pending.fontSizeSp} sp.\n\nLa configurazione attuale verrà sostituita.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmImport()
                        importText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("SOSTITUISCI") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text("ANNULLA") } },
        )
    }
}

private fun themeName(theme: AppTheme) = when (theme) {
    AppTheme.LIGHT -> "chiaro"
    AppTheme.DARK -> "scuro"
    AppTheme.HIGH_CONTRAST -> "alto contrasto"
}
