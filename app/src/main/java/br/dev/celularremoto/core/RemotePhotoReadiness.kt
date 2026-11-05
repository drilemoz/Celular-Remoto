package br.dev.celularremoto.core

/** Decisão pura de interface; estado recebido somente pelo snapshot autenticado. */
object RemotePhotoReadiness {
    enum class State { READY, SETUP, SESSION, UNKNOWN }
    fun state(value: String?): State = when (value) {
        "ready" -> State.READY
        "setup" -> State.SETUP
        "session", "off" -> State.SESSION
        else -> State.UNKNOWN // Versões antigas não fornecem o diagnóstico.
    }
    fun headline(value: State): String = when (value) {
        State.READY -> "Câmera pronta no monitorado"
        State.SETUP -> "Câmera exige preparação local"
        State.SESSION -> "Sessão fotográfica não está armada"
        State.UNKNOWN -> "Estado da câmera não confirmado"
    }
    fun canTryPhoto(value: State): Boolean = value == State.READY || value == State.UNKNOWN
}
