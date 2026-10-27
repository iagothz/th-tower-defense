package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap

class SaveData(private val prefs: Preferences) {
    /** Best wave on [map]. The record from before maps existed ("bestWave") belongs to the default map. */
    fun bestWave(map: GameMap): Int {
        val legacy = if (map == GameMap.DEFAULT) prefs.getInteger("bestWave", 0) else 0
        return maxOf(legacy, prefs.getInteger("best.${map.id}", 0))
    }

    /** Saves [wave] as [map]'s record if it beats it; returns whether it did. */
    fun recordWave(map: GameMap, wave: Int): Boolean {
        if (wave <= bestWave(map)) return false
        prefs.putInteger("best.${map.id}", wave)
        prefs.flush()
        return true
    }

    var map: GameMap
        get() = prefs.getString("map", null)?.let { GameMap.byId(it) } ?: GameMap.DEFAULT
        set(value) {
            prefs.putString("map", value.id)
            prefs.flush()
        }

    var tutorialDone: Boolean
        get() = prefs.getBoolean("tutorialDone", false)
        set(value) {
            prefs.putBoolean("tutorialDone", value)
            prefs.flush()
        }

    var vibrationOn: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) {
            prefs.putBoolean("vibration", value)
            prefs.flush()
        }

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) {
            prefs.putBoolean("sound", value)
            prefs.flush()
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
