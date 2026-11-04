package br.dev.celularremoto.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Nunca confia em um broadcast externo sem conferir o ID persistido. */
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id <= 0 || id != UpdatePreferences.getLong(context, "download_id")) return
        UpdateVerifyJobService.enqueue(context.applicationContext)
    }
}
