package br.dev.celularremoto.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailCopyPolicyTest {
    @Test fun unavailableValuesCannotBeCopied() {
        listOf("", " ", "null", "Não disponível", "Não compartilhado",
            "Sem leitura/permissão", "Permissão não concedida").forEach {
            assertFalse(DetailCopyPolicy.isCopyable(it))
        }
    }

    @Test fun phoneAndIpCanBeCopiedWhenTheyAreActuallyPresent() {
        assertTrue(DetailCopyPolicy.isCopyable("+55 31 99999-9999"))
        assertTrue(DetailCopyPolicy.isCopyable("192.168.100.210"))
        assertTrue(DetailCopyPolicy.isCopyable("0 mA"))
    }

    @Test fun completeReportHasSectionsWithoutMissingValues() {
        val report = DetailCopyPolicy.report("Redmi", "05/11 11:33", listOf(
            DetailCopyPolicy.Field("Rede", "IP", "192.168.100.210"),
            DetailCopyPolicy.Field("Chip", "Número da linha", "Não compartilhado"),
            DetailCopyPolicy.Field("Chip", "Operadora", "VIVO")
        ))
        assertTrue(report.contains("IP: 192.168.100.210"))
        assertTrue(report.contains("Operadora: VIVO"))
        assertFalse(report.contains("Número da linha"))
        assertFalse(report.contains("Não compartilhado"))
    }

    @Test fun phoneIsIncludedOnlyWhenProvided() {
        val report = DetailCopyPolicy.report("", "", listOf(
            DetailCopyPolicy.Field("Chip", "Número da linha", "+55 31 99999-9999")
        ))
        assertTrue(report.contains("Número da linha: +55 31 99999-9999"))
    }
}
