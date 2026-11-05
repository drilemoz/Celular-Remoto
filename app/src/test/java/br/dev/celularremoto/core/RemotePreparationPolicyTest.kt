package br.dev.celularremoto.core

import org.junit.Assert.*
import org.junit.Test

class RemotePreparationPolicyTest {
    @Test fun classificationDoesNotTreatSessionConsentAsGranted() {
        val states = listOf(
            RemotePreparationPolicy.Item("monitor", "Servidor", RemotePreparationPolicy.READY, "Ativo"),
            RemotePreparationPolicy.Item("screen", "Tela", RemotePreparationPolicy.SESSION, "Exige consentimento"),
            RemotePreparationPolicy.Item("updates", "Atualizações", RemotePreparationPolicy.OFF, "Não autorizado"),
            RemotePreparationPolicy.Item("installer", "Instalador", RemotePreparationPolicy.SETUP, "Pendente")
        )
        assertTrue(RemotePreparationPolicy.valid(states))
        assertEquals(1, RemotePreparationPolicy.readyCount(states))
        assertEquals(1, RemotePreparationPolicy.pendingCount(states))
        assertEquals("Por sessão", RemotePreparationPolicy.status(RemotePreparationPolicy.SESSION))
        assertEquals("Desativado", RemotePreparationPolicy.status(RemotePreparationPolicy.OFF))
    }

    @Test fun rejectDuplicateIdsAndUnknownStates() {
        val one = RemotePreparationPolicy.Item("test", "Teste", RemotePreparationPolicy.READY, "ok")
        assertFalse(RemotePreparationPolicy.valid(listOf(one, one)))
        assertFalse(RemotePreparationPolicy.valid(listOf(one.copy(state = "granted_forever"))))
    }
}
