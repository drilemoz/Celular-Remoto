package br.dev.celularremoto.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.content.res.ColorStateList

/** Componentes visuais reutilizáveis: estado, ações, avisos e diálogo central.
 * Não iniciam nenhuma operação remota por conta própria.
 */
internal fun MainActivity.visualHeader(icon: Int, title: String, subtitle: String = ""): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val badge = LinearLayout(this@visualHeader).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(if (darkTheme()) Color.rgb(35, 104, 101) else Color.rgb(222, 248, 242),
                    if (darkTheme()) Color.rgb(30, 73, 94) else Color.rgb(229, 243, 253))
            ).apply { cornerRadius = dp(18).toFloat() }
            addView(vectorIcon(icon, 26, highlight))
        }
        addView(badge, LinearLayout.LayoutParams(dp(54), dp(54)))
        val words = LinearLayout(this@visualHeader).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13), 0, 0, 0) }
        words.addView(label(title, 19f, strong = true).apply { setPadding(0,0,0,0); maxLines = 2 })
        if (subtitle.isNotBlank()) words.addView(label(subtitle, 12f, muted = true).apply {
            maxLines = 2; setPadding(0, dp(3),0,0)
        })
        addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    }

internal fun MainActivity.visualTag(value: String, kind: SettingsBadgeKind = SettingsBadgeKind.NEUTRAL): TextView =
    infoChip(value, accent = kind == SettingsBadgeKind.ACTIVE).apply {
        setTypeface(null, Typeface.BOLD)
        textSize = 11f
        minWidth = 0
        when (kind) {
            SettingsBadgeKind.WARNING -> {
                background = shape(if (darkTheme()) Color.rgb(78, 60, 29) else Color.rgb(255, 246, 224), 999)
                setTextColor(if (darkTheme()) Color.rgb(251, 208, 130) else Color.rgb(132, 83, 15))
            }
            SettingsBadgeKind.DANGER -> {
                background = shape(if (darkTheme()) Color.rgb(75, 43, 48) else Color.rgb(255, 236, 237), 999)
                setTextColor(if (darkTheme()) Color.rgb(255, 170, 173) else Color.rgb(166, 52, 61))
            }
            else -> Unit
        }
    }

internal fun MainActivity.visualActionRow(
    icon: Int, title: String, subtitle: String, tag: String, dangerous: Boolean = false,
    onClick: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(70)
    background = shape(fieldColor, 18, true)
    setPadding(dp(12), dp(11), dp(10), dp(11))
    isClickable = true
    isFocusable = true
    contentDescription = "$title. $subtitle. $tag"
    setOnClickListener { onClick() }
    val iconFrame = LinearLayout(this@visualActionRow).apply {
        gravity = Gravity.CENTER
        background = shape(if (dangerous) {
            if (darkTheme()) Color.rgb(79, 52, 42) else Color.rgb(255, 237, 225)
        } else surface, 14)
        addView(vectorIcon(icon, 22, if (dangerous) Color.rgb(198, 104, 44) else highlight))
    }
    addView(iconFrame, LinearLayout.LayoutParams(dp(42), dp(42)))
    val words = LinearLayout(this@visualActionRow).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
    words.addView(label(title, 14f, strong = true).apply { setPadding(0,0,0,0); maxLines = 1 })
    words.addView(label(subtitle, 11f, muted = true).apply {
        setPadding(0, dp(4), 0, 0); maxLines = 2
    })
    addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    addView(label("›", 23f, muted = true).apply { setPadding(dp(7),0,0,0) })
}

internal fun MainActivity.visualInfoPanel(icon: Int, title: String, body: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(if (darkTheme()) Color.rgb(50, 48, 42) else Color.rgb(255, 248, 233), 17)
        setPadding(dp(13), dp(12), dp(13), dp(12))
        val heading = LinearLayout(this@visualInfoPanel).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(vectorIcon(icon, 20, if (darkTheme()) Color.rgb(252, 205, 132) else Color.rgb(160, 109, 37)))
        heading.addView(label(title, 13f, strong = true).apply { setPadding(dp(9),0,0,0) })
        addView(heading)
        addView(label(body, 12f, muted = true).apply {
            setPadding(0, dp(7),0,0); setLineSpacing(dp(1).toFloat(), 1f)
        })
    }

/** Padrão consistente para conteúdo de diálogos legados. */
internal fun MainActivity.visualDialogContainer(): Pair<ScrollView, LinearLayout> =
    dialogWrapper().also { (_, body) ->
        body.background = shape(surface, 23)
        body.setPadding(dp(18), dp(18), dp(18), dp(17))
    }
