package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

/** Contrato de identificação usado por Mensagens e pelo observador. */
class RemoteMessagePairingTest {
    @Test fun sameIpNewKeyProducesNewSession() {
        val host = "192.168.100.25"
        val older = RemoteEventNoticePolicy.pairingScope(host, byteArrayOf(1, 2, 3))
        val newPair = RemoteEventNoticePolicy.pairingScope(host, byteArrayOf(1, 2, 4))
        assertNotEquals(older, newPair)
        assertEquals(older, RemoteEventNoticePolicy.pairingScope(host, byteArrayOf(1, 2, 3)))
    }
    @Test fun oldPairingIsRejectedEvenWhenIpMatches() {
        val host = "192.168.1.10"
        val oldScope = RemoteEventNoticePolicy.pairingScope(host, byteArrayOf(1, 2, 3))
        val newScope = RemoteEventNoticePolicy.pairingScope(host, byteArrayOf(1, 2, 9))
        assertTrue(RemoteMessageSessionPolicy.matches(host, oldScope, host, oldScope))
        assertFalse(RemoteMessageSessionPolicy.matches(host, oldScope, host, newScope))
        assertFalse(RemoteMessageSessionPolicy.matches(host, oldScope, "192.168.1.11", oldScope))
        assertFalse(RemoteMessageSessionPolicy.matches(host, "", host, oldScope))
        assertFalse(RemoteMessageSessionPolicy.matches("", oldScope, host, oldScope))
    }
    @Test fun distinctHostsMustNotShareLastViewedCursor() {
        val key = byteArrayOf(40, 41, 42)
        assertNotEquals(RemoteEventNoticePolicy.pairingScope("192.168.1.10", key),
            RemoteEventNoticePolicy.pairingScope("192.168.1.11", key))
    }
}
