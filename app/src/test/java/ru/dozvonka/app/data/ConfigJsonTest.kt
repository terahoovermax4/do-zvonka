package ru.dozvonka.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.dozvonka.app.core.BellState
import ru.dozvonka.app.core.ScheduleEngine
import ru.dozvonka.app.core.parseDateRu
import ru.dozvonka.app.core.parseDateTimeRu
import ru.dozvonka.app.core.shortenedDay
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ConfigJsonTest {

    /** Файл в репозитории совпадает со встроенным расписанием. */
    @Test fun repoConfigMatchesBuiltIn() {
        val f = listOf(File("../config/schedule.json"), File("config/schedule.json")).first { it.exists() }
        val cfg = ConfigJson.parse(f.readText())
        assertEquals(SchoolSchedule.schedule.days, cfg.schedule.days)
    }

    @Test fun roundTrip() {
        val tue = LocalDate.of(2026, 11, 17)
        val cfg = RemoteConfig(
            schedule = SchoolSchedule.schedule.copy(
                overrides = mapOf(
                    tue to DayOverride("Сокращённый день", shortenedDay(SchoolSchedule.schedule.lessonsFor(DayOfWeek.TUESDAY), LocalTime.of(8, 30), 30, 10)),
                    tue.plusDays(1) to DayOverride("Уроков нет", emptyList()),
                    tue.plusDays(2) to DayOverride("Фотографирование класса", null),
                ),
            ),
            announcements = listOf(
                Announcement("a1", "Физики не будет", "Учитель заболел", LocalDateTime.of(2026, 11, 14, 22, 0), tue),
            ),
            updatedAt = "2026-11-14T22:40:00",
        )
        assertEquals(cfg, ConfigJson.parse(ConfigJson.encode(cfg)))
    }

    @Test fun shortenedTuesday() {
        val short = shortenedDay(SchoolSchedule.schedule.lessonsFor(DayOfWeek.TUESDAY), LocalTime.of(8, 30), 30, 10)
        assertEquals(6, short.size)
        assertEquals(LocalTime.of(8, 30), short[0].start)
        assertEquals(LocalTime.of(9, 0), short[0].end)
        assertEquals(LocalTime.of(9, 10), short[1].start)
        assertEquals(LocalTime.of(12, 10), short[5].end)
        assertEquals("Литература", short[5].subject)
    }

    @Test fun overridesChangeTheDay() {
        val tue = LocalDate.of(2026, 11, 17)
        val s = SchoolSchedule.schedule.copy(
            overrides = mapOf(
                tue to DayOverride("Сокращённый день", shortenedDay(SchoolSchedule.schedule.lessonsFor(DayOfWeek.TUESDAY), LocalTime.of(8, 30), 30, 10)),
                tue.plusDays(1) to DayOverride("Уроков нет", emptyList()),
                tue.plusDays(2) to DayOverride("Заметка", null),
            ),
        )
        // Вторник 09:05 — в сокращённый день это уже перемена
        assertTrue(ScheduleEngine.stateAt(s, tue.atTime(9, 5)) is BellState.OnBreak)
        // Среда — уроков нет, следующий учебный день четверг
        val wed = ScheduleEngine.stateAt(s, tue.plusDays(1).atTime(10, 0)) as BellState.DayOff
        assertEquals(tue.plusDays(2).atTime(8, 30), wed.nextStart)
        // Четверг — только заметка, уроки обычные
        assertEquals(7, s.lessonsOn(tue.plusDays(2)).size)
        assertEquals("Заметка", s.noteOn(tue.plusDays(2)))
    }

    @Test fun badEntriesAreSkipped() {
        val cfg = ConfigJson.parse(
            """{"week":{"MONDAY":[{"n":1,"start":"9:99","end":"10:00","subject":"x"},
               {"n":2,"start":"10:00","end":"10:40","subject":"Физика"}],"FUNDAY":[]},
               "announcements":[{"id":"z","title":"t","created":"oops"}]}""",
        )
        assertEquals(1, cfg.schedule.lessonsFor(DayOfWeek.MONDAY).size)
        assertTrue(cfg.announcements.isEmpty())
    }

    @Test fun ruParsing() {
        assertEquals(LocalDate.of(2026, 11, 17), parseDateRu("17.11.2026"))
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 5), parseDateTimeRu("17.11.2026 8:05"))
        assertEquals(null, parseDateRu("31.02.2026"))
    }
}
