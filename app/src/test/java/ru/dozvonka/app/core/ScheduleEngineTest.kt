package ru.dozvonka.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.dozvonka.app.data.SchoolSchedule
import java.time.LocalDateTime

class ScheduleEngineTest {
    private val s = SchoolSchedule.schedule

    // Ноябрь 2026: 16 — понедельник, 17 — вторник, 19 — четверг, 20 — пятница, 21 — суббота
    private fun at(day: Int, h: Int, m: Int, sec: Int = 0) = LocalDateTime.of(2026, 11, day, h, m, sec)

    @Test fun mondayZeroLesson() {
        val st = ScheduleEngine.stateAt(s, at(16, 8, 20)) as BellState.InLesson
        assertEquals(0, st.lesson.number)
        assertEquals("Разговоры о важном", st.lesson.subject)
        val c = BellContent.from(st)!!
        assertEquals("Нулевой урок", c.chip)
        assertEquals("10:00", formatCountdown(c.remaining(at(16, 8, 20))))
    }

    @Test fun mondayBeforeSchoolStartsAtZeroLesson() {
        val st = ScheduleEngine.stateAt(s, at(16, 7, 50)) as BellState.BeforeSchool
        assertEquals(at(16, 8, 15), st.startsAt)
        assertEquals("0 урок в 08:15", BellContent.from(st)!!.details)
        assertTrue(ScheduleEngine.isActive(st, at(16, 7, 50)))
        assertFalse(ScheduleEngine.isActive(st, at(16, 7, 0)))
    }

    @Test fun tuesdayBigBreak() {
        val st = ScheduleEngine.stateAt(s, at(17, 10, 5)) as BellState.OnBreak
        assertEquals(3, st.next.number)
        val c = BellContent.from(st)!!
        assertEquals("Большая перемена", c.title)
        assertEquals("10:00–10:20 · 20 мин", c.details)
        assertEquals("3 урок · География", c.nextTitle)
        assertEquals("10:20–11:00", c.nextSub)
    }

    @Test fun lessonTextWithoutRoom() {
        val st = ScheduleEngine.stateAt(s, at(17, 8, 47, 18)) as BellState.InLesson
        val c = BellContent.from(st)!!
        assertEquals("1 урок из 6", c.chip)
        assertEquals("1 урок · Геометрия", c.title)
        assertEquals("08:30–09:10", c.details)
        assertEquals("22:42", formatCountdown(c.remaining(at(17, 8, 47, 18))))
        assertEquals("Перемена 10 мин", c.nextTitle)
        assertEquals("потом 2 урок · Русский язык", c.nextSub)
        assertEquals("Геометрия", c.nowShort)
    }

    @Test fun exactlyOnBell() {
        assertTrue(ScheduleEngine.stateAt(s, at(17, 9, 10)) is BellState.OnBreak)
        assertTrue(ScheduleEngine.stateAt(s, at(17, 9, 20)) is BellState.InLesson)
    }

    @Test fun thursdayLastLesson() {
        val st = ScheduleEngine.stateAt(s, at(19, 14, 10)) as BellState.InLesson
        assertEquals(7, st.lesson.number)
        assertNull(st.next)
        assertEquals("до конца уроков", BellContent.from(st)!!.countdownLabel)
    }

    @Test fun fridayAfterSchoolPointsToMonday() {
        val st = ScheduleEngine.stateAt(s, at(20, 15, 0)) as BellState.AfterSchool
        assertEquals(at(23, 8, 15), st.nextStart)
        assertEquals("Уроки в понедельник в 08:15", BellContent.idleText(st, at(20, 15, 0)).second)
    }

    @Test fun saturdayIsDayOff() {
        val st = ScheduleEngine.stateAt(s, at(21, 12, 0)) as BellState.DayOff
        assertEquals(at(23, 8, 15), st.nextStart)
        assertEquals("Сегодня выходной", BellContent.idleText(st, at(21, 12, 0)).first)
    }
}
