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
import br.dev.celularremoto.ui.MainActivity

internal object ConnectionHealthNotifier {
    private const val CHANNEL = "connection_diagnostics"
    private const val NOTIFICATION_ID = 2771

    fun alert(context: Context, transition: String) {
        if (!ConnectionHealthStore.alertsEnabled(context)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            "Estabilidade da conexão", NotificationManager.IMPORTANCE_DEFAULT))
        val title = if (transition == "lost") "Celular Remoto: sem resposta" else "Celular Remoto: comunicação recuperada"
        val message = if (transition == "lost")
            "Três verificações autenticadas falharam. Abra o diagnóstico para conferir a rede."
        else "O aparelho pareado voltou a responder a uma verificação autenticada."
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_connection_diagnostics", true)
        }
        val pending = PendingIntent.getActivity(context, 2771, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title).setContentText(message).setContentIntent(pending)
            .setAutoCancel(true).build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
