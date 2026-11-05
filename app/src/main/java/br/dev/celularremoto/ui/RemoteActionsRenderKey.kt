package br.dev.celularremoto.ui

/** Identidade do painel de atalhos, sem incluir telemetria nem material secreto. */
internal data class RemoteActionsRenderKey(
    val host: String,
    val keyAvailable: Boolean,
    val eventCount: Int,
    val theme: String,
    val fontScale: Float,
    val widthDp: Int
)
