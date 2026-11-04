package br.dev.celularremoto.updates

import android.app.job.JobParameters
import android.app.job.JobService
import kotlin.concurrent.thread

/** Nenhuma Activity é lançada por serviço de fundo. */
class UpdateCheckJobService : JobService() {
    @Volatile private var stopped = false
    override fun onStartJob(params: JobParameters): Boolean {
        if (!UpdatePreferences.checking(this)) return false
        stopped = false
        thread(name = "CelularRemoto-update-check") {
            try {
                if (!stopped) UpdateController.check(applicationContext, automatic = true)
            } finally {
                if (!stopped) jobFinished(params, false)
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { stopped = true; return false }
}
