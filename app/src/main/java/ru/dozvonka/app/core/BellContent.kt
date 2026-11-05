package ru.dozvonka.app.core

import java.time.Duration
import java.time.LocalDateTime

/**
 * Готовые тексты для экрана, уведомления и виджетов —
 * чтобы везде было написано одинаково.
 */
data class BellContent(
    /** true — перемена или «скоро уроки» (оранжевый), false — урок (синий). */
    val isBreak: Boolean,
    val chip: String,           // «3 урок из 7» / «Перемена»
    val title: String,          // «3 урок · Алгебра»
    val details: String,        // «каб. 214 · 10:30–11:15»
    val countdownLabel: String, // «до перемены»
    val nextTitle: String,      // «Перемена 20 минут»
    val nextSub: String,        // «потом 4 урок · Физика, каб. 305»
    val nextLine: String,       // одной строкой для уведомления
    val nowShort: String,       // для широкого виджета
    val nextShort: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    fun remaining(now: LocalDateTime): Duration =
        Duration.between(now, end).coerceAtLeast(Duration.ZERO)

    fun progress(now: LocalDateTime): Float {
        val total = Duration.between(start, end).toMillis().coerceAtLeast(1)
        val done = Duration.between(start, now).toMillis()
        return (done.toFloat() / total).coerceIn(0f, 1f)
    }

    companion object {

        fun from(state: BellState): BellContent? = when (state) {
            is BellState.InLesson -> {
                val l = state.lesson
                val next = state.next
                val breakMin = next?.let { Duration.between(l.end, it.start).toMinutes() }
                BellContent(
                    isBreak = false,
                    chip = "${l.number} урок из ${state.total}",
                    title = "${l.number} урок · ${l.subject}",
                    details = "${l.roomLabel} · ${l.start.hm()}–${l.end.hm()}",
                    countdownLabel = if (next != null) "до перемены" else "до конца уроков",
                    nextTitle = if (next != null) "Перемена $breakMin мин" else "Домой",
                    nextSub = if (next != null) "потом ${next.number} урок · ${next.subject}, ${next.roomLabel}"
                    else "это последний урок",
                    nextLine = if (next != null) "Дальше: перемена $breakMin мин, потом ${next.subject}"
                    else "Это последний урок",
                    nowShort = "${l.subject} · ${l.room}",
                    nextShort = next?.let { "${it.subject} · ${it.room}" } ?: "Домой",
                    start = state.startsAt,
                    end = state.endsAt,
                )
            }

            is BellState.OnBreak -> {
                val n = state.next
                val min = Duration.between(state.startsAt, state.endsAt).toMinutes()
                BellContent(
                    isBreak = true,
                    chip = "Перемена",
                    title = if (min >= 20) "Большая перемена" else "Перемена",
                    details = "${state.previous.end.hm()}–${n.start.hm()} · $min мин",
                    countdownLabel = "до урока",
                    nextTitle = "${n.number} урок · ${n.subject}",
                    nextSub = "${n.roomLabel} · ${n.start.hm()}–${n.end.hm()}",
                    nextLine = "Дальше: ${n.number} урок · ${n.subject}, ${n.roomLabel}",
                    nowShort = "Перемена",
                    nextShort = "${n.subject} · ${n.room}",
                    start = state.startsAt,
                    end = state.endsAt,
                )
            }

            is BellState.BeforeSchool -> {
                val f = state.first
                BellContent(
                    isBreak = true,
                    chip = "Скоро уроки",
                    title = "До начала уроков",
                    details = "1 урок в ${f.start.hm()}",
                    countdownLabel = "до первого урока",
                    nextTitle = "${f.number} урок · ${f.subject}",
                    nextSub = "${f.roomLabel} · ${f.start.hm()}–${f.end.hm()}",
                    nextLine = "Первый урок: ${f.subject}, ${f.roomLabel}",
                    nowShort = "Перед уроками",
                    nextShort = "${f.subject} · ${f.room}",
                    start = state.startsAt.minusMinutes(ScheduleEngine.SHOW_BEFORE_MINUTES),
                    end = state.startsAt,
                )
            }

            is BellState.AfterSchool, is BellState.DayOff -> null
        }

        /** Заголовок и подпись, когда уроков сейчас нет. */
        fun idleText(state: BellState, now: LocalDateTime): Pair<String, String> {
            val next = when (state) {
                is BellState.AfterSchool -> state.nextStart
                is BellState.DayOff -> state.nextStart
                else -> null
            }
            val title = if (state is BellState.DayOff) "Сегодня выходной" else "Уроки закончились"
            val sub = if (next == null) {
                "Расписание пустое"
            } else {
                val today = now.toLocalDate()
                val day = when (next.toLocalDate()) {
                    today -> "сегодня"
                    today.plusDays(1) -> "завтра"
                    else -> next.dayOfWeek.onDayRu()
                }
                "Уроки $day в ${next.toLocalTime().hm()}"
            }
            return title to sub
        }
    }
}
