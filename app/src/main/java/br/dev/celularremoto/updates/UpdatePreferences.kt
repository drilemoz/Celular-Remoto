package br.dev.celularremoto.updates

import android.content.Context

/** Somente preferências não sensíveis; dados de pareamento não entram no atualizador. */
internal object UpdatePreferences {
    private const val NAME = "celular_remoto_updates_v1"
    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun checking(ctx: Context) = prefs(ctx).getBoolean("checking", true)
    fun autoDownload(ctx: Context) = prefs(ctx).getBoolean("auto_download", true)
    fun unmetered(ctx: Context) = prefs(ctx).getBoolean("unmetered", true)
    fun batteryCare(ctx: Context) = prefs(ctx).getBoolean("battery_care", true)
    fun intervalMinutes(ctx: Context): Int {
        val stored = prefs(ctx)
        // Migração sem alterar as preferências anteriores: 6/12/24 h continuam válidas.
        val old = stored.getInt("interval", 0).takeIf { it in listOf(6, 12, 24) }
        return stored.getInt("interval_minutes", old?.takeIf { it != 24 }?.times(60) ?: 60)
            .takeIf { it in listOf(30, 60, 180, 360, 720, 1440) } ?: 60
    }
    fun remoteRequests(ctx: Context): Boolean = prefs(ctx).getBoolean("remote_requests", false)
    fun update(ctx: Context, key: String, value: Boolean) = prefs(ctx).edit().putBoolean(key, value).apply()
    fun interval(ctx: Context, minutes: Int) { require(minutes in listOf(30, 60, 180, 360, 720, 1440)); prefs(ctx).edit().putInt("interval_minutes", minutes).apply() }
    fun getString(ctx: Context, key: String): String = prefs(ctx).getString(key, "").orEmpty()
    fun getLong(ctx: Context, key: String): Long = prefs(ctx).getLong(key, 0L)
    fun putString(ctx: Context, key: String, value: String) = prefs(ctx).edit().putString(key, value).apply()
    fun putLong(ctx: Context, key: String, value: Long) = prefs(ctx).edit().putLong(key, value).apply()
    fun clearAvailable(ctx: Context) = prefs(ctx).edit()
        .remove("available_version").remove("available_code")
        .remove("available_sha").remove("available_url").remove("available_name").apply()
    fun clearDownload(ctx: Context) = prefs(ctx).edit()
        .remove("download_id").remove("download_name").remove("download_code")
        .remove("download_hash").remove("download_version").apply()
}
