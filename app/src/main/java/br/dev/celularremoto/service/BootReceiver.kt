package br.dev.celularremoto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.dev.celularremoto.core.SettingsStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        // O verificador do controlador é independente da opção de reiniciar o servidor local.
        ConnectionHealthScheduler.refresh(context)
        if (!SettingsStore.autoStartEnabled(context) ||
            (!SettingsStore.isMonitorEnabled(context) && !SettingsStore.localBatteryMonitoring(context))) return
        try {
            RecoveryScheduler.refresh(context)
            MonitorService.start(context)
            // Android may reject a background FGS; never bypass permission requirements.
            if (SettingsStore.isMonitorEnabled(context)) LocationRecoveryStarter.tryStart(context)
        } catch (_: Exception) {
            // Restrições de fabricante/SO: iniciar manualmente se necessário.
        }
    }
}
