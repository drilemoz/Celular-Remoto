package br.dev.celularremoto.core

/** Sinal local para consultas de eventos com espera limitada.
 * Não guarda mensagem, remetente ou chaves. A revisão só existe neste processo.
 */
object RemoteEventSignal {
    private val lock = Object()
    private var revision = 1L

    fun current(): Long = synchronized(lock) { revision }

    fun changed() = synchronized(lock) {
        revision = if (revision == Long.MAX_VALUE) 1L else revision + 1L
        lock.notifyAll()
    }

    /** Retorna no evento, no prazo ou após interrupção do worker.
     * Nunca aguarda mais de 12 s para não prender os workers da LAN.
     */
    fun await(since: Long, timeoutMs: Long): Long {
        require(timeoutMs in 0L..12_000L)
        synchronized(lock) {
            if (since != revision || timeoutMs == 0L) return revision
            val deadline = System.nanoTime() + timeoutMs * 1_000_000L
            while (since == revision) {
                val remain = deadline - System.nanoTime()
                if (remain <= 0) break
                try { java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(lock, remain) }
                catch (_: InterruptedException) { Thread.currentThread().interrupt(); break }
            }
            return revision
        }
    }
}
