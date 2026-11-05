package ru.dozvonka.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import ru.dozvonka.app.R
import ru.dozvonka.app.core.BellContent
import ru.dozvonka.app.core.BellState
import ru.dozvonka.app.core.toEpochMilli
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.sched.BellUpdater
import java.time.LocalDateTime

/** Виджет 2×2: только таймер. */
class SmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        BellUpdater.refresh(context)
    }
}

/** Виджет 4×2: таймер + «сейчас» и «дальше». */
class WideWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        BellUpdater.refresh(context)
    }
}

/**
 * Рисует оба виджета. Цифры тикают сами (Chronometer считается на стороне
 * рабочего стола), приложение обновляет виджет только на звонках и раз в несколько минут.
 */
object WidgetRenderer {

    fun render(context: Context, state: BellState, now: LocalDateTime) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val content = BellContent.from(state)

        val small = manager.getAppWidgetIds(ComponentName(context, SmallWidgetProvider::class.java))
        if (small.isNotEmpty()) manager.updateAppWidget(small, smallViews(context, state, content, now))

        val wide = manager.getAppWidgetIds(ComponentName(context, WideWidgetProvider::class.java))
        if (wide.isNotEmpty()) manager.updateAppWidget(wide, wideViews(context, state, content, now))
    }

    private fun smallViews(context: Context, state: BellState, c: BellContent?, now: LocalDateTime): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_small)
        rv.setOnClickPendingIntent(R.id.widget_root, BellNotification.openApp(context))
        if (c == null) {
            showIdle(rv, state, now)
            return rv
        }
        showActive(rv)
        rv.setTextViewText(R.id.chip, c.chip.uppercase())
        rv.setTextColor(R.id.chip, BellNotification.accent(context, c))
        setCountdown(rv, c)
        rv.setTextViewText(R.id.label, c.countdownLabel)
        return rv
    }

    private fun wideViews(context: Context, state: BellState, c: BellContent?, now: LocalDateTime): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_wide)
        rv.setOnClickPendingIntent(R.id.widget_root, BellNotification.openApp(context))
        if (c == null) {
            showIdle(rv, state, now)
            return rv
        }
        showActive(rv)
        rv.setTextViewText(R.id.chip, c.chip.uppercase())
        rv.setTextColor(R.id.chip, BellNotification.accent(context, c))
        setCountdown(rv, c)
        rv.setTextViewText(R.id.label, c.countdownLabel)
        rv.setTextViewText(R.id.now_text, c.nowShort)
        rv.setTextViewText(R.id.next_text, c.nextShort)

        val progress = (c.progress(now) * 1000).toInt()
        val shown = if (c.isBreak) R.id.progress_break else R.id.progress_lesson
        val hidden = if (c.isBreak) R.id.progress_lesson else R.id.progress_break
        rv.setViewVisibility(shown, View.VISIBLE)
        rv.setViewVisibility(hidden, View.GONE)
        rv.setProgressBar(shown, 1000, progress, false)
        return rv
    }

    private fun setCountdown(rv: RemoteViews, c: BellContent) {
        val base = SystemClock.elapsedRealtime() + (c.end.toEpochMilli() - System.currentTimeMillis())
        rv.setChronometer(R.id.chrono, base, null, true)
        rv.setChronometerCountDown(R.id.chrono, true)
    }

    private fun showActive(rv: RemoteViews) {
        rv.setViewVisibility(R.id.active_group, View.VISIBLE)
        rv.setViewVisibility(R.id.idle_group, View.GONE)
    }

    private fun showIdle(rv: RemoteViews, state: BellState, now: LocalDateTime) {
        val (title, sub) = BellContent.idleText(state, now)
        rv.setViewVisibility(R.id.active_group, View.GONE)
        rv.setViewVisibility(R.id.idle_group, View.VISIBLE)
        rv.setTextViewText(R.id.idle_title, title)
        rv.setTextViewText(R.id.idle_sub, sub)
    }
}
