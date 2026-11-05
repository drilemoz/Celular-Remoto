package br.dev.celularremoto.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePolicyTest {
    @Test fun amoledDoesNotDependOnSystemNightMode() {
        assertTrue(AppearancePolicy.isDark("amoled", false))
        assertTrue(AppearancePolicy.isAmoled("amoled"))
        assertFalse(AppearancePolicy.isAmoled("dark"))
        assertFalse(AppearancePolicy.isDark("light", true))
        assertTrue(AppearancePolicy.isDark("system", true))
        assertFalse(AppearancePolicy.isDark("system", false))
    }

    @Test fun secondaryActionsDoNotCrowdOnSmallScreens() {
        assertTrue(AppearancePolicy.stackSecondaryActions(320, 1f))
        assertTrue(AppearancePolicy.stackSecondaryActions(393, 1.4f))
        assertFalse(AppearancePolicy.stackSecondaryActions(393, 1f))
        assertEquals(76, AppearancePolicy.navigationHeightDp(1.4f))
        assertEquals(60, AppearancePolicy.navigationHeightDp(1f))
        assertEquals(3, AppearancePolicy.actionLabelLines(1.25f))
    }
}
