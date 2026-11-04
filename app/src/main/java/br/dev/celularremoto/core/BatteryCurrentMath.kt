package br.dev.celularremoto.core

import java.util.ArrayDeque
import kotlin.math.abs

/** Regras conservadoras adaptadas do Pulso Bateria: a corrente é líquida na bateria.
 * STATUS_CHARGING não impõe sinal positivo: um carregador fraco pode produzir descarga líquida.
 * A conversão por µA segue o contrato Android; correções OEM exigem evidências independentes.
 */
internal object BatteryCurrentMath {
    data class Reading(val ma: Double?, val source: String, val quality: String,
        val diagnostic: String, val warning: String?, val nowMa: Double?, val averageMa: Double?)

    fun rawMa(raw: Int?): Double? = raw?.takeUnless {
        it == Int.MIN_VALUE || it == Int.MAX_VALUE || abs(it.toLong()) > 30_000_000L
    }?.div(1000.0)

    fun select(rawNow: Int?, rawAverage: Int?, counterMeanMa: Double?, status: Int): Reading {
        val now = rawMa(rawNow)
        val average = rawMa(rawAverage)
        val counter = counterMeanMa?.takeIf { it.isFinite() && abs(it) <= 30_000.0 }
        // Preferir leitura instantânea válida. Zero não é sentinela; só usar média quando
        // instantânea é zero mas média apresenta resolução não nula.
        val (value, source) = when {
            now != null && now != 0.0 -> now to "NOW"
            average != null && average != 0.0 -> average to "AVERAGE"
            counter != null -> counter to "COUNTER"
            now != null -> now to "NOW"
            average != null -> average to "AVERAGE"
            else -> null to "NONE"
        }
        val note = when (source) {
            "NOW" -> "Corrente líquida instantânea do Android (µA → mA)"
            "AVERAGE" -> "Corrente líquida média do Android (janela definida pelo hardware)"
            "COUNTER" -> "Média derivada de variação observada de CHARGE_COUNTER (µAh)"
            else -> "Propriedades de corrente não disponíveis ou não confiáveis"
        }
        val directionWarning = when {
            value == null -> null
            value == 0.0 -> "Zero informado; resolução do sensor não confirmada"
            status == 2 && value < 0.0 -> "Corrente negativa apesar de CHARGING: a bateria pode estar perdendo carga líquida"
            status == 3 && value > 0.0 -> "Corrente positiva apesar de DISCHARGING: confirmar comportamento do fabricante"
            else -> null
        }
        return Reading(value, source,
            when (source) { "NOW", "AVERAGE" -> "ANDROID"; "COUNTER" -> "CONTADOR"; else -> "INDISPONIVEL" },
            note, directionWarning, now, average)
    }

    fun batteryPowerWatts(voltageMv: Int, currentMa: Double?): Double? =
        if (voltageMv in 2_000..6_000 && currentMa != null && currentMa.isFinite() && abs(currentMa) <= 30_000.0)
            voltageMv.toDouble() * currentMa / 1_000_000.0 else null
}

/** Média por diferença de contador com relógio monotônico e janela real.
 * Nunca corrige sinal/escala OEM baseado apenas no estado de carregamento.
 * Estado somente em memória; reinício e lacunas não geram corrente retroativa.
 */
internal class BatteryCounterMeanTracker {
    private data class Point(val timeMs: Long, val chargeMicroAh: Int)
    private val points = ArrayDeque<Point>()
    private var lastStatus = -1
    private var lastValue: Double? = null
    var windowMs: Long? = null
        private set

    @Synchronized fun observe(rawCounter: Int?, status: Int, elapsedMs: Long): Double? {
        if (rawCounter == null || rawCounter !in 0..100_000_000 || status !in 2..5 || elapsedMs < 0) {
            reset(); return null
        }
        if (lastStatus != status) reset()
        lastStatus = status
        val last = points.peekLast()
        if (last != null) {
            val dt = elapsedMs - last.timeMs
            if (dt == 0L && rawCounter == last.chargeMicroAh) return lastValue
            val delta = rawCounter.toLong() - last.chargeMicroAh
            if (dt <= 0L || dt > 180_000L || abs(delta * 3_600.0 / dt) > 30_000.0) reset()
        }
        lastStatus = status
        points.addLast(Point(elapsedMs, rawCounter))
        while (points.size > 2 && elapsedMs - points.first.timeMs > 600_000L) points.removeFirst()
        if (points.size < 3) return null
        val duration = elapsedMs - points.first.timeMs
        val delta = rawCounter.toLong() - points.first.chargeMicroAh
        if (duration !in 90_000L..600_000L || abs(delta) < 1_000L) return null
        val mean = delta * 3_600.0 / duration
        if (!mean.isFinite() || abs(mean) > 30_000.0) return null
        windowMs = duration
        lastValue = mean
        return mean
    }

    private fun reset() { points.clear(); windowMs = null; lastValue = null; lastStatus = -1 }
}
