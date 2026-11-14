package com.easyhome.senior.system

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sa se una qualsiasi Activity del launcher (Home o configuratore) è visibile.
 * Quando è false l'utente si trova dentro un'app esterna: il servizio overlay
 * mostra allora il pulsante "Torna all'inizio". Nessun permesso aggiuntivo richiesto.
 */
object AppForegroundTracker : DefaultLifecycleObserver {

    private val _isLauncherVisible = MutableStateFlow(false)
    val isLauncherVisible: StateFlow<Boolean> = _isLauncherVisible.asStateFlow()

    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        _isLauncherVisible.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        _isLauncherVisible.value = false
    }
}
