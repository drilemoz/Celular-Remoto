package br.dev.celularremoto.core

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Histórico automático de sessões, independente dos quatro experimentos Labs.
 * Somente o serviço Monitorado grava; sem wake lock, sem captura adicional nem internet.
 */
object BatterySessionStore {
    private const val FILE = "battery_sessions_v1.json"
    private const val MAX_SESSIONS = 120
    private const val MAX_POINTS = 240
    private const val PUBLIC_SESSIONS = 12

    private fun atomic(context: Context) = AtomicFile(File(context.applicationContext.filesDir, FILE))
    private fun read(context: Context): JSONArray = runCatching {
        JSONArray(String(atomic(context).readFully(), Charsets.UTF_8))
    }.getOrElse { JSONArray() }
    private fun write(context: Context, data: JSONArray) {
        val file = atomic(context)
        val out = file.startWrite()
        try {
            out.write(data.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (exception: Exception) {
            file.failWrite(out)
            throw exception
        }
    }
    private fun intOrNull(item: JSONObject, key: String): Int? =
        item.optInt(key, -1).takeIf { it in 0..100 }
    private fun doubleOrNull(item: JSONObject, key: String): Double? =
        if (!item.has(key) || item.isNull(key)) null else item.optDouble(key).takeIf { it.isFinite() }

    private fun batteryMode(raw: JSONObject): String {
        val status = raw.optInt("statusCode", -1)
        val plugged = raw.optInt("pluggedCode", -1)
        return when {
            plugged > 0 && status == 2 -> "CHARGE"
            plugged == 0 && status == 3 -> "DISCHARGE"
            else -> "UNKNOWN"
        }
    }

    private fun sample(raw: JSONObject, now: Long, elapsed: Long): JSONObject = JSONObject()
        .put("wall", now).put("elapsed", elapsed)
        .put("percent", intOrNull(raw, "levelPercent") ?: JSONObject.NULL)
        .put("currentMa", doubleOrNull(raw, "currentMa") ?: JSONObject.NULL)
        .put("source", raw.optString("currentSource", "NONE"))
        .put("temperatureC", doubleOrNull(raw, "temperatureC") ?: JSONObject.NULL)
        .put("voltageMv", raw.optInt("voltageMv", -1))

    private fun summary(item: JSONObject, pointsAllowed: Boolean): JSONObject {
        val first = item.optLong("startedAt")
        val last = item.optLong("endedAt", 0L).takeIf { it > 0 } ?: item.optLong("lastWall", first)
        val startLevel = item.optInt("initialPercent", -1)
        val finalLevel = item.optInt("finalPercent", -1)
        val type = item.optString("kind")
        val observedMs = item.optLong("observedMs", 0L)
        val forecast = if (item.optLong("endedAt", 0L) == 0L)
            BatterySessionMath.forecastMinutes(type, startLevel, finalLevel, observedMs) else null
        val points = item.optJSONArray("points") ?: JSONArray()
        return JSONObject().put("id", item.optString("id"))
            .put("kind", type).put("startedAt", first).put("endedAt", item.optLong("endedAt", 0L))
            .put("lastWall", item.optLong("lastWall", 0L))
            .put("durationMs", (last - first).coerceAtLeast(0L))
            .put("observedMs", observedMs)
            .put("initialPercent", startLevel.takeIf { it in 0..100 } ?: JSONObject.NULL)
            .put("finalPercent", finalLevel.takeIf { it in 0..100 } ?: JSONObject.NULL)
            .put("maxTemperatureC", doubleOrNull(item, "maxTemp") ?: JSONObject.NULL)
            .put("finalTemperatureC", doubleOrNull(item, "finalTemp") ?: JSONObject.NULL)
            .put("forecastMinutes", forecast ?: JSONObject.NULL)
            .put("stopReason", item.optString("stopReason", ""))
            .put("active", item.optLong("endedAt", 0L) == 0L)
            .put("pointsCount", points.length())
            .apply { if (pointsAllowed) {
                val view = JSONArray()
                for (i in (points.length() - 48).coerceAtLeast(0) until points.length())
                    view.put(points.getJSONObject(i))
                put("points", view)
            } }
    }

    private fun close(item: JSONObject, reason: String) {
        if (item.optLong("endedAt", 0L) != 0L) return
        item.put("endedAt", item.optLong("lastWall"))
            .put("stopReason", reason)
        // Preserva o primeiro ponto e os últimos 79, sem reescrever o histórico completo.
        val points = item.optJSONArray("points") ?: return
        if (points.length() <= 80) return
        val reduced = JSONArray().put(points.get(0))
        for (i in points.length() - 79 until points.length()) reduced.put(points.get(i))
        item.put("points", reduced)
    }

    private fun newSession(raw: JSONObject, type: String, now: Long, elapsed: Long): JSONObject {
        val pct = raw.optInt("levelPercent", -1)
        val temp = doubleOrNull(raw, "temperatureC")
        return JSONObject().put("id", UUID.randomUUID().toString())
            .put("kind", type).put("startedAt", now)
            .put("lastWall", now).put("lastElapsed", elapsed).put("endedAt", 0L)
            .put("initialPercent", pct).put("finalPercent", pct)
            .put("maxTemp", temp ?: JSONObject.NULL).put("finalTemp", temp ?: JSONObject.NULL)
            .put("observedMs", 0L).put("points", JSONArray().put(sample(raw, now, elapsed)))
    }

    @Synchronized fun tick(context: Context) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val raw = BatteryReader.snapshot(context)
        val mode = batteryMode(raw)
        val percent = intOrNull(raw, "levelPercent")
        if (percent == null) return
        val all = read(context)
        val current = all.optJSONObject(0)?.takeIf { it.optLong("endedAt", 0L) == 0L }
        val previousMode = current?.optString("kind")
        val gap = if (current != null) elapsed - current.optLong("lastElapsed") else 0L
        val wallGap = if (current != null) now - current.optLong("lastWall") else 0L
        val reset = current != null && (gap <= 0L || gap > BatterySessionMath.MAX_GAP_MS ||
            kotlin.math.abs(gap - wallGap) > 10_000L)
        var changed = false
        if (current != null && (reset || mode != previousMode || (mode == "UNKNOWN"))) {
            close(current, if (reset) "Coleta interrompida ou relógio alterado" else
                if (previousMode == "CHARGE" && percent == 100) "Bateria atingiu 100%" else
                if (mode == "UNKNOWN") "Estado da bateria mudou ou ficou indisponível" else "Mudança entre carga e descarga")
            changed = true
        }
        val canBegin = (mode == "CHARGE" && percent < 100) ||
            (mode == "DISCHARGE" && percent > 0)
        if (current == null || current.optLong("endedAt", 0L) != 0L) {
            if (canBegin) {
                val updated = JSONArray().put(newSession(raw, mode, now, elapsed))
                for (i in 0 until minOf(all.length(), MAX_SESSIONS - 1)) updated.put(all.get(i))
                write(context, updated)
            } else if (changed) write(context, all)
            return
        }
        val lastPoint = current.optJSONArray("points")?.let { it.optJSONObject(it.length() - 1) }
        val lastSampleElapsed = lastPoint?.optLong("elapsed", 0L) ?: 0L
        val shouldStore = percent != current.optInt("finalPercent") || elapsed - lastSampleElapsed >= 60_000L
        if (!shouldStore) return
        val interval = BatterySessionMath.validInterval(mode, previousMode ?: "UNKNOWN", mode, gap, wallGap)
        current.put("observedMs", current.optLong("observedMs") + interval)
        current.put("finalPercent", percent)
        current.put("lastWall", now).put("lastElapsed", elapsed)
        val temp = doubleOrNull(raw, "temperatureC")
        if (temp != null && temp in -30.0..100.0) {
            current.put("finalTemp", temp)
            val maxT = doubleOrNull(current, "maxTemp")
            current.put("maxTemp", maxOf(maxT ?: temp, temp))
        }
        val points = current.getJSONArray("points")
        points.put(sample(raw, now, elapsed))
        if (points.length() > MAX_POINTS) {
            val recent = JSONArray()
            for (i in points.length() - MAX_POINTS until points.length()) recent.put(points.get(i))
            current.put("points", recent)
        }
        if (mode == "CHARGE" && percent == 100) close(current, "Bateria atingiu 100%")
        if (mode == "DISCHARGE" && percent == 0) close(current, "Bateria atingiu 0%")
        write(context, all)
    }

    /** Relatório somente leitura: não executa novas medições nem inicia coleta na UI. */
    @Synchronized fun report(context: Context): JSONObject {
        val all = read(context)
        val active = all.optJSONObject(0)?.takeIf { it.optLong("endedAt", 0L) == 0L }
        val history = JSONArray()
        for (i in 0 until minOf(all.length(), PUBLIC_SESSIONS)) {
            val item = all.optJSONObject(i) ?: continue
            history.put(summary(item, item === active || i == 0))
        }
        val forecast = active?.let { summary(it, false) }
        return JSONObject().put("active", forecast ?: JSONObject.NULL)
            .put("history", history).put("reportedAt", System.currentTimeMillis())
    }
}
