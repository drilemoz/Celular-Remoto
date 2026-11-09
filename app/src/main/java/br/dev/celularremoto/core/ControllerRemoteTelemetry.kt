package br.dev.celularremoto.core

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/** Última telemetria autenticada em RAM, sem guardar nome ou conteúdo de mensagens. */
object ControllerRemoteTelemetry {
    private data class Reading(val host: String, val receivedMs: Long, val battery: JSONObject)
    @Volatile private var reading: Reading? = null

    @Synchronized fun onSnapshot(context: Context, host: String, snapshot: JSONObject) {
        if (host.isBlank() || host != SettingsStore.getRemoteHost(context)) return
        val battery = snapshot.optJSONObject("battery") ?: return
        reading = Reading(host, System.currentTimeMillis(), JSONObject(battery.toString()))
    }

    fun line(context: Context, nowMs: Long = System.currentTimeMillis()): String {
        val data = reading ?: return "Remoto: sem leitura recente"
        if (data.host != SettingsStore.getRemoteHost(context) || data.receivedMs > nowMs ||
            nowMs - data.receivedMs > 180_000L) return "Remoto: sem leitura recente"
        val b = data.battery
        val parts = mutableListOf<String>()
        if (b.has("levelPercent") && !b.isNull("levelPercent"))
            b.optInt("levelPercent", -1).takeIf { it in 0..100 }?.let { parts.add("$it%") }
        if (b.has("temperatureC") && !b.isNull("temperatureC"))
            b.optDouble("temperatureC", Double.NaN).takeIf { it.isFinite() && it in -20.0..95.0 }
                ?.let { parts.add(String.format(Locale.forLanguageTag("pt-BR"), "%.1f °C", it)) }
        if (b.has("currentMa") && !b.isNull("currentMa"))
            b.optDouble("currentMa", Double.NaN).takeIf { it.isFinite() && it in -30_000.0..30_000.0 }
                ?.let { parts.add(String.format(Locale.forLanguageTag("pt-BR"), "%+.0f mA", it)) }
        return "Remoto: " + parts.joinToString(" · ").ifBlank { "sem dados de bateria" }
    }
}
