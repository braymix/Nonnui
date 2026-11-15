package com.easyhome.senior.ui.admin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.AppShortcut
import com.easyhome.senior.system.ContactResolver
import com.easyhome.senior.domain.model.ButtonPalette
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.InstalledApp
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.ui.common.rememberAppIcon
import com.easyhome.senior.ui.home.IconLoader

/** Selettore di un numero dalla rubrica: concede un permesso temporaneo sulla sola riga scelta. */
class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}

private enum class AddChoice(val title: String, val subtitle: String) {
    APP("App", "Apre un'app installata (es. WhatsApp, Fotocamera)"),
    PHONE("Chiamata a un contatto", "Telefonata diretta"),
    WA_VIDEO("Videochiamata WhatsApp", "Videochiamata diretta a un contatto"),
    WA_VOICE("Chiamata WhatsApp", "Chiamata vocale WhatsApp a un contatto"),
    WA_CHAT("Chat WhatsApp di un contatto", "Apre direttamente la conversazione"),
    TG_CHAT("Chat Telegram di un contatto", "Apre la conversazione Telegram tramite numero"),
    SHORTCUT("Chat o gruppo (scorciatoia app)", "Chat e gruppi recenti di WhatsApp, Telegram e altre app"),
    LINK("Gruppo tramite link", "Link di invito WhatsApp (chat.whatsapp.com) o Telegram (t.me)"),
}

@Composable
fun ButtonsStep(state: AdminUiState, viewModel: AdminViewModel, iconLoader: IconLoader) {
    val context = LocalContext.current
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    var showShortcutPicker by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var pendingContactType by rememberSaveable { mutableStateOf<ActionType?>(null) }
    var buttonToDelete by remember { mutableStateOf<HomeButton?>(null) }

    val pickContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        val type = pendingContactType
        pendingContactType = null
        if (uri != null && type != null) viewModel.onContactPicked(uri, type)
    }
    // Qualunque sia la risposta, si prosegue: senza permesso si usano i ripieghi (DIAL / chat).
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions()
        pickContact.launch(Unit)
    }

    fun startContactFlow(type: ActionType) {
        pendingContactType = type
        val permission = when (type) {
            ActionType.PHONE_CALL -> Manifest.permission.CALL_PHONE
            ActionType.TELEGRAM_CHAT -> null
            else -> Manifest.permission.READ_CONTACTS
        }
        if (permission == null ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        ) {
            pickContact.launch(Unit)
        } else {
            requestPermission.launch(permission)
        }
    }

    fun onChoice(choice: AddChoice) {
        showAddMenu = false
        when (choice) {
            AddChoice.APP -> showAppPicker = true
            AddChoice.PHONE -> startContactFlow(ActionType.PHONE_CALL)
            AddChoice.WA_VIDEO -> startContactFlow(ActionType.WHATSAPP_VIDEO)
            AddChoice.WA_VOICE -> startContactFlow(ActionType.WHATSAPP_VOICE)
            AddChoice.WA_CHAT -> startContactFlow(ActionType.WHATSAPP_CHAT)
            AddChoice.TG_CHAT -> startContactFlow(ActionType.TELEGRAM_CHAT)
            AddChoice.SHORTCUT -> {
                viewModel.loadShortcuts()
                showShortcutPicker = true
            }
            AddChoice.LINK -> showLinkDialog = true
        }
    }

    val config = state.config
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Aggiungi tutti i pulsanti che servono. Se non entrano in una schermata, " +
                "sulla Home compaiono i tasti AVANTI / INDIETRO (la disposizione si sceglie al passo 2).",
            style = MaterialTheme.typography.bodyLarge,
        )
        LivePreview(config, iconLoader, Modifier.align(Alignment.CenterHorizontally))
        Text(
            "${config.buttons.size} pulsanti · ${config.pageCount} " +
                (if (config.pageCount == 1) "schermata" else "schermate"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        config.buttons.forEachIndexed { index, button ->
            ButtonEditorCard(
                button = button,
                index = index,
                total = config.buttons.size,
                description = AdminViewModel.describe(button, state.apps),
                notInstalled = (button.type == ActionType.APP || button.type == ActionType.SHORTCUT) &&
                    !state.appsLoading && button.packageName !in state.installedPackages,
                onLabelChange = { viewModel.updateLabel(button.id, it) },
                onColorChange = { viewModel.updateColor(button.id, it) },
                onMoveUp = { viewModel.moveButton(button.id, -1) },
                onMoveDown = { viewModel.moveButton(button.id, +1) },
                onDelete = { buttonToDelete = button },
            )
        }

        Button(
            onClick = { showAddMenu = true },
            enabled = !config.isFull,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("AGGIUNGI PULSANTE", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Suggerimento: chat e gruppi", fontWeight = FontWeight.Bold)
                Text(
                    "In WhatsApp apri la chat o il gruppo → menu ⋮ → Altro → \"Aggiungi scorciatoia\". " +
                        "In Telegram: profilo del gruppo → ⋮ → \"Aggiungi alla schermata Home\". " +
                        "Il pulsante comparirà qui da solo (EasyHome deve essere il launcher predefinito).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showAddMenu) {
        Dialog(onDismissRequest = { showAddMenu = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cosa vuoi aggiungere?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showAddMenu = false }) { Icon(Icons.Filled.Close, contentDescription = "Chiudi") }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AddChoice.entries.toList()) { choice ->
                            OutlinedCard(
                                onClick = { onChoice(choice) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(choice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(choice.subtitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = state.apps,
            loading = state.appsLoading,
            iconLoader = iconLoader,
            onPick = {
                showAppPicker = false
                viewModel.addApp(it)
            },
            onDismiss = { showAppPicker = false },
        )
    }

    if (showShortcutPicker) {
        ShortcutPickerDialog(
            shortcuts = state.shortcuts,
            loading = state.shortcutsLoading,
            canUseShortcuts = state.canUseShortcuts,
            iconLoader = iconLoader,
            onPick = {
                showShortcutPicker = false
                viewModel.addShortcut(it)
            },
            onDismiss = { showShortcutPicker = false },
        )
    }

    if (showLinkDialog) {
        LinkDialog(
            onConfirm = { label, link ->
                showLinkDialog = false
                viewModel.addLink(label, link)
            },
            onDismiss = { showLinkDialog = false },
        )
    }

    buttonToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { buttonToDelete = null },
            title = { Text("Eliminare il pulsante?") },
            text = { Text("Il pulsante \"${target.label}\" verrà rimosso dalla Home.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeButton(target.id)
                        buttonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("ELIMINA") }
            },
            dismissButton = { TextButton(onClick = { buttonToDelete = null }) { Text("ANNULLA") } },
        )
    }
}

@Composable
private fun LinkDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var label by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gruppo tramite link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "WhatsApp: info gruppo → Invita tramite link → Copia link.\n" +
                        "Telegram: info gruppo/canale → link t.me/...\n" +
                        "Se la persona è già nel gruppo, il link apre direttamente la chat.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it.trim() },
                    label = { Text("Link (es. https://chat.whatsapp.com/...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { clipboard.getText()?.text?.let { link = it.trim() } }) {
                    Text("INCOLLA DAGLI APPUNTI")
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = LauncherConfig.normalizeLabel(it) },
                    label = { Text("Nome del pulsante (es. FAMIGLIA)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(label, link) }, enabled = link.isNotBlank()) { Text("AGGIUNGI") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } },
    )
}

@Composable
private fun ShortcutPickerDialog(
    shortcuts: List<AppShortcut>,
    loading: Boolean,
    canUseShortcuts: Boolean,
    iconLoader: IconLoader,
    onPick: (AppShortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(shortcuts, query) {
        val q = query.trim()
        val list = if (q.isEmpty()) shortcuts else shortcuts.filter {
            it.label.contains(q, ignoreCase = true) || it.appLabel.contains(q, ignoreCase = true)
        }
        // Prima le app di messaggistica, poi le altre.
        list.sortedWith(compareBy({ !isMessaging(it.packageName) }, { it.appLabel.lowercase() }, { it.label.lowercase() }))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat, gruppi e scorciatoie", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Chiudi") }
                }
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    !canUseShortcuts -> Text(
                        "Per leggere chat e gruppi delle app, EasyHome deve essere il LAUNCHER PREDEFINITO. " +
                            "Impostalo al passo 3 e riprova.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                    else -> {
                        Text(
                            "WhatsApp e Telegram mostrano qui le chat usate di recente. Se una chat manca, " +
                                "aprila e scrivi un messaggio, oppure usa \"Aggiungi scorciatoia\" dall'app.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            label = { Text("Cerca") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        if (filtered.isEmpty()) {
                            Text("Nessuna scorciatoia disponibile.", modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(filtered, key = { it.packageName + "/" + it.shortcutId }) { sc ->
                                    ShortcutRow(sc, iconLoader) { onPick(sc) }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isMessaging(pkg: String) =
    pkg == ContactResolver.WHATSAPP_PACKAGE || pkg == "com.whatsapp.w4b" || pkg in ContactResolver.TELEGRAM_PACKAGES

@Composable
private fun ShortcutRow(shortcut: AppShortcut, iconLoader: IconLoader, onClick: () -> Unit) {
    val sizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val icon = rememberAppIcon(shortcut.packageName, shortcut.shortcutId, sizePx, iconLoader)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        val bmp = icon
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape))
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(shortcut.label, style = MaterialTheme.typography.titleMedium)
            Text(shortcut.appLabel, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
    }
}

@Composable
private fun ButtonEditorCard(
    button: HomeButton,
    index: Int,
    total: Int,
    description: String,
    notInstalled: Boolean,
    onLabelChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    // Stato locale del campo: evita salti del cursore durante il salvataggio asincrono.
    var label by remember(button.id) { mutableStateOf(button.label) }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PULSANTE ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta su")
                }
                IconButton(onClick = onMoveDown, enabled = index < total - 1) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta giù")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (notInstalled) {
                Text(
                    "ATTENZIONE: app non installata su questo telefono.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
            OutlinedTextField(
                value = label,
                onValueChange = { value ->
                    label = LauncherConfig.normalizeLabel(value)
                    onLabelChange(label)
                },
                label = { Text("Etichetta (es. MESSAGGI)") },
                singleLine = true,
                supportingText = { Text("${label.length}/${LauncherConfig.MAX_LABEL_LENGTH}") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Colore", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ButtonPalette.colors.forEach { (name, argb) ->
                    val selected = argb == button.colorArgb
                    val color = Color(argb)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (selected) 4.dp else 1.dp, if (selected) Color.Black else Color.Gray, CircleShape)
                            .clickable { onColorChange(argb) }
                            .semantics { contentDescription = "Colore $name" + if (selected) ", selezionato" else "" },
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.45f) Color.Black else Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    loading: Boolean,
    iconLoader: IconLoader,
    onPick: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Scegli un'app", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Chiudi") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Cerca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    filtered.isEmpty() -> Text("Nessuna app trovata.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppRow(app, iconLoader) { onPick(app) }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, iconLoader: IconLoader, onClick: () -> Unit) {
    val sizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val icon = rememberAppIcon(app.packageName, null, sizePx, iconLoader)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        val bmp = icon
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(44.dp))
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
    }
}

