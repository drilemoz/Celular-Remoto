package br.dev.celularremoto.core

/** Eventos de limiar precisam de cruzamento entre amostras reais; valores iniciais
 * ou valores recebidos pela primeira vez não são eventos retroativos.
 */
internal object BatteryGoalPolicy {
    fun crossings(previousPercent: Int, currentPercent: Int, previousTemp: Double?,
        currentTemp: Double?, plugged: Boolean, goal: Int, low: Int, hot: Int): List<String> {
        if (previousPercent !in 0..100 || currentPercent !in 0..100) return emptyList()
        val triggered = mutableListOf<String>()
        if (plugged && previousPercent < goal && currentPercent >= goal) triggered.add("GOAL")
        if (previousPercent > low && currentPercent <= low) triggered.add("LOW")
        if (previousTemp != null && currentTemp != null && previousTemp.isFinite() && currentTemp.isFinite() &&
            previousTemp < hot && currentTemp >= hot) triggered.add("HOT")
        return triggered
    }
}
