package com.daftar.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.daftar.app.store.StoreRepository
import com.daftar.app.store.snapshotToJson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration

// The sync bridge (FR-8.3, one-way by design): the PHONE is the single source of truth and
// stays 100% offline-capable (NFR-4) — this worker merely pushes the full backup JSON to
// the owner-tools API whenever connectivity happens to exist. If it never runs, nothing in
// the shop breaks; when it runs, the dashboard catches up. No conflicts are possible:
// the server import is wipe-and-replace of this single writer's snapshot.
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun storeRepository(): StoreRepository
    }

    override suspend fun doWork(): Result {
        val url = syncUrl(applicationContext).ifBlank { return Result.success() }
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val snapshot = deps.storeRepository().load() ?: return Result.success()
        val json = snapshotToJson(snapshot)

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                connection.disconnect()
                if (code in 200..299) Result.success() else Result.retry()
            } catch (e: Exception) {
                Result.retry() // offline / server down — WorkManager backs off and tries again
            }
        }
    }

    companion object {
        private const val PREFS = "sync"
        private const val KEY_URL = "url"
        private const val PERIODIC_WORK = "backup-sync"

        fun syncUrl(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, "") ?: ""

        fun setSyncUrl(context: Context, url: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_URL, url.trim()).apply()
            schedule(context)
        }

        // Every 6h while a URL is set, only when the network is actually there.
        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            if (syncUrl(context).isBlank()) {
                manager.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofHours(6))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        // "مزامنة الآن" — queued with the network constraint, so tapping it offline is safe:
        // it simply fires when the connection returns.
        fun syncNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build(),
            )
        }
    }
}
