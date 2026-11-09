package br.dev.celularremoto.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque

/** SMS recebido em tempo real: somente na RAM e depois de autorização local. */
object SmsLiveJournal {
    private data class Entry(val id: Long, val address: String, val text: String, val whenMs: Long, val arrivalMs: Long)
    private var sequence = 0L
    private val entries = ArrayDeque<Entry>()
    @Synchronized fun add(address: String, body: String, whenMs: Long) {
        if (SensitiveContent.isSensitive(address, body) || MessagePolicy.looksLikeAuthenticationSms(body)) return
        if (entries.any { it.address == address && it.text == body && kotlin.math.abs(it.whenMs - whenMs) < 30_000L }) return
        entries.addLast(Entry(++sequence, address.take(40), body.take(400), whenMs, System.currentTimeMillis()))
        while (entries.size > 20) entries.removeFirst()
    }
    @Synchronized fun clear() = entries.clear()
    /** Metadados mínimos para aviso de chegada. Nunca envia remetente nem texto nesta operação. */
    @Synchronized fun alerts(): JSONArray = JSONArray().apply {
        entries.forEach { put(JSONObject().put("id", it.id).put("whenMs", it.arrivalMs)) }
    }
    @Synchronized fun snapshot(): JSONArray = JSONArray().apply {
        entries.forEach { put(JSONObject().put("address", it.address).put("text", it.text)
            .put("whenMs", it.whenMs).put("direction", "recebido").put("live", true)) }
    }
}
