package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType

class SaveData(private val prefs: Preferences) {
    var bestWave: Int
        get() = prefs.getInteger("bestWave", 0)
        set(value) {
            prefs.putInteger("bestWave", value)
            prefs.flush()
        }

    var deck: List<DieType>
        get() = DeckCodec.decode(prefs.getString("deck", null))
        set(value) {
            prefs.putString("deck", DeckCodec.encode(value))
            prefs.flush()
        }
}
