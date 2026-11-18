package br.dev.celularremoto.service

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog.Calls
import br.dev.celularremoto.core.CallLogSharing
import br.dev.celularremoto.core.CallNoticePolicy
import java.util.concurrent.Executors

/** Só observa durante o modo Monitorado e após o consentimento local. */
internal class CallLogObserver(private val context: Context, private val handler: Handler) {
    private val executor = Executors.newSingleThreadExecutor { work ->
        Thread(work, "CelularRemoto-call-observer").apply { isDaemon = true }
    }
    private var observer: ContentObserver? = null
    private var generation = 0
    private var armedAfter = 0L
    private var highWater = -1L
    private var inFlight = false
    private var pending = false
    private val read = Runnable { query() }

    fun sync() {
        if (!CallLogSharing.allowed(context)) {
            if (observer != null) stop()
            return
        }
        if (observer != null) return
        val candidate = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                if (observer !== this) return
                pending = true
                if (!inFlight) {
                    handler.removeCallbacks(read)
                    handler.post(read)
                }
            }
        }
        if (runCatching { context.contentResolver.registerContentObserver(Calls.CONTENT_URI, true, candidate) }.isFailure)
            return
        observer = candidate
        armedAfter = System.currentTimeMillis()
        pending = true
        query()
    }

    private fun query() {
        if (observer == null || !pending || inFlight) return
        pending = false
        inFlight = true
        val token = generation
        executor.execute {
            val rows = CallLogSharing.observeRows(context)
            handler.post {
                if (generation != token) return@post
                inFlight = false
                if (!CallLogSharing.allowed(context)) { stop(); return@post }
                if (rows != null) {
                    val seen = CallNoticePolicy.advance(rows, highWater, armedAfter, System.currentTimeMillis())
                    highWater = seen.highWater
                    CallLogSharing.addNotices(context, seen.fresh)
                }
                if (pending) query()
            }
        }
    }

    fun stop() {
        generation++
        handler.removeCallbacks(read)
        observer?.let { runCatching { context.contentResolver.unregisterContentObserver(it) } }
        observer = null
        highWater = -1L
        pending = false
        inFlight = false
        CallLogSharing.clear()
    }

    fun close() {
        stop()
        executor.shutdownNow()
    }
}
