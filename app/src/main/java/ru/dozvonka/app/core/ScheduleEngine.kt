package ru.dozvonka.app.core

import ru.dozvonka.app.data.Lesson
import ru.dozvonka.app.data.Schedule
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Что происходит прямо сейчас. */
sealed interface BellState {
    /** Уроки сегодня есть, но ещё не начались. */
    data class BeforeSchool(val first: Lesson, val startsAt: LocalDateTime) : BellState

    /** Идёт урок. [next] = null, если это последний урок. */
    data class InLesson(
        val lesson: Lesson,
        /** Номер последнего урока дня (для «3 урок из 6»). */
        val lastNumber: Int,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        val next: Lesson?,
    ) : BellState

    /** Перемена между [previous] и [next]. */
    data class OnBreak(
        val previous: Lesson,
        val next: Lesson,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
    ) : BellState

    /** Уроки на сегодня закончились. */
    data class AfterSchool(val nextStart: LocalDateTime?) : BellState

    /** Сегодня уроков нет. */
    data class DayOff(val nextStart: LocalDateTime?) : BellState
}

/** Вся «математика» расписания. Без Android — легко проверять тестами. */
object ScheduleEngine {

    /** За сколько минут до первого урока показывать таймер в уведомлении. */
    const val SHOW_BEFORE_MINUTES = 60L

    fun stateAt(schedule: Schedule, now: LocalDateTime): BellState {
        val date = now.toLocalDate()
        val lessons = schedule.lessonsFor(date.dayOfWeek)
        if (lessons.isEmpty()) return BellState.DayOff(nextSchoolStart(schedule, date.plusDays(1)))

        val t = now.toLocalTime()
        val first = lessons.first()
        if (t < first.start) return BellState.BeforeSchool(first, date.atTime(first.start))

        for (i in lessons.indices) {
            val lesson = lessons[i]
            val next = lessons.getOrNull(i + 1)
            if (t < lesson.end) {
                return BellState.InLesson(
                    lesson = lesson,
                    lastNumber = lessons.last().number,
                    startsAt = date.atTime(lesson.start),
                    endsAt = date.atTime(lesson.end),
                    next = next,
                )
            }
            if (next != null && t < next.start) {
                return BellState.OnBreak(lesson, next, date.atTime(lesson.end), date.atTime(next.start))
            }
        }
        return BellState.AfterSchool(nextSchoolStart(schedule, date.plusDays(1)))
    }

    /** Начало первого урока в ближайший учебный день, начиная с [from]. */
    fun nextSchoolStart(schedule: Schedule, from: LocalDate): LocalDateTime? {
        for (d in 0L until 14L) {
            val day = from.plusDays(d)
            val first = schedule.lessonsFor(day.dayOfWeek).firstOrNull() ?: continue
            return day.atTime(first.start)
        }
        return null
    }

    /** Нужно ли сейчас показывать уведомление с таймером. */
    fun isActive(state: BellState, now: LocalDateTime): Boolean = when (state) {
        is BellState.InLesson, is BellState.OnBreak -> true
        is BellState.BeforeSchool -> Duration.between(now, state.startsAt).toMinutes() < SHOW_BEFORE_MINUTES
        is BellState.AfterSchool, is BellState.DayOff -> false
    }

    /** Когда в следующий раз что-то поменяется и приложению надо проснуться. */
    fun nextWakeup(state: BellState, now: LocalDateTime): LocalDateTime {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
        return when (state) {
            is BellState.InLesson -> state.endsAt
            is BellState.OnBreak -> state.endsAt
            is BellState.BeforeSchool -> {
                val showAt = state.startsAt.minusMinutes(SHOW_BEFORE_MINUTES)
                if (now < showAt) showAt else state.startsAt
            }
            is BellState.AfterSchool -> earliest(state.nextStart?.minusMinutes(SHOW_BEFORE_MINUTES), nextMidnight)
            is BellState.DayOff -> earliest(state.nextStart?.minusMinutes(SHOW_BEFORE_MINUTES), nextMidnight)
        }
    }

    private fun earliest(a: LocalDateTime?, b: LocalDateTime): LocalDateTime =
        if (a != null && a < b) a else b
}
