package br.dev.celularremoto.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.dev.celularremoto.R
import br.dev.celularremoto.ui.MainActivity
import org.json.JSONObject

/** Avisos genéricos de eventos autenticados. O mesmo cursor deduplica a espera ativa e a consulta periódica. */
object RemoteEventAlerts {
    private const val STORE = "remote_event_cursor_v1"
    private const val CHANNEL = "remote_messages_notice"
    private const val CALL_CHANNEL = "remote_calls_notice"
    private const val MAX_NOTICES_PER_POLL = 3

    /** Reativação dos avisos estabelece uma nova linha de base, sem replay. */
    @Synchronized fun resetCursors(context: Context) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Synchronized fun onSnapshot(context: Context, host: String, snapshot: JSONObject, expectedKey: ByteArray? = null) {
        if (host.isBlank() || host != SettingsStore.getRemoteHost(context) ||
            !SettingsStore.controllerNotificationAlerts(context)) return
        val epoch = snapshot.optString("epoch").takeIf { it.isNotBlank() } ?: return
        val key = RecentDevices.key(context, host) ?: return
        if (expectedKey != null && !java.security.MessageDigest.isEqual(key, expectedKey)) return
        val identity = RemoteEventNoticePolicy.pairingScope(host, key)
        val smsAlerts = SettingsStore.controllerSmsAlerts(context)
        val whatsappAlerts = SettingsStore.controllerWhatsappAlerts(context)
        val otherAlerts = SettingsStore.controllerOtherNotificationAlerts(context)
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lists = listOf("notification" to snapshot.optJSONArray("events"),
            "sms" to snapshot.optJSONArray("smsAlertEvents"),
            "call" to snapshot.optJSONArray("callAlertEvents"))
        for ((kind, array) in lists) {
            // Cliente antigo ou consulta sem opt-in: campo ausente não muda o cursor.
            val list = array ?: continue
            val prefix = "${identity}_${kind}_"
            val currentEpoch = prefs.getString(prefix + "epoch", null)
            val maxId = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
                .filter { kind != "notification" || it.optString("type") == "notificacao" }
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
                .filter { it.optLong("id") > cursor && (kind != "notification" || it.optString("type") == "notificacao") }
                .sortedBy { it.optLong("id") }
            for (item in pending) {
                val seq = item.optLong("id")
                val at = item.optLong("whenMs")
                if (!RemoteEventNoticePolicy.recentArrival(at, now) ||
                    (kind == "call" && at < SettingsStore.controllerCallAlertsSince(context))) {
                    cursor = maxOf(cursor, seq) // não reapresentar avisos antigos
                    continue
                }
                val category = RemoteEventNoticePolicy.classify(kind, item.optString("app"))
                if (!RemoteEventNoticePolicy.enabled(category, smsAlerts, whatsappAlerts, otherAlerts,
                        SettingsStore.controllerCallAlerts(context))) {
                    cursor = maxOf(cursor, seq) // Nunca reapresentar eventos anteriores à ativação do filtro.
                    continue
                }
                if (sent >= MAX_NOTICES_PER_POLL) break
                val isCall = category == RemoteEventNoticePolicy.Category.CALL
                val callTitle = if (isCall) CallNoticePolicy.label(item.optString("kind")) else null
                if (isCall && callTitle == null) { cursor = maxOf(cursor, seq); continue }
                val channel = if (isCall) CALL_CHANNEL else CHANNEL
                val title = when (category) {
                    RemoteEventNoticePolicy.Category.CALL -> "${callTitle} no aparelho remoto"
                    RemoteEventNoticePolicy.Category.SMS -> "Novo SMS no aparelho remoto"
                    RemoteEventNoticePolicy.Category.WHATSAPP -> "Nova notificação do WhatsApp"
                    RemoteEventNoticePolicy.Category.OTHER -> "Notificação do aparelho remoto"
                }
                val intent = Intent(context, MainActivity::class.java)
                    .putExtra(if (isCall) MainActivity.EXTRA_OPEN_CALLS else MainActivity.EXTRA_OPEN_MESSAGES, true)
                    .putExtra(MainActivity.EXTRA_NOTICE_SCOPE, identity)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(context,
                    (prefix + seq).hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val publicNote = Notification.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_stat_shield)
                    .setContentTitle("Celular Remoto")
                    .setContentText("Novo aviso do aparelho pareado")
                    .build()
                val note = Notification.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_stat_shield)
                    .setContentTitle(title)
                    .setContentText(RemoteEventNoticePolicy.ageLabel(at, now))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicNote)
                    .build()
                val delivery = AlertDelivery.publish(context, channel,
                    if (isCall) "Chamadas do aparelho pareado" else "Mensagens de aparelhos pareados",
                    if (isCall) "REMOTE_CALL" else "REMOTE_MESSAGE",
                    (prefix + seq).hashCode(), note)
                if (!AlertDeliveryPolicy.acknowledged(delivery)) break
                cursor = seq
                sent++
            }
            prefs.edit().putLong(prefix + "cursor", cursor).apply()
        }
    }
}
