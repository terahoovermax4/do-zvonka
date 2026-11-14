package ru.dozvonka.app.data

import android.content.Context

/**
 * Откуда приложение берёт расписание. Сейчас — из кода (SchoolSchedule.kt).
 * Потом сюда добавим загрузку настоящего (из файла/настроек).
 */
object ScheduleRepository {
    @Suppress("UNUSED_PARAMETER")
    fun get(context: Context): Schedule = SchoolSchedule.schedule
}
