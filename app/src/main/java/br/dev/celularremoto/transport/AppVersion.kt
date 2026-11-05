package br.dev.celularremoto.transport

/**
 * A versão real do APK não é um requisito do pareamento. WIRE_COMPAT é o marcador
 * de compatibilidade do protocolo AES-GCM introduzido na alpha.111; aparelhos
 * nessa revisão ainda exigem exatamente este marcador. Os campos extras são
 * ignorados pelo receptor antigo; novos recursos exigem negociação de suporte.
 *
 * Protocolo v1 (WireProtocol.VERSION) não deve ser alterado sem revisar operações.
 */
object AppVersion {
    const val NAME = "0.1.0-alpha.123"
    const val WIRE_COMPAT = "0.1.0-alpha.111"

    fun compatible(remote: String?): Boolean = remote == WIRE_COMPAT || remote == NAME
    fun incompatibility(remote: String?): String =
        "Protocolo remoto não suportado. Este aparelho aceita a base alpha.111 ou posterior " +
        "com protocolo compatível. Marcador recebido: ${remote?.takeIf { it.isNotBlank() } ?: "ausente"}. " +
        "Atualize o aparelho antigo; versões anteriores à alpha.111 podem recusar a conexão."
}
