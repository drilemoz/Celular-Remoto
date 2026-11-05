package br.dev.celularremoto.ui

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import br.dev.celularremoto.R

/** Alpha.120: apresentação apenas. A coleta, a autenticação e os cálculos permanecem nos módulos existentes. */
internal fun MainActivity.batteryHero(level: Int, status: String, source: String): LinearLayout = panel(12).apply {
    background = shape(fieldColor, 17)
    val top = LinearLayout(this@batteryHero).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val main = LinearLayout(this@batteryHero).apply { orientation = LinearLayout.VERTICAL }
    main.addView(label(if (level in 0..100) "$level%" else "—", 30f, strong = true).apply {
        setPadding(0, 0, 0, 0)
        contentDescription = if (level in 0..100) "Nível da bateria: $level por cento" else "Nível da bateria indisponível"
    })
    main.addView(label(status, 12.5f, muted = true).apply { setPadding(0, dp(2), 0, 0) })
    top.addView(main, LinearLayout.LayoutParams(0, -2, 1f))
    top.addView(vectorIcon(R.drawable.ic_battery_charging, 29, highlight),
        LinearLayout.LayoutParams(dp(29), dp(29)))
    addView(top)
    val bar = ProgressBar(this@batteryHero, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progress = level.coerceIn(0, 100)
        progressTintList = ColorStateList.valueOf(highlight)
        progressBackgroundTintList = ColorStateList.valueOf(lineColor)
        contentDescription = if (level in 0..100) "Bateria em $level por cento" else "Bateria sem leitura válida"
    }
    addView(bar, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(11) })
    addView(label("Fonte: $source", 10.5f, muted = true).apply { setPadding(0, dp(5), 0, 0) })
}

internal fun MainActivity.batteryQuickMetric(icon: Int, heading: String, value: String, detail: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        minimumHeight = dp(78)
        background = shape(fieldColor, 13)
        setPadding(dp(8), dp(9), dp(6), dp(8))
        contentDescription = "$heading: $value. $detail"
        val row = LinearLayout(this@batteryQuickMetric).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(vectorIcon(icon, 15, highlight), LinearLayout.LayoutParams(dp(15), dp(15)))
        row.addView(label(heading, 10.5f, muted = true).apply {
            setPadding(dp(3), 0, 0, 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(row)
        addView(label(value, 16f, strong = true).apply {
            setPadding(0, dp(5), 0, 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        addView(label(detail, 10f, muted = true).apply {
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
    }

/** Três colunas em larguras usuais, duas quando a fonte ou a tela exigirem mais espaço. */
internal fun MainActivity.batteryMetricGrid(parent: LinearLayout, items: List<LinearLayout>) {
    val columns = BatteryDashboardPolicy.metricColumns(
        resources.configuration.screenWidthDp, resources.configuration.fontScale
    )
    items.chunked(columns).forEachIndexed { index, chunk ->
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        repeat(columns) { column ->
            row.addView(chunk.getOrNull(column) ?: View(this), LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (column < columns - 1) rightMargin = dp(5)
            })
        }
        parent.addView(row)
        if (index < (items.size - 1) / columns) sectionGap(parent, 6)
    }
}

internal fun MainActivity.batteryInlineAction(
    icon: Int, title: String, detail: String = "", onClick: () -> Unit
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(49)
    isClickable = true
    isFocusable = true
    background = shape(fieldColor, 12)
    contentDescription = listOf(title, detail).filter { it.isNotBlank() }.joinToString(". ")
    setOnClickListener { onClick() }
    setPadding(dp(10), dp(6), dp(9), dp(6))
    addView(vectorIcon(icon, 18, highlight), LinearLayout.LayoutParams(dp(19), dp(19)))
    val texts = LinearLayout(this@batteryInlineAction).apply { orientation = LinearLayout.VERTICAL }
    texts.addView(label(title, 12.5f, strong = true).apply { setPadding(0, 0, 0, 0) })
    if (detail.isNotBlank()) texts.addView(label(detail, 10.5f, muted = true).apply {
        setPadding(0, dp(2), 0, 0)
        maxLines = 2
    })
    addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(9) })
    addView(label("›", 18f, muted = true).apply { setPadding(0, 0, 0, 0) })
}

/** Uma lista com separadores, em vez de um grande cartão individual para cada sessão. */
internal fun MainActivity.batterySessionListRow(
    parent: LinearLayout, icon: Int, title: String, summary: String,
    onClick: () -> Unit
) {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(57)
        setPadding(dp(3), dp(6), dp(3), dp(6))
        isClickable = true
        isFocusable = true
        contentDescription = "$title. $summary"
        setOnClickListener { onClick() }
    }
    row.addView(vectorIcon(icon, 19, highlight), LinearLayout.LayoutParams(dp(20), dp(20)))
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    column.addView(label(title, 12.5f, strong = true).apply {
        setPadding(0, 0, 0, 0)
        maxLines = 2
    })
    column.addView(label(summary, 10.5f, muted = true).apply {
        setPadding(0, dp(3), 0, 0)
        maxLines = 2
    })
    row.addView(column, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(9) })
    row.addView(label("›", 17f, muted = true).apply { setPadding(0, 0, 0, 0) })
    if (parent.childCount > 0) compactDivider(parent)
    parent.addView(row)
}

internal fun MainActivity.batteryKeyValue(labelText: String, value: String): LinearLayout = LinearLayout(this).apply {
    gravity = Gravity.CENTER_VERTICAL
    orientation = LinearLayout.HORIZONTAL
    minimumHeight = dp(34)
    addView(label(labelText, 11.5f, muted = true).apply {
        setPadding(0, dp(3), dp(6), dp(3))
    }, LinearLayout.LayoutParams(0, -2, 1f))
    addView(label(value, 12.5f, strong = true).apply {
        setPadding(0, dp(3), 0, dp(3))
    })
}
