package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode

/** The sentence the game over screen hands to the share sheet (or the clipboard on desktop). */
object Share {
    fun text(wave: Int, map: GameMap, mode: GameMode, code: String?): String {
        val line = S.f("Cheguei à onda %d no mapa %s (%s) no Merge TD.", wave, S[map.title], S[mode.title])
        return if (code == null) line else line + " " + S.f("Código da partida: %s", code)
    }
}
