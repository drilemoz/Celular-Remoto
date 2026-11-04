package br.dev.celularremoto.transport

/**
 * Janela limitada aos acessos autenticados realmente recentes.
 * O mesmo cliente não gera outro alerta a cada snapshot; após expirar e retornar,
 * ele passa a ser uma sessão nova. Não armazena chaves de pareamento.
 */
internal class AccessAlertWindow {
    private val seen = mutableSetOf<String>()

    /** Retorna true somente se alguma sessão nova foi observada. */
    fun observe(activeSessions: Collection<String>): Boolean {
        val active = activeSessions.toSet()
        seen.retainAll(active)
        var newlySeen = false
        for (session in active) {
            if (seen.add(session)) newlySeen = true
        }
        return newlySeen
    }

    fun clear() { seen.clear() }
    internal fun retainedCount(): Int = seen.size
}
