package com.intu.taxi.updates

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.intu.taxi.BuildConfig
import com.intu.taxi.driver.DriverSession
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Android runs this even after the activity/process closes, subject to battery restrictions. */
class AppUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val notices = AppUpdateNotifications(applicationContext)
        notices.clearInstalled(BuildConfig.VERSION_CODE)
        if (DriverSession.appInForeground || !notices.enabled()) return Result.success()
        return try {
            val release = AppUpdateRepository(applicationContext.packageName, Build.VERSION.SDK_INT).latest()
            if (!DriverSession.appInForeground) notices.show(release, BuildConfig.VERSION_CODE)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        internal const val WORK_NAME = "intu-app-update-check"

        fun schedule(context: Context): androidx.work.Operation {
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            return WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
