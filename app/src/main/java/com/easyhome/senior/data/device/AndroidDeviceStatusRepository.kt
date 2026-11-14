package com.easyhome.senior.data.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.BatteryStatus
import com.easyhome.senior.domain.repository.DeviceStatusRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AndroidDeviceStatusRepository(context: Context) : DeviceStatusRepository {

    private val appContext = context.applicationContext

    override val battery: Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(parseBattery(intent))
            }
        }
        val sticky = ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(parseBattery(it)) }
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

    override val isOnline: Flow<Boolean> = callbackFlow {
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        fun hasInternet(caps: NetworkCapabilities?): Boolean =
            caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(hasInternet(networkCapabilities))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }
        trySend(hasInternet(cm.getNetworkCapabilities(cm.activeNetwork)))
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    override val clock: Flow<LocalDateTime> = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            // Si risveglia allo scoccare del secondo: aggiornamento puntuale senza drift.
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }.distinctUntilChanged()

    private fun parseBattery(intent: Intent): BatteryStatus {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else 100
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged != 0
        return BatteryStatus(percent.coerceIn(0, 100), charging)
    }
}
