package br.dev.celularremoto.core

/** A disponibilidade é a fração de sondagens realmente executadas que receberam resposta autenticada.
 * Um intervalo sem sondagem NÃO é contado como falha nem como tempo online.
 */
object ConnectionHealthMath {
    const val STALE_AFTER_MS = 45_000L
    const val MISSING_SAMPLE_AFTER_MS = 90_000L
    const val ALERT_AFTER_FAILURES = 3

    data class Sample(val atMs: Long, val succeeded: Boolean, val rttMs: Long?)
    data class Summary(val attempts: Int, val succeeded: Int, val failed: Int, val availabilityPercent: Double?,
        val meanRttMs: Long?, val missedWindows: Int, val longestObservedFailureMs: Long,
        val failuresInSequence: Int)

    fun summarize(samples: List<Sample>): Summary {
        val ordered = samples.filter { it.atMs > 0L }.sortedBy { it.atMs }
        val success = ordered.count { it.succeeded }
        val rtts = ordered.filter { it.succeeded }.mapNotNull { it.rttMs?.takeIf { ms -> ms >= 0 } }
        var missing = 0
        var longest = 0L
        var failureStart: Long? = null
        var streak = 0
        var previous: Long? = null
        for (s in ordered) {
            if (previous != null && s.atMs - previous > MISSING_SAMPLE_AFTER_MS) {
                missing++
                failureStart = null // Tempo sem sondagem não prova continuidade da queda.
            }
            if (s.succeeded) {
                if (failureStart != null) longest = maxOf(longest, s.atMs - failureStart)
                failureStart = null
                streak = 0
            } else {
                if (failureStart == null) failureStart = s.atMs
                streak++
            }
            previous = s.atMs
        }
        return Summary(ordered.size, success, ordered.size - success,
            if (ordered.isEmpty()) null else success * 100.0 / ordered.size,
            if (rtts.isEmpty()) null else rtts.average().toLong(), missing, longest, streak)
    }

    fun probeDelayMs(consecutiveFailures: Int): Long = when {
        consecutiveFailures <= 0 -> 30_000L
        consecutiveFailures == 1 -> 30_000L
        consecutiveFailures == 2 -> 45_000L
        consecutiveFailures == 3 -> 60_000L
        consecutiveFailures == 4 -> 120_000L
        else -> 300_000L
    }

    fun connectionLabel(lastSuccessMs: Long, nowMs: Long): String = when {
        lastSuccessMs <= 0L -> "Não verificado"
        nowMs < lastSuccessMs -> "Aguardando nova verificação"
        nowMs - lastSuccessMs <= STALE_AFTER_MS -> "Resposta recente"
        else -> "Aguardando nova verificação"
    }
}
