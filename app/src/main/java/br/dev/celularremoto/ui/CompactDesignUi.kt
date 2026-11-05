package br.dev.celularremoto.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.dev.celularremoto.R

/** Componentes de apresentação, sem chamadas de rede nem alterações de permissões.
 * Três colunas só onde cabem títulos curtos, com adaptação à fonte ampliada.
 */
internal object CompactDesignPolicy {
    fun columns(widthDp: Int, fontScale: Float): Int = when {
        fontScale > 1.20f || widthDp < 360 -> 2
        widthDp >= 600 -> 4
        else -> 3
    }
}

internal fun MainActivity.compactColumns(): Int =
    CompactDesignPolicy.columns(resources.configuration.screenWidthDp, resources.configuration.fontScale)

/** Atalho grande o bastante para toque e com rótulo acessível completo. */
internal fun MainActivity.compactActionTile(
    icon: Int, title: String, detail: String = "", selected: Boolean = false, onClick: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    minimumHeight = dp(76)
    background = shape(if (selected) highlight else fieldColor, 14, !selected)
    setPadding(dp(5), dp(9), dp(5), dp(8))
    isClickable = true
    isFocusable = true
    contentDescription = listOf(title, detail).filter { it.isNotBlank() }.joinToString(". ")
    setOnClickListener { onClick() }
    addView(vectorIcon(icon, 22, if (selected) highlightText else highlight),
        LinearLayout.LayoutParams(dp(22), dp(22)).apply { gravity = Gravity.CENTER_HORIZONTAL })
    addView(label(title, 11.5f, strong = true).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        includeFontPadding = false
        setTextColor(if (selected) highlightText else ink)
        setPadding(dp(2), dp(5), dp(2), 0)
    })
}

/** Grade adaptativa; nunca deixa a última linha de um item ocupando largura toda. */
internal fun MainActivity.compactActionGrid(
    parent: LinearLayout, tiles: List<LinearLayout>, preferredColumns: Int = compactColumns()
) {
    val columns = preferredColumns.coerceIn(1, 4)
    tiles.chunked(columns).forEachIndexed { rowIndex, chunk ->
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        repeat(columns) { index ->
            val item = chunk.getOrNull(index) ?: View(this)
            row.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index != columns - 1) rightMargin = dp(7)
            })
        }
        parent.addView(row)
        if (rowIndex < (tiles.size - 1) / columns) sectionGap(parent, 7)
    }
}

internal fun MainActivity.compactSectionTitle(title: String, trailing: String = ""): LinearLayout =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        addView(label(title, 16f, strong = true).apply {
            maxLines = 2
            setPadding(0, 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (trailing.isNotBlank()) addView(label(trailing, 11f, muted = true).apply {
            maxLines = 1
            setPadding(dp(5), 0, 0, 0)
        })
    }

internal fun MainActivity.compactDivider(parent: LinearLayout, insetDp: Int = 0) {
    parent.addView(View(this).apply { setBackgroundColor(lineColor) },
        LinearLayout.LayoutParams(-1, dp(1)).apply { leftMargin = dp(insetDp) })
}
