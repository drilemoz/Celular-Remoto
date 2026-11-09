package br.dev.celularremoto.core

/** Validação da sessão visual de Mensagens sem armazenar chave ou conteúdo em disco. */
internal object RemoteMessageSessionPolicy {
    fun matches(sessionHost: String, sessionScope: String, selectedHost: String, currentScope: String): Boolean =
        sessionHost.isNotBlank() && sessionScope.isNotBlank() &&
            sessionHost == selectedHost && sessionScope == currentScope
}
