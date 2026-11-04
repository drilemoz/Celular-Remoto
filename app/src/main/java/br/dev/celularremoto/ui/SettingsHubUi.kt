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
    val header = panel(14)
    header.addView(visualHeader(R.drawable.ic_settings, "Configurações", "Controle o que este aparelho compartilha"))
    sectionGap(header, 12)
    val overview = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(fieldColor, 16)
        setPadding(dp(11), dp(10), dp(11), dp(10))
    }
    overview.addView(label("PERMISSÕES DO ANDROID", 10.5f, strong = true, muted = true).apply {
        setPadding(0, 0, 0, dp(7))
    })
    fun overviewRow(icon: Int, labelText: String, enabled: Boolean, section: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(45)
            isClickable = true
            isFocusable = true
            contentDescription = "$labelText: ${if (enabled) "Autorizado" else "Não autorizado"}. Abrir configurações"
            setOnClickListener { openSettingsSection(section) }
        }
        row.addView(vectorIcon(icon, 18, highlight), LinearLayout.LayoutParams(dp(21), dp(21)))
        row.addView(label(labelText, 12.5f, strong = true).apply {
            setPadding(dp(9), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(statePill(if (enabled) "Autorizado" else "Pendente", if (enabled) "active" else "neutral"))
        overview.addView(row)
    }
    overviewRow(R.drawable.ic_pin, "Localização", LocationShareStore.hasPermission(this), "location")
    overviewRow(R.drawable.ic_remote_camera, "Câmera", checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED, "physical")
    overviewRow(R.drawable.ic_bell, "Notificações", hasListener(), "notifications")
    header.addView(overview)
    sectionGap(header, 8)
    header.addView(actionButton("Ver todas as permissões (${PermissionCenter.pending(this).size} pendentes)") {
        openSettingsSection("permissions")
    })
    content.addView(header)
    sectionGap(content, 11)

    val searchRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 15, true)
        setPadding(dp(12), 0, dp(6), 0)
    }
    searchRow.addView(vectorIcon(R.drawable.ic_search, 20, dim))
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
    searchRow.addView(search, LinearLayout.LayoutParams(0, dp(50), 1f))
    val clear = TextView(this).apply {
        text = "✕"
        textSize = 16f
        setTextColor(dim)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = "Limpar pesquisa"
        visibility = View.GONE
        setOnClickListener { search.text?.clear() }
    }
    searchRow.addView(clear, LinearLayout.LayoutParams(dp(42), dp(48)))
    content.addView(searchRow)
    val resultCount = label("", 11f, muted = true).apply { setPadding(dp(4), dp(8), 0, dp(4)) }
    content.addView(resultCount)
    val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    content.addView(results)

    var renderedQuery: String? = null
    fun update(query: String) {
        val normalized = folded(query)
        if (normalized == renderedQuery) return
        renderedQuery = normalized
        results.removeAllViews()
        clear.visibility = if (query.isNotBlank()) View.VISIBLE else View.GONE
        var count = 0
        settingGroups.forEach { group ->
            val matches = group.entries.filter { entry ->
                normalized.isBlank() || settingSearchIndex[group.title]?.get(entry)?.contains(normalized) == true
            }
            if (matches.isEmpty()) return@forEach
            count += matches.size
            val card = panel(12)
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(3), dp(2), 0, dp(7))
            }
            head.addView(View(this).apply { background = shape(highlight, 99) },
                LinearLayout.LayoutParams(dp(3), dp(17)))
            head.addView(label(group.title, 10.5f, strong = true, muted = true).apply {
                setPadding(dp(9), 0, 0, 0)
            })
            card.addView(head)
            matches.forEachIndexed { index, item ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(62)
                    isClickable = true
                    isFocusable = true
                    setPadding(dp(5), dp(8), dp(4), dp(8))
                    contentDescription = "${item.title}. ${item.hint}"
                    setOnClickListener {
                        if (item.section == "adb") startActivity(Intent(this@renderSettingsHome, AdbActivity::class.java))
                        else openSettingsSection(item.section)
                    }
                }
                val iconPlate = LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    background = shape(fieldColor, 13)
                    addView(vectorIcon(item.icon, 21, highlight))
                }
                row.addView(iconPlate, LinearLayout.LayoutParams(dp(40), dp(40)))
                val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
                words.addView(label(item.title, 13.5f, strong = true).apply {
                    maxLines = 2
                    setPadding(0, 0, 0, 0)
                })
                words.addView(label(item.hint, 11f, muted = true).apply {
                    maxLines = 2
                    setPadding(0, dp(2), 0, 0)
                })
                row.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(label("›", 21f, muted = true).apply { gravity = Gravity.CENTER })
                card.addView(row)
                if (index < matches.lastIndex) card.addView(View(this).apply {
                    background = shape(lineColor, 1)
                }, LinearLayout.LayoutParams(-1, dp(1)).apply { leftMargin = dp(54) })
            }
            results.addView(card)
            sectionGap(results, 9)
        }
        resultCount.text = if (normalized.isBlank()) "$count opções organizadas por categoria"
            else "$count resultado(s) para “${query.trim()}”"
        if (count == 0) results.addView(panel(14).apply {
            addView(visualHeader(R.drawable.ic_search, "Nenhum resultado", "Tente buscar por GPS, câmera, notificações ou arquivos"))
        })
    }
    // Atualização com 120 ms de pausa: evita destruir/recriar dezenas de Views
    // em cada tecla, sem atrasar abertura das categorias ou o botão Limpar.
    val pendingSearch = Runnable {
        if (results.isAttachedToWindow) update(search.text?.toString().orEmpty())
    }
    search.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            search.removeCallbacks(pendingSearch)
            if (s.isNullOrEmpty()) update("") else search.postDelayed(pendingSearch, 120L)
        }
        override fun afterTextChanged(s: Editable?) = Unit
    })
    search.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) { search.removeCallbacks(pendingSearch) }
    })
    update("")
}
