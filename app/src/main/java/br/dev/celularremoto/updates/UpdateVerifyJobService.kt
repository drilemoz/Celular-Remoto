package br.dev.celularremoto.updates

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlin.concurrent.thread

/** Verificar um APK pode consumir segundos; nunca bloquear BroadcastReceiver. */
class UpdateVerifyJobService : JobService() {
    @Volatile private var stopped = false
    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        thread(name = "CelularRemoto-verify-apk") {
            try {
                val id = UpdatePreferences.getLong(applicationContext, "download_id")
                if (!stopped && id > 0) UpdateController.reconcileDownload(applicationContext, id)
            } finally { if (!stopped) jobFinished(params, false) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { stopped = true; return false }

    companion object {
        private const val JOB_ID = 2758
        private const val FALLBACK_ID = 2759
        fun scheduleFallback(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(FALLBACK_ID) != null) return
            scheduler.schedule(JobInfo.Builder(FALLBACK_ID, ComponentName(context, UpdateVerifyJobService::class.java))
                .setPeriodic(30L * 60_000L)
                .setPersisted(true)
                .build())
        }
        fun stopFallback(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(FALLBACK_ID)
        }
        fun enqueue(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val request = JobInfo.Builder(JOB_ID, ComponentName(context, UpdateVerifyJobService::class.java))
                .setMinimumLatency(0L)
                .setOverrideDeadline(5_000L)
                .build()
            scheduler.schedule(request)
        }
    }
}
