package br.dev.celularremoto.updates

/** Regras puras do atualizador; sem dados de pareamento ou Android. */
internal object UpdateProgressPolicy {
    fun percent(downloaded: Long, total: Long): Int? =
        if (total <= 0L || downloaded < 0L) null
        else ((downloaded.coerceAtMost(total).toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)

    fun monitorRemote(stateCode: String): Boolean = stateCode in setOf("checking", "downloading", "queued", "verifying")

    fun cancellable(stateCode: String): Boolean = stateCode in setOf("downloading", "queued", "error")

    fun installPermissionLabel(ready: Boolean, allowed: Boolean): String = when {
        !ready -> "APK ainda não verificado"
        !allowed -> "Instalador requer autorização local"
        else -> "Instalação exige confirmação local"
    }
}
