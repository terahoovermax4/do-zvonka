package ru.dozvonka.app.sched

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.dozvonka.app.core.BellState
import ru.dozvonka.app.core.ScheduleEngine
import java.time.LocalDateTime
import ru.dozvonka.app.prefs.AppClock

/** Будильники: точный — ровно на звонок, и «подкрутка» прогресса раз в 5 минут во время уроков. */
object BellScheduler {
    private const val RC_BELL = 10
    private const val RC_REFRESH = 11
    private const val REFRESH_MS = 5 * 60_000L

    fun canExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    fun schedule(context: Context, state: BellState, now: LocalDateTime, active: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return

        // +1 секунда, чтобы точно оказаться уже «после» звонка.
        val at = AppClock.realMillis(ScheduleEngine.nextWakeup(state, now)) + 1_000
        val bell = pending(context, RC_BELL)
        try {
            if (canExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, bell)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, bell)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, bell)
        }

        val refresh = pending(context, RC_REFRESH)
        if (active) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + REFRESH_MS, refresh)
        } else {
            am.cancel(refresh)
        }
    }

    private fun pending(context: Context, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        code,
        Intent(context, BellAlarmReceiver::class.java).setAction("ru.dozvonka.app.BELL_$code"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
