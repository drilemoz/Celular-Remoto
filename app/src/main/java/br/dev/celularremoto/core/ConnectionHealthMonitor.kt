package br.dev.celularremoto.core

import android.content.Context
import android.os.SystemClock
import br.dev.celularremoto.transport.RemoteClient
import java.security.MessageDigest

/** Probes são ações curtas em thread de I/O, somente em hosts já pareados. */
object ConnectionHealthMonitor {
    data class Result(val ok: Boolean, val host: String, val rttMs: Long?, val changedHost: Boolean,
        val problem: String = "")

    fun record(context: Context, host: String, success: Boolean, rttMs: Long?, error: Throwable? = null) {
        if (!ConnectionHealthStore.enabled(context) || host.isBlank()) return
        val transition = ConnectionHealthStore.record(context, host, success, rttMs,
            error?.javaClass?.simpleName.orEmpty())
        if (transition != null) ConnectionHealthNotifier.alert(context, transition)
    }

    /** O endereço é escolhido apenas entre perfis locais com credencial idêntica.
     * Não varre sub-redes e não altera a chave privada do pareamento.
     */
    fun probe(context: Context, allowKnownHostRecovery: Boolean = true, forceRecovery: Boolean = false): Result {
        val app = context.applicationContext
        val host = SettingsStore.getRemoteHost(app)
        val key = if (host.isNotBlank()) RecentDevices.key(app, host) else null
        if (key == null) return Result(false, host, null, false, "Sem aparelho pareado")
        val start = SystemClock.elapsedRealtime()
        val first = runCatching { RemoteClient.heartbeat(host, key, app) }
        if (first.isSuccess) {
            val rtt = (SystemClock.elapsedRealtime() - start).coerceAtLeast(0L)
            record(app, host, true, rtt)
            return Result(true, host, rtt, false)
        }
        record(app, host, false, null, first.exceptionOrNull())
        val streak = ConnectionHealthStore.status(app, host).failureStreak
        if (!allowKnownHostRecovery || (streak < 3 && !forceRecovery)) return Result(false, host, null, false,
            first.exceptionOrNull()?.javaClass?.simpleName.orEmpty())
        // Limite pequeno: no máximo quatro candidatos salvos e só a cada sequência de falhas.
        for (candidate in RecentDevices.list(app).filter { it.host != host }.take(4)) {
            val candidateKey = RecentDevices.key(app, candidate.host) ?: continue
            if (!MessageDigest.isEqual(key, candidateKey)) continue
            val candidateStart = SystemClock.elapsedRealtime()
            val test = runCatching { RemoteClient.heartbeat(candidate.host, candidateKey, app) }
            if (test.isFailure) continue
            val rtt = (SystemClock.elapsedRealtime() - candidateStart).coerceAtLeast(0L)
            record(app, candidate.host, true, rtt)
            synchronized(this) {
                // Se o usuário mudou o dispositivo enquanto o teste rodava, não alterar seleção.
                if (SettingsStore.getRemoteHost(app) == host &&
                    RecentDevices.key(app, host)?.let { MessageDigest.isEqual(it, key) } == true) {
                    SettingsStore.setRemoteHost(app, candidate.host)
                    return Result(true, candidate.host, rtt, true)
                }
            }
            return Result(true, candidate.host, rtt, false)
        }
        return Result(false, host, null, false, first.exceptionOrNull()?.javaClass?.simpleName.orEmpty())
    }
}
