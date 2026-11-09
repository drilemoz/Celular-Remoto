package br.dev.celularremoto.service

import android.content.Context
import android.os.Handler
import br.dev.celularremoto.core.ControllerRemoteTelemetry
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.RemoteBatteryPollingPolicy
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.RemoteClient
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Leitura econômica na LAN com serviço foreground explícito; sem despertar CPU/rede à força.
 * Uma única consulta em andamento, confirmação da chave após a resposta e backoff.
 */
internal class ControllerBatteryBackgroundPoller(
    context: Context,
    private val handler: Handler,
    private val onUpdated: () -> Unit
) {
    private val app = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { work ->
        Thread(work, "CelularRemoto-remote-battery").apply { isDaemon = true }
    }
    private var enabled = false
    private var revision = 0L
    private var failures = 0
    private val pulse = object : Runnable {
        override fun run() {
            if (!enabled || !SettingsStore.controllerBatteryPolling(app)) return
            val host = SettingsStore.getRemoteHost(app)
            val key = if (host.isNotBlank()) RecentDevices.key(app, host) else null
            if (key == null) {
                failures = 3
                ControllerRemoteTelemetry.failed("Sem aparelho pareado")
                onUpdated()
                handler.postDelayed(this, RemoteBatteryPollingPolicy.nextDelayMs(failures))
                return
            }
            val token = revision
            executor.execute {
                val result = runCatching { RemoteClient.batteryProbe(host, key, app) }
                handler.post {
                    if (!enabled || token != revision || !SettingsStore.controllerBatteryPolling(app))
                        return@post
                    val current = SettingsStore.getRemoteHost(app) == host &&
                        RecentDevices.key(app, host)?.let { MessageDigest.isEqual(it, key) } == true
                    if (current && result.isSuccess) {
                        ControllerRemoteTelemetry.onSnapshot(app, host, result.getOrThrow())
                        failures = 0
                    } else {
                        failures = (failures + 1).coerceAtMost(3)
                        ControllerRemoteTelemetry.failed(when {
                            !current -> "Pareamento alterado"
                            result.exceptionOrNull() is java.net.SocketTimeoutException -> "Consulta expirou"
                            result.exceptionOrNull() is java.net.ConnectException -> "Aparelho indisponível"
                            else -> "Sem resposta autenticada (requer alpha.140)"
                        })
                    }
                    onUpdated()
                    handler.postDelayed(this, RemoteBatteryPollingPolicy.nextDelayMs(failures))
                }
            }
        }
    }

    /** Chamado da thread principal em mudanças de autorização/serviço. */
    fun sync() {
        val desired = SettingsStore.controllerBatteryPolling(app)
        if (desired == enabled) return
        revision++
        enabled = desired
        failures = 0
        handler.removeCallbacks(pulse)
        if (enabled) handler.post(pulse)
    }

    fun close() {
        enabled = false
        revision++
        handler.removeCallbacks(pulse)
        executor.shutdownNow()
    }
}
