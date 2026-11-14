package ru.dozvonka.app.sched

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.dozvonka.app.data.RemoteSync
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

/** Раз в 15 минут: сверка таймера, если система «потеряла» будильник. */
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
            // Скачивание расписания — отдельно и только когда есть интернет.
            val sync = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "schedule-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                sync,
            )
        }
    }
}

/** Раз в 30 минут при наличии интернета: свежее расписание и объявления с GitHub. */
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        RemoteSync.sync(applicationContext, force = true)
        return Result.success()
    }
}
