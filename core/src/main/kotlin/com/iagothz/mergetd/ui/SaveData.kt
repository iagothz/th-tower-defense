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

    /** The match left running when the app went to the background, with the record it started from. */
    val savedMatch: Pair<String, Int>?
        get() = prefs.getString("match", null)?.let { it to prefs.getInteger("matchBest", 0) }

    fun saveMatch(snapshot: String, previousBest: Int) {
        prefs.putString("match", snapshot)
        prefs.putInteger("matchBest", previousBest)
        prefs.flush()
    }

    fun clearMatch() {
        prefs.remove("match")
        prefs.remove("matchBest")
        prefs.flush()
    }
}
