package br.dev.celularremoto.updates

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/** JobScheduler evita uma segunda infraestrutura e respeita Doze; periodicidade não é exata. */
object UpdateScheduler {
    private const val JOB_ID = 2757
    fun refresh(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!UpdatePreferences.checking(context)) {
            scheduler.cancel(JOB_ID)
            return
        }
        val interval = UpdatePreferences.intervalHours(context).toLong() * 60 * 60 * 1000
        if (scheduler.getPendingJob(JOB_ID)?.intervalMillis == interval) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, UpdateCheckJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(interval)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }
}
