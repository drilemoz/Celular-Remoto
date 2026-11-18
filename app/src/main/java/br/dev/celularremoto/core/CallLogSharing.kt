package br.dev.celularremoto.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog.Calls
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque

/** Uma leitura sob demanda ou um pequeno buffer de avisos sem número, nome ou áudio. */
internal object CallLogSharing {
    private const val LIMIT = 30
    private data class Notice(val id: Long, val whenMs: Long, val kind: String)
    private val journal = ArrayDeque<Notice>()
    private var sequence = 0L

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun allowed(context: Context): Boolean =
        SettingsStore.isMonitorEnabled(context) && SettingsStore.shareCallHistory(context) && hasPermission(context)

    private fun <T> read(context: Context, projection: Array<String>, order: String, build: (Cursor) -> T): T? {
        if (!allowed(context)) return null
        val uri = Calls.CONTENT_URI.buildUpon().appendQueryParameter(Calls.LIMIT_PARAM_KEY, LIMIT.toString()).build()
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, order)?.use { cursor ->
                build(cursor).takeIf { allowed(context) }
            }
        }.getOrNull()
    }

    fun observeRows(context: Context): List<CallNoticePolicy.Row>? =
        read(context, arrayOf(Calls._ID, Calls.DATE, Calls.TYPE, Calls.DURATION), "${Calls._ID} DESC") { cursor ->
            buildList {
                var scanned = 0
                while (scanned++ < LIMIT && cursor.moveToNext()) {
                    add(CallNoticePolicy.Row(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), cursor.getLong(3)))
                }
            }
        }

    fun history(context: Context): JSONArray? =
        read(context, arrayOf(Calls.DATE, Calls.TYPE, Calls.DURATION, Calls.NUMBER, Calls.NUMBER_PRESENTATION),
            "${Calls.DATE} DESC, ${Calls._ID} DESC") { cursor ->
            JSONArray().apply {
                var scanned = 0
                while (scanned++ < LIMIT && cursor.moveToNext()) {
                    val kind = CallNoticePolicy.kind(cursor.getInt(1)) ?: continue
                    val at = cursor.getLong(0)
                    if (at <= 0L) continue
                    put(JSONObject().put("kind", kind).put("whenMs", at)
                        .put("durationSeconds", cursor.getLong(2).coerceAtLeast(0L))
                        .put("number", CallNoticePolicy.visibleNumber(cursor.getInt(4), cursor.getString(3)) ?: JSONObject.NULL))
                }
            }
        }

    @Synchronized fun addNotices(context: Context, rows: List<CallNoticePolicy.Row>) {
        if (rows.isEmpty() || !allowed(context)) return
        val at = System.currentTimeMillis()
        rows.forEach { row -> CallNoticePolicy.kind(row.type)?.let { kind ->
            journal.addLast(Notice(++sequence, at, kind))
        } }
        while (journal.size > LIMIT) journal.removeFirst()
        RemoteEventSignal.changed()
    }

    @Synchronized fun clear() {
        if (journal.isEmpty()) return
        journal.clear()
        RemoteEventSignal.changed()
    }

    /** Apenas sequência, horário de detecção e tipo, separados do registro completo. */
    @Synchronized fun alerts(context: Context): JSONArray = JSONArray().apply {
        if (allowed(context)) journal.forEach {
            put(JSONObject().put("id", it.id).put("whenMs", it.whenMs).put("kind", it.kind))
        }
    }
}
