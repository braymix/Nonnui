package com.easyhome.senior.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.easyhome.senior.di.AppContainer
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.repository.ConfigRepository
import com.easyhome.senior.domain.repository.DeviceStatusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Stato immutabile della Home. */
data class HomeUiState(
    val config: LauncherConfig = LauncherConfig(),
    val timeText: String = "--:--",
    val dateText: String = "",
    val battery: BatteryStatus = BatteryStatus(100, false),
    val isOnline: Boolean = true,
    val isLoaded: Boolean = false,
    val message: String? = null,
    /** Pagina corrente quando i pulsanti non entrano in una sola schermata. */
    val page: Int = 0,
)

class HomeViewModel(
    configRepository: ConfigRepository,
    deviceStatusRepository: DeviceStatusRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
    private val page = MutableStateFlow(0)
    private var messageJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        configRepository.config,
        deviceStatusRepository.clock,
        deviceStatusRepository.battery,
        deviceStatusRepository.isOnline,
        combine(message.asStateFlow(), page.asStateFlow()) { msg, p -> msg to p },
    ) { config, now, battery, online, (msg, currentPage) ->
        HomeUiState(
            config = config,
            timeText = formatTime(now),
            dateText = formatDate(now),
            battery = battery,
            isOnline = online,
            isLoaded = true,
            message = msg,
            page = currentPage.coerceIn(0, config.pageCount - 1),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun showMessage(text: String) {
        messageJob?.cancel()
        message.value = text
        messageJob = viewModelScope.launch {
            delay(MESSAGE_DURATION_MS)
            message.value = null
        }
    }

    fun goToPage(index: Int) {
        page.value = index.coerceAtLeast(0)
    }

    /** Tasto Home / "Torna all'inizio": si riparte sempre dalla prima pagina. */
    fun resetPage() {
        page.value = 0
    }

    fun dismissMessage() {
        messageJob?.cancel()
        message.value = null
    }

    companion object {
        private const val MESSAGE_DURATION_MS = 4_000L
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)

        fun formatTime(now: LocalDateTime): String = now.format(TIME_FORMAT)
        fun formatDate(now: LocalDateTime): String = now.format(DATE_FORMAT).uppercase(Locale.ITALIAN)

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(container.configRepository, container.deviceStatusRepository)
            }
        }
    }
}
