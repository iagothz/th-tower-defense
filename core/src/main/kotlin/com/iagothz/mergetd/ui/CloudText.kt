package com.iagothz.mergetd.ui

import com.iagothz.mergetd.ui.CloudController.Status

/** The settings row for the cloud: one line of state and the button that fits it (null: no button). */
object CloudText {
    /** The Portuguese string-table key for [status] (with `%d` for the days line); empty when unavailable. */
    fun key(status: Status, today: Int): String = when (status) {
        Status.Unavailable -> ""
        Status.Off -> "Nuvem: desligada"
        Status.SignedOut -> "Nuvem: sem conta"
        Status.Working -> "Nuvem: sincronizando..."
        is Status.Synced -> if (today - status.day <= 0) "Nuvem: sincronizada hoje" else "Nuvem: sincronizada há %d dias"
        Status.Offline -> "Nuvem: sem conexão"
        is Status.Error -> "Nuvem: falhou"
    }

    fun line(status: Status, today: Int): String {
        val k = key(status, today)
        return if (status is Status.Synced && k.contains("%d")) S.f(k, today - status.day) else S[k]
    }

    fun button(status: Status): String? = when (status) {
        Status.Unavailable, Status.Working -> null
        Status.Off -> "Ligar"
        Status.SignedOut -> "Entrar"
        is Status.Synced, Status.Offline -> "Desligar"
        is Status.Error -> "Tentar"
    }
}
