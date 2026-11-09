package br.dev.celularremoto.core

import java.security.MessageDigest

/** Regras determinísticas para avisos genéricos; nenhuma mensagem é persistida. */
internal object RemoteEventNoticePolicy {
    enum class Category { SMS, WHATSAPP, OTHER }
    private const val MAX_AGE_MS = 60 * 60_000L
    private const val FUTURE_TOLERANCE_MS = 60_000L

    fun classify(kind: String, packageName: String): Category = when {
        kind == "sms" -> Category.SMS
        packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b" -> Category.WHATSAPP
        else -> Category.OTHER
    }

    fun enabled(category: Category, sms: Boolean, whatsapp: Boolean, other: Boolean): Boolean = when (category) {
        Category.SMS -> sms
        Category.WHATSAPP -> whatsapp
        Category.OTHER -> other
    }

    /** O instante precisa vir do recebimento no Android, e não de data livre do remetente SMS. */
    fun recentArrival(whenMs: Long, nowMs: Long): Boolean =
        whenMs > 0 && nowMs >= 0 && whenMs <= nowMs + FUTURE_TOLERANCE_MS &&
            (whenMs >= nowMs || nowMs - whenMs <= MAX_AGE_MS)

    /** Mostra atraso real, sem confundir uma consulta periódica com push imediato. */
    fun ageLabel(whenMs: Long, nowMs: Long): String = when {
        whenMs <= 0L || whenMs > nowMs -> "Toque para consultar no aparelho pareado"
        nowMs - whenMs < 60_000L -> "Recebida há menos de 1 minuto"
        else -> "Recebida há ${(nowMs - whenMs) / 60_000L} min · toque para consultar"
    }

    /** Isola o cursor de cada pareamento, inclusive ao reutilizar o mesmo IP com outra chave.
     * A preferência recebe somente um identificador irreversível, nunca a chave em texto.
     */
    fun pairingScope(host: String, key: ByteArray): String {
        require(host.isNotBlank() && key.isNotEmpty())
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(host.trim().lowercase(java.util.Locale.ROOT).toByteArray(Charsets.UTF_8))
        digest.update(0)
        digest.update(key)
        return digest.digest().take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
