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

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) {
            prefs.putBoolean("sound", value)
            prefs.flush()
        }

    /** Saves [wave] if it beats the best; returns whether it did. */
    fun recordWave(wave: Int): Boolean {
        if (wave <= bestWave) return false
        bestWave = wave
        return true
    }

    var deck: List<DieType>
        get() = DeckCodec.decode(prefs.getString("deck", null))
        set(value) {
            prefs.putString("deck", DeckCodec.encode(value))
            prefs.flush()
        }
}
