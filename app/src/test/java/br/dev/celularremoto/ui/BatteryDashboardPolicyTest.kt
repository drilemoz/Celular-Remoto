package br.dev.celularremoto.ui

import org.junit.Assert.assertEquals
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
}
