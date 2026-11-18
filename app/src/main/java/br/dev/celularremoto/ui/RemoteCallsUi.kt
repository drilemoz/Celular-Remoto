package br.dev.celularremoto.ui

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.CallLogSharing
import br.dev.celularremoto.core.CallNoticePolicy
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.RemoteEventNoticePolicy
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.transport.RemoteClient
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date
import kotlin.concurrent.thread

internal fun MainActivity.noticeMatchesSelectedPair(intent: Intent): Boolean {
    val expected = intent.getStringExtra(MainActivity.EXTRA_NOTICE_SCOPE) ?: return true
    val host = SettingsStore.getRemoteHost(this)
    val key = if (host.isNotBlank()) RecentDevices.key(this, host) else null
    return key != null && RemoteEventNoticePolicy.pairingScope(host, key) == expected
}

internal fun MainActivity.consumeCallNotice(intent: Intent) {
    if (!active || !intent.getBooleanExtra(MainActivity.EXTRA_OPEN_CALLS, false)) return
    intent.removeExtra(MainActivity.EXTRA_OPEN_CALLS)
    if (intent.getStringExtra(MainActivity.EXTRA_NOTICE_SCOPE).isNullOrBlank()) return
    if (!noticeMatchesSelectedPair(intent)) {
        message("Este aviso é de outro pareamento. Nenhum registro foi aberto.")
        return
    }
    if (mode != 1) navigateMainTab(1)
    showRemoteCallHistory()
}

internal fun MainActivity.showRemoteCallHistory() {
    val host = SettingsStore.getRemoteHost(this)
    val key = if (host.isNotBlank()) RecentDevices.key(this, host) else null
    if (key == null) {
        message("Selecione um aparelho pareado na aba Controlar.")
        return
    }
    val app = applicationContext
    val (scroll, body) = dialogWrapper()
    val dialog = themedPopup(this).setTitle("Chamadas do aparelho remoto").setView(scroll)
        .setNeutralButton("Atualizar", null)
        .setPositiveButton("Fechar", null).create()
    remoteCallsDialog = dialog
    dialog.setOnDismissListener { if (remoteCallsDialog === dialog) remoteCallsDialog = null }
    showManagedDialog(dialog)
    val refresh = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
    val dateTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    fun load() {
        if (!active || !dialog.isShowing || !refresh.isEnabled) return
        refresh.isEnabled = false
        body.removeAllViews()
        body.addView(label("Consultando aparelho pareado…", 13f, muted = true))
        thread(name = "CelularRemoto-call-history") {
            val result = runCatching { RemoteClient.callHistory(host, key, app) }
            handler.post {
                if (!active || isFinishing || isDestroyed || !dialog.isShowing || remoteCallsDialog !== dialog) return@post
                val samePair = SettingsStore.getRemoteHost(app) == host &&
                    RecentDevices.key(app, host)?.let { MessageDigest.isEqual(it, key) } == true
                if (!samePair) {
                    dialog.dismiss()
                    message("O pareamento mudou. Abra Chamadas novamente.")
                    return@post
                }
                refresh.isEnabled = true
                body.removeAllViews()
                val reply = result.getOrNull()
                if (reply?.optBoolean("ok") != true) {
                    val why = when (reply?.optString("reason")) {
                        "SHARING_OFF" -> "No outro celular, abra Ajustes → SMS e telefonia → Compartilhar registro de chamadas."
                        "PERMISSION" -> "O Android do outro celular não concedeu acesso ao registro de chamadas."
                        "MONITOR_OFF" -> "Ative Monitorado no outro celular."
                        "UNAVAILABLE" -> "O Android do outro celular não retornou o registro de chamadas."
                        else -> "Sem resposta do aparelho. Confira a conexão e a versão do Celular Remoto nos dois celulares."
                    }
                    body.addView(label(why, 13f, muted = true))
                    return@post
                }
                val items = reply.optJSONArray("items")
                var shown = 0
                for (index in 0 until minOf(items?.length() ?: 0, 30)) {
                    val entry = items?.optJSONObject(index) ?: continue
                    val kind = CallNoticePolicy.label(entry.optString("kind")) ?: continue
                    val whenMs = entry.optLong("whenMs", 0L)
                    if (whenMs <= 0L) continue
                    val number = if (entry.isNull("number")) null else
                        CallNoticePolicy.visibleNumber(1, entry.optString("number"))
                    val details = listOfNotNull(dateTime.format(Date(whenMs)),
                        entry.optLong("durationSeconds", 0L).takeIf { it > 0 }?.let {
                            if (it < 60) "${it}s" else "${it / 60}min ${it % 60}s"
                        }).joinToString(" · ")
                    if (shown > 0) sectionGap(body, 6)
                    body.addView(dialogCardRow(kind, details + "\n" + (number ?: "Número não disponível"), ""))
                    shown++
                }
                if (shown == 0) body.addView(label("Nenhuma chamada disponível no aparelho.", 13f, muted = true))
            }
        }
    }
    refresh.setOnClickListener { load() }
    load()
}

internal fun MainActivity.addCallSharingSetting(layout: LinearLayout) {
    val enabled = SettingsStore.shareCallHistory(this)
    val hasPermission = CallLogSharing.hasPermission(this)
    val status = when {
        enabled && hasPermission -> "Registro e avisos liberados para aparelhos pareados"
        enabled -> "Acesso ao registro bloqueado pelo Android"
        else -> "Bloqueado neste aparelho"
    }
    layout.addView(iconMenuRow(R.drawable.ic_phone, "Compartilhar registro de chamadas", status,
        badge = SettingsBadgeKind.forEnabled(enabled && hasPermission)) {
        if (enabled) {
            setLocalCallSharing(false)
        } else showAppConfirm("Compartilhar chamadas com o controlador?",
            "Aparelhos pareados poderão consultar até 30 chamadas deste celular. O aviso de uma nova chamada não mostra número, nome ou áudio. É possível desativar aqui.",
            R.drawable.ic_phone, "Autorizar") {
            if (CallLogSharing.hasPermission(this)) setLocalCallSharing(true)
            else requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), callLogPermissionRequest)
        }
    })
    sectionGap(layout, 8)
}

internal fun MainActivity.setLocalCallSharing(enabled: Boolean) {
    SettingsStore.setShareCallHistory(this, enabled)
    if (MonitorService.isServiceActive) runCatching { MonitorService.refreshModes(this) }
    if (mode == 3) render()
}

internal fun MainActivity.toggleLiveNoticeMode() {
    if (SettingsStore.controllerLiveAlerts(this)) {
        SettingsStore.setControllerLiveAlerts(this, false)
        runCatching { MonitorService.refreshModes(this) }
        render()
        return
    }
    val host = SettingsStore.getRemoteHost(this)
    if (host.isBlank() || RecentDevices.key(this, host) == null) {
        message("Pareie o outro celular na aba Controlar antes de ativar os avisos.")
        return
    }
    showAppConfirm("Ativar avisos em tempo real?",
        "Receba avisos de chamadas e SMS autorizados com a tela apagada. O Celular Remoto mantém uma notificação de serviço e precisa alcançar o aparelho pareado pela rede. Você pode parar o modo a qualquer momento.",
        R.drawable.ic_bell, "Ativar") {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), liveNoticePermissionRequest)
        } else enableLiveNoticeMode()
    }
}

internal fun MainActivity.enableLiveNoticeMode() {
    if (!active) { pendingLiveNoticeEnable = true; return }
    val host = SettingsStore.getRemoteHost(this)
    if (host.isBlank() || RecentDevices.key(this, host) == null) return
    if (getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() != true) {
        message("Permita notificações do Celular Remoto nos Ajustes do Android.")
        return
    }
    val alreadyEnabled = SettingsStore.controllerNotificationAlerts(this)
    SettingsStore.setControllerNotificationAlerts(this, true)
    SettingsStore.setControllerLiveAlerts(this, true)
    runCatching { MonitorService.refreshModes(this) }.onFailure {
        SettingsStore.setControllerLiveAlerts(this, false)
        if (!alreadyEnabled) SettingsStore.setControllerNotificationAlerts(this, false)
        message("O Android não permitiu iniciar os avisos. Tente novamente com o aplicativo aberto.")
    }
    eventWatcher.reconsider()
    render()
}

internal fun MainActivity.resumeRemoteNoticeActions() {
    if (pendingLiveNoticeEnable) {
        pendingLiveNoticeEnable = false
        enableLiveNoticeMode()
    }
    consumeCallNotice(intent)
}

internal fun MainActivity.onRemoteNoticePermission(requestCode: Int, granted: Boolean) {
    when (requestCode) {
        callLogPermissionRequest -> {
            setLocalCallSharing(granted)
            if (!granted) message("O Android não liberou o registro de chamadas. O instalador pode restringir essa permissão.")
        }
        liveNoticePermissionRequest -> {
            if (granted) enableLiveNoticeMode()
            else message("Sem permissão para notificações, os avisos em tempo real não foram ativados.")
        }
    }
}
