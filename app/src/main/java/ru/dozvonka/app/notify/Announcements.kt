package ru.dozvonka.app.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.dozvonka.app.R
import ru.dozvonka.app.data.Announcement
import ru.dozvonka.app.prefs.AppClock
import java.time.temporal.ChronoUnit

/** Уведомления о новых объявлениях от админа. */
object Announcements {
    const val CHANNEL_ID = "announcements"
    private const val SEEN_KEY = "seen_announcements"

    fun createChannel(context: Context) {
        val ch = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_ann_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_ann_desc) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
    }

    /** Активные объявления (срок ещё не вышел), новые сверху. */
    fun active(all: List<Announcement>): List<Announcement> {
        val today = AppClock.now().toLocalDate()
        return all.filter { !it.until.isBefore(today) }.sortedByDescending { it.created }
    }

    /** Показать уведомление о каждом новом объявлении один раз. */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun notifyNew(context: Context, all: List<Announcement>) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val seen = prefs.getStringSet(SEEN_KEY, emptySet())!!.toMutableSet()
        val now = AppClock.now()
        val nm = NotificationManagerCompat.from(context)
        var changed = false

        for (a in active(all)) {
            if (a.id in seen) continue
            seen += a.id
            changed = true
            // Старые объявления новому пользователю не присылаем — он увидит их на главном экране.
            if (ChronoUnit.HOURS.between(a.created, now) > 48) continue
            if (!nm.areNotificationsEnabled()) continue
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle(a.title.ifBlank { "Объявление" })
                .setContentText(a.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(a.text))
                .setContentIntent(BellNotification.openApp(context))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            try {
                nm.notify("ann", a.id.hashCode(), n)
            } catch (e: SecurityException) {
            }
        }
        if (changed) prefs.edit().putStringSet(SEEN_KEY, seen).apply()
    }
}
