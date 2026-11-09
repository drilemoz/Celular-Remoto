package br.dev.celularremoto.ui

import android.os.Build
import android.view.View
import android.widget.LinearLayout

/** Corrige o layout sob as barras e recortes reais, inclusive no Android 16 (target 36). */
internal fun MainActivity.configureSystemBars() {
    val light = if (darkTheme()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
    window.decorView.systemUiVisibility = light or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
}

/** Padding é atribuído, não acumulado, evitando cortes e espaços duplicados. */
internal fun MainActivity.installSafeInsets(root: LinearLayout) {
    root.setOnApplyWindowInsetsListener { view, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or
                android.view.WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        } else {
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        insets
    }
}
