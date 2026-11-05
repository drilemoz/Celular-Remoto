package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class ConnectionHealthMathTest {
    private fun s(ms: Long, ok: Boolean, latency: Long? = null) = ConnectionHealthMath.Sample(ms, ok, latency)

    @Test fun noSamplesDoesNotMeanFullUptime() {
        val stats = ConnectionHealthMath.summarize(emptyList())
        assertNull(stats.availabilityPercent)
        assertNull(stats.meanRttMs)
    }
    @Test fun countsOnlyActualProbes() {
        val stats = ConnectionHealthMath.summarize(listOf(s(1000, true, 60), s(16_000, false), s(31_000, true, 40)))
        assertEquals(3, stats.attempts)
        assertEquals(2, stats.succeeded)
        assertEquals(1, stats.failed)
        assertEquals(66.666, requireNotNull(stats.availabilityPercent), 0.01)
        assertEquals(50L, requireNotNull(stats.meanRttMs))
    }
    @Test fun gapsAreUnknownNotFailures() {
        val stats = ConnectionHealthMath.summarize(listOf(s(1000, true), s(200_000, true)))
        assertEquals(2, stats.succeeded)
        assertEquals(0, stats.failed)
        assertEquals(1, stats.missedWindows)
        assertEquals(100.0, requireNotNull(stats.availabilityPercent), 0.01)
    }
    @Test fun failureWindowsStopAtGaps() {
        val stats = ConnectionHealthMath.summarize(listOf(
            s(1000, false), s(16_000, false), s(220_000, true)))
        assertEquals(0L, stats.longestObservedFailureMs)
    }
    @Test fun consecutiveFailuresTriggerOnlyAfterThree() {
        val stats = ConnectionHealthMath.summarize(listOf(s(1000, true), s(2000, false), s(3000, false), s(4000, false)))
        assertEquals(3, stats.failuresInSequence)
        assertEquals(3, ConnectionHealthMath.ALERT_AFTER_FAILURES)
    }
    @Test fun aRecoveryClosesFailureWindow() {
        val stats = ConnectionHealthMath.summarize(listOf(s(1000, false), s(30_000, false), s(55_000, true, 30)))
        assertEquals(54_000L, stats.longestObservedFailureMs)
        assertEquals(0, stats.failuresInSequence)
    }
    @Test fun adaptivePollingAvoidsFastRepeatedFailures() {
        assertEquals(30_000L, ConnectionHealthMath.probeDelayMs(0))
        assertEquals(60_000L, ConnectionHealthMath.probeDelayMs(3))
        assertEquals(300_000L, ConnectionHealthMath.probeDelayMs(10))
    }
    @Test fun oldResponseIsNotStillConnected() {
        assertEquals("Resposta recente", ConnectionHealthMath.connectionLabel(1000, 45_000))
        assertEquals("Aguardando nova verificação", ConnectionHealthMath.connectionLabel(1000, 50_000))
    }
}
