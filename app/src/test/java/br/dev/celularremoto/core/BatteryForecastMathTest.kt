package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class BatteryForecastMathTest {
    private val hour = 3_600_000L
    private fun points(start: Int = 20): List<BatteryForecastMath.Point> = (0..20).map {
        BatteryForecastMath.Point(2_000_000L + it * 60_000L, 10_000_000L + it * 60_000L, start + it / 5)
    }
    private fun session(mode: String, source: String, end: Long, start: Int, finish: Int) =
        BatteryForecastMath.Session(mode, source, end - hour, end, hour, start, finish, 20)

    @Test fun liveForecastUsesObservedIntervals() {
        assertEquals(12.0, BatteryForecastMath.liveRate("CHARGE", points())!!, 0.001)
        val result = BatteryForecastMath.predict("CHARGE", 24, 11_200_000, 11_200_000, points(), "USB", emptyList())!!
        assertEquals(380L, result.minutes)
        assertEquals("TENDENCIA_ATUAL", result.evidence)
        assertEquals(280L, result.to80)
        assertEquals(330L, result.to90)
    }

    @Test fun discontinuitiesAndInsufficientGainAreNotInterpolated() {
        val interrupted = points().mapIndexed { i, p ->
            if (i >= 15) p.copy(elapsedMs = p.elapsedMs + 8 * 60_000L, wallMs = p.wallMs + 8 * 60_000L) else p
        }
        assertNull(BatteryForecastMath.liveRate("CHARGE", interrupted))
        assertNull(BatteryForecastMath.liveRate("CHARGE", points(20).map { it.copy(percent = 20) }))
    }

    @Test fun historyRequiresSameChargeSourceAndMultipleSessions() {
        val now = 20 * hour
        val history = listOf(session("CHARGE", "USB", now - hour, 20, 30),
            session("CHARGE", "USB", now - 2 * hour, 40, 50),
            session("CHARGE", "Tomada", now - 3 * hour, 10, 70))
        val rate = BatteryForecastMath.historyRate("CHARGE", "USB", history, now)!!
        assertEquals(10.0, rate.first, 0.001)
        assertEquals(2, rate.second)
        assertNull(BatteryForecastMath.historyRate("CHARGE", null, history, now))
        assertNull(BatteryForecastMath.historyRate("CHARGE", "Tomada", history, now))
    }

    @Test fun staleOrContradictoryPredictionIsNotShown() {
        val history = emptyList<BatteryForecastMath.Session>()
        assertNull(BatteryForecastMath.predict("CHARGE", 24, 11_200_000, 11_500_001, points(), "USB", history))
        assertNull(BatteryForecastMath.predict("DISCHARGE", 24, 11_200_000, 11_200_000, points(), null, history))
    }
}
