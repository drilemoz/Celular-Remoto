package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorNotificationContentTest {
    private val battery = MonitorNotificationContent.Battery(32, "Carregando", 39.9, -658.0,
        152L, 12.0, 100_000L)

    @Test fun batteryAndTemperatureAreVisible() {
        assertEquals("Celular Remoto · 32% · 39,9 °C", MonitorNotificationContent.title(battery))
    }

    @Test fun negativeCurrentStaysNegativeAndForecastIsClearlyEstimate() {
        assertEquals("Este aparelho: 32% · temp. bateria 39,9 °C · Carregando · -658 mA líquidos · 100% em ~2h 32min (estim.)",
            MonitorNotificationContent.batteryLine(battery))
    }

    @Test fun staleBatteryIsNotPresentedAsCurrent() {
        assertEquals(null, MonitorNotificationContent.valid(battery, 300_001L))
        assertEquals(battery, MonitorNotificationContent.valid(battery, 150_000L))
    }

    @Test fun remoteLastResponseIsNotCalledConnected() {
        assertEquals("Remoto (Redmi): última resposta há 2 min",
            MonitorNotificationContent.remoteLine("Redmi", 10_000L, 130_000L))
        assertEquals("Remoto (Redmi): sem resposta registrada",
            MonitorNotificationContent.remoteLine("Redmi", 0L, 130_000L))
    }

    @Test fun invalidUnknownMeasurementsAreOmitted() {
        val unknown = MonitorNotificationContent.Battery(null, "", Double.NaN, null, null, null, 1L)
        assertEquals("Celular Remoto · monitoramento", MonitorNotificationContent.title(unknown))
        assertEquals("Este aparelho: Estado desconhecido", MonitorNotificationContent.batteryLine(unknown))
    }

    @Test fun dischargeForecastNotLabeledAsCharge() {
        assertEquals("Este aparelho: 32% · temp. bateria 39,9 °C · Descarregando · autonomia ~1h 5min (estim.)",
            MonitorNotificationContent.batteryLine(battery.copy(state="Descarregando", currentMa=null, forecastMinutes=65)))
    }
}
