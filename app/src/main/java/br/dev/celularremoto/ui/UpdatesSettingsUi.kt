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
    layout.addView(iconSectionHeader(R.drawable.ic_folder_download, "Atualizações do aplicativo", "GitHub Releases · Celular Remoto"))
    sectionGap(layout, 8)
    dialogIntro(layout, "Verificação online em segundo plano. Downloads somente de Releases assinadas, conferidos por SHA-256 e certificado Android. A instalação exige confirmação neste aparelho.")
    sectionGap(layout, 8)
    layout.addView(dialogMetricRow("Versão instalada", br.dev.celularremoto.transport.AppVersion.NAME))
    layout.addView(dialogMetricRow("Estado", UpdateController.state(this)))
    UpdateController.downloadProgress(this)?.let { layout.addView(dialogMetricRow("Transferência", it)) }
    val time = UpdateController.lastCheck(this)
    layout.addView(dialogMetricRow("Última consulta", if (time > 0) DateFormat.getDateTimeInstance().format(Date(time)) else "Nunca"))
    val version = UpdateController.availableVersion(this)
    if (version.isNotBlank()) layout.addView(dialogMetricRow("Última encontrada", version))
    sectionGap(layout, 12)
    layout.addView(actionButton("Verificar atualização agora") {
        message("Consultando as Releases do GitHub…")
        thread(name = "CelularRemoto-update-manual") {
            val result = UpdateController.check(applicationContext, automatic = false)
            runOnUiThread { if (!isFinishing && !isDestroyed) {
                if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                message(result)
            } }
        }
    })
    sectionGap(layout, 8)
    if (UpdateController.prepared(this)) {
        layout.addView(actionButton("Instalar APK verificado") {
            showAppConfirm("Instalar atualização do Celular Remoto?",
                "O Android pedirá sua confirmação. A instalação pode reiniciar o aplicativo e interromper a conexão remota e medições em andamento. Faça isso em um momento seguro.",
                R.drawable.ic_shield, "Abrir instalador") {
                message(UpdateController.installFromUserTap(this))
            }
        })
        sectionGap(layout, 8)
    } else if (version.isNotBlank() &&
        (UpdatePreferences.getString(this, "available_code").toIntOrNull() ?: 0) >
            br.dev.celularremoto.BuildConfig.VERSION_CODE &&
        UpdatePreferences.getLong(this, "download_id") == 0L) {
        layout.addView(actionButton("Baixar APK agora") {
            thread(name = "CelularRemoto-update-download") {
                val outcome = UpdateController.download(applicationContext)
                runOnUiThread { if (!isFinishing && !isDestroyed) {
                    if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                    message(outcome)
                } }
            }
        })
        sectionGap(layout, 8)
    }
    fun toggle(key: String, title: String, hint: String, current: Boolean) {
        layout.addView(iconMenuRow(R.drawable.ic_settings, title,
            "${if (current) "Ativado" else "Desativado"} · $hint",
            badge = SettingsBadgeKind.forEnabled(current)) {
            UpdatePreferences.update(this, key, !current)
            UpdateScheduler.refresh(this)
            openSettingsSection("updates")
        })
        sectionGap(layout, 9)
    }
    toggle("checking", "Verificação automática", "Sem abrir o aplicativo", UpdatePreferences.checking(this))
    toggle("auto_download", "Download automático", "Prepara o APK sem instalar", UpdatePreferences.autoDownload(this))
    toggle("unmetered", "Somente rede não tarifada", "Evita consumo de dados móveis", UpdatePreferences.unmetered(this))
    toggle("battery_care", "Economia de energia", "Adia download abaixo de 25% e durante testes", UpdatePreferences.batteryCare(this))
    val minutes = UpdatePreferences.intervalMinutes(this)
    val choices = listOf(30, 60, 180, 360, 720, 1440)
    fun frequency(value: Int): String = if (value < 60) "$value minutos" else "${value / 60} hora${if (value == 60) "" else "s"}"
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Intervalo de consulta", "Aproximadamente a cada ${frequency(minutes)}", actionLabel = "ALTERAR") {
        showSettingsChoiceDialog("Periodicidade de verificação", "O Android pode atrasar a execução para economizar energia.",
            choices.map { frequency(it) to (if (it == 60) "Padrão recomendado" else "Verificação periódica aproximada") },
            choices.indexOf(minutes)) { which ->
            UpdatePreferences.interval(this, choices[which])
            UpdateScheduler.refresh(this)
            openSettingsSection("updates")
        }
    })
    sectionGap(layout, 12)
    layout.addView(iconSectionHeader(R.drawable.ic_settings, "Atualização de aparelhos pareados", "Sem instalação remota"))
    sectionGap(layout, 5)
    toggle("remote_requests", "Aceitar pedidos remotos", "Autoriza apenas verificação e download solicitados por controlador pareado; desligado por padrão",
        UpdatePreferences.remoteRequests(this))
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
        layout.addView(actionButton("Consultar atualizador remoto") { remoteRequest("status") })
        sectionGap(layout, 6)
        layout.addView(actionButton("Verificar e baixar no remoto") {
            showAppConfirm("Solicitar atualização no aparelho remoto?",
                "O aparelho pareado verificará GitHub Releases e, se houver APK mais novo, tentará baixá-lo respeitando rede e bateria. Ele precisa autorizar pedidos remotos em Configurações. A instalação jamais acontece por este comando.",
                R.drawable.ic_shield, "Solicitar") { remoteRequest("checkDownload") }
        })
        sectionGap(layout, 5)
        layout.addView(label("Após a solicitação, use Consultar atualizador remoto para acompanhar a transferência. Este comando requer Celular Remoto alpha.112 ou superior no aparelho de destino.", 12f, muted = true))
    }
    sectionGap(layout, 12)
    layout.addView(label("Instalação manual obrigatória em cada celular. A verificação em segundo plano é aproximada; apenas Releases com assinatura estável são oferecidas. Versões 111 e 112 permanecem compatíveis nas operações antigas. Transferência de APK por Wi-Fi local será uma evolução posterior.", 12f, muted = true))
}
