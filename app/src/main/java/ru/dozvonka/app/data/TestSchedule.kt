package ru.dozvonka.app.data

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * ТЕСТОВОЕ расписание. Когда будет настоящее — поменять звонки (bells)
 * и предметы по дням ниже. Суббота и воскресенье — выходные.
 */
object TestSchedule {

    /** Расписание звонков: начало и конец каждого урока. */
    private val bells = listOf(
        "08:30" to "09:15",
        "09:25" to "10:10",
        "10:30" to "11:15", // перед 3 уроком большая перемена 20 мин
        "11:35" to "12:20", // и перед 4 тоже
        "12:30" to "13:15",
        "13:25" to "14:10",
        "14:20" to "15:05",
    )

    /** Уроки дня по порядку: предмет и кабинет. */
    private fun day(vararg lessons: Pair<String, String>): List<Lesson> =
        lessons.mapIndexed { i, (subject, room) ->
            val (start, end) = bells[i]
            Lesson(i + 1, subject, room, LocalTime.parse(start), LocalTime.parse(end))
        }

    val schedule = Schedule(
        mapOf(
            DayOfWeek.MONDAY to day(
                "Разговоры о важном" to "203",
                "Алгебра" to "214",
                "Русский язык" to "203",
                "Физика" to "305",
                "История" to "118",
                "Физкультура" to "спортзал",
            ),
            DayOfWeek.TUESDAY to day(
                "Геометрия" to "214",
                "Литература" to "203",
                "Английский" to "221",
                "Биология" to "310",
                "Химия" to "312",
                "Обществознание" to "118",
            ),
            DayOfWeek.WEDNESDAY to day(
                "Русский язык" to "203",
                "Алгебра" to "214",
                "Информатика" to "401",
                "География" to "116",
                "Английский" to "221",
                "Физкультура" to "спортзал",
                "Музыка" to "105",
            ),
            DayOfWeek.THURSDAY to day(
                "Русский язык" to "203",
                "Литература" to "203",
                "Алгебра" to "214",
                "Физика" to "305",
                "История" to "118",
                "Английский" to "221",
                "Физкультура" to "спортзал",
            ),
            DayOfWeek.FRIDAY to day(
                "Геометрия" to "214",
                "Химия" to "312",
                "Русский язык" to "203",
                "Биология" to "310",
                "Труд" to "102",
            ),
        )
    )
}
