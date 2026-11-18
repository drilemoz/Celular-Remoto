package br.dev.celularremoto.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import br.dev.celularremoto.R
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService

/** Fica no quadro de conteúdo, acima da navegação e dentro dos insets da Activity. */
internal fun MainActivity.withFloatingFunctionSearch(scroll: ScrollView): FrameLayout =
    FrameLayout(this).apply {
        addView(scroll, FrameLayout.LayoutParams(-1, -1))
        // Garante que as últimas linhas possam subir totalmente acima da lupa.
        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight,
            maxOf(content.paddingBottom, dp(68)))
        val launcher = ImageButton(this@withFloatingFunctionSearch).apply {
            setImageResource(R.drawable.ic_search)
            setImageTintList(ColorStateList.valueOf(highlightText))
            background = RippleDrawable(ColorStateList.valueOf(highlightText and 0x33ffffff),
                shape(highlight, 24), null)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            elevation = dp(5).toFloat()
            contentDescription = "Pesquisar funções do aplicativo"
            tooltipText = "Pesquisar funções"
            setOnClickListener { showAppFunctionSearch() }
        }
        addView(launcher, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.BOTTOM or Gravity.END).apply {
            marginEnd = dp(12)
            bottomMargin = dp(10)
        })
    }

private fun MainActivity.showAppFunctionSearch() {
    val body = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(8), dp(16), 0)
    }
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 14, true)
    }
    row.addView(vectorIcon(R.drawable.ic_search, 20, dim),
        LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(12) })
    val field = field("Ex.: chamadas, painel solar, tema", "").apply {
        background = null
        contentDescription = "Digite o nome de uma função"
        imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        setSingleLine(true)
        filters = arrayOf(InputFilter.LengthFilter(120))
        setPadding(dp(10), dp(8), dp(4), dp(8))
    }
    row.addView(field, LinearLayout.LayoutParams(0, -2, 1f))
    val clear = TextView(this).apply {
        text = "×"
        setTextColor(dim)
        textSize = 24f
        gravity = Gravity.CENTER
        contentDescription = "Limpar pesquisa"
        isFocusable = true
        visibility = View.INVISIBLE
        setOnClickListener { field.text.clear(); field.requestFocus() }
    }
    row.addView(clear, LinearLayout.LayoutParams(dp(48), dp(48)))
    body.addView(row)
    val summary = label("", 11.5f, muted = true)
    body.addView(summary)

    val list = ListView(this).apply {
        divider = null
        clipToPadding = false
        isNestedScrollingEnabled = false
    }
    val adapter = FunctionResultAdapter(this)
    list.adapter = adapter
    body.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
    val dialog = themedPopup(this).setTitle("Pesquisar funções").setView(body)
        .setNegativeButton("Fechar", null).create()
    fun update(raw: String) {
        adapter.items = AppFunctionCatalog.search.find(raw)
        adapter.notifyDataSetChanged()
        list.setSelection(0)
        clear.visibility = if (raw.isBlank()) View.INVISIBLE else View.VISIBLE
        summary.text = when {
            raw.isBlank() -> "Todas as funções · ${adapter.count} atalhos"
            adapter.count == 0 -> "Nenhuma função encontrada. Tente outras palavras."
            adapter.count == 1 -> "1 função encontrada"
            else -> "${adapter.count} funções encontradas"
        }
    }
    fun open(item: AppFunction) {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(field.windowToken, 0)
        dialog.dismiss()
        openAppFunction(item.destination)
    }
    list.setOnItemClickListener { _, _, position, _ -> adapter.items.getOrNull(position)?.let { open(it) } }
    field.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            update(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) = Unit
    })
    field.setOnEditorActionListener { _, action, _ ->
        if (action != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
        if (adapter.count == 1) open(adapter.items.first())
        else getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(field.windowToken, 0)
        true
    }
    update("")
    showManagedDialog(dialog)
    val metrics = resources.displayMetrics
    dialog.window?.apply {
        setLayout((metrics.widthPixels * .94f).toInt().coerceAtMost(dp(520)),
            (metrics.heightPixels * .78f).toInt().coerceAtMost(dp(545)))
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    field.requestFocus()
    field.post {
        if (dialog.isShowing) getSystemService(InputMethodManager::class.java)
            ?.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
    }
}

private class FunctionResultAdapter(private val activity: MainActivity) : BaseAdapter() {
    var items: List<AppFunction> = emptyList()
    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    private class Row(activity: MainActivity) : LinearLayout(activity) {
        val title = activity.label("", 14f, strong = true)
        val path = activity.label("", 11f, muted = true)
        val bullet = activity.vectorIcon(R.drawable.ic_search, 16, activity.highlight)
        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.dp(54)
            setPadding(activity.dp(5), activity.dp(6), activity.dp(5), activity.dp(6))
            bullet.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(bullet, LayoutParams(activity.dp(19), activity.dp(19)))
            val text = LinearLayout(activity).apply {
                orientation = VERTICAL
                title.setPadding(0, 0, 0, 0)
                path.setPadding(0, activity.dp(2), 0, 0)
                addView(title)
                addView(path)
            }
            addView(text, LayoutParams(0, -2, 1f).apply { marginStart = activity.dp(10) })
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView as? Row ?: Row(activity)
        val item = getItem(position)
        row.title.text = item.title
        row.bullet.setImageResource(when (item.area) {
            FunctionArea.CONTROL -> R.drawable.ic_phone
            FunctionArea.MONITOR -> R.drawable.ic_shield
            FunctionArea.BATTERY -> R.drawable.ic_battery
            FunctionArea.SETTINGS -> R.drawable.ic_settings
        })
        val currentPath = if (item.path == item.area.title) item.path else "${item.area.title} › ${item.path}"
        row.path.text = currentPath
        row.contentDescription = "${item.title}. $currentPath"
        return row
    }
}

/** Realça a linha, sem executar seu clique (ajustes, capturas e comandos mantêm confirmação). */
internal fun MainActivity.focusFunctionRow(anchors: List<String>) {
    if (anchors.isEmpty()) return
    val currentMode = mode
    val currentSection = settingsSection
    val scroll = content.parent as? ScrollView ?: return
    fun showRow() {
        if (currentMode != mode || currentSection != settingsSection || !scroll.isAttachedToWindow) return
        fun allText(view: View): Sequence<TextView> = sequence {
            if (view is TextView) yield(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(allText(view.getChildAt(i)))
        }
        val visibleRows = allText(content).filter { it.isShown }.toList()
        val target = anchors.firstNotNullOfOrNull { anchor ->
            val folded = AppFunctionSearch.fold(anchor)
            visibleRows.firstOrNull { AppFunctionSearch.fold(it.text.toString()) == folded }
        } ?: return
        val rect = Rect()
        target.getDrawingRect(rect)
        scroll.offsetDescendantRectToMyCoords(target, rect)
        scroll.smoothScrollTo(0, (rect.top - dp(14)).coerceAtLeast(0))
        target.setTextColor(highlight)
        if (getSystemService(AccessibilityManager::class.java)?.isEnabled == true)
            target.announceForAccessibility(target.text)
    }
    val observer = scroll.viewTreeObserver
    if (!observer.isAlive) return
    observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (observer.isAlive) observer.removeOnPreDrawListener(this)
            showRow()
            return true
        }
    })
    scroll.invalidate()
}

private fun MainActivity.openAppFunction(destination: FunctionDestination) {
    when (destination) {
        is FunctionDestination.Controller -> {
            if (mode != 1 && !(mode == 4 && destination.tool == ControllerTool.MESSAGES)) navigateMainTab(1)
            openControllerTool(destination.tool)
        }
        is FunctionDestination.Settings -> {
            if (mode == 4) clearRemoteMessagesScreen()
            if (mode != 3) openSettingsModule()
            openSettingsSection(destination.section)
            focusFunctionRow(destination.anchors)
        }
        is FunctionDestination.MonitorRow -> {
            navigateMainTab(0)
            focusFunctionRow(destination.anchors)
        }
        is FunctionDestination.Battery -> openBatterySearchDestination(destination)
        is FunctionDestination.Shortcut -> openFunctionShortcut(destination.target)
    }
}

private fun MainActivity.openFunctionShortcut(shortcut: SearchShortcut) {
    val host = SettingsStore.getRemoteHost(this)
    when (shortcut) {
        SearchShortcut.CONTROL -> navigateMainTab(1)
        SearchShortcut.MONITOR -> navigateMainTab(0)
        SearchShortcut.PAIR -> { navigateMainTab(1); showConnectionChooser() }
        SearchShortcut.RENAME_DEVICE -> {
            navigateMainTab(1)
            RecentDevices.list(this).firstOrNull { it.host == host }
                ?.let { renameSavedDevice(it, returnToChooser = false) } ?: showConnectionChooser()
        }
        SearchShortcut.DEVICE_DETAILS -> {
            navigateMainTab(1)
            val current = remoteConnection.cached?.takeIf { remoteConnection.cachedHost == host &&
                host.isNotBlank() && RecentDevices.key(this, host) != null }
            if (current != null) showRemoteDetailsDialog(current)
            else focusFunctionRow(listOf("Detalhes", "Atualizar", "Conectar"))
        }
        SearchShortcut.CONNECTION_DIAGNOSTICS -> {
            if (mode == 4) clearRemoteMessagesScreen()
            openConnectionDiagnostics()
        }
        SearchShortcut.CALLS -> {
            if (mode != 4) navigateMainTab(1)
            showRemoteCallHistory()
        }
        SearchShortcut.MESSAGE_NOTICES -> {
            if (mode != 4) {
                navigateMainTab(1)
                if (host.isBlank() || RecentDevices.key(this, host) == null) { showConnectionChooser(); return }
                openRemoteMessagesScreen()
            }
            focusFunctionRow(listOf("Ativar avisos em tempo real", "Avisos em tempo real: ativos"))
        }
        SearchShortcut.SHOW_MONITOR_INFO -> { navigateMainTab(0); showMyConnectionInfo() }
        SearchShortcut.GENERATE_QR -> {
            navigateMainTab(0)
            if (SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning) chooseQrAddress()
            else focusFunctionRow(listOf("Ativar", "Retomar", "Monitoramento local"))
        }
        SearchShortcut.PREPARE_PHOTO -> { navigateMainTab(0); startActivity(Intent(this, RemotePhotoSetupActivity::class.java)) }
        SearchShortcut.ADB -> startActivity(Intent(this, AdbActivity::class.java))
    }
}
