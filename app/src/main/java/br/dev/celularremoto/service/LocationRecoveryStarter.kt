package br.dev.celularremoto.service

import android.content.Context
import android.content.Intent
import android.os.Build
import br.dev.celularremoto.core.LocationRecoveryMode
import br.dev.celularremoto.core.LocationShareStore
import br.dev.celularremoto.core.SettingsStore

/** Best effort restoration, only when Android granted background location already. */
object LocationRecoveryStarter {
    fun allowed(context: Context): Boolean = LocationRecoveryMode.canRecover(
        SettingsStore.locationRecoveryEnabled(context),
        SettingsStore.isMonitorEnabled(context),
        LocationShareStore.hasPermission(context),
        LocationShareStore.hasBackgroundPermission(context),
        LocationShareStore.locationEnabled(context)
    )
    fun tryStart(context: Context): Boolean {
        if (!allowed(context)) return false
        if (LocationShareService.running) return true
        return runCatching {
            val intent = Intent(context, LocationShareService::class.java)
                .setAction(LocationShareService.ACTION_RECOVER)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
            true
        }.getOrDefault(false)
    }
}
