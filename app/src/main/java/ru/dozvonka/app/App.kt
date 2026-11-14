package ru.dozvonka.app

import android.app.Application
import ru.dozvonka.app.notify.Announcements
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.prefs.AppClock
import ru.dozvonka.app.sched.BellWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppClock.init(this)
        BellNotification.createChannel(this)
        Announcements.createChannel(this)
        // Раз в 15 минут: скачать свежее расписание/объявления и сверить таймер,
        // даже если система «потеряла» будильник.
        BellWorker.enqueue(this)
    }
}
