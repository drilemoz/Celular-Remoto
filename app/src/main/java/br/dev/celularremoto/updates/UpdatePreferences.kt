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
    fun intervalHours(ctx: Context) = prefs(ctx).getInt("interval", 24).takeIf { it in listOf(6, 12, 24) } ?: 24
    fun update(ctx: Context, key: String, value: Boolean) = prefs(ctx).edit().putBoolean(key, value).apply()
    fun interval(ctx: Context, hours: Int) { require(hours in listOf(6, 12, 24)); prefs(ctx).edit().putInt("interval", hours).apply() }
    fun getString(ctx: Context, key: String): String = prefs(ctx).getString(key, "").orEmpty()
    fun getLong(ctx: Context, key: String): Long = prefs(ctx).getLong(key, 0L)
    fun putString(ctx: Context, key: String, value: String) = prefs(ctx).edit().putString(key, value).apply()
    fun putLong(ctx: Context, key: String, value: Long) = prefs(ctx).edit().putLong(key, value).apply()
    fun clearDownload(ctx: Context) = prefs(ctx).edit()
        .remove("download_id").remove("download_name").remove("download_code")
        .remove("download_hash").remove("download_version").apply()
}
