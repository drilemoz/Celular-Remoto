package br.dev.celularremoto.ui

import android.app.AlertDialog
import br.dev.celularremoto.core.BatteryTemperaturePolicy
import br.dev.celularremoto.core.RemoteOperationHistory
import br.dev.celularremoto.core.RemoteOperationHistoryPolicy
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.Locale
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore

/** A atualização mostra uma fotografia autenticada do estado, não um canal persistente. */
internal fun MainActivity.displayResult(root: JSONObject, fromCache: Boolean = false) {
    val container = resultBox ?: return
    container.removeAllViews()
    val battery = root.optJSONObject("battery")
    if (battery == null) {
        displayEmptyRemoteDashboard()
        return
    }
    val network = root.optJSONObject("network")
    val level = if (battery.has("levelPercent") && !battery.isNull("levelPercent"))
        battery.optInt("levelPercent", -1).takeIf { it in 0..100 }?.let { "$it%" } ?: "--%"
        else "--%"
    val charging = battery.optBoolean("chargingIndicator", false)
    val state = battery.optString("state").takeIf {
        it in setOf("Carregando", "Descarregando", "Cheia", "Não carregando")
    } ?: "Sem leitura"
    val source = battery.optString("powerSource").takeIf { it in setOf("USB", "Tomada", "Sem fio", "Base") }
    val temperature = if (battery.has("temperatureC") && !battery.isNull("temperatureC"))
        String.format(Locale.getDefault(), "%.1f °C", battery.optDouble("temperatureC")) else "-- °C"
    val tempC = if (battery.has("temperatureC") && !battery.isNull("temperatureC"))
        battery.optDouble("temperatureC") else null
    val tempZone = BatteryTemperaturePolicy.zone(tempC)
    val liveReading = !fromCache && remoteConnection.state == "connected"
    mobileDashboardHeader(container, "Informações do aparelho",
        if (liveReading) "Consulta recente" else "Última leitura")
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    row.addView(mobileMetricCard(if (charging) R.drawable.ic_battery_charging else R.drawable.ic_battery,
        "Bateria", if (charging) "$level ⚡" else level,
        listOfNotNull(state, source).joinToString(" · ")) { showRemoteThermalDialog(battery, fromCache) },
        LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(View(this), LinearLayout.LayoutParams(dp(9), 1))
    row.addView(mobileMetricCard(R.drawable.ic_thermometer, "Temperatura", temperature,
        BatteryTemperaturePolicy.label(tempZone), tempZone) { showRemoteThermalDialog(battery, fromCache) },
        LinearLayout.LayoutParams(0, -2, 1f))
    container.addView(row)
    sectionGap(container, 9)
    val networkType = network?.optString("type")?.ifBlank { "Indisponível" } ?: "Indisponível"
    val ssid = network?.optString("wifiSsid")?.takeIf { it.isNotBlank() && it != "null" }
    val signal = network?.optInt("wifiRssiDbm", 0)?.takeIf { it in -110..-10 }?.let { "$it dBm" }
    val wifiLabel = if (ssid != null) listOfNotNull(ssid, signal).joinToString(" · ")
        else if (network?.optBoolean("wifiPermission") == false) "SSID: permissão pendente no monitorado"
        else "SSID indisponível"
    container.addView(mobileNetworkRow(networkType, wifiLabel) {
        showAppNotice("Rede do monitorado", if (ssid == null && network?.optBoolean("wifiPermission") == false)
            "No celular monitorado: Configurações → Diagnóstico → Nome e sinal do Wi-Fi. Autorize Localização precisa e ative a localização. A permissão não pode ser concedida remotamente."
            else "Tipo: $networkType\n${ssid ?: "SSID indisponível"}\n${signal ?: "RSSI indisponível"}")
    })
    sectionGap(container, 9)
    container.addView(label(if (liveReading)
        "Resposta autenticada: ${formatDate(remoteConnection.lastSuccessMs)}"
        else "Dados armazenados. Última resposta: ${if (remoteConnection.lastSuccessMs > 0) formatDate(remoteConnection.lastSuccessMs) else "desconhecida"}",
        11f, muted = true).apply { setPadding(dp(3), 0, 0, 0) })
    root.optJSONObject("healthAlerts")?.let { health ->
        if (health.optString("level") in setOf("WARNING", "CRITICAL")) {
            sectionGap(container, 10)
            container.addView(dialogCardRow("Alerta de saúde", health.optString("reason", "Condição desconhecida"), "!", accent = true) {
                showAppNotice("Saúde do monitorado", health.optString("reason"))
            })
        }
    }
    sectionGap(container, 18)
    renderRemoteActions(container, root, fromCache)
    val selectedHost = SettingsStore.getRemoteHost(this)
    val recent = RemoteOperationHistory.read(this, selectedHost).firstOrNull()
    if (recent != null) {
        sectionGap(container, 12)
        container.addView(dialogCardRow("Atividade recente de arquivos",
            "${RemoteOperationHistoryPolicy.label(recent.operation)} · ${recent.outcome} · ${recent.done}/${recent.total} item(ns)", "›") {
            startActivity(Intent(this, RemoteFilesActivity::class.java).apply {
                putExtra(RemoteFilesActivity.EXTRA_HOST, selectedHost)
                putExtra(RemoteFilesActivity.EXTRA_METHOD, remoteConnection.method)
            })
        })
    }
}

/** Os controles aparecem antes da primeira consulta, mas exigem aparelho pareado. */
internal fun MainActivity.displayEmptyRemoteDashboard() {
    val container = resultBox ?: return
    container.removeAllViews()
    mobileDashboardHeader(container, "Informações do aparelho", "Sem leitura")
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    row.addView(mobileMetricCard(R.drawable.ic_battery, "Bateria", "--%", "Carga indisponível") {
        showAppNotice("Bateria", "Conecte e atualize o aparelho para consultar a bateria.")
    }, LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(View(this), LinearLayout.LayoutParams(dp(9), 1))
    row.addView(mobileMetricCard(R.drawable.ic_thermometer, "Temperatura", "-- °C", "Sem leitura") {
        showAppNotice("Temperatura", "Conecte e atualize o aparelho para consultar a temperatura.")
    }, LinearLayout.LayoutParams(0, -2, 1f))
    container.addView(row)
    sectionGap(container, 9)
    container.addView(mobileNetworkRow("Rede do aparelho", "Tipo, SSID e sinal quando disponíveis") {
        showAppNotice("Rede", "Nenhuma leitura de rede recebida deste aparelho.")
    })
    sectionGap(container, 10)
    container.addView(label("Os dados serão exibidos depois de uma consulta autenticada.", 11.5f, muted = true))
    sectionGap(container, 18)
    renderRemoteActions(container, null, true)
}

private fun MainActivity.mobileDashboardHeader(parent: LinearLayout, title: String, side: String) {
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    row.addView(label(title, 16f, strong = true).apply { maxLines = 2 },
        LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(label(side, 10.5f, muted = true).apply { gravity = Gravity.END })
    parent.addView(row)
    sectionGap(parent, 9)
}

private fun MainActivity.mobileMetricCard(icon: Int, title: String, value: String,
    subtitle: String, temperatureZone: BatteryTemperaturePolicy.Zone? = null, onClick: () -> Unit): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    minimumHeight = dp(91)
    val themed = when (temperatureZone) {
        BatteryTemperaturePolicy.Zone.COLD -> if (darkTheme()) android.graphics.Color.rgb(35, 61, 78) else android.graphics.Color.rgb(224, 241, 252)
        BatteryTemperaturePolicy.Zone.WARM -> if (darkTheme()) android.graphics.Color.rgb(91, 67, 35) else android.graphics.Color.rgb(255, 240, 214)
        BatteryTemperaturePolicy.Zone.HOT -> if (darkTheme()) android.graphics.Color.rgb(100, 42, 47) else android.graphics.Color.rgb(255, 226, 227)
        BatteryTemperaturePolicy.Zone.NORMAL -> fieldColor
        else -> fieldColor
    }
    background = shape(themed, 20)
    setPadding(dp(11), dp(9), dp(9), dp(9))
    isClickable = true; isFocusable = true
    contentDescription = "$title: $value. $subtitle"
    setOnClickListener { onClick() }
    val caption = LinearLayout(this@mobileMetricCard).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    caption.addView(vectorIcon(icon, 19, dim), LinearLayout.LayoutParams(dp(21), dp(21)))
    caption.addView(label(title, 12f, muted = true).apply { setPadding(dp(5), 0, 0, 0) })
    addView(caption)
    addView(label(value, 19f, strong = true).apply { setPadding(0, dp(6), 0, 0) })
    addView(label(subtitle, 11f, muted = true).apply {
        maxLines = 2
        setPadding(0, dp(5), 0, 0)
    })
}

private fun MainActivity.mobileNetworkRow(title: String, subtitle: String, onClick: () -> Unit): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(58)
        background = shape(fieldColor, 19)
        setPadding(dp(11), dp(8), dp(9), dp(8))
        isClickable = true; isFocusable = true
        contentDescription = "$title. $subtitle"
        setOnClickListener { onClick() }
        addView(vectorIcon(R.drawable.ic_wifi, 23, ink), LinearLayout.LayoutParams(dp(26), dp(26)))
        val words = LinearLayout(this@mobileNetworkRow).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        words.addView(label(title, 13.5f, strong = true).apply { setPadding(0, 0, 0, 0) })
        words.addView(label(subtitle, 11.5f, muted = true).apply { setPadding(0, dp(3), 0, 0); maxLines = 2 })
        addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        addView(label("›", 23f, muted = true))
    }

/** Quatro atalhos fixos + oito funções extras. A dobra não altera disponibilidade. */
private fun MainActivity.renderRemoteActions(container: LinearLayout, snapshot: JSONObject?, cachedReading: Boolean) {
    val host = SettingsStore.getRemoteHost(this)
    val keyAvailable = host.isNotBlank() && br.dev.celularremoto.core.RecentDevices.key(this, host) != null
    val eventCount = snapshot?.optJSONArray("events")?.let { events ->
        (0 until events.length()).count { events.optJSONObject(it)?.optString("type") == "notificacao" }
    } ?: 0
    val cards = listOf(
        actionTile(R.drawable.ic_folder_download, "Arquivos", "Downloads fixos", enabled = true) {
            startActivity(Intent(this, RemoteFilesActivity::class.java).apply {
                putExtra(RemoteFilesActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemoteFilesActivity.EXTRA_METHOD, remoteConnection.method)
            })
        },
        actionTile(R.drawable.ic_bell, "Alertas", if (eventCount > 0) "$eventCount evento(s)" else "", enabled = true) {
            startActivity(Intent(this, RemoteCatalogActivity::class.java).apply {
                putExtra(RemoteCatalogActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemoteCatalogActivity.EXTRA_METHOD, remoteConnection.method)
                putExtra(RemoteCatalogActivity.EXTRA_TYPE, "notifications")
            })
        },
        actionTile(R.drawable.ic_phone, "Apps", "Instalados · autorizados", enabled = true) {
            startActivity(Intent(this, RemoteCatalogActivity::class.java).apply {
                putExtra(RemoteCatalogActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemoteCatalogActivity.EXTRA_METHOD, remoteConnection.method)
                putExtra(RemoteCatalogActivity.EXTRA_TYPE, "apps")
            })
        },
        actionTile(R.drawable.ic_message, "Mensagens", "SMS e respostas", enabled = true) {
            openRemoteMessagesScreen()
        },
        actionTile(R.drawable.ic_monitor, "Tela", "Ao vivo · autorizado", enabled = true) {
            startActivity(Intent(this, RemoteScreenActivity::class.java).apply {
                putExtra(RemoteScreenActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemoteScreenActivity.EXTRA_METHOD, remoteConnection.method)
            })
        },
        actionTile(R.drawable.ic_remote_camera, "Foto", "JPEG único · sem vídeo", enabled = true) {
            startActivity(Intent(this, RemotePhotoActivity::class.java).apply {
                putExtra(RemotePhotoActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemotePhotoActivity.EXTRA_METHOD, remoteConnection.method)
            })
        },
        actionTile(R.drawable.ic_remote_camera, "Câmeras", "Ao vivo · início local", enabled = true) {
            startActivity(Intent(this, RemoteCameraActivity::class.java).apply {
                putExtra(RemoteCameraActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this@renderRemoteActions))
                putExtra(RemoteCameraActivity.EXTRA_METHOD, remoteConnection.method)
            })
        },
        actionTile(R.drawable.ic_bell, "Som", "Localizar · até 30 s", enabled = true) {
            showRemotePhysicalDialog(sound = true)
        },
        actionTile(R.drawable.ic_flashlight, "Lanterna", "Ligar e desligar", enabled = true) {
            showRemotePhysicalDialog(sound = false)
        },
        actionTile(R.drawable.ic_shield, "Bloquear", "Confirmação + PIN", enabled = true) {
            showRemoteLockDialog()
        },
        actionTile(R.drawable.ic_settings, "Energia", "Bloqueio e manutenção", enabled = true) {
            showRemoteMaintenance()
        },
        actionTile(R.drawable.ic_pin, "Localização", "Consulta autorizada") {
            startActivity(Intent(this, RemoteLocationActivity::class.java)
                .putExtra(RemoteLocationActivity.EXTRA_HOST, br.dev.celularremoto.core.SettingsStore.getRemoteHost(this))
                .putExtra(RemoteLocationActivity.EXTRA_METHOD, "Chave salva"))
        },
    )

    fun guarded(tiles: List<LinearLayout>) {
        // Os cards já têm callbacks para suas respectivas Activities; em estado sem
        // pareamento, bloquear esses callbacks e encaminhar ao pareamento explícito.
        if (!keyAvailable) tiles.forEach { item ->
            item.setOnClickListener { showConnectionChooser() }
            item.contentDescription = "${item.contentDescription}. Conecte um aparelho primeiro"
        }
    }
    guarded(cards)
    val shortcuts = listOf(cards[4], cards[0], cards[6], cards[11]) // Tela, Arquivos, Câmeras, Localização
    val others = cards.filterNot { it in shortcuts }
    mobileDashboardHeader(container, "Acesso rápido", "4 funções")
    mobileActionGrid(container, shortcuts)
    sectionGap(container, 11)
    mobileDashboardHeader(container, "Outras funções", "8 funções")
    val toggle = actionButton(if (expandedRemoteFunctions) "⌃  Ocultar funções" else "⌄  Mostrar todas as funções") {
        expandedRemoteFunctions = !expandedRemoteFunctions
        // Redesenhar somente a seção remota; não refazer polling ou reconectar.
        if (snapshot == null) displayEmptyRemoteDashboard() else displayResult(snapshot,
            fromCache = cachedReading)
    }.apply {
        textSize = 13.5f
        background = shape(surface, 18, true)
        minimumHeight = dp(48)
    }
    if (expandedRemoteFunctions) {
        mobileActionGrid(container, others)
        sectionGap(container, 9)
    }
    container.addView(toggle, LinearLayout.LayoutParams(-1, dp(50)))
}

private fun MainActivity.mobileActionGrid(parent: LinearLayout, tiles: List<LinearLayout>) {
    compactActionGrid(parent, tiles)
}

/** A conexão normal AES-GCM não confere privilégio de reinicialização. */
private fun MainActivity.showRemoteMaintenance() {
    val selectedHost = SettingsStore.getRemoteHost(this)
    val name = currentDeviceTitle?.text?.toString().orEmpty().ifBlank { "Aparelho remoto" }
    val (wrapper, layout) = visualDialogContainer()
    layout.addView(visualHeader(R.drawable.ic_settings, "Energia e Manutenção", "Controles do aparelho remoto"))
    sectionGap(layout, 14)

    val device = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(fieldColor, 18)
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }
    val deviceLine = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val deviceText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    deviceText.addView(label(name, 15f, strong = true).apply { setPadding(0,0,0,0); maxLines = 1 })
    deviceText.addView(label(selectedHost.ifBlank { "Endereço indisponível" }, 11.5f, muted = true).apply { setPadding(0,dp(3),0,0) })
    deviceLine.addView(deviceText, LinearLayout.LayoutParams(0, -2, 1f))
    deviceLine.addView(visualTag("Chave salva", SettingsBadgeKind.ACTIVE))
    device.addView(deviceLine)
    layout.addView(device)
    sectionGap(layout, 12)

    layout.addView(label("AÇÕES DISPONÍVEIS", 10.5f, strong = true, muted = true).apply {
        letterSpacing = .10f; setPadding(0,0,0,dp(7))
    })
    layout.addView(visualActionRow(R.drawable.ic_shield, "Bloquear tela",
        "Exige autorização e bloqueio seguro", "Confirmar") {
        activeUiDialog?.dismiss()
        showRemoteLockDialog()
    })
    sectionGap(layout, 7)
    layout.addView(visualActionRow(R.drawable.ic_remote_wake, "Acender tela",
        "Sem desbloquear o Android", "Ver tela") {
        activeUiDialog?.dismiss()
        showAppNotice("Acender tela", "A opção está em Ver tela. Precisa de autorização local no aparelho monitorado e não remove PIN nem desbloqueia o Android.")
    })
    sectionGap(layout, 7)
    layout.addView(visualActionRow(R.drawable.ic_power, "Reiniciar normalmente",
        "ADB pareado · confirmação obrigatória", "ADB", dangerous = true) {
        activeUiDialog?.dismiss()
        startActivity(Intent(this, AdbActivity::class.java).apply {
            putExtra(AdbActivity.EXTRA_MAINTENANCE_HOST, selectedHost)
        })
    })
    sectionGap(layout, 13)

    val notice = visualInfoPanel(R.drawable.ic_info, "Antes de reiniciar",
        "Se o aparelho não voltar a iniciar, pode ficar inacessível sem o botão Power. A Depuração sem fios pode não voltar sozinha.")
    layout.addView(notice)
    layout.addView(label("Nenhum comando é executado ao abrir este painel. Reiniciar requer confirmação no ADB.",
        11f, muted = true).apply { setPadding(0,dp(9),0,0) })
    sectionGap(layout, 12)
    layout.addView(actionButton("Fechar") { activeUiDialog?.dismiss() }.apply {
        minHeight = dp(46)
    }, LinearLayout.LayoutParams(-1, dp(46)))

    val dialog = themedPopup(this).setView(wrapper).create()
    showManagedDialog(dialog)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout((resources.displayMetrics.widthPixels * .92f).toInt().coerceAtMost(dp(520)), -2)
}

private fun MainActivity.actionTile(
    icon: Int, title: String, subtitle: String, enabled: Boolean = false, onClick: () -> Unit
): LinearLayout = compactActionTile(icon, title, subtitle, selected = false, onClick = onClick)

private fun MainActivity.showFeatureSoon(name: String, description: String) {
    showAppNotice(name, "$description\n\nEste recurso ainda não está implementado.", R.drawable.ic_info)
}

internal fun MainActivity.showRemoteDetailsDialog(root: JSONObject) {
    val name = currentDeviceTitle?.text?.toString().orEmpty()
    val selectedHost = br.dev.celularremoto.core.SettingsStore.getRemoteHost(this)
    startActivity(Intent(this, RemoteDetailsActivity::class.java).apply {
        putExtra(RemoteDetailsActivity.EXTRA_SNAPSHOT, root.toString())
        putExtra(RemoteDetailsActivity.EXTRA_DEVICE, name)
        putExtra(RemoteDetailsActivity.EXTRA_HOST, selectedHost)
        putExtra(RemoteDetailsActivity.EXTRA_METHOD, remoteConnection.method)
        putExtra(RemoteDetailsActivity.EXTRA_UPDATED, remoteConnection.lastSuccessMs)
    })
}
