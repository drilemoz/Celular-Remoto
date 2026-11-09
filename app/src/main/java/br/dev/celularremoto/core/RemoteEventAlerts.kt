package br.dev.celularremoto.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.dev.celularremoto.R
import br.dev.celularremoto.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Avisos sem conteúdo de mensagens. A lista vem somente de snapshot cifrado autenticado.
 * No segundo plano a consulta é periódica, não é um push e NÃO garante imediatismo.
 */
object RemoteEventAlerts {
    private const val STORE = "remote_event_cursor_v1"
    private const val CHANNEL = "remote_messages_notice"
    private const val MAX_AGE_MS = 5 * 60_000L
    private const val MAX_NOTICES_PER_POLL = 3

    @Synchronized fun onSnapshot(context: Context, host: String, snapshot: JSONObject) {
        if (host.isBlank() || host != SettingsStore.getRemoteHost(context) ||
            !SettingsStore.controllerNotificationAlerts(context)) return
        val epoch = snapshot.optString("epoch").takeIf { it.isNotBlank() } ?: return
        val identity = MessageDigest.getInstance("SHA-256").digest(host.toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lists = listOf("notification" to snapshot.optJSONArray("events"),
            "sms" to snapshot.optJSONArray("smsAlertEvents"))
        for ((kind, array) in lists) {
            val list = array ?: JSONArray()
            val prefix = "${identity}_${kind}_"
            val currentEpoch = prefs.getString(prefix + "epoch", null)
            val maxId = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
                .filter { kind == "sms" || it.optString("type") == "notificacao" }
                .maxOfOrNull { it.optLong("id") } ?: 0L
            // Primeira observação ou reinício do monitorado: registra apenas a linha de base.
            // Não lança dezenas de notificações antigas na primeira conexão.
            if (currentEpoch != epoch) {
                prefs.edit().putString(prefix + "epoch", epoch).putLong(prefix + "cursor", maxId).apply()
                continue
            }
            var cursor = prefs.getLong(prefix + "cursor", 0L)
            var sent = 0
            val pending = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
                .filter { it.optLong("id") > cursor && (kind == "sms" || it.optString("type") == "notificacao") }
                .sortedBy { it.optLong("id") }
            for (item in pending) {
                val seq = item.optLong("id")
                val at = item.optLong("whenMs")
                if (at <= 0 || at > now + 60_000L || now - at > MAX_AGE_MS) {
                    cursor = maxOf(cursor, seq) // não reapresentar avisos antigos
                    continue
                }
                if (sent >= MAX_NOTICES_PER_POLL) break
                val isWhatsapp = kind == "notification" && item.optString("app") in
                    setOf("com.whatsapp", "com.whatsapp.w4b")
                val title = when {
                    kind == "sms" -> "Novo SMS no aparelho remoto"
                    isWhatsapp -> "Nova notificação do WhatsApp"
                    else -> "Notificação do aparelho remoto"
                }
                val intent = Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_MESSAGES, true)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(context,
                    (prefix + seq).hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val note = Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_shield)
                    .setContentTitle(title)
                    .setContentText("Toque para consultar no aparelho pareado")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .build()
                val delivery = AlertDelivery.publish(context, CHANNEL,
                    "Mensagens de aparelhos pareados", "REMOTE_MESSAGE",
                    (prefix + seq).hashCode(), note)
                if (!AlertDeliveryPolicy.acknowledged(delivery)) break
                cursor = seq
                sent++
            }
            prefs.edit().putLong(prefix + "cursor", cursor).apply()
        }
    }
}
