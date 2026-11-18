package br.dev.celularremoto.ui

import org.junit.Assert.*
import org.junit.Test

class AppFunctionSearchTest {
    private val index = AppFunctionCatalog.search
    private val entries = AppFunctionCatalog.entries

    @Test fun indexIsUniqueAndContainsAllMainActionsAndSettings() {
        assertTrue(entries.size > 120)
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        val tools = entries.mapNotNull { (it.destination as? FunctionDestination.Controller)?.tool }.toSet()
        assertEquals(ControllerTool.entries.toSet(), tools)
        val sections = entries.mapNotNull { (it.destination as? FunctionDestination.Settings)?.section }.toSet()
        assertEquals(settingGroups.flatMap { group -> group.entries.map { it.section } }.toSet() - "adb", sections)
        assertEquals(PermissionCenter.entries().size, entries.count { it.id.startsWith("setting-permissions-") })
        assertEquals(entries, index.find(""))
    }

    @Test fun matchesExactTitleAccentInsensitiveAndToleratesTypos() {
        assertEquals("Teste solar", index.find("TESTE SOLAR").first().title)
        assertEquals("Teste solar", index.find("testi soalr").first().title)
        assertEquals("Localização do aparelho", index.find("localizacao do aparelho").first().title)
        assertEquals("Histórico de chamadas", index.find("onde encontro historico de chamdas").first().title)
    }

    @Test fun matchesCommonWordsAndShowsIndependentDestinations() {
        assertTrue(index.find("placa solar").take(3).any { it.title == "Teste solar" })
        assertTrue(index.find("zap").take(3).any { it.id == "setting-notifications-wa" })
        assertTrue(index.find("baixar apk").any { it.id == "setting-updates-download" })
        assertTrue(index.find("tema escuro").any { it.id == "setting-appearance-dark" })
        assertTrue(index.find("push sms").any { it.id == "shortcut-MESSAGE_NOTICES" })
        assertTrue(index.find("chamadas").any { it.id == "setting-sms-share_calls" })
    }

    @Test fun noResultsForUnrelatedDataOrForStopWordsAlone() {
        assertTrue(index.find("jhbtq99zz").isEmpty())
        assertTrue(index.find("de em o").isEmpty())
        assertTrue(index.find("teste solar", FunctionArea.SETTINGS).none { it.id == "battery-solar" })
        assertTrue(index.find("", FunctionArea.BATTERY).all { it.area == FunctionArea.BATTERY })
    }
}
