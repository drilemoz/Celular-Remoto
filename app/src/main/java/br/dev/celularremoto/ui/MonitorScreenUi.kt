package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.capture.ScreenFrameStore
import br.dev.celularremoto.capture.ScreenShareService
import android.media.projection.MediaProjectionManager
import br.dev.celularremoto.transport.AuthenticatedAccess

/** Alpha.22: módulo de interface extraído de MainActivity. Sem estado de conexão próprio. */

/** Tela monitorada: ações agrupadas à direita do cartão, sem botões longos. */
internal fun MainActivity.renderMonitor() {
    content.addView(compactSectionTitle("Monitoramento local"))
    gap(8)
    val enabled = SettingsStore.isMonitorEnabled(this)
    val running = MonitorService.isRunning
    val card = panel(10).apply { background = shape(surface, 16, true) }
    val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val iconHolder = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        background = shape(fieldColor, 17)
        addView(vectorIcon(br.dev.celularremoto.R.drawable.ic_phone, 25, highlight))
    }
    header.addView(iconHolder, LinearLayout.LayoutParams(dp(42), dp(42)))
    header.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
    val headerText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    headerText.addView(label("Este aparelho", 17f, strong = true).apply { setPadding(0, 0, 0, 0) })
    val state = when {
        !enabled -> "● Monitoramento inativo"
        running -> "● Monitoramento ativo"
        MonitorService.isServiceActive -> "● Serviço ativo · rede em recuperação"
        else -> "● Serviço sem execução"
    }
    headerText.addView(label(state.removePrefix("● "), 12.5f, strong = true).apply {
        setPadding(0, dp(6), 0, 0)
        setTextColor(if (enabled && running) highlight else if (enabled) Color.rgb(190, 88, 61) else dim)
    })
    header.addView(headerText, LinearLayout.LayoutParams(0, -2, 1f))
    card.addView(header)
    sectionGap(card, 7)
    card.addView(statePill(
        if (enabled && running) "Disponível na rede" else if (enabled) "Aguardando serviço" else "Acesso remoto parado",
        if (enabled && running) "active" else if (enabled) "warning" else "neutral"
    ))
    sectionGap(card, 8)
    val networkLine = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    networkLine.addView(vectorIcon(br.dev.celularremoto.R.drawable.ic_wifi, 18, highlight))
    networkLine.addView(label(preferredLocalAddress() ?: "IP indisponível", 12f, strong = true).apply {
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(dp(7), 0, dp(4), 0)
    }, LinearLayout.LayoutParams(0, -2, 1f))
    networkLine.addView(vectorIcon(br.dev.celularremoto.R.drawable.ic_shield, 17, dim))
    networkLine.addView(label("Acesso autorizado", 11f, muted = true).apply {
        setPadding(dp(5), 0, 0, 0)
    })
    card.addView(networkLine)
    sectionGap(card, 9)

    val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val actionTiles = mutableListOf<LinearLayout>()
    fun addCompact(title: String, icon: Int, emphasize: Boolean = false, onClick: () -> Unit) {
        actionTiles.add(compactActionTile(icon, title, selected = emphasize, onClick = onClick))
    }
    if (enabled && running) {
        addCompact("Gerar QR", br.dev.celularremoto.R.drawable.ic_qr, true) { chooseQrAddress() }
        addCompact("Mostrar info", br.dev.celularremoto.R.drawable.ic_info) { showMyConnectionInfo() }
        addCompact("Interromper", br.dev.celularremoto.R.drawable.ic_power) {
            SettingsStore.setMonitorEnabled(this, false)
            ScreenShareService.stop(this)
            MonitorService.refreshModes(this)
            render()
        }
    } else {
        addCompact(if (enabled) "Retomar" else "Ativar", br.dev.celularremoto.R.drawable.ic_power, true) {
            if (enabled) startServiceExplicitly() else requestStart()
        }
        addCompact("Mostrar info", br.dev.celularremoto.R.drawable.ic_info) { showMyConnectionInfo() }
        addCompact("Ativar + Foto", br.dev.celularremoto.R.drawable.ic_remote_camera) {
            startActivity(Intent(this, RemotePhotoSetupActivity::class.java))
        }
    }
    compactActionGrid(actions, actionTiles)
    card.addView(actions)
    content.addView(card)
    gap(9)
    val localState = br.dev.celularremoto.core.DeviceStateReader.snapshot(this)
    val hasUsage = localState.optBoolean("usagePermission")
    val hasWifi = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val diagCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    diagCard.addView(iconMenuRow(br.dev.celularremoto.R.drawable.ic_activity,
        "Estado e permissões do aparelho",
        "Tela: ${localState.optString("screenState")} · Uso: ${if (hasUsage) "ok" else "pendente"} · Localização: ${if (hasWifi) "ok" else "pendente"}",
        badge = if (hasUsage && hasWifi) SettingsBadgeKind.ACTIVE else SettingsBadgeKind.WARNING,
        actionLabel = if (hasUsage && hasWifi) "Ver" else "Revisar") {
        openSettingsModule(); openSettingsSection("diagnostics")
    })
    content.addView(diagCard)
    gap(7)
    content.addView(settingsSlimRow(br.dev.celularremoto.R.drawable.ic_shield,
        "Preparar aparelho para controle remoto",
        "Autorizações e prontidão: atualizações, captura, comandos e permissões", "Ver") {
        openSettingsModule(); openSettingsSection("preparation")
    })
    if (enabled && running) {
        gap(11)
        content.addView(compactSectionTitle("Captura e compartilhamento", "Ativação local"))
        gap(7)
        val cameraRunning = br.dev.celularremoto.capture.CameraFrameStore.isActive()
        val photoArmed = br.dev.celularremoto.service.PhotoReadyService.armed
        val captures = listOf(
            compactActionTile(br.dev.celularremoto.R.drawable.ic_monitor,
                "Tela", if (ScreenFrameStore.sharing) "Em transmissão" else "Exige autorização por sessão",
                selected = ScreenFrameStore.sharing) {
                if (ScreenFrameStore.sharing) {
                    ScreenShareService.stop(this)
                    handler.postDelayed({ if (active && mode == 0) render() }, 300L)
                } else {
                    showAppConfirm("Compartilhar a tela?",
                        "O aparelho controlador pareado poderá ver o que aparecer nesta tela, inclusive informações privadas. " +
                        "O Android solicitará sua autorização para a sessão. Sem gravação. Toque e áudio só funcionam se habilitados separadamente em Configurações. A sessão permanece ativa durante desconexões até você parar ou o Android encerrar.",
                        confirmText = "Autorizar captura") {
                        val manager = getSystemService(MediaProjectionManager::class.java)
                        startActivityForResult(manager.createScreenCaptureIntent(), screenCaptureRequest)
                    }
                }
            },
            compactActionTile(br.dev.celularremoto.R.drawable.ic_remote_camera,
                "Câmera", if (cameraRunning) "Ativa" else "Prévia iniciada localmente",
                selected = cameraRunning) {
                showAppConfirm("Compartilhar câmera ao vivo?",
                    "A câmera ficará aberta somente enquanto a tela de câmera estiver visível neste telefone. Um controlador pareado poderá ver quadros JPEG da câmera frontal ou traseira. Não há áudio ou gravação no monitorado. Voltar ou bloquear a tela interrompe a captura.",
                    confirmText = "Abrir câmera") {
                    startActivity(Intent(this, CameraShareActivity::class.java))
                }
            },
            compactActionTile(br.dev.celularremoto.R.drawable.ic_remote_camera,
                "Foto", if (photoArmed) "Fotografia preparada" else "Armar com consentimento local",
                selected = photoArmed) {
                startActivity(Intent(this, RemotePhotoSetupActivity::class.java))
            }
        )
        val capturePanel = panel(9).apply { background = shape(surface, 16, true) }
        compactActionGrid(capturePanel, captures)
        sectionGap(capturePanel, 7)
        capturePanel.addView(label(
            "Tela: ${if (ScreenFrameStore.sharing) "compartilhando" else "parada"} · " +
            "Câmera: ${if (cameraRunning) "ativa" else "parada"} · " +
            "Foto: ${if (photoArmed) "preparada" else "desarmada"}",
            11f, muted = true).apply { maxLines = 3; setPadding(dp(2), 0, dp(2), 0) })
        content.addView(capturePanel)
        gap(5)
        content.addView(label("Tela e câmera exigem autorização local; o controlador não pode ativá-las sozinho.",
            10.5f, muted = true))
    }
    gap(12)
    if (enabled && running) renderIncomingAccess()
    else content.addView(statePill("Aguardando ativação local"))
    gap(10)
    addText("Permissões e privacidade podem ser revisadas em Configurações.", 11f, muted = true)
}

/** Event-driven on authenticated requests, plus timeout refresh for disconnected clients. */
private fun MainActivity.renderIncomingAccess() {
    val peers = AuthenticatedAccess.recent()
    val card = panel(10).apply { background = shape(surface, 16, true) }
    card.addView(compactSectionTitle("Acessos autenticados",
        if (peers.isEmpty()) "Aguardando consultas" else "${peers.size} recente(s)"))
    sectionGap(card, 6)
    if (peers.isEmpty()) {
        card.addView(label("Nenhum acesso autenticado nos últimos 45 s. Não há socket permanente.", 11f, muted = true))
    } else {
        peers.forEach { peer ->
            sectionGap(card, 6)
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val data = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            data.addView(label(peer.name.ifBlank { "Aparelho não identificado" }, 13f, strong = true).apply {
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            data.addView(label("${peer.address} · ${peer.method}", 11f, muted = true).apply {
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            line.addView(data, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(actionButton("Ver") { showIncomingAccessActions(peer) }.apply {
                textSize = 11f; minWidth = 0
                setPadding(dp(3), 0, dp(3), 0)
            }, LinearLayout.LayoutParams(dp(78), dp(40)))
            card.addView(line)
        }
    }
    content.addView(card)
}

private fun MainActivity.showIncomingAccessActions(peer: AuthenticatedAccess.Peer) {
    // O cartão pode permanecer na tela sem redesenhar a cada consulta. Buscar a
    // atividade mais recente somente no momento em que o usuário abre os detalhes.
    val latest = AuthenticatedAccess.recent().firstOrNull { it.key == peer.key } ?: peer
    val (wrapper, layout) = dialogWrapper()
    dialogIntro(layout, "Identidade e método declarados pelo cliente autenticado; não constituem verificação independente.")
    layout.addView(dialogMetricRow("Nome informado", latest.name.ifBlank { "Não informado" }))
    layout.addView(dialogMetricRow("IP de origem", latest.address))
    layout.addView(dialogMetricRow("Método informado", latest.method))
    layout.addView(dialogMetricRow("Último acesso", formatDate(latest.lastSeenMs)))
    layout.addView(dialogMetricRow("Primeiro acesso", formatDate(latest.sinceMs)))
    sectionGap(layout, 8)
    layout.addView(label("O acesso não é uma sessão TCP contínua. Interromper ou trocar a chave afeta todos os controladores.", 11f, muted = true))
    val dialog = themedPopup(this)
        .setTitle("Acesso autenticado")
        .setView(wrapper)
        .setNeutralButton("Trocar chave") { _, _ -> confirmRotateMonitorKey() }
        .setNegativeButton("Fechar", null)
        .setPositiveButton("Parar serviço") { _, _ ->
            showAppConfirm(
                "Interromper monitoramento?",
                "Todos os aparelhos pareados perderão acesso aos dados enquanto o serviço estiver parado.",
                br.dev.celularremoto.R.drawable.ic_power, "Interromper"
            ) {
                SettingsStore.setMonitorEnabled(this, false)
                ScreenShareService.stop(this)
            MonitorService.refreshModes(this)
                render()
            }
        }.create()
    showManagedDialog(dialog)
}

