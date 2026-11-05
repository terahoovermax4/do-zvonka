package ru.dozvonka.app.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val RU: Locale = Locale.forLanguageTag("ru-RU")

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_TITLE = DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)

fun LocalTime.hm(): String = format(HM)

fun LocalDateTime.toEpochMilli(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** «Четверг, 5 ноября» */
fun LocalDate.dayTitle(): String = format(DAY_TITLE).replaceFirstChar { it.titlecase(RU) }

/** 27:42 или 1:05:09, если больше часа. Секунды округляем вверх. */
fun formatCountdown(d: Duration): String {
    val total = ((d.toMillis().coerceAtLeast(0) + 999) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

fun DayOfWeek.shortRu(): String = when (this) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

/** «в понедельник», «во вторник»… */
fun DayOfWeek.onDayRu(): String = when (this) {
    DayOfWeek.MONDAY -> "в понедельник"
    DayOfWeek.TUESDAY -> "во вторник"
    DayOfWeek.WEDNESDAY -> "в среду"
    DayOfWeek.THURSDAY -> "в четверг"
    DayOfWeek.FRIDAY -> "в пятницу"
    DayOfWeek.SATURDAY -> "в субботу"
    DayOfWeek.SUNDAY -> "в воскресенье"
}
