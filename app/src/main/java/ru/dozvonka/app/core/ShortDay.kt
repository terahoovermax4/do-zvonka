package ru.dozvonka.app.core

import ru.dozvonka.app.data.Lesson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Сокращённый день: те же уроки и предметы, но короче уроки и перемены. */
fun shortenedDay(base: List<Lesson>, start: LocalTime, lessonMin: Int, breakMin: Int): List<Lesson> {
    var t = start
    return base.sortedBy { it.start }.map { l ->
        val s = t
        val e = s.plusMinutes(lessonMin.toLong())
        t = e.plusMinutes(breakMin.toLong())
        l.copy(start = s, end = e)
    }
}

private val DATE = DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT)
private val DATE_SHOW = DateTimeFormatter.ofPattern("dd.MM.uuuu")
private val TIME = DateTimeFormatter.ofPattern("H:mm")

/** «17.11.2026» → дата, либо null. */
fun parseDateRu(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim(), DATE) }.getOrNull()

/** «8:30» / «08:30» → время, либо null. */
fun parseTimeRu(text: String): LocalTime? = runCatching { LocalTime.parse(text.trim(), TIME) }.getOrNull()

/** «17.11.2026 08:20» → дата и время, либо null. */
fun parseDateTimeRu(text: String): LocalDateTime? {
    val parts = text.trim().split(Regex("\\s+"))
    if (parts.size != 2) return null
    val d = parseDateRu(parts[0]) ?: return null
    val t = parseTimeRu(parts[1]) ?: return null
    return d.atTime(t)
}

fun LocalDate.ru(): String = format(DATE_SHOW)
