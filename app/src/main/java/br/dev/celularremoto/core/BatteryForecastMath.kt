package br.dev.celularremoto.core

import kotlin.math.abs
import kotlin.math.ceil

/** Previsão conservadora inspirada nas regras de Charge/DischargeForecast do Pulso Bateria.
 * Somente percentual real + duração observada; histórico do MESMO tipo de fonte Android.
 * 'USB' não distingue carregador convencional de painel USB — não inferir potência solar.
 */
internal object BatteryForecastMath {
    data class Point(val elapsedMs: Long, val wallMs: Long, val percent: Int)
    data class Session(val mode: String, val source: String?, val startedMs: Long, val endedMs: Long,
        val observedMs: Long, val startPercent: Int, val endPercent: Int, val samples: Int)
    data class Prediction(val minutes: Long, val percentPerHour: Double, val basis: String,
        val evidence: String, val historicalSessions: Int, val to80: Long?, val to90: Long?)

    private const val HOUR_MS = 3_600_000L
    private const val RECENT_MS = 7 * 24 * HOUR_MS
    private const val MAX_ESTIMATE_MIN = 7 * 24 * 60L
    private const val MIN_INTERVAL_MS = 10 * 60_000L
    private const val MAX_GAP_MS = 120_000L

    /** Tendência dos últimos 45 min, somente em sequência sem lacunas; 2 p.p. de sinal válido. */
    fun liveRate(mode: String, points: List<Point>): Double? {
        if (mode !in setOf("CHARGE", "DISCHARGE") || points.size < 3) return null
        val last = points.last()
        if (last.percent !in 0..100 || last.elapsedMs < 0L) return null
        var first = last
        var previous = last
        var count = 1
        for (index in points.size - 2 downTo 0) {
            val candidate = points[index]
            val dt = previous.elapsedMs - candidate.elapsedMs
            val wall = previous.wallMs - candidate.wallMs
            if (dt !in 1..MAX_GAP_MS || wall !in 1..MAX_GAP_MS || abs(dt - wall) > 10_000L ||
                last.elapsedMs - candidate.elapsedMs > 45 * 60_000L || candidate.percent !in 0..100) break
            first = candidate
            previous = candidate
            count++
        }
        val elapsed = last.elapsedMs - first.elapsedMs
        val delta = if (mode == "CHARGE") last.percent - first.percent else first.percent - last.percent
        if (count < 3 || elapsed < MIN_INTERVAL_MS || delta < 2) return null
        return (delta.toDouble() * HOUR_MS / elapsed).takeIf { it.isFinite() && it in 0.25..40.0 }
    }

    /** Taxa histórica ponderada pelo tempo observado; requer >=2 sessões e >=4 p.p.
     * Recargas com fonte não identificada não são misturadas com outras fontes.
     */
    fun historyRate(mode: String, source: String?, sessions: List<Session>, nowMs: Long): Pair<Double, Int>? {
        if (mode !in setOf("CHARGE", "DISCHARGE") || nowMs <= 0L ||
            (mode == "CHARGE" && source.isNullOrBlank())) return null
        val matches = sessions.asSequence().filter { s ->
            s.mode == mode && (mode != "CHARGE" || s.source == source) &&
                s.endedMs > 0 && s.endedMs <= nowMs && nowMs - s.endedMs <= RECENT_MS &&
                s.startedMs in 1..s.endedMs && s.observedMs in 600_000L..(24 * HOUR_MS) &&
                s.observedMs <= s.endedMs - s.startedMs + 60_000L && s.samples >= 3 &&
                s.startPercent in 0..100 && s.endPercent in 0..100 &&
                (if (mode == "CHARGE") s.endPercent - s.startPercent else s.startPercent - s.endPercent) >= 2
        }.take(12).toList()
        if (matches.size < 2) return null
        val duration = matches.sumOf { it.observedMs }
        val delta = matches.sumOf { (if (mode == "CHARGE") it.endPercent - it.startPercent else it.startPercent - it.endPercent).toLong() }
        if (duration < 30 * 60_000L || delta < 4L) return null
        val rate = delta.toDouble() * HOUR_MS / duration
        return if (rate.isFinite() && rate in 0.25..40.0) rate to matches.size else null
    }

    fun predict(mode: String, percent: Int, readAtMs: Long, nowMs: Long, points: List<Point>,
        source: String?, history: List<Session>): Prediction? {
        if (mode !in setOf("CHARGE", "DISCHARGE") || percent !in 0..100 || readAtMs <= 0L ||
            nowMs < readAtMs || nowMs - readAtMs > 180_000L) return null
        val live = liveRate(mode, points)
        val past = historyRate(mode, source, history, nowMs)
        val rate = when {
            live != null && past != null -> 0.70 * live + 0.30 * past.first
            live != null -> live
            past != null -> past.first
            else -> return null
        }
        if (!rate.isFinite() || rate !in 0.25..40.0) return null
        fun to(target: Int): Long? = if (percent >= target) 0L else
            ceil((target - percent) * 60.0 / rate).toLong().takeIf { it in 1..MAX_ESTIMATE_MIN }
        val remaining = if (mode == "CHARGE") to(100) else
            if (percent == 0) 0L else ceil(percent * 60.0 / rate).toLong().takeIf { it in 1..MAX_ESTIMATE_MIN }
        if (remaining == null) return null
        val basis = when {
            live != null && past != null -> "Tendência atual (70%) + histórico da mesma categoria de fonte (30%)"
            live != null -> "Tendência atual observada, últimos até 45 minutos"
            else -> "Histórico recente de ${past!!.second} sessões comparáveis"
        }
        val evidence = when {
            live != null && past != null -> "TENDENCIA_E_HISTORICO"
            live != null -> "TENDENCIA_ATUAL"
            else -> "HISTORICO"
        }
        return Prediction(remaining, rate, basis, evidence, past?.second ?: 0,
            if (mode == "CHARGE") to(80) else null,
            if (mode == "CHARGE") to(90) else null)
    }
}
