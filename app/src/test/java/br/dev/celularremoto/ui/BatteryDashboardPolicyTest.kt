package br.dev.celularremoto.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDashboardPolicyTest {
    @Test fun compactWhenWidthIsNarrow() {
        assertEquals(2, BatteryDashboardPolicy.metricColumns(320, 1.0f))
        assertEquals(2, BatteryDashboardPolicy.metricColumns(359, 1.0f))
    }

    @Test fun threeMetricsWhenSpaceAllows() {
        assertEquals(3, BatteryDashboardPolicy.metricColumns(360, 1.0f))
        assertEquals(3, BatteryDashboardPolicy.metricColumns(393, 1.20f))
        assertEquals(3, BatteryDashboardPolicy.metricColumns(800, 1.0f))
    }

    @Test fun largeFontsAlwaysUseTwoColumns() {
        assertEquals(2, BatteryDashboardPolicy.metricColumns(393, 1.3f))
        assertEquals(2, BatteryDashboardPolicy.metricColumns(800, 1.5f))
    }

    @Test fun forecastNamesTheActualTargetAndNeverPromisesMissingData() {
        assertEquals("≈ 5h35 até 0%", BatteryDashboardPolicy.forecastLabel("DISCHARGE", 335L))
        assertEquals("≈ 25min até 100%", BatteryDashboardPolicy.forecastLabel("CHARGE", 25L))
        assertEquals("≈ 2h05 até 100%", BatteryDashboardPolicy.forecastLabel("CHARGE", 125L))
        assertEquals("Bateria em 0%", BatteryDashboardPolicy.forecastLabel("DISCHARGE", 0L))
        assertEquals("Bateria em 100%", BatteryDashboardPolicy.forecastLabel("CHARGE", 0L))
        assertNull(BatteryDashboardPolicy.forecastLabel("DISCHARGE", -1L))
        assertNull(BatteryDashboardPolicy.forecastLabel("UNKNOWN", 35L))
    }

    @Test fun rejectsStaleMissingAndFutureTimestamps() {
        assertTrue(BatteryDashboardPolicy.isRecent(300_000L, 150_000L, 150_000L))
        assertFalse(BatteryDashboardPolicy.isRecent(300_000L, 149_999L, 150_000L))
        assertFalse(BatteryDashboardPolicy.isRecent(300_000L, 300_001L, 150_000L))
        assertFalse(BatteryDashboardPolicy.isRecent(300_000L, 0L, 150_000L))
    }

    @Test fun rejectsForecastFromDifferentLevelOrBatteryState() {
        assertTrue(BatteryDashboardPolicy.matchesSession(3, 0, 20, "DISCHARGE", 20))
        assertTrue(BatteryDashboardPolicy.matchesSession(3, 1, 20, "DISCHARGE", 20))
        assertTrue(BatteryDashboardPolicy.matchesSession(2, 1, 61, "CHARGE", 61))
        assertFalse(BatteryDashboardPolicy.matchesSession(2, 0, 61, "CHARGE", 61))
        assertFalse(BatteryDashboardPolicy.matchesSession(2, 1, 20, "DISCHARGE", 20))
        assertFalse(BatteryDashboardPolicy.matchesSession(3, 0, 20, "DISCHARGE", 21))
        assertFalse(BatteryDashboardPolicy.matchesSession(4, 0, 20, "DISCHARGE", 20))
        assertFalse(BatteryDashboardPolicy.matchesSession(3, 0, -1, "DISCHARGE", -1))
    }

    @Test fun chartTimeIsElapsedAndHasUsefulUnitsForShortSessions() {
        assertEquals("0s", BatteryDashboardPolicy.durationLabel(0L))
        assertEquals("30s", BatteryDashboardPolicy.durationLabel(30_000L))
        assertEquals("1min 5s", BatteryDashboardPolicy.durationLabel(65_000L))
        assertEquals("22h 48min 59s", BatteryDashboardPolicy.durationLabel(82_139_000L))
    }

    @Test fun axesKeepUsefulPrecisionAndNeverExceedPercentageBounds() {
        val locale = Locale.forLanguageTag("pt-BR")
        assertEquals("40,6", BatteryDashboardPolicy.axisLabel("temp", 40.61, locale))
        assertEquals("40,4", BatteryDashboardPolicy.axisLabel("temp", 40.41, locale))
        assertEquals("-561", BatteryDashboardPolicy.axisLabel("ma", -561.05, locale))
        assertEquals("60%", BatteryDashboardPolicy.axisLabel("percent", 59.88, locale))
        assertEquals("0%", BatteryDashboardPolicy.axisLabel("percent", -0.1, locale))
        assertEquals("100%", BatteryDashboardPolicy.axisLabel("percent", 100.12, locale))
        assertNull(BatteryDashboardPolicy.axisLabel("unknown", 1.0, locale))
    }
}
