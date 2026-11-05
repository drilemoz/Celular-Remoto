package br.dev.celularremoto.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.ConnectionPowerController
import br.dev.celularremoto.service.MonitorService

/** Página LOCAL: nenhum comando remoto pode ativar WakeLock ou pedir isenção. */
internal fun MainActivity.fillConnectionPowerSettings(layout: LinearLayout) {
    dialogIntro(layout, "A tela pode apagar normalmente. WakeLock mantém a CPU ativa quando permitido, mas não impede o Doze nem garante que o Wi-Fi responda. Pode aumentar bastante o consumo da bateria.")
    val power = getSystemService(PowerManager::class.java)
    val cm = getSystemService(ConnectivityManager::class.java)
    val cap = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
    val wifi = cap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val am = getSystemService(ActivityManager::class.java)
    val unrestricted = if (Build.VERSION.SDK_INT >= 23) power.isIgnoringBatteryOptimizations(packageName) else true
    val idle = if (Build.VERSION.SDK_INT >= 23) power.isDeviceIdleMode else false
    val restricted = Build.VERSION.SDK_INT >= 28 && am.isBackgroundRestricted

    layout.addView(iconSectionHeader(R.drawable.ic_wifi, "Estado do Android", "Dados consultados neste aparelho"))
    sectionGap(layout, 8)
    layout.addView(dialogMetricRow("Serviço Monitorado", if (MonitorService.isServiceActive) "Ativo" else "Desligado"))
    layout.addView(dialogMetricRow("Otimização de bateria", if (unrestricted) "Isento do Doze (parcial)" else "Sujeito às otimizações"))
    layout.addView(dialogMetricRow("Estado Doze", if (idle) "Modo ocioso detectado" else "Não detectado neste instante"))
    layout.addView(dialogMetricRow("Economia de energia", if (power.isPowerSaveMode) "Ativa" else "Desativada"))
    layout.addView(dialogMetricRow("Restrição em segundo plano", if (restricted) "Restrito pelo Android" else "Não informada/sem restrição"))
    layout.addView(dialogMetricRow("Rede ativa", if (wifi) "Wi-Fi" else if (cap != null) "Outra rede" else "Indisponível"))
    layout.addView(dialogMetricRow("WakeLock CPU", if (ConnectionPowerController.cpuHeld) "Adquirido" else "Não adquirido"))
    layout.addView(dialogMetricRow("Wi-Fi Lock", if (ConnectionPowerController.wifiHeld) "Adquirido" else "Não adquirido"))
    sectionGap(layout, 10)
    layout.addView(iconMenuRow(R.drawable.ic_battery, "Otimização de bateria do Android",
        if (unrestricted) "O aplicativo está na lista de isenções parciais" else "Abrir lista e escolher Sem restrições, se disponível") {
        showAppConfirm("Configurar bateria no Android",
            "O sistema Xiaomi pode ter restrições adicionais. Isenção não elimina todos os limites de Doze. A opção precisa ser autorizada no próprio aparelho.",
            R.drawable.ic_battery, "Abrir configurações") {
            runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                .onFailure { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
    })
    sectionGap(layout, 7)
    layout.addView(iconMenuRow(R.drawable.ic_phone, "Inicialização automática / Xiaomi",
        "Conferir em Aplicativos → Celular Remoto → Bateria e Início automático") {
        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
            .onFailure { message("Não foi possível abrir as configurações do sistema.") }
    })
    sectionGap(layout, 14)
    layout.addView(iconSectionHeader(R.drawable.ic_power, "Manter CPU acordada (WakeLock)", "Escolha voluntária; padrão desligado"))
    sectionGap(layout, 8)
    val mode = SettingsStore.connectionWakeMode(this)
    val modes = listOf(
        Triple("off", "Desligado (recomendado)", "CPU pode repousar normalmente; menor consumo"),
        Triple("30m", "Teste de 30 minutos", "WakeLock temporário durante Monitorado; expira sozinho"),
        Triple("monitor", "Enquanto Monitorado estiver ativo", "Maior consumo; liberado ao parar o serviço")
    )
    modes.forEach { (id, name, description) ->
        layout.addView(iconMenuRow(R.drawable.ic_power, name, description,
            badge = SettingsBadgeKind.forEnabled(mode == id)) {
            if (id == "off") {
                SettingsStore.setConnectionWakeMode(this, id)
                render()
            } else showAppConfirm("Ativar WakeLock de CPU?",
                "O processador pode permanecer ativo com a tela apagada, causando maior consumo e aquecimento. Não garante rede durante Doze. Será usado SOMENTE enquanto Monitorado estiver ativo. Pode ser desligado aqui.",
                R.drawable.ic_power, "Ativar") {
                SettingsStore.setConnectionWakeMode(this, id)
                render()
            }
        })
        sectionGap(layout, 6)
    }
    val wifiLock = SettingsStore.connectionWifiLock(this)
    layout.addView(iconMenuRow(R.drawable.ic_wifi, "Manter rádio Wi-Fi ativo (opcional)",
        if (wifiLock) "Wi-Fi Lock habilitado no serviço Monitorado" else "Desligado · menor consumo",
        badge = SettingsBadgeKind.forEnabled(wifiLock)) {
        if (wifiLock) {
            SettingsStore.setConnectionWifiLock(this, false)
            render()
        } else showAppConfirm("Ativar Wi-Fi Lock?",
            "Ajuda a manter o rádio Wi-Fi ligado durante o monitoramento, mas não supera o Doze, modo avião ou Wi-Fi desligado. Pode gastar mais bateria.",
            R.drawable.ic_wifi, "Ativar") {
            SettingsStore.setConnectionWifiLock(this, true)
            render()
        }
    })
    sectionGap(layout, 14)
    layout.addView(iconSectionHeader(R.drawable.ic_activity, "Diagnóstico de suspensão", "Lacunas do supervisor não são prova de perda de Wi-Fi"))
    sectionGap(layout, 8)
    layout.addView(dialogMetricRow("Lacunas acima de 45 s", SettingsStore.supervisorGapCount(this).toString()))
    val gap = SettingsStore.lastSupervisorGapMs(this)
    layout.addView(dialogMetricRow("Última lacuna observada", if (gap > 0L) "${gap / 1000} s" else "Nenhuma registrada"))
    layout.addView(dialogMetricRow("Último evento de rede", SettingsStore.connectionNetworkEvent(this)))
    layout.addView(iconMenuRow(R.drawable.ic_refresh, "Atualizar diagnóstico",
        "Reconsultar estado Doze, restrições, locks e rede") { render() })
    sectionGap(layout, 12)
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Ver histórico de conexão",
        "Latência, falhas reais e períodos sem medições") { openSettingsSection("connection") })
    sectionGap(layout, 10)
    layout.addView(label("Nota: o Android pode ignorar WakeLocks durante Doze. Aumentar a disponibilidade exige teste real com a tela apagada; restringir energia e parar o app à força impedem garantias de funcionamento contínuo.", 12f, muted = true))
}
