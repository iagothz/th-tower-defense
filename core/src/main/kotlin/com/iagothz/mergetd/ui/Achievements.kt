package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.iagothz.mergetd.logic.GameMode

/** A purely visual reward: a board frame colour, a sky tint or a pip shape. */
enum class Cosmetic(val slot: Slot, val title: String, val unlockedByDefault: Boolean = false) {
    FRAME_DEFAULT(Slot.FRAME, "Padrão", unlockedByDefault = true),
    FRAME_BRONZE(Slot.FRAME, "Bronze"),
    FRAME_SILVER(Slot.FRAME, "Prata"),
    FRAME_GOLD(Slot.FRAME, "Ouro"),
    FRAME_DIAMOND(Slot.FRAME, "Diamante"),
    FRAME_EMERALD(Slot.FRAME, "Esmeralda"),
    FRAME_RUBY(Slot.FRAME, "Rubi"),
    SKY_DEFAULT(Slot.SKY, "Padrão", unlockedByDefault = true),
    SKY_DUSK(Slot.SKY, "Entardecer"),
    SKY_AURORA(Slot.SKY, "Aurora"),
    SKY_NEBULA(Slot.SKY, "Nebulosa"),
    SKY_GOLDEN(Slot.SKY, "Noite dourada"),
    PIPS_ROUND(Slot.PIPS, "Redondos", unlockedByDefault = true),
    PIPS_SQUARE(Slot.PIPS, "Quadrados"),
    PIPS_DIAMOND(Slot.PIPS, "Losangos"),
    PIPS_STAR(Slot.PIPS, "Estrelas"),
    ;

    enum class Slot(val title: String) { FRAME("Moldura"), SKY("Céu"), PIPS("Pips") }

    val id: String get() = name.lowercase()

    /** Frame colour, or the tint multiplied into the sky; pips have no colour of their own. */
    val color: Color
        get() = when (this) {
            FRAME_BRONZE -> Color(0.55f, 0.36f, 0.22f, 1f)
            FRAME_SILVER -> Color(0.70f, 0.72f, 0.78f, 1f)
            FRAME_GOLD -> Color(0.85f, 0.68f, 0.22f, 1f)
            FRAME_DIAMOND -> Color(0.60f, 0.85f, 0.95f, 1f)
            FRAME_EMERALD -> Color(0.25f, 0.65f, 0.40f, 1f)
            FRAME_RUBY -> Color(0.75f, 0.20f, 0.30f, 1f)
            SKY_DUSK -> Color(1.6f, 1.0f, 0.9f, 1f)
            SKY_AURORA -> Color(0.9f, 1.5f, 1.3f, 1f)
            SKY_NEBULA -> Color(1.4f, 0.9f, 1.6f, 1f)
            SKY_GOLDEN -> Color(1.5f, 1.3f, 0.8f, 1f)
            else -> Color.WHITE
        }

    companion object {
        fun byId(id: String): Cosmetic? = entries.firstOrNull { it.id == id }
        fun default(slot: Slot): Cosmetic = entries.first { it.slot == slot && it.unlockedByDefault }
    }
}

enum class Achievement(val id: String, val title: String, val description: String, val unlocks: Cosmetic? = null) {
    FIRST_BLOOD("first_blood", "Primeiro sangue", "Derrote um inimigo", Cosmetic.FRAME_BRONZE),
    WAVE_10("wave10", "Dez ondas", "Chegue à onda 10", Cosmetic.SKY_DUSK),
    WAVE_20("wave20", "Vinte ondas", "Chegue à onda 20", Cosmetic.FRAME_SILVER),
    WAVE_30("wave30", "Trinta ondas", "Chegue à onda 30", Cosmetic.FRAME_GOLD),
    WAVE_40("wave40", "Quarenta ondas", "Chegue à onda 40", Cosmetic.FRAME_DIAMOND),
    BOSS_1("boss1", "Caçador de chefes", "Derrote um chefe", Cosmetic.PIPS_SQUARE),
    BOSSES_10("bosses10", "Dez chefes", "Derrote 10 chefes no total", Cosmetic.SKY_AURORA),
    KILLS_1000("kills1000", "Mil abates", "Derrote 1000 inimigos no total", Cosmetic.SKY_NEBULA),
    SEVEN_PIPS("seven", "Sete pips", "Tenha um dado de 7 pips", Cosmetic.PIPS_DIAMOND),
    SYNERGY("synergy", "Combinação", "Ative uma sinergia", Cosmetic.FRAME_EMERALD),
    HUNTER_50("hunter50", "Caçador 50", "Tenha um Caçador com 50 stacks", Cosmetic.FRAME_RUBY),
    DAILY("daily", "Desafiante", "Jogue um desafio do dia", Cosmetic.SKY_GOLDEN),
    BOSS_RUSH_5("bossrush5", "Cinco chefes seguidos", "Chegue à onda 5 no Boss Rush"),
    SURVIVOR("survivor", "Sobrevivente", "Chegue à onda 15 em Sobrevivência"),
    SPEEDSTER("speedster", "Veloz", "Chegue à onda 15 no Rápido"),
    EXPLORER("explorer", "Explorador", "Onda 10 em todos os mapas", Cosmetic.PIPS_STAR),
    UNDO("undo", "Arrependido", "Use o Desfazer"),
    RICH("rich", "Economista", "Acumule 1000 SP numa partida"),
    VETERAN("veteran", "Veterano", "Jogue 10 partidas"),
    MERGES_100("merges100", "Fundidor", "Faça 100 merges no total"),
    ;

    companion object {
        fun byId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

/** Everything the badges look at: this match's result and flags plus lifetime totals after it. */
data class AchievementProgress(
    val wave: Int,
    val mode: GameMode,
    val kills: Int,
    val bosses: Int,
    val merges: Int,
    val games: Int,
    val maxPips: Int,
    val synergy: Boolean,
    val hunterStacks: Int,
    val daily: Boolean,
    val undo: Boolean,
    val rich: Boolean,
    val mapsAtTen: Int,
)

object Achievements {
    /** Every badge [p] satisfies; the save decides which are new. */
    fun earned(p: AchievementProgress): List<Achievement> = Achievement.entries.filter { a ->
        when (a) {
            Achievement.FIRST_BLOOD -> p.kills >= 1
            Achievement.WAVE_10 -> p.mode == GameMode.NORMAL && p.wave >= 10
            Achievement.WAVE_20 -> p.mode == GameMode.NORMAL && p.wave >= 20
            Achievement.WAVE_30 -> p.mode == GameMode.NORMAL && p.wave >= 30
            Achievement.WAVE_40 -> p.mode == GameMode.NORMAL && p.wave >= 40
            Achievement.BOSS_1 -> p.bosses >= 1
            Achievement.BOSSES_10 -> p.bosses >= 10
            Achievement.KILLS_1000 -> p.kills >= 1000
            Achievement.SEVEN_PIPS -> p.maxPips >= 7
            Achievement.SYNERGY -> p.synergy
            Achievement.HUNTER_50 -> p.hunterStacks >= 50
            Achievement.DAILY -> p.daily
            Achievement.BOSS_RUSH_5 -> p.mode == GameMode.BOSS_RUSH && p.wave >= 5
            Achievement.SURVIVOR -> p.mode == GameMode.SURVIVAL && p.wave >= 15
            Achievement.SPEEDSTER -> p.mode == GameMode.RUSH && p.wave >= 15
            Achievement.EXPLORER -> p.mapsAtTen >= 4
            Achievement.UNDO -> p.undo
            Achievement.RICH -> p.rich
            Achievement.VETERAN -> p.games >= 10
            Achievement.MERGES_100 -> p.merges >= 100
        }
    }
}

/** The cosmetics in use, read by the renderers; set from the save at start and whenever one is equipped. */
object Cosmetics {
    var frame: Cosmetic = Cosmetic.FRAME_DEFAULT
    var sky: Cosmetic = Cosmetic.SKY_DEFAULT
    var pips: Cosmetic = Cosmetic.PIPS_ROUND

    fun load(save: SaveData) {
        frame = save.equipped(Cosmetic.Slot.FRAME)
        sky = save.equipped(Cosmetic.Slot.SKY)
        pips = save.equipped(Cosmetic.Slot.PIPS)
    }

    /** Board rim colour: the frame cosmetic, or the default rim. */
    val frameColor: Color get() = if (frame == Cosmetic.FRAME_DEFAULT) Skin.BOARD_RIM else frame.color
}
