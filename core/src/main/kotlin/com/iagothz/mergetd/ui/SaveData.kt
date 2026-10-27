package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.GameMap

/** Lifetime counters shown on the stats screen. */
class Totals(val games: Int, val kills: Int, val bosses: Int, val merges: Int, val waves: Int)

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

    /**
     * Books a finished match: lifetime totals always; the map and deck records for a normal match,
     * or the day's record for a daily challenge ([daily] is its day). Returns whether a record fell.
     */
    fun recordMatch(map: GameMap, deck: List<DieType>, wave: Int, kills: Int, bosses: Int, merges: Int, daily: Int? = null): Boolean {
        val t = totals
        prefs.putInteger("total.games", t.games + 1)
        prefs.putInteger("total.kills", t.kills + kills)
        prefs.putInteger("total.bosses", t.bosses + bosses)
        prefs.putInteger("total.merges", t.merges + merges)
        prefs.putInteger("total.waves", t.waves + wave)
        prefs.flush()
        if (daily != null) return recordDaily(daily, wave)
        val key = "deckbest.${DeckCodec.encode(deck)}"
        if (wave > prefs.getInteger(key, 0)) {
            prefs.putInteger(key, wave)
            prefs.flush()
        }
        return recordWave(map, wave)
    }

    val totals: Totals
        get() = Totals(
            prefs.getInteger("total.games", 0),
            prefs.getInteger("total.kills", 0),
            prefs.getInteger("total.bosses", 0),
            prefs.getInteger("total.merges", 0),
            prefs.getInteger("total.waves", 0),
        )

    /** Every deck ever played with its best wave, best first. */
    fun deckRecords(): List<Pair<List<DieType>, Int>> = entries("deckbest.")
        .map { (deck, wave) -> DeckCodec.decode(deck) to wave }
        .sortedByDescending { it.second }

    fun dailyBest(day: Int): Int = prefs.getInteger("daily.$day", 0)

    fun recordDaily(day: Int, wave: Int): Boolean {
        if (wave <= dailyBest(day)) return false
        prefs.putInteger("daily.$day", wave)
        prefs.flush()
        return true
    }

    /** (day, best wave) for every day played, newest first. */
    fun dailyHistory(): List<Pair<Int, Int>> = entries("daily.")
        .mapNotNull { (day, wave) -> day.toIntOrNull()?.let { it to wave } }
        .sortedByDescending { it.first }

    /** Desktop preferences hand values back as strings, Android as numbers; both parse. */
    private fun entries(prefix: String): List<Pair<String, Int>> = prefs.get().entries
        .filter { it.key.startsWith(prefix) }
        .mapNotNull { e -> e.value?.toString()?.toIntOrNull()?.let { e.key.removePrefix(prefix) to it } }

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

    var musicOn: Boolean
        get() = prefs.getBoolean("music", true)
        set(value) {
            prefs.putBoolean("music", value)
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
