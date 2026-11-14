package ru.dozvonka.app.prefs

import android.content.Context
import ru.dozvonka.app.core.toEpochMilli
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Часы приложения. Обычно = настоящее время.
 * В админке можно включить «тестовое время» — сдвиг только на этом телефоне,
 * чтобы проверить уведомления и виджеты в любой момент.
 */
object AppClock {
    private const val KEY = "clock_offset_ms"

    @Volatile
    private var offsetMs: Long = 0

    fun init(context: Context) {
        offsetMs = prefs(context).getLong(KEY, 0)
    }

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val isFake: Boolean get() = offsetMs != 0L

    fun nowMillis(): Long = System.currentTimeMillis() + offsetMs

    fun now(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis()), ZoneId.systemDefault())

    /** Момент «по часам приложения» → настоящее время (для будильников и системных таймеров). */
    fun realMillis(appTime: LocalDateTime): Long = appTime.toEpochMilli() - offsetMs

    /** null — вернуть настоящее время. */
    fun setFakeNow(context: Context, fake: LocalDateTime?) {
        offsetMs = if (fake == null) 0 else fake.toEpochMilli() - System.currentTimeMillis()
        prefs(context).edit().putLong(KEY, offsetMs).apply()
    }
}
