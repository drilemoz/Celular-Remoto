package br.dev.celularremoto.core

import java.util.Locale

/**
 * Conteúdo resumido da notificação foreground.
 * Apenas dados observados; nunca confunde uma última resposta TCP com conexão ativa
 * nem inventa previsão ou potência de um painel solar.
 */
internal object MonitorNotificationContent {
    data class Battery(
        val percent: Int?, val state: String, val temperatureC: Double?,
        val currentMa: Double?, val forecastMinutes: Long?,
        val ratePercentHour: Double?, val observedAtMs: Long,
    )

    private fun decimal(value: Double, digits: Int = 1): String =
        String.format(Locale.forLanguageTag("pt-BR"), "%1$.${digits}f", value)

    fun valid(battery: Battery?, nowMs: Long): Battery? = battery?.takeIf {
        it.observedAtMs > 0 && nowMs >= it.observedAtMs && nowMs - it.observedAtMs <= 180_000L
    }

    /** Evita apresentar corrente com sinal conflitante como descarga confirmada. */
    fun currentReliable(b: Battery): Boolean {
        val ma = b.currentMa ?: return false
        if (!ma.isFinite() || ma !in -30_000.0..30_000.0 || ma == 0.0) return false
        return when (b.state) {
            "Descarregando" -> ma < 0.0
            "Carregando" -> ma > 0.0
            else -> false
        }
    }

    fun title(battery: Battery?): String {
        if (battery == null) return "Bateria · sem leitura"
        val parts = mutableListOf<String>()
        battery.percent?.takeIf { it in 0..100 }?.let { parts.add("$it%") }
        battery.temperatureC?.takeIf { it.isFinite() && it in -20.0..95.0 }
            ?.let { parts.add("${decimal(it)} °C") }
        if (currentReliable(battery)) parts.add("${decimal(battery.currentMa!!, 0)} mA")
        return parts.joinToString(" · ").ifBlank { "Bateria · sem dados" }
    }

    /** Previsão derivada da sessão observada, nunca de corrente instantânea. */
    fun batteryLine(battery: Battery?, nowMs: Long = System.currentTimeMillis()): String {
        if (battery == null) return "Aguardando leitura da bateria"
        val minutes = battery.forecastMinutes?.takeIf { it in 1..10_080 }
        val mode = battery.state
        if (minutes != null && mode in setOf("Carregando", "Descarregando") &&
            battery.observedAtMs in 1..nowMs && nowMs - battery.observedAtMs <= 180_000L) {
            val predicted = java.util.Calendar.getInstance().apply { timeInMillis = nowMs + minutes * 60_000L }
            val time = String.format(Locale.forLanguageTag("pt-BR"), "%02d:%02d",
                predicted.get(java.util.Calendar.HOUR_OF_DAY), predicted.get(java.util.Calendar.MINUTE))
            val day = if (java.text.SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(java.util.Date(nowMs)) !=
                java.text.SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(java.util.Date(predicted.timeInMillis))) " (outro dia)" else ""
            return if (mode == "Carregando") "100% previsto: ~$time$day"
                else "Término previsto: ~$time$day"
        }
        return if (mode in setOf("Carregando", "Descarregando")) "Previsão indisponível" else mode.ifBlank { "Estado indisponível" }
    }

    /** 'Recente' significa somente resposta anterior válida, jamais socket permanente. */
    fun remoteLine(name: String?, lastSuccessMs: Long, nowMs: Long): String {
        if (name == null) return "Remoto: nenhum aparelho selecionado"
        val title = name.trim().take(24).ifBlank { "pareado" }
        if (lastSuccessMs <= 0L || lastSuccessMs > nowMs) return "Remoto ($title): sem resposta registrada"
        val age = nowMs - lastSuccessMs
        val time = when {
            age < 60_000L -> "há menos de 1 min"
            age < 3_600_000L -> "há ${age / 60_000L} min"
            age < 86_400_000L -> "há ${age / 3_600_000L} h"
            else -> "há ${age / 86_400_000L} dia(s)"
        }
        return "Remoto ($title): última resposta $time"
    }
}
