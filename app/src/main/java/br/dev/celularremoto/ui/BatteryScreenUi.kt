package br.dev.celularremoto.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
import android.widget.PopupMenu
import android.text.InputType
import br.dev.celularremoto.R
import br.dev.celularremoto.core.BatteryLabStore
import br.dev.celularremoto.core.BatteryHistoryMath
import br.dev.celularremoto.core.BatteryGoalAlerts
import br.dev.celularremoto.core.BatteryPresentationMath
import br.dev.celularremoto.core.BatteryTelemetry
import br.dev.celularremoto.core.ControllerBatteryEvents
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.transport.RemoteClient
import org.json.JSONObject
import org.json.JSONArray
import java.security.MessageDigest
import kotlin.concurrent.thread
import java.util.Locale

private const val SESSION_RECENT_MS = 150_000L
private const val REMOTE_REPORT_RECENT_MS = 180_000L

internal fun MainActivity.renderBatteryScreen() {
    val host = SettingsStore.getRemoteHost(this)
    val paired = host.isNotBlank() && RecentDevices.key(this, host) != null
    if (batteryRemote && !paired) {
        batteryRemote = false
        batteryData = null
        batteryError = null
    }
    if (!batteryRemote) {
        batteryData = BatteryLabStore.report(this)
        batteryDataHost = ""
    } else if (batteryDataHost != host) {
        batteryData = null
        batteryDataHost = host
        batteryError = null
        batteryLoading = false
    }

    val data = batteryData
    val battery = data?.optJSONObject("battery")
    val goals = data?.optJSONObject("goalAlerts")
    val automatic = data?.optJSONObject("automatic")
    val running = data?.optJSONObject("active")
    renderMobileHeader { anchor -> showBatteryMenu(anchor, battery, goals) }

    val switchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    switchRow.addView(actionButton("Este aparelho", !batteryRemote) { selectBatteryDevice(false) }.apply {
        isSelected = !batteryRemote
        minimumHeight = dp(48)
    }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
    switchRow.addView(actionButton("Remoto", batteryRemote) { selectBatteryDevice(true) }.apply {
        isEnabled = paired
        isSelected = batteryRemote
        minimumHeight = dp(48)
        alpha = if (paired) 1f else 0.5f
    }, LinearLayout.LayoutParams(0, -2, 1f))
    content.addView(switchRow)

    val now = System.currentTimeMillis()
    val reportedAt = data?.optLong("reportedAt", 0L) ?: 0L
    val remoteRecent = BatteryDashboardPolicy.isRecent(now, reportedAt, REMOTE_REPORT_RECENT_MS)
    if (batteryRemote) {
        gap(4)
        renderBatteryRemoteStatus(reportedAt, remoteRecent)
        if (data == null && !batteryLoading && batteryError == null)
            content.post { if (active && mode == 5 && batteryRemote) queryBatteryReport() }
    }
    batteryError?.let { content.addView(statePill(it, "warning")) }

    gap(8)
    val sections = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    listOf("resumo" to "Resumo", "testes" to "Testes", "historico" to "Histórico").forEachIndexed { index, (id, caption) ->
        sections.addView(actionButton(caption, batterySection == id) {
            batterySection = id
            render()
        }.apply {
            isSelected = batterySection == id
            minimumHeight = dp(48)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply {
            if (index < 2) rightMargin = dp(5)
        })
    }
    content.addView(sections)
    gap(8)

    when (batterySection) {
        "testes" -> {
            if (running != null && !batteryRemote && !MonitorService.isBatteryMonitoringActive)
                content.addView(statePill("Coleta pausada. Ative o Monitorado ou a coleta contínua local.", "warning"))
            renderBatteryTests(running)
        }
        "historico" -> renderBatteryHistory(data?.optJSONArray("history") ?: JSONArray(), automatic)
        else -> {
            val session = automatic?.optJSONObject("active")
            if (battery != null) {
                val fresh = !batteryRemote || (remoteRecent && batteryError == null)
                renderBatteryOverview(battery, session, goals, now, fresh)
            } else if (!batteryRemote) {
                content.addView(label("Leitura da bateria indisponível.", 12f, muted = true))
            }
            if (running != null) {
                gap(6)
                val paused = !batteryRemote && !MonitorService.isBatteryMonitoringActive
                content.addView(batteryInlineAction(R.drawable.ic_activity,
                    if (paused) "Teste com coleta pausada" else "Ver teste em andamento",
                    batteryKind(running.optString("kind"))) {
                    batterySection = "testes"
                    render()
                })
            }
        }
    }
}

private fun MainActivity.renderBatteryRemoteStatus(reportedAt: Long, recent: Boolean) {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val status = when {
        batteryLoading -> "Consultando aparelho remoto…"
        reportedAt <= 0 -> "Nenhuma leitura remota"
        batteryError != null || !recent -> "Última leitura às ${formatBatteryTime(reportedAt)} · anterior"
        else -> "Atualizado às ${formatBatteryTime(reportedAt)}"
    }
    row.addView(label(status, 11.5f, muted = true), LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(headerIconButton(R.drawable.ic_refresh, "Atualizar leitura remota") {
        if (!batteryLoading) {
            queryBatteryReport()
            render()
        }
    }.apply {
        isEnabled = !batteryLoading
        alpha = if (batteryLoading) 0.5f else 1f
    }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(6) })
    content.addView(row)
}

private fun MainActivity.showBatteryMenu(anchor: View, battery: JSONObject?, goals: JSONObject?) {
    val popup = PopupMenu(themedPopup(this).context, anchor, Gravity.END)
    fun option(title: String, action: () -> Unit) {
        popup.menu.add(title).setOnMenuItemClickListener { action(); true }
    }
    if (goals != null) option("Metas e alertas") { showBatteryAlertDialog(goals) }
    if (battery != null) option("Diagnóstico elétrico") { showBatteryCurrentDiagnostic(battery) }
    if (!batteryRemote) {
        val enabled = SettingsStore.localBatteryMonitoring(this)
        option(if (enabled) "Pausar coleta contínua local" else "Ativar coleta contínua local") {
            val next = !SettingsStore.localBatteryMonitoring(this)
            SettingsStore.setLocalBatteryMonitoring(this, next)
            runCatching { MonitorService.refreshModes(this) }.onFailure {
                SettingsStore.setLocalBatteryMonitoring(this, !next)
                message("Não foi possível ajustar a coleta contínua: ${it.message}")
            }
            render()
        }
    }
    if (batteryRemote && !SettingsStore.controllerHealthAlerts(this)) {
        option("Ativar alertas neste controlador") {
            SettingsStore.setControllerHealthAlerts(this, true)
            render()
        }
    }
    if (popup.menu.size() == 0) popup.menu.add("Aguardando leitura remota").isEnabled = false
    popup.show()
}

private fun MainActivity.renderBatteryOverview(battery: JSONObject, session: JSONObject?, goals: JSONObject?,
    now: Long, fresh: Boolean) {
    val level = battery.optInt("levelPercent", -1)
    val status = battery.optInt("statusCode", -1)
    val plugged = battery.optInt("pluggedCode", -1)
    val current = battery.safeDouble("currentMa")
    val temp = battery.safeDouble("temperatureC")
    val power = BatteryPresentationMath.visualPowerW(battery.optInt("voltageMv", -1), current, status)
    val source = battery.optString("powerSource", "").takeUnless { it.isBlank() || it == "null" }
    val kind = session?.optString("kind").orEmpty()
    val sessionMatches = session != null &&
        BatteryDashboardPolicy.matchesSession(status, plugged, level, kind, session.optInt("finalPercent", -1))
    val forecast = if (session != null && fresh) {
        when {
            !BatteryDashboardPolicy.isRecent(now, session.optLong("lastWall"), SESSION_RECENT_MS) ->
                "Sessão sem leituras recentes"
            !sessionMatches ->
                "Aguardando nova leitura da sessão"
            else -> BatteryDashboardPolicy.forecastLabel(kind, session.optLong("forecastMinutes", -1L))
                ?: "Sem dados suficientes para estimar"
        }
    } else null
    val summary = batteryHero(level, battery.optString("state", "Desconhecido"), source,
        BatteryTelemetry.showChargingIndicator(status, plugged), forecast)
    batteryMetricGrid(summary, listOf(
        batteryQuickMetric("Temperatura", temp?.let { "%.1f °C".format(Locale.getDefault(), it) } ?: "—"),
        batteryQuickMetric("Corrente líquida", BatteryPresentationMath.currentText(current, status)),
        batteryQuickMetric("Potência líquida", power?.let { "%+.2f W".format(Locale.getDefault(), it) } ?: "—")
    ))
    battery.optString("currentWarning", "").takeUnless { it.isBlank() || it == "null" }?.let { warning ->
        val compactWarning = when {
            BatteryPresentationMath.isVisuallyAdjusted(current, status) -> "Sinal de corrente ajustado na tela. Veja o diagnóstico."
            status == 2 && current != null && current < 0 -> "Android indica carga, mas a corrente líquida está negativa."
            else -> warning
        }
        sectionGap(summary, 4)
        summary.addView(statePill(compactWarning, "warning"))
    }
    if (fresh && goals != null) {
        val low = goals.optInt("lowPercent", -1)
        val hot = goals.optInt("hotC", -1)
        val warnings = listOfNotNull(
            "Bateria no alerta de carga baixa".takeIf { status != 2 && low in 5..40 && level in 0..low },
            "Temperatura no limite de alerta ou acima".takeIf { temp != null && hot in 38..55 && temp >= hot }
        )
        if (warnings.isNotEmpty()) {
            sectionGap(summary, 4)
            summary.addView(statePill(warnings.joinToString(" · "), "warning"))
        }
    }
    if (session != null) {
        summary.addView(actionButton("Ver sessão") { showBatteryAutomaticReport(session, allowForecast = fresh && sessionMatches) },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(5) })
    }
    content.addView(summary)
}

private fun formatBatteryTime(timestamp: Long): String =
    java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(timestamp))

/** O atalho entra na aba existente. Controles de gravação/alerta continuam no fluxo normal. */
internal fun MainActivity.openBatterySearchDestination(destination: FunctionDestination.Battery) {
    val host = SettingsStore.getRemoteHost(this)
    if (destination.remote == true && (host.isBlank() || RecentDevices.key(this, host) == null)) {
        showConnectionChooser()
        return
    }
    batterySection = destination.page.tab
    if (destination.page == BatterySearchPage.HISTORY) {
        batteryHistoryType = "ALL"
        batteryHistoryPeriod = "ALL"
    }
    if (mode != 5) {
        if (destination.remote != null) batteryRemote = destination.remote
        navigateMainTab(5)
    } else if (destination.remote != null && destination.remote != batteryRemote) {
        selectBatteryDevice(destination.remote)
    } else render()
    val data = batteryData
    when (destination.page) {
        BatterySearchPage.DIAGNOSTIC -> data?.optJSONObject("battery")?.let { showBatteryCurrentDiagnostic(it) }
        BatterySearchPage.GOALS -> data?.optJSONObject("goalAlerts")?.let { showBatteryAlertDialog(it) }
        BatterySearchPage.SESSION -> focusFunctionRow(listOf("Ver sessão"))
        else -> destination.focus?.let { focusFunctionRow(listOf(it)) }
    }
}

private fun MainActivity.selectBatteryDevice(remote: Boolean) {
    if (remote == batteryRemote) return
    batteryRemote = remote
    batteryData = null
    batteryDataHost = ""
    batteryError = null
    batteryLoading = false
    batteryRequestGeneration++
    render()
}

/** Não aceite resposta tardia de outro host, chave revogada ou Activity pausada. */
internal fun MainActivity.queryBatteryReport(action: String = "status", kind: String = "", sky: String = "",
    panelWatts: Int = 0, minutes: Int = 0, screen: String = "NAO_INFORMADA",
    goalPercent: Int = 80, lowPercent: Int = 20, hotC: Int = 43) {
    if (batteryLoading && action == "status") return
    if (action != "status") batteryLoading = true else if (!batteryLoading) batteryLoading = true
    val remote = batteryRemote
    val host = if (remote) SettingsStore.getRemoteHost(this) else ""
    val key = if (remote && host.isNotBlank()) RecentDevices.key(this, host) else null
    val method = remoteConnection.method
    val token = ++batteryRequestGeneration
    batteryError = null
    thread(name = "CelularRemoto-battery-lab") {
        val result = runCatching {
            if (remote) {
                check(key != null) { "Aparelho remoto não pareado." }
                RemoteClient.batteryLab(host, key, applicationContext, method,
                    action, kind, sky, panelWatts, minutes, screen, goalPercent, lowPercent, hotC)
            } else {
                when (action) {
                    "start" -> {
                        check(MonitorService.isServiceActive) { "Ative o Monitorado para testar com a tela apagada." }
                        BatteryLabStore.begin(applicationContext, kind, sky, panelWatts, minutes, screen)
                    }
                    "stop" -> BatteryLabStore.end(applicationContext)
                    "alerts" -> BatteryGoalAlerts.configure(applicationContext, goalPercent, lowPercent, hotC)
                }
                BatteryLabStore.report(applicationContext)
            }
        }
        handler.post {
            if (token != batteryRequestGeneration || mode != 5 || isFinishing || isDestroyed ||
                batteryRemote != remote || (remote && SettingsStore.getRemoteHost(this) != host)) return@post
            batteryLoading = false
            if (remote && (key == null || RecentDevices.key(this, host)?.let { MessageDigest.isEqual(key, it) } != true)) {
                batteryData = null
                batteryError = "Pareamento alterado: descarte a resposta anterior."
            } else result.onSuccess {
                if (remote) ControllerBatteryEvents.onReport(applicationContext, host, it)
                batteryData = it
                batteryDataHost = host
                batteryError = null
            }.onFailure {
                batteryError = it.message ?: "Falha na leitura da bateria."
            }
            render()
        }
    }
}

private fun MainActivity.showBatteryCurrentDiagnostic(battery: JSONObject) {
    val rawNow = battery.optInt("rawNowMicroA", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    val rawAverage = battery.optInt("rawAverageMicroA", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    val counter = battery.optInt("chargeCounterMicroAh", -1).takeIf { it >= 0 }
    val counterWindow = battery.optLong("counterWindowMs", -1L).takeIf { it > 0 }
    val (wrapper, body) = dialogWrapper()
    val selected = battery.safeDouble("currentMa")
    val statusCode = battery.optInt("statusCode", -1).takeIf { it >= 0 }
    val visualCurrent = BatteryPresentationMath.visualCurrentMa(selected, statusCode)
    val voltageMv = battery.optInt("voltageMv", -1)
    val origin = battery.optString("currentSource", "NONE")
    body.addView(batteryKeyValue("Corrente líquida", visualCurrent?.let {
        "%+.1f mA".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    body.addView(batteryKeyValue("Potência líquida", BatteryPresentationMath.visualPowerW(voltageMv, selected, statusCode)?.let {
        "%+.2f W".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    body.addView(batteryKeyValue("Origem da corrente", BatteryPresentationMath.sourceLabel(origin)))
    battery.optString("currentWarning", "").takeUnless { it.isBlank() || it == "null" }?.let { warning ->
        body.addView(label(warning, 12f, muted = true))
    }
    body.addView(label("São medidas da bateria, não da geração do painel solar.", 11.5f, muted = true))

    batteryDisclosure(body, "Valores brutos e médias", "Ocultar valores brutos") {
        addView(batteryKeyValue("Tensão da bateria", voltageMv.takeIf { it > 0 }?.let { "$it mV" } ?: "Indisponível"))
        addView(batteryKeyValue("Corrente instantânea Android", battery.safeDouble("instantMa")?.let {
            "%+.1f mA".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        addView(batteryKeyValue("Corrente média Android", battery.safeDouble("averageMa")?.let {
            "%+.1f mA".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        addView(batteryKeyValue("Média pelo contador de carga", battery.safeDouble("counterMeanMa")?.let {
            "%+.1f mA".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        if (counterWindow != null) addView(batteryKeyValue("Janela do contador", batteryDuration(counterWindow)))
        else addView(label("O contador precisa de ≥90 s de amostras válidas.", 11.5f, muted = true))
        addView(batteryKeyValue("CURRENT_NOW bruto", rawNow?.let { "$it µA" } ?: "Indisponível"))
        addView(batteryKeyValue("CURRENT_AVERAGE bruto", rawAverage?.let { "$it µA" } ?: "Indisponível"))
        addView(batteryKeyValue("CHARGE_COUNTER bruto", counter?.let { "$it µAh" } ?: "Indisponível"))
        addView(batteryKeyValue("Origem selecionada", origin))
        addView(label(battery.optString("currentDiagnostic", "Leitura indisponível"), 12f, muted = true))
        if (BatteryPresentationMath.isVisuallyAdjusted(selected, statusCode)) {
            addView(label("O painel ajusta apenas o sinal exibido na descarga. O valor bruto do Android não é alterado.", 12f, muted = true))
        }
        addView(label("Sinais e unidades vêm do Android. Não há correção automática de unidades do fabricante.", 11.5f, muted = true))
    }
    showManagedDialog(themedPopup(this).setTitle("Diagnóstico elétrico").setView(wrapper)
        .setPositiveButton("Fechar", null).create())
}

private fun MainActivity.batteryDisclosure(parent: LinearLayout, title: String, hideTitle: String, build: LinearLayout.() -> Unit) {
    val details = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        build()
    }
    val toggle = actionButton("$title ▾") {}
    toggle.setOnClickListener {
        val expand = details.visibility != View.VISIBLE
        details.visibility = if (expand) View.VISIBLE else View.GONE
        toggle.text = if (expand) "$hideTitle ▴" else "$title ▾"
        toggle.isSelected = expand
    }
    parent.addView(toggle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
    parent.addView(details)
}

private fun JSONObject.safeDouble(name: String): Double? =
    if (isNull(name) || !has(name)) null else optDouble(name).takeIf { it.isFinite() }

private fun MainActivity.renderBatteryTests(active: JSONObject?) {
    content.addView(label("Testes de energia e consumo", 16f, strong = true))
    if (active != null) {
        renderBatteryActive(active)
        return
    }
    val options = listOf(
        Triple("SOLAR", "☀ Teste Solar", "Condição do céu informada; ganho e corrente líquida da bateria"),
        Triple("ELECTRIC", "⚡ Teste Elétrico", "Carga por fonte elétrica conectada"),
        Triple("DISCHARGE", "▾ Teste de Consumo", "Descarga sem fonte externa conectada"),
        Triple("AUTO", "↻ Carga Automática", "Finaliza ao chegar a 100% ou após desconexão confirmada")
    )
    val icons = mapOf(
        "SOLAR" to R.drawable.ic_power,
        "ELECTRIC" to R.drawable.ic_battery_charging,
        "DISCHARGE" to R.drawable.ic_battery,
        "AUTO" to R.drawable.ic_activity
    )
    compactActionGrid(content, options.map { (kind, title, description) ->
        compactActionTile(icons[kind] ?: R.drawable.ic_battery, title.removePrefix("☀ ").removePrefix("⚡ ")
            .removePrefix("▾ ").removePrefix("↻ "), description) { showBatteryStartDialog(kind) }
    }, preferredColumns = 2)
}

private fun MainActivity.showBatteryStartDialog(kind: String) {
    val (wrapper, body) = dialogWrapper()
    val labels = mapOf("SOLAR" to "Teste Solar", "ELECTRIC" to "Teste Elétrico",
        "DISCHARGE" to "Teste de Consumo", "AUTO" to "Carga Automática")
    body.addView(label(labels[kind] ?: "Teste", 18f, strong = true))
    body.addView(label("As medições serão realizadas e salvas no aparelho escolhido. O controle remoto requer pareamento e Monitorado ativo.", 12f, muted = true))
    val skies = listOf("Sol forte / limpo", "Sol entre nuvens", "Nublado", "Chuva")
    val skySpinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@showBatteryStartDialog, android.R.layout.simple_spinner_dropdown_item, skies)
    }
    if (kind == "SOLAR") {
        body.addView(label("Condição do céu", 13f, strong = true))
        body.addView(skySpinner)
    }
    val watts = field("Potência nominal do painel em W (opcional)", "")
        .apply { inputType = InputType.TYPE_CLASS_NUMBER }
    if (kind == "SOLAR") {
        body.addView(label("Potência nominal declarada (não medida)", 12f, muted = true))
        body.addView(watts)
    }
    val minutes = field("Duração em minutos", "120").apply { inputType = InputType.TYPE_CLASS_NUMBER }
    if (kind != "AUTO") {
        body.addView(label("Duração (1 a 1440 minutos)", 12f, strong = true))
        body.addView(minutes)
    }
    val screens = listOf("Não informada", "Tela acesa", "Tela apagada")
    val screenSpinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@showBatteryStartDialog, android.R.layout.simple_spinner_dropdown_item, screens)
    }
    if (kind != "AUTO") {
        body.addView(label("Condição da tela durante o teste", 12f, strong = true))
        body.addView(screenSpinner)
    }
    val dialog = themedPopup(this).setTitle("Iniciar ${labels[kind]}").setView(wrapper)
        .setNegativeButton("Cancelar", null).setPositiveButton("Iniciar", null).create()
    showManagedDialog(dialog)
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val duration = if (kind == "AUTO") 0 else minutes.text.toString().toIntOrNull()
        val panel = if (kind == "SOLAR") watts.text.toString().trim().ifBlank { "0" }.toIntOrNull() else 0
        if (duration == null || duration !in 0..1440 || (kind != "AUTO" && duration == 0) ||
            panel == null || panel !in 0..200) {
            showAppNotice("Dados inválidos", "Informe duração entre 1 e 1440 minutos e painel entre 0 e 200 W.")
            return@setOnClickListener
        }
        val sky = if (kind == "SOLAR") listOf("forte", "parcial", "nublado", "chuva")[skySpinner.selectedItemPosition.coerceIn(0,3)] else ""
        val screen = listOf("NAO_INFORMADA", "ACESA", "APAGADA")[screenSpinner.selectedItemPosition.coerceIn(0,2)]
        dialog.dismiss()
        queryBatteryReport("start", kind, sky, panel, duration, screen)
    }
}

private fun MainActivity.renderBatteryActive(session: JSONObject) {
    val box = panel(12)
    batterySessionHeader(box, session, batteryKind(session.optString("kind")))
    box.addView(label("Valores até a última leitura salva.", 11.5f, muted = true))
    box.addView(actionButton("Ver relatório do teste") { showBatteryReportDialog(session) },
        LinearLayout.LayoutParams(-2, -2))
    content.addView(box)
    gap(6)
    content.addView(button("Finalizar teste", emphasis = false) {
        showAppConfirm("Finalizar teste?", "As medições acumuladas serão preservadas no aparelho selecionado.",
            confirmText = "Finalizar") { queryBatteryReport("stop") }
    })
}

private fun batteryKind(kind: String): String = when (kind) {
    "SOLAR" -> "Solar"
    "ELECTRIC" -> "Elétrico"
    "DISCHARGE" -> "Consumo"
    "AUTO" -> "Automático"
    else -> "Não identificado"
}

private fun batteryDuration(ms: Long): String = BatteryDashboardPolicy.durationLabel(ms)

private fun JSONObject.batteryPercent(field: String): String =
    optInt(field, -1).takeIf { it in 0..100 }?.let { "$it%" } ?: "—"

private fun JSONObject.batteryHistorySession(): BatteryHistoryMath.Session {
    val duration = optLong("durationMs", 0L).coerceAtLeast(0L)
    return BatteryHistoryMath.Session(
        kind = optString("kind", ""),
        startedAt = optLong("startedAt", 0L), endedAt = optLong("endedAt", 0L),
        durationMs = duration,
        observedMs = (duration - optLong("unknownMs", duration).coerceAtLeast(0L)).coerceAtLeast(0L),
        initialPercent = optInt("initialPercent", -1).takeIf { it in 0..100 },
        finalPercent = optInt("finalPercent", -1).takeIf { it in 0..100 },
        sky = optString("sky", ""), panelWatts = optInt("panelWatts", 0),
        screen = optString("screen", "NAO_INFORMADA")
    )
}

private fun JSONObject.batteryProgress(): String {
    val first = optInt("initialPercent", -1)
    val last = optInt("finalPercent", -1)
    return if (first in 0..100 && last in 0..100) "%+d p.p.".format(Locale.getDefault(), last - first)
        else "Variação indisponível"
}

private fun MainActivity.renderBatteryHistory(history: JSONArray, automatic: JSONObject?) {
    content.addView(compactSectionTitle("Histórico", "Local ou remoto"))
    val kinds = listOf("ALL" to "Todos", "SOLAR" to "☀ Solar", "ELECTRIC" to "⚡ Elétrico",
        "DISCHARGE" to "Consumo", "AUTO" to "Carga auto (teste)", "CHARGE" to "Carga normal",
        "AUTO_DISCHARGE" to "Descarga normal")
    val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
    val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    kinds.forEach { (key, title) ->
        choices.addView(actionButton(title, batteryHistoryType == key) {
            batteryHistoryType = key
            render()
        }, LinearLayout.LayoutParams(-2, dp(42)).apply { rightMargin = dp(5) })
    }
    horizontal.addView(choices)
    content.addView(horizontal)
    gap(8)
    val periods = listOf("ALL" to "Tudo", "7" to "7 dias", "30" to "30 dias", "90" to "90 dias")
    val periodRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    periods.forEachIndexed { index, (key, title) ->
        periodRow.addView(actionButton(title, batteryHistoryPeriod == key) {
            batteryHistoryPeriod = key
            render()
        }, LinearLayout.LayoutParams(0, dp(39), 1f).apply { if (index < periods.lastIndex) rightMargin = dp(4) })
    }
    content.addView(periodRow)
    gap(10)
    // Mesmo relatório autenticado para o aparelho local e o remoto; filtros somente na UI.
    val visible = mutableListOf<Pair<JSONObject, Boolean>>()
    for (i in 0 until history.length()) history.optJSONObject(i)?.let { visible.add(it to false) }
    val autos = automatic?.optJSONArray("history") ?: JSONArray()
    for (i in 0 until autos.length()) autos.optJSONObject(i)?.let { visible.add(it to true) }
    val now = System.currentTimeMillis()
    val periodDays = batteryHistoryPeriod.toIntOrNull()
    val filtered = visible.filter { (item, isAuto) ->
        val kind = item.optString("kind")
        val group = if (isAuto && kind == "DISCHARGE") "AUTO_DISCHARGE" else kind
        (batteryHistoryType == "ALL" || batteryHistoryType == group) &&
            (periodDays == null || item.optLong("startedAt") >= now - periodDays * 86_400_000L)
    }.sortedByDescending { it.first.optLong("startedAt") }
    content.addView(label("${filtered.size} sessões encontradas · ${visible.size} recebidas", 11.5f, muted = true))
    if (filtered.isEmpty()) {
        content.addView(label("Nenhuma sessão corresponde aos filtros. Selecione Todos ou um período maior.", 13f, muted = true))
        return
    }
    val completedLabs = filtered.filter { !it.second && !it.first.optBoolean("active") }
        .map { it.first }.filter { it.optString("kind") in setOf("SOLAR", "ELECTRIC", "DISCHARGE", "AUTO") }
    if (completedLabs.size >= 2) {
        content.addView(button("Comparar duas sessões registradas") { showBatteryCompareDialog(completedLabs) })
        gap(7)
    }
    val historyList = panel(9)
    filtered.forEach { (item, isAuto) ->
        val labelKind = if (isAuto) when (item.optString("kind")) {
            "CHARGE" -> "Carga normal"
            "DISCHARGE" -> "Descarga normal"
            else -> "Sessão automática"
        } else batteryKind(item.optString("kind"))
        val badge = if (item.optBoolean("active")) " · EM ANDAMENTO" else ""
        val rate = if (!isAuto) BatteryHistoryMath.rate(item.batteryHistorySession()) else null
        val cover = if (isAuto) item.safeDouble("coveragePercent") else
            (100.0 * BatteryHistoryMath.coverage(item.batteryHistorySession().observedMs,
                item.optLong("durationMs")))
        val progress = item.batteryProgress().takeUnless { it == "+0 p.p." }
        val note = listOfNotNull(
            badge.takeIf { it.isNotBlank() },
            progress,
            batteryDuration(item.optLong("durationMs")),
            if (cover != null && item.optLong("durationMs") > 0L) "cobertura ${"%.0f".format(Locale.getDefault(), cover)}%" else null,
            rate?.let { "${"%+.1f".format(Locale.getDefault(), it)} p.p./h" }
        ).joinToString(" · ")
        val icon = when (item.optString("kind")) {
            "SOLAR", "ELECTRIC", "CHARGE", "AUTO" -> R.drawable.ic_battery_charging
            else -> R.drawable.ic_battery
        }
        batterySessionListRow(historyList, icon,
            "$labelKind · ${formatDate(item.optLong("startedAt"))}",
            listOf("${item.batteryPercent("initialPercent")} → ${item.batteryPercent("finalPercent")}", note)
                .filter { it.isNotBlank() }.joinToString(" · ")) {
            if (isAuto) showBatteryAutomaticReport(item) else showBatteryReportDialog(item)
        }
    }
    content.addView(historyList)
    content.addView(label("O remoto envia até 24 testes e 24 sessões automáticas por consulta. Gráficos completos apenas quando disponíveis.", 10.5f, muted = true))
}

private fun MainActivity.showBatteryCompareDialog(sessions: List<JSONObject>) {
    val (wrapper, body) = dialogWrapper()
    body.addView(label("Compare dois testes concluídos do mesmo tipo e com condições semelhantes.", 12f, muted = true))
    val captions = sessions.map { item ->
        "${batteryKind(item.optString("kind"))} · ${formatDate(item.optLong("startedAt"))} · ${item.batteryProgress()}"
    }
    fun selector(title: String, index: Int): Spinner {
        body.addView(label(title, 12.5f, strong = true))
        return Spinner(this).apply {
            adapter = ArrayAdapter(this@showBatteryCompareDialog, android.R.layout.simple_spinner_dropdown_item, captions)
            setSelection(index.coerceAtMost(captions.lastIndex))
            body.addView(this)
        }
    }
    val one = selector("Primeira sessão", 0)
    val two = selector("Segunda sessão", 1)
    showManagedDialog(themedPopup(this).setTitle("Comparar sessões").setView(wrapper)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Comparar") { _, _ ->
            val a = one.selectedItemPosition
            val b = two.selectedItemPosition
            if (a == b) showAppNotice("Seleção inválida", "Escolha duas sessões diferentes.")
            else showBatteryCompareResult(sessions[a], sessions[b])
        }.create())
}

private fun MainActivity.showBatteryCompareResult(first: JSONObject, second: JSONObject) {
    val a = first.batteryHistorySession()
    val b = second.batteryHistorySession()
    val result = BatteryHistoryMath.compare(a, b)
    fun percentage(value: Double) = "%.0f%%".format(Locale.getDefault(), value * 100)
    fun rate(value: Double?) = value?.let { "%+.2f p.p./h".format(Locale.getDefault(), it) } ?: "Indisponível"
    val (scroll, body) = dialogWrapper()
    body.addView(label("Sessão A · ${batteryKind(a.kind)} · ${formatDate(a.startedAt)}", 13f, strong = true))
    body.addView(dialogMetricRow("Variação", first.batteryProgress()))
    body.addView(dialogMetricRow("Ritmo observado", rate(result.firstRate)))
    body.addView(dialogMetricRow("Cobertura", percentage(result.firstCoverage)))
    body.addView(dialogMetricRow("Temperatura máxima", first.safeDouble("maxTemperatureC")?.let {
        "%.1f °C".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    body.addView(label("Sessão B · ${batteryKind(b.kind)} · ${formatDate(b.startedAt)}", 13f, strong = true))
    body.addView(dialogMetricRow("Variação", second.batteryProgress()))
    body.addView(dialogMetricRow("Ritmo observado", rate(result.secondRate)))
    body.addView(dialogMetricRow("Cobertura", percentage(result.secondCoverage)))
    body.addView(dialogMetricRow("Temperatura máxima", second.safeDouble("maxTemperatureC")?.let {
        "%.1f °C".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    if (result.meaningful) body.addView(dialogMetricRow("Diferença B − A", rate(result.rateDifference)))
    else body.addView(label("Comparação de ritmo não conclusiva. Os valores individuais acima são observados, não garantem desempenho equivalente.", 12f, muted = true))
    result.warnings.forEach { body.addView(label("Atenção: $it", 11.5f, muted = true)) }
    body.addView(label("Taxas em pontos percentuais por hora do tempo decorrido, não energia solar medida. Nenhuma leitura ausente é preenchida.", 11f, muted = true))
    showManagedDialog(themedPopup(this).setTitle("Análise comparativa").setView(scroll)
        .setPositiveButton("Fechar", null).create())
}

private fun MainActivity.showBatteryReportDialog(session: JSONObject) {
    val coverage = BatteryHistoryMath.coverage(session.batteryHistorySession().observedMs,
        session.optLong("durationMs"))
    val coverageText = "%.0f%%".format(Locale.getDefault(), coverage * 100)
    val rate = BatteryHistoryMath.rate(session.batteryHistorySession())?.let {
        "%+.2f p.p./h".format(Locale.getDefault(), it)
    }
    val text = batteryReportText(session) + buildString {
        appendLine()
        appendLine("Cobertura confirmada: $coverageText")
        appendLine("Variação observada: ${session.batteryProgress()}")
        if (rate != null) appendLine("Ritmo observado: $rate")
    }
    val (scroll, body) = dialogWrapper()
    batterySessionHeader(body, session, batteryKind(session.optString("kind")))
    showBatteryChartSelector(body, session)
    batteryDisclosure(body, "Mais detalhes", "Ocultar detalhes") { addView(label(text, 12.5f)) }
    showManagedDialog(themedPopup(this).setTitle("Relatório de bateria").setView(scroll)
        .setNegativeButton("Fechar", null)
        .setPositiveButton("Compartilhar") { _, _ -> shareBatteryReport(text, "Compartilhar relatório") })
}

private fun MainActivity.batteryReportText(item: JSONObject): String = buildString {
    appendLine("Celular Remoto · ${batteryKind(item.optString("kind"))}")
    appendLine("Início: ${formatDate(item.optLong("startedAt"))}")
    appendLine("Duração observada: ${batteryDuration(item.optLong("durationMs"))}")
    appendLine("Bateria: ${item.batteryPercent("initialPercent")} → ${item.batteryPercent("finalPercent")}")
    appendLine("Carregando: ${batteryDuration(item.optLong("chargingMs"))}")
    appendLine("Sem carregar: ${batteryDuration(item.optLong("notChargingMs"))}")
    appendLine("Desconhecido: ${batteryDuration(item.optLong("unknownMs"))}")
    appendLine("Corrente média durante carga: ${item.safeDouble("meanChargeMa")?.let { "%.1f mA".format(Locale.getDefault(), it) } ?: "indisponível"}")
    appendLine("Temperatura inicial/final/máxima: ${listOf("initialTemperatureC", "finalTemperatureC", "maxTemperatureC").joinToString(" / ") { key -> item.safeDouble(key)?.let { "%.1f °C".format(Locale.getDefault(), it) } ?: "—" }}")
    if (item.optString("kind") == "SOLAR") appendLine("Céu informado: ${item.optString("sky")} · painel nominal: ${item.optInt("panelWatts").takeIf { it > 0 }?.let { "$it W" } ?: "não informado"}")
    appendLine("Motivo de término: ${item.optString("stopReason", "em andamento")}")
    append("Medição: corrente líquida da bateria, não produção real do painel solar.")
}

private fun MainActivity.showBatteryAlertDialog(current: JSONObject) {
    val (wrapper, body) = dialogWrapper()
    val destination = if (batteryRemote) "Aparelho remoto" else "Este aparelho"
    body.addView(label(destination, 12f, muted = true))
    fun numberField(caption: String, initial: Int): android.widget.EditText {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val entry = field(caption, initial.toString()).apply {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        row.addView(label(caption, 12f, strong = true).apply {
            labelFor = entry.id
            setPadding(0, dp(4), dp(8), dp(4))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(entry, LinearLayout.LayoutParams(dp(90), -2))
        body.addView(row)
        return entry
    }
    val goal = numberField("Meta de carga (50–100%)", current.optInt("goalPercent", 80))
    val low = numberField("Bateria baixa (5–40%)", current.optInt("lowPercent", 20))
    val hot = numberField("Temperatura de alerta (38–55 °C)", current.optInt("hotC", 43))
    body.addView(label("O alerta não altera a proteção térmica do Android.", 11.5f, muted = true))
    val dialog = themedPopup(this).setTitle("Alertas da bateria").setView(wrapper)
        .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create()
    showManagedDialog(dialog)
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val g = goal.text.toString().toIntOrNull()
        val l = low.text.toString().toIntOrNull()
        val t = hot.text.toString().toIntOrNull()
        if (g == null || l == null || t == null || g !in 50..100 || l !in 5..40 || t !in 38..55) {
            if (g == null || g !in 50..100) goal.error = "Digite de 50 a 100%"
            if (l == null || l !in 5..40) low.error = "Digite de 5 a 40%"
            if (t == null || t !in 38..55) hot.error = "Digite de 38 a 55 °C"
            return@setOnClickListener
        }
        dialog.dismiss()
        queryBatteryReport("alerts", goalPercent = g, lowPercent = l, hotC = t)
    }
}

private fun MainActivity.showBatteryAutomaticReport(session: JSONObject, allowForecast: Boolean = false) {
    val kind = session.optString("kind")
    val type = if (kind == "CHARGE") "Carregamento" else "Descarga"
    val observedAt = session.optLong("lastWall")
    val now = System.currentTimeMillis()
    val validForecast = allowForecast && BatteryDashboardPolicy.isRecent(now, observedAt, SESSION_RECENT_MS)
    val minutes = session.optLong("forecastMinutes", -1L)
    val forecast = if (validForecast) BatteryDashboardPolicy.forecastLabel(kind, minutes) else null
    val detail = buildString {
        appendLine("Celular Remoto · Sessão automática de $type")
        appendLine("Início: ${formatDate(session.optLong("startedAt"))}")
        appendLine("Bateria: ${session.batteryPercent("initialPercent")} → ${session.batteryPercent("finalPercent")}")
        appendLine("Duração: ${batteryDuration(session.optLong("durationMs"))}")
        appendLine("Tempo com leituras consecutivas válidas: ${batteryDuration(session.optLong("observedMs"))}")
        appendLine("Cobertura das medições: ${session.safeDouble("coveragePercent")?.let { "%.0f%%".format(Locale.getDefault(), it) } ?: "indisponível"}")
        appendLine("Corrente média líquida: ${BatteryPresentationMath.currentText(session.safeDouble("meanCurrentMa"), kind = kind)}")
        appendLine("Fonte Android: ${session.optString("powerSource", "").takeUnless { it.isBlank() || it == "null" } ?: "não identificada"}")
        appendLine("Temperatura máxima observada: ${session.safeDouble("maxTemperatureC")?.let { "%.1f °C".format(Locale.getDefault(), it) } ?: "indisponível"}")
        appendLine("Término: ${session.optString("stopReason").ifBlank {
            if (session.optBoolean("active")) "Em andamento" else "Concluída"
        }}")
        if (forecast != null) {
            appendLine("Previsão: $forecast")
            if (minutes > 0) appendLine("Horário aproximado: ${formatDate(observedAt + minutes * 60_000L)}")
            if (kind == "CHARGE") {
                listOf(80 to "forecast80Minutes", 90 to "forecast90Minutes").forEach { (goal, key) ->
                    val remaining = session.optLong(key, -1L)
                    if (remaining >= 0) appendLine("Até $goal%: " + if (remaining == 0L) "alcançado"
                        else "≈ ${BatteryDashboardPolicy.minutesLabel(remaining)}")
                }
            }
            session.safeDouble("forecastRatePercentHour")?.let { rate ->
                appendLine("Ritmo usado na previsão: ${"%.1f p.p./h".format(Locale.getDefault(), rate)}")
            }
            session.optString("forecastBasis").takeIf { it.isNotBlank() }?.let { appendLine("Base da estimativa: $it") }
        }
        append("Previsões são estimativas baseadas na variação do percentual, não medições de capacidade.")
    }
    val (wrapper, body) = dialogWrapper()
    batterySessionHeader(body, session, type, forecast)
    showBatteryChartSelector(body, session)
    batteryDisclosure(body, "Mais detalhes", "Ocultar detalhes") { addView(label(detail, 12.5f)) }
    showManagedDialog(themedPopup(this).setTitle("Sessão automática").setView(wrapper)
        .setNegativeButton("Fechar", null)
        .setPositiveButton("Compartilhar") { _, _ -> shareBatteryReport(detail, "Compartilhar sessão") })
}

private fun MainActivity.batterySessionHeader(body: LinearLayout, session: JSONObject, type: String,
    forecast: String? = null) {
    val started = session.optLong("startedAt", 0L)
    val date = started.takeIf { it > 0 }?.let { formatDate(it) }
    val status = "Em andamento".takeIf { session.optBoolean("active") }
    val meta = listOfNotNull(type, date, status).joinToString(" · ")
    body.addView(label(meta, 12f, muted = true))
    val progress = "${session.batteryPercent("initialPercent")} → ${session.batteryPercent("finalPercent")}"
    body.addView(label("$progress · ${batteryDuration(session.optLong("durationMs"))}", 19f, strong = true))
    if (forecast != null) body.addView(label(forecast, 13f, strong = true))
}

private fun MainActivity.showBatteryChartSelector(body: LinearLayout, session: JSONObject) {
    val points = session.optJSONArray("points")
    if (points == null || points.length() < 2) {
        body.addView(label("Sem amostras suficientes para exibir o gráfico.", 11.5f, muted = true))
        return
    }
    val metrics = listOf("percent" to "Nível (%)", "ma" to "Corrente líquida (mA)", "temp" to "Temperatura (°C)")
    val chartHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val selector = Spinner(themedPopup(this).context, Spinner.MODE_DROPDOWN).apply {
        contentDescription = "Métrica do gráfico"
        minimumHeight = dp(48)
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, metrics.map { it.second }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val (metric, title) = metrics[position]
                chartHost.removeAllViews()
                val chart = BatteryLabChart(this@showBatteryChartSelector, points, metric, ink, highlight,
                    session.optLong("startedAt", 0L)).apply {
                    contentDescription = "$title ao longo da sessão"
                }
                chartHost.addView(chart, LinearLayout.LayoutParams(-1, dp(122)))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
    body.addView(selector, LinearLayout.LayoutParams(-1, -2))
    body.addView(chartHost, LinearLayout.LayoutParams(-1, -2))
}

private fun MainActivity.shareBatteryReport(text: String, title: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        this.type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, title))
}

/** Gráfico com eixos, faixas e lacunas: nunca conecta amostras com diferença >90s. */
private class BatteryLabChart(context: Context, private val samples: JSONArray, private val metric: String,
    ink: Int, accent: Int, private val sessionStartMs: Long) : View(context) {
    private val density = resources.displayMetrics.density
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent; style = Paint.Style.STROKE; strokeWidth = 2.5f * density
    }
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.FILL }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; alpha = 46; strokeWidth = 1f * density }
    private val axisText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink; alpha = 175; textSize = 10f * resources.displayMetrics.scaledDensity
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val right = width.toFloat() - 8f * density
        val top = 5f * density + axisText.textSize / 2
        val timeBaseline = height - 3f * density - axisText.descent()
        val bottom = timeBaseline - axisText.textSize - 3f * density
        if (right <= 0 || bottom <= top) return
        val entries = (0 until samples.length()).mapNotNull { samples.optJSONObject(it) }
            .map { sample ->
                val value = when (metric) {
                    "percent" -> sample.optInt("percent", -1).takeIf { it in 0..100 }?.toDouble()
                    "ma" -> (sample.safeDouble("ma") ?: sample.safeDouble("currentMa"))?.takeIf { it in -30_000.0..30_000.0 }
                    "temp" -> (sample.safeDouble("temp") ?: sample.safeDouble("temperatureC"))?.takeIf { it in -30.0..100.0 }
                    else -> null
                }
                sample to value
            }
        val valid = entries.filter { it.second != null && it.first.optLong("wall", 0L) > 0L }
        if (valid.size < 2) {
            drawInsufficient(canvas, (bottom + top) / 2f)
            return
        }
        val minT = valid.minOf { it.first.optLong("wall") }
        val maxT = valid.maxOf { it.first.optLong("wall") }
        if (maxT <= minT) {
            drawInsufficient(canvas, (bottom + top) / 2f)
            return
        }
        val values = valid.mapNotNull { it.second }
        val minimum = values.minOrNull() ?: return
        val maximum = values.maxOrNull() ?: return
        val pad = (maximum - minimum).coerceAtLeast(1.0) * 0.12
        val low = minimum - pad
        val high = maximum + pad
        val span = high - low
        val locale = Locale.getDefault()
        val ticks = listOf(0f, 1f, 0.5f).mapNotNull { ratio ->
            BatteryDashboardPolicy.axisLabel(metric, low + ratio * span, locale)?.let { ratio to it }
        }.distinctBy { it.second }
        val left = (ticks.maxOfOrNull { axisText.measureText(it.second) } ?: 0f) + 12f * density
        if (right <= left) return
        ticks.forEach { (ratio, tick) ->
            val y = bottom - ratio * (bottom - top)
            canvas.drawLine(left, y, right, y, grid)
            canvas.drawText(tick, 2f * density, y - (axisText.ascent() + axisText.descent()) / 2, axisText)
        }
        val origin = sessionStartMs.takeIf { it in 1..minT } ?: minT
        val firstTime = batteryDuration(minT - origin)
        val lastTime = batteryDuration(maxT - origin)
        val endX = right - axisText.measureText(lastTime)
        canvas.drawText(firstTime, left, timeBaseline, axisText)
        if (lastTime != firstTime && endX >= left + axisText.measureText(firstTime) + 6f * density)
            canvas.drawText(lastTime, endX, timeBaseline, axisText)
        fun x(t: Long): Float = left + (t - minT).toFloat() / (maxT - minT) * (right - left)
        fun y(v: Double): Float = bottom - ((v - low) / span).toFloat() * (bottom - top)
        var previous: Pair<JSONObject, Double>? = null
        entries.forEach { (item, value) ->
            if (value == null || item.optLong("wall", 0L) <= 0L) {
                previous = null // Não conecta sobre amostra inválida.
                return@forEach
            }
            val prior = previous
            val t = item.optLong("wall")
            if (prior != null) {
                val deltaWall = t - prior.first.optLong("wall")
                val deltaElapsed = item.optLong("elapsed", -1L) - prior.first.optLong("elapsed", -1L)
                if (deltaWall in 1..90_000L && deltaElapsed in 1..90_000L &&
                    kotlin.math.abs(deltaElapsed - deltaWall) <= 10_000L) {
                    canvas.drawLine(x(prior.first.optLong("wall")), y(prior.second), x(t), y(value), stroke)
                }
            }
            canvas.drawCircle(x(t), y(value), 1.65f * density, mark)
            previous = item to value
        }
    }
    private fun drawInsufficient(canvas: Canvas, y: Float) {
        val text = "Poucas amostras"
        canvas.drawText(text, ((width - axisText.measureText(text)) / 2).coerceAtLeast(0f), y, axisText)
    }
}
