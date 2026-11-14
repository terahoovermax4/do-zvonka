package ru.dozvonka.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Откуда приложение берёт расписание и объявления.
 * 1) последняя версия, скачанная с GitHub (файл в памяти телефона);
 * 2) если её ещё нет — встроенное расписание из SchoolSchedule.kt.
 */
object ScheduleRepository {
    private const val FILE = "remote_config.json"

    val builtIn = RemoteConfig(SchoolSchedule.schedule, emptyList(), updatedAt = "")

    private val state = MutableStateFlow(builtIn)

    @Volatile
    private var loaded = false

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val f = File(context.applicationContext.filesDir, FILE)
        if (f.exists()) {
            runCatching { ConfigJson.parse(f.readText()) }.onSuccess { state.value = it }
        }
    }

    fun config(context: Context): RemoteConfig {
        ensureLoaded(context)
        return state.value
    }

    fun get(context: Context): Schedule = config(context).schedule

    /** Для экранов: меняется сразу, как пришло новое расписание. */
    fun flow(context: Context): StateFlow<RemoteConfig> {
        ensureLoaded(context)
        return state
    }

    @Synchronized
    fun save(context: Context, text: String, config: RemoteConfig) {
        File(context.applicationContext.filesDir, FILE).writeText(text)
        loaded = true
        state.value = config
    }
}
