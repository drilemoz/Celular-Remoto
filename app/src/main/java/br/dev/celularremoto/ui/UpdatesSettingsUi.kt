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
    val localCode = UpdatePreferences.getString(this, "state_code")
    if (UpdateController.prepared(this)) {
        layout.addView(dialogMetricRow("Origem de instalação",
            if (android.os.Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls())
                "Autorizada; Android ainda pedirá confirmação" else "Requer autorização local no Android"))
    }
    if (UpdatePreferences.getLong(this, "download_id") > 0L && !UpdateController.prepared(this)) {
        settingsActionPair(layout, "Atualizar estado", {
            thread(name = "CelularRemoto-update-reconcile") {
                val outcome = UpdateController.reconcilePending(applicationContext)
                runOnUiThread { if (!isFinishing && !isDestroyed) {
                    if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                    message(outcome)
                } }
            }
        }, "Cancelar download", {
            showAppConfirm("Cancelar download deste aparelho?",
                "Cancela somente a transferência atual; o aplicativo instalado não é alterado.",
                R.drawable.ic_shield, "Cancelar download") {
                thread(name = "CelularRemoto-update-cancel") {
                    val outcome = UpdateController.cancelDownload(applicationContext)
                    runOnUiThread { if (!isFinishing && !isDestroyed) {
                        if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                        message(outcome.second)
                    } }
                }
            }
        })
    }
    if (localCode == "error" || localCode == "cancelled")
        layout.addView(label("O APK instalado foi preservado. Confira a rede e tente baixar novamente.", 11.5f, muted = true))
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
        val downloadedCode = UpdatePreferences.getLong(this, "download_code")
        val availableCode = UpdatePreferences.getString(this, "available_code").toLongOrNull() ?: 0L
        if (availableCode > downloadedCode) {
            layout.addView(actionButton("Baixar versão mais recente") {
                showAppConfirm("Substituir APK preparado por versão mais recente?",
                    "O arquivo anterior será substituído após iniciar o novo download; a versão instalada não muda.",
                    R.drawable.ic_folder_download, "Baixar nova versão") {
                    thread(name = "CelularRemoto-update-newer") {
                        val outcome = UpdateController.download(applicationContext)
                        runOnUiThread { if (!isFinishing && !isDestroyed) {
                            if (mode == 3 && settingsSection == "updates") openSettingsSection("updates")
                            message(outcome)
                        } }
                    }
                }
            })
        }
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
        val remoteChecked = if (selectedAtLastCheck) UpdatePreferences.getLong(this, "last_remote_checked") else 0L
        if (remoteChecked > 0) layout.addView(dialogMetricRow("Última resposta do monitorado",
            DateFormat.getDateTimeInstance().format(Date(remoteChecked))))
        if (remoteVersion.isNotBlank()) layout.addView(dialogMetricRow("Versão remota consultada", remoteVersion))
        if (remoteState.isNotBlank()) layout.addView(dialogMetricRow("Atualizador remoto", remoteState))
        if (remoteProgress.isNotBlank()) layout.addView(dialogMetricRow("Transferência remota", remoteProgress))
        sectionGap(layout, 6)
        settingsActionPair(layout, "Consultar remoto", { requestRemoteAppUpdate("status") },
            "Verificar e baixar", {
                showAppConfirm("Solicitar atualização no remoto?",
                    "O aparelho pareado verificará as Releases e tentará baixar um APK " +
                        "respeitando rede e bateria. A instalação permanece local.",
                    R.drawable.ic_shield, "Solicitar") {
                    requestRemoteAppUpdate("checkDownload")
                }
            })
        sectionGap(layout, 5)
        val remoteCode = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_state_code") else ""
        val remotePercent = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_percent").toIntOrNull() else null
        if (remotePercent != null && remotePercent in 0..100 && remoteCode in setOf("downloading", "queued"))
            layout.addView(dialogMetricRow("Progresso do monitorado", "$remotePercent%"))
        val installedPermission = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_install_permission") else ""
        val remoteReady = remoteCode == "ready"
        if (remoteReady) layout.addView(dialogMetricRow("Instalação no monitorado",
            if (installedPermission == "true") "APK pronto; confirme no outro celular" else "Libere a origem no outro celular e confirme"))
        if (remoteCode.isNotBlank()) layout.addView(dialogMetricRow("Etapa remota", remoteCode))
        val remoteAllowed = if (selectedAtLastCheck) UpdatePreferences.getString(this, "last_remote_allowed") else ""
        if (remoteAllowed == "false") layout.addView(label(
            "O monitorado não autorizou pedidos de atualização. Ative a autorização nele para baixar remotamente.", 11.5f, muted = true))
        sectionGap(layout, 6)
        layout.addView(actionButton("Acompanhar remoto por 3 minutos") {
            beginRemoteUpdatePolling()
            requestRemoteAppUpdate("status")
        })
        val remotePending = if (selectedAtLastCheck)
            UpdatePreferences.getString(this, "last_remote_pending") == "true" else false
        if (remotePending) {
            sectionGap(layout, 5)
            settingsActionPair(layout, "Atualizar progresso", {
                requestRemoteAppUpdate("status")
            }, "Cancelar remoto", {
                showAppConfirm("Cancelar download no aparelho monitorado?",
                    "Cancela somente a transferência pendente. Não altera o APK instalado nem as permissões.",
                    R.drawable.ic_shield, "Cancelar download") {
                    requestRemoteAppUpdate("cancel")
                }
            })
        } else if (remoteCode in setOf("available", "error", "cancelled", "waiting")) {
            sectionGap(layout, 5)
            layout.addView(actionButton("Tentar download remoto") { requestRemoteAppUpdate("download") })
        }
        sectionGap(layout, 5)
        layout.addView(label("O acompanhamento consulta o aparelho a cada 15 s somente com esta tela aberta, por até 3 minutos. " +
            "Com a tela apagada ou sem rede, as consultas param. A instalação nunca é remota.", 11f, muted = true))
    }
    sectionGap(layout, 12)
    layout.addView(label("A instalação exige confirmação local. São aceitas apenas Releases verificadas e assinadas. Consultas periódicas podem atrasar no Android.", 11.5f, muted = true))
}

/** Comando cifrado para um único par autenticado; retorno assíncrono com checagem de identidade. */
internal fun MainActivity.requestRemoteAppUpdate(action: String, silent: Boolean = false) {
    if (updatePollingBusy) return
    val targetHost = SettingsStore.getRemoteHost(this)
    val expectedKey = if (targetHost.isNotBlank()) RecentDevices.key(this, targetHost)?.copyOf() else null
    if (expectedKey == null) { if (!silent) message("Selecione e pareie o aparelho remoto."); return }
    val accessMethod = remoteConnection.method
    updatePollingBusy = true
    if (!silent) message("Solicitando $action ao aparelho pareado…")
    thread(name = "CelularRemoto-update-remote") {
        val result = runCatching {
            RemoteClient.appUpdate(targetHost, expectedKey, applicationContext, accessMethod, action)
        }
        val unchanged = SettingsStore.getRemoteHost(applicationContext) == targetHost &&
            RecentDevices.key(applicationContext, targetHost)?.contentEquals(expectedKey) == true
        if (unchanged && result.isSuccess) {
            val reply = result.getOrThrow()
            val data = reply.optJSONObject("data") ?: JSONObject()
            UpdatePreferences.putLong(applicationContext, "last_remote_checked", System.currentTimeMillis())
            UpdatePreferences.putString(applicationContext, "last_remote_host", targetHost)
            UpdatePreferences.putString(applicationContext, "last_remote_version", data.optString("version").take(50))
            UpdatePreferences.putString(applicationContext, "last_remote_state", data.optString("state").take(160))
            UpdatePreferences.putString(applicationContext, "last_remote_progress", data.optString("progress").take(80))
            UpdatePreferences.putString(applicationContext, "last_remote_state_code", data.optString("stateCode").take(32))
            UpdatePreferences.putString(applicationContext, "last_remote_percent", data.optInt("progressPercent", -1).toString())
            UpdatePreferences.putString(applicationContext, "last_remote_install_permission", data.optBoolean("installOriginAllowed", false).toString())
            UpdatePreferences.putString(applicationContext, "last_remote_allowed", data.optBoolean("remoteAllowed", false).toString())
            UpdatePreferences.putString(applicationContext, "last_remote_pending", data.optBoolean("transferPending", false).toString())
        }
        runOnUiThread {
            updatePollingBusy = false
            if (!isFinishing && !isDestroyed && unchanged && active && mode == 3 && settingsSection == "updates") {
                val scroll = content.parent as? android.widget.ScrollView
                val y = scroll?.scrollY ?: 0
                render()
                scroll?.post { scroll.scrollTo(0, y) }
                if (!silent) {
                    val reply = result.getOrNull()
                    message(if (reply != null) reply.optString("message", "Solicitação recebida")
                        else (result.exceptionOrNull()?.message ?: "Falha ao consultar o aparelho remoto"))
                    if (action == "checkDownload" && reply?.optBoolean("ok") == true) beginRemoteUpdatePolling()
                }
            }
        }
    }
}

/** Polling limitado à tela visível. Nunca cria serviço ou temporizador de fundo. */
internal fun MainActivity.beginRemoteUpdatePolling() {
    updatePollingUntilMs = android.os.SystemClock.elapsedRealtime() + 3 * 60_000L
    handler.removeCallbacks(remoteUpdateStatusPoller)
    if (active && mode == 3 && settingsSection == "updates")
        handler.postDelayed(remoteUpdateStatusPoller, 15_000L)
}

internal fun MainActivity.pollRemoteUpdateOnce() {
    if (!active || mode != 3 || settingsSection != "updates" ||
        android.os.SystemClock.elapsedRealtime() >= updatePollingUntilMs) return
    if (!updatePollingBusy) requestRemoteAppUpdate("status", silent = true)
    handler.postDelayed(remoteUpdateStatusPoller, 15_000L)
}
