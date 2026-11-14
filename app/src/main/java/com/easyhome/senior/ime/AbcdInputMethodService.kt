package com.easyhome.senior.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.ui.theme.EasyHomeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Tastiera alfabetica ABCD renderizzata in Jetpack Compose.
 * Nessuna autocorrezione, nessun suggerimento, nessuno swipe typing:
 * ogni tasto inserisce esattamente ciò che è scritto sopra.
 */
class AbcdInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val mode = mutableStateOf(KeyboardMode.LETTERS)
    @Volatile private var config = LauncherConfig()

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        scope.launch { appContainer.configRepository.config.collect { config = it } }
    }

    override fun onCreateInputView(): View {
        // Compose cerca i "ViewTree owners" a partire dalla decor view della finestra IME.
        window.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AbcdInputMethodService)
            setViewTreeViewModelStoreOwner(this@AbcdInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@AbcdInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val currentConfig by appContainer.configRepository.config.collectAsState(initial = config)
                EasyHomeTheme(theme = currentConfig.theme) {
                    AbcdKeyboard(
                        mode = mode.value,
                        onText = ::commitText,
                        onDelete = ::deleteBackward,
                        onEnter = ::sendEnter,
                        onToggleMode = ::toggleMode,
                    )
                }
            }
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        mode.value = when (editorInfo.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME -> KeyboardMode.NUMBERS
            else -> KeyboardMode.LETTERS
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        scope.cancel()
        super.onDestroy()
    }

    /** Mai a schermo intero (niente "extract view" che confonde l'utente). */
    override fun onEvaluateFullscreenMode(): Boolean = false

    /** Nessuna barra dei suggerimenti. */
    override fun onCreateCandidatesView(): View? = null

    // ---- Azioni tasti ---------------------------------------------------------------------

    private fun commitText(text: String) {
        feedback()
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteBackward() {
        feedback()
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            // Gestisce correttamente emoji e caratteri composti (surrogate pairs).
            if (!ic.deleteSurroundingTextInCodePoints(1, 0)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        }
    }

    private fun sendEnter() {
        feedback()
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun toggleMode() {
        feedback()
        mode.value = if (mode.value == KeyboardMode.LETTERS) KeyboardMode.NUMBERS else KeyboardMode.LETTERS
    }

    private fun feedback() = appContainer.feedbackManager.keyFeedback(config)
}
