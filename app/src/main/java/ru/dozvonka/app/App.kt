package ru.dozvonka.app

import android.app.Application
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.sched.BellWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        BellNotification.createChannel(this)
        // Страховка: раз в 15 минут сверяемся с расписанием,
        // даже если система «потеряла» будильник.
        BellWorker.enqueue(this)
    }
}
