package ru.dozvonka.app.sched

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Сработал будильник (звонок или плановое обновление). */
class BellAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BellUpdater.refresh(context)
    }
}

/** Перезагрузка, обновление приложения, смена времени или часового пояса. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BellUpdater.refresh(context)
    }
}

/** Страховка раз в 15 минут через WorkManager. */
class BellWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        BellUpdater.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<BellWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "bell-resync",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
