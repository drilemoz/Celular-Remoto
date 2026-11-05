package br.dev.celularremoto.ui

import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService

/** Opt-in local: o modo Bateria não abre servidor, não solicita chaves ou autoriza câmera. */
internal fun MainActivity.fillLocalBatterySettings(layout: LinearLayout) {
    dialogIntro(layout, "O acompanhamento local registra bateria, temperatura e sessões com a tela apagada, sem ativar o servidor remoto. A coleta contínua pode consumir mais bateria e não ignora restrições do Android.")
    val enabled = SettingsStore.localBatteryMonitoring(this)
    val remote = MonitorService.isServiceActive
    val active = MonitorService.isBatteryMonitoringActive
    layout.addView(iconSectionHeader(R.drawable.ic_battery, "Coleta contínua local", when {
        active && remote -> "Ativa · compartilhada com o Monitorado"
        active -> "Ativa · somente neste aparelho"
        enabled -> "Autorizada · aguardando retomada"
        else -> "Desligada (padrão)"
    }))
    sectionGap(layout, 9)
    layout.addView(iconMenuRow(R.drawable.ic_power,
        if (enabled) "Desativar coleta local" else "Ativar coleta local",
        if (enabled) "Pausar a coleta independente; histórico preservado" else "Continuar com a tela apagada, sem ativar compartilhamento remoto",
        badge = SettingsBadgeKind.forEnabled(enabled)) {
        val next = !SettingsStore.localBatteryMonitoring(this)
        SettingsStore.setLocalBatteryMonitoring(this, next)
        runCatching { MonitorService.refreshModes(this) }.onFailure {
            SettingsStore.setLocalBatteryMonitoring(this, !next)
            message("O Android recusou iniciar o acompanhamento: ${it.message}")
        }
        render()
    })
    sectionGap(layout, 10)
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Ver medições e histórico",
        "Bateria, corrente, temperatura e previsões") {
        batteryRemote = false
        mode = 5
        render()
    })
    sectionGap(layout, 10)
    layout.addView(iconMenuRow(R.drawable.ic_wifi, "Restrições do Android e WakeLock",
        "Conferir Doze e energia; WakeLock permanece opcional apenas para Monitorado") {
        openSettingsSection("power")
    })
    sectionGap(layout, 13)
    layout.addView(label("A mesma rotina amostra no máximo a cada 30 segundos, esteja Monitorado ativo ou não. Sem duas notificações permanentes. Ao parar a bateria local, Monitorado continua se estiver autorizado.", 12f, muted = true))
}
