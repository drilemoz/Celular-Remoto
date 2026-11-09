package br.dev.celularremoto.adb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdbAutoConnectPolicyTest {
    private fun endpoint(host: String, port: Int, type: String = "_adb-tls-connect._tcp.") =
        AdbDiscovery.Endpoint("service", type, host, port)

    @Test fun neverUsesForeignHostOrPairingPort() {
        val services = listOf(
            endpoint("192.168.1.10", 37123),
            endpoint("192.168.1.11", 41120),
            endpoint("192.168.1.10", 34115, "_adb-tls-pairing._tcp.")
        )
        assertEquals(listOf(37123), AdbAutoConnectPolicy.connectionServices("192.168.1.10", services).map { it.port })
    }

    @Test fun ambiguousPortsNeedManualChoice() {
        val services = listOf(endpoint("192.168.1.10", 37123), endpoint("192.168.1.10", 42111))
        assertEquals(2, AdbAutoConnectPolicy.connectionServices("192.168.1.10", services).size)
        assertTrue(AdbAutoConnectPolicy.connectionServices("192.168.1.12", services).isEmpty())
    }

    @Test fun rejectsLookalikeServicesAndInvalidPorts() {
        val host = "192.168.1.10"
        val services = listOf(
            endpoint(host, 41001, "not_adb-tls-connect._tcp."),
            endpoint(host, 41002, "_adb-tls-connect-fake._tcp."),
            endpoint(host, 0),
            endpoint(host, 65536),
            endpoint(host, 41003, "_adb-tls-connect._tcp."),
            endpoint(host, 41003, "_adb-tls-connect._tcp."),
            endpoint(host, 41004, "_ADB-TLS-CONNECT._TCP."),
            endpoint(host, 41006, "_adb-tls-connect._tcp.fake."),
            endpoint(host, 41007, "_adb-tls-connect._tcp.local."),
            endpoint("192.168.1.11", 41005)
        )
        assertEquals(listOf(41003, 41004, 41007),
            AdbAutoConnectPolicy.connectionServices(host, services).map { it.port })
    }
}
