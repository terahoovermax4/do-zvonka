package ru.dozvonka.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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

/**
 * Особый день: сокращённый, отменённый или просто с заметкой.
 * [lessons] = null — уроки как обычно (только заметка); пустой список — уроков нет.
 */
data class DayOverride(
    val note: String,
    val lessons: List<Lesson>?,
)

/** Объявление от админа. */
data class Announcement(
    val id: String,
    val title: String,
    val text: String,
    val created: LocalDateTime,
    /** Показывать до этой даты включительно. */
    val until: LocalDate,
)

/**
 * Расписание: обычная неделя + особые дни по датам.
 * Нет записи для дня недели — выходной.
 */
data class Schedule(
    val days: Map<DayOfWeek, List<Lesson>>,
    val overrides: Map<LocalDate, DayOverride> = emptyMap(),
) {
    /** Уроки обычной недели (без учёта особых дней) — для экрана «Расписание». */
    fun lessonsFor(day: DayOfWeek): List<Lesson> = days[day].orEmpty().sortedBy { it.start }

    /** Уроки в конкретную дату с учётом сокращённых и отменённых дней. */
    fun lessonsOn(date: LocalDate): List<Lesson> =
        overrides[date]?.lessons?.sortedBy { it.start } ?: lessonsFor(date.dayOfWeek)

    /** Заметка к дате («Сокращённый день»), если есть. */
    fun noteOn(date: LocalDate): String? = overrides[date]?.note?.takeIf { it.isNotBlank() }
}

/** Всё, что приходит с GitHub: расписание и объявления. */
data class RemoteConfig(
    val schedule: Schedule,
    val announcements: List<Announcement>,
    val updatedAt: String,
)

/** Склеивает непустые части через « · ». */
fun joinParts(vararg parts: String): String = parts.filter { it.isNotBlank() }.joinToString(" · ")
