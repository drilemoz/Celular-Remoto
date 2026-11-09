package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorNotificationContentTest {
    private val battery = MonitorNotificationContent.Battery(32, "Carregando", 39.9, 658.0,
        152L, 12.0, 100_000L)

    @Test fun compactTitleIncludesMeasurements() {
        assertEquals("32% · 39,9 °C · 658 mA", MonitorNotificationContent.title(battery))
    }
    @Test fun contradictoryCurrentIsNotMisrepresented() {
        assertEquals("32% · 39,9 °C", MonitorNotificationContent.title(battery.copy(state="Descarregando")))
    }
    @Test fun forecastIsNotInvented() {
        assertEquals("Previsão indisponível", MonitorNotificationContent.batteryLine(battery.copy(forecastMinutes=null)))
    }
    @Test fun forecastUsesObservedSession() {
        org.junit.Assert.assertTrue(MonitorNotificationContent.batteryLine(battery, 100_000L).startsWith("100% previsto: ~"))
    }
    @Test fun staleBatteryIsNotPresentedAsCurrent() {
        assertEquals(null, MonitorNotificationContent.valid(battery, 300_001L))
        assertEquals(battery, MonitorNotificationContent.valid(battery, 150_000L))
    }
    @Test fun remoteLastResponseIsNotCalledConnected() {
        assertEquals("Remoto (Redmi): última resposta há 2 min",
            MonitorNotificationContent.remoteLine("Redmi", 10_000L, 130_000L))
    }
    @Test fun unknownMeasurementsAreOmitted() {
        val unknown = MonitorNotificationContent.Battery(null, "", Double.NaN, null, null, null, 1L)
        assertEquals("Bateria · sem dados", MonitorNotificationContent.title(unknown))
        assertEquals("Estado indisponível", MonitorNotificationContent.batteryLine(unknown))
    }
}
