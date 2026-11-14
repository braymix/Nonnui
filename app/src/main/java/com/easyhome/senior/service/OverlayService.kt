package com.easyhome.senior.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.easyhome.senior.R
import com.easyhome.senior.di.appContainer
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.OverlayPosition
import com.easyhome.senior.system.AppForegroundTracker
import com.easyhome.senior.ui.home.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service che mostra, SOLO quando l'utente è dentro un'app esterna,
 * un pulsante fisso e inamovibile "🏠 TORNA ALL'INIZIO" (TYPE_APPLICATION_OVERLAY).
 */
class OverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var shownPosition: OverlayPosition? = null
    private var observeJob: Job? = null
    private var latestConfig = LauncherConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground va chiamato SEMPRE dopo startForegroundService(), anche se poi ci si ferma,
        // altrimenti il sistema termina il processo (ForegroundServiceDidNotStartInTimeException).
        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (observeJob == null) {
            observeJob = scope.launch {
                combine(
                    AppForegroundTracker.isLauncherVisible,
                    appContainer.configRepository.config,
                ) { launcherVisible, config -> launcherVisible to config }
                    .collect { (launcherVisible, config) ->
                        latestConfig = config
                        val shouldShow = !launcherVisible && Settings.canDrawOverlays(this@OverlayService)
                        if (shouldShow) showOverlay(config.overlayPosition) else removeOverlay()
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tasto Torna all'inizio", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
                description = "Mantiene attivo il pulsante per tornare alla schermata principale."
            },
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_home)
            .setContentTitle("EasyHome attivo")
            .setContentText("Il tasto \"Torna all'inizio\" è disponibile nelle altre app.")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun showOverlay(position: OverlayPosition) {
        if (overlayView != null && shownPosition == position) return
        removeOverlay()
        val view = createOverlayView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Non prende il focus: la tastiera e il resto dell'app sottostante restano utilizzabili.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or if (position == OverlayPosition.BOTTOM) Gravity.BOTTOM else Gravity.TOP
            // In basso resta sopra la barra di navigazione e sopra la riga di scrittura delle chat.
            y = dp(if (position == OverlayPosition.BOTTOM) 96 else 40)
            title = "EasyHomeTornaAllInizio"
        }
        try {
            windowManager.addView(view, params)
            overlayView = view
            shownPosition = position
        } catch (_: WindowManager.BadTokenException) {
            overlayView = null
        } catch (_: SecurityException) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: IllegalArgumentException) {
                // Vista già rimossa dal sistema.
            }
        }
        overlayView = null
        shownPosition = null
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun createOverlayView(): View = TextView(this).apply {
        text = "🏠  TORNA ALL'INIZIO"
        contentDescription = "Torna all'inizio"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        minWidth = dp(280)
        minHeight = dp(76)
        setPadding(dp(28), dp(14), dp(28), dp(14))
        background = GradientDrawable().apply {
            setColor(0xFF0D47A1.toInt())
            cornerRadius = dp(40).toFloat()
            setStroke(dp(4), Color.WHITE)
        }
        elevation = dp(12).toFloat()
        isClickable = true
        isFocusable = false
        // Feedback visivo immediato alla pressione.
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.alpha = 0.7f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
            }
            false
        }
        setOnClickListener { goHome() }
    }

    private fun goHome() {
        appContainer.feedbackManager.buttonFeedback(latestConfig)
        removeOverlay()
        val intent = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        try {
            startActivity(intent)
        } catch (_: RuntimeException) {
            // Ripiego universale: l'Intent HOME è sempre consentito e il launcher predefinito siamo noi.
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val CHANNEL_ID = "overlay_home_button"
        private const val NOTIFICATION_ID = 1001

        /** Avvia il servizio solo se il permesso overlay è stato concesso. Mai crash. */
        fun startIfPermitted(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            } catch (_: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (API 31+) estende IllegalStateException:
                // il servizio verrà avviato al prossimo onResume della Home.
            } catch (_: SecurityException) {
            }
        }
    }
}
