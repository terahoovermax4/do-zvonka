package ru.dozvonka.app.data

import java.time.DayOfWeek
import java.time.LocalTime

/** Один урок в конкретный день. */
data class Lesson(
    val number: Int,
    val subject: String,
    val room: String,
    val start: LocalTime,
    val end: LocalTime,
) {
    /** «каб. 214», для «спортзал» — «спортзал», если кабинет не указан — пусто. */
    val roomLabel: String
        get() = if (room.firstOrNull()?.isDigit() == true) "каб. $room" else room

    /** «Физика» или «Физика · 305», если кабинет указан. */
    val subjectWithRoom: String
        get() = joinParts(subject, room)
}

/** Расписание на неделю: для каждого дня свой список уроков. Нет записи — выходной. */
data class Schedule(val days: Map<DayOfWeek, List<Lesson>>) {
    fun lessonsFor(day: DayOfWeek): List<Lesson> = days[day].orEmpty().sortedBy { it.start }
}

/** Склеивает непустые части через « · ». */
fun joinParts(vararg parts: String): String = parts.filter { it.isNotBlank() }.joinToString(" · ")
