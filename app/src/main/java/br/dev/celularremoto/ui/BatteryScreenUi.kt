package br.dev.celularremoto.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
import android.text.InputType
import br.dev.celularremoto.R
import br.dev.celularremoto.core.BatteryLabStore
import br.dev.celularremoto.core.BatteryHistoryMath
import br.dev.celularremoto.core.BatteryGoalAlerts
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

/** Quinta aba: um único app coleta no celular monitorado e mostra os dados em ambos. */
internal fun MainActivity.renderBatteryScreen() {
    val host = SettingsStore.getRemoteHost(this)
    val paired = host.isNotBlank() && RecentDevices.key(this, host) != null
    if (batteryRemote && !paired) {
        batteryRemote = false
        batteryData = null
        batteryError = null
    }
    val switchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    switchRow.addView(actionButton("Este aparelho", !batteryRemote) { selectBatteryDevice(false) },
        LinearLayout.LayoutParams(0, dp(45), 1f).apply { rightMargin = dp(5) })
    switchRow.addView(actionButton("Remoto", batteryRemote) { selectBatteryDevice(true) }.apply {
        isEnabled = paired
        alpha = if (paired) 1f else 0.5f
    }, LinearLayout.LayoutParams(0, dp(45), 1f))
    content.addView(switchRow)
    gap(11)
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
    if (paired && MonitorService.isServiceActive) {
        content.addView(statePill("Modo duplo disponível: este aparelho também está Monitorado", "active"))
        gap(7)
    }
    val status = when {
        !batteryRemote -> "Leitura deste aparelho"
        batteryLoading -> "Consultando aparelho pareado…"
        batteryError != null -> "Não foi possível atualizar"
        data != null -> "Última resposta autenticada: ${formatDate(data.optLong("reportedAt"))}"
        else -> "Aguardando consulta autenticada"
    }
    content.addView(label(status, 11.5f, muted = true))
    if (batteryRemote && batteryError != null)
        content.addView(label(batteryError.orEmpty(), 12.5f, muted = true))
    if (batteryRemote) {
        content.addView(button(if (batteryLoading) "Consultando…" else "Atualizar medições") {
            if (!batteryLoading) queryBatteryReport()
        }.apply { isEnabled = !batteryLoading })
        if (data == null && !batteryLoading && batteryError == null)
            content.post { if (active && mode == 5 && batteryRemote) queryBatteryReport() }
    } else if (!MonitorService.isServiceActive) {
        content.addView(dialogCardRow("Ative o modo Monitorado",
            "O serviço existente é necessário para coletar com a tela apagada.") { requestStart() })
    }
    val battery = data?.optJSONObject("battery")
    if (battery != null) {
        gap(5)
        val percent = battery.optInt("levelPercent", -1).takeIf { it in 0..100 }?.let { "$it%" } ?: "—"
        val state = battery.optString("state", "Desconhecido")
        val powerSource = battery.optString("powerSource", "")
            .takeUnless { it.isBlank() || it == "null" } ?: "não identificada"
        val ma = battery.safeDouble("currentMa")
        val voltage = battery.optInt("voltageMv", -1)
        val temp = battery.safeDouble("temperatureC")
        val watts = battery.safeDouble("batteryNetPowerW")
        val one = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        one.addView(batteryMetric("Bateria", percent, state, R.drawable.ic_battery), LinearLayout.LayoutParams(0, -2, 1f))
        one.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        one.addView(batteryMetric("Corrente líquida", ma?.let { "%+.0f mA".format(Locale.getDefault(), it) } ?: "—",
            when (battery.optString("currentSource")) {
                "NOW" -> "Instantânea · Android"
                "AVERAGE" -> "Média do hardware · Android"
                "COUNTER" -> "Contador de carga · média observada"
                else -> "Leitura indisponível"
            }, R.drawable.ic_activity), LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(one)
        gap(7)
        val two = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        two.addView(batteryMetric("Temperatura", temp?.let { "%.1f °C".format(Locale.getDefault(), it) } ?: "—",
            "Sensor da bateria", R.drawable.ic_thermometer), LinearLayout.LayoutParams(0, -2, 1f))
        two.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        two.addView(batteryMetric("Potência líquida", watts?.let { "%+.2f W".format(Locale.getDefault(), it) } ?: "—",
            if (voltage > 0) "${voltage} mV · não é potência solar" else "Tensão indisponível", R.drawable.ic_power),
            LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(two)
        val warning = battery.optString("currentWarning", "")
            .takeUnless { it.isBlank() || it == "null" }
        if (warning != null) content.addView(statePill(warning, "warning"))
        content.addView(dialogCardRow("Diagnóstico da corrente", "Leituras brutas, média Android e contador de carga · ${battery.optString("currentSource", "NONE")}") {
            showBatteryCurrentDiagnostic(battery)
        })
        content.addView(label("Corrente líquida da bateria, não da entrada USB. Fonte: $powerSource. A geração solar real exige medidor externo.", 11f, muted = true))
    }
    val goalAlerts = data?.optJSONObject("goalAlerts")
    if (goalAlerts != null) {
        gap(9)
        content.addView(dialogCardRow("🔔 Alertas e metas do aparelho",
            "Carga ${goalAlerts.optInt("goalPercent", 80)}% · baixa ${goalAlerts.optInt("lowPercent", 20)}% · temperatura ${goalAlerts.optInt("hotC", 43)} °C") {
            showBatteryAlertDialog(goalAlerts)
        })
        if (batteryRemote && !SettingsStore.controllerHealthAlerts(this)) {
            content.addView(label("Alertas no controlador desativados. O aparelho monitorado continua registrando eventos locais.", 11f, muted = true))
            content.addView(button("Ativar alertas neste controlador") {
                SettingsStore.setControllerHealthAlerts(this, true)
                render()
            })
        }
    }
    gap(15)
    val sections = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    listOf("resumo" to "Resumo", "testes" to "Testes", "historico" to "Histórico").forEachIndexed { index, (id, caption) ->
        sections.addView(actionButton(caption, batterySection == id) {
            batterySection = id
            render()
        }, LinearLayout.LayoutParams(0, dp(43), 1f).apply { if (index < 2) rightMargin = dp(4) })
    }
    content.addView(sections)
    gap(11)
    val active = data?.optJSONObject("active")
    if (active != null && !batteryRemote && !MonitorService.isServiceActive)
        content.addView(statePill("Coleta pausada: serviço Monitorado inativo", "warning"))
    val history = data?.optJSONArray("history") ?: JSONArray()
    when (batterySection) {
        "testes" -> renderBatteryTests(active)
        "historico" -> renderBatteryHistory(history, data?.optJSONObject("automatic"))
        else -> {
            renderAutomaticBatterySummary(data?.optJSONObject("automatic"))
            if (active != null) renderBatteryActive(active)
            else content.addView(label("Nenhum teste em andamento neste aparelho.", 13f, muted = true))
            if (history.length() > 0) {
                gap(12)
                content.addView(label("Última sessão", 16f, strong = true))
                val last = history.optJSONObject(0)
                if (last != null) renderBatterySessionCard(last, compact = true)
            }
        }
    }
    gap(12)
    content.addView(label("Medições locais pelo serviço Monitorado. Testes são avaliados a cada 30 s; sessões automáticas têm pontos a cada 60 s ou mudança de percentual. Lacunas e previsões insuficientes não são inventadas. Sem internet.", 11f, muted = true))
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

private fun MainActivity.batteryMetric(title: String, value: String, subtitle: String, icon: Int): LinearLayout = panel(11).apply {
    val heading = LinearLayout(this@batteryMetric).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }
    heading.addView(vectorIcon(icon, 17, dim), LinearLayout.LayoutParams(dp(17), dp(17)))
    heading.addView(label(title, 11.5f, muted = true), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
    addView(heading)
    addView(label(value, 21f, strong = true))
    addView(label(subtitle, 10.5f, muted = true).apply { maxLines = 2 })
}

private fun MainActivity.showBatteryCurrentDiagnostic(battery: JSONObject) {
    val rawNow = battery.optInt("rawNowMicroA", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    val rawAverage = battery.optInt("rawAverageMicroA", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    val counter = battery.optInt("chargeCounterMicroAh", -1).takeIf { it >= 0 }
    val counterWindow = battery.optLong("counterWindowMs", -1L).takeIf { it > 0 }
    val (wrapper, body) = dialogWrapper()
    body.addView(label("Diagnóstico das propriedades BatteryManager", 14f, strong = true))
    body.addView(dialogMetricRow("Corrente instantânea Android", battery.safeDouble("instantMa")?.let {
        "%+.1f mA".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    body.addView(dialogMetricRow("Corrente média Android", battery.safeDouble("averageMa")?.let {
        "%+.1f mA".format(Locale.getDefault(), it)
    } ?: "Indisponível"))
    body.addView(dialogMetricRow("Média pelo contador de carga", battery.safeDouble("counterMeanMa")?.let {
        "%+.1f mA".format(Locale.getDefault(), it) + (counterWindow?.let { window -> " · janela ${window / 1000} s" } ?: "")
    } ?: "Aguardando ≥90 s de amostras válidas"))
    body.addView(dialogMetricRow("CURRENT_NOW bruto", rawNow?.let { "$it µA" } ?: "Indisponível"))
    body.addView(dialogMetricRow("CURRENT_AVERAGE bruto", rawAverage?.let { "$it µA" } ?: "Indisponível"))
    body.addView(dialogMetricRow("CHARGE_COUNTER bruto", counter?.let { "$it µAh" } ?: "Indisponível"))
    body.addView(dialogMetricRow("Origem selecionada", battery.optString("currentSource", "NONE")))
    body.addView(label(battery.optString("currentDiagnostic", "Leitura indisponível"), 12f, muted = true))
    val warning = battery.optString("currentWarning", "").takeUnless { it.isBlank() || it == "null" }
    if (warning != null) body.addView(label("Atenção: $warning", 12f, muted = true))
    body.addView(label("Sinais e unidades são reportados pelo Android. O contador exige observação contínua; não há correção automática de unidades do fabricante sem evidência suficiente. Potência líquida da bateria ≠ potência do painel solar.", 11.5f, muted = true))
    showManagedDialog(themedPopup(this).setTitle("Diagnóstico elétrico").setView(wrapper)
        .setPositiveButton("Fechar", null).create())
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
    options.forEach { (kind, title, description) ->
        content.addView(dialogCardRow(title, description) { showBatteryStartDialog(kind) })
        gap(7)
    }
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
    content.addView(statePill("Teste em andamento · ${batteryKind(session.optString("kind"))}", "active"))
    renderBatterySessionCard(session, compact = false)
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

private fun batteryDuration(ms: Long): String {
    val sec = (ms.coerceAtLeast(0) / 1000)
    return "${sec / 3600}h ${(sec % 3600) / 60}min ${sec % 60}s"
}

private fun JSONObject.batteryPercent(field: String): String =
    optInt(field, -1).takeIf { it in 0..100 }?.let { "$it%" } ?: "—"

private fun MainActivity.renderBatterySessionCard(session: JSONObject, compact: Boolean) {
    val box = panel()
    box.addView(label("${batteryKind(session.optString("kind"))} · ${formatDate(session.optLong("startedAt"))}", 14f, strong = true))
    box.addView(label("${session.batteryPercent("initialPercent")} → ${session.batteryPercent("finalPercent")} · ${batteryDuration(session.optLong("durationMs"))}", 13.5f))
    val observed = session.batteryHistorySession()
    if (session.optLong("durationMs") > 0L) box.addView(label("${session.batteryProgress()} · cobertura ${"%.0f".format(Locale.getDefault(), BatteryHistoryMath.coverage(observed.observedMs, observed.durationMs) * 100)}%", 11.5f, muted = true))
    if (!compact) {
        box.addView(dialogMetricRow("Carregando confirmado", batteryDuration(session.optLong("chargingMs"))))
        box.addView(dialogMetricRow("Sem carregar confirmado", batteryDuration(session.optLong("notChargingMs"))))
        box.addView(dialogMetricRow("Sem dados suficientes", batteryDuration(session.optLong("unknownMs"))))
        box.addView(dialogMetricRow("Corrente média durante carga", session.safeDouble("meanChargeMa")?.let {
            "%.0f mA".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        box.addView(dialogMetricRow("Temperatura máxima", session.safeDouble("maxTemperatureC")?.let {
            "%.1f °C".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        if (session.optString("kind") == "SOLAR") box.addView(dialogMetricRow("Céu / painel declarado",
            "${session.optString("sky")} · ${session.optInt("panelWatts").takeIf { it > 0 }?.let { "$it W (nominal)" } ?: "potência não informada"}"))
    }
    val points = session.optJSONArray("points")
    if (points != null && points.length() >= 2) {
        listOf(
            Triple("percent", "Bateria (%)", 0),
            Triple("ma", "Corrente líquida (mA)", 1),
            Triple("temp", "Temperatura (°C)", 2)
        ).forEach { (metric, title, _) ->
            box.addView(label("$title · amostras reais recentes", 11f, muted = true))
            box.addView(BatteryLabChart(this, points, metric, ink, highlight), LinearLayout.LayoutParams(-1, dp(112)))
        }
    }
    if (session.optBoolean("active")) box.addView(label("Em andamento · valores apenas até a última leitura salva", 11f, muted = true))
    else box.addView(label(session.optString("stopReason", "Concluído"), 11.5f, muted = true))
    if (compact) box.setOnClickListener { showBatteryReportDialog(session) }
    content.addView(box)
}

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
    content.addView(label("Histórico do aparelho selecionado", 16f, strong = true))
    content.addView(label("Filtros aplicados aos registros recebidos deste aparelho. Valores não são preenchidos em lacunas.", 11.5f, muted = true))
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
        val note = "$badge${if (badge.isNotBlank()) " · " else ""}${item.batteryProgress()} · " +
            "${batteryDuration(item.optLong("durationMs"))}" +
            (if (cover != null && item.optLong("durationMs") > 0L) " · cobertura ${"%.0f".format(Locale.getDefault(), cover)}%" else "") +
            (rate?.let { " · ${"%+.1f".format(Locale.getDefault(), it)} p.p./h" } ?: "")
        content.addView(dialogCardRow("$labelKind · ${formatDate(item.optLong("startedAt"))}",
            "${item.batteryPercent("initialPercent")} → ${item.batteryPercent("finalPercent")}$note") {
            if (isAuto) showBatteryAutomaticReport(item) else showBatteryReportDialog(item)
        })
        gap(6)
    }
    content.addView(label("Até 24 resumos de testes e 24 sessões automáticas são enviados pelo aparelho. Gráficos são incluídos apenas nas sessões mais recentes para poupar Wi-Fi e bateria. O histórico local é maior.", 11f, muted = true))
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
    val text = batteryReportText(session)
    val (scroll, body) = dialogWrapper()
    body.addView(label(text, 13f))
    val coverage = BatteryHistoryMath.coverage(session.batteryHistorySession().observedMs,
        session.optLong("durationMs"))
    body.addView(dialogMetricRow("Cobertura confirmada", "%.0f%%".format(Locale.getDefault(), coverage * 100)))
    body.addView(dialogMetricRow("Variação observada", session.batteryProgress()))
    BatteryHistoryMath.rate(session.batteryHistorySession())?.let {
        body.addView(dialogMetricRow("Ritmo observado", "%+.2f p.p./h".format(Locale.getDefault(), it)))
    }
    val points = session.optJSONArray("points")
    if (points != null && points.length() >= 2) {
        listOf("percent" to "Bateria (%)", "ma" to "Corrente líquida (mA)", "temp" to "Temperatura (°C)").forEach { (metric, title) ->
            body.addView(label(title, 12f, strong = true))
            body.addView(BatteryLabChart(this, points, metric, ink, highlight), LinearLayout.LayoutParams(-1, dp(150)))
        }
    } else body.addView(label("Gráficos não incluídos neste resumo remoto. Consulte as sessões mais recentes ou o aparelho de origem.", 11f, muted = true))
    showManagedDialog(themedPopup(this).setTitle("Relatório de bateria").setView(scroll)
        .setNegativeButton("Fechar", null)
        .setPositiveButton("Compartilhar") { _, _ ->
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Compartilhar relatório"))
        })
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
    body.addView(label("Configurar metas e alertas", 17f, strong = true))
    body.addView(label("As alterações são salvas no aparelho escolhido. O limite de segurança térmica do Android continua independente.", 12f, muted = true))
    fun numberField(caption: String, initial: Int): android.widget.EditText {
        body.addView(label(caption, 12f, strong = true))
        return field(caption, initial.toString()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            body.addView(this)
        }
    }
    val goal = numberField("Meta de carga (50–100%)", current.optInt("goalPercent", 80))
    val low = numberField("Bateria baixa (5–40%)", current.optInt("lowPercent", 20))
    val hot = numberField("Temperatura de alerta (38–55 °C)", current.optInt("hotC", 43))
    val dialog = themedPopup(this).setTitle("Alertas da bateria").setView(wrapper)
        .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create()
    showManagedDialog(dialog)
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val g = goal.text.toString().toIntOrNull()
        val l = low.text.toString().toIntOrNull()
        val t = hot.text.toString().toIntOrNull()
        if (g == null || l == null || t == null || g !in 50..100 || l !in 5..40 || t !in 38..55) {
            showAppNotice("Limites inválidos", "Meta 50–100%, bateria baixa 5–40% e temperatura 38–55 °C.")
            return@setOnClickListener
        }
        dialog.dismiss()
        queryBatteryReport("alerts", goalPercent = g, lowPercent = l, hotC = t)
    }
}

private fun MainActivity.renderAutomaticBatterySummary(data: JSONObject?) {
    content.addView(label("Acompanhamento automático", 16f, strong = true))
    val active = data?.optJSONObject("active")
    if (active == null) {
        content.addView(label("Sem sessão de carga/descarga ativa. Ative o Monitorado para registrar automaticamente.", 12.5f, muted = true))
    } else {
        val mode = if (active.optString("kind") == "CHARGE") "Carregamento" else "Descarga"
        val paused = System.currentTimeMillis() - active.optLong("lastWall") > 150_000L
        content.addView(statePill(if (paused) "Sessão sem leituras recentes" else "$mode em andamento",
            if (paused) "warning" else "active"))
        val forecast = active.optLong("forecastMinutes", -1L)
        fun etaText(minutes: Long): String = if (minutes == 0L) "Meta alcançada" else
            "~${minutes / 60}h ${minutes % 60}min · aprox. ${formatDate(System.currentTimeMillis() + minutes * 60_000L)}"
        content.addView(dialogMetricRow(if (mode == "Carregamento") "Previsão até 100%" else "Autonomia até 0%",
            if (paused) "Indisponível: coleta pausada" else if (forecast >= 0L) etaText(forecast)
            else "Calculando… (exige tendência válida ou sessões comparáveis)"))
        if (!paused && mode == "Carregamento" && forecast >= 0L) {
            listOf("Até 80%" to "forecast80Minutes", "Até 90%" to "forecast90Minutes").forEach { (caption, key) ->
                val value = active.optLong(key, -1L)
                if (value >= 0L) content.addView(dialogMetricRow(caption, etaText(value)))
            }
        }
        val rate = active.safeDouble("forecastRatePercentHour")
        if (!paused && rate != null) content.addView(dialogMetricRow("Ritmo estimado",
            "%.1f p.p./h".format(Locale.getDefault(), rate)))
        val basis = active.optString("forecastBasis", "")
        if (!paused && basis.isNotBlank()) content.addView(label("Base: $basis. Previsão aproximada, não garantia de horário.", 11f, muted = true))
        content.addView(dialogMetricRow("Variação observada",
            "${active.batteryPercent("initialPercent")} → ${active.batteryPercent("finalPercent")}"))
        content.addView(dialogMetricRow("Intervalos efetivamente observados", batteryDuration(active.optLong("observedMs"))))
        content.addView(dialogMetricRow("Cobertura das leituras", active.safeDouble("coveragePercent")?.let {
            "%.0f%%".format(Locale.getDefault(), it)
        } ?: "Indisponível"))
        content.addView(dialogMetricRow("Corrente líquida média da sessão", active.safeDouble("meanCurrentMa")?.let {
            "%+.0f mA".format(Locale.getDefault(), it)
        } ?: "Sem leituras suficientes"))
    }
    val recent = data?.optJSONArray("history") ?: JSONArray()
    if (recent.length() > 0) {
        content.addView(label("Últimas sessões de carga e descarga", 14f, strong = true))
        for (i in 0 until minOf(3, recent.length())) {
            val item = recent.optJSONObject(i) ?: continue
            val sessionType = if (item.optString("kind") == "CHARGE") "Carregamento" else "Descarga"
            content.addView(dialogCardRow("$sessionType · ${formatDate(item.optLong("startedAt"))}",
                "${item.batteryPercent("initialPercent")} → ${item.batteryPercent("finalPercent")} · ${batteryDuration(item.optLong("durationMs"))}") {
                showBatteryAutomaticReport(item)
            })
        }
    }
    gap(7)
}

private fun MainActivity.renderAutoSessionList(sessions: JSONArray) {
    if (sessions.length() == 0) {
        content.addView(label("Nenhuma sessão automática registrada.", 12.5f, muted = true))
        return
    }
    for (i in 0 until sessions.length()) {
        val record = sessions.optJSONObject(i) ?: continue
        val kind = if (record.optString("kind") == "CHARGE") "Carregamento" else "Descarga"
        content.addView(dialogCardRow("${if (kind == "Carregamento") "⚡" else "▾"} $kind · ${formatDate(record.optLong("startedAt"))}",
            "${record.batteryPercent("initialPercent")} → ${record.batteryPercent("finalPercent")} · ${batteryDuration(record.optLong("durationMs"))}") {
            showBatteryAutomaticReport(record)
        })
        gap(6)
    }
    content.addView(label("Até 12 sessões recentes são exibidas; o aparelho conserva até 120. Leituras são limitadas para economizar armazenamento.", 11f, muted = true))
}

private fun MainActivity.showBatteryAutomaticReport(session: JSONObject) {
    val type = if (session.optString("kind") == "CHARGE") "Carregamento" else "Descarga"
    val detail = buildString {
        appendLine("Celular Remoto · Sessão automática de $type")
        appendLine("Início: ${formatDate(session.optLong("startedAt"))}")
        appendLine("Bateria: ${session.batteryPercent("initialPercent")} → ${session.batteryPercent("finalPercent")}")
        appendLine("Duração: ${batteryDuration(session.optLong("durationMs"))}")
        appendLine("Tempo com leituras consecutivas válidas: ${batteryDuration(session.optLong("observedMs"))}")
        appendLine("Cobertura das medições: ${session.safeDouble("coveragePercent")?.let { "%.0f%%".format(Locale.getDefault(), it) } ?: "indisponível"}")
        appendLine("Corrente média líquida: ${session.safeDouble("meanCurrentMa")?.let { "%+.0f mA".format(Locale.getDefault(), it) } ?: "indisponível"}")
        appendLine("Fonte Android: ${session.optString("powerSource", "").ifBlank { "não identificada" }}")
        appendLine("Temperatura máxima observada: ${session.safeDouble("maxTemperatureC")?.let { "%.1f °C".format(Locale.getDefault(), it) } ?: "indisponível"}")
        appendLine("Término: ${session.optString("stopReason").ifBlank { "Em andamento" }}")
        append("Previsões são estimativas baseadas na variação real do percentual, não medições de capacidade.")
    }
    val (wrapper, body) = dialogWrapper()
    body.addView(label(detail, 12.5f))
    val points = session.optJSONArray("points")
    if (points != null && points.length() >= 2) {
        listOf("percent" to "Nível (%)", "ma" to "Corrente (mA)", "temp" to "Temperatura (°C)").forEach { (metric, title) ->
            body.addView(label(title, 12f, strong = true))
            body.addView(BatteryLabChart(this, points, metric, ink, highlight), LinearLayout.LayoutParams(-1, dp(110)))
        }
    }
    showManagedDialog(themedPopup(this).setTitle("Sessão automática").setView(wrapper)
        .setNegativeButton("Fechar", null)
        .setPositiveButton("Compartilhar") { _, _ ->
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, detail)
            }, "Compartilhar sessão"))
        }.create())
}

/** Gráfico com eixos, faixas e lacunas: nunca conecta amostras com diferença >90s. */
private class BatteryLabChart(context: Context, private val samples: JSONArray, private val metric: String,
    private val ink: Int, private val accent: Int) : View(context) {
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
        val left = 43f * density
        val right = width.toFloat() - 7f * density
        val top = 7f * density
        val bottom = height.toFloat() - 19f * density
        if (right <= left || bottom <= top) return
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
            canvas.drawText("Sem amostras suficientes", left, (bottom + top) / 2f, axisText)
            return
        }
        val minT = valid.minOf { it.first.optLong("wall") }
        val maxT = valid.maxOf { it.first.optLong("wall") }
        if (maxT <= minT) return
        val values = valid.mapNotNull { it.second }
        val minimum = values.minOrNull() ?: return
        val maximum = values.maxOrNull() ?: return
        val pad = (maximum - minimum).coerceAtLeast(1.0) * 0.12
        val low = minimum - pad
        val high = maximum + pad
        val span = high - low
        for (fraction in 0..2) {
            val ratio = fraction / 2f
            val y = bottom - ratio * (bottom - top)
            canvas.drawLine(left, y, right, y, grid)
            val value = low + ratio * span
            val label = if (metric == "percent") "%.0f%%".format(Locale.getDefault(), value)
                else "%.0f".format(Locale.getDefault(), value)
            canvas.drawText(label, 2f * density, y + 4f * density, axisText)
        }
        val firstTime = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(minT))
        val lastTime = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(maxT))
        canvas.drawText(firstTime, left, height - 3f * density, axisText)
        canvas.drawText(lastTime, (right - axisText.measureText(lastTime)).coerceAtLeast(left), height - 3f * density, axisText)
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
}
