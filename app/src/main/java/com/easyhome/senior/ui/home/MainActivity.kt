package com.easyhome.senior.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.domain.model.CalendarSwipe
import com.easyhome.senior.domain.model.CalendarTarget
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.service.OverlayService
import com.easyhome.senior.system.LaunchResult
import com.easyhome.senior.ui.admin.AdminActivity
import com.easyhome.senior.ui.theme.EasyHomeTheme

/** Launcher di sistema (CATEGORY_HOME) per l'utente anziano. */
class MainActivity : ComponentActivity() {

    private val container by lazy { appContainer }
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.factory(container) }

    /** Visibilità della domanda anti-errore; azzerata ad ogni pressione del tasto Home. */
    private var showAdminGate by mutableStateOf(false)

    /** Agenda semplificata aperta con lo swipe. */
    private var showAgenda by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        hideStatusBar()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Il tasto/gesto Indietro non deve mai far uscire l'anziano dalla Home.
            BackHandler(enabled = true) {
                if (showAgenda) showAgenda = false else viewModel.dismissMessage()
            }

            // Luminosità adattiva/fissa: riapplicata ogni volta che cambia la configurazione.
            LaunchedEffect(state.isLoaded, state.config.brightnessMode, state.config.brightnessLevel) {
                if (state.isLoaded) applyBrightness(state.config)
            }

            EasyHomeTheme(theme = state.config.theme) {
                HomeScreen(
                    state = state,
                    iconLoader = container.appsRepository::loadIcon,
                    onPageChange = { page ->
                        container.feedbackManager.buttonFeedback(state.config)
                        viewModel.goToPage(page)
                    },
                    onButtonClick = { button ->
                        // Feedback immediato, prima ancora di avviare l'Intent.
                        container.feedbackManager.buttonFeedback(state.config)
                        val result = container.actionLauncher.launch(this@MainActivity, button)
                        if (result is LaunchResult.Failure) viewModel.showMessage(result.message)
                    },
                    onCalendarSwipe = {
                        container.feedbackManager.buttonFeedback(state.config)
                        when (state.config.calendarTarget) {
                            CalendarTarget.SYSTEM_APP -> openSystemCalendar()
                            CalendarTarget.AGENDA -> {
                                viewModel.loadAgenda()
                                showAgenda = true
                            }
                        }
                    },
                    onAdminHoldCompleted = {
                        container.feedbackManager.vibrate(state.config.vibration, short = false)
                        showAdminGate = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )
                val fromLeft = state.config.calendarSwipe != CalendarSwipe.FROM_RIGHT
                AnimatedVisibility(
                    visible = showAgenda,
                    enter = slideInHorizontally { width -> if (fromLeft) -width else width },
                    exit = slideOutHorizontally { width -> if (fromLeft) -width else width },
                ) {
                    val agenda by viewModel.agenda.collectAsStateWithLifecycle()
                    AgendaScreen(
                        state = agenda,
                        fontSizeSp = state.config.fontSizeSp,
                        // Si chiude scorrendo nel verso opposto a quello di apertura.
                        closeTowardRight = !fromLeft,
                        onOpenSystemCalendar = {
                            container.feedbackManager.buttonFeedback(state.config)
                            openSystemCalendar()
                        },
                        onClose = {
                            container.feedbackManager.buttonFeedback(state.config)
                            showAgenda = false
                        },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    )
                }
                if (showAdminGate) {
                    AdminGateDialog(
                        onDismiss = { showAdminGate = false },
                        onSuccess = {
                            showAdminGate = false
                            startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
        val state = viewModel.uiState.value
        if (state.isLoaded) applyBrightness(state.config)
        OverlayService.startIfPermitted(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Premuto "Home" (o "Torna all'inizio") mentre si è già sulla Home: stato pulito.
        showAdminGate = false
        showAgenda = false
        viewModel.dismissMessage()
        viewModel.resetPage()
    }

    private fun openSystemCalendar() {
        val result = container.actionLauncher.openSystemCalendar(this)
        if (result is LaunchResult.Failure) viewModel.showMessage(result.message)
    }

    private fun applyBrightness(config: LauncherConfig) {
        container.brightnessController.applySystem(config)
        container.brightnessController.applyToWindow(this, config)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    /**
     * La barra di stato di sistema viene nascosta (ora e batteria sono già nella TopBar gigante):
     * si riducono così le aperture accidentali della tendina notifiche.
     */
    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}
