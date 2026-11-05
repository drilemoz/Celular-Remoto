package br.dev.celularremoto.core

/** Regras puras para não confirmar eventos cuja notificação foi bloqueada. */
object AlertDeliveryPolicy {
    enum class Result { POSTED, PERMISSION_DENIED, APP_BLOCKED, CHANNEL_BLOCKED, SERVICE_UNAVAILABLE, ERROR, SUPPRESSED }

    fun acknowledged(result: Result): Boolean = result == Result.POSTED || result == Result.SUPPRESSED

    /** Não reenviar uma meta local cuja condição já deixou de existir. */
    fun goalStillRelevant(kind: String, percent: Int, temperature: Double?,
        goal: Int, low: Int, hot: Int): Boolean = when (kind) {
        "GOAL" -> percent >= goal
        "LOW" -> percent <= low
        "HOT" -> temperature != null && temperature.isFinite() && temperature >= hot
        else -> false
    }

    /** Eventos antigos não devem reaparecer depois de dias sem rede/permissão. */
    fun retryAllowed(eventTime: Long, now: Long, acknowledged: Boolean): Boolean =
        !acknowledged && eventTime > 0 && eventTime <= now + 60_000L && now - eventTime <= 24L * 60 * 60_000L
}
