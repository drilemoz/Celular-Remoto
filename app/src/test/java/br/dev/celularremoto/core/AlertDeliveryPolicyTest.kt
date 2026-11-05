package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class AlertDeliveryPolicyTest {
    @Test fun blockedIsNotAcknowledged() {
        assertFalse(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.PERMISSION_DENIED))
        assertFalse(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.APP_BLOCKED))
        assertFalse(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.CHANNEL_BLOCKED))
        assertFalse(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.ERROR))
        assertTrue(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.POSTED))
        assertTrue(AlertDeliveryPolicy.acknowledged(AlertDeliveryPolicy.Result.SUPPRESSED))
    }

    @Test fun resolvedLocalGoalsDoNotReplay() {
        assertTrue(AlertDeliveryPolicy.goalStillRelevant("GOAL", 82, 35.0, 80, 20, 43))
        assertFalse(AlertDeliveryPolicy.goalStillRelevant("GOAL", 60, 35.0, 80, 20, 43))
        assertTrue(AlertDeliveryPolicy.goalStillRelevant("LOW", 18, 35.0, 80, 20, 43))
        assertFalse(AlertDeliveryPolicy.goalStillRelevant("LOW", 50, 35.0, 80, 20, 43))
        assertTrue(AlertDeliveryPolicy.goalStillRelevant("HOT", 60, 44.0, 80, 20, 43))
        assertFalse(AlertDeliveryPolicy.goalStillRelevant("HOT", 60, 39.0, 80, 20, 43))
    }

    @Test fun retryRecentOnly() {
        val now = 1_900_000_000_000L
        assertTrue(AlertDeliveryPolicy.retryAllowed(now - 60_000L, now, false))
        assertFalse(AlertDeliveryPolicy.retryAllowed(now - 60_000L, now, true))
        assertFalse(AlertDeliveryPolicy.retryAllowed(now - 25 * 60L * 60_000L, now, false))
        assertFalse(AlertDeliveryPolicy.retryAllowed(now + 120_000L, now, false))
        assertFalse(AlertDeliveryPolicy.retryAllowed(0L, now, false))
    }
}
