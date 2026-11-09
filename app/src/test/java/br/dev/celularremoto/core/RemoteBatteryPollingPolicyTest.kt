package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteBatteryPollingPolicyTest {
    @Test fun regularMinuteAndBackoff() {
        assertEquals(60_000L, RemoteBatteryPollingPolicy.nextDelayMs(0))
        assertEquals(120_000L, RemoteBatteryPollingPolicy.nextDelayMs(1))
        assertEquals(180_000L, RemoteBatteryPollingPolicy.nextDelayMs(2))
        assertEquals(300_000L, RemoteBatteryPollingPolicy.nextDelayMs(3))
        assertEquals(300_000L, RemoteBatteryPollingPolicy.nextDelayMs(20))
    }
}
