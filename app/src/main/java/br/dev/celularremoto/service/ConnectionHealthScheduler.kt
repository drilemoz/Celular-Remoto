package br.dev.celularremoto.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import br.dev.celularremoto.core.ConnectionHealthStore
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore

/** 15 minutos é o menor período garantido pela API do agendador, não tempo real. */
object ConnectionHealthScheduler {
    const val JOB_ID = 2783
    fun refresh(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val host = SettingsStore.getRemoteHost(context)
        val allowed = ConnectionHealthStore.enabled(context) &&
            ConnectionHealthStore.backgroundEnabled(context) &&
            host.isNotBlank() && RecentDevices.key(context, host) != null
        if (!allowed) { scheduler.cancel(JOB_ID); return }
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, ConnectionHealthJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(15L * 60_000L)
            .setPersisted(false)
            .build()
        scheduler.schedule(job)
    }
}
