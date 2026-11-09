package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.concurrent.thread

class RemoteEventSignalTest {
    @Test fun initialAndImmediateChange() {
        val initial = RemoteEventSignal.current()
        assertEquals(initial, RemoteEventSignal.await(initial, 0))
        RemoteEventSignal.changed()
        assertTrue(RemoteEventSignal.await(initial, 100) != initial)
    }

    @Test fun notificationWakesEarly() {
        val initial = RemoteEventSignal.current()
        val notifier = thread { Thread.sleep(60); RemoteEventSignal.changed() }
        val updated = RemoteEventSignal.await(initial, 1_000)
        notifier.join()
        assertTrue(updated != initial)
    }

    @Test fun timeoutDoesNotInventRevision() {
        val initial = RemoteEventSignal.current()
        assertEquals(initial, RemoteEventSignal.await(initial, 10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooLongWaitIsRejected() { RemoteEventSignal.await(RemoteEventSignal.current(), 12_001) }
}
