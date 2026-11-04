package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class BatteryCurrentMathTest {
    @Test fun negativeCurrentMustRemainVisibleWhileCharging() {
        val result = BatteryCurrentMath.select(-428_000, -390_000, null, 2)
        assertEquals(-428.0, result.ma!!, 0.001)
        assertEquals("NOW", result.source)
        assertTrue(result.warning!!.contains("negativa"))
    }

    @Test fun usesAverageWhenInstantaneousIsZeroOrUnavailable() {
        assertEquals("AVERAGE", BatteryCurrentMath.select(0, 310_000, null, 2).source)
        assertEquals(310.0, BatteryCurrentMath.select(null, 310_000, null, 2).ma!!, 0.001)
        assertEquals("NOW", BatteryCurrentMath.select(0, null, null, 2).source)
    }

    @Test fun invalidReadingsCanUseObservedCounterMean() {
        val reading = BatteryCurrentMath.select(Int.MIN_VALUE, Int.MAX_VALUE, 255.0, 2)
        assertEquals("COUNTER", reading.source)
        assertEquals(255.0, reading.ma!!, 0.001)
        assertNull(BatteryCurrentMath.select(Int.MIN_VALUE, Int.MAX_VALUE, null, 2).ma)
    }

    @Test fun counterRequiresRealElapsedTimeAndRejectsLargeJumps() {
        val tracker = BatteryCounterMeanTracker()
        assertNull(tracker.observe(2_000_000, 2, 0L))
        assertNull(tracker.observe(2_004_000, 2, 60_000L))
        val mean = tracker.observe(2_008_000, 2, 120_000L)
        assertNotNull(mean)
        assertEquals(240.0, mean!!, 0.001)
        assertEquals(120_000L, tracker.windowMs)
        assertNull(tracker.observe(10_000_000, 2, 121_000L))
    }

    @Test fun netPowerIsNotSolarPanelPower() {
        assertEquals(-1.6692, BatteryCurrentMath.batteryPowerWatts(3900, -428.0)!!, 0.0001)
        assertNull(BatteryCurrentMath.batteryPowerWatts(-1, 428.0))
    }
}
