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

    /** Título recolhido sem o nome do aplicativo: percentual + estado observado. */
    fun title(battery: Battery?): String {
        if (battery == null) return "Bateria · sem leitura"
        val percent = battery.percent?.takeIf { it in 0..100 }?.let { "$it%" } ?: "Bateria"
        return "$percent · ${battery.state.ifBlank { "estado indisponível" }}"
    }

    /** Sem previsões ou histórico de rede na notificação permanente. */
    fun batteryLine(battery: Battery?): String {
        if (battery == null) return "Este aparelho: sem leitura"
        val base = mutableListOf<String>()
        battery.percent?.takeIf { it in 0..100 }?.let { base.add("$it%") }
        battery.temperatureC?.takeIf { it.isFinite() && it in -20.0..95.0 }
            ?.let { base.add("${decimal(it)} °C") }
        battery.currentMa?.takeIf { it.isFinite() && it in -30_000.0..30_000.0 }
            ?.let { base.add("${if (it > 0.0) "+" else ""}${decimal(it, 0)} mA") }
        return "Este aparelho: " + base.joinToString(" · ").ifBlank { "sem dados de bateria" }
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
