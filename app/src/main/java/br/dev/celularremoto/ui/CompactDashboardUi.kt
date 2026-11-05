package br.dev.celularremoto.ui

import android.text.TextUtils
import android.view.Gravity
import android.widget.LinearLayout
import br.dev.celularremoto.R

/** Densidade visual compacta, sem estado, rede ou efeitos sobre permissões. */
internal fun MainActivity.compactTelemetryTile(
    icon: Int, title: String, value: String, detail: String,
    warning: Boolean = false, onClick: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    minimumHeight = dp(84)
    val fill = if (warning) {
        if (darkTheme()) android.graphics.Color.rgb(74, 47, 36)
        else android.graphics.Color.rgb(255, 242, 224)
    } else fieldColor
    background = shape(fill, 15)
    setPadding(dp(10), dp(9), dp(8), dp(9))
    isClickable = true
    isFocusable = true
    contentDescription = "$title: $value. $detail"
    setOnClickListener { onClick() }
    val heading = LinearLayout(this@compactTelemetryTile).apply {
        gravity = Gravity.CENTER_VERTICAL
    }
    heading.addView(vectorIcon(icon, 17, highlight), LinearLayout.LayoutParams(dp(18), dp(18)))
    heading.addView(label(title, 11f, muted = true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(5), 0, 0, 0)
    }, LinearLayout.LayoutParams(0, -2, 1f))
    addView(heading)
    addView(label(value, 17f, strong = true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(5), 0, 0)
    })
    addView(label(detail, 10.5f, muted = true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(3), 0, 0)
    })
}

/** Faixa de estados sem repetir cartões altos dentro de cartões maiores. */
internal fun MainActivity.compactNote(text: String): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(48)
    background = shape(fieldColor, 13)
    setPadding(dp(10), dp(7), dp(10), dp(7))
    addView(vectorIcon(R.drawable.ic_info, 17, dim), LinearLayout.LayoutParams(dp(18), dp(18)))
    addView(label(text, 11f, muted = true).apply {
        setPadding(dp(9), 0, 0, 0)
        maxLines = 3
    }, LinearLayout.LayoutParams(0, -2, 1f))
}
