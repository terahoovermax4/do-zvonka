package ru.dozvonka.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.dozvonka.app.data.TestSchedule
import java.time.LocalDateTime

class ScheduleEngineTest {
    private val s = TestSchedule.schedule
    // 5 ноября 2026 — четверг (7 уроков), 7 ноября — суббота
    private fun at(day: Int, h: Int, m: Int, sec: Int = 0) = LocalDateTime.of(2026, 11, day, h, m, sec)

    @Test fun lessonInProgress() {
        val st = ScheduleEngine.stateAt(s, at(5, 10, 47, 18)) as BellState.InLesson
        assertEquals(3, st.lesson.number)
        assertEquals(at(5, 11, 15), st.endsAt)
        val c = BellContent.from(st)!!
        assertEquals("27:42", formatCountdown(c.remaining(at(5, 10, 47, 18))))
        assertEquals("до перемены", c.countdownLabel)
    }

    @Test fun breakBetweenLessons() {
        val st = ScheduleEngine.stateAt(s, at(5, 11, 22)) as BellState.OnBreak
        assertEquals(4, st.next.number)
        assertEquals("Большая перемена", BellContent.from(st)!!.title)
    }

    @Test fun exactlyOnBellIsNextState() {
        assertTrue(ScheduleEngine.stateAt(s, at(5, 11, 15)) is BellState.OnBreak)
        assertTrue(ScheduleEngine.stateAt(s, at(5, 11, 35)) is BellState.InLesson)
    }

    @Test fun beforeAndAfterSchool() {
        val before = ScheduleEngine.stateAt(s, at(5, 7, 0))
        assertTrue(before is BellState.BeforeSchool)
        assertFalse(ScheduleEngine.isActive(before, at(5, 7, 0)))
        assertTrue(ScheduleEngine.isActive(ScheduleEngine.stateAt(s, at(5, 7, 45)), at(5, 7, 45)))
        assertEquals(at(5, 7, 30), ScheduleEngine.nextWakeup(before, at(5, 7, 0)))

        val after = ScheduleEngine.stateAt(s, at(5, 16, 0)) as BellState.AfterSchool
        assertEquals(at(6, 8, 30), after.nextStart)
    }

    @Test fun weekendPointsToMonday() {
        val st = ScheduleEngine.stateAt(s, at(7, 12, 0)) as BellState.DayOff
        assertEquals(at(9, 8, 30), st.nextStart)
        assertEquals("Уроки в понедельник в 08:30", BellContent.idleText(st, at(7, 12, 0)).second)
    }
}
