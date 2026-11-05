package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.widget.RadioButton
import android.content.res.ColorStateList
import android.widget.LinearLayout
import br.dev.celularremoto.R

/** Um único diálogo do aplicativo por Activity: evita menus e avisos empilhados. */
internal fun MainActivity.showManagedDialog(dialog: AlertDialog): AlertDialog {
    activeUiDialog?.takeIf { it !== dialog && it.isShowing }?.dismiss()
    activeUiDialog = dialog
    dialog.show()
    // Janelas com largura controlada, texto legível e botões neutros em qualquer tema.
    dialog.window?.setLayout(
        (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(520)),
        android.view.WindowManager.LayoutParams.WRAP_CONTENT
    )
    val actionTint = highlight
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
        isAllCaps = false
        setTextColor(actionTint)
        minimumHeight = dp(48)
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
        isAllCaps = false
        setTextColor(dim)
        minimumHeight = dp(48)
    }
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
        isAllCaps = false
        setTextColor(dim)
        minimumHeight = dp(48)
    }
    return dialog
}

internal fun MainActivity.showManagedDialog(builder: AlertDialog.Builder): AlertDialog =
    showManagedDialog(builder.create())

/** Diálogos compactos de tema único; alertas importantes mantêm confirmação explícita. */
private fun MainActivity.showThemedMessageDialog(
    title: String, description: String, icon: Int,
    primary: String, destructive: Boolean = false, secondary: String? = null,
    onPrimary: (() -> Unit)? = null,
) {
    val (wrapper, layout) = visualDialogContainer()
    layout.addView(visualHeader(icon, title))
    sectionGap(layout, 9)
    // Texto direto no diálogo; evita card dentro de card e rolagem desnecessária.
    layout.addView(label(description, 13f).apply {
        setPadding(dp(2), 0, dp(2), 0)
        setLineSpacing(dp(2).toFloat(), 1f)
    })
    sectionGap(layout, 12)
    lateinit var dialog: AlertDialog
    val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    if (secondary != null) {
        actions.addView(actionButton(secondary) { dialog.dismiss() },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(8) })
    }
    val confirmButton = actionButton(primary, emphasis = !destructive) {
        dialog.dismiss()
        onPrimary?.invoke()
    }
    if (destructive) {
        confirmButton.setTextColor(android.graphics.Color.rgb(188, 62, 55))
        confirmButton.background = shape(if (darkTheme()) android.graphics.Color.rgb(72, 41, 43)
            else android.graphics.Color.rgb(255, 236, 237), 16)
    }
    actions.addView(confirmButton, LinearLayout.LayoutParams(0, dp(46), 1f))
    layout.addView(actions)
    dialog = themedPopup(this).setView(wrapper).create()
    showManagedDialog(dialog)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(520)), -2)
}

internal fun MainActivity.showAppNotice(
    title: String,
    description: String,
    icon: Int = R.drawable.ic_info,
    buttonText: String = "Entendi",
) = showThemedMessageDialog(title, description, icon, buttonText)

internal fun MainActivity.showAppConfirm(
    title: String,
    description: String,
    icon: Int = R.drawable.ic_shield,
    confirmText: String,
    onConfirm: () -> Unit,
) = showThemedMessageDialog(title, description, icon, confirmText,
    destructive = title.contains("excluir", true) || title.contains("apagar", true) || title.contains("revogar", true),
    secondary = "Cancelar", onPrimary = onConfirm)

/** Seleção visual imediata: destacada e acessível, sem alterar a configuração ao abrir. */
internal fun MainActivity.showSettingsChoiceDialog(
    title: String,
    description: String,
    items: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    require(items.isNotEmpty())
    val (wrapper, layout) = visualDialogContainer()
    layout.addView(visualHeader(R.drawable.ic_settings, title, description))
    sectionGap(layout, 8)
    lateinit var chooser: AlertDialog
    items.forEachIndexed { index, (name, hint) ->
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = if (index == selected) shape(fieldColor, 12) else null
            minimumHeight = dp(52)
            setPadding(dp(10), dp(7), dp(9), dp(7))
            isClickable = true
            isFocusable = true
            contentDescription = "$name. $hint. ${if (index == selected) "Selecionado" else "Selecionar"}"
            setOnClickListener {
                chooser.dismiss()
                onSelect(index)
            }
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(label(name, 13.5f, strong = true).apply { setPadding(0, 0, 0, 0) })
        labels.addView(label(hint, 11f, muted = true).apply { setPadding(0, dp(3), 0, 0); maxLines = 2 })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val radio = RadioButton(this).apply {
            isChecked = index == selected
            isClickable = false
            isFocusable = false
            buttonTintList = ColorStateList.valueOf(highlight)
            contentDescription = null
        }
        row.addView(radio)
        layout.addView(row)
        if (index < items.lastIndex) compactDivider(layout, 7)
    }
    chooser = themedPopup(this).setView(wrapper).setNegativeButton("Cancelar", null).create()
    showManagedDialog(chooser)
}
