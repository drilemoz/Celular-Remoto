package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class RemoteEventNoticePolicyTest {
    @Test fun categoriesKeepWhatsappSeparateFromOtherApps() {
        assertEquals(RemoteEventNoticePolicy.Category.SMS, RemoteEventNoticePolicy.classify("sms", ""))
        assertEquals(RemoteEventNoticePolicy.Category.WHATSAPP, RemoteEventNoticePolicy.classify("notification", "com.whatsapp"))
        assertEquals(RemoteEventNoticePolicy.Category.WHATSAPP, RemoteEventNoticePolicy.classify("notification", "com.whatsapp.w4b"))
        assertEquals(RemoteEventNoticePolicy.Category.OTHER, RemoteEventNoticePolicy.classify("notification", "org.example"))
    }
    @Test fun filtersAreIndependent() {
        assertTrue(RemoteEventNoticePolicy.enabled(RemoteEventNoticePolicy.Category.SMS, true, false, false))
        assertFalse(RemoteEventNoticePolicy.enabled(RemoteEventNoticePolicy.Category.WHATSAPP, true, false, false))
        assertFalse(RemoteEventNoticePolicy.enabled(RemoteEventNoticePolicy.Category.OTHER, true, true, false))
    }
    @Test fun alertAgeIsBoundedAndInvalidTimesDoNotNotify() {
        val now = 1_800_000_000_000L
        assertTrue(RemoteEventNoticePolicy.recentArrival(now - 3_600_000L, now))
        assertFalse(RemoteEventNoticePolicy.recentArrival(now - 3_600_001L, now))
        assertTrue(RemoteEventNoticePolicy.recentArrival(now + 60_000L, now))
        assertFalse(RemoteEventNoticePolicy.recentArrival(now + 60_001L, now))
        assertFalse(RemoteEventNoticePolicy.recentArrival(0, now))
        assertEquals("Recebida há 17 min · toque para consultar",
            RemoteEventNoticePolicy.ageLabel(now - 17 * 60_000L, now))
    }
    @Test fun changedPairingCannotReuseAlertCursor() {
        val a = RemoteEventNoticePolicy.pairingScope("192.168.1.5", byteArrayOf(1, 2, 3))
        val b = RemoteEventNoticePolicy.pairingScope("192.168.1.5", byteArrayOf(3, 2, 1))
        assertNotEquals(a, b)
        assertEquals(a, RemoteEventNoticePolicy.pairingScope(" 192.168.1.5 ", byteArrayOf(1, 2, 3)))
        assertNotEquals(a, RemoteEventNoticePolicy.pairingScope("192.168.1.6", byteArrayOf(1, 2, 3)))
    }
}
