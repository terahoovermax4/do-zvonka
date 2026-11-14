package ru.dozvonka.app.notify

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.annotation.RequiresApi
import ru.dozvonka.app.R
import ru.dozvonka.app.core.BellContent
import java.time.LocalDateTime
import ru.dozvonka.app.prefs.AppClock

/**
 * Android 16+: «Live Update» — уведомление закрепляется наверху,
 * показывается развёрнутым на экране блокировки и значком-таймером в строке состояния.
 * Для этого нельзя использовать свою вёрстку — только системный ProgressStyle.
 */
@RequiresApi(36)
object LiveUpdate {

    /** Ключ запроса «поднять» уведомление до Live Update (тот же, что использует androidx). */
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    fun build(context: Context, c: BellContent, now: LocalDateTime): Notification {
        val accent = BellNotification.accent(context, c)
        val endMs = AppClock.realMillis(c.end)
        val nowMs = System.currentTimeMillis()

        val style = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(listOf(Notification.ProgressStyle.Segment(1000).setColor(accent)))
            .setProgress((c.progress(now) * 1000).toInt())

        return Notification.Builder(context, BellNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(c.title)
            .setContentText("${c.countdownLabel} · ${c.nextLine}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(BellNotification.openApp(context))
            .setTimeoutAfter((endMs - nowMs).coerceAtLeast(0) + 3 * 60_000L)
            .setStyle(style)
            .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
            .build()
    }
}
