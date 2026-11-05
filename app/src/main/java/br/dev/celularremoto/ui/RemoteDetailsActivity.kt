package br.dev.celularremoto.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Informações recebidas somente após consulta autenticada; nada é consultado diretamente no controlador. */
class RemoteDetailsActivity : Activity() {
    companion object {
        const val EXTRA_SNAPSHOT = "br.dev.celularremoto.DETAILS_SNAPSHOT"
        const val EXTRA_DEVICE = "br.dev.celularremoto.DETAILS_DEVICE"
        const val EXTRA_HOST = "br.dev.celularremoto.DETAILS_HOST"
        const val EXTRA_METHOD = "br.dev.celularremoto.DETAILS_METHOD"
        const val EXTRA_UPDATED = "br.dev.celularremoto.DETAILS_UPDATED"
    }
    private val dark: Boolean get() = when (SettingsStore.theme(this)) {
        "dark", "amoled" -> true
        "system" -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        else -> false
    }
    private val pageBg: Int get() = if (dark) Color.rgb(14, 22, 35) else Color.rgb(247, 249, 252)
    private val cardBg: Int get() = if (dark) Color.rgb(28, 41, 58) else Color.WHITE
    private val tileBg: Int get() = if (dark) Color.rgb(22, 34, 50) else Color.rgb(243, 246, 249)
    private val fg: Int get() = if (dark) Color.rgb(233, 239, 247) else Color.rgb(23, 34, 48)
    private val subdued: Int get() = if (dark) Color.rgb(167, 184, 203) else Color.rgb(92, 109, 128)
    private val border: Int get() = if (dark) Color.rgb(47, 64, 83) else Color.rgb(224, 231, 239)
    private val teal: Int get() = if (dark) Color.rgb(90, 214, 188) else Color.rgb(24, 166, 146)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun drawable(color: Int, radius: Int, outline: Boolean = false): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (outline) setStroke(dp(1), border)
    }
    private fun text(value: String, size: Float, bold: Boolean = false, muted: Boolean = false) = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(if (muted) subdued else fg)
        if (bold) setTypeface(null, Typeface.BOLD)
        includeFontPadding = false
    }
    private fun icon(id: Int, size: Int, tint: Int = fg) = ImageView(this).apply {
        setImageResource(id)
        imageTintList = ColorStateList.valueOf(tint)
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = null
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }
    private fun gap(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun labelValue(data: JSONObject?, key: String): String = data?.optString(key)
        ?.takeIf { it.isNotBlank() && it != "null" } ?: "Não disponível"
    private fun yesNo(data: JSONObject?, key: String): String = when {
        data == null || !data.has(key) || data.isNull(key) -> "Não disponível"
        data.optBoolean(key) -> "Sim"
        else -> "Não"
    }
    private fun bytes(data: JSONObject?, key: String): String = if (data == null || !data.has(key) || data.isNull(key)) {
        "Não disponível"
    } else Formatter.formatFileSize(this, data.optLong(key))
    private fun time(value: Long) = if (value > 0L) {
        SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(value))
    } else "Não disponível"

    /** Clipboard is global to the device; keep Android 13+ previews redacted for all fields. */
    private fun copyDetail(label: String, value: String) {
        if (!DetailCopyPolicy.isCopyable(value)) return
        val clip = ClipData.newPlainText("Celular Remoto: $label", value.trim())
        if (Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < 33) {
            android.widget.Toast.makeText(this, "$label copiado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        val payload = runCatching { JSONObject(intent.getStringExtra(EXTRA_SNAPSHOT).orEmpty()) }.getOrNull()
        if (payload == null) { finish(); return }
        val battery = payload.optJSONObject("battery")
        val network = payload.optJSONObject("network")
        val system = payload.optJSONObject("deviceInfo")
        val sim = payload.optJSONObject("telephony")
        val device = intent.getStringExtra(EXTRA_DEVICE).orEmpty().ifBlank {
            labelValue(battery, "device")
        }
        val host = intent.getStringExtra(EXTRA_HOST).orEmpty()
        val method = intent.getStringExtra(EXTRA_METHOD).orEmpty()
        val received = intent.getLongExtra(EXTRA_UPDATED, 0L)

        // Se houver número compartilhado, impedir que outras apps façam screenshot da tela.
        if (sim?.optString("phoneNumber")?.takeIf { it.isNotBlank() && it != "null" } != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        window.statusBarColor = pageBg
        window.navigationBarColor = pageBg
        window.decorView.systemUiVisibility = if (dark) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val wrapper = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(pageBg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(34))
        }
        wrapper.addView(root)
        setContentView(wrapper)

        val skin = MonitorScreenSkin(this)
        root.addView(skin.header(R.drawable.ic_info, "Detalhes do aparelho", device) { finish() })
        root.addView(gap(14))
        val readingCard = skin.card()
        val readingRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val securityIcon = icon(R.drawable.ic_shield, 20, teal)
        readingRow.addView(securityIcon)
        readingRow.addView(View(this), LinearLayout.LayoutParams(dp(10), 1))
        val readingText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        readingText.addView(text("Dados da última consulta", 13.5f, bold = true))
        readingText.addView(gap(4))
        readingText.addView(text(time(received), 11.5f, muted = true))
        readingRow.addView(readingText, LinearLayout.LayoutParams(0, -2, 1f))
        readingCard.addView(readingRow)
        root.addView(readingCard)
        root.addView(gap(12))

        val detailFields = mutableListOf<DetailCopyPolicy.Field>()
        var currentSection = ""
        fun record(label: String, value: String) {
            if (DetailCopyPolicy.isCopyable(value)) {
                detailFields += DetailCopyPolicy.Field(currentSection, label, value.trim())
            }
        }
        fun copyGlyph(): ImageView = icon(R.drawable.ic_copy, 19, teal).apply {
            // Whole row/tile is the 48dp touch target; glyph is only a visual affordance.
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            setPadding(dp(9), dp(9), dp(9), dp(9))
        }
        fun section(iconId: Int, title: String, compose: (LinearLayout) -> Unit) {
            currentSection = title
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = drawable(cardBg, 22, true)
                setPadding(dp(15), dp(14), dp(15), dp(14))
            }
            val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            titleRow.addView(icon(iconId, 22, teal))
            titleRow.addView(View(this), LinearLayout.LayoutParams(dp(10), 1))
            titleRow.addView(text(title, 16f, bold = true))
            card.addView(titleRow)
            card.addView(gap(12))
            compose(card)
            root.addView(card)
            root.addView(gap(10))
        }
        fun cell(label: String, value: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = drawable(tileBg, 14)
            setPadding(dp(12), dp(11), dp(6), dp(12))
            minimumHeight = dp(60)
            addView(text(label, 10.5f, muted = true))
            addView(gap(7))
            val valueLine = LinearLayout(this@RemoteDetailsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            valueLine.addView(text(value, 13f, bold = true).apply {
                maxLines = 5
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (DetailCopyPolicy.isCopyable(value)) {
                record(label, value)
                valueLine.addView(copyGlyph())
                isClickable = true
                isFocusable = true
                contentDescription = "Copiar $label: $value"
                setOnClickListener { copyDetail(label, value) }
            }
            addView(valueLine)
        }
        fun pair(parent: LinearLayout, first: Pair<String, String>, second: Pair<String, String>) {
            val cells = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            cells.addView(cell(first.first, first.second), LinearLayout.LayoutParams(0, -2, 1f).apply {
                rightMargin = dp(8)
            })
            cells.addView(cell(second.first, second.second), LinearLayout.LayoutParams(0, -2, 1f))
            parent.addView(cells)
            parent.addView(gap(8))
        }
        fun row(parent: LinearLayout, key: String, value: String) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(48)
            }
            line.addView(text(key, 12f, muted = true).apply {
                maxLines = 5
            }, LinearLayout.LayoutParams(0, -2, 0.95f))
            line.addView(text(value, 12f, bold = true).apply {
                gravity = Gravity.END
                maxLines = 5
            }, LinearLayout.LayoutParams(0, -2, 1.05f))
            if (DetailCopyPolicy.isCopyable(value)) {
                record(key, value)
                line.addView(copyGlyph())
                line.isClickable = true
                line.isFocusable = true
                line.contentDescription = "Copiar $key: $value"
                line.setBackgroundResource(android.R.drawable.list_selector_background)
                line.setOnClickListener { copyDetail(key, value) }
            }
            parent.addView(line)
            parent.addView(gap(6))
        }
        fun decimal(data: JSONObject?, key: String, unit: String, digits: Int = 1): String =
            if (data == null || !data.has(key) || data.isNull(key)) "Não disponível"
            else String.format(Locale.getDefault(), "%.${digits}f %s", data.optDouble(key), unit)

        section(R.drawable.ic_battery, "Bateria") { card ->
            val state = battery?.optString("state")?.takeIf {
                it in setOf("Carregando", "Descarregando", "Cheia", "Não carregando")
            } ?: "Desconhecido"
            val supply = battery?.optString("powerSource")?.takeIf {
                it in setOf("Tomada", "USB", "Sem fio", "Base")
            } ?: "Não informada"
            pair(card, "Carga" to decimal(battery, "levelPercent", "%", 0),
                "Temperatura" to decimal(battery, "temperatureC", "°C"))
            pair(card, "Estado" to state,
                "Tensão" to if (battery?.has("voltageMv") == true && !battery.isNull("voltageMv"))
                    "${battery.optInt("voltageMv")} mV" else "Não disponível")
            row(card, "Alimentação", supply)
            pair(card, "Corrente atual" to decimal(battery, "currentMa", "mA", 0),
                "Corrente média" to decimal(battery, "averageMa", "mA", 0))
            card.addView(text("Corrente e tensão são leituras fornecidas pelo Android; podem variar conforme o hardware.", 10.5f, muted = true))
        }
        section(R.drawable.ic_wifi, "Wi-Fi e conexão") { card ->
            pair(card, "Rede ativa" to labelValue(network, "type"),
                "IP de acesso" to host.ifBlank { "Não disponível" })
            pair(card, "Wi-Fi detectado" to yesNo(network, "wifiDetected"),
                "Dados móveis detectados" to yesNo(network, "cellularDetected"))
            row(card, "Internet validada pelo Android", yesNo(network, "internetValidated"))
            row(card, "Rede tarifada", yesNo(network, "metered"))
            row(card, "IP informado pelo aparelho", labelValue(network, "localIpv4"))
            row(card, "Nome do Wi-Fi (SSID)", labelValue(network, "wifiSsid"))
            row(card, "Sinal recebido (RSSI)", if (network?.has("wifiRssiDbm") == true && !network.isNull("wifiRssiDbm")) "${network.optInt("wifiRssiDbm")} dBm" else "Sem leitura/permissão")
            row(card, "Método de pareamento", method.ifBlank { "Não disponível" })
        }
        section(R.drawable.ic_monitor, "Tela e câmera") { card ->
            val state = payload.optJSONObject("deviceState")
            row(card, "Display", state?.optString("screenState")?.takeIf { it.isNotBlank() } ?: "Não disponível")
            row(card, "Bloqueio", when {
                state == null || state.isNull("deviceLocked") -> "Não disponível"
                state.optBoolean("deviceLocked") -> "Bloqueado"
                else -> "Desbloqueado"
            })
            row(card, "Disponibilidade da câmera", when {
                state == null || state.isNull("cameraPossiblyBusy") -> "Sem leitura"
                state.optBoolean("cameraPossiblyBusy") -> "Possivelmente ocupada"
                else -> "Disponível"
            })
            row(card, "App em primeiro plano", when {
                state?.optBoolean("usagePermission") != true -> "Permissão não concedida"
                state.isNull("foregroundPackage") -> "Não identificado"
                else -> state.optString("foregroundPackage")
            })
            card.addView(text("Câmera ocupada não confirma gravação do Minha Câmera. Modo ambiente não é tela totalmente desligada.", 10.5f, muted = true))
        }
        section(R.drawable.ic_cellular, "Rede móvel e chip") { card ->
            pair(card, "Chip" to labelValue(sim, "simState"),
                "Operadora do chip" to labelValue(sim, "operator"))
            row(card, "Operadora da rede", labelValue(sim, "networkOperator"))
            val shareNumber = sim?.optBoolean("numberAuthorized") == true
            row(card, "Número da linha", if (shareNumber) labelValue(sim, "phoneNumber") else "Não compartilhado")
            card.addView(text("O número depende da operadora e de uma autorização opcional no celular monitorado.", 10.5f, muted = true))
        }
        section(R.drawable.ic_storage, "Armazenamento") { card ->
            val total = system?.optLong("storageTotalBytes") ?: 0L
            val available = system?.optLong("storageAvailableBytes") ?: 0L
            val valid = total > 0L && available in 0L..total
            if (valid) {
                val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 1000
                    this.progress = (((total - available).toDouble() / total) * 1000).toInt()
                    progressTintList = ColorStateList.valueOf(teal)
                    progressBackgroundTintList = ColorStateList.valueOf(border)
                }
                card.addView(progress, LinearLayout.LayoutParams(-1, dp(9)))
                card.addView(gap(11))
            }
            pair(card, "Capacidade" to bytes(system, "storageTotalBytes"),
                "Disponível" to bytes(system, "storageAvailableBytes"))
            val used = if (valid) Formatter.formatFileSize(this, total - available) else "Não disponível"
            row(card, "Ocupado (partição de dados)", used)
            card.addView(text("Mostra o armazenamento de dados do aparelho, não o tamanho de contas na nuvem.", 10.5f, muted = true))
        }
        section(R.drawable.ic_cpu, "Sistema e hardware") { card ->
            pair(card, "Fabricante" to labelValue(system, "manufacturer"),
                "Modelo" to labelValue(system, "model"))
            pair(card, "Android" to labelValue(system, "androidVersion"),
                "API do Android" to labelValue(system, "sdk"))
            pair(card, "RAM total" to bytes(system, "ramTotalBytes"),
                "RAM disponível" to bytes(system, "ramAvailableBytes"))
            row(card, "Arquitetura", labelValue(system, "architecture"))
            row(card, "Patch de segurança", labelValue(system, "securityPatch"))
            val uptime = system?.optLong("uptimeMs") ?: 0L
            row(card, "Tempo ligado", if (uptime > 0L) "${uptime / 3_600_000} h ${(uptime / 60_000) % 60} min" else "Não disponível")
        }

        val actionStyle: (Button) -> Unit = { button ->
            button.textSize = 13.5f
            button.isAllCaps = false
            button.setTextColor(if (dark) Color.rgb(14, 22, 35) else Color.WHITE)
            button.background = drawable(teal, 16)
            button.minimumHeight = dp(51)
        }
        val copyButton = Button(this).apply {
            text = "Copiar resumo técnico"
            actionStyle(this)
            setOnClickListener {
                val summary = buildString {
                    append(device).append("\nAtualização: ").append(time(received))
                    append("\nBateria: ").append(decimal(battery, "levelPercent", "%", 0))
                    append("\nTemperatura: ").append(decimal(battery, "temperatureC", "°C"))
                    append("\nRede: ").append(labelValue(network, "type"))
                    append("\nArmazenamento: ").append(bytes(system, "storageTotalBytes"))
                    append("\nAndroid: ").append(labelValue(system, "androidVersion"))
                }
                copyDetail("Resumo técnico", summary)
            }
        }
        root.addView(copyButton, LinearLayout.LayoutParams(-1, dp(51)))
        root.addView(gap(8))
        val copyAll = Button(this).apply {
            text = "Copiar todos os dados disponíveis"
            actionStyle(this)
            setOnClickListener {
                val report = DetailCopyPolicy.report(device, time(received), detailFields)
                if (report.isBlank()) return@setOnClickListener
                val includesPhone = detailFields.any { it.section == "Rede móvel e chip" && it.label == "Número da linha" }
                if (includesPhone) {
                    AlertDialog.Builder(this@RemoteDetailsActivity)
                        .setTitle("Copiar dados com número de telefone?")
                        .setMessage("O relatório inclui o número autorizado no monitorado. A área de transferência pode ser acessada por outros aplicativos; compartilhe com cuidado.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Copiar") { _, _ -> copyDetail("Relatório do aparelho", report) }
                        .show()
                } else copyDetail("Relatório do aparelho", report)
            }
        }
        root.addView(copyAll, LinearLayout.LayoutParams(-1, dp(51)))
        root.addView(gap(10))
        root.addView(text("Toque em um valor com ícone de cópia para copiá-lo. Número da linha: é necessário autorizar o compartilhamento no aparelho monitorado; a operadora pode não fornecê-lo.", 11f, muted = true))
        root.addView(gap(8))
        root.addView(text("Dados disponíveis apenas enquanto o aparelho monitorado responder. Informações não acessíveis são indicadas explicitamente.", 11f, muted = true))
    }
}
