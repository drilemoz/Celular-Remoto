package br.dev.celularremoto.updates

import org.junit.Assert.*
import org.junit.Test

class UpdateRulesTest {
    private val sha = "a".repeat(64)
    private val name = "CelularRemoto-v0.1.0-alpha.111.apk"

    @Test fun acceptsSignedNewerRelease() {
        assertTrue(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 110, sha,
            UpdateRules.PACKAGE, true, "v0.1.0-alpha.111", name))
    }

    @Test fun rejectsDowngradeAndUnsignedBuild() {
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 111, sha,
            UpdateRules.PACKAGE, true, "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 110, sha,
            UpdateRules.PACKAGE, false, "v0.1.0-alpha.111", name))
    }

    @Test fun rejectsMismatchedPackageVersionTagAndHash() {
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 110, sha,
            "br.dev.monitorremoto", true, "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 112, 110, sha,
            UpdateRules.PACKAGE, true, "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 110, "not-hash",
            UpdateRules.PACKAGE, true, "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.isValidRelease("0.1.0-alpha.111", 111, 110, sha,
            UpdateRules.PACKAGE, true, "v0.1.0-alpha.110", name))
    }

    @Test fun validatesExactAssetOrigin() {
        val expected = "https://github.com/drilemoz/Celular-Remoto/releases/download/v0.1.0-alpha.111/$name"
        assertTrue(UpdateRules.allowedAssetUrl(expected, "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.allowedAssetUrl(expected.replace("github.com", "github.com.evil.tld"),
            "v0.1.0-alpha.111", name))
        assertFalse(UpdateRules.allowedAssetUrl(expected.replace("https://", "http://"),
            "v0.1.0-alpha.111", name))
    }

    @Test fun acceptsLaterSemanticVersionWithoutGuessingNumericCode() {
        assertEquals("CelularRemoto-v1.0.0.apk", UpdateRules.apkName("1.0.0"))
        assertNull(UpdateRules.apkName("../../evil.apk"))
    }
}
