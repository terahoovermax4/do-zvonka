package ru.dozvonka.app.data

import android.content.Context
import ru.dozvonka.app.notify.Announcements
import ru.dozvonka.app.sched.BellUpdater
import java.net.HttpURLConnection
import java.net.URL

/** Где лежит общее расписание. */
object Repo {
    const val OWNER = "terahoovermax4"
    const val NAME = "do-zvonka"
    const val BRANCH = "main"
    const val CONFIG_PATH = "config/schedule.json"
}

/**
 * Скачивает config/schedule.json с GitHub. Вызывать НЕ из главного потока.
 * Работает при открытии приложения и раз в 15 минут в фоне.
 */
object RemoteSync {
    /** Не чаще раза в 2 минуты (при частом открытии приложения). */
    private const val MIN_INTERVAL_MS = 2 * 60_000L

    @Volatile
    private var lastSyncAt = 0L

    private fun urls(): List<String> {
        val bust = System.currentTimeMillis() / 60_000 // не чаще раза в минуту — в обход кэша CDN
        return listOf(
            "https://raw.githubusercontent.com/${Repo.OWNER}/${Repo.NAME}/${Repo.BRANCH}/${Repo.CONFIG_PATH}?t=$bust",
            "https://api.github.com/repos/${Repo.OWNER}/${Repo.NAME}/contents/${Repo.CONFIG_PATH}?ref=${Repo.BRANCH}",
        )
    }

    private sealed interface Fetch {
        data class Body(val text: String, val etag: String?) : Fetch
        data object NotModified : Fetch
    }

    /** true — расписание или объявления изменились. */
    fun sync(context: Context, force: Boolean = false): Boolean {
        val app = context.applicationContext
        val t = System.currentTimeMillis()
        if (!force && t - lastSyncAt < MIN_INTERVAL_MS) return false
        lastSyncAt = t

        val prefs = app.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val etag = prefs.getString("etag", null)
        // Файл не менялся — GitHub ответит «304» без тела, трафик почти нулевой.
        val result = urls().firstNotNullOfOrNull { fetch(it, etag) } ?: return false
        if (result is Fetch.NotModified) return false
        result as Fetch.Body
        val text = result.text
        val fresh = runCatching { ConfigJson.parse(text) }.getOrNull() ?: return false

        val old = ScheduleRepository.config(app)
        val changed = fresh != old
        if (changed) {
            ScheduleRepository.save(app, text, fresh)
            BellUpdater.refresh(app)
        }
        prefs.edit().putString("etag", result.etag).apply()
        Announcements.notifyNew(app, fresh.announcements)
        return changed
    }

    private fun fetch(url: String, etag: String?): Fetch? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.useCaches = false
            c.setRequestProperty("Accept", "application/vnd.github.raw")
            c.setRequestProperty("User-Agent", "DoZvonka-Android")
            if (etag != null) c.setRequestProperty("If-None-Match", etag)
            when (c.responseCode) {
                200 -> Fetch.Body(c.inputStream.bufferedReader().use { it.readText() }, c.getHeaderField("ETag"))
                304 -> Fetch.NotModified
                else -> null
            }
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
