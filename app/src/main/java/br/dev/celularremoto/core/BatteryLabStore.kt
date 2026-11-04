package br.dev.celularremoto.core

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Sessões privadas do aparelho que faz a leitura. Não depende do Pulso Bateria.
 * Coleta somente durante teste ativo, pelo serviço MonitorService já autorizado.
 * Arquivo atômico: falhas de escrita não devem destruir o histórico anterior.
 */
object BatteryLabStore {
    private const val FILE = "battery_labs_v1.json"
    private const val MAX_SESSIONS = 80
    private const val MAX_POINTS = 480
    private const val MAX_PUBLIC_HISTORY = 12
    private const val MAX_COMPLETED_POINTS = 60

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.applicationContext.filesDir, FILE))

    private fun load(context: Context): JSONArray = runCatching {
        JSONArray(String(file(context).readFully(), Charsets.UTF_8))
    }.getOrElse { JSONArray() }

    private fun save(context: Context, sessions: JSONArray) {
        val atomic = file(context)
        val stream = atomic.startWrite()
        try {
            stream.write(sessions.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun validDouble(reading: JSONObject, key: String): Double? =
        if (reading.isNull(key) || !reading.has(key)) null else
            reading.optDouble(key).takeIf { it.isFinite() }

    private fun mode(reading: JSONObject): String = when (reading.optInt("statusCode", -1)) {
        2 -> "CHARGING"
        3 -> "DISCHARGING"
        4 -> "IDLE"
        5 -> "FULL"
        else -> "UNKNOWN"
    }

    private fun sample(reading: JSONObject, wall: Long, elapsed: Long): JSONObject = JSONObject()
        .put("wall", wall).put("elapsed", elapsed)
        .put("mode", mode(reading))
        .put("plugged", reading.optInt("pluggedCode", -1))
        .put("percent", reading.optInt("levelPercent", -1))
        .put("ma", validDouble(reading, "currentMa") ?: JSONObject.NULL)
        .put("source", reading.optString("currentSource", "NONE"))
        .put("temp", validDouble(reading, "temperatureC") ?: JSONObject.NULL)
        .put("mv", reading.optInt("voltageMv", -1))

    private fun validPercent(value: Int): Any = if (value in 0..100) value else JSONObject.NULL
    private fun validTemp(value: Double?): Double? = value?.takeIf { it in -30.0..100.0 }

    /** Returns a bounded, UI-safe summary; sample history never leaves app storage wholesale. */
    private fun summary(session: JSONObject, includePoints: Boolean): JSONObject {
        val start = session.optLong("startedAt")
        val end = session.optLong("endedAt", 0L).takeIf { it > 0 } ?: session.optLong("lastWall", start)
        val total = (end - start).coerceAtLeast(0L)
        val charge = session.optLong("chargingMs", 0L)
        val idle = session.optLong("idleMs", 0L)
        val points = session.optJSONArray("points") ?: JSONArray()
        return JSONObject().put("id", session.optString("id"))
            .put("kind", session.optString("kind"))
            .put("sky", session.optString("sky"))
            .put("panelWatts", session.optInt("panelWatts", 0))
            .put("screen", session.optString("screen", "NAO_INFORMADA"))
            .put("startedAt", start).put("endedAt", session.optLong("endedAt", 0L))
            .put("targetMinutes", session.optInt("targetMinutes", 0))
            .put("stopReason", session.optString("stopReason", ""))
            .put("active", session.optLong("endedAt", 0L) == 0L)
            .put("durationMs", total)
            .put("chargingMs", charge).put("notChargingMs", idle)
            .put("unknownMs", BatteryLabMath.unknownMs(total, charge, idle))
            .put("meanChargeMa", BatteryLabMath.meanCurrent(session.optDouble("currentMaMs", 0.0),
                session.optLong("validCurrentMs", 0L)) ?: JSONObject.NULL)
            .put("validCurrentMs", session.optLong("validCurrentMs", 0L))
            .put("initialPercent", validPercent(session.optInt("initialPercent", -1)))
            .put("finalPercent", validPercent(session.optInt("finalPercent", -1)))
            .put("initialTemperatureC", validTemp(session.optDouble("initialTemp", Double.NaN)) ?: JSONObject.NULL)
            .put("finalTemperatureC", validTemp(session.optDouble("finalTemp", Double.NaN)) ?: JSONObject.NULL)
            .put("maxTemperatureC", validTemp(session.optDouble("maxTemp", Double.NaN)) ?: JSONObject.NULL)
            .put("meanTemperatureC", if (session.optInt("tempCount", 0) > 0)
                session.optDouble("tempSum") / session.optInt("tempCount") else JSONObject.NULL)
            .put("pointsCount", points.length())
            .apply {
                if (includePoints) {
                    val limited = JSONArray()
                    for (i in (points.length() - 48).coerceAtLeast(0) until points.length())
                        limited.put(points.getJSONObject(i))
                    put("points", limited)
                }
            }
    }

    /** Retém agregados completos, reduz os pontos após o término para limitar I/O e RAM. */
    private fun compactCompleted(session: JSONObject) {
        val points = session.optJSONArray("points") ?: return
        if (points.length() <= MAX_COMPLETED_POINTS) return
        val recent = JSONArray()
        for (i in points.length() - MAX_COMPLETED_POINTS until points.length()) recent.put(points.get(i))
        session.put("points", recent)
    }

    /** Reavalia descontinuidade do relógio monotônico (ex.: reboot). */
    private fun repairIfRebooted(sessions: JSONArray, nowWall: Long, nowElapsed: Long): Boolean {
        var changed = false
        for (i in 0 until sessions.length()) {
            val item = sessions.optJSONObject(i) ?: continue
            if (item.optLong("endedAt") != 0L) continue
            val oldWall = item.optLong("lastWall")
            val oldElapsed = item.optLong("lastElapsed")
            if (nowElapsed < oldElapsed || kotlin.math.abs((nowWall - oldWall) - (nowElapsed - oldElapsed)) > 60_000L) {
                item.put("endedAt", oldWall).put("stopReason", "Interrompido por reinício ou mudança de relógio")
                compactCompleted(item)
                changed = true
            }
        }
        return changed
    }

    @Synchronized fun begin(context: Context, kind: String, sky: String = "", panelWatts: Int = 0,
        targetMinutes: Int = 0, screen: String = "NAO_INFORMADA"): JSONObject {
        require(kind in setOf("SOLAR", "ELECTRIC", "DISCHARGE", "AUTO")) { "Tipo de teste inválido." }
        require(sky.isEmpty() || sky in setOf("forte", "parcial", "nublado", "chuva"))
        require(kind != "SOLAR" || sky in setOf("forte", "parcial", "nublado", "chuva"))
        require(panelWatts in 0..200 && (kind == "SOLAR" || panelWatts == 0))
        require(targetMinutes in 0..1440 && (kind == "AUTO" || targetMinutes > 0))
        require(screen in setOf("NAO_INFORMADA", "ACESA", "APAGADA"))
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val all = load(context)
        if (repairIfRebooted(all, wall, elapsed)) save(context, all)
        check((0 until all.length()).none { all.optJSONObject(it)?.optLong("endedAt") == 0L }) {
            "Já existe um teste em andamento neste celular."
        }
        val reading = BatteryReader.snapshot(context)
        val m = mode(reading)
        val plugged = reading.optInt("pluggedCode", -1)
        val percent = reading.optInt("levelPercent", -1)
        check(percent in 0..100) { "Percentual de bateria indisponível." }
        if (kind == "DISCHARGE") check(plugged == 0 && m == "DISCHARGING") {
            "Desconecte a fonte de energia e aguarde estado de descarga confirmado."
        }
        if (kind in setOf("ELECTRIC", "AUTO")) check(plugged > 0 && m in setOf("CHARGING", "FULL")) {
            "Conecte uma fonte e aguarde estado de carga confirmado."
        }
        check(!(kind != "DISCHARGE" && percent == 100)) { "Bateria já em 100%." }
        val points = JSONArray().put(sample(reading, wall, elapsed))
        val temp = validTemp(validDouble(reading, "temperatureC"))
        val session = JSONObject().put("id", UUID.randomUUID().toString())
            .put("kind", kind).put("sky", sky).put("panelWatts", panelWatts)
            .put("targetMinutes", targetMinutes).put("screen", screen)
            .put("startedAt", wall).put("startedElapsed", elapsed)
            .put("lastWall", wall).put("lastElapsed", elapsed)
            .put("endedAt", 0L).put("initialPercent", percent).put("finalPercent", percent)
            .put("chargingMs", 0L).put("idleMs", 0L)
            .put("currentMaMs", 0.0).put("validCurrentMs", 0L)
            .put("initialTemp", temp ?: JSONObject.NULL)
            .put("finalTemp", temp ?: JSONObject.NULL)
            .put("maxTemp", temp ?: JSONObject.NULL)
            .put("tempSum", temp ?: 0.0).put("tempCount", if (temp != null) 1 else 0)
            .put("disconnectedSince", 0L).put("fullConfirmations", 0).put("points", points)
        val result = JSONArray().put(session)
        for (i in 0 until minOf(all.length(), MAX_SESSIONS - 1)) result.put(all.get(i))
        save(context, result)
        return summary(session, true)
    }

    @Synchronized fun tick(context: Context) {
        val all = load(context)
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        if (repairIfRebooted(all, wall, elapsed)) { save(context, all); return }
        val item = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .firstOrNull { it.optLong("endedAt", 0L) == 0L } ?: return
        val previous = item.optJSONArray("points")?.let { arr -> arr.optJSONObject(arr.length() - 1) } ?: return
        val next = sample(BatteryReader.snapshot(context), wall, elapsed)
        val dt = elapsed - previous.optLong("elapsed")
        val dw = wall - previous.optLong("wall")
        val interval = BatteryLabMath.classify(previous.optString("mode"), next.optString("mode"),
            validDouble(previous, "ma"), validDouble(next, "ma"),
            previous.optString("source"), next.optString("source"), dt, dw)
        item.put("chargingMs", item.optLong("chargingMs") + interval.chargingMs)
        item.put("idleMs", item.optLong("idleMs") + interval.idleMs)
        item.put("currentMaMs", item.optDouble("currentMaMs") + interval.currentMaMs)
        item.put("validCurrentMs", item.optLong("validCurrentMs") + interval.validCurrentMs)
        val temp = validTemp(validDouble(next, "temp"))
        if (temp != null) {
            item.put("finalTemp", temp)
            item.put("maxTemp", maxOf(item.optDouble("maxTemp", temp).takeIf { it.isFinite() } ?: temp, temp))
            item.put("tempSum", item.optDouble("tempSum") + temp)
            item.put("tempCount", item.optInt("tempCount") + 1)
        }
        val percent = next.optInt("percent", -1)
        if (percent in 0..100) item.put("finalPercent", percent)
        item.put("lastWall", wall).put("lastElapsed", elapsed)
        val points = item.getJSONArray("points")
        points.put(next)
        if (points.length() > MAX_POINTS) {
            val reduced = JSONArray()
            for (i in points.length() - MAX_POINTS until points.length()) reduced.put(points.get(i))
            item.put("points", reduced)
        }
        val kind = item.optString("kind")
        // O firmware pode continuar informando CHARGING em 100%. Confirmar duas
        // leituras consecutivas para evitar fechar por arredondamento pontual.
        val confirmedFull = BatteryLabMath.fullConfirmations(item.optInt("fullConfirmations", 0),
            kind, percent, next.optInt("plugged", -1), next.optString("mode"))
        item.put("fullConfirmations", confirmedFull)
        val reason = when {
            kind == "DISCHARGE" && next.optInt("plugged", -1) > 0 -> "Fonte externa conectada: teste de consumo encerrado"
            kind != "DISCHARGE" && confirmedFull >= 2 -> "Meta de 100% confirmada em duas leituras"
            item.optInt("targetMinutes") > 0 && elapsed - item.optLong("startedElapsed") >=
                item.optInt("targetMinutes").toLong() * 60_000L -> "Prazo concluído"
            kind == "AUTO" -> {
                val unplugged = next.optInt("plugged", -1) == 0
                val seen = item.optLong("disconnectedSince", 0L)
                if (unplugged && dt in 1..BatteryLabMath.MAX_INTERVAL_MS &&
                    previous.optInt("plugged", -1) == 0) {
                    if (seen == 0L) item.put("disconnectedSince", previous.optLong("elapsed"))
                } else item.put("disconnectedSince", 0L)
                if (item.optLong("disconnectedSince", 0L) > 0 &&
                    elapsed - item.optLong("disconnectedSince") >= 180_000L) "Fonte desconectada por 3 minutos" else null
            }
            else -> null
        }
        if (reason != null) {
            item.put("endedAt", wall).put("stopReason", reason)
            compactCompleted(item)
        }
        save(context, all)
    }

    @Synchronized fun end(context: Context): JSONObject {
        // Observa uma leitura final real antes de encerrar, sem inventar intervalo.
        tick(context)
        val all = load(context)
        val item = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .firstOrNull { it.optLong("endedAt") == 0L }
            ?: throw IllegalStateException("O teste já foi concluído; atualize o histórico.")
        item.put("endedAt", System.currentTimeMillis()).put("stopReason", "Encerrado manualmente")
        compactCompleted(item)
        save(context, all)
        return summary(item, true)
    }

    @Synchronized fun report(context: Context): JSONObject {
        val all = load(context)
        val wall = System.currentTimeMillis()
        if (repairIfRebooted(all, wall, SystemClock.elapsedRealtime())) save(context, all)
        val active = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .firstOrNull { it.optLong("endedAt") == 0L }
        val history = JSONArray()
        for (i in 0 until minOf(all.length(), MAX_PUBLIC_HISTORY)) {
            val item = all.optJSONObject(i) ?: continue
            history.put(summary(item, item === active || i == 0))
        }
        return JSONObject().put("battery", BatteryReader.snapshot(context))
            .put("active", active?.let { summary(it, true) } ?: JSONObject.NULL)
            .put("history", history)
            .put("automatic", BatterySessionStore.report(context))
            .put("goalAlerts", BatteryGoalAlerts.snapshot(context))
            .put("reportedAt", wall)
    }
}
