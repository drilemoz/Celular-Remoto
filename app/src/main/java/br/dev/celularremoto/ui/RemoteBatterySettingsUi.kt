package br.dev.celularremoto.ui

import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.ControllerRemoteTelemetry
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService

/** Opt-in explícito para bateria do aparelho pareado com a tela do controlador apagada. */
internal fun MainActivity.fillRemoteBatterySettings(layout: LinearLayout) {
    dialogIntro(layout, "Consulta somente bateria, temperatura e corrente pela rede local, com a chave salva. " +
        "Mantém uma notificação visível enquanto ativo. Não ativa câmera, SMS, GPS, servidor local ou ADB. " +
        "O Android ainda pode encerrar o serviço para economizar energia.")
    val selected = SettingsStore.getRemoteHost(this)
    val paired = selected.isNotBlank() && RecentDevices.key(this, selected) != null
    val enabled = SettingsStore.controllerBatteryPolling(this)
    layout.addView(iconSectionHeader(R.drawable.ic_battery, "Bateria remota contínua", when {
        enabled && MonitorService.isControllerBatteryPollingActive -> "Ativa · consulta de 60 s, com recuo em falhas"
        enabled -> "Autorizada · serviço ainda não confirmado"
        paired -> "Desligada · aparelho pareado"
        else -> "Selecione e conecte um aparelho antes de habilitar"
    }))
    sectionGap(layout, 9)
    layout.addView(iconMenuRow(R.drawable.ic_power,
        if (enabled) "Desativar acompanhamento" else "Ativar acompanhamento",
        if (enabled) "Parar consultas automáticas; última medição permanece identificada" else
            "Continuar consultando a bateria pela LAN em segundo plano (opcional)",
        badge = SettingsBadgeKind.forEnabled(enabled)) {
        if (!enabled && !paired) {
            message("Escolha primeiro um aparelho com chave salva na aba Controlar.")
        } else if (enabled) {
            SettingsStore.setControllerBatteryPolling(this, false)
            runCatching { MonitorService.refreshModes(this) }
            openSettingsSection("battery_remote")
        } else showAppConfirm("Ativar bateria remota contínua?",
            "O celular controlador manterá um serviço foreground visível e fará uma consulta AES-GCM " +
                "aproximadamente a cada 60 segundos enquanto Android permitir. Após falhas, o intervalo aumenta " +
                "até cinco minutos. Isso consome bateria e exige o servidor Monitorado ativo no outro celular, " +
                "na mesma rede local. Nenhum conteúdo de mensagens ou localização é consultado.",
            R.drawable.ic_battery, "Ativar acompanhamento") {
            SettingsStore.setControllerBatteryPolling(this, true)
            val started = runCatching { MonitorService.refreshModes(this) }
            if (started.isFailure) {
                SettingsStore.setControllerBatteryPolling(this, false)
                message("O Android impediu iniciar o serviço: ${started.exceptionOrNull()?.javaClass?.simpleName}")
            }
            openSettingsSection("battery_remote")
            if (started.isSuccess) requestHealthNotificationPermission()
        }
    })
    sectionGap(layout, 10)
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Última telemetria",
        ControllerRemoteTelemetry.status(this)) {
        message(ControllerRemoteTelemetry.status(this) + ". Se não houver leitura, confira Wi-Fi, pareamento e Monitorado ativo no outro aparelho.")
    })
    sectionGap(layout, 10)
    layout.addView(iconMenuRow(R.drawable.ic_wifi, "Diagnóstico de conexão",
        "Verifique a mesma rede local, a chave e as restrições de bateria") {
        openSettingsSection("connection")
    })
    sectionGap(layout, 11)
    layout.addView(label("Dados de bateria recentes são válidos por três minutos. " +
        "Quando a conexão falhar, o aplicativo indica leitura desatualizada, sem fingir que está online. " +
        "O monitorado deve usar alpha.141 ou superior para a consulta compacta.", 12f, muted = true))
}
