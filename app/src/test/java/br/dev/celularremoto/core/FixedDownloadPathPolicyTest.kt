package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class FixedDownloadPathPolicyTest {
    @Test fun rootFolder() {
        assertEquals("Download/Celular Remoto/", FixedDownloadPathPolicy.folder(emptyList()))
        assertTrue(FixedDownloadPathPolicy.contained(FixedDownloadPathPolicy.ROOT))
    }
    @Test fun nestedFolder() {
        assertEquals("Download/Celular Remoto/Videos/2026/",
            FixedDownloadPathPolicy.folder(listOf("Videos", "2026")))
        assertTrue(FixedDownloadPathPolicy.contained("Download/Celular Remoto/Videos/2026/"))
    }
    @Test fun cannotEscapeFolder() {
        for (name in listOf("..", ".", "A/B", "A\\B", "\u0000secret")) {
            assertThrows(IllegalArgumentException::class.java) {
                FixedDownloadPathPolicy.folder(listOf(name))
            }
        }
        assertFalse(FixedDownloadPathPolicy.contained("Download/Celular Remoto/../"))
        assertFalse(FixedDownloadPathPolicy.contained("Download/Outra/"))
        assertFalse(FixedDownloadPathPolicy.contained("Download/Celular Remoto2/"))
        assertFalse(FixedDownloadPathPolicy.contained("Download/Celular Remoto/Vi//"))
    }
    @Test fun depthLimit() {
        assertTrue(FixedDownloadPathPolicy.contained(
            FixedDownloadPathPolicy.folder(List(RemoteFilePolicy.MAX_DEPTH) { "a" })))
        assertThrows(IllegalArgumentException::class.java) {
            FixedDownloadPathPolicy.folder(List(RemoteFilePolicy.MAX_DEPTH + 1) { "a" })
        }
    }
}
