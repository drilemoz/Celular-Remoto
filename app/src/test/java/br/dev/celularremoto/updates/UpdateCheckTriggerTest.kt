package br.dev.celularremoto.updates

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckTriggerTest {
    @Test fun foregroundAndJobRespectAutomaticCheckSwitchAndCooldown() {
        for (trigger in listOf(UpdateCheckTrigger.FOREGROUND, UpdateCheckTrigger.SCHEDULED)) {
            assertFalse(trigger.canCheck(false, null, 0L))
            assertTrue(trigger.canCheck(true, null, 0L))
            assertFalse(trigger.canCheck(true, 0L, 59_999L))
            assertTrue(trigger.canCheck(true, 0L, 60_000L))
        }
    }

    @Test fun manualCheckCanRunAndAutoDownloadIndependentlyOfAutoCheckSwitch() {
        val manual = UpdateCheckTrigger.LOCAL_MANUAL
        assertTrue(manual.canCheck(false, 0L, 1L))
        assertTrue(manual.autoDownloads(true))
        assertFalse(manual.autoDownloads(false))
    }

    @Test fun remoteCheckDoesNotInheritAutomaticDownload() {
        assertTrue(UpdateCheckTrigger.REMOTE.canCheck(false, 0L, 1L))
        assertFalse(UpdateCheckTrigger.REMOTE.autoDownloads(true))
    }

    @Test fun onlyCanonicalAlphaTagsHaveAnInferredVersionCode() {
        assertEquals(144, UpdateRules.alphaCodeFromTag("v0.1.0-alpha.144") ?: -1)
        assertNull(UpdateRules.alphaCodeFromTag("0.1.0-alpha.144"))
        assertNull(UpdateRules.alphaCodeFromTag("v0.1.0-alpha.0144"))
        assertNull(UpdateRules.alphaCodeFromTag("v0.1.0-beta.144"))
        assertNull(UpdateRules.alphaCodeFromTag("v1.0.2"))
    }
}
