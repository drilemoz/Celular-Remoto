package br.dev.celularremoto.updates

/** Separa quem dispara a consulta da política de download. */
internal enum class UpdateCheckTrigger(val automatic: Boolean) {
    FOREGROUND(true), SCHEDULED(true), LOCAL_MANUAL(false), REMOTE(false);

    fun canCheck(enabled: Boolean, lastElapsedMs: Long?, nowElapsedMs: Long): Boolean =
        !automatic || (enabled && (lastElapsedMs == null || nowElapsedMs - lastElapsedMs >= MIN_AUTO_GAP_MS))

    fun autoDownloads(enabled: Boolean): Boolean = enabled && this != REMOTE

    companion object {
        const val MIN_AUTO_GAP_MS = 60_000L
    }
}
