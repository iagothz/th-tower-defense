package com.iagothz.mergetd.ui

import com.badlogic.gdx.Preferences
import com.iagothz.mergetd.logic.CardDrop
import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DiceUnlocks
import com.iagothz.mergetd.logic.DieLevels
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.GameMap
import com.iagothz.mergetd.logic.GameMode
import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionProgress
import com.iagothz.mergetd.logic.Mutator
import com.iagothz.mergetd.logic.Progression
import kotlin.random.Random

/** One finished match, as the history screen shows it. */
data class MatchRecord(
    val day: Int,
    val map: GameMap,
    val mode: GameMode,
    val deck: List<DieType>,
    val wave: Int,
    val killedBy: EnemyKind?,
    val code: String,
)

/** Lifetime counters shown on the stats screen. */
class Totals(val games: Int, val kills: Int, val bosses: Int, val merges: Int, val waves: Int)

class SaveData(private val prefs: Preferences) {
    companion object {
        const val HISTORY_SIZE = 20
        /** Most cards the one-time chest for matches played before cards existed can hold. */
        const val RETRO_CAP = 60
    }

    /** Best wave on [map] in [mode]. The record from before maps existed ("bestWave") belongs to the default map. */
    fun bestWave(map: GameMap, mode: GameMode = GameMode.NORMAL): Int {
        if (mode != GameMode.NORMAL) return prefs.getInteger("best.${map.id}.${mode.id}", 0)
        val legacy = if (map == GameMap.DEFAULT) prefs.getInteger("bestWave", 0) else 0
        return maxOf(legacy, prefs.getInteger("best.${map.id}", 0))
    }

    /** Saves [wave] as the record of [map] in [mode] if it beats it; returns whether it did. */
    fun recordWave(map: GameMap, wave: Int, mode: GameMode = GameMode.NORMAL): Boolean {
        if (wave <= bestWave(map, mode)) return false
        prefs.putInteger(if (mode == GameMode.NORMAL) "best.${map.id}" else "best.${map.id}.${mode.id}", wave)
        prefs.flush()
        return true
    }

    var mode: GameMode
        get() = prefs.getString("mode", null)?.let { GameMode.byId(it) } ?: GameMode.NORMAL
        set(value) {
            prefs.putString("mode", value.id)
            prefs.flush()
        }

    /**
     * Books a finished match: lifetime totals always; the map and deck records for a normal match,
     * or the day's record for a daily challenge ([daily] is its day). Returns whether a record fell.
     */
    fun recordMatch(map: GameMap, deck: List<DieType>, wave: Int, kills: Int, bosses: Int, merges: Int, daily: Int? = null, mode: GameMode = GameMode.NORMAL, locked: Boolean = false): Boolean {
        val t = totals
        prefs.putInteger("total.games", t.games + 1)
        prefs.putInteger("total.kills", t.kills + kills)
        prefs.putInteger("total.bosses", t.bosses + bosses)
        prefs.putInteger("total.merges", t.merges + merges)
        prefs.putInteger("total.waves", t.waves + wave)
        prefs.flush()
        if (daily != null) return recordDaily(daily, wave)
        // A match from a code had its map and deck dictated: it counts for the totals only.
        if (locked) return false
        val key = "deckbest.${DeckCodec.encode(deck)}"
        if (wave > prefs.getInteger(key, 0)) {
            prefs.putInteger(key, wave)
            prefs.flush()
        }
        return recordWave(map, wave, mode)
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

    // --- History ---

    fun history(): List<MatchRecord> = prefs.getString("history", "").lines().filter { it.isNotBlank() }.mapNotNull { line ->
        val f = line.split("|")
        if (f.size < 7) return@mapNotNull null
        MatchRecord(
            f[0].toIntOrNull() ?: return@mapNotNull null,
            GameMap.byId(f[1]) ?: return@mapNotNull null,
            GameMode.byId(f[2]) ?: return@mapNotNull null,
            DeckCodec.decode(f[3]),
            f[4].toIntOrNull() ?: return@mapNotNull null,
            f[5].takeIf { it.isNotEmpty() }?.let { k -> EnemyKind.entries.firstOrNull { it.name == k } },
            f[6],
        )
    }

    /** Prepends [r]; only the last [HISTORY_SIZE] stay. */
    fun addHistory(r: MatchRecord) {
        val line = listOf(r.day, r.map.id, r.mode.id, DeckCodec.encode(r.deck), r.wave, r.killedBy?.name ?: "", r.code).joinToString("|")
        val lines = (listOf(line) + prefs.getString("history", "").lines().filter { it.isNotBlank() }).take(HISTORY_SIZE)
        prefs.putString("history", lines.joinToString("\n"))
        prefs.flush()
    }

    // --- Achievements and cosmetics ---

    fun hasAchievement(a: Achievement): Boolean = prefs.getBoolean("ach.${a.id}", false)

    /** Marks [earned] as done; returns the ones that weren't before. */
    fun unlock(earned: List<Achievement>): Set<Achievement> {
        val fresh = earned.filter { !hasAchievement(it) }.toSet()
        for (a in fresh) prefs.putBoolean("ach.${a.id}", true)
        if (fresh.isNotEmpty()) prefs.flush()
        return fresh
    }

    fun hasCosmetic(c: Cosmetic): Boolean = c.unlockedByDefault || Achievement.entries.any { it.unlocks == c && hasAchievement(it) }

    fun equipped(slot: Cosmetic.Slot): Cosmetic =
        prefs.getString("cosmetic.${slot.name.lowercase()}", null)?.let { Cosmetic.byId(it) }?.takeIf { hasCosmetic(it) }
            ?: Cosmetic.default(slot)

    fun equip(c: Cosmetic) {
        if (!hasCosmetic(c)) return
        prefs.putString("cosmetic.${c.slot.name.lowercase()}", c.id)
        prefs.flush()
    }

    /** How many maps have a normal-mode record of at least [wave]. */
    fun mapsAt(wave: Int): Int = GameMap.entries.count { bestWave(it) >= wave }

    // --- Progression: XP and the day's missions ---

    var xp: Int
        get() = prefs.getInteger("xp", 0)
        set(value) {
            prefs.putInteger("xp", value)
            prefs.flush()
        }

    /** Progress on [missions] for [day], one number per mission, 0 when nothing was done yet. */
    fun missionProgress(day: Int, missions: List<Mission>): List<Int> {
        val parts = prefs.getString("missions.$day", "").split("|").mapNotNull { it.toIntOrNull() }
        return missions.indices.map { parts.getOrElse(it) { 0 } }
    }

    /**
     * Adds a match's [progress] to the day's missions: counters add up, wave goals keep their best, both capped at the
     * target. Missions that reach the target now pay their XP; returns them.
     */
    fun applyMissions(day: Int, missions: List<Mission>, progress: List<MissionProgress>): List<Mission> {
        val before = missionProgress(day, missions)
        val after = missions.mapIndexed { i, m ->
            val p = progress.getOrElse(i) { MissionProgress() }
            minOf(m.target, if (m.counts) before[i] + p.delta else maxOf(before[i], p.absolute))
        }
        val done = missions.filterIndexed { i, m -> before[i] < m.target && after[i] >= m.target }
        prefs.putString("missions.$day", after.joinToString("|"))
        if (done.isNotEmpty()) prefs.putInteger("xp", xp + done.sumOf { it.xp })
        prefs.flush()
        return done
    }

    val accountLevel: Int get() = Progression.level(xp)

    // --- Dice: unlocks by account level, levels paid with cards ---

    fun isUnlocked(t: DieType): Boolean = DiceUnlocks.isUnlocked(t, accountLevel)

    fun unlockedDice(): List<DieType> = DiceUnlocks.unlocked(accountLevel)

    /** The saved deck with any die the account hasn't opened swapped out (old saves, or a level that never was). */
    fun playableDeck(): List<DieType> = DiceUnlocks.sanitize(deck, accountLevel)

    fun cards(t: DieType): Int = prefs.getInteger("cards.${t.id}", 0)

    fun addCards(drop: Map<DieType, Int>) {
        for ((t, n) in drop) prefs.putInteger("cards.${t.id}", cards(t) + n)
        if (drop.isNotEmpty()) prefs.flush()
    }

    fun dieLevel(t: DieType): Int = prefs.getInteger("dielevel.${t.id}", 1).coerceIn(1, DieLevels.MAX)

    /** Only the dice above level 1, as [com.iagothz.mergetd.logic.Match] takes them. */
    fun dieLevels(): Map<DieType, Int> = DieType.entries.filter { dieLevel(it) > 1 }.associateWith { dieLevel(it) }

    fun canLevelUp(t: DieType): Boolean = DieLevels.cardsToNext(dieLevel(t))?.let { cards(t) >= it } ?: false

    fun canLevelUpAny(): Boolean = DieType.entries.any { canLevelUp(it) }

    /** Spends the cards for the next level of [t]; false when they aren't there or it is maxed. */
    fun levelUp(t: DieType): Boolean {
        val cost = DieLevels.cardsToNext(dieLevel(t)) ?: return false
        if (cards(t) < cost) return false
        prefs.putInteger("cards.${t.id}", cards(t) - cost)
        prefs.putInteger("dielevel.${t.id}", dieLevel(t) + 1)
        prefs.flush()
        return true
    }

    /** Dice a chest can hold cards for: opened and not yet at the top. */
    fun cardCandidates(): List<DieType> = unlockedDice().filter { dieLevel(it) < DieLevels.MAX }

    /**
     * The chest for the matches played before cards existed, paid once per save from the lifetime waves and bosses
     * (capped at [RETRO_CAP]); empty afterwards, and for a save with no matches.
     */
    fun retroCards(rng: Random): Map<DieType, Int> {
        if (prefs.getBoolean("cards.retro", false)) return emptyMap()
        prefs.putBoolean("cards.retro", true)
        prefs.flush()
        val t = totals
        if (t.games == 0) return emptyMap()
        val drop = CardDrop.draw(cardCandidates(), minOf(RETRO_CAP, CardDrop.count(t.waves, t.bosses)), rng)
        addCards(drop)
        return drop
    }

    var map: GameMap
        get() = prefs.getString("map", null)?.let { GameMap.byId(it) } ?: GameMap.DEFAULT
        set(value) {
            prefs.putString("map", value.id)
            prefs.flush()
        }

    /** Keys of the one-time hints already shown (see [Hints]). */
    var hints: Set<String>
        get() = prefs.getString("hints", "").split(",").filter { it.isNotBlank() }.toSet()
        set(value) {
            prefs.putString("hints", value.joinToString(","))
            prefs.flush()
        }

    /** Accessibility: no screen shake, vignettes, slow motion or camera zoom. */
    var reduceMotion: Boolean
        get() = prefs.getBoolean("reduceMotion", false)
        set(value) {
            prefs.putBoolean("reduceMotion", value)
            prefs.flush()
        }

    /** Accessibility: every font 15% bigger. */
    var largeText: Boolean
        get() = prefs.getBoolean("largeText", false)
        set(value) {
            prefs.putBoolean("largeText", value)
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

    /** 0..1 multipliers on the effect and music volumes. */
    var soundVolume: Float
        get() = prefs.getFloat("soundVolume", 1f)
        set(value) {
            prefs.putFloat("soundVolume", value.coerceIn(0f, 1f))
            prefs.flush()
        }

    var musicVolume: Float
        get() = prefs.getFloat("musicVolume", 1f)
        set(value) {
            prefs.putFloat("musicVolume", value.coerceIn(0f, 1f))
            prefs.flush()
        }

    var language: Language
        get() = prefs.getString("language", null)?.let { Language.byId(it) } ?: Language.AUTO
        set(value) {
            prefs.putString("language", value.id)
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

    /** Mutators chosen on the match screen for ordinary matches. */
    var mutators: Set<Mutator>
        get() = prefs.getString("mutators", "").split(",").mapNotNull { Mutator.byId(it) }.toSet()
        set(value) {
            prefs.putString("mutators", value.joinToString(",") { it.id })
            prefs.flush()
        }

    /** Best mutator score of any ordinary match. */
    var bestScore: Int
        get() = prefs.getInteger("score.best", 0)
        set(value) {
            prefs.putInteger("score.best", value)
            prefs.flush()
        }

    fun weeklyBest(week: Int): Int = prefs.getInteger("weekly.$week", 0)

    /** Books the week's score; true when it beat the record. */
    fun recordWeekly(week: Int, score: Int): Boolean {
        if (score <= weeklyBest(week)) return false
        prefs.putInteger("weekly.$week", score)
        prefs.flush()
        return true
    }

    /** One of the three saved deck presets, or null when the slot is empty. */
    fun deckSlot(i: Int): List<DieType>? = prefs.getString("deckslot.$i", null)?.let { DeckCodec.decode(it) }?.takeIf { it.size == DeckCodec.SIZE }

    fun saveDeckSlot(i: Int, deck: List<DieType>) {
        prefs.putString("deckslot.$i", DeckCodec.encode(deck))
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
