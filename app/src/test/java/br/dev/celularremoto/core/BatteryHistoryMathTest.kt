package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class BatteryHistoryMathTest {
    private fun sample(kind: String = "SOLAR", sky: String = "forte", panel: Int = 8,
        screen: String = "APAGADA", observed: Long = 1_800_000L, final: Int = 45,
        duration: Long = 1_800_000L, ended: Long = 2_800_000L): BatteryHistoryMath.Session =
        BatteryHistoryMath.Session(kind, 1_000_000L, ended, duration, observed, 35, final,
            sky, panel, screen)

    @Test fun rateUsesElapsedTimeNotNominalPanelPower() {
        assertEquals(20.0, BatteryHistoryMath.rate(sample()), 0.001)
    }

    @Test fun insufficientCoverageHidesRate() {
        assertNull(BatteryHistoryMath.rate(sample(observed = 600_000L)))
    }

    @Test fun invalidPercentIsNotEstimated() {
        assertNull(BatteryHistoryMath.rate(sample().copy(finalPercent = 101)))
    }

    @Test fun twoPercentThresholdPreventsFalsePrecision() {
        assertNull(BatteryHistoryMath.rate(sample(final = 36)))
    }

    @Test fun activeTestIsNotCompared() {
        assertNull(BatteryHistoryMath.rate(sample(ended = 0L)))
    }

    @Test fun shortTestsAreNotExtrapolated() {
        assertNull(BatteryHistoryMath.rate(sample(duration = 300_000L)))
    }

    @Test fun equalConditionsAllowRateComparison() {
        val r = BatteryHistoryMath.compare(sample(final = 45), sample(final = 40))
        assertTrue(r.meaningful)
        assertEquals(-10.0, r.rateDifference!!, 0.001)
    }

    @Test fun differentSkyIsExplicitWarning() {
        val r = BatteryHistoryMath.compare(sample(), sample(sky = "nublado"))
        assertFalse(r.meaningful)
        assertTrue(r.warnings.any { "céu" in it })
        assertNull(r.rateDifference)
    }

    @Test fun differentKindsDoNotAssertComparableRates() {
        val r = BatteryHistoryMath.compare(sample(), sample(kind = "ELECTRIC"))
        assertFalse(r.meaningful)
        assertTrue(r.warnings.any { "Tipos" in it })
    }

    @Test fun unknownPanelIsWarned() {
        val r = BatteryHistoryMath.compare(sample(panel = 0), sample(panel = 0))
        assertFalse(r.meaningful)
        assertTrue(r.warnings.any { "Painéis" in it })
    }

    @Test fun differentScreenStatesAreWarned() {
        val r = BatteryHistoryMath.compare(sample(), sample(screen = "ACESA"))
        assertFalse(r.meaningful)
    }

    @Test fun observedTimeIsClampedToDuration() {
        assertEquals(1.0, BatteryHistoryMath.coverage(3_600_000L, 1_800_000L), 0.0)
        assertEquals(0.0, BatteryHistoryMath.coverage(10L, 0L), 0.0)
    }
}
