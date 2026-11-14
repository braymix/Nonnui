# EasyHome Senior

Launcher Android ultra-semplificato per persone anziane, con tastiera alfabetica **ABCD** integrata,
pulsante galleggiante **"🏠 TORNA ALL'INIZIO"** e configuratore guidato a prova di errore per i familiari.

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
   più (consigliati) chiamate dirette e contatti per la videochiamata WhatsApp.

Ogni modifica è salvata immediatamente: si può uscire in qualsiasi momento.

## Scelte progettuali e limiti noti

- **Indietro** è intercettato con `BackHandler`. Android non consente a un'app normale di disabilitare i
  gesti Home/Recenti; essendo però il launcher predefinito, Home riporta sempre qui. La barra di stato è
  nascosta per ridurre le aperture accidentali della tendina.
- **Tocco**: l'azione parte al rilascio del dito se questo è dentro il pulsante allargato di 48 dp, quindi
  vale sia il tap rapido sia la pressione prolungata accidentale; tocchi ripetuti entro 700 ms sono ignorati.
- **Overlay**: visibile solo quando nessuna Activity del launcher è in primo piano (`ProcessLifecycleOwner`):
  non servono permessi di Usage Access né un servizio di accessibilità. La posizione (in basso / in alto) è
  configurabile, perché in basso può sovrapporsi alla riga di scrittura delle chat.
- **Videochiamata WhatsApp**: usa la riga di rubrica `vnd.com.whatsapp.video.call` (richiede READ_CONTACTS);
  se manca il permesso o il contatto non è su WhatsApp, apre la chat (`wa.me`) da cui la videochiamata è a un
  tocco. Dopo un import su un altro telefono gli ID vengono ricalcolati dal numero.
- **Batteria**: il banner lampeggiante (1 Hz, sicuro per fotosensibili) appare sotto il 20% se il telefono
  non è in carica.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` è soggetto alle policy di Google Play: per la distribuzione
  sul Play Store va motivato (servizio di accessibilità sempre attivo) o sostituito dal solo link alle impostazioni.
