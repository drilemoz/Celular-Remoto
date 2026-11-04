package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class BatterySessionMathTest {
    @Test fun forecastRequiresTenMinutesAndTwoPoints() {
        assertNull(BatterySessionMath.forecastMinutes("CHARGE", 20, 21, 20 * 60_000L))
        assertNull(BatterySessionMath.forecastMinutes("CHARGE", 20, 24, 9 * 60_000L))
        assertEquals(380L, BatterySessionMath.forecastMinutes("CHARGE", 20, 24, 20 * 60_000L))
        assertEquals(380L, BatterySessionMath.forecastMinutes("DISCHARGE", 80, 76, 20 * 60_000L))
    }

    @Test fun forecastRejectsWrongDirectionAndUnknown() {
        assertNull(BatterySessionMath.forecastMinutes("CHARGE", 55, 50, 20 * 60_000L))
        assertNull(BatterySessionMath.forecastMinutes("DISCHARGE", 50, 55, 20 * 60_000L))
        assertNull(BatterySessionMath.forecastMinutes("UNKNOWN", 10, 20, 30 * 60_000L))
        assertNull(BatterySessionMath.forecastMinutes("CHARGE", 0, 95, 10 * 60_000L))
    }

    @Test fun intervalsDoNotFabricateGaps() {
        assertEquals(60_000L, BatterySessionMath.validInterval("CHARGE", "CHARGE", "CHARGE", 60_000L, 60_000L))
        assertEquals(0L, BatterySessionMath.validInterval("CHARGE", "CHARGE", "CHARGE", 121_000L, 121_000L))
        assertEquals(0L, BatterySessionMath.validInterval("CHARGE", "DISCHARGE", "CHARGE", 60_000L, 60_000L))
        assertEquals(0L, BatterySessionMath.validInterval("CHARGE", "CHARGE", "CHARGE", 60_000L, 30_000L))
    }
}
