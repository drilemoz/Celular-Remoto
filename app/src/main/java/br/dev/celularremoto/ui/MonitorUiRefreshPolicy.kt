package br.dev.celularremoto.ui

/** Valores baratos de ler. Ignore timestamps de acesso: o servidor já notifica novos clientes
 * e o relógio de 5 segundos retira os clientes quando o prazo de 45 segundos vence.
 * Não guardar chaves, endereços secretos nem conteúdo de tela nesta assinatura visual.
 */
internal object MonitorUiRefreshPolicy {
    data class State(
        val enabled: Boolean,
        val serviceRunning: Boolean,
        val serviceActive: Boolean,
        val screenSharing: Boolean,
        val cameraActive: Boolean,
        val photoPrepared: Boolean,
        val address: String,
        val recentClients: List<String>
    )

    fun needsRender(previous: State?, current: State): Boolean = previous != current
}
