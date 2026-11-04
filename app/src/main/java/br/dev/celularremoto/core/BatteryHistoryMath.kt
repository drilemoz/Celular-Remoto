package br.dev.celularremoto.core

import kotlin.math.abs

/** Regras conservadoras para o histórico. Não infere produção do painel nem preenche lacunas. */
internal object BatteryHistoryMath {
    private const val MIN_DURATION_MS = 10 * 60_000L

    data class Session(
        val kind: String,
        val startedAt: Long,
        val endedAt: Long,
        val durationMs: Long,
        val observedMs: Long,
        val initialPercent: Int?,
        val finalPercent: Int?,
        val sky: String = "",
        val panelWatts: Int = 0,
        val screen: String = "NAO_INFORMADA"
    )

    data class Comparison(val firstRate: Double?, val secondRate: Double?,
        val rateDifference: Double?, val firstCoverage: Double, val secondCoverage: Double,
        val warnings: List<String>, val meaningful: Boolean)

    fun coverage(observedMs: Long, durationMs: Long): Double =
        if (durationMs <= 0L) 0.0 else (observedMs.coerceAtLeast(0L).toDouble() / durationMs).coerceIn(0.0, 1.0)

    /** p.p./h do tempo decorrido, não uma potência elétrica nem medição contínua. */
    fun rate(session: Session): Double? {
        if (session.durationMs < MIN_DURATION_MS || session.endedAt <= 0L ||
            session.startedAt <= 0L || session.endedAt < session.startedAt ||
            coverage(session.observedMs, session.durationMs) < 0.70) return null
        val start = session.initialPercent?.takeIf { it in 0..100 } ?: return null
        val finish = session.finalPercent?.takeIf { it in 0..100 } ?: return null
        if (abs(finish - start) < 2) return null // resolução de percentual insuficiente
        return (finish - start) * 3_600_000.0 / session.durationMs
    }

    fun compare(a: Session, b: Session): Comparison {
        val warnings = mutableListOf<String>()
        if (a.endedAt <= 0L || b.endedAt <= 0L) warnings += "Não comparar testes em andamento."
        if (a.kind != b.kind) warnings += "Tipos de teste diferentes; taxas não são equivalentes."
        val aCoverage = coverage(a.observedMs, a.durationMs)
        val bCoverage = coverage(b.observedMs, b.durationMs)
        if (aCoverage < 0.70 || bCoverage < 0.70)
            warnings += "Cobertura insuficiente em pelo menos um teste (<70%)."
        if (a.kind == "SOLAR" && b.kind == "SOLAR") {
            if (a.sky != b.sky) warnings += "Condição do céu informada diferente."
            if (a.panelWatts != b.panelWatts || a.panelWatts == 0)
                warnings += "Painéis nominais diferentes ou não informados."
        }
        if (a.screen != b.screen || a.screen == "NAO_INFORMADA")
            warnings += "Condição da tela diferente ou não informada."
        val r1 = rate(a)
        val r2 = rate(b)
        if (r1 == null || r2 == null)
            warnings += "Não há variação/tempo válidos suficientes para comparar taxas."
        val meaningful = warnings.isEmpty() && r1 != null && r2 != null
        return Comparison(r1, r2, if (meaningful) r2!! - r1!! else null,
            aCoverage, bCoverage, warnings.distinct(), meaningful)
    }
}
