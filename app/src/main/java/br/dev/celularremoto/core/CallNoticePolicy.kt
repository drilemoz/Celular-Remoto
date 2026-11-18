package br.dev.celularremoto.core

/** Tipos públicos de CallLog.Calls, sem dependência de Android nos testes JVM. */
internal object CallNoticePolicy {
    data class Row(val id: Long, val dateMs: Long, val type: Int, val durationSeconds: Long = 0L)
    data class Progress(val highWater: Long, val fresh: List<Row>)

    fun kind(type: Int): String? = when (type) {
        1 -> "INCOMING"
        2 -> "OUTGOING"
        3 -> "MISSED"
        5 -> "REJECTED"
        6 -> "BLOCKED"
        7 -> "EXTERNAL"
        else -> null // Voicemail não é uma chamada listada por este recurso.
    }

    fun label(kind: String): String? = when (kind) {
        "INCOMING" -> "Chamada recebida"
        "OUTGOING" -> "Chamada efetuada"
        "MISSED" -> "Chamada perdida"
        "REJECTED" -> "Chamada recusada"
        "BLOCKED" -> "Chamada bloqueada"
        "EXTERNAL" -> "Chamada atendida em outro dispositivo"
        else -> null
    }

    fun advance(rows: List<Row>, highWater: Long, armedAfterMs: Long, nowMs: Long): Progress {
        val newest = rows.maxOfOrNull { it.id } ?: highWater
        val fresh = if (highWater < 0L) emptyList() else rows.asSequence()
            .filter { row ->
                val end = row.dateMs + row.durationSeconds.coerceIn(0L, 7 * 24 * 60 * 60L) * 1_000L
                row.id > highWater && row.dateMs > 0L && end >= armedAfterMs &&
                    kind(row.type) != null && RemoteEventNoticePolicy.recentArrival(end, nowMs)
            }
            .distinctBy { it.id }.sortedBy { it.id }.toList()
        return Progress(maxOf(0L, highWater, newest), fresh)
    }

    /** PRESENTATION_ALLOWED = 1. Não revelar números ocultos nem inferir de outras APIs. */
    fun visibleNumber(presentation: Int, number: String?): String? =
        number?.filter { it in "0123456789+*#().- " }?.trim()?.take(45)
            ?.takeIf { presentation == 1 && it.any { char -> char in '0'..'9' } }
}
