package br.dev.celularremoto.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.RemotePreparation
import br.dev.celularremoto.core.RemotePreparationPolicy
import br.dev.celularremoto.updates.UpdatePreferences
import br.dev.celularremoto.updates.UpdateScheduler
import org.json.JSONObject

/** Preparação local: orientação e navegação, SEM solicitar permissão ou iniciar captura ao entrar. */
internal fun MainActivity.renderRemotePreparation(layout: LinearLayout) {
    val snapshot = RemotePreparation.snapshot(this)
    val total = snapshot.optInt("total")
    val ready = snapshot.optInt("readyCount")
    val needsSetup = snapshot.optInt("setupCount")
    layout.addView(settingsSummaryBand("Preparar aparelho monitorado",
        "$ready/$total estados prontos · $needsSetup requerem configuração · demais são opcionais ou por sessão",
        R.drawable.ic_shield))
    sectionGap(layout, 8)
    layout.addView(label("Esta tela informa o que está disponível neste celular. " +
        "Nenhuma permissão ou função é habilitada automaticamente. " +
        "Aparelhos pareados podem consultar apenas esses estados genéricos.", 11.5f, muted = true))
    sectionGap(layout, 9)
    settingsActionPair(layout, "Central de permissões", { openPermissionCenter() },
        "Revisar atualizações", { openSettingsSection("updates") }, emphasizeFirst = true)
    sectionGap(layout, 8)
    val enabled = UpdatePreferences.remoteRequests(this)
    layout.addView(settingsToggleRow(R.drawable.ic_folder_download,
        "Aceitar verificação/download remotos",
        "Somente controlador pareado; nunca instala APK sem confirmação local", enabled) {
        if (enabled) {
            UpdatePreferences.update(this, "remote_requests", false)
            UpdateScheduler.refresh(this)
            openSettingsSection("preparation")
        } else {
            showAppConfirm("Autorizar pedidos de atualização?",
                "Um controlador com chave válida poderá solicitar consulta e download do APK. " +
                    "O Android continuará exigindo sua ação neste aparelho para instalar.",
                R.drawable.ic_shield, "Autorizar") {
                UpdatePreferences.update(this, "remote_requests", true)
                UpdateScheduler.refresh(this)
                openSettingsSection("preparation")
            }
        }
    })
    sectionGap(layout, 7)
    layout.addView(settingsSlimRow(R.drawable.ic_folder_download,
        "Permissão do instalador Android",
        if (RemotePreparation.installerAuthorized(this))
            "Autorizada para solicitar instalação; confirmação local continua obrigatória"
        else "Pendente: permita instalar apps desta origem nas Configurações Android",
        if (RemotePreparation.installerAuthorized(this)) "OK" else "Revisar") {
        showAppConfirm("Autorizar origem de instalação?",
            "O Celular Remoto poderá abrir o instalador oficial para APKs verificados. " +
                "Nenhuma atualização será instalada automaticamente.",
            R.drawable.ic_shield, "Abrir Android") {
            try {
                if (Build.VERSION.SDK_INT >= 26) startActivity(Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")))
                else openAppPermissionSettings()
            } catch (_: Exception) { openAppPermissionSettings() }
        }
    })
    sectionGap(layout, 12)
    layout.addView(settingsSubheading("Prontidão dos comandos"))
    val options = snapshot.optJSONArray("items")
    if (options != null) for (i in 0 until options.length()) {
        val row = options.optJSONObject(i) ?: continue
        val title = row.optString("title")
        val state = row.optString("state")
        val note = row.optString("note")
        val section = when (row.optString("id")) {
            "monitor" -> "service"
            "alerts" -> "health"
            "update_requests", "installer" -> "updates"
            "location" -> "location"
            "screen", "touch" -> "screen"
            "camera", "photo" -> "physical"
            "sound", "torch" -> "physical"
            "lock" -> "lock"
            "apps" -> "apps"
            "files" -> "files"
            "sms" -> "sms"
            else -> "permissions"
        }
        layout.addView(settingsSlimRow(
            if (state == RemotePreparationPolicy.READY) R.drawable.ic_shield else R.drawable.ic_activity,
            title, note, RemotePreparationPolicy.status(state)) { openSettingsSection(section) })
        if (i != options.length() - 1) compactDivider(layout, 32)
    }
    sectionGap(layout, 10)
    layout.addView(label("'Pronto' indica apenas que os pré-requisitos observáveis estão presentes. " +
        "O Android pode exigir nova confirmação para tela e câmera e restringir tarefas em segundo plano.",
        11f, muted = true))
    sectionGap(layout, 6)
    layout.addView(actionButton("Atualizar diagnóstico") { openSettingsSection("preparation") })
}

/** Uma resposta cifrada contém somente uma avaliação pontual; não promete conexão permanente. */
internal fun MainActivity.showRemotePreparationSummary(readiness: JSONObject, fromCache: Boolean) {
    val items = readiness.optJSONArray("items") ?: return
    val lines = mutableListOf<String>()
    for (i in 0 until items.length().coerceAtMost(24)) {
        val r = items.optJSONObject(i) ?: continue
        val state = RemotePreparationPolicy.status(r.optString("state"))
        lines += "${r.optString("title").take(64)}: $state — ${r.optString("note").take(160)}"
    }
    showAppNotice("Preparação do monitorado",
        (if (fromCache) "Última informação armazenada; confirme com uma nova consulta.\n\n" else "Resposta autenticada.\n\n") +
        lines.joinToString("\n\n"))
}
