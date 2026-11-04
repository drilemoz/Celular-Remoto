package br.dev.celularremoto.core

import kotlin.math.abs

/** Adaptado das regras conservadoras de LabMath do Pulso Bateria alpha.73.
 * Apenas intervalos entre leituras reais, com tempo monotônico e relógio coerentes.
 */
internal object BatteryLabMath {
    const val MAX_INTERVAL_MS = 90_000L
    data class Interval(val chargingMs: Long = 0L, val idleMs: Long = 0L,
        val currentMaMs: Double = 0.0, val validCurrentMs: Long = 0L)

    fun classify(previousMode: String, nextMode: String, previousMa: Double?, nextMa: Double?,
        previousSource: String?, nextSource: String?, elapsedDeltaMs: Long, wallDeltaMs: Long): Interval {
        if (elapsedDeltaMs !in 1..MAX_INTERVAL_MS || wallDeltaMs <= 0L ||
            abs(wallDeltaMs - elapsedDeltaMs) > 10_000L || previousMode != nextMode) return Interval()
        if (previousMode == "CHARGING") {
            val valid = setOf("NOW", "AVERAGE", "COUNTER")
            if (previousSource in valid && nextSource in valid && previousMa != null && nextMa != null &&
                previousMa.isFinite() && nextMa.isFinite() && previousMa in 0.00001..30_000.0 &&
                nextMa in 0.00001..30_000.0)
                return Interval(chargingMs = elapsedDeltaMs,
                    currentMaMs = (previousMa + nextMa) / 2.0 * elapsedDeltaMs,
                    validCurrentMs = elapsedDeltaMs)
            return Interval(chargingMs = elapsedDeltaMs)
        }
        return if (previousMode in setOf("DISCHARGING", "IDLE", "FULL"))
            Interval(idleMs = elapsedDeltaMs) else Interval()
    }

    /** Firmware pode seguir informando CHARGING quando o indicador chegou a 100%. */
    fun fullConfirmations(previous: Int, kind: String, percent: Int,
        plugged: Int, mode: String): Int =
        if (kind != "DISCHARGE" && percent == 100 && plugged > 0 &&
            mode in setOf("CHARGING", "FULL")) (previous.coerceIn(0, 2) + 1).coerceAtMost(2)
        else 0

    fun meanCurrent(weightedMaMs: Double, validMs: Long): Double? =
        if (validMs > 0L && weightedMaMs.isFinite() && weightedMaMs > 0.0)
            (weightedMaMs / validMs).takeIf { it in 0.0..30_000.0 } else null

    fun unknownMs(totalMs: Long, chargingMs: Long, idleMs: Long): Long =
        (totalMs.coerceAtLeast(0L) - chargingMs.coerceAtLeast(0L) - idleMs.coerceAtLeast(0L))
            .coerceAtLeast(0L)
}
