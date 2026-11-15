# EasyHome Senior

Launcher Android ultra-semplificato per persone anziane, con tastiera alfabetica **ABCD** integrata,
pulsante galleggiante **"🏠 TORNA ALL'INIZIO"** e configuratore guidato a prova di errore per i familiari.

- **Pulsanti illimitati**, disposti a **lista o griglia (2 o 3 colonne)** con righe per schermata
  configurabili; se non entrano tutti, la Home si sfoglia con grandi tasti AVANTI / INDIETRO (niente swipe).
- **Scorciatoie intelligenti**: chiamata, videochiamata e chiamata WhatsApp, chat WhatsApp o Telegram di
  un contatto, gruppi tramite link di invito, e chat/gruppi da "Aggiungi scorciatoia" di WhatsApp e Telegram.
- **Luminosità** del telefono: come da sistema, **adattiva** (sensore) o fissa.
- Temi a colori pieni (chiaro, scuro, alto contrasto): nessun colore pastello.

- Kotlin 2.0 · Jetpack Compose (Material 3) · MVVM + Clean Architecture (`StateFlow` immutabili)
- Jetpack DataStore (Preferences) · minSdk 26 · target/compile SDK 35
- Nessuna libreria di terze parti: solo AndroidX e API di piattaforma (`PackageManager`,
  `InputMethodService`, `WindowManager`, `Settings`, `VibratorManager`, `ToneGenerator`, `org.json`)

## Compilazione

```bash
./gradlew :app:assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest      # test JVM (backup JSON, normalizzazione config)
./gradlew :app:lintDebug
```

## Struttura

```
app/src/main/
├── AndroidManifest.xml                     HOME launcher, IME, overlay FGS, boot receiver, FileProvider
├── res/  (values, xml/method.xml, xml/file_paths.xml, drawable, mipmap-anydpi)
└── java/com/easyhome/senior/
    ├── EasyHomeApp.kt                      Application: crea il grafo DI, installa il tracker foreground
    ├── di/AppContainer.kt                  Dependency injection manuale (singleton di processo)
    ├── domain/
    │   ├── model/LauncherConfig.kt         HomeButton, ActionType, AppTheme, limiti, palette colori
    │   ├── model/DeviceModels.kt           BatteryStatus, InstalledApp, PickedContact
    │   └── repository/Repositories.kt      Interfacce ConfigRepository / AppsRepository / DeviceStatusRepository
    ├── data/
    │   ├── config/DataStoreConfigRepository.kt   Persistenza DataStore (Preferences)
    │   ├── config/ConfigJsonSerializer.kt        JSON per DataStore + backup/ripristino validato
    │   ├── apps/PackageManagerAppsRepository.kt  App lanciabili + icone con LruCache
    │   └── device/AndroidDeviceStatusRepository.kt  Batteria, connettività, orologio (Flow)
    ├── system/
    │   ├── ActionLauncher.kt               App / chiamata (CALL→DIAL) / videochiamata WhatsApp (→ chat)
    │   ├── ContactResolver.kt              Rubrica e righe "vnd.com.whatsapp.video.call"
    │   ├── FeedbackManager.kt              Vibrazione (VibrationEffect) + suono (ToneGenerator)
    │   ├── PermissionChecker.kt            Semaforo permessi + Intent di configurazione
    │   ├── AppForegroundTracker.kt         Launcher visibile? (ProcessLifecycleOwner)
    │   └── BackupSharer.kt                 ACTION_SEND testo / file tramite FileProvider
    ├── ui/
    │   ├── theme/Theme.kt                  Chiaro, Scuro, Alto contrasto (giallo su nero)
    │   ├── common/SeniorTapModifier.kt     Tocco tollerante (slop ampio, debounce) + pressione ripetuta
    │   ├── common/AutoSizeText.kt          Testo che si rimpicciolisce senza spezzare parole
    │   ├── common/AppIcon.kt, SeniorIcons.kt  Icone app, batteria e videocamera disegnate a mano
    │   ├── home/MainActivity.kt            Activity HOME: BackHandler, status bar nascosta, avvio overlay
    │   ├── home/HomeViewModel.kt           HomeUiState (config + ora + batteria + rete)
    │   ├── home/HomeScreen.kt              Griglia 1-4 pulsanti giganti, banner batteria/rete
    │   ├── home/StatusTopBar.kt            Orologio (pressione 5 s con anello), data, batteria
    │   ├── home/AdminGateDialog.kt         Domanda anti-errore "Quanto fa a + b?"
    │   └── admin/                          Wizard a 4 passi con anteprima dal vivo
    │       ├── AdminActivity.kt, AdminScreen.kt, AdminViewModel.kt
    │       ├── ButtonsStep.kt              1. App/contatti, etichette, colori, ordine (max 4)
    │       ├── AppearanceStep.kt           2. Tema, font 24-48 sp, icone, suono, vibrazione
    │       ├── PermissionsStep.kt          3. Semaforo permessi (aggiornato a ON_RESUME)
    │       ├── BackupStep.kt               4. Export/import JSON (copia, condividi, file)
    │       └── LivePreview.kt              HomeScreen reale rimpicciolita
    ├── ui/pin/ConfirmPinShortcutActivity.kt  Riceve "Aggiungi scorciatoia" da WhatsApp/Telegram
    ├── system/BrightnessController.kt  Luminosità adattiva/fissa (WRITE_SETTINGS) + ripiego sulla finestra
    ├── service/OverlayService.kt           Pulsante "TORNA ALL'INIZIO" (TYPE_APPLICATION_OVERLAY)
    ├── service/BootReceiver.kt             Riattiva l'overlay dopo riavvio/aggiornamento
    └── ime/
        ├── AbcdInputMethodService.kt       InputMethodService con Compose (Lifecycle/SavedState owner)
        └── AbcdKeyboard.kt                 Layout A-Z, 123/ABC, SPAZIO 75%, CANCELLA rosso, INVIO verde
```

## Primo avvio (familiare)

1. Installare l'APK e aprire **EasyHome Senior**.
2. Tenere premuto l'**orologio** per 5 secondi e rispondere alla domanda.
3. Seguire i 4 passi. Al passo 3 tutte le voci devono diventare **VERDI**:
   launcher predefinito, tastiera ABCD (abilitata + selezionata), overlay, esclusione risparmio energetico,
   più (consigliati) chiamate dirette, luminosità e contatti per chiamate/chat WhatsApp dirette.

### Aggiungere una chat o un gruppo WhatsApp/Telegram

- **Dal configuratore**: AGGIUNGI PULSANTE → "Chat o gruppo (scorciatoia app)" mostra le chat recenti
  pubblicate da WhatsApp/Telegram (serve che EasyHome sia il launcher predefinito).
- **Dall'app**: WhatsApp → chat/gruppo → ⋮ → Altro → *Aggiungi scorciatoia*; Telegram → profilo → ⋮ →
  *Aggiungi alla schermata Home*. EasyHome chiede conferma e crea il pulsante.
- **Tramite link**: "Gruppo tramite link" con il link di invito (`chat.whatsapp.com/...` o `t.me/...`).

## APK automatico

Ogni push esegue test e build su GitHub Actions e aggiorna la release
[`ultima-build`](../../releases/tag/ultima-build), scaricabile direttamente dal telefono.

Ogni modifica è salvata immediatamente: si può uscire in qualsiasi momento.

## Scelte progettuali e limiti noti

- **Indietro** è intercettato con `BackHandler`. Android non consente a un'app normale di disabilitare i
  gesti Home/Recenti; essendo però il launcher predefinito, Home riporta sempre qui. La barra di stato è
  nascosta per ridurre le aperture accidentali della tendina.
- **Pagine**: con più pulsanti di quanti ne entrino (colonne × righe) la Home si sfoglia a pagine; il tasto
  Home/"Torna all'inizio" riporta sempre alla prima.
- **Tocco**: l'azione parte al rilascio del dito se questo è dentro il pulsante allargato di 48 dp, quindi
  vale sia il tap rapido sia la pressione prolungata accidentale; tocchi ripetuti entro 700 ms sono ignorati.
- **Overlay**: visibile solo quando nessuna Activity del launcher è in primo piano (`ProcessLifecycleOwner`):
  non servono permessi di Usage Access né un servizio di accessibilità. La posizione (in basso / in alto) è
  configurabile, perché in basso può sovrapporsi alla riga di scrittura delle chat.
- **WhatsApp** (videochiamata, chiamata, chat): usa le righe di rubrica `vnd.com.whatsapp.*` (richiede
  READ_CONTACTS); se mancano, apre la chat via `wa.me` con il numero normalizzato in formato internazionale.
  Dopo un import su un altro telefono gli ID vengono ricalcolati dal numero.
- **Telegram**: chat tramite `tg://resolve?phone=`; funziona se il numero è visibile su Telegram, altrimenti
  conviene la scorciatoia "Aggiungi alla schermata Home" o il link `t.me/...`.
- **Gruppi WhatsApp**: Android non offre un modo per aprire un gruppo tramite ID; si usano la scorciatoia
  dell'app oppure il link di invito, che per chi è già membro apre direttamente la chat del gruppo.
- **Luminosità adattiva/fissa** per tutto il telefono richiede il permesso speciale "Modifica impostazioni di
  sistema"; senza, la luminosità fissa vale solo sulla Home.
- **Batteria**: il banner lampeggiante (1 Hz, sicuro per fotosensibili) appare sotto il 20% se il telefono
  non è in carica.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` è soggetto alle policy di Google Play: per la distribuzione
  sul Play Store va motivato (servizio di accessibilità sempre attivo) o sostituito dal solo link alle impostazioni.
