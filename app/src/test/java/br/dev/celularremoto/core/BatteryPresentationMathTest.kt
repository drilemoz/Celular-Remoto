package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryPresentationMathTest {
    @Test fun keepsNegativeCurrentWhileCharging() {
        assertEquals(-428.0, requireNotNull(BatteryPresentationMath.visualCurrentMa(-428.0, statusCode = 2)), 0.001)
        assertFalse(BatteryPresentationMath.isVisuallyAdjusted(-428.0, statusCode = 2))
    }

    @Test fun flipsPositiveCurrentForDischargingDisplayOnly() {
        assertEquals(-949.0, requireNotNull(BatteryPresentationMath.visualCurrentMa(949.0, statusCode = 3)), 0.001)
        assertTrue(BatteryPresentationMath.isVisuallyAdjusted(949.0, statusCode = 3))
        assertEquals("-949 mA", BatteryPresentationMath.currentText(949.0, statusCode = 3))
    }

    @Test fun sessionKindCanDriveDischargePresentation() {
        assertEquals(-437.0, requireNotNull(BatteryPresentationMath.visualCurrentMa(437.0, kind = "DISCHARGE")), 0.001)
        assertEquals("Saída líquida da bateria", BatteryPresentationMath.directionLabel(437.0, kind = "DISCHARGE"))
    }
}
