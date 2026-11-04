package br.dev.celularremoto.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import org.json.JSONObject

object BatteryReader {
    private val counterTracker = BatteryCounterMeanTracker()
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
        // CURRENT_NOW/AVERAGE são µA. Um estado CHARGING pode coexistir com descarga
        // líquida quando o consumo excede a potência efetivamente recebida.
        val counterMean = counterTracker.observe(
            chargeCounter.takeIf { it in 0..100_000_000 }, status, SystemClock.elapsedRealtime())
        val currentReading = BatteryCurrentMath.select(current, average, counterMean, status)
        val selected = currentReading.ma
        val source = currentReading.source

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
            put("currentDiagnostic", currentReading.diagnostic)
            put("currentWarning", currentReading.warning ?: JSONObject.NULL)
            put("currentQuality", currentReading.quality)
            put("instantMa", currentReading.nowMa ?: JSONObject.NULL)
            put("counterMeanMa", counterMean ?: JSONObject.NULL)
            put("counterWindowMs", counterTracker.windowMs ?: JSONObject.NULL)
            put("averageMa", currentReading.averageMa ?: JSONObject.NULL)
            put("rawNowMicroA", if (currentReading.nowMa != null) current else JSONObject.NULL)
            put("rawAverageMicroA", if (currentReading.averageMa != null) average else JSONObject.NULL)
            put("chargeCounterMicroAh", if (chargeCounter in 0..100_000_000) chargeCounter else JSONObject.NULL)
            // Potência líquida no terminal da bateria, NÃO potência gerada pelo painel solar.
            put("batteryNetPowerW", BatteryCurrentMath.batteryPowerWatts(voltageMv, selected) ?: JSONObject.NULL)
            put("updatedMs", System.currentTimeMillis())
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            put("androidSdk", Build.VERSION.SDK_INT)
        }
    }
}
