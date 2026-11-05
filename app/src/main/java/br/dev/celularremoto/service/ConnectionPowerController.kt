package br.dev.celularremoto.service

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import br.dev.celularremoto.core.SettingsStore

/** Locks opcionais pertencem EXCLUSIVAMENTE ao serviço Monitorado. Não iniciar segundo serviço. */
internal class ConnectionPowerController(private val context: Context) {
    private var cpuLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        @Volatile var cpuHeld: Boolean = false
            private set
        @Volatile var wifiHeld: Boolean = false
            private set
    }

    fun sync(active: Boolean) {
        val now = System.currentTimeMillis()
        val mode = SettingsStore.connectionWakeMode(context)
        if (mode == "30m" && SettingsStore.connectionWakeDeadline(context) <= now) {
            SettingsStore.setConnectionWakeMode(context, "off")
        }
        val wantCpu = ConnectionPowerPolicy.cpuWanted(mode,
            SettingsStore.connectionWakeDeadline(context), now, active)
        if (wantCpu) {
            try {
                val lock = cpuLock ?: context.getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CelularRemoto:MonitoradoOpcional").also {
                        it.setReferenceCounted(false)
                        cpuLock = it
                    }
                if (!lock.isHeld) lock.acquire(ConnectionPowerPolicy.CPU_LEASE_MS)
            } catch (error: Exception) {
                Log.w("CelularRemotoPower", "WakeLock CPU indisponível", error)
            }
        } else runCatching { cpuLock?.takeIf { it.isHeld }?.release() }
        cpuHeld = cpuLock?.isHeld == true

        val wantWifi = active && SettingsStore.connectionWifiLock(context)
        if (wantWifi) {
            try {
                @Suppress("DEPRECATION")
                val modeWifi = WifiManager.WIFI_MODE_FULL_HIGH_PERF
                val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val lock = wifiLock ?: manager.createWifiLock(modeWifi, "CelularRemoto:WifiOpcional").also {
                    it.setReferenceCounted(false)
                    wifiLock = it
                }
                if (!lock.isHeld) lock.acquire()
            } catch (error: Exception) {
                Log.w("CelularRemotoPower", "WifiLock indisponível", error)
            }
        } else runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        wifiHeld = wifiLock?.isHeld == true
    }

    fun release() {
        runCatching { cpuLock?.takeIf { it.isHeld }?.release() }
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        cpuHeld = false
        wifiHeld = false
    }
}
