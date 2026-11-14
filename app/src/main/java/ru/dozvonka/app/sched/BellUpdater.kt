package ru.dozvonka.app.sched

import android.content.Context
import ru.dozvonka.app.core.BellContent
import ru.dozvonka.app.core.ScheduleEngine
import ru.dozvonka.app.data.ScheduleRepository
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.prefs.Prefs
import ru.dozvonka.app.widget.WidgetRenderer
import ru.dozvonka.app.prefs.AppClock

/**
 * Одна точка обновления: посмотреть на часы и расписание →
 * обновить уведомление и виджеты → завести будильник на следующий звонок.
 * Вызывается отовсюду: будильник, перезагрузка, виджет, открытие приложения, WorkManager.
 */
object BellUpdater {

    @Synchronized
    fun refresh(context: Context) {
        val app = context.applicationContext
        val now = AppClock.now()
        val state = ScheduleEngine.stateAt(ScheduleRepository.get(app), now)
        val active = ScheduleEngine.isActive(state, now)
        val content = BellContent.from(state)

        if (active && content != null && Prefs.notificationEnabled(app)) {
            BellNotification.show(app, BellNotification.build(app, content, now))
            if (Prefs.boostMode(app)) BellForegroundService.ensureRunning(app)
            else BellForegroundService.stop(app)
        } else {
            BellForegroundService.stop(app)
            BellNotification.cancel(app)
        }

        WidgetRenderer.render(app, state, now)
        BellScheduler.schedule(app, state, now, active)
    }
}
