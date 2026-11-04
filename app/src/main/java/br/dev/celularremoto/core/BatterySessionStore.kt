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
    private const val PUBLIC_SESSIONS = 24
    private const val PUBLIC_GRAPH_SESSIONS = 3
    private const val PUBLIC_GRAPH_POINTS = 40

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
            status == 3 -> "DISCHARGE" // Pode haver descarga líquida mesmo com fonte conectada.
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
            .put("coveragePercent", if (last > first) (100.0 * observedMs / (last - first)).coerceIn(0.0, 100.0) else 0.0)
            .put("powerSource", item.optString("powerSource", "").takeUnless { it == "null" } ?: "")
            .put("validCurrentMs", item.optLong("validCurrentMs", 0L))
            .put("meanCurrentMa", item.optLong("validCurrentMs", 0L).takeIf { it > 0 }?.let {
                item.optDouble("currentMaMs", 0.0) / it
            }?.takeIf { it.isFinite() } ?: JSONObject.NULL)
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
                for (i in (points.length() - PUBLIC_GRAPH_POINTS).coerceAtLeast(0) until points.length())
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
            .put("observedMs", 0L).put("currentMaMs", 0.0).put("validCurrentMs", 0L)
            .put("powerSource", raw.optString("powerSource", "").takeUnless { it == "null" } ?: "")
            .put("points", JSONArray().put(sample(raw, now, elapsed)))
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
        val currentSource = raw.optString("powerSource", "").takeUnless { it == "null" } ?: ""
        val sourceChanged = current != null && previousMode == "CHARGE" &&
            current.optString("powerSource", "") != currentSource
        val gap = if (current != null) elapsed - current.optLong("lastElapsed") else 0L
        val wallGap = if (current != null) now - current.optLong("lastWall") else 0L
        val reset = current != null && (gap <= 0L || gap > BatterySessionMath.MAX_GAP_MS ||
            kotlin.math.abs(gap - wallGap) > 10_000L)
        var changed = false
        if (current != null && (reset || mode != previousMode || (mode == "UNKNOWN") || sourceChanged)) {
            close(current, if (reset) "Coleta interrompida ou relógio alterado" else
                if (previousMode == "CHARGE" && percent == 100) "Bateria atingiu 100%" else
                if (sourceChanged) "Tipo de fonte de alimentação mudou" else
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
        val previousMa = lastPoint?.let { doubleOrNull(it, "currentMa") }?.takeIf { it in -30_000.0..30_000.0 }
        val latestMa = doubleOrNull(raw, "currentMa")?.takeIf { it in -30_000.0..30_000.0 }
        if (interval > 0L && previousMa != null && latestMa != null) {
            current.put("currentMaMs", current.optDouble("currentMaMs", 0.0) +
                (previousMa + latestMa) / 2.0 * interval)
            current.put("validCurrentMs", current.optLong("validCurrentMs", 0L) + interval)
        }
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
            history.put(summary(item, item === active || i < PUBLIC_GRAPH_SESSIONS))
        }
        val now = System.currentTimeMillis()
        val forecast = active?.let { session ->
            val result = summary(session, false)
            val points = session.optJSONArray("points") ?: JSONArray()
            val observedPoints = (0 until points.length()).mapNotNull { points.optJSONObject(it) }
                .mapNotNull { point ->
                    point.optInt("percent", -1).takeIf { it in 0..100 }?.let { percent ->
                        BatteryForecastMath.Point(point.optLong("elapsed", -1L), point.optLong("wall", -1L), percent)
                    }
                }
            val completed = (0 until all.length()).mapNotNull { all.optJSONObject(it) }
                .filter { it.optLong("endedAt", 0L) > 0L }
                .map { item ->
                    BatteryForecastMath.Session(item.optString("kind"),
                        item.optString("powerSource", "").takeUnless { it.isBlank() || it == "null" },
                        item.optLong("startedAt"), item.optLong("endedAt"), item.optLong("observedMs"),
                        item.optInt("initialPercent", -1), item.optInt("finalPercent", -1),
                        item.optJSONArray("points")?.length() ?: 0)
                }
            val estimate = BatteryForecastMath.predict(session.optString("kind"),
                session.optInt("finalPercent", -1), session.optLong("lastWall"), now,
                observedPoints, session.optString("powerSource", "").takeUnless { it.isBlank() || it == "null" },
                completed)
            // Não mostrar a estimativa legada quando há lacunas ou dados insuficientes.
            result.put("forecastMinutes", estimate?.minutes ?: JSONObject.NULL)
            result.put("forecast80Minutes", estimate?.to80 ?: JSONObject.NULL)
            result.put("forecast90Minutes", estimate?.to90 ?: JSONObject.NULL)
            result.put("forecastRatePercentHour", estimate?.percentPerHour ?: JSONObject.NULL)
            result.put("forecastBasis", estimate?.basis ?: "")
            result.put("forecastEvidence", estimate?.evidence ?: "INSUFICIENTE")
            result.put("forecastHistoricalSessions", estimate?.historicalSessions ?: 0)
            result
        }
        return JSONObject().put("active", forecast ?: JSONObject.NULL)
            .put("history", history).put("reportedAt", now)
    }
}
