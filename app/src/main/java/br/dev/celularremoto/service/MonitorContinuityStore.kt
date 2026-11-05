package br.dev.celularremoto.service

import android.content.Context
import android.os.SystemClock

/** Metadados locais mínimos de continuidade. Nenhum IP, chave, leitura ou identificador remoto. */
internal object MonitorContinuityStore {
    private const val FILE = "monitor_continuity_private"
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private var lastHeartbeatElapsed = 0L

    internal data class Snapshot(
        val startMs: Long, val lastHeartbeatMs: Long, val lastStopMs: Long,
        val unclosedRestarts: Int, val lastStartUnclosed: Boolean, val sessionOpen: Boolean
    )

    @Synchronized fun started(context: Context) {
        val p = prefs(context)
        val priorOpen = p.getBoolean("session_open", false)
        val count = MonitorContinuityPolicy.nextUnclosedCount(
            p.getInt("unclosed_restarts", 0), priorOpen)
        p.edit().putBoolean("session_open", true)
            .putBoolean("last_start_unclosed", priorOpen)
            .putInt("unclosed_restarts", count)
            .putLong("start_ms", System.currentTimeMillis()).apply()
        lastHeartbeatElapsed = 0L
        heartbeat(context, SystemClock.elapsedRealtime())
    }

    @Synchronized fun heartbeat(context: Context, nowElapsed: Long) {
        if (!MonitorContinuityPolicy.shouldSaveHeartbeat(lastHeartbeatElapsed, nowElapsed)) return
        lastHeartbeatElapsed = nowElapsed
        prefs(context).edit().putLong("heartbeat_ms", System.currentTimeMillis()).apply()
    }

    @Synchronized fun stopped(context: Context) {
        lastHeartbeatElapsed = 0L
        val p = prefs(context)
        if (!p.getBoolean("session_open", false)) return
        p.edit().putBoolean("session_open", false)
            .putLong("stop_ms", System.currentTimeMillis()).apply()
    }

    fun snapshot(context: Context): Snapshot {
        val p = prefs(context)
        return Snapshot(p.getLong("start_ms", 0L), p.getLong("heartbeat_ms", 0L),
            p.getLong("stop_ms", 0L), p.getInt("unclosed_restarts", 0),
            p.getBoolean("last_start_unclosed", false), p.getBoolean("session_open", false))
    }
}
