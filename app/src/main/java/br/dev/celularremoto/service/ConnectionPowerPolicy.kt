package br.dev.celularremoto.service

/** Regras puras para não confundir lacuna do agendador com queda de rede confirmada. */
internal object ConnectionPowerPolicy {
    const val WATCHDOG_INTERVAL_MS = 15_000L
    const val SCHEDULING_GAP_MS = 45_000L
    const val CPU_LEASE_MS = 10 * 60_000L
    const val TEST_MS = 30 * 60_000L

    fun validMode(mode: String) = mode in setOf("off", "30m", "monitor")
    fun cpuWanted(mode: String, testDeadlineMs: Long, nowMs: Long, monitorActive: Boolean): Boolean =
        monitorActive && (mode == "monitor" || (mode == "30m" && testDeadlineMs > nowMs))
    fun schedulingGap(previousElapsed: Long, currentElapsed: Long): Long? =
        (currentElapsed - previousElapsed).takeIf { previousElapsed > 0L && it > SCHEDULING_GAP_MS }
}
