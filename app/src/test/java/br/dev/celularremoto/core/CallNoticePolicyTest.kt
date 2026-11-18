package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class CallNoticePolicyTest {
    @Test fun baselineIsSilentAndNewRecordsAreNotDuplicated() {
        val armed = 100_000L
        val first = CallNoticePolicy.advance(listOf(CallNoticePolicy.Row(10, 95_000L, 1)), -1L, armed, armed)
        assertEquals(10L, first.highWater)
        assertTrue(first.fresh.isEmpty())
        val missed = CallNoticePolicy.Row(11, 101_000L, 3)
        val next = CallNoticePolicy.advance(listOf(missed, missed), first.highWater, armed, 101_000L)
        assertEquals(listOf(missed), next.fresh)
        assertTrue(CallNoticePolicy.advance(listOf(missed), next.highWater, armed, 103_000L).fresh.isEmpty())
    }

    @Test fun checksCompletionAndRejectsImportedOldCallsVoicemailAndFutureRows() {
        val armed = 10_000_000L
        val answered = CallNoticePolicy.Row(41, armed - 7_200_000L, 1, 7_201L)
        val old = CallNoticePolicy.Row(42, armed - 7_200_000L, 3)
        val voicemail = CallNoticePolicy.Row(43, armed, 4)
        val future = CallNoticePolicy.Row(44, armed + 70_000L, 2)
        val state = CallNoticePolicy.advance(listOf(old, answered, future, voicemail), 40, armed, armed + 1_000L)
        assertEquals(listOf(answered), state.fresh)
        assertEquals(44L, state.highWater)
    }

    @Test fun doesNotExposeHiddenNumbersAndLabelsOnlyKnownCallTypes() {
        assertEquals("+55 (37) 1234-5678", CallNoticePolicy.visibleNumber(1, " +55 (37) 1234-5678 "))
        assertNull(CallNoticePolicy.visibleNumber(2, "37912345678"))
        assertNull(CallNoticePolicy.visibleNumber(1, "restrito"))
        assertEquals("Chamada perdida", CallNoticePolicy.label(requireNotNull(CallNoticePolicy.kind(3))))
        assertNull(CallNoticePolicy.kind(4))
    }

    @Test fun callAndSmsFiltersAreIndependent() {
        val call = RemoteEventNoticePolicy.classify("call", "")
        val sms = RemoteEventNoticePolicy.classify("sms", "")
        assertTrue(RemoteEventNoticePolicy.enabled(call, sms = false, whatsapp = false, other = false, call = true))
        assertFalse(RemoteEventNoticePolicy.enabled(sms, sms = false, whatsapp = true, other = true, call = true))
        assertFalse(RemoteEventNoticePolicy.enabled(call, sms = true, whatsapp = true, other = true, call = false))
    }
}
