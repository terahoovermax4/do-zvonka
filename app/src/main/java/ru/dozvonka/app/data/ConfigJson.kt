package ru.dozvonka.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Чтение и запись config/schedule.json.
 *
 * {
 *   "updatedAt": "2026-11-14T22:30:00",
 *   "week": { "MONDAY": [ {"n":0,"start":"08:15","end":"08:30","subject":"…","room":""} ], … },
 *   "overrides": { "2026-11-17": {"note":"Сокращённый день","lessons":[…]} },   // без "lessons" — обычные уроки
 *   "announcements": [ {"id":"…","title":"…","text":"…","created":"2026-11-14T22:30:00","until":"2026-11-20"} ]
 * }
 */
object ConfigJson {

    fun parse(text: String): RemoteConfig {
        val root = JSONObject(text)

        val week = mutableMapOf<DayOfWeek, List<Lesson>>()
        root.optJSONObject("week")?.let { w ->
            for (key in w.keys()) {
                val day = runCatching { DayOfWeek.valueOf(key.uppercase()) }.getOrNull() ?: continue
                week[day] = lessons(w.optJSONArray(key))
            }
        }

        val overrides = mutableMapOf<LocalDate, DayOverride>()
        root.optJSONObject("overrides")?.let { o ->
            for (key in o.keys()) {
                val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: continue
                val d = o.optJSONObject(key) ?: continue
                overrides[date] = DayOverride(
                    note = d.optString("note", ""),
                    lessons = if (d.has("lessons") && !d.isNull("lessons")) lessons(d.optJSONArray("lessons")) else null,
                )
            }
        }

        val announcements = mutableListOf<Announcement>()
        root.optJSONArray("announcements")?.let { arr ->
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                val created = runCatching { LocalDateTime.parse(a.optString("created")) }.getOrNull() ?: continue
                announcements += Announcement(
                    id = a.optString("id").ifBlank { "a$i" },
                    title = a.optString("title"),
                    text = a.optString("text"),
                    created = created,
                    until = runCatching { LocalDate.parse(a.optString("until")) }.getOrNull()
                        ?: created.toLocalDate().plusDays(3),
                )
            }
        }

        return RemoteConfig(
            schedule = Schedule(week, overrides),
            announcements = announcements,
            updatedAt = root.optString("updatedAt", ""),
        )
    }

    private fun lessons(arr: JSONArray?): List<Lesson> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Lesson>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val start = runCatching { LocalTime.parse(o.optString("start")) }.getOrNull() ?: continue
            val end = runCatching { LocalTime.parse(o.optString("end")) }.getOrNull() ?: continue
            if (!end.isAfter(start)) continue
            out += Lesson(
                number = o.optInt("n", i + 1),
                subject = o.optString("subject"),
                room = o.optString("room", ""),
                start = start,
                end = end,
            )
        }
        return out.sortedBy { it.start }
    }

    fun encode(config: RemoteConfig): String {
        val root = JSONObject()
        root.put("updatedAt", config.updatedAt)

        val week = JSONObject()
        for (day in DayOfWeek.values()) {
            val list = config.schedule.days[day] ?: continue
            week.put(day.name, lessonsJson(list))
        }
        root.put("week", week)

        val overrides = JSONObject()
        for (entry in config.schedule.overrides.entries.sortedBy { it.key.toEpochDay() }) {
            val o = entry.value
            val d = JSONObject().put("note", o.note)
            val ls = o.lessons
            if (ls != null) d.put("lessons", lessonsJson(ls))
            overrides.put(entry.key.toString(), d)
        }
        root.put("overrides", overrides)

        val ann = JSONArray()
        for (a in config.announcements) {
            ann.put(
                JSONObject()
                    .put("id", a.id)
                    .put("title", a.title)
                    .put("text", a.text)
                    .put("created", a.created.withNano(0).toString())
                    .put("until", a.until.toString()),
            )
        }
        root.put("announcements", ann)
        return root.toString(2)
    }

    private fun lessonsJson(list: List<Lesson>): JSONArray {
        val arr = JSONArray()
        for (l in list.sortedBy { it.start }) {
            arr.put(
                JSONObject()
                    .put("n", l.number)
                    .put("start", l.start.toString())
                    .put("end", l.end.toString())
                    .put("subject", l.subject)
                    .put("room", l.room),
            )
        }
        return arr
    }
}
