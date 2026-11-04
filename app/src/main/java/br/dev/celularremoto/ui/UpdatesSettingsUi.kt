package br.dev.celularremoto.ui

import android.widget.LinearLayout
import br.dev.celularremoto.R
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
    val hours = UpdatePreferences.intervalHours(this)
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Intervalo de consulta", "Aproximadamente a cada $hours horas", actionLabel = "ALTERAR") {
        showSettingsChoiceDialog("Periodicidade de verificação", "O Android pode atrasar a execução para economizar energia.",
            listOf("6 horas" to "Atualizações mais frequentes", "12 horas" to "Intermediário", "24 horas" to "Padrão econômico"),
            listOf(6, 12, 24).indexOf(hours)) { which ->
            UpdatePreferences.interval(this, listOf(6, 12, 24)[which])
            UpdateScheduler.refresh(this)
            openSettingsSection("updates")
        }
    })
    sectionGap(layout, 12)
    layout.addView(label("Instalação manual obrigatória: o Android pode solicitar autorização para instalar APKs desta origem. A verificação em segundo plano é aproximada, e só versões publicadas com assinatura estável são oferecidas. A transferência de APK entre celulares por Wi-Fi fica para uma próxima etapa.", 12f, muted = true))
}
