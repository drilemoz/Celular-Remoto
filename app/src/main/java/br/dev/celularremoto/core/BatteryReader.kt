package br.dev.celularremoto.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import org.json.JSONObject

object BatteryReader {
    fun snapshot(context: Context): JSONObject {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = BatteryTelemetry.percentage(level, scale)
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temperatureTenth = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1000) ?: -1000
        val current = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val average = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeCounter = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        // Contrato Android: CURRENT_NOW/AVERAGE são µA, não corrente de entrada USB.
        // Alguns fabricantes apresentam escalas/sinais incoerentes: não corrigir sem evidência.
        fun readableCurrent(raw: Int): Double? = raw.toLong()
            .takeUnless { raw == Int.MIN_VALUE || raw == Int.MAX_VALUE ||
                kotlin.math.abs(it) > 30_000_000L }?.div(1000.0)
        val rawNowMa = readableCurrent(current)
        val rawAverageMa = readableCurrent(average)
        fun matchesState(value: Double): Boolean = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> value >= 0.0
            BatteryManager.BATTERY_STATUS_DISCHARGING -> value <= 0.0
            else -> true
        }
        val source = when {
            rawNowMa != null && rawNowMa != 0.0 && matchesState(rawNowMa) -> "NOW"
            rawAverageMa != null && rawAverageMa != 0.0 && matchesState(rawAverageMa) -> "AVERAGE"
            rawNowMa != null && matchesState(rawNowMa) -> "NOW"
            rawAverageMa != null && matchesState(rawAverageMa) -> "AVERAGE"
            else -> "NONE"
        }
        val selected = when (source) {
            "NOW" -> rawNowMa
            "AVERAGE" -> rawAverageMa
            else -> null
        }

        return JSONObject().apply {
            put("levelPercent", percent ?: JSONObject.NULL)
            put("state", BatteryTelemetry.state(status))
            put("statusCode", if (status in 1..5) status else JSONObject.NULL)
            put("powerSource", BatteryTelemetry.powerSource(plugged) ?: JSONObject.NULL)
            put("pluggedCode", if (plugged >= 0) plugged else JSONObject.NULL)
            put("chargingIndicator", BatteryTelemetry.showChargingIndicator(status, plugged))
            // Campos legados permanecem para consumidores existentes do snapshot.
            put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
            put("full", status == BatteryManager.BATTERY_STATUS_FULL)
            put("temperatureC", if (temperatureTenth > -1000) temperatureTenth / 10.0 else JSONObject.NULL)
            put("voltageMv", if (voltageMv > 0) voltageMv else JSONObject.NULL)
            put("currentMa", selected ?: JSONObject.NULL)
            put("currentSource", source)
            put("currentDiagnostic", if (source == "NONE") "Corrente não confirmada ou sinal incompatível" else
                "Corrente líquida; ${if (source == "NOW") "instantânea" else "média do hardware"}; µA → mA")
            put("averageMa", rawAverageMa ?: JSONObject.NULL)
            put("rawNowMicroA", if (rawNowMa != null) current else JSONObject.NULL)
            put("rawAverageMicroA", if (rawAverageMa != null) average else JSONObject.NULL)
            put("chargeCounterMicroAh", if (chargeCounter in 0..100_000_000) chargeCounter else JSONObject.NULL)
            // Potência líquida no terminal da bateria, NÃO potência gerada pelo painel solar.
            put("batteryNetPowerW", if (voltageMv > 0 && selected != null)
                voltageMv.toDouble() * selected / 1_000_000.0 else JSONObject.NULL)
            put("updatedMs", System.currentTimeMillis())
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            put("androidSdk", Build.VERSION.SDK_INT)
        }
    }
}
