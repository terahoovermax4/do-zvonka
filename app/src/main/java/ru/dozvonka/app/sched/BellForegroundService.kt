package ru.dozvonka.app.sched

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import ru.dozvonka.app.core.BellContent
import ru.dozvonka.app.core.ScheduleEngine
import ru.dozvonka.app.data.ScheduleRepository
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.prefs.Prefs
import ru.dozvonka.app.prefs.AppClock

/**
 * «Усиленный режим». Работает только во время уроков и держит то же самое уведомление.
 * Прошивки вроде Funtouch/OriginOS (Vivo) и HiOS (Tecno) почти не трогают такие службы,
 * поэтому таймер не пропадает. Раз в минуту обновляет прогресс.
 */
class BellForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            BellUpdater.refresh(applicationContext)
            if (running) handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val now = AppClock.now()
        val state = ScheduleEngine.stateAt(ScheduleRepository.get(this), now)
        val content = BellContent.from(state)
        val active = content != null && ScheduleEngine.isActive(state, now) && Prefs.notificationEnabled(this)

        val notification = if (content != null) BellNotification.build(this, content, now)
        else BellNotification.placeholder(this)

        // startForeground нужно вызвать в любом случае, иначе система уронит приложение.
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    BellNotification.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(BellNotification.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            running = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (!active) {
            running = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        running = true
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, TICK_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(ticker)
        // Оставляем уведомление на месте — его дальше обновляет BellUpdater.
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 2 * 60_000L

        @Volatile
        var running = false
            private set

        fun ensureRunning(context: Context) {
            if (running) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, BellForegroundService::class.java))
            } catch (e: Exception) {
                // Из фона Android 12+ иногда не даёт запуститься — ничего страшного,
                // обычное уведомление уже показано, а будильники продолжают работать.
            }
        }

        fun stop(context: Context) {
            if (!running) return
            context.stopService(Intent(context, BellForegroundService::class.java))
        }
    }
}
