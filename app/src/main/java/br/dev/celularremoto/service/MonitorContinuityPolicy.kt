package br.dev.celularremoto.service

/** Diagnóstico de execução, sem inferir Doze, queda de Wi-Fi ou encerramento forçado. */
internal object MonitorContinuityPolicy {
    const val HEARTBEAT_PERIOD_MS = 120_000L

    fun shouldSaveHeartbeat(lastElapsed: Long, nowElapsed: Long): Boolean =
        lastElapsed == 0L || nowElapsed < lastElapsed ||
            nowElapsed - lastElapsed >= HEARTBEAT_PERIOD_MS

    fun nextUnclosedCount(previous: Int, priorSessionOpen: Boolean): Int =
        (previous.coerceIn(0, 1_000_000) + if (priorSessionOpen) 1 else 0).coerceAtMost(1_000_000)

    /** Retorna null para horários ausentes ou no futuro (relógio alterado). */
    fun ageMinutes(timestampMs: Long, nowMs: Long): Long? =
        (nowMs - timestampMs).takeIf { timestampMs > 0L && it >= 0L }?.div(60_000L)
}
