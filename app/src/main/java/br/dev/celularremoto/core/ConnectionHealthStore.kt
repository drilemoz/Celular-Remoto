package br.dev.celularremoto.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Histórico local e limitado por aparelho. Não armazena chaves, conteúdo remoto nem IP em notificações. */
object ConnectionHealthStore {
    private const val PREFS = "connection_health_v1"
    private const val MAX_SAMPLES = 2048
    private const val RETAIN_MS = 7L * 24 * 60 * 60_000L

    data class Status(val samples: List<ConnectionHealthMath.Sample>, val summary: ConnectionHealthMath.Summary,
        val lastAttemptMs: Long, val lastSuccessMs: Long, val failureStreak: Int,
        val lastReason: String, val lastRttMs: Long?, val alerted: Boolean)

    private fun preferences(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    // Armazenamento separado para cada endereço. Não usar nomes fornecidos pela rede como chave de preferência.
    private fun id(host: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(host.trim().lowercase(java.util.Locale.ROOT).toByteArray()).take(12)
        .joinToString("") { "%02x".format(it) }

    fun enabled(ctx: Context): Boolean = preferences(ctx).getBoolean("enabled", true)
    fun setEnabled(ctx: Context, enabled: Boolean) { preferences(ctx).edit().putBoolean("enabled", enabled).apply() }
    fun backgroundEnabled(ctx: Context): Boolean = preferences(ctx).getBoolean("background", false)
    fun setBackgroundEnabled(ctx: Context, enabled: Boolean) { preferences(ctx).edit().putBoolean("background", enabled).apply() }
    fun alertsEnabled(ctx: Context): Boolean = preferences(ctx).getBoolean("alerts", true)
    fun setAlertsEnabled(ctx: Context, enabled: Boolean) { preferences(ctx).edit().putBoolean("alerts", enabled).apply() }

    private fun loadObject(ctx: Context, host: String): JSONObject = runCatching {
        JSONObject(preferences(ctx).getString("peer_" + id(host), "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun samples(data: JSONObject): List<ConnectionHealthMath.Sample> {
        val array = data.optJSONArray("samples") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONArray(i) ?: return@mapNotNull null
            val time = item.optLong(0)
            if (time <= 0) null else ConnectionHealthMath.Sample(time, item.optInt(1) == 1,
                item.optLong(2, -1).takeIf { it >= 0 })
        }
    }

    @Synchronized fun status(ctx: Context, host: String): Status {
        if (host.isBlank()) return Status(emptyList(), ConnectionHealthMath.summarize(emptyList()), 0, 0, 0, "", null, false)
        val raw = loadObject(ctx, host)
        val current = samples(raw).filter { it.atMs > System.currentTimeMillis() - RETAIN_MS }
        return Status(current, ConnectionHealthMath.summarize(current),
            raw.optLong("lastAttempt"), raw.optLong("lastSuccess"), raw.optInt("streak"),
            raw.optString("reason"), raw.optLong("rtt", -1L).takeIf { it >= 0L }, raw.optBoolean("alerted"))
    }

    /** Retorna 'lost' / 'restored' apenas em transições confirmadas (>=3 falhas). */
    @Synchronized fun record(ctx: Context, host: String, succeeded: Boolean, rttMs: Long?,
        reason: String = "", atMs: Long = System.currentTimeMillis()): String? {
        if (host.isBlank() || atMs <= 0L || !enabled(ctx)) return null
        val raw = loadObject(ctx, host)
        // Evitar contabilizar a mesma sondagem concluída duas vezes por duas telas.
        val previous = raw.optLong("lastAttempt", 0L)
        if (previous > atMs) return null
        var streak = raw.optInt("streak", 0)
        val hadAlert = raw.optBoolean("alerted", false)
        streak = if (succeeded) 0 else (streak + 1).coerceAtMost(10_000)
        val alert = when {
            succeeded && hadAlert -> "restored"
            !succeeded && !hadAlert && streak >= ConnectionHealthMath.ALERT_AFTER_FAILURES -> "lost"
            else -> null
        }
        val oldSamples = samples(raw).filter { it.atMs >= atMs - RETAIN_MS }.takeLast(MAX_SAMPLES - 1)
        val updated = JSONArray()
        oldSamples.forEach { s -> updated.put(JSONArray().put(s.atMs).put(if (s.succeeded) 1 else 0).put(s.rttMs ?: -1)) }
        updated.put(JSONArray().put(atMs).put(if (succeeded) 1 else 0).put(rttMs ?: -1))
        raw.put("samples", updated).put("lastAttempt", atMs)
            .put("streak", streak).put("rtt", if (succeeded) rttMs ?: -1 else raw.optLong("rtt", -1))
            .put("reason", if (succeeded) "" else reason.take(100))
            .put("alerted", if (succeeded) false else hadAlert || alert == "lost")
        if (succeeded) raw.put("lastSuccess", atMs)
        preferences(ctx).edit().putString("peer_" + id(host), raw.toString()).apply()
        return alert
    }

    @Synchronized fun clear(ctx: Context, host: String) {
        if (host.isNotBlank()) preferences(ctx).edit().remove("peer_" + id(host)).apply()
    }
}
