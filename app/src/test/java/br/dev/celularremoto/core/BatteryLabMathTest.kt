package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryLabMathTest {
    @Test fun stableChargeComputesWeightedMean() {
        val interval = BatteryLabMath.classify("CHARGING", "CHARGING", 100.0, 200.0,
            "NOW", "AVERAGE", 30_000L, 30_050L)
        assertEquals(30_000L, interval.chargingMs)
        assertEquals(30_000L, interval.validCurrentMs)
        assertEquals(150.0, BatteryLabMath.meanCurrent(interval.currentMaMs, interval.validCurrentMs)!!, 0.00001)
    }
    @Test fun gapDoesNotFabricateTime() {
        val interval = BatteryLabMath.classify("CHARGING", "CHARGING", 100.0, 150.0,
            "NOW", "NOW", 91_000L, 91_000L)
        assertEquals(0L, interval.chargingMs)
        assertEquals(0L, interval.validCurrentMs)
        assertEquals(91_000L, BatteryLabMath.unknownMs(91_000L, 0L, 0L))
    }
    @Test fun stateTransitionOrClockJumpIsUnknown() {
        val transition = BatteryLabMath.classify("CHARGING", "IDLE", 100.0, null,
            "NOW", "NONE", 30_000L, 30_000L)
        val drift = BatteryLabMath.classify("CHARGING", "CHARGING", 100.0, 200.0,
            "NOW", "NOW", 30_000L, 55_000L)
        assertEquals(0L, transition.chargingMs)
        assertEquals(0L, drift.chargingMs)
    }
    @Test fun invalidSourceAndSignDoNotCountTowardAverage() {
        val negative = BatteryLabMath.classify("CHARGING", "CHARGING", -200.0, -100.0,
            "NOW", "NOW", 30_000L, 30_000L)
        val estimate = BatteryLabMath.classify("CHARGING", "CHARGING", 200.0, 250.0,
            "ESTIMATE", "ESTIMATE", 30_000L, 30_000L)
        assertEquals(30_000L, negative.chargingMs)
        assertEquals(0L, negative.validCurrentMs)
        assertEquals(0L, estimate.validCurrentMs)
        assertNull(BatteryLabMath.meanCurrent(0.0, 0L))
    }
    @Test fun dischargeCannotBeCountedAsCharging() {
        val interval = BatteryLabMath.classify("DISCHARGING", "DISCHARGING", -200.0, -100.0,
            "NOW", "NOW", 30_000L, 30_000L)
        assertEquals(0L, interval.chargingMs)
        assertEquals(30_000L, interval.idleMs)
    }
}
