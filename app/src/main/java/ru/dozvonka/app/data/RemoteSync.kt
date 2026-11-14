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

    private fun urls(): List<String> {
        val bust = System.currentTimeMillis() / 60_000 // не чаще раза в минуту — в обход кэша
        return listOf(
            "https://raw.githubusercontent.com/${Repo.OWNER}/${Repo.NAME}/${Repo.BRANCH}/${Repo.CONFIG_PATH}?t=$bust",
            "https://api.github.com/repos/${Repo.OWNER}/${Repo.NAME}/contents/${Repo.CONFIG_PATH}?ref=${Repo.BRANCH}",
        )
    }

    /** true — расписание или объявления изменились. */
    fun sync(context: Context): Boolean {
        val app = context.applicationContext
        val text = urls().firstNotNullOfOrNull { fetch(it) } ?: return false
        val fresh = runCatching { ConfigJson.parse(text) }.getOrNull() ?: return false

        val old = ScheduleRepository.config(app)
        val changed = fresh != old
        if (changed) {
            ScheduleRepository.save(app, text, fresh)
            BellUpdater.refresh(app)
        }
        Announcements.notifyNew(app, fresh.announcements)
        return changed
    }

    private fun fetch(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.useCaches = false
            c.setRequestProperty("Accept", "application/vnd.github.raw")
            c.setRequestProperty("User-Agent", "DoZvonka-Android")
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
