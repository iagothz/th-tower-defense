package com.iagothz.mergetd.ui

import com.iagothz.mergetd.ui.CloudController.Status.Error
import com.iagothz.mergetd.ui.CloudController.Status.Off
import com.iagothz.mergetd.ui.CloudController.Status.Offline
import com.iagothz.mergetd.ui.CloudController.Status.SignedOut
import com.iagothz.mergetd.ui.CloudController.Status.Synced
import com.iagothz.mergetd.ui.CloudController.Status.Unavailable
import com.iagothz.mergetd.ui.CloudController.Status.Working
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudTextTest {
    @Test
    fun eachStatusHasALineAndTheRightButton() {
        assertEquals("" to null, CloudText.line(Unavailable, 10) to CloudText.button(Unavailable))
        assertEquals("Nuvem: desligada" to "Ligar", CloudText.line(Off, 10) to CloudText.button(Off))
        assertEquals("Nuvem: sem conta" to "Entrar", CloudText.line(SignedOut, 10) to CloudText.button(SignedOut))
        assertEquals("Nuvem: sincronizando..." to null, CloudText.line(Working, 10) to CloudText.button(Working))
        assertEquals("Nuvem: sincronizada hoje" to "Desligar", CloudText.line(Synced(10), 10) to CloudText.button(Synced(10)))
        assertEquals("Nuvem: sincronizada há 3 dias", CloudText.line(Synced(7), 10))
        assertEquals("Nuvem: sem conexão" to "Desligar", CloudText.line(Offline, 10) to CloudText.button(Offline))
        assertEquals("Nuvem: falhou" to "Tentar", CloudText.line(Error("x"), 10) to CloudText.button(Error("x")))
    }

    @Test
    fun everyTextIsTranslated() {
        for (s in listOf(Off, SignedOut, Working, Synced(1), Synced(0), Offline, Error("x"))) {
            assertTrue(S.has(Language.EN, CloudText.key(s, 1)), CloudText.key(s, 1))
            CloudText.button(s)?.let { assertTrue(S.has(Language.EN, it), it) }
        }
    }
}
