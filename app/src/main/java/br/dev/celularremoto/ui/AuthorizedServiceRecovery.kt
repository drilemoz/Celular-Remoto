package br.dev.celularremoto.ui

import android.os.SystemClock
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService

/** Retoma somente opções já autorizadas, com o app visível e limite monotônico. */
internal class AuthorizedServiceRecovery {
    private var lastRecoveryAttemptMs = 0L

    fun recoverAuthorizedServiceIfStopped(activity: MainActivity) = with(activity) {
        fun needsRecovery(): Boolean =
            (SettingsStore.isMonitorEnabled(this) && !MonitorService.isServiceActive) ||
            (SettingsStore.localBatteryMonitoring(this) && !MonitorService.isBatteryMonitoringActive) ||
            (SettingsStore.controllerBatteryPolling(this) && !MonitorService.isControllerBatteryPollingActive) ||
            (SettingsStore.controllerLiveAlerts(this) && !MonitorService.isControllerLiveAlertsActive)

        if (!needsRecovery()) return@with
        val now = SystemClock.elapsedRealtime()
        if (lastRecoveryAttemptMs != 0L && now - lastRecoveryAttemptMs < 60_000L) return@with
        lastRecoveryAttemptMs = now
        handler.postDelayed({
            if (active && !isFinishing && !isDestroyed && needsRecovery()) {
                runCatching { MonitorService.start(this) }.onFailure {
                    SettingsStore.recordRecoveryEvent(this, "Retomada negada pelo sistema")
                }
                handler.postDelayed({ if (active && mode == 0) render() }, 1_000L)
            }
        }, 800L)
        Unit
    }
}
