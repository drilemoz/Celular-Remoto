package br.dev.celularremoto.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorUiRefreshPolicyTest {
    private val sample = MonitorUiRefreshPolicy.State(
        enabled = true, serviceRunning = true, serviceActive = true,
        screenSharing = false, cameraActive = false, photoPrepared = false,
        address = "192.168.1.10", recentClients = listOf("client-a")
    )

    @Test fun firstDisplayAlwaysRenders() {
        assertTrue(MonitorUiRefreshPolicy.needsRender(null, sample))
    }

    @Test fun identicalStateKeepsExistingViews() {
        assertFalse(MonitorUiRefreshPolicy.needsRender(sample, sample.copy()))
    }

    @Test fun sessionAndServiceTransitionsRefresh() {
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(screenSharing = true)))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(photoPrepared = true)))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(cameraActive = true)))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(serviceRunning = false)))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(enabled = false)))
    }

    @Test fun clientExpiryAndIpChangesRefresh() {
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(recentClients = emptyList())))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(recentClients = listOf("client-b"))))
        assertTrue(MonitorUiRefreshPolicy.needsRender(sample, sample.copy(address = "192.168.1.11")))
    }
}
