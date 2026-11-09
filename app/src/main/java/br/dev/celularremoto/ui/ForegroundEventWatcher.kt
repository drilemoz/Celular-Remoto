package br.dev.celularremoto.ui

import android.content.Context
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.RemoteMessageSessionPolicy
import br.dev.celularremoto.core.RemoteEventAlerts
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.RemoteClient
import java.net.Socket
import java.security.MessageDigest
import kotlin.concurrent.thread

/** Uma espera AES-GCM de no máximo 11s, somente com Activity visível.
 * NÃO inicia serviço, não pede isenção de Doze e não promete eventos em segundo plano.
 * As consultas periódicas existentes continuam sendo o fallback.
 */
internal class ForegroundEventWatcher(
    context: Context,
    private val shouldWatch: () -> Boolean,
    private val onEventsChanged: (host: String, scope: String) -> Unit
) {
    private val appContext = context.applicationContext
    private val lock = Object()
    private var socket: Socket? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    @Volatile private var session = 0L

    @Synchronized fun start() {
        if (running) return
        running = true
        val id = ++session
        worker = thread(name = "CelularRemoto-event-watch", isDaemon = true) { loop(id) }
    }

    @Synchronized fun stop() {
        running = false
        session++
        synchronized(lock) { runCatching { socket?.close() }; socket = null }
        worker?.interrupt()
        worker = null
    }

    /** Desperta a espera pendente ao desligar atualização da página de Mensagens. */
    fun reconsider() {
        if (!shouldWatch()) synchronized(lock) { runCatching { socket?.close() }; socket = null }
    }

    private fun sleep(ms: Long) { try { Thread.sleep(ms) } catch (_: InterruptedException) { } }

    private fun loop(id: Long) {
        var previousHost = ""
        var previousScope = ""
        var revision = 0L
        var failures = 0
        while (running && id == session) {
            if (!shouldWatch()) { revision = 0L; sleep(1_500L); continue }
            val host = SettingsStore.getRemoteHost(appContext)
            val key = if (host.isNotBlank()) RecentDevices.key(appContext, host) else null
            if (key == null || host.isBlank()) { revision = 0L; sleep(4_000L); continue }
            val scope = br.dev.celularremoto.core.RemoteEventNoticePolicy.pairingScope(host, key)
            if (host != previousHost || scope != previousScope) {
                previousHost = host
                previousScope = scope
                revision = 0L
                failures = 0
            }
            val before = revision
            val response = runCatching {
                RemoteClient.watchEvents(host, key, appContext, revision) { connected ->
                    synchronized(lock) {
                        if (running && id == session && shouldWatch()) socket = connected else connected.close()
                    }
                }
            }
            synchronized(lock) { if (id == session) socket = null }
            if (!running || id != session) break
            response.onSuccess { reply ->
                failures = 0
                if (reply.optBoolean("busy")) {
                    sleep(3_000L)
                } else if (running && id == session && shouldWatch() && host == SettingsStore.getRemoteHost(appContext) &&
                    RecentDevices.key(appContext, host)?.let { MessageDigest.isEqual(it, key) } == true) {
                    revision = reply.optLong("revision", 0L)
                    RemoteEventAlerts.onSnapshot(appContext, host, reply)
                    if (before != 0L && revision != before && running && id == session) onEventsChanged(host, scope)
                }
            }.onFailure {
                // Peer antigo, rede desconectada, timeout ou parada do Activity.
                // Backoff exponencial conservador para peer antigo, perda de rede e Doze.
                // Fallback JobScheduler + consulta manual continuam disponíveis.
                if (running && id == session) {
                    if (!shouldWatch()) { failures = 0; sleep(1_500L) }
                    else {
                        failures = (failures + 1).coerceAtMost(5)
                        sleep((15_000L shl (failures - 1)).coerceAtMost(240_000L))
                    }
                }
            }
        }
    }
}

/** A Activity fornece contexto e estados; o observador nunca segura uma View. */
internal fun MainActivity.makeForegroundEventWatcher() = ForegroundEventWatcher(applicationContext,
    shouldWatch = { active && (SettingsStore.controllerNotificationAlerts(this) ||
        (mode == 4 && messagesAutoRefresh)) },
    onEventsChanged = { notifiedHost, notifiedScope ->
        handler.post {
            // A mensagem de evento pode ficar na fila principal enquanto o usuário
            // troca de aparelho, chave ou página. Nunca atualizar a sessão nova.
            val currentKey = RecentDevices.key(applicationContext, notifiedHost)
            val validSelection = currentKey != null &&
                br.dev.celularremoto.core.RemoteEventNoticePolicy.pairingScope(notifiedHost, currentKey) == notifiedScope &&
                RemoteMessageSessionPolicy.matches(messagesHost, messagesPairingScope,
                    SettingsStore.getRemoteHost(this), notifiedScope) && messagesHost == notifiedHost
            if (active && mode == 4 && messagesAutoRefresh && validSelection) {
                if (messagesLoading) messagesRefreshPending = true else loadRemoteMessages()
            }
        }
    })
