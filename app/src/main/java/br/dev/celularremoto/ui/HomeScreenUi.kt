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

/** Lista de aparelhos mobile-first: dados salvos não significam conexão ativa. */
internal fun MainActivity.renderHome() {
    val recentAccess = AuthenticatedAccess.recent()
    if (recentAccess.isNotEmpty() && SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning) {
        val access = mobileSurfaceCard()
        access.addView(iconMenuRow(R.drawable.ic_shield, "Acessos recentes",
            "${recentAccess.size} aparelho(s) consultaram este telefone") { navigateMainTab(0) })
        content.addView(access)
        gap(12)
    }

    val saved = RecentDevices.list(this)
    val devices = mobileSurfaceCard()
    devices.addView(mobileHeading(R.drawable.ic_phone, "Meus aparelhos"))
    sectionGap(devices, 12)
    if (saved.isEmpty()) {
        val empty = mobileWhiteCard().apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(21), dp(16), dp(21))
        }
        empty.addView(vectorIcon(R.drawable.ic_phone, 28, dim))
        empty.addView(label("Nenhum aparelho salvo", 15f, strong = true).apply { gravity = Gravity.CENTER })
        empty.addView(label("Conecte por QR Code ou informe o IP e a chave de pareamento.", 12f, muted = true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        devices.addView(empty)
    } else {
        saved.forEachIndexed { index, item ->
            if (index > 0) sectionGap(devices, 9)
            val device = mobileWhiteCard()
            val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            words.addView(label(item.name, 16f, strong = true).apply {
                maxLines = 2
                setPadding(0, 0, 0, 0)
            })
            val lastUsed = if (item.lastSuccessMs > 0L)
                "Última resposta: " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.lastSuccessMs))
                else "Aguardando primeira resposta"
            words.addView(label(lastUsed, 11.5f, muted = true).apply { setPadding(0, dp(5), 0, 0) })
            heading.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
            val menu = ImageView(this).apply {
                setImageResource(R.drawable.ic_settings)
                imageTintList = ColorStateList.valueOf(dim)
                setPadding(dp(11), dp(11), dp(11), dp(11))
                background = shape(fieldColor, 13, true)
                contentDescription = "Gerenciar ${item.name}"
                isClickable = true; isFocusable = true
                setOnClickListener { showHomeSavedDeviceOptions(item) }
            }
            heading.addView(menu, LinearLayout.LayoutParams(dp(48), dp(48)))
            device.addView(heading)
            sectionGap(device, 10)
            device.addView(actionButton("Abrir controles", onClick = { connectSavedDevice(item) }).apply {
                textSize = 14f
                minimumHeight = dp(50)
                background = shape(fieldColor, 15, true)
            }, LinearLayout.LayoutParams(-1, dp(50)))
            devices.addView(device)
        }
    }
    sectionGap(devices, 11)
    val addNew = actionButton("＋  Conectar novo aparelho") { showConnectionChooser() }.apply {
        textSize = 14f
        minHeight = dp(60)
        background = shape(surface, 18, true)
    }
    devices.addView(addNew, LinearLayout.LayoutParams(-1, dp(60)))
    content.addView(devices)
    gap(21)

    content.addView(label("Este telefone", 18f, strong = true).apply { setPadding(0, 0, 0, dp(12)) })
    val enabled = SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning
    val own = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 20, true)
        setPadding(dp(15), dp(16), dp(14), dp(16))
        isClickable = true; isFocusable = true
        contentDescription = "Monitoramento: ${if (enabled) "ativo" else "inativo"}. Gerenciar compartilhamento"
        setOnClickListener { navigateMainTab(0) }
    }
    own.addView(vectorIcon(R.drawable.ic_shield, 26, if (enabled) highlight else dim),
        LinearLayout.LayoutParams(dp(32), dp(32)))
    val ownText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
    ownText.addView(label("Monitoramento", 16.5f, strong = true).apply { setPadding(0, 0, 0, 0) })
    ownText.addView(label("Gerenciar compartilhamento", 12f, muted = true).apply { setPadding(0, dp(3), 0, 0) })
    own.addView(ownText, LinearLayout.LayoutParams(0, -2, 1f))
    own.addView(label("›", 27f, muted = true))
    content.addView(own)
}

private fun MainActivity.mobileSurfaceCard(): LinearLayout = panel(13).apply {
    background = shape(fieldColor, 23)
}

private fun MainActivity.mobileWhiteCard(): LinearLayout = panel(15).apply {
    background = shape(surface, 19)
}

private fun MainActivity.mobileHeading(icon: Int, title: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(vectorIcon(icon, 23, ink), LinearLayout.LayoutParams(dp(25), dp(25)))
        addView(label(title, 18f, strong = true).apply { setPadding(dp(10), 0, 0, 0) })
    }

/** Opções de um aparelho ficam em cartões com ícones, sem lista nativa simples. */
private fun MainActivity.showHomeSavedDeviceOptions(item: RecentDevices.Device) {
    val (wrapper, layout) = dialogWrapper()
    dialogIntro(layout, "${item.name} · ${item.host}")
    var popup: AlertDialog? = null
    layout.addView(iconMenuRow(R.drawable.ic_phone, "Conectar", "Abrir este aparelho salvo") {
        popup?.dismiss()
        connectSavedDevice(item)
    })
    sectionGap(layout, 9)
    layout.addView(iconMenuRow(R.drawable.ic_info, "Renomear", "Alterar o nome do aparelho salvo") {
        popup?.dismiss()
        renameSavedDevice(item, returnToChooser = false)
    })
    sectionGap(layout, 9)
    layout.addView(iconMenuRow(R.drawable.ic_logout, "Esquecer aparelho", "Remover credencial e histórico local", dangerous = true) {
        popup?.dismiss()
        forgetSavedDevice(item, returnToChooser = false)
    })
    popup = themedPopup(this)
        .setTitle("Gerenciar aparelho")
        .setView(wrapper)
        .setNegativeButton("Fechar", null)
        .create()
    popup?.let { showManagedDialog(it) }
}
