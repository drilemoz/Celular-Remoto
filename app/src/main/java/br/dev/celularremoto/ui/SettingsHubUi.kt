package br.dev.celularremoto.ui

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.content.pm.PackageManager
import android.Manifest
import br.dev.celularremoto.core.LocationShareStore
import android.view.View
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import br.dev.celularremoto.R
import java.text.Normalizer
import java.util.Locale

/** Alpha.88: índice pesquisável, sem mudar consentimentos nem abrir permissões automaticamente. */
private data class SettingsDestination(
    val icon: Int, val title: String, val hint: String, val section: String,
)
private data class SettingsGroup(val title: String, val entries: List<SettingsDestination>)

private val settingGroups = listOf(
    SettingsGroup("SEGURANÇA E PERMISSÕES", listOf(
        SettingsDestination(R.drawable.ic_shield, "Central de permissões", "Solicite todos os acessos e confira pendências", "permissions"),
        SettingsDestination(R.drawable.ic_shield, "Segurança e pareamento", "Chaves, dispositivos e revogação", "security"),
        SettingsDestination(R.drawable.ic_shield, "Bloqueio remoto", "Permissão e confirmação para bloquear", "lock"),
        SettingsDestination(R.drawable.ic_activity, "Diagnóstico", "Status do servidor e permissões", "diagnostics"),
        SettingsDestination(R.drawable.ic_activity, "Estabilidade da conexão", "Latência, quedas, reconexão e histórico", "connection"),
        SettingsDestination(R.drawable.ic_shield, "Modo avançado ADB", "Conexão ADB local e autorizada", "adb"),
    )),
    SettingsGroup("RECUPERAÇÃO E LOCALIZAÇÃO", listOf(
        SettingsDestination(R.drawable.ic_pin, "Localização", "Recuperação automática e GPS", "location"),
        SettingsDestination(R.drawable.ic_power, "Serviço e recuperação", "Inicialização, retomada e parada", "service"),
        SettingsDestination(R.drawable.ic_flashlight, "Som e lanterna", "Autorizações para localizar aparelho", "physical"),
    )),
    SettingsGroup("CÂMERA E TELA", listOf(
        SettingsDestination(R.drawable.ic_remote_camera, "Câmera e fotografia", "Permissão da câmera e sessão autorizada", "physical"),
        SettingsDestination(R.drawable.ic_monitor, "Tela e controle remoto", "Compartilhamento, toque e áudio", "screen"),
    )),
    SettingsGroup("ARQUIVOS E APLICATIVOS", listOf(
        SettingsDestination(R.drawable.ic_folder_download, "Arquivos e armazenamento", "Pasta fixa em Downloads", "files"),
        SettingsDestination(R.drawable.ic_phone, "Aplicativos instalados", "Compartilhamento da lista e abertura", "apps"),
    )),
    SettingsGroup("ALERTAS E COMUNICAÇÃO", listOf(
        SettingsDestination(R.drawable.ic_bell, "Notificações", "Acesso, seleção e respostas", "notifications"),
        SettingsDestination(R.drawable.ic_message, "SMS e telefonia", "Consulta, envio e chips", "sms"),
        SettingsDestination(R.drawable.ic_thermometer, "Alertas e proteção", "Bateria, temperatura e saúde", "health"),
    )),
    SettingsGroup("APARÊNCIA E APLICATIVO", listOf(
        SettingsDestination(R.drawable.ic_folder_download, "Atualizações", "GitHub Releases, download automático e instalação local", "updates"),
        SettingsDestination(R.drawable.ic_palette, "Aparência", "Temas claro, escuro e sistema", "appearance"),
        SettingsDestination(R.drawable.ic_info, "Novidades da versão", "Mudanças nesta atualização", "news"),
        SettingsDestination(R.drawable.ic_info, "Sobre", "Versão e privacidade", "about"),
    )),
)

private fun folded(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

// Evita normalizar a mesma lista de opções em cada caractere pesquisado.
private val settingSearchIndex by lazy {
    settingGroups.associate { group -> group.title to group.entries.associateWith { entry ->
        folded("${group.title} ${entry.title} ${entry.hint}")
    } }
}

/** Índice compacto e pesquisável, com estado verdadeiro das permissões locais. */
internal fun MainActivity.renderSettingsHome() {
    // A página inicial apresenta categorias/atalhos. Os 20 itens completos aparecem
    // apenas após selecionar uma categoria ou pesquisar, evitando rolagem obrigatória.
    val pending = PermissionCenter.pending(this).size
    val permissionsStrip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(if (darkTheme()) android.graphics.Color.rgb(37, 54, 53)
            else android.graphics.Color.rgb(230, 247, 242), 14)
        setPadding(dp(11), dp(8), dp(9), dp(8))
        minimumHeight = dp(54)
        isClickable = true
        isFocusable = true
        contentDescription = "Central de permissões: $pending pendentes. Abrir"
        setOnClickListener { openSettingsSection("permissions") }
    }
    permissionsStrip.addView(vectorIcon(R.drawable.ic_shield, 21, highlight))
    val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
    words.addView(label("Permissões do aparelho", 13f, strong = true).apply { setPadding(0, 0, 0, 0) })
    words.addView(label(if (pending > 0) "$pending pendência(s) · toque para revisar" else "Acessos solicitáveis revisados",
        11f, muted = true).apply { setPadding(0, dp(2), 0, 0) })
    permissionsStrip.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    permissionsStrip.addView(label("›", 21f, muted = true))
    content.addView(permissionsStrip)
    sectionGap(content, 14)

    val searchRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 14)
        setPadding(dp(11), 0, dp(3), 0)
    }
    searchRow.addView(vectorIcon(R.drawable.ic_search, 19, dim))
    val search = EditText(this).apply {
        hint = "Buscar configuração"
        textSize = 14f
        setTextColor(ink)
        setHintTextColor(dim)
        background = null
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        isSingleLine = true
        setPadding(dp(10), dp(5), dp(4), dp(5))
        contentDescription = "Pesquisar configurações"
    }
    searchRow.addView(search, LinearLayout.LayoutParams(0, dp(48), 1f))
    val clear = TextView(this).apply {
        text = "✕"
        textSize = 17f
        setTextColor(dim)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = "Limpar pesquisa"
        visibility = View.GONE
        setOnClickListener { search.text?.clear() }
    }
    searchRow.addView(clear, LinearLayout.LayoutParams(dp(48), dp(48)))
    content.addView(searchRow)
    sectionGap(content, 15)
    content.addView(compactSectionTitle("Categorias"))
    sectionGap(content, 8)
    val categoryGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    content.addView(categoryGrid)
    sectionGap(content, 15)
    val resultHeader = compactSectionTitle("Acesso rápido")
    content.addView(resultHeader)
    sectionGap(content, 7)
    val resultCount = label("", 11f, muted = true).apply { setPadding(0, 0, 0, dp(6)) }
    content.addView(resultCount)
    val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    content.addView(results)

    var selectedGroup: String? = null
    var renderedKey = ""
    fun openItem(item: SettingsDestination) {
        if (item.section == "adb") startActivity(Intent(this, AdbActivity::class.java))
        else openSettingsSection(item.section)
    }
    lateinit var refresh: (String) -> Unit
    refresh = { raw ->
        val normalized = folded(raw)
        val key = "$normalized|$selectedGroup"
        if (key != renderedKey) {
            renderedKey = key
            clear.visibility = if (raw.isNotBlank()) View.VISIBLE else View.GONE
            categoryGrid.removeAllViews()
            compactActionGrid(categoryGrid, settingGroups.map { group ->
                val label = when (group.title) {
                    "SEGURANÇA E PERMISSÕES" -> "Segurança"
                    "RECUPERAÇÃO E LOCALIZAÇÃO" -> "Conexão e GPS"
                    "CÂMERA E TELA" -> "Câmera e tela"
                    "ARQUIVOS E APLICATIVOS" -> "Arquivos e apps"
                    "ALERTAS E COMUNICAÇÃO" -> "Alertas"
                    else -> "Aplicativo"
                }
                compactActionTile(group.entries.first().icon, label, group.title,
                    selected = normalized.isBlank() && group.title == selectedGroup) {
                    selectedGroup = if (selectedGroup == group.title) null else group.title
                    refresh(search.text?.toString().orEmpty())
                }
            })
            results.removeAllViews()
            val quick = setOf("permissions", "connection", "updates", "appearance", "health")
            val matches = settingGroups.flatMap { group ->
                group.entries.filter { item -> when {
                    normalized.isNotBlank() -> settingSearchIndex[group.title]?.get(item)?.contains(normalized) == true
                    selectedGroup != null -> group.title == selectedGroup
                    else -> item.section in quick
                } }
            }
            val heading = when {
                normalized.isNotBlank() -> "Resultados da busca"
                selectedGroup != null -> when (selectedGroup) {
                    "SEGURANÇA E PERMISSÕES" -> "Segurança e permissões"
                    "RECUPERAÇÃO E LOCALIZAÇÃO" -> "Recuperação e localização"
                    "CÂMERA E TELA" -> "Câmera e tela"
                    "ARQUIVOS E APLICATIVOS" -> "Arquivos e aplicativos"
                    "ALERTAS E COMUNICAÇÃO" -> "Alertas e comunicação"
                    else -> "Aparência e aplicativo"
                }
                else -> "Acesso rápido"
            }
            (resultHeader.getChildAt(0) as? TextView)?.text = heading
            resultCount.text = if (normalized.isNotBlank()) "${matches.size} resultado(s)"
                else if (selectedGroup == null) "Selecione uma categoria para ver todas as opções"
                else "${matches.size} opções nesta categoria"
            val list = panel(4).apply { background = shape(surface, 15, true) }
            matches.forEachIndexed { index, item ->
                val entry = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(58)
                    setPadding(dp(10), dp(5), dp(9), dp(5))
                    isClickable = true
                    isFocusable = true
                    contentDescription = "${item.title}. ${item.hint}"
                    setOnClickListener { openItem(item) }
                }
                entry.addView(vectorIcon(item.icon, 20, highlight), LinearLayout.LayoutParams(dp(25), dp(25)))
                val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), 0, dp(4), 0) }
                texts.addView(label(item.title, 13.5f, strong = true).apply {
                    setPadding(0, 0, 0, 0)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                texts.addView(label(item.hint, 10.5f, muted = true).apply {
                    setPadding(0, dp(2), 0, 0)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                entry.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
                entry.addView(label("›", 20f, muted = true))
                list.addView(entry)
                if (index < matches.lastIndex) compactDivider(list, 43)
            }
            if (matches.isEmpty()) list.addView(label("Nenhuma opção encontrada. Tente outro termo.", 12.5f, muted = true))
            results.addView(list)
        }
    }
    val pendingSearch = Runnable {
        if (results.isAttachedToWindow) refresh(search.text?.toString().orEmpty())
    }
    search.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            search.removeCallbacks(pendingSearch)
            search.postDelayed(pendingSearch, 120L)
        }
        override fun afterTextChanged(s: Editable?) = Unit
    })
    search.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) { search.removeCallbacks(pendingSearch) }
    })
    refresh("")
}
