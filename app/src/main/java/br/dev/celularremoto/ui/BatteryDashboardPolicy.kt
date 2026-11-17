package br.dev.celularremoto.ui

import java.util.Locale

/** Política pura de responsividade. Não depende de rede, sensores ou Android. */
internal object BatteryDashboardPolicy {
    fun metricColumns(widthDp: Int, fontScale: Float): Int = when {
        widthDp < 360 || fontScale > 1.20f -> 2
        else -> 3
    }

    fun isRecent(nowMs: Long, observedAtMs: Long, maxAgeMs: Long): Boolean =
        observedAtMs > 0 && observedAtMs <= nowMs && nowMs - observedAtMs <= maxAgeMs

    /** O resumo atual não deve misturar uma previsão da amostra anterior com a leitura nova. */
    fun matchesSession(status: Int, plugged: Int, level: Int, kind: String, finalPercent: Int): Boolean =
        level in 0..100 && level == finalPercent && when (kind) {
            "CHARGE" -> status == 2 && plugged > 0
            "DISCHARGE" -> status == 3
            else -> false
        }

    fun forecastLabel(kind: String, minutes: Long): String? {
        val target = when (kind) {
            "CHARGE" -> 100
            "DISCHARGE" -> 0
            else -> return null
        }
        return when {
            minutes < 0L -> null
            minutes == 0L -> if (target == 100) "Bateria em 100%" else "Bateria em 0%"
            else -> "≈ ${minutesLabel(minutes)} até $target%"
        }
    }

    fun minutesLabel(minutes: Long): String =
        if (minutes < 60) "${minutes}min" else if (minutes % 60 == 0L) "${minutes / 60}h"
        else "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}"

    fun durationLabel(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1_000
        val parts = listOf(seconds / 3600 to "h", seconds / 60 % 60 to "min", seconds % 60 to "s")
        return parts.filter { it.first > 0 }.joinToString(" ") { "${it.first}${it.second}" }.ifEmpty { "0s" }
    }

    /** Formata as marcas antes de remover duplicatas geradas pelo arredondamento. */
    fun axisLabel(metric: String, value: Double, locale: Locale): String? = when (metric) {
        "percent" -> "%.0f%%".format(locale, value.coerceIn(0.0, 100.0))
        "temp" -> "%.1f".format(locale, value)
        "ma" -> "%.0f".format(locale, value)
        else -> null
    }
}
