package com.easyhome.senior.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.easyhome.senior.ui.home.IconLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    state: AdminUiState,
    viewModel: AdminViewModel,
    iconLoader: IconLoader,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val step = state.step
    val steps = WizardStep.entries
    val scrollState = rememberScrollState()

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }
    LaunchedEffect(step) { scrollState.scrollTo(0) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Configurazione EasyHome", fontWeight = FontWeight.Bold)
                            Text(
                                "Passo ${step.ordinal + 1} di ${steps.size}: ${step.title}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onExit,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                        ) { Text("ESCI", fontWeight = FontWeight.Bold) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                LinearProgressIndicator(
                    progress = { (step.ordinal + 1f) / steps.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                StepTabs(current = step, onSelect = viewModel::goTo)
                HorizontalDivider()
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = viewModel::back,
                        enabled = step.ordinal > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) { Text("INDIETRO") }
                    Spacer(Modifier.width(12.dp))
                    val isLast = step.ordinal == steps.lastIndex
                    Button(
                        onClick = { if (isLast) onExit() else viewModel.next() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) { Text(if (isLast) "FINE" else "AVANTI") }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.isConfigLoaded) return@Box
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            ) {
                when (step) {
                    WizardStep.BUTTONS -> ButtonsStep(state, viewModel, iconLoader)
                    WizardStep.APPEARANCE -> AppearanceStep(state, viewModel, iconLoader)
                    WizardStep.PERMISSIONS -> PermissionsStep(state, viewModel)
                    WizardStep.BACKUP -> BackupStep(state, viewModel)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepTabs(current: WizardStep, onSelect: (WizardStep) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WizardStep.entries.forEach { s ->
            val label = "${s.ordinal + 1}"
            if (s == current) {
                Button(onClick = { onSelect(s) }, modifier = Modifier.weight(1f)) {
                    Text(label, textAlign = TextAlign.Center)
                }
            } else {
                OutlinedButton(onClick = { onSelect(s) }, modifier = Modifier.weight(1f)) {
                    Text(label, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
