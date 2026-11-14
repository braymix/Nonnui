package com.easyhome.senior.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val MAX_ATTEMPTS = 3
private const val AUTO_CLOSE_MS = 60_000L

/** Domanda anti-errore che separa l'anziano dalla configurazione. */
@Composable
fun AdminGateDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(2, 10) }
    val b = rememberSaveable { Random.nextInt(1, 10) }
    var answer by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }

    // Se l'anziano apre la finestra per sbaglio, si richiude da sola.
    LaunchedEffect(Unit) {
        delay(AUTO_CLOSE_MS)
        onDismiss()
    }

    fun check() {
        if (answer.toIntOrNull() == a + b) {
            onSuccess()
        } else {
            attempts++
            error = true
            answer = ""
            if (attempts >= MAX_ATTEMPTS) onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accesso familiari", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Quanto fa $a + $b?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { value ->
                        answer = value.filter { it.isDigit() }.take(3)
                        error = false
                    },
                    singleLine = true,
                    isError = error,
                    label = { Text("Risposta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { check() }),
                )
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Risposta errata (tentativo $attempts di $MAX_ATTEMPTS)",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { check() }) { Text("ENTRA") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } },
    )
}
