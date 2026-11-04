package br.dev.celularremoto.core

import kotlin.math.abs
import kotlin.math.ceil

/** Estimativas conservadoras baseadas na variação OBSERVADA do percentual.
 *  Não traduz corrente da bateria em potência do painel solar nem assume capacidade nominal.
 */
internal object BatterySessionMath {
    const val MAX_GAP_MS = 120_000L
    const val MIN_DURATION_MS = 10 * 60_000L
    fun forecastMinutes(mode: String, startPercent: Int, endPercent: Int, observedMs: Long): Long? {
        if (mode !in setOf("CHARGE", "DISCHARGE") ||
            startPercent !in 0..100 || endPercent !in 0..100 ||
            observedMs < MIN_DURATION_MS) return null
        val delta = if (mode == "CHARGE") endPercent - startPercent else startPercent - endPercent
        if (delta < 2) return null
        val ratePerHour = delta.toDouble() * 3_600_000.0 / observedMs
        if (!ratePerHour.isFinite() || ratePerHour !in 0.25..40.0) return null
        val remaining = if (mode == "CHARGE") 100 - endPercent else endPercent
        if (remaining <= 0) return 0L
        return ceil(remaining * 60.0 / ratePerHour).toLong().takeIf { it in 1..10_080L }
    }

    fun validInterval(mode: String, before: String, after: String, elapsedMs: Long, wallMs: Long): Long =
        if (mode == before && before == after && elapsedMs in 1..MAX_GAP_MS &&
            wallMs > 0 && abs(elapsedMs - wallMs) <= 10_000L) elapsedMs else 0L
}
