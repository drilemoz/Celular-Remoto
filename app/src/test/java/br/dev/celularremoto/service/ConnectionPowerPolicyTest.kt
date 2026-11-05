package br.dev.celularremoto.service

import org.junit.Assert.*
import org.junit.Test

class ConnectionPowerPolicyTest {
    @Test fun modesAndDeadline() {
        assertFalse(ConnectionPowerPolicy.validMode("always-remote"))
        assertTrue(ConnectionPowerPolicy.validMode("off"))
        assertTrue(ConnectionPowerPolicy.cpuWanted("monitor", 0, 1_000, true))
        assertFalse(ConnectionPowerPolicy.cpuWanted("monitor", 0, 1_000, false))
        assertTrue(ConnectionPowerPolicy.cpuWanted("30m", 2_000, 1_000, true))
        assertFalse(ConnectionPowerPolicy.cpuWanted("30m", 2_000, 2_000, true))
        assertFalse(ConnectionPowerPolicy.cpuWanted("off", 2_000, 1_000, true))
    }
    @Test fun gapsAreNotConnectionFailures() {
        assertNull(ConnectionPowerPolicy.schedulingGap(0L, 60_000L))
        assertNull(ConnectionPowerPolicy.schedulingGap(10_000L, 25_000L))
        assertEquals(60_000L, ConnectionPowerPolicy.schedulingGap(10_000L, 70_000L))
    }
}
