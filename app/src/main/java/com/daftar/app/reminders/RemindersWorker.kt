package com.daftar.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.daftar.app.MainActivity
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.ReminderDao
import com.daftar.app.kernel.i18n.Str
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

// NFR-5 offline reminders (D2): a once-a-day WorkManager digest, not per-reminder exact
// alarms. WorkManager persists across reboots and app kills with no exact-alarm permission
// (Play-safe). The notification is a projection of the same reminder book the tab shows,
// re-queried fresh each morning — so settled customers simply never appear (FR-3.3).
class RemindersWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    // Reach the Room DAOs without a HiltWorkerFactory — the default WorkManager factory
    // instantiates this plain worker, and we pull dependencies from the Hilt graph here.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Daos {
        fun customerDao(): CustomerDao
        fun ledgerDao(): LedgerDao
        fun reminderDao(): ReminderDao
    }

    override suspend fun doWork(): Result {
        val daos = EntryPointAccessors.fromApplication(applicationContext, Daos::class.java)
        val book = ReminderBook.build(
            customers = daos.customerDao().getAll(),
            entries = daos.ledgerDao().getAll(),
            reminders = daos.reminderDao().getAll(),
            today = LocalDate.now(),
        )
        val due = ReminderBook.due(book)
        if (due.isNotEmpty()) postDigest(applicationContext, due)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
        private const val NOTIF_ID = 1001
        private const val DAILY_WORK = "daily-reminder-digest"
        private const val NOTIFY_HOUR = 9 // ~9am local, when the shop opens

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, Str.notifChannelName, NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = Str.notifChannelDesc }
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }

        // First run at the next ~9am, then every 24h. Enqueued KEEP so it survives restarts.
        fun scheduleDaily(context: Context) {
            val now = ZonedDateTime.now()
            var firstRun = now.withHour(NOTIFY_HOUR).withMinute(0).withSecond(0).withNano(0)
            if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
            val request = PeriodicWorkRequestBuilder<RemindersWorker>(Duration.ofDays(1))
                .setInitialDelay(Duration.between(now, firstRun))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }

        // Dev/demo affordance: run the digest immediately to preview the notification.
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RemindersWorker>().build())
        }

        private fun postDigest(context: Context, due: List<ReminderBook.Entry>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return // no notification permission — the in-app المواعيد tab still shows everything
            }

            ensureChannel(context)
            val names = due.joinToString(Str.listSeparator) { it.customer.name }
            val pending = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(Str.notifTitle)
                .setContentText(names)
                .setStyle(NotificationCompat.BigTextStyle().bigText(names))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        }
    }
}
