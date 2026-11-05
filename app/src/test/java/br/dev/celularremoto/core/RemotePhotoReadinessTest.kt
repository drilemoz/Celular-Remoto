package br.dev.celularremoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePhotoReadinessTest {
    @Test fun readyCanCapture() {
        assertEquals(RemotePhotoReadiness.State.READY, RemotePhotoReadiness.state("ready"))
        assertTrue(RemotePhotoReadiness.canTryPhoto(RemotePhotoReadiness.State.READY))
    }
    @Test fun pendingSessionBlocksCaptureButNotStatusQuery() {
        assertFalse(RemotePhotoReadiness.canTryPhoto(RemotePhotoReadiness.state("session")))
        assertFalse(RemotePhotoReadiness.canTryPhoto(RemotePhotoReadiness.state("setup")))
    }
    @Test fun oldVersionDoesNotBlockAnAvailablePhoto() {
        assertEquals(RemotePhotoReadiness.State.UNKNOWN, RemotePhotoReadiness.state(null))
        assertTrue(RemotePhotoReadiness.canTryPhoto(RemotePhotoReadiness.state("future")))
    }
}
