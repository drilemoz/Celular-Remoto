package br.dev.celularremoto.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.dev.celularremoto.R

/** Estrutura mobile-first: cabeçalho simples e cinco destinos principais fixos.
 * Os botões apenas navegam; nenhum serviço ou operação remota é iniciado automaticamente.
 */
internal fun MainActivity.renderMobileHeader() {
    val header = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(54)
    }
    if (mode == 3 && settingsSection != "home") {
        header.addView(headerIconButton(R.drawable.ic_back, "Voltar às categorias") {
            openSettingsSection("home")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(View(this), LinearLayout.LayoutParams(dp(7), 1))
    }
    val headings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val mainTitle = when (mode) {
        1 -> "Controlar"
        0 -> "Monitorado"
        5 -> "Bateria"
        3 -> "Ajustes"
        else -> "Celular Remoto"
    }
    headings.addView(label(mainTitle, if (mode == 2) 23f else 22f, strong = true).apply {
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, 0, 0, 0)
    })
    headings.addView(label(when (mode) {
        1 -> "Painel do aparelho remoto"
        0 -> "Compartilhamento deste telefone"
        5 -> "Corrente, testes solares e histórico"
        3 -> "Preferências e permissões"
        else -> "Controle seguro dos seus aparelhos"
    }, 12.5f, muted = true).apply {
        maxLines = 2
        setPadding(0, dp(3), 0, 0)
    })
    header.addView(headings, LinearLayout.LayoutParams(0, -2, 1f))
    if (mode != 3) {
        header.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        header.addView(headerIconButton(R.drawable.ic_settings, "Abrir ajustes") { openSettingsModule() },
            LinearLayout.LayoutParams(dp(48), dp(48)))
    }
    content.addView(header)
    sectionGap(content, 11)
}

internal fun MainActivity.renderBottomNavigation() {
    bottomNavigation.removeAllViews()
    bottomNavigation.visibility = if (mode == 4) View.GONE else View.VISIBLE
    if (mode == 4) return
    val entries = listOf(
        Triple(2, R.drawable.ic_remote_home, "Início"),
        Triple(1, R.drawable.ic_monitor, "Controlar"),
        Triple(0, R.drawable.ic_shield, "Monitorado"),
        Triple(5, R.drawable.ic_battery, "Bateria"),
        Triple(3, R.drawable.ic_settings, "Ajustes"),
    )
    val navHeight = AppearancePolicy.navigationHeightDp(resources.configuration.fontScale)
    entries.forEach { (destination, icon, title) ->
        val selected = mode == destination
        val tab = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(navHeight)
            background = if (selected) shape(fieldColor, 13) else null
            isClickable = true
            isFocusable = true
            contentDescription = "$title${if (selected) ", selecionado" else ""}"
            setOnClickListener { navigateMainTab(destination) }
        }
        tab.addView(vectorIcon(icon, 22, if (selected) ink else dim),
            LinearLayout.LayoutParams(dp(22), dp(22)))
        tab.addView(label(title, 10.5f, strong = selected, muted = !selected).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(0, dp(5), 0, 0)
            maxLines = 2
            ellipsize = null
            gravity = Gravity.CENTER
        })
        bottomNavigation.addView(tab, LinearLayout.LayoutParams(0, dp(navHeight), 1f).apply {
            if (destination != 3) rightMargin = dp(2)
        })
    }
}
