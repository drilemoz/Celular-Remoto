package br.dev.celularremoto.core

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/** Estado e diagnóstico das tentativas de NOTIFICAR (não de leitura pelo usuário).
 * Registra apenas canal, origem genérica, instante e resultado. Nunca mensagens,
 * hosts, SMS, localização, IDs de aparelhos ou credenciais. */
object AlertDelivery {
    private const val PREF = "celular_alert_delivery_v1"
    private const val MAX_LOG = 48
    private const val DUPLICATE_WINDOW_MS = 10 * 60_000L
    private val channels = listOf(
        "monitor_health_safety" to "Saúde local",
        "celular_battery_goals" to "Metas locais",
        "monitor_health_remote" to "Saúde remota",
        "celular_battery_remote_events" to "Metas remotas",
        "connection_diagnostics" to "Estabilidade da conexão",
        "celular_alert_test" to "Testes manuais",
        "celular_remoto_updates" to "Atualizações"
    )

    fun canPublish(context: Context, channelId: String): AlertDeliveryPolicy.Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return AlertDeliveryPolicy.Result.PERMISSION_DENIED
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return AlertDeliveryPolicy.Result.SERVICE_UNAVAILABLE
        if (!manager.areNotificationsEnabled()) return AlertDeliveryPolicy.Result.APP_BLOCKED
        if (Build.VERSION.SDK_INT >= 26 &&
            manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE)
            return AlertDeliveryPolicy.Result.CHANNEL_BLOCKED
        return AlertDeliveryPolicy.Result.POSTED // somente pré-condições; não comprova publicação
    }

    @Synchronized fun publish(context: Context, channelId: String, channelName: String,
        source: String, notificationId: Int, note: Notification,
        importance: Int = NotificationManager.IMPORTANCE_HIGH): AlertDeliveryPolicy.Result {
        val result = try {
            val manager = context.getSystemService(NotificationManager::class.java)
                ?: return record(context, source, channelId, AlertDeliveryPolicy.Result.SERVICE_UNAVAILABLE)
            if (Build.VERSION.SDK_INT >= 26)
                manager.createNotificationChannel(NotificationChannel(channelId, channelName, importance))
            val availability = canPublish(context, channelId)
            if (availability != AlertDeliveryPolicy.Result.POSTED) availability
            else {
                manager.notify(notificationId, note)
                AlertDeliveryPolicy.Result.POSTED
            }
        } catch (_: Exception) { AlertDeliveryPolicy.Result.ERROR }
        return record(context, source, channelId, result)
    }

    @Synchronized fun record(context: Context, source: String, channel: String,
        result: AlertDeliveryPolicy.Result): AlertDeliveryPolicy.Result {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val history = runCatching { JSONArray(prefs.getString("history", "[]")) }.getOrElse { JSONArray() }
        val previous = history.optJSONObject(history.length() - 1)
        // Alertas bloqueados podem ser reavaliados a cada 30 s sem gravar centenas de linhas iguais.
        if (result != AlertDeliveryPolicy.Result.POSTED && previous != null &&
            previous.optString("source") == source && previous.optString("channel") == channel &&
            previous.optString("result") == result.name && now - previous.optLong("at") in 0..DUPLICATE_WINDOW_MS) return result
        history.put(JSONObject().put("source", source).put("channel", channel)
            .put("result", result.name).put("at", now))
        val bounded = JSONArray()
        for (i in (history.length() - MAX_LOG).coerceAtLeast(0) until history.length())
            bounded.put(history.optJSONObject(i))
        prefs.edit().putString("history", bounded.toString()).apply()
        return result
    }

    @Synchronized fun diagnostic(context: Context): JSONObject {
        val manager = context.getSystemService(NotificationManager::class.java)
        val rows = JSONArray()
        for ((id, name) in channels) {
            val state = canPublish(context, id)
            rows.put(JSONObject().put("name", name).put("id", id).put("status", state.name))
        }
        val history = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("history", "[]") ?: "[]"
        return JSONObject().put("appEnabled", manager?.areNotificationsEnabled() == true)
            .put("channels", rows)
            .put("history", runCatching { JSONArray(history) }.getOrElse { JSONArray() })
    }
}
