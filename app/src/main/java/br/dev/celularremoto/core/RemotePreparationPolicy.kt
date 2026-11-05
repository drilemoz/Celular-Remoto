package br.dev.celularremoto.core

/** Estados informativos; nenhum estado implica autorização remota por si só. */
object RemotePreparationPolicy {
    const val READY = "ready"
    const val SETUP = "setup"
    const val SESSION = "session"
    const val OFF = "off"

    data class Item(val id: String, val title: String, val state: String, val note: String)

    fun valid(items: List<Item>): Boolean = items.size <= 24 && items.map { it.id }.distinct().size == items.size &&
        items.all { it.id.matches(Regex("[a-z_]{2,32}")) && it.state in setOf(READY, SETUP, SESSION, OFF) &&
            it.title.length <= 64 && it.note.length <= 160 }

    fun readyCount(items: List<Item>): Int = items.count { it.state == READY }
    fun pendingCount(items: List<Item>): Int = items.count { it.state == SETUP }
    fun status(state: String): String = when (state) {
        READY -> "Pronto"
        SETUP -> "Configurar"
        SESSION -> "Por sessão"
        OFF -> "Desativado"
        else -> "Desconhecido"
    }
}
