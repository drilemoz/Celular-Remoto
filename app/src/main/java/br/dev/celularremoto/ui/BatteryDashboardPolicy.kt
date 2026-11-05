package br.dev.celularremoto.ui

/** Política pura de responsividade. Não depende de rede, sensores ou Android. */
internal object BatteryDashboardPolicy {
    fun metricColumns(widthDp: Int, fontScale: Float): Int = when {
        widthDp < 360 || fontScale > 1.20f -> 2
        else -> 3
    }
}
