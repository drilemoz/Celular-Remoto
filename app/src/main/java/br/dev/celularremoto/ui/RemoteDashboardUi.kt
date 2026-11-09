package br.dev.celularremoto.ui

import android.app.AlertDialog
import br.dev.celularremoto.core.BatteryTemperaturePolicy
import br.dev.celularremoto.core.RemoteOperationHistory
import br.dev.celularremoto.core.RemoteOperationHistoryPolicy
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.Locale
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore

/** A atualização mostra uma fotografia autenticada do estado, não um canal persistente. */
internal fun MainActivity.displayResult(root: JSONObject, fromCache: Boolean = false) {
    val container = resultBox ?: return
    val battery = root.optJSONObject("battery")
    if (battery == null) {
        displayEmptyRemoteDashboard()
        return
    }
    container.removeAllViews()
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
    val networkType = network?.optString("type")?.ifBlank { "Indisponível" } ?: "Indisponível"
    val ssid = network?.optString("wifiSsid")?.takeIf { it.isNotBlank() && it != "null" }
    val signal = network?.optInt("wifiRssiDbm", 0)?.takeIf { it in -110..-10 }?.let { "$it dBm" }
    val wifiLabel = if (ssid != null) listOfNotNull(ssid, signal).joinToString(" · ")
        else if (network?.optBoolean("wifiPermission") == false) "Permissão pendente"
        else "Sinal indisponível"
    val metrics = listOf(
        compactTelemetryTile(if (charging) R.drawable.ic_battery_charging else R.drawable.ic_battery,
            "Bateria", level, listOfNotNull(state, source).joinToString(" · ")) {
            showRemoteThermalDialog(battery, fromCache)
        },
        compactTelemetryTile(R.drawable.ic_thermometer, "Temperatura", temperature,
            BatteryTemperaturePolicy.label(tempZone), warning = tempZone == BatteryTemperaturePolicy.Zone.HOT) {
            showRemoteThermalDialog(battery, fromCache)
        },
        compactTelemetryTile(R.drawable.ic_wifi, "Rede", networkType, wifiLabel) {
            showRemoteNetworkDetails(network, fromCache)
        }
    )
    compactActionGrid(container, metrics)
    sectionGap(container, 9)
    container.addView(label(if (liveReading)
        "Resposta autenticada: ${formatDate(remoteConnection.lastSuccessMs)}"
        else "Dados armazenados. Última resposta: ${if (remoteConnection.lastSuccessMs > 0) formatDate(remoteConnection.lastSuccessMs) else "desconhecida"}",
        11f, muted = true).apply { setPadding(dp(3), 0, 0, 0) })
    // Prontidão publicada somente no snapshot autenticado, nunca resultado de sondagem sem chave.
    root.optJSONObject("readiness")?.takeIf { it.optInt("schema") == 1 }?.let { readiness ->
        sectionGap(container, 8)
        val ready = readiness.optInt("readyCount")
        val total = readiness.optInt("total")
        val setup = readiness.optInt("setupCount")
        container.addView(dialogCardRow("Preparação do monitorado",
            "$ready/$total estados prontos · $setup precisam de configuração · outros opcionais",
            "›") { showRemotePreparationSummary(readiness, fromCache) })
    }
    root.optJSONObject("healthAlerts")?.let { health ->
        if (health.optString("level") in setOf("WARNING", "CRITICAL")) {
            sectionGap(container, 10)
            container.addView(dialogCardRow("Alerta de saúde", health.optString("reason", "Condição desconhecida"), "!", accent = true) {
                showAppNotice("Saúde do monitorado", health.optString("reason"))
            })
        }
    }
    sectionGap(container, 18)
    renderRemoteActions(container, root)
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
    compactActionGrid(container, listOf(
        compactTelemetryTile(R.drawable.ic_battery, "Bateria", "--%", "Sem leitura") {
            showAppNotice("Bateria", "Conecte e atualize o aparelho para consultar a bateria.")
        },
        compactTelemetryTile(R.drawable.ic_thermometer, "Temperatura", "-- °C", "Sem leitura") {
            showAppNotice("Temperatura", "Nenhuma medição térmica recebida deste aparelho.")
        },
        compactTelemetryTile(R.drawable.ic_wifi, "Rede", "--", "Sem leitura") {
            showAppNotice("Rede", "Nenhuma leitura de rede recebida deste aparelho.")
        }
    ))
    sectionGap(container, 10)
    container.addView(label("Os dados serão exibidos depois de uma consulta autenticada.", 11.5f, muted = true))
    sectionGap(container, 18)
    renderRemoteActions(container, null)
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

/** Quatro atalhos fixos e oito comandos sempre visíveis, com ou sem primeira leitura.
 * Reutilizar as Views quando os comandos/autorizações não mudam evita reconstruir 12
 * cartões em cada sondagem periódica de telemetria. Não armazena snapshot nem chave.
 */
private fun MainActivity.renderRemoteActions(container: LinearLayout, snapshot: JSONObject?) {
    val host = SettingsStore.getRemoteHost(this)
    val keyAvailable = host.isNotBlank() && br.dev.celularremoto.core.RecentDevices.key(this, host) != null
    val eventCount = snapshot?.optJSONArray("events")?.let { events ->
        (0 until events.length()).count { events.optJSONObject(it)?.optString("type") == "notificacao" }
    } ?: 0
    val cacheKey = RemoteActionsRenderKey(
        host = host,
        keyAvailable = keyAvailable,
        eventCount = eventCount,
        theme = "${SettingsStore.theme(this)}:${darkTheme()}:${amoledTheme()}",
        fontScale = resources.configuration.fontScale,
        widthDp = resources.configuration.screenWidthDp
    )
    remoteActionsUiCache?.takeIf { it.first == cacheKey }?.second?.let { panel ->
        (panel.parent as? ViewGroup)?.removeView(panel)
        container.addView(panel)
        return
    }
    val actionsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
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
        actionTile(R.drawable.ic_bell, "Sirene", "Alerta sonoro · até 30 s", enabled = true) {
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
    // Uma única grade: todos os comandos permanecem visíveis, sem separar atalhos arbitrariamente.
    val ordered = listOf(cards[4], cards[0], cards[6], cards[11], cards[3], cards[1],
        cards[2], cards[5], cards[7], cards[8], cards[9], cards[10])
    mobileDashboardHeader(actionsPanel, "Funcionalidades", "12 recursos")
    mobileActionGrid(actionsPanel, ordered)
    sectionGap(actionsPanel, 9)
    remoteActionsUiCache = cacheKey to actionsPanel
    container.addView(actionsPanel)
}

/** Informações de rede da última resposta autenticada. Nenhuma medição nova é presumida. */
private fun MainActivity.showRemoteNetworkDetails(network: JSONObject?, fromCache: Boolean) {
    val (wrapper, body) = visualDialogContainer()
    body.addView(visualHeader(R.drawable.ic_wifi, "Rede do monitorado",
        if (fromCache) "Leitura armazenada · não é conexão em tempo real" else "Última consulta autenticada"))
    sectionGap(body, 9)
    fun value(key: String): String = if (network == null || !network.has(key) || network.isNull(key))
        "Indisponível" else network.optString(key).ifBlank { "Indisponível" }
    fun flag(key: String): String = if (network == null || !network.has(key) || network.isNull(key))
        "Não informado" else if (network.optBoolean(key)) "Sim" else "Não"
    val items = listOf(
        "Tipo de conexão" to value("type"),
        "Nome do Wi-Fi (SSID)" to value("wifiSsid"),
        "RSSI medido" to (network?.optInt("wifiRssiDbm", 0)?.takeIf { it in -110..-10 }
            ?.let { "$it dBm" } ?: "Sem leitura"),
        "IP local informado" to value("localIpv4"),
        "Internet validada pelo Android" to flag("internetValidated"),
        "Rede tarifada" to flag("metered"),
        "Wi-Fi detectado" to flag("wifiDetected"),
        "Dados móveis detectados" to flag("cellularDetected")
    )
    items.forEachIndexed { index, pair ->
        body.addView(dialogCardRow(pair.first, pair.second, ""))
        if (index < items.lastIndex) sectionGap(body, 4)
    }
    if (value("wifiSsid") == "Indisponível") {
        sectionGap(body, 9)
        body.addView(visualInfoPanel(R.drawable.ic_info, "Permissões de Wi-Fi",
            "O SSID e o sinal podem exigir Localização precisa autorizada e localização ligada no monitorado. Nenhuma permissão será concedida remotamente."))
    }
    sectionGap(body, 10)
    body.addView(actionButton("Fechar") { activeUiDialog?.dismiss() })
    val dialog = themedPopup(this).setView(wrapper).create()
    showManagedDialog(dialog)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout((resources.displayMetrics.widthPixels * .92f).toInt().coerceAtMost(dp(520)), -2)
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
    sectionGap(layout, 9)

    val device = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(fieldColor, 18)
        setPadding(dp(11), dp(8), dp(11), dp(8))
    }
    val deviceLine = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val deviceText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    deviceText.addView(label(name, 15f, strong = true).apply { setPadding(0,0,0,0); maxLines = 1 })
    deviceText.addView(label(selectedHost.ifBlank { "Endereço indisponível" }, 11.5f, muted = true).apply { setPadding(0,dp(3),0,0) })
    deviceLine.addView(deviceText, LinearLayout.LayoutParams(0, -2, 1f))
    deviceLine.addView(visualTag("Chave salva", SettingsBadgeKind.ACTIVE))
    device.addView(deviceLine)
    layout.addView(device)
    sectionGap(layout, 9)

    layout.addView(label("AÇÕES DISPONÍVEIS", 10.5f, strong = true, muted = true))
    sectionGap(layout, 8)
    // Menu de energia compacto em três colunas, como as ações de um aparelho.
    val powerTiles = listOf(
        compactActionTile(R.drawable.ic_shield, "Bloquear", "Solicita confirmação") {
            activeUiDialog?.dismiss()
            showRemoteLockDialog()
        },
        compactActionTile(R.drawable.ic_remote_wake, "Acender tela", "Não desbloqueia") {
            activeUiDialog?.dismiss()
            showAppConfirm("Acender tela remota",
                "Solicita apenas que a tela do aparelho pareado acenda. Não remove PIN, senha ou bloqueio. Exige a autorização Acender tela em Ajustes do próprio monitorado.",
                R.drawable.ic_remote_wake, "Solicitar") {
                remoteConnection.wakeScreen(selectedHost) { result ->
                    showAppNotice("Resultado do comando",
                        result.getOrElse { it.message ?: "Não foi possível obter a confirmação do aparelho." },
                        R.drawable.ic_remote_wake)
                }
            }
        },
        compactActionTile(R.drawable.ic_power, "Reiniciar", "ADB autorizado") {
            activeUiDialog?.dismiss()
            startActivity(Intent(this, AdbActivity::class.java).apply {
                putExtra(AdbActivity.EXTRA_MAINTENANCE_HOST, selectedHost)
            })
        }
    )
    compactActionGrid(layout, powerTiles, preferredColumns =
        if (resources.configuration.fontScale > 1.6f || resources.configuration.screenWidthDp < 320) 1 else 3)
    sectionGap(layout, 9)

    val notice = visualInfoPanel(R.drawable.ic_info, "Antes de reiniciar",
        "Se o aparelho não voltar a iniciar, pode ficar inacessível sem o botão Power. A Depuração sem fios pode não voltar sozinha.")
    layout.addView(notice)
    layout.addView(label("Nenhum comando é executado ao abrir este painel. Reiniciar requer confirmação no ADB.",
        11f, muted = true).apply { setPadding(0,dp(9),0,0) })
    sectionGap(layout, 9)
    layout.addView(actionButton("Fechar") { activeUiDialog?.dismiss() }.apply {
        minHeight = dp(48)
    }, LinearLayout.LayoutParams(-1, dp(48)))

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
