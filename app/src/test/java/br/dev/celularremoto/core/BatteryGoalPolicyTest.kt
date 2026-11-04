package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class BatteryGoalPolicyTest {
    @Test fun alertsOnlyOnCrossings() {
        assertEquals(listOf("GOAL"), BatteryGoalPolicy.crossings(79, 80, 36.0, 36.2, true, 80, 20, 43))
        assertTrue(BatteryGoalPolicy.crossings(80, 81, 36.0, 37.0, true, 80, 20, 43).isEmpty())
        assertTrue(BatteryGoalPolicy.crossings(79, 80, 36.0, 37.0, false, 80, 20, 43).isEmpty())
        assertEquals(listOf("LOW"), BatteryGoalPolicy.crossings(21, 20, null, null, false, 80, 20, 43))
        assertEquals(listOf("HOT"), BatteryGoalPolicy.crossings(50, 50, 42.8, 43.0, false, 80, 20, 43))
    }

    @Test fun noRetroactiveOrInvalidValues() {
        assertTrue(BatteryGoalPolicy.crossings(-1, 80, 30.0, 50.0, true, 80, 20, 43).isEmpty())
        assertTrue(BatteryGoalPolicy.crossings(0, 0, null, null, false, 80, 20, 43).isEmpty())
        assertTrue(BatteryGoalPolicy.crossings(50, 50, Double.NaN, 44.0, false, 80, 20, 43).isEmpty())
    }
}
