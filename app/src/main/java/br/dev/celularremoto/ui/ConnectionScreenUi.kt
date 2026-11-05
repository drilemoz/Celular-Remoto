package br.dev.celularremoto.ui

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import br.dev.celularremoto.core.Ipv4Suggestion
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.PairingCode
import java.net.Inet4Address

/** Cabeçalho do controlador em linhas independentes; evita compressão em 320-360 dp. */
internal fun MainActivity.renderController() {
    val host = SettingsStore.getRemoteHost(this)
    val saved = RecentDevices.list(this).firstOrNull { it.host == host }
    val remoteName = remoteConnection.cached?.optJSONObject("battery")?.optString("device")
        ?.takeIf { remoteConnection.cachedHost == host && it.isNotBlank() }
    val titleText = if (saved?.customName == true) saved.name
        else remoteName ?: saved?.name ?: "Nenhum aparelho selecionado"
    val card = panel(14).apply { background = shape(fieldColor, 22) }
    val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val phone = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        background = shape(surface, 15)
        addView(vectorIcon(br.dev.celularremoto.R.drawable.ic_phone, 25, ink))
    }
    heading.addView(phone, LinearLayout.LayoutParams(dp(48), dp(48)))
    val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
    currentDeviceTitle = label(titleText, 16f, strong = true).apply {
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, 0, 0, 0)
    }
    names.addView(currentDeviceTitle)
    names.addView(label(if (host.isBlank()) "Escolha um aparelho" else connectionKind(host), 11.5f, muted = true).apply {
        setPadding(0, dp(3), 0, 0)
    })
    heading.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
    heading.addView(headerIconButton(br.dev.celularremoto.R.drawable.ic_edit, "Renomear aparelho") {
        RecentDevices.list(this).firstOrNull { it.host == host }
            ?.let { renameSavedDevice(it, returnToChooser = false) }
            ?: showAppNotice("Renomear", "Pareie ou salve este aparelho primeiro.")
    }, LinearLayout.LayoutParams(dp(48), dp(48)))
    heading.addView(headerIconButton(br.dev.celularremoto.R.drawable.ic_phone, "Escolher outro aparelho") {
        showConnectionChooser()
    }, LinearLayout.LayoutParams(dp(48), dp(48)))
    card.addView(heading)
    sectionGap(card, 10)

    val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    currentStatus = label("", 12f, strong = true).apply {
        background = shape(surface, 13)
        setPadding(dp(9), dp(7), dp(9), dp(7))
        maxLines = 1
    }
    statusRow.addView(currentStatus, LinearLayout.LayoutParams(0, -2, 1f))
    currentConnectButton = actionButton("Conectar") {
        when (effectiveConnectionState()) {
            "connected", "loading" -> disconnectCurrent()
            "waiting" -> refreshRemote(silent = false)
            else -> showConnectionChooser()
        }
    }.apply { textSize = 12.5f; background = shape(surface, 14, true) }
    statusRow.addView(currentConnectButton, LinearLayout.LayoutParams(dp(116), dp(48)))
    card.addView(statusRow)
    currentUpdated = label("", 11f, muted = true).apply {
        setPadding(0, dp(8), 0, 0)
        maxLines = 3
    }
    card.addView(currentUpdated)
    sectionGap(card, 12)
    val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    actions.addView(actionButton("⟳  Atualizar") { refreshRemote(silent = false) }.apply {
        textSize = 13.5f
        background = shape(surface, 15)
    }, LinearLayout.LayoutParams(0, dp(48), 1f))
    actions.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
    actions.addView(actionButton("◉  Diagnóstico") { openConnectionDiagnostics() }.apply {
        textSize = 12.5f
        background = shape(surface, 15)
    }, LinearLayout.LayoutParams(0, dp(48), 1f))
    actions.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
    actions.addView(actionButton("ⓘ  Detalhes") {
        val current = remoteConnection.cached?.takeIf { remoteConnection.cachedHost == host }
        if (current != null) showRemoteDetailsDialog(current)
        else showAppNotice("Detalhes", "Atualize o aparelho para obter as informações remotas.")
    }.apply {
        textSize = 13.5f
        background = shape(surface, 15)
    }, LinearLayout.LayoutParams(0, dp(48), 1f))
    card.addView(actions)
    content.addView(card)
    gap(15)
    resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    content.addView(resultBox)
    updateConnectionLabels()
    val cached = remoteConnection.cached
    if (cached != null && remoteConnection.cachedHost == host) displayResult(cached, fromCache = true)
    else {
        displayEmptyRemoteDashboard()
        if (remoteConnection.state == "offline") {
            resultBox?.addView(connectionIssueCard(host), 0)
        }
    }
}

internal fun MainActivity.effectiveConnectionState(): String {
    val host = SettingsStore.getRemoteHost(this)
    return remoteConnection.effectiveState(host)
}

internal fun MainActivity.updateConnectionLabels() {
    val host = SettingsStore.getRemoteHost(this)
    val hasKey = host.isNotBlank() && RecentDevices.key(this, host) != null
    val actualState = effectiveConnectionState()
    val diagnostic = br.dev.celularremoto.core.ConnectionHealthStore.status(this, host)
    val probeRecovered = actualState == "offline" && diagnostic.lastSuccessMs > remoteConnection.lastAttemptMs &&
        System.currentTimeMillis() - diagnostic.lastSuccessMs <= 45_000L
    val text = when {
        probeRecovered -> "◌ Sonda respondeu; atualizando painel"
        else -> when (actualState) {
        "connected" -> "● Resposta recente"
        "offline" -> "● Sem resposta"
        "waiting" -> "◌ Aguardando nova verificação"
        "loading" -> "◌ Atualizando…"
        else -> "○ Desconectado"
        }
    }
    currentStatus?.text = text
    currentStatus?.setTextColor(if (actualState == "connected") highlight else
        if (actualState == "offline") Color.rgb(190, 88, 61) else dim)
    currentConnectButton?.contentDescription = if (actualState == "connected" || actualState == "loading")
        "Desconectar e voltar à Home" else "Conectar ou escolher aparelho"
    currentConnectButton?.text = when (actualState) {
        "connected", "loading" -> "Desconectar"
        "offline" -> "Opções"
        "waiting" -> "Verificar"
        else -> "Conectar"
    }
    currentUpdated?.text = when {
        !hasKey -> "Selecione uma forma de conexão no botão Conectar"
        actualState == "offline" -> "Última tentativa: ${formatDate(remoteConnection.lastAttemptMs)}" +
            if (remoteConnection.cachedHost == host && remoteConnection.lastSuccessMs > 0L)
                " · última leitura: ${formatDate(remoteConnection.lastSuccessMs)}" else ""
        remoteConnection.lastSuccessMs > 0 && remoteConnection.cachedHost == host ->
            "Leitura: ${formatDate(remoteConnection.lastSuccessMs)}" +
                (remoteConnection.lastRoundTripMs?.let { " · ${it} ms" } ?: "")
        else -> remoteConnection.message ?: "Aguardando resposta de $host"
    }
}

internal fun MainActivity.networkIcon(host: String): String = when {
    host.isBlank() -> "○"
    localWifiPrefix()?.let { host.startsWith(it) } == true -> "📶"
    PairingCode.isPrivateIpv4(host) -> "📡"
    else -> "🛡"
}

internal fun MainActivity.connectionKind(host: String): String = when {
    host.isBlank() -> "Sem aparelho"
    localWifiPrefix()?.let { host.startsWith(it) } == true -> "IP da rede local"
    PairingCode.isPrivateIpv4(host) -> "IP privado / VPN"
    else -> "VPN / nome de rede"
}

/** Apenas o IP da interface de rede Wi-Fi ativa; sem supor prefixos de outras redes. */
internal fun MainActivity.localWifiPrefix(): String? {
    return try {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
        val capabilities = network?.let { manager.getNetworkCapabilities(it) }
        if (network == null || capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) null
        else manager.getLinkProperties(network)?.linkAddresses?.firstNotNullOfOrNull { link ->
            val ip = link.address as? Inet4Address ?: return@firstNotNullOfOrNull null
            if (!ip.isSiteLocalAddress) return@firstNotNullOfOrNull null
            Ipv4Suggestion.prefix(ip.hostAddress.orEmpty(), link.prefixLength)
        }
    } catch (_: Exception) { null }
}
