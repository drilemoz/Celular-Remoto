package br.dev.celularremoto.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.dev.celularremoto.core.SettingsStore

/** Action sheet visual compartilhada entre arquivos e aplicativos.
 *  O toque sempre fecha a folha antes de chamar a ação existente.
 *  Não executa comandos, não altera políticas nem inicia operações automaticamente.
 */
internal data class MonitorSheetAction(
    val icon: Int,
    val title: String,
    val detail: String = "",
    val destructive: Boolean = false,
    val invoke: () -> Unit,
)

internal fun Activity.showMonitorActionSheet(
    title: String,
    subtitle: String,
    actions: List<MonitorSheetAction>,
): AlertDialog {
    require(actions.isNotEmpty() && actions.size <= 40)
    val night = when (SettingsStore.theme(this)) {
        "dark" -> true
        "system" -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        else -> false
    }
    val density = resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt()
    val panel = if (night) Color.rgb(27, 40, 55) else Color.WHITE
    val field = if (night) Color.rgb(40, 55, 72) else Color.rgb(242, 246, 249)
    val ink = if (night) Color.rgb(238, 244, 251) else Color.rgb(22, 34, 48)
    val muted = if (night) Color.rgb(177, 190, 206) else Color.rgb(97, 112, 129)
    val highlight = if (night) Color.rgb(90, 214, 188) else Color.rgb(19, 156, 138)
    val red = if (night) Color.rgb(255, 171, 174) else Color.rgb(179, 56, 66)
    fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        maxLines = 3
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(14), dp(18), dp(14))
        background = shape(panel, 27)
    }
    val handle = View(this).apply { background = shape(if (night) Color.rgb(92, 107, 123) else Color.rgb(209, 218, 226), 99) }
    content.addView(handle, LinearLayout.LayoutParams(dp(44), dp(4)).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        bottomMargin = dp(18)
    })
    content.addView(text(title, 19f, ink, true))
    if (subtitle.isNotBlank()) content.addView(text(subtitle, 12.5f, muted).apply {
        setPadding(0, dp(7), 0, dp(11))
        ellipsize = android.text.TextUtils.TruncateAt.END
    }) else content.addView(View(this), LinearLayout.LayoutParams(1, dp(12)))
    val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = false }
    val options = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scroll.addView(options)
    content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    lateinit var dialog: AlertDialog
    actions.forEachIndexed { index, item ->
        val tone = if (item.destructive) red else highlight
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(if (item.detail.isBlank()) 56 else 69)
            setPadding(dp(11), dp(9), dp(11), dp(9))
            background = shape(field, 15)
            isClickable = true
            isFocusable = true
            contentDescription = listOf(item.title, item.detail).filter { it.isNotBlank() }.joinToString(". ")
            setOnClickListener { dialog.dismiss(); item.invoke() }
        }
        val picture = ImageView(this).apply {
            setImageResource(item.icon)
            imageTintList = ColorStateList.valueOf(tone)
            contentDescription = null
        }
        row.addView(picture, LinearLayout.LayoutParams(dp(24), dp(24)))
        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }
        words.addView(text(item.title, 14f, if (item.destructive) red else ink, true))
        if (item.detail.isNotBlank()) words.addView(text(item.detail, 11.5f, muted).apply {
            setPadding(0, dp(4), 0, 0)
        })
        row.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text("›", 23f, muted))
        options.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = if (index == actions.lastIndex) 0 else dp(7)
        })
    }
    val close = text("Fechar", 13.5f, highlight, true).apply {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        minimumHeight = dp(44)
        setOnClickListener { dialog.dismiss() }
    }
    content.addView(close, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(7) })
    dialog = themedPopup(this).setView(content).create()
    dialog.show()
    dialog.window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        setGravity(Gravity.BOTTOM)
        val wanted = dp(145 + actions.sumOf { if (it.detail.isBlank()) 63 else 78 })
        val ceiling = (resources.displayMetrics.heightPixels * .82f).toInt()
        setLayout(WindowManager.LayoutParams.MATCH_PARENT, wanted.coerceAtMost(ceiling))
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setDimAmount(.62f)
    }
    return dialog
}
