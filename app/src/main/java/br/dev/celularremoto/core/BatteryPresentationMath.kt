package br.dev.celularremoto.core

import java.util.Locale
import kotlin.math.abs

/**
 * Regras de apresentação: não alteram a telemetria armazenada.
 * A interface pode ajustar o sinal visual quando o estado da bateria
 * indica descarga, mas o valor bruto continua disponível no diagnóstico.
 */
internal object BatteryPresentationMath {
    fun visualCurrentMa(currentMa: Double?, statusCode: Int? = null, kind: String? = null): Double? {
        if (currentMa == null || !currentMa.isFinite()) return null
        return if (isDischarging(statusCode, kind) && currentMa > 0.0) -currentMa else currentMa
    }

    fun visualPowerW(voltageMv: Int, currentMa: Double?, statusCode: Int? = null, kind: String? = null): Double? =
        BatteryCurrentMath.batteryPowerWatts(voltageMv, visualCurrentMa(currentMa, statusCode, kind))

    fun directionLabel(currentMa: Double?, statusCode: Int? = null, kind: String? = null): String = when {
        visualCurrentMa(currentMa, statusCode, kind) == null -> "Sem leitura"
        abs(visualCurrentMa(currentMa, statusCode, kind) ?: 0.0) < 0.0001 -> "Fluxo líquido próximo de zero"
        (visualCurrentMa(currentMa, statusCode, kind) ?: 0.0) < 0.0 -> "Saída líquida da bateria"
        else -> "Entrada líquida na bateria"
    }

    fun sourceLabel(source: String): String = when (source) {
        "NOW" -> "Instantânea"
        "AVERAGE" -> "Média Android"
        "COUNTER" -> "Contador"
        else -> "Sem leitura"
    }

    fun isVisuallyAdjusted(currentMa: Double?, statusCode: Int? = null, kind: String? = null): Boolean {
        if (currentMa == null || !currentMa.isFinite()) return false
        return isDischarging(statusCode, kind) && currentMa > 0.0
    }

    fun currentText(currentMa: Double?, statusCode: Int? = null, kind: String? = null): String =
        visualCurrentMa(currentMa, statusCode, kind)?.let { "%+.0f mA".format(Locale.getDefault(), it) } ?: "—"

    fun powerText(voltageMv: Int, currentMa: Double?, statusCode: Int? = null, kind: String? = null): String =
        visualPowerW(voltageMv, currentMa, statusCode, kind)?.let { "%+.2f W".format(Locale.getDefault(), it) } ?: "—"

    private fun isDischarging(statusCode: Int?, kind: String?): Boolean =
        statusCode == 3 || kind == "DISCHARGE"
}
