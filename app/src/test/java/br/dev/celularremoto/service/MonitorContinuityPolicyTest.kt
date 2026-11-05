package br.dev.celularremoto.service

import org.junit.Assert.*
import org.junit.Test

class MonitorContinuityPolicyTest {
    @Test fun savesOccasionallyAndAfterClockReset() {
        assertTrue(MonitorContinuityPolicy.shouldSaveHeartbeat(0L, 10L))
        assertFalse(MonitorContinuityPolicy.shouldSaveHeartbeat(10_000L, 20_000L))
        assertTrue(MonitorContinuityPolicy.shouldSaveHeartbeat(10_000L, 130_000L))
        assertTrue(MonitorContinuityPolicy.shouldSaveHeartbeat(150_000L, 1_000L))
    }
    @Test fun doesntMislabelUnknownAge() {
        assertNull(MonitorContinuityPolicy.ageMinutes(0L, 100_000L))
        assertNull(MonitorContinuityPolicy.ageMinutes(200_000L, 100_000L))
        assertEquals(2L, MonitorContinuityPolicy.ageMinutes(60_000L, 180_000L))
    }
    @Test fun countsOnlyUnclosedStartsAndCaps() {
        assertEquals(3, MonitorContinuityPolicy.nextUnclosedCount(3, false))
        assertEquals(4, MonitorContinuityPolicy.nextUnclosedCount(3, true))
        assertEquals(1_000_000, MonitorContinuityPolicy.nextUnclosedCount(1_000_000, true))
        assertEquals(1_000_000, MonitorContinuityPolicy.nextUnclosedCount(Int.MAX_VALUE, true))
    }
}
