package br.dev.celularremoto.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Trocas de aparelho/permissão/tema/evento devem refazer os comandos. */
class RemoteActionsRenderKeyTest {
    private val base = RemoteActionsRenderKey("192.168.1.10", true, 0, "light", 1f, 393)

    @Test fun unchangedStateReusesPanel() {
        assertEquals(base, base.copy())
    }

    @Test fun differentDeviceOrPairingRequiresNewPanel() {
        assertNotEquals(base, base.copy(host = "192.168.1.11"))
        assertNotEquals(base, base.copy(keyAvailable = false))
    }

    @Test fun eventsOrAppearanceRequiresNewPanel() {
        assertNotEquals(base, base.copy(eventCount = 1))
        assertNotEquals(base, base.copy(theme = "amoled"))
        assertNotEquals(base, base.copy(fontScale = 1.3f))
        assertNotEquals(base, base.copy(widthDp = 320))
    }
}
