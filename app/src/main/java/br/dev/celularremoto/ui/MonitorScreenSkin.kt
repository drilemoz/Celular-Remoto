package br.dev.celularremoto.ui

import br.dev.celularremoto.R

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.dev.celularremoto.core.SettingsStore

/** Estilo único das telas especializadas. Não executa nenhum comando ou permissão. */
internal class MonitorScreenSkin(private val activity: Activity) {
    private val night = when (SettingsStore.theme(activity)) {
        "dark", "amoled" -> true
        "system" -> activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        else -> false
    }
    val background: Int = if (night) Color.rgb(14, 23, 35) else Color.rgb(247, 249, 252)
    val surface: Int = if (night) Color.rgb(28, 40, 55) else Color.WHITE
    val secondary: Int = if (night) Color.rgb(40, 55, 71) else Color.rgb(238, 244, 248)
    val foreground: Int = if (night) Color.rgb(240, 245, 251) else Color.rgb(25, 40, 54)
    val muted: Int = if (night) Color.rgb(174, 191, 204) else Color.rgb(99, 116, 131)
    val accent: Int = if (night) Color.rgb(74, 211, 187) else Color.rgb(21, 158, 142)
    val warning: Int = if (night) Color.rgb(245, 191, 110) else Color.rgb(176, 113, 31)
    val critical: Int = if (night) Color.rgb(255, 170, 174) else Color.rgb(180, 61, 70)
    fun dp(value: Int) = (activity.resources.displayMetrics.density * value).toInt()
    fun rounded(color: Int, radius: Int = 19, outline: Boolean = false) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (outline) setStroke(dp(1), if (night) Color.rgb(49, 64, 81) else Color.rgb(222, 231, 236))
    }
    fun label(value: String, size: Float, emphasis: Boolean = false): TextView = TextView(activity).apply {
        text = value
        textSize = size
        setTextColor(if (emphasis) this@MonitorScreenSkin.foreground else muted)
        includeFontPadding = false
        if (emphasis) setTypeface(null, Typeface.BOLD)
    }
    fun button(value: String, primary: Boolean = false, onClick: () -> Unit): Button = Button(activity).apply {
        text = value
        isAllCaps = false
        textSize = 13f
        minHeight = dp(48)
        minWidth = 0
        setTextColor(if (primary) Color.WHITE else this@MonitorScreenSkin.foreground)
        background = rounded(if (primary) accent else secondary, 15, !primary)
        elevation = 0f
        stateListAnimator = null
        setOnClickListener { onClick() }
    }
    fun badge(value: String, good: Boolean): TextView = TextView(activity).apply {
        text = value
        textSize = 11.5f
        setTextColor(if (good) accent else warning)
        setTypeface(null, Typeface.BOLD)
        background = rounded(if (good) secondary else if (night) Color.rgb(63, 50, 37) else Color.rgb(255, 247, 234), 99)
        setPadding(dp(12), dp(7), dp(12), dp(7))
    }
    fun header(icon: Int, title: String, description: String, onBack: () -> Unit): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val back = ImageView(activity).apply {
                setImageResource(R.drawable.ic_back)
                imageTintList = ColorStateList.valueOf(this@MonitorScreenSkin.foreground)
                background = rounded(secondary, 14)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(12), dp(12), dp(12), dp(12))
                isClickable = true
                isFocusable = true
                contentDescription = "Voltar"
                setOnClickListener { onBack() }
            }
            addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
            val symbol = ImageView(activity).apply {
                setImageResource(icon)
                imageTintList = ColorStateList.valueOf(accent)
                background = rounded(secondary, 15)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(12), dp(12), dp(12), dp(12))
                contentDescription = null
            }
            addView(symbol, LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(9) })
            val words = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(label(title, 20f, true))
                if (description.isNotBlank()) addView(label(description, 12f).apply {
                    maxLines = 2
                    setPadding(0, dp(4), 0, 0)
                })
            }
            addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        }
    fun card(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(surface, 20, true)
        setPadding(dp(15), dp(14), dp(15), dp(14))
    }
    fun applyWindow() {
        activity.window.statusBarColor = background
        activity.window.navigationBarColor = background
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = if (night) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}
