package br.dev.celularremoto.ui

import android.view.Gravity
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.ConnectionHealthMath
import br.dev.celularremoto.core.ConnectionHealthMonitor
import br.dev.celularremoto.core.ConnectionHealthStore
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.ConnectionHealthScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

internal fun MainActivity.openConnectionDiagnostics() {
    openSettingsModule()
    openSettingsSection("connection")
}

/** Sem afirmar "conectado permanentemente": relatório apenas de tentativas executadas. */
internal fun MainActivity.fillConnectionHealthSettings(layout: LinearLayout) {
    val host = SettingsStore.getRemoteHost(this)
    val enabled = ConnectionHealthStore.enabled(this)
    val status = ConnectionHealthStore.status(this, host)
    val recent = status.samples.filter { it.atMs >= System.currentTimeMillis() - 24L * 60 * 60_000L }
    val summary = ConnectionHealthMath.summarize(recent)
    val name = RecentDevices.list(this).firstOrNull { it.host == host }?.name ?: host
    layout.addView(visualHeader(R.drawable.ic_activity, "Estabilidade da conexão",
        "Sondas autenticadas e histórico deste controlador"))
    sectionGap(layout, 9)
    layout.addView(label(if (host.isBlank()) "Nenhum aparelho selecionado" else "Aparelho: $name", 13f, strong = true))
    sectionGap(layout, 7)
    layout.addView(label(
        "Não existe um socket TCP permanente. Esta página mede respostas às consultas realmente realizadas, " +
        "sem considerar períodos sem amostras como tempo online ou offline.", 12f, muted = true))
    sectionGap(layout, 12)

    fun item(title: String, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(label(title, 12.5f, muted = true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(value, 13f, strong = true))
        layout.addView(row)
        sectionGap(layout, 8)
    }
    val fmt = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    item("Estado recente", when {
        host.isBlank() -> "Sem aparelho"
        status.failureStreak >= ConnectionHealthMath.ALERT_AFTER_FAILURES -> "Sem resposta confirmada"
        else -> ConnectionHealthMath.connectionLabel(status.lastSuccessMs, System.currentTimeMillis())
    })
    item("Última resposta", if (status.lastSuccessMs > 0) fmt.format(Date(status.lastSuccessMs)) else "Nenhuma")
    item("Última tentativa", if (status.lastAttemptMs > 0) fmt.format(Date(status.lastAttemptMs)) else "Nenhuma")
    item("Latência da última resposta", status.lastRttMs?.let { "$it ms" } ?: "Não disponível")
    item("Disponibilidade observada", summary.availabilityPercent?.let { "%.1f%%".format(Locale.ROOT, it) } ?: "Sem amostras")
    item("Tentativas confirmadas", "${summary.attempts} (${summary.succeeded} respostas / ${summary.failed} falhas)")
    item("Latência média", summary.meanRttMs?.let { "$it ms" } ?: "Não disponível")
    item("Falhas consecutivas", "${status.failureStreak}")
    item("Intervalos sem amostra", "${summary.missedWindows}")
    item("Maior sequência de falhas observada", if (summary.longestObservedFailureMs > 0)
        "${summary.longestObservedFailureMs / 1_000} s entre sondagens" else "Não calculada")
    if (status.lastReason.isNotBlank() && status.failureStreak > 0)
        layout.addView(label("Última falha: ${status.lastReason}", 11f, muted = true))
    sectionGap(layout, 10)

    val bars = recent.takeLast(20)
    if (bars.isNotEmpty()) {
        layout.addView(label("Últimas ${bars.size} verificações", 12.5f, strong = true))
        sectionGap(layout, 5)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        bars.forEachIndexed { i, sample ->
            if (i > 0 && sample.atMs - bars[i-1].atMs > ConnectionHealthMath.MISSING_SAMPLE_AFTER_MS) {
                row.addView(label("·", 14f, muted = true))
            }
            row.addView(label(if (sample.succeeded) "▮" else "▯", 13f, strong = true).apply {
                setTextColor(if (sample.succeeded) highlight else android.graphics.Color.rgb(204, 92, 70))
                contentDescription = if (sample.succeeded) "Resposta autenticada" else "Falha de consulta"
            })
        }
        layout.addView(row)
        layout.addView(label("▮ respondeu  ▯ falhou  · sem verificação. O Android pode adiar sondagens em segundo plano.", 10.5f, muted = true))
        sectionGap(layout, 10)
    }

    layout.addView(iconMenuRow(R.drawable.ic_activity, "Diagnóstico com o app aberto",
        if (enabled) "Ativo, consulta leve a cada ~30 s (Controle reaproveita as consultas)" else "Desligado",
        badge = SettingsBadgeKind.forEnabled(enabled)) {
        ConnectionHealthStore.setEnabled(this, !enabled)
        ConnectionHealthScheduler.refresh(this)
        openSettingsSection("connection")
    })
    sectionGap(layout, 8)
    val bg = ConnectionHealthStore.backgroundEnabled(this)
    layout.addView(iconMenuRow(R.drawable.ic_activity, "Verificar em segundo plano",
        if (bg) "JobScheduler ~15 min; pode atrasar conforme Android" else "Desligado para economizar energia",
        badge = SettingsBadgeKind.forEnabled(bg)) {
        ConnectionHealthStore.setBackgroundEnabled(this, !bg)
        ConnectionHealthScheduler.refresh(this)
        openSettingsSection("connection")
    })
    sectionGap(layout, 8)
    val alerts = ConnectionHealthStore.alertsEnabled(this)
    layout.addView(iconMenuRow(R.drawable.ic_bell, "Avisos de queda e recuperação",
        if (alerts) "Avisa após 3 falhas e quando voltar a responder" else "Notificações desativadas",
        badge = SettingsBadgeKind.forEnabled(alerts)) {
        ConnectionHealthStore.setAlertsEnabled(this, !alerts)
        openSettingsSection("connection")
    })
    sectionGap(layout, 10)
    layout.addView(actionButton("Verificar agora e tentar reconectar") {
        if (host.isBlank() || RecentDevices.key(this, host) == null) {
            showAppNotice("Diagnóstico", "Selecione e pareie um aparelho primeiro.")
        } else {
            val application = applicationContext
            thread(name = "CelularRemoto-manual-health") {
                val response = ConnectionHealthMonitor.probe(application, forceRecovery = true)
                handler.post {
                    if (!isFinishing && !isDestroyed) {
                        if (response.changedHost) remoteConnection.select("Chave salva")
                        openSettingsSection("connection")
                        showAppNotice("Verificação concluída", if (response.ok)
                            "Resposta autenticada: ${response.rttMs ?: 0} ms" +
                                (if (response.changedHost) ". Outro endereço salvo foi recuperado." else ".")
                            else "Sem resposta do aparelho. Confira a rede e o serviço Monitorado. " +
                                "A descoberta automática só testa endereços já pareados com a mesma chave.")
                    }
                }
            }
        }
    })
    sectionGap(layout, 8)
    layout.addView(actionButton("Limpar histórico deste aparelho") {
        showAppConfirm("Apagar histórico de conexão?",
            "Apaga somente os resultados e falhas registrados para o endereço selecionado, sem remover o pareamento.",
            R.drawable.ic_activity, "Limpar histórico") {
            ConnectionHealthStore.clear(this, host)
            openSettingsSection("connection")
        }
    })
}
