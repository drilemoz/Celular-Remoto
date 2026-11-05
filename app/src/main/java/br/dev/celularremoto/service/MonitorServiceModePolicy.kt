package br.dev.celularremoto.service

/** Modos exclusivos de um único serviço: servidor remoto tem precedência para a coleta. */
object MonitorServiceModePolicy {
    enum class Mode { OFF, BATTERY_ONLY, REMOTE }

    fun resolve(remoteAuthorized: Boolean, localBatteryAuthorized: Boolean): Mode = when {
        remoteAuthorized -> Mode.REMOTE
        localBatteryAuthorized -> Mode.BATTERY_ONLY
        else -> Mode.OFF
    }

    /** Mesmo quando dois usos são autorizados existe uma só agenda de 30 s. */
    fun shouldCollect(mode: Mode): Boolean = mode != Mode.OFF
}
