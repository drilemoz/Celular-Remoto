package br.dev.celularremoto.ui

import br.dev.celularremoto.core.ConnectionHealthMath
import br.dev.celularremoto.core.ConnectionHealthMonitor
import br.dev.celularremoto.core.ConnectionHealthStore
import br.dev.celularremoto.core.SettingsStore
import kotlin.concurrent.thread

/** Verificações de melhor esforço somente enquanto a Activity está visível. */
internal fun MainActivity.makeConnectionHealthPoller(): Runnable {
    var busy = false
    return object : Runnable {
        override fun run() {
            if (!active || isFinishing || isDestroyed) return
            val host = SettingsStore.getRemoteHost(applicationContext)
            val lastStatus = ConnectionHealthStore.status(applicationContext, host)
            val canProbeNow = System.currentTimeMillis() - lastStatus.lastAttemptMs >=
                ConnectionHealthMath.probeDelayMs(lastStatus.failureStreak)
            if (canProbeNow && ConnectionHealthStore.enabled(applicationContext) && host.isNotBlank() &&
                (mode != 1 || remoteConnection.effectiveState(host) != "connected") && !busy) {
                busy = true
                thread(name = "CelularRemoto-health-ui") {
                    val outcome = runCatching { ConnectionHealthMonitor.probe(applicationContext) }.getOrNull()
                    handler.post {
                        busy = false
                        if (active && outcome?.changedHost == true) {
                            remoteConnection.select("Chave salva")
                            render()
                        } else if (active && mode == 1) updateConnectionLabels()
                    }
                }
            }
            if (active) handler.postDelayed(this, 30_000L)
        }
    }
}

/** O modo Controlar reaproveita o snapshot existente, com espera progressiva ao falhar. */
internal fun MainActivity.makeControllerPoller(): Runnable = object : Runnable {
    override fun run() {
        if (active && mode == 1) {
            val host = SettingsStore.getRemoteHost(applicationContext)
            val history = ConnectionHealthStore.status(applicationContext, host)
            val delay = ConnectionHealthMath.probeDelayMs(history.failureStreak)
            if (remoteConnection.state != "offline" || System.currentTimeMillis() -
                remoteConnection.lastAttemptMs >= delay) refreshRemote(silent = true)
            handler.postDelayed(this, 15_000L)
        }
    }
}
