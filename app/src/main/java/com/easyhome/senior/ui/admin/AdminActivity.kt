package com.easyhome.senior.ui.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.service.OverlayService
import com.easyhome.senior.ui.theme.AdminTheme

/** Configuratore guidato a 4 passi per i familiari. Accessibile solo dalla Home protetta. */
class AdminActivity : ComponentActivity() {

    private val container by lazy { appContainer }
    private val viewModel: AdminViewModel by viewModels { AdminViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AdminTheme {
                AdminScreen(
                    state = state,
                    viewModel = viewModel,
                    iconLoader = container.appsRepository::loadIcon,
                    onExit = { finish() },
                    // La tastiera di sistema non deve coprire i campi di testo.
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.ime),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        OverlayService.startIfPermitted(this)
    }
}
