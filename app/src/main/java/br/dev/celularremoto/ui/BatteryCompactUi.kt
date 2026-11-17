package br.dev.celularremoto.ui

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import br.dev.celularremoto.R

/** Cartão único de bateria. A fonte aparece somente quando o Android a identifica. */
internal fun MainActivity.batteryHero(
    level: Int, status: String, source: String?, charging: Boolean, forecast: String?
): LinearLayout = panel(12).apply {
    val top = LinearLayout(this@batteryHero).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val main = LinearLayout(this@batteryHero).apply { orientation = LinearLayout.VERTICAL }
    main.addView(label(if (level in 0..100) "$level%" else "—", 28f, strong = true).apply {
        setPadding(0, 0, 0, 0)
        contentDescription = if (level in 0..100) "Nível da bateria: $level por cento" else "Nível da bateria indisponível"
    })
    val statusAndSource = listOfNotNull(status, source).joinToString(" · ")
    main.addView(label(statusAndSource, 12.5f, muted = true).apply { setPadding(0, dp(2), 0, 0) })
    top.addView(main, LinearLayout.LayoutParams(0, -2, 1f))
    top.addView(vectorIcon(if (charging) R.drawable.ic_battery_charging else R.drawable.ic_battery, 26, highlight),
        LinearLayout.LayoutParams(dp(26), dp(26)))
    addView(top)
    if (level in 0..100) {
        val bar = ProgressBar(this@batteryHero, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = level
            progressTintList = ColorStateList.valueOf(highlight)
            progressBackgroundTintList = ColorStateList.valueOf(lineColor)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(bar, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(6) })
    }
    if (forecast != null) {
        addView(label(forecast, 14f, strong = true).apply { setPadding(0, dp(6), 0, 0) })
    }
    sectionGap(this, 7)
    compactDivider(this)
    sectionGap(this, 6)
}

/** Métricas sem subcartões. Quebram linha com fonte grande em vez de cortar valores. */
internal fun MainActivity.batteryQuickMetric(heading: String, value: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(heading, 11f, muted = true).apply { setPadding(0, 0, 0, 0) })
        addView(label(value, 14f, strong = true).apply { setPadding(0, dp(2), 0, 0) })
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
        minimumHeight = dp(57)
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
