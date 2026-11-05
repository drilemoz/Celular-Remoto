package br.dev.celularremoto.ui

import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.RemoteClient
import org.json.JSONObject
import br.dev.celularremoto.updates.UpdateController
import br.dev.celularremoto.updates.UpdatePreferences
import br.dev.celularremoto.updates.UpdateScheduler
import java.text.DateFormat
import java.util.Date
import kotlin.concurrent.thread

/** Atualizações disponíveis só na tela local; nenhum comando remoto inicia instalação. */
internal fun MainActivity.fillUpdatesSettings(layout: LinearLayout) {
    val status = UpdateController.state(this)
    layout.addView(settingsSummaryBand("Atualizações online",
        "Instalado: ${br.dev.celularremoto.transport.AppVersion.NAME} · $status",
        R.drawable.ic_folder_download))
    sectionGap(layout, 8)
    val last = UpdateController.lastCheck(this)
    val lastText = if (last > 0) DateFormat.getDateTimeInstance().format(Date(last)) else "Nunca"
    layout.addView(dialogMetricRow("Última consulta", lastText))
    val version = UpdateController.availableVersion(this)
    if (version.isNotBlank()) layout.addView(dialogMetricRow("Nova versão", version))
    UpdateController.downloadProgress(this)?.let {
        layout.addView(dialogMetricRow("Download", it))
    }
    sectionGap(layout, 8)
    fun verifyNow() {
        message("Consultando as Releases do GitHub…")
        thread(name = "CelularRemoto-update-manual") {
            val result = UpdateController.check(applicationContext, automatic = false)
            runOnUiThread { if (!isFinishing && !isDestroyed) {
                if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                message(result)
            } }
        }
    }
    if (UpdateController.prepared(this)) {
        settingsActionPair(layout, "Verificar agora", { verifyNow() }, "Instalar APK", {
            showAppConfirm("Instalar atualização do Celular Remoto?",
                "O Android pedirá sua confirmação. A instalação pode reiniciar o app e " +
                    "interromper uma medição ou conexão ativa.",
                R.drawable.ic_shield, "Abrir instalador") {
                message(UpdateController.installFromUserTap(this))
            }
        }, emphasizeFirst = true)
    } else if (version.isNotBlank() &&
        (UpdatePreferences.getString(this, "available_code").toIntOrNull() ?: 0) >
        br.dev.celularremoto.BuildConfig.VERSION_CODE &&
        UpdatePreferences.getLong(this, "download_id") == 0L) {
        settingsActionPair(layout, "Verificar agora", { verifyNow() }, "Baixar APK", {
            thread(name = "CelularRemoto-update-download") {
                val outcome = UpdateController.download(applicationContext)
                runOnUiThread { if (!isFinishing && !isDestroyed) {
                    if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                    message(outcome)
                } }
            }
        }, emphasizeFirst = true)
    } else {
        layout.addView(actionButton("Verificar agora", emphasis = true) { verifyNow() })
    }
    sectionGap(layout, 9)
    layout.addView(settingsSubheading("Automação neste aparelho"))
    fun toggle(key: String, title: String, hint: String, current: Boolean) {
        layout.addView(settingsToggleRow(R.drawable.ic_settings, title, hint, current) {
            UpdatePreferences.update(this, key, !current)
            UpdateScheduler.refresh(this)
            openSettingsSection("updates")
        })
        compactDivider(layout, 33)
    }
    toggle("checking", "Verificação automática", "Consultas sem abrir o aplicativo",
        UpdatePreferences.checking(this))
    toggle("auto_download", "Download automático", "Prepara o APK sem instalar",
        UpdatePreferences.autoDownload(this))
    toggle("unmetered", "Somente Wi-Fi não tarifado", "Não usar dados móveis",
        UpdatePreferences.unmetered(this))
    toggle("battery_care", "Economia de bateria", "Adiar em bateria baixa ou durante testes",
        UpdatePreferences.batteryCare(this))
    val minutes = UpdatePreferences.intervalMinutes(this)
    val choices = listOf(30, 60, 180, 360, 720, 1440)
    fun frequency(value: Int): String =
        if (value < 60) "$value minutos" else "${value / 60} hora${if (value == 60) "" else "s"}"
    layout.addView(settingsSlimRow(R.drawable.ic_activity, "Intervalo de consulta",
        "Aproximadamente a cada ${frequency(minutes)}", "Alterar") {
        showSettingsChoiceDialog("Periodicidade de verificação",
            "O Android pode atrasar a execução para economizar energia.",
            choices.map { frequency(it) to
                (if (it == 60) "Padrão recomendado" else "Intervalo aproximado") },
            choices.indexOf(minutes)) { which ->
            UpdatePreferences.interval(this, choices[which])
            UpdateScheduler.refresh(this)
            openSettingsSection("updates")
        }
    })
    sectionGap(layout, 11)
    layout.addView(settingsSubheading("Aparelhos pareados"))
    val requests = UpdatePreferences.remoteRequests(this)
    layout.addView(settingsToggleRow(R.drawable.ic_shield, "Aceitar pedidos remotos",
        "Permite consultar e baixar; nunca instalar remotamente", requests) {
        UpdatePreferences.update(this, "remote_requests", !requests)
        UpdateScheduler.refresh(this)
        openSettingsSection("updates")
    })
    compactDivider(layout, 33)
    val host = SettingsStore.getRemoteHost(this)
    val key = if (host.isNotBlank()) RecentDevices.key(this, host) else null
    if (key == null) {
        layout.addView(label("Selecione e pareie outro aparelho para consultar e solicitar a atualização dele.", 12f, muted = true))
    } else {
        layout.addView(dialogMetricRow("Aparelho selecionado", host))
        val selectedAtLastCheck = UpdatePreferences.getString(this, "last_remote_host") == host
        val remoteState = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_state") else ""
        val remoteProgress = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_progress") else ""
        val remoteVersion = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_version") else ""
        if (remoteVersion.isNotBlank()) layout.addView(dialogMetricRow("Versão remota consultada", remoteVersion))
        if (remoteState.isNotBlank()) layout.addView(dialogMetricRow("Atualizador remoto", remoteState))
        if (remoteProgress.isNotBlank()) layout.addView(dialogMetricRow("Transferência remota", remoteProgress))
        sectionGap(layout, 6)
        fun remoteRequest(action: String) {
            val accessMethod = remoteConnection.method
            // A chave é conferida novamente após a resposta para não misturar aparelhos.
            val targetHost = host
            val expectedKey = key.copyOf()
            message("Enviando solicitação autenticada ao aparelho remoto…")
            thread(name = "CelularRemoto-update-remote-request") {
                val result = runCatching {
                    RemoteClient.appUpdate(targetHost, expectedKey, applicationContext, accessMethod, action)
                }
                val unchanged = SettingsStore.getRemoteHost(applicationContext) == targetHost &&
                    RecentDevices.key(applicationContext, targetHost)?.contentEquals(expectedKey) == true
                if (unchanged && result.isSuccess) {
                    val reply = result.getOrThrow()
                    val data = reply.optJSONObject("data") ?: JSONObject()
                    UpdatePreferences.putString(applicationContext, "last_remote_host", targetHost)
                    UpdatePreferences.putString(applicationContext, "last_remote_version", data.optString("version"))
                    UpdatePreferences.putString(applicationContext, "last_remote_state", data.optString("state").take(160))
                    UpdatePreferences.putString(applicationContext, "last_remote_progress", data.optString("progress").take(80))
                }
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && unchanged) {
                        if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                        val reply = result.getOrNull()
                        message(if (reply != null) reply.optString("message", "Solicitação recebida")
                            else (result.exceptionOrNull()?.message ?: "Falha ao consultar o aparelho remoto"))
                    }
                }
            }
        }
        settingsActionPair(layout, "Consultar remoto", { remoteRequest("status") },
            "Verificar e baixar", {
                showAppConfirm("Solicitar atualização no remoto?",
                    "O aparelho pareado verificará as Releases e tentará baixar um APK " +
                        "respeitando suas opções de rede e bateria. A instalação permanece local.",
                    R.drawable.ic_shield, "Solicitar") { remoteRequest("checkDownload") }
            })
        sectionGap(layout, 5)
        layout.addView(label("Acompanhe o download usando Consultar remoto. Requer alpha.112+ no destino.", 11f, muted = true))
    }
    sectionGap(layout, 12)
    layout.addView(label("A instalação exige confirmação local. São aceitas apenas Releases verificadas e assinadas. Consultas periódicas podem atrasar no Android.", 11.5f, muted = true))
}
