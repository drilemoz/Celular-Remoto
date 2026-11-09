package br.dev.celularremoto.core

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/** Dados mínimos da última bateria autenticada. Sem nome, IP, conteúdo ou chave em disco. */
object ControllerRemoteTelemetry {
    private const val FILE = "remote_battery_last_140"
    private const val FRESH_MS = 180_000L
    private const val KEEP_MS = 86_400_000L
    private data class Reading(val scope: String, val receivedMs: Long, val battery: JSONObject)
    @Volatile private var reading: Reading? = null
    @Volatile private var recentFailure: String = ""

    /** Chamado apenas após decodificação AES-GCM e validação de host/chave do controlador. */
    @Synchronized fun onSnapshot(context: Context, host: String, snapshot: JSONObject) {
        if (host.isBlank() || host != SettingsStore.getRemoteHost(context)) return
        val source = snapshot.optJSONObject("battery") ?: return
        val key = RecentDevices.key(context, host) ?: return
        val scope = RemoteEventNoticePolicy.pairingScope(host, key)
        val battery = JSONObject()
        for (field in listOf("levelPercent", "temperatureC", "currentMa", "state")) {
            if (source.has(field)) battery.put(field, source.get(field))
        }
        if (!battery.has("levelPercent") || battery.isNull("levelPercent") ||
            battery.optInt("levelPercent", -1) !in 0..100) return
        val at = System.currentTimeMillis()
        reading = Reading(scope, at, battery)
        recentFailure = ""
        // Sem opt-in contínuo: manter apenas RAM como nas versões anteriores.
        if (!SettingsStore.controllerBatteryPolling(context)) return
        // A UI pode consultar a cada 15 s; persistir no máximo uma vez por minuto.
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val lastPersisted = prefs.getLong("received", 0L)
        if (prefs.getString("scope", "") != scope || at - lastPersisted >= 60_000L || at < lastPersisted) {
            prefs.edit().putString("scope", scope).putLong("received", at)
                .putString("battery", battery.toString()).apply()
        }
    }

    /** Motivos de falha técnicos genéricos, sem IP ou dados particulares em armazenamento. */
    fun failed(reason: String) { recentFailure = reason.take(60) }

    @Synchronized private fun current(context: Context): Reading? {
        val host = SettingsStore.getRemoteHost(context)
        val key = RecentDevices.key(context, host) ?: return null
        val scope = RemoteEventNoticePolicy.pairingScope(host, key)
        val memory = reading
        if (memory != null && memory.scope == scope) return memory
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getString("scope", "") != scope) {
            prefs.edit().clear().apply() // não reter dados da credencial anterior
            return null
        }
        val at = prefs.getLong("received", 0L)
        if (at <= 0 || at > System.currentTimeMillis() || System.currentTimeMillis() - at > KEEP_MS) {
            prefs.edit().clear().apply()
            return null
        }
        val battery = runCatching { JSONObject(prefs.getString("battery", "{}")) }.getOrNull() ?: return null
        return Reading(scope, at, battery).also { reading = it }
    }

    private fun age(ms: Long): String = when {
        ms < 60_000 -> "há ${ms / 1000}s"
        ms < 3_600_000 -> "há ${ms / 60_000}min"
        else -> "há ${ms / 3_600_000}h"
    }

    fun status(context: Context): String {
        val data = current(context)
        val at = data?.receivedMs ?: 0L
        val now = System.currentTimeMillis()
        val detail = if (at <= 0 || at > now || now - at > KEEP_MS) "Sem leitura salva"
            else "Última resposta ${age(now - at)}"
        return if (recentFailure.isNotBlank()) "$detail · ${recentFailure}" else detail
    }

    fun line(context: Context, nowMs: Long = System.currentTimeMillis()): String {
        val data = current(context) ?: return "Remoto: sem leitura recente"
        val ms = nowMs - data.receivedMs
        if (data.receivedMs <= 0 || ms < 0 || ms > KEEP_MS) return "Remoto: sem leitura recente"
        // Nunca exibir valores antigos como atuais, ainda que sobrevivam ao restart.
        if (ms > FRESH_MS) return "Remoto: leitura desatualizada (${age(ms)})"
        val b = data.battery
        val parts = mutableListOf<String>()
        if (!b.isNull("levelPercent"))
            b.optInt("levelPercent", -1).takeIf { it in 0..100 }?.let { parts.add("$it%") }
        if (!b.isNull("temperatureC"))
            b.optDouble("temperatureC", Double.NaN).takeIf { it.isFinite() && it in -20.0..95.0 }
                ?.let { parts.add(String.format(Locale.forLanguageTag("pt-BR"), "%.1f °C", it)) }
        val currentMa = b.optDouble("currentMa", Double.NaN)
        val directionValid = when (b.optString("state")) {
            "Carregando" -> currentMa > 0
            "Descarregando" -> currentMa < 0
            else -> false
        }
        if (!b.isNull("currentMa") && currentMa.isFinite() && currentMa in -30_000.0..30_000.0 && directionValid)
            parts.add(String.format(Locale.forLanguageTag("pt-BR"), "%+.0f mA", currentMa))
        return "Remoto: " + parts.joinToString(" · ").ifBlank { "sem dados de bateria" } + " · ${age(ms)}"
    }
}
