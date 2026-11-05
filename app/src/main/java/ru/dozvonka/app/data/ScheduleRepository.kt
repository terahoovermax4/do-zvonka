package ru.dozvonka.app.data

import android.content.Context

/**
 * Откуда приложение берёт расписание. Сейчас — тестовое из кода.
 * Потом сюда добавим загрузку настоящего (из файла/настроек).
 */
object ScheduleRepository {
    @Suppress("UNUSED_PARAMETER")
    fun get(context: Context): Schedule = TestSchedule.schedule
}
