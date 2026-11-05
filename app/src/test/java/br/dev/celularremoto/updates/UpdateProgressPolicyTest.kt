package br.dev.celularremoto.updates

import org.junit.Assert.*
import org.junit.Test

class UpdateProgressPolicyTest {
    @Test fun rejectsUnknownAndInvalidDownloadSizes() {
        assertNull(UpdateProgressPolicy.percent(0, 0))
        assertNull(UpdateProgressPolicy.percent(-1, 100))
        assertNull(UpdateProgressPolicy.percent(1, -1))
    }
    @Test fun clampsProgressWithoutOverflow() {
        assertEquals(0, UpdateProgressPolicy.percent(0, 100))
        assertEquals(50, UpdateProgressPolicy.percent(50, 100))
        assertEquals(100, UpdateProgressPolicy.percent(250, 100))
        assertEquals(50, UpdateProgressPolicy.percent(Long.MAX_VALUE / 2, Long.MAX_VALUE - 1))
    }
    @Test fun recognizesPendingAndTerminalStages() {
        assertTrue(UpdateProgressPolicy.monitorRemote("checking"))
        assertTrue(UpdateProgressPolicy.monitorRemote("downloading"))
        assertTrue(UpdateProgressPolicy.monitorRemote("verifying"))
        assertFalse(UpdateProgressPolicy.monitorRemote("ready"))
        assertFalse(UpdateProgressPolicy.monitorRemote("error"))
        assertTrue(UpdateProgressPolicy.cancellable("downloading"))
        assertFalse(UpdateProgressPolicy.cancellable("ready"))
    }
    @Test fun installRequiresExplicitLocalApproval() {
        assertEquals("APK ainda não verificado", UpdateProgressPolicy.installPermissionLabel(false, false))
        assertTrue(UpdateProgressPolicy.installPermissionLabel(true, false).contains("autorização local"))
        assertTrue(UpdateProgressPolicy.installPermissionLabel(true, true).contains("confirmação local"))
    }
}
