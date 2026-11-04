package br.dev.celularremoto.service

/** Regra pura: um Job nunca reativa monitoramento recusado ou já ativo. */
object RecoveryDecision {
    fun shouldAttempt(monitorEnabled: Boolean, jobEnabled: Boolean, serviceActive: Boolean): Boolean =
        monitorEnabled && jobEnabled && !serviceActive
}
