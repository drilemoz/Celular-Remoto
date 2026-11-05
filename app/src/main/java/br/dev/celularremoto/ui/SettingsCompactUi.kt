package br.dev.celularremoto.ui

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import br.dev.celularremoto.R

/** Elementos compactos da alpha.121. Apenas UI: nenhuma permissão é concedida e
 * nenhum comando remoto é executado implicitamente. */
internal fun MainActivity.settingsSubheading(text: String): TextView =
    label(text.uppercase(), 10.5f, strong = true, muted = true).apply {
        letterSpacing = 0.06f
        setPadding(0, dp(9), 0, dp(5))
    }

/** Linha condensada com descrição acessível completa, sem card individual. */
internal fun MainActivity.settingsSlimRow(
    icon: Int,
    title: String,
    detail: String = "",
    trailing: String = "›",
    onClick: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(56)
    setPadding(dp(5), dp(5), dp(6), dp(5))
    isClickable = true
    isFocusable = true
    contentDescription = listOf(title, detail, trailing).filter(String::isNotBlank).joinToString(". ")
    setOnClickListener { onClick() }
    addView(vectorIcon(icon, 20, highlight), LinearLayout.LayoutParams(dp(25), dp(25)))
    val words = LinearLayout(this@settingsSlimRow).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8), 0, dp(5), 0)
    }
    words.addView(label(title, 13.5f, strong = true).apply {
        setPadding(0, 0, 0, 0)
        maxLines = 2
    })
    if (detail.isNotBlank()) words.addView(label(detail, 11f, muted = true).apply {
        setPadding(0, dp(2), 0, 0)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    })
    addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    if (trailing.isNotBlank()) addView(label(trailing, 11f, muted = true).apply {
        maxLines = 1
        setPadding(dp(4), 0, 0, 0)
    })
}

internal fun MainActivity.settingsToggleRow(
    icon: Int,
    title: String,
    detail: String,
    value: Boolean,
    onToggle: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(60)
    setPadding(dp(5), dp(6), dp(5), dp(6))
    isClickable = true
    isFocusable = true
    contentDescription = "$title. ${if (value) "Ativado" else "Desativado"}. $detail"
    setOnClickListener { onToggle() }
    addView(vectorIcon(icon, 20, highlight), LinearLayout.LayoutParams(dp(25), dp(25)))
    val texts = LinearLayout(this@settingsToggleRow).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8), 0, dp(6), 0)
    }
    texts.addView(label(title, 13.5f, strong = true).apply { setPadding(0,0,0,0); maxLines = 2 })
    texts.addView(label(detail, 11f, muted = true).apply {
        setPadding(0,dp(2),0,0)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    })
    addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
    // O toque pertence à linha inteira. O controle é apenas o indicador visual;
    // a configuração só muda no callback acima, sem estado duplicado.
    addView(Switch(this@settingsToggleRow).apply {
        isChecked = value
        isClickable = false
        isFocusable = false
        showText = false
        thumbTintList = ColorStateList.valueOf(if (value) highlight else dim)
    }, LinearLayout.LayoutParams(-2, -2))
}

/** Coloca as ações secundárias lado a lado, mas mantém largura e toque mínimos. */
internal fun MainActivity.settingsActionPair(
    parent: LinearLayout,
    firstTitle: String,
    first: () -> Unit,
    secondTitle: String,
    second: () -> Unit,
    emphasizeFirst: Boolean = false
) {
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    row.addView(actionButton(firstTitle, emphasis = emphasizeFirst, onClick = first),
        LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(7) })
    row.addView(actionButton(secondTitle, onClick = second), LinearLayout.LayoutParams(0, dp(46), 1f))
    parent.addView(row)
}

internal fun MainActivity.settingsSummaryBand(title: String, note: String, icon: Int): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 14)
        setPadding(dp(11), dp(9), dp(11), dp(9))
        addView(vectorIcon(icon, 22, highlight))
        val texts = LinearLayout(this@settingsSummaryBand).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        texts.addView(label(title, 14f, strong = true).apply { setPadding(0,0,0,0) })
        texts.addView(label(note, 11f, muted = true).apply { setPadding(0,dp(3),0,0); maxLines = 3 })
        addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
    }
