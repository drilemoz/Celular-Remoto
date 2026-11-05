package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.transport.AuthenticatedAccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tela inicial compacta: o nome salvo não implica conexão ativa. */
internal fun MainActivity.renderHome() {
    val saved = RecentDevices.list(this)
    val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
    top.addView(compactSectionTitle("Meus aparelhos", "${saved.size} salvo(s)"),
        LinearLayout.LayoutParams(0, -2, 1f))
    top.addView(headerIconButton(R.drawable.ic_phone, "Adicionar aparelho por QR ou IP") {
        showConnectionChooser()
    }, LinearLayout.LayoutParams(dp(48), dp(48)))
    content.addView(top)
    sectionGap(content, 8)

    val devices = panel(8).apply { background = shape(surface, 18, true) }
    if (saved.isEmpty()) {
        devices.addView(label("Nenhum aparelho pareado", 14f, strong = true))
        devices.addView(label("Use o botão + para conectar via QR ou IP e chave autorizada.", 12f, muted = true))
        devices.addView(actionButton("Conectar novo aparelho") { showConnectionChooser() })
    } else {
        saved.forEachIndexed { index, item ->
            val device = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(76)
                setPadding(dp(7), dp(5), dp(4), dp(5))
                isClickable = true
                isFocusable = true
                contentDescription = "${item.name}. Abrir controles; última resposta salva não é conexão permanente"
                setOnClickListener { connectSavedDevice(item) }
            }
            device.addView(vectorIcon(R.drawable.ic_phone, 24, highlight),
                LinearLayout.LayoutParams(dp(28), dp(28)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(4), 0) }
            info.addView(label(item.name, 14f, strong = true).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, 0, 0, 0)
            })
            val last = if (item.lastSuccessMs > 0L)
                "Última resposta · " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.lastSuccessMs))
                else "Aguardando primeira resposta"
            info.addView(label(last, 11f, muted = true).apply { setPadding(0, dp(3), 0, 0) })
            device.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            device.addView(headerIconButton(R.drawable.ic_settings, "Gerenciar ${item.name}") {
                showHomeSavedDeviceOptions(item)
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            devices.addView(device)
            if (index < saved.lastIndex) compactDivider(devices, 38)
        }
    }
    content.addView(devices)
    sectionGap(content, 16)

    val enabled = SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning
    content.addView(compactSectionTitle("Este telefone"))
    sectionGap(content, 8)
    val local = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(68)
        background = shape(surface, 17, true)
        setPadding(dp(12), dp(8), dp(10), dp(8))
        isClickable = true
        isFocusable = true
        contentDescription = "Monitorado: ${if (enabled) "serviço ativo" else "sem serviço ativo"}. Gerenciar"
        setOnClickListener { navigateMainTab(0) }
    }
    local.addView(vectorIcon(R.drawable.ic_shield, 23, if (enabled) highlight else dim))
    val localText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
    localText.addView(label("Monitoramento", 14f, strong = true).apply { setPadding(0, 0, 0, 0) })
    localText.addView(label(if (enabled) "Servidor local em execução" else "Toque para configurar", 11f, muted = true).apply {
        setPadding(0, dp(2), 0, 0)
    })
    local.addView(localText, LinearLayout.LayoutParams(0, -2, 1f))
    local.addView(statePill(if (enabled) "Ativo" else "Inativo", if (enabled) "active" else "neutral"))
    content.addView(local)

    val recentCount = if (enabled) AuthenticatedAccess.recent().size else 0
    if (recentCount > 0) {
        sectionGap(content, 7)
        content.addView(label("$recentCount acesso(s) autenticado(s) recente(s) · ver em Monitorado", 11f, muted = true))
    }
    sectionGap(content, 17)
    content.addView(compactSectionTitle("Acesso rápido"))
    sectionGap(content, 8)
    compactActionGrid(content, listOf(
        compactActionTile(R.drawable.ic_monitor, "Controlar", "Aparelho pareado") { navigateMainTab(1) },
        compactActionTile(R.drawable.ic_shield, "Monitorado", "Este celular") { navigateMainTab(0) },
        compactActionTile(R.drawable.ic_battery, "Bateria", "Leituras e testes") { navigateMainTab(5) }
    ))
}

private fun MainActivity.showHomeSavedDeviceOptions(item: RecentDevices.Device) {
    val (wrapper, layout) = dialogWrapper()
    dialogIntro(layout, "${item.name} · ${item.host}")
    var popup: AlertDialog? = null
    layout.addView(iconMenuRow(R.drawable.ic_phone, "Conectar", "Abrir este aparelho salvo") {
        popup?.dismiss()
        connectSavedDevice(item)
    })
    sectionGap(layout, 7)
    layout.addView(iconMenuRow(R.drawable.ic_info, "Renomear", "Alterar nome local") {
        popup?.dismiss()
        renameSavedDevice(item, returnToChooser = false)
    })
    sectionGap(layout, 7)
    layout.addView(iconMenuRow(R.drawable.ic_logout, "Esquecer aparelho", "Remove credencial e histórico local", dangerous = true) {
        popup?.dismiss()
        forgetSavedDevice(item, returnToChooser = false)
    })
    popup = themedPopup(this).setTitle("Gerenciar aparelho").setView(wrapper).setNegativeButton("Fechar", null).create()
    popup?.let { showManagedDialog(it) }
}
