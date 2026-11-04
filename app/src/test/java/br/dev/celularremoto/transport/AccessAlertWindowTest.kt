package br.dev.celularremoto.transport

import org.junit.Assert.*
import org.junit.Test

class AccessAlertWindowTest {
    @Test fun warnsOnceForTheSameSession() {
        val window = AccessAlertWindow()
        assertTrue(window.observe(listOf("phone-A:1000")))
        assertFalse(window.observe(listOf("phone-A:1000")))
        assertFalse(window.observe(listOf("phone-A:1000", "phone-A:1000")))
    }

    @Test fun expiresAndTreatsReturningClientAsNew() {
        val window = AccessAlertWindow()
        assertTrue(window.observe(listOf("phone-A:1000")))
        assertFalse(window.observe(emptyList()))
        assertEquals(0, window.retainedCount())
        assertTrue(window.observe(listOf("phone-A:80000")))
    }

    @Test fun keepsOnlyActiveSessionsWithBoundedMemory() {
        val window = AccessAlertWindow()
        repeat(10000) { index ->
            assertTrue(window.observe(listOf("phone-$index:$index")))
            assertEquals(1, window.retainedCount())
        }
    }

    @Test fun alertsForNewClientWithoutRealertingOldClient() {
        val window = AccessAlertWindow()
        assertTrue(window.observe(listOf("A:1")))
        assertTrue(window.observe(listOf("A:1", "B:2")))
        assertFalse(window.observe(listOf("B:2", "A:1")))
        window.clear()
        assertTrue(window.observe(listOf("B:2")))
    }
}
