package br.dev.celularremoto.transport

import br.dev.celularremoto.BuildConfig
import org.junit.Assert.*
import org.junit.Test

class AppVersionTest {
    @Test fun apkVersionAndWireVersionAreIndependent() {
        // Não repetir o número da versão no teste: o Gradle é a fonte de verdade.
        assertEquals(BuildConfig.VERSION_NAME, AppVersion.NAME)
        assertEquals("0.1.0-alpha.${BuildConfig.VERSION_CODE}", AppVersion.NAME)
        assertNotEquals(AppVersion.NAME, AppVersion.WIRE_COMPAT)
        assertEquals("0.1.0-alpha.111", AppVersion.WIRE_COMPAT)
        assertTrue(AppVersion.compatible(AppVersion.WIRE_COMPAT))
        assertTrue(AppVersion.compatible(AppVersion.NAME))
    }
    @Test fun unknownOrOlderProtocolsAreRejected() {
        listOf("0.1.0-alpha.19", "0.1.0-alpha.43", "0.1.0-alpha.107", "0.1.0-alpha.110", "", "malformed").forEach {
            assertFalse(AppVersion.compatible(it))
        }
        assertFalse(AppVersion.compatible(null))
        assertTrue(AppVersion.incompatibility("0.1.0-alpha.110").contains("Protocolo remoto"))
    }
}
