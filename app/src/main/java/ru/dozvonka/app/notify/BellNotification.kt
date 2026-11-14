package ru.dozvonka.app.notify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.dozvonka.app.R
import ru.dozvonka.app.core.BellContent
import ru.dozvonka.app.prefs.Prefs
import ru.dozvonka.app.ui.MainActivity
import java.time.LocalDateTime
import ru.dozvonka.app.prefs.AppClock

/**
 * Уведомление с обратным отсчётом. Таймер тикает силами самой системы
 * (Chronometer), поэтому идёт, даже если приложение выгружено из памяти.
 */
object BellNotification {
    const val CHANNEL_ID = "bell_timer"
    const val NOTIFICATION_ID = 1001

    /** Если приложение вдруг не проснётся на звонке — уведомление само исчезнет через 3 мин. */
    private const val GRACE_MS = 3 * 60_000L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            // DEFAULT, а не LOW: «беззвучные» уведомления многие прошивки прячут с экрана блокировки.
            // Звука и вибрации всё равно нет — они выключены ниже.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun accent(context: Context, content: BellContent): Int =
        context.getColor(if (content.isBreak) R.color.accent_break else R.color.accent_lesson)

    fun build(context: Context, content: BellContent, now: LocalDateTime): Notification =
        if (Build.VERSION.SDK_INT >= 36) {
            LiveUpdate.build(context, content, now)
        } else {
            buildCompat(context, content, now, Prefs.simpleNotification(context))
        }

    /** Пустышка для службы, если звать её пришлось вне уроков (сразу будет убрана). */
    fun placeholder(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.app_name))
            .setSilent(true)
            .build()

    @SuppressLint("MissingPermission") // проверяем через canPost
    fun show(context: Context, notification: Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
        }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    // ---------- Android 8–15 ----------

    private fun buildCompat(context: Context, c: BellContent, now: LocalDateTime, simple: Boolean): Notification {
        val endMs = AppClock.realMillis(c.end)
        val nowMs = System.currentTimeMillis()
        val progress = (c.progress(now) * 1000).toInt()

        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(c.title)
            .setContentText("${c.countdownLabel} · ${c.details}")
            .setColor(accent(context, c))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp(context))
            .setTimeoutAfter((endMs - nowMs).coerceAtLeast(0) + GRACE_MS)
            .setProgress(1000, progress, false)

        if (!simple) {
            b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views(context, c, R.layout.notification_small, progress))
                .setCustomBigContentView(views(context, c, R.layout.notification_big, progress))
        }
        return b.build()
    }

    /** Своя вёрстка: крупный таймер, подпись, прогресс. */
    private fun views(context: Context, c: BellContent, layout: Int, progress: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, layout)
        val base = SystemClock.elapsedRealtime() + (AppClock.realMillis(c.end) - System.currentTimeMillis())
        rv.setChronometer(R.id.chrono, base, null, true)
        rv.setChronometerCountDown(R.id.chrono, true)
        rv.setTextColor(R.id.chrono, accent(context, c))
        rv.setTextViewText(R.id.label, c.countdownLabel)
        rv.setTextViewText(R.id.title, c.title)
        if (layout == R.layout.notification_big) {
            rv.setTextViewText(R.id.next, c.nextLine)
            val shown = if (c.isBreak) R.id.progress_break else R.id.progress_lesson
            val hidden = if (c.isBreak) R.id.progress_lesson else R.id.progress_break
            rv.setViewVisibility(shown, View.VISIBLE)
            rv.setViewVisibility(hidden, View.GONE)
            rv.setProgressBar(shown, 1000, progress, false)
        }
        return rv
    }
}
