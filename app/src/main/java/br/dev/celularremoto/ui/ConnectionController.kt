package br.dev.celularremoto.ui

import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore

internal fun MainActivity.disconnectCurrent() {
    val host = SettingsStore.getRemoteHost(this)
    remoteConnection.disconnect(host)
    SettingsStore.setRemoteHost(this, "")
    br.dev.celularremoto.service.ConnectionHealthScheduler.refresh(this)
    remoteConnection.clear()
    mode = 1
    render()
}

/** Keep Activity visibility/lifecycle checks here, and network/state in RemoteConnectionManager. */
internal fun MainActivity.refreshRemote(silent: Boolean) {
    if (mode != 1) return
    val host = SettingsStore.getRemoteHost(this)
    remoteConnection.fetch(
        host = host,
        stillVisible = { active && mode == 1 && !isFinishing && !isDestroyed && SettingsStore.getRemoteHost(this) == host },
    ) { finished, fresh ->
        if (finished) {
            if (fresh != null) {
                val name = fresh.optJSONObject("battery")?.optString("device").orEmpty()
                currentDeviceTitle?.text = RecentDevices.list(this).firstOrNull { it.host == host }
                    ?.name ?: name.ifBlank { host }
                displayResult(fresh)
            } else if (remoteConnection.state == "offline") {
                val old = remoteConnection.cached?.takeIf { remoteConnection.cachedHost == host }
                if (old != null) {
                    displayResult(old, fromCache = true)
                    resultBox?.addView(connectionIssueCard(host), 0)
                } else {
                    displayEmptyRemoteDashboard()
                    resultBox?.addView(connectionIssueCard(host), 0)
                }
            }
        }
        updateConnectionLabels()
    }
}
