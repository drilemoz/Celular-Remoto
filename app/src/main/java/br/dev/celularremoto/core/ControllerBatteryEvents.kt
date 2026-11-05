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
import org.json.JSONObject
import java.security.MessageDigest

/** Deduplicação de eventos de metas por host pareado, sem armazenar credenciais do remoto. */
object ControllerBatteryEvents {
    private const val PREF = "celular_battery_remote_received"
    private const val CHANNEL = "celular_battery_remote_events"
    @Synchronized fun onReport(context: Context, host: String, result: JSONObject) {
        if (!SettingsStore.controllerHealthAlerts(context) || host.isBlank()) return
        val data = result.optJSONObject("goalAlerts") ?: result.optJSONObject("batteryGoalAlerts") ?: return
        val list = data.optJSONArray("events") ?: return
        val id = MessageDigest.getInstance("SHA-256").digest(host.toByteArray(Charsets.UTF_8))
            .take(10).joinToString("") { "%02x".format(it.toInt() and 255) }
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val lastSeq = p.getLong("seq_$id", 0L)
        val lastTime = p.getLong("time_$id", 0L)
        val newest = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
            .filter { it.optLong("seq") > 0L && it.optLong("at") > 0L }
            .sortedBy { it.optLong("seq") }
        val now = System.currentTimeMillis()
        var maxSeq = lastSeq
        var maxTime = lastTime
        for (item in newest) {
            val seq = item.optLong("seq")
            val at = item.optLong("at")
            if (seq <= lastSeq && at <= lastTime) continue
            if (at > now + 60_000L || now - at > 24L * 60L * 60_000L || at < lastTime) continue
            // Só confirmar a sequência depois que o Android aceitar a notificação.
            val name = RecentDevices.list(context).firstOrNull { it.host == host }?.name ?: "Aparelho remoto"
            val ago = ((now - at) / 60_000L).coerceAtLeast(0L)
            val text = item.optString("message", "Evento de bateria").take(100) +
                if (ago > 5L) " · há $ago min" else ""
            val launch = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val note = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_battery)
                .setContentTitle("$name · bateria").setContentText(text)
                .setContentIntent(launch).setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE).build()
            val delivery = AlertDelivery.publish(context, CHANNEL, "Metas e bateria de aparelhos pareados",
                "REMOTE_GOAL", 410 + ((id + seq).hashCode() and 511), note)
            if (!AlertDeliveryPolicy.acknowledged(delivery)) break
            maxSeq = maxOf(maxSeq, seq)
            maxTime = maxOf(maxTime, at)
        }
        if (maxSeq != lastSeq || maxTime != lastTime)
            p.edit().putLong("seq_$id", maxSeq).putLong("time_$id", maxTime).apply()
    }
}
