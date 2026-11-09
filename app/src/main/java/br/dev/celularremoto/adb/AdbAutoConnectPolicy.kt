package br.dev.celularremoto.adb

/** Associação conservadora: o QR/código determina o IP; mDNS só fornece a porta TLS desse IP. */
object AdbAutoConnectPolicy {
    fun connectionServices(pairedHost: String, discovered: List<AdbDiscovery.Endpoint>): List<AdbDiscovery.Endpoint> =
        discovered.filter {
            // Nomes mDNS parecidos não são necessariamente o serviço TLS do ADB.
            val type = it.type.trimEnd('.').lowercase(java.util.Locale.ROOT)
            it.host == pairedHost &&
                (type == "_adb-tls-connect._tcp" || type == "_adb-tls-connect._tcp.local") &&
                it.port in 1..65535
        }.distinctBy { "${it.host}:${it.port}" }
}
