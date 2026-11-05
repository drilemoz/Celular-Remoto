package br.dev.celularremoto.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorServiceModePolicyTest {
    @Test fun neitherEnabledIsOff() {
        assertEquals(MonitorServiceModePolicy.Mode.OFF, MonitorServiceModePolicy.resolve(false, false))
        assertFalse(MonitorServiceModePolicy.shouldCollect(MonitorServiceModePolicy.Mode.OFF))
    }
    @Test fun localOnlyDoesNotOpenRemote() {
        assertEquals(MonitorServiceModePolicy.Mode.BATTERY_ONLY, MonitorServiceModePolicy.resolve(false, true))
        assertTrue(MonitorServiceModePolicy.shouldCollect(MonitorServiceModePolicy.Mode.BATTERY_ONLY))
    }
    @Test fun remoteWinsWithoutDuplicatingSampling() {
        assertEquals(MonitorServiceModePolicy.Mode.REMOTE, MonitorServiceModePolicy.resolve(true, true))
        assertEquals(MonitorServiceModePolicy.Mode.REMOTE, MonitorServiceModePolicy.resolve(true, false))
    }
}
