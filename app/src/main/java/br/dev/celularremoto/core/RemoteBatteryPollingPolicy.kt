package br.dev.celularremoto.core

/** Acompanhamento opcional: no máximo uma consulta curta por minuto após sucesso.
 * Falhas não criam loop de reconexão nem wake lock permanente.
 */
object RemoteBatteryPollingPolicy {
    fun nextDelayMs(consecutiveFailures: Int): Long = when {
        consecutiveFailures <= 0 -> 60_000L
        consecutiveFailures == 1 -> 120_000L
        consecutiveFailures == 2 -> 180_000L
        else -> 300_000L
    }
}
