package br.dev.celularremoto.service

import android.app.job.JobParameters
import android.app.job.JobService
import br.dev.celularremoto.core.ConnectionHealthMonitor
import br.dev.celularremoto.core.ConnectionHealthStore
import kotlin.concurrent.thread

/** Não mantém conexão ou processo contínuo. Não reinicia servidor que foi desligado. */
class ConnectionHealthJobService : JobService() {
    @Volatile private var stopped = false
    override fun onStartJob(params: JobParameters): Boolean {
        if (!ConnectionHealthStore.enabled(this) || !ConnectionHealthStore.backgroundEnabled(this)) return false
        stopped = false
        thread(name = "CelularRemoto-connection-health") {
            try {
                if (!stopped) ConnectionHealthMonitor.probe(applicationContext)
            } finally {
                if (!stopped) jobFinished(params, false)
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        return false
    }
}
