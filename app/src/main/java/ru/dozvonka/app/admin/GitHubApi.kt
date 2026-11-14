package ru.dozvonka.app.admin

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import ru.dozvonka.app.data.ConfigJson
import ru.dozvonka.app.data.RemoteConfig
import ru.dozvonka.app.data.Repo
import ru.dozvonka.app.data.ScheduleRepository
import ru.dozvonka.app.sched.BellUpdater
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

/**
 * Публикация общего расписания в репозиторий на GitHub.
 * Ключ (токен) админа хранится только на его телефоне. Всё — НЕ из главного потока.
 */
object GitHubApi {
    private const val API = "https://api.github.com/repos/${Repo.OWNER}/${Repo.NAME}"

    private class Http(val code: Int, val body: String)

    private fun request(method: String, url: String, token: String?, body: String? = null): Http {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.useCaches = false
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            c.setRequestProperty("User-Agent", "DoZvonka-Android")
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Http(code, text)
        } finally {
            c.disconnect()
        }
    }

    private fun explain(code: Int): String = when (code) {
        401 -> "Ключ неверный или просрочен. Создайте новый."
        403 -> "У ключа нет права записи. Нужно: репозиторий ${Repo.NAME}, Contents — Read and write."
        404 -> "Репозиторий не найден или ключ не даёт к нему доступа."
        409, 422 -> "Кто-то только что изменил расписание. Нажмите ещё раз."
        else -> "GitHub ответил ошибкой $code. Проверьте интернет и попробуйте ещё раз."
    }

    /** Проверить ключ: видит ли он репозиторий и может ли в него писать. */
    fun checkToken(token: String): Result<String> = runCatching {
        val r = request("GET", API, token)
        if (r.code != 200) error(explain(r.code))
        val canPush = JSONObject(r.body).optJSONObject("permissions")?.optBoolean("push") == true
        if (!canPush) error(explain(403))
        "Ключ работает: можно публиковать изменения."
    }

    /**
     * Сохранить новое расписание/объявления для всех.
     * После успеха сразу применяет у себя, у остальных — в течение ~15 минут.
     */
    fun publish(context: Context, token: String, config: RemoteConfig, message: String): Result<Unit> = runCatching {
        val stamped = config.copy(updatedAt = LocalDateTime.now().withNano(0).toString())
        val text = ConfigJson.encode(stamped)
        val url = "$API/contents/${Repo.CONFIG_PATH}"

        var lastCode = 0
        repeat(2) {
            val cur = request("GET", "$url?ref=${Repo.BRANCH}", token)
            val sha = if (cur.code == 200) JSONObject(cur.body).optString("sha").ifBlank { null } else null
            if (cur.code != 200 && cur.code != 404) error(explain(cur.code))

            val body = JSONObject()
                .put("message", message)
                .put("content", Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                .put("branch", Repo.BRANCH)
            if (sha != null) body.put("sha", sha)

            val r = request("PUT", url, token, body.toString())
            if (r.code == 200 || r.code == 201) {
                ScheduleRepository.save(context, text, stamped)
                BellUpdater.refresh(context)
                return@runCatching
            }
            lastCode = r.code
            if (r.code != 409 && r.code != 422) error(explain(r.code))
        }
        error(explain(lastCode))
    }

    /** Сколько раз скачали APK (сумма по всем сборкам). */
    fun downloads(): Result<Int> = runCatching {
        val r = request("GET", "$API/releases?per_page=100", null)
        if (r.code != 200) error(explain(r.code))
        val arr = JSONArray(r.body)
        var total = 0
        for (i in 0 until arr.length()) {
            val assets = arr.getJSONObject(i).optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) total += assets.getJSONObject(j).optInt("download_count")
        }
        total
    }
}
