package br.dev.celularremoto.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactDesignPolicyTest {
    @Test fun smallScreensAndLargeFontsUseTwoColumns() {
        assertEquals(2, CompactDesignPolicy.columns(320, 1f))
        assertEquals(2, CompactDesignPolicy.columns(359, 1f))
        assertEquals(2, CompactDesignPolicy.columns(393, 1.3f))
    }

    @Test fun regularPhonesUseThreeAndTabletsFour() {
        assertEquals(3, CompactDesignPolicy.columns(360, 1f))
        assertEquals(3, CompactDesignPolicy.columns(393, 1f))
        assertEquals(4, CompactDesignPolicy.columns(600, 1f))
    }

    @Test fun accessibleScaleTakesPrecedenceOverTabletWidth() {
        assertEquals(2, CompactDesignPolicy.columns(800, 1.25f))
    }
}
