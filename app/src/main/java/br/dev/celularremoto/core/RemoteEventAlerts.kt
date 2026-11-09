package br.dev.celularremoto.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.dev.celularremoto.R
import br.dev.celularremoto.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject

/** Avisos sem conteúdo de mensagens. A lista vem somente de snapshot cifrado autenticado.
 * No segundo plano a consulta é periódica, não é um push e NÃO garante imediatismo.
 */
object RemoteEventAlerts {
    private const val STORE = "remote_event_cursor_v1"
    private const val CHANNEL = "remote_messages_notice"
    private const val MAX_NOTICES_PER_POLL = 3

    /** Reativação dos avisos estabelece uma nova linha de base, sem replay. */
    @Synchronized fun resetCursors(context: Context) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Synchronized fun onSnapshot(context: Context, host: String, snapshot: JSONObject) {
        if (host.isBlank() || host != SettingsStore.getRemoteHost(context) ||
            !SettingsStore.controllerNotificationAlerts(context)) return
        val epoch = snapshot.optString("epoch").takeIf { it.isNotBlank() } ?: return
        val key = RecentDevices.key(context, host) ?: return
        val identity = RemoteEventNoticePolicy.pairingScope(host, key)
        val smsAlerts = SettingsStore.controllerSmsAlerts(context)
        val whatsappAlerts = SettingsStore.controllerWhatsappAlerts(context)
        val otherAlerts = SettingsStore.controllerOtherNotificationAlerts(context)
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
                if (!RemoteEventNoticePolicy.recentArrival(at, now)) {
                    cursor = maxOf(cursor, seq) // não reapresentar avisos antigos
                    continue
                }
                val category = RemoteEventNoticePolicy.classify(kind, item.optString("app"))
                if (!RemoteEventNoticePolicy.enabled(category, smsAlerts, whatsappAlerts, otherAlerts)) {
                    cursor = maxOf(cursor, seq) // Nunca reapresentar eventos anteriores à ativação do filtro.
                    continue
                }
                if (sent >= MAX_NOTICES_PER_POLL) break
                val title = when (category) {
                    RemoteEventNoticePolicy.Category.SMS -> "Novo SMS no aparelho remoto"
                    RemoteEventNoticePolicy.Category.WHATSAPP -> "Nova notificação do WhatsApp"
                    RemoteEventNoticePolicy.Category.OTHER -> "Notificação do aparelho remoto"
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
                    .setContentText(RemoteEventNoticePolicy.ageLabel(at, now))
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
