package br.dev.celularremoto.core

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import br.dev.celularremoto.R
import br.dev.celularremoto.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject

/** Alertas de metas por aparelho. Não desativa a proteção térmica padrão do Android/Monitorado.
 * Exige transição observada (nunca notifica baseado em valor antigo de primeiro acesso).
 */
object BatteryGoalAlerts {
    private const val PREF = "celular_battery_alerts_v1"
    private const val CHANNEL = "celular_battery_goals"
    private const val MAX_EVENTS = 16
    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    @Synchronized fun configure(context: Context, goal: Int, low: Int, hot: Int) {
        require(goal in 50..100 && low in 5..40 && hot in 38..55) { "Limites de alerta inválidos." }
        val p = prefs(context)
        p.edit().putInt("goal", goal).putInt("low", low).putInt("hot", hot)
            // Para evitar disparar um alerta retroativo depois de alterar os limites.
            .putLong("lastObservedAt", 0L).apply()
    }

    private fun notifyOwner(context: Context, kind: String, message: String): AlertDeliveryPolicy.Result {
        // Na temperatura padrão de 43 °C, HealthAlertMonitor já notifica esse mesmo fato.
        if (kind == "HOT" && prefs(context).getInt("hot", 43) == 43)
            return AlertDeliveryPolicy.Result.SUPPRESSED
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Celular Remoto · Bateria").setContentText(message.take(125))
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return AlertDelivery.publish(context, CHANNEL, "Metas e avisos da bateria", "LOCAL_GOAL",
            395 + when (kind) { "GOAL" -> 0; "LOW" -> 1; else -> 2 }, note)
    }

    /** Chamado pelo serviço já existente; nenhum outro serviço ou receiver é necessário. */
    @Synchronized fun check(context: Context) {
        val raw = BatteryReader.snapshot(context)
        val percent = raw.optInt("levelPercent", -1).takeIf { it in 0..100 } ?: return
        val temperature = raw.optDouble("temperatureC", Double.NaN).takeIf { it.isFinite() && it in -30.0..100.0 }
        val p = prefs(context)
        val previousAt = p.getLong("lastObservedAt", 0L)
        val previousPercent = p.getInt("lastPercent", -1)
        val previousTemp = p.getFloat("lastTemp", Float.NaN).toDouble()
        val goal = p.getInt("goal", 80)
        val low = p.getInt("low", 20)
        val hot = p.getInt("hot", 43)
        val now = System.currentTimeMillis()
        val events = runCatching { JSONArray(p.getString("events", "[]")) }.getOrElse { JSONArray() }
        var sequence = p.getLong("sequence", 0L)
        val newEvents = mutableListOf<Pair<String, String>>()
        if (previousAt > 0L && now >= previousAt && now - previousAt <= 30L * 60_000L) {
            BatteryGoalPolicy.crossings(previousPercent, percent,
                previousTemp.takeIf { it.isFinite() }, temperature,
                raw.optInt("pluggedCode", -1) > 0, goal, low, hot).forEach { event ->
                val text = when (event) {
                    "GOAL" -> "Meta de carga atingida: $percent% (meta $goal%)"
                    "LOW" -> "Bateria baixa: $percent% (limite $low%)"
                    else -> "Temperatura da bateria: %.1f °C (limite %d °C)".format(temperature, hot)
                }
                newEvents.add(event to text)
            }
        }
        newEvents.forEach { (kind, message) ->
            sequence++
            events.put(JSONObject().put("seq", sequence).put("at", now)
                .put("kind", kind).put("message", message).put("posted", false))
        }
        // A notificação bloqueada fica pendente apenas por 24h; sem duplicar o evento.
        for (index in 0 until events.length()) {
            val item = events.optJSONObject(index) ?: continue
            if (!item.has("posted") || item.optBoolean("posted") ||
                !AlertDeliveryPolicy.retryAllowed(item.optLong("at"), now, false)) continue
            if (!AlertDeliveryPolicy.goalStillRelevant(item.optString("kind"), percent,
                    temperature, goal, low, hot)) {
                item.put("posted", true).put("resolved", true)
                continue
            }
            if (!SettingsStore.localHealthAlerts(context)) continue
            val delivery = notifyOwner(context, item.optString("kind"), item.optString("message"))
            if (AlertDeliveryPolicy.acknowledged(delivery)) item.put("posted", true)
        }
        val limited = JSONArray()
        for (index in (events.length() - MAX_EVENTS).coerceAtLeast(0) until events.length())
            limited.put(events.get(index))
        p.edit().putLong("lastObservedAt", now).putInt("lastPercent", percent)
            .putFloat("lastTemp", temperature?.toFloat() ?: Float.NaN)
            .putLong("sequence", sequence).putString("events", limited.toString()).apply()
    }

    @Synchronized fun snapshot(context: Context): JSONObject {
        val p = prefs(context)
        val events = runCatching { JSONArray(p.getString("events", "[]")) }.getOrElse { JSONArray() }
        return JSONObject().put("goalPercent", p.getInt("goal", 80))
            .put("lowPercent", p.getInt("low", 20)).put("hotC", p.getInt("hot", 43))
            .put("eventSequence", p.getLong("sequence", 0L))
            .put("events", events)
    }
}
