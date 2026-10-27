package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.iagothz.mergetd.logic.DieType
import com.iagothz.mergetd.logic.EnemyKind

/** All per-type visuals live here so shapes can later be swapped for sprites. */
object Skin {
    val BACKGROUND = Color(0.08f, 0.09f, 0.12f, 1f)
    val PANEL = Color(0.16f, 0.17f, 0.22f, 1f)
    /** Empty board cell; a touch lighter than the panel it sits on. */
    val CELL = Color(0.20f, 0.21f, 0.27f, 1f)
    val PATH = Color(0.24f, 0.21f, 0.17f, 1f)
    val PATH_EDGE = Color(0.16f, 0.13f, 0.10f, 1f)
    val SHADOW = Color(0f, 0f, 0f, 0.35f)
    val GOLD = Color(0.98f, 0.80f, 0.25f, 1f)
    val HEART = Color(0.92f, 0.28f, 0.36f, 1f)

    /** Hues chosen so no two dice are closer than 0.2 in RGB (see SkinTest); glyphs tell the rest apart. */
    private val dieColors = mapOf(
        DieType.FIRE to Color(0.92f, 0.30f, 0.22f, 1f),
        DieType.ICE to Color(0.55f, 0.82f, 0.98f, 1f),
        DieType.POISON to Color(0.36f, 0.76f, 0.30f, 1f),
        DieType.ELECTRIC to Color(1.00f, 0.90f, 0.25f, 1f),
        DieType.WIND to Color(0.22f, 0.78f, 0.78f, 1f),
        DieType.IRON to Color(0.62f, 0.64f, 0.70f, 1f),
        DieType.MINE to Color(0.80f, 0.66f, 0.20f, 1f),
        DieType.SNIPER to Color(0.58f, 0.36f, 0.88f, 1f),
        DieType.MIRROR to Color(0.93f, 0.93f, 0.96f, 1f),
        DieType.GROWTH to Color(0.98f, 0.62f, 0.70f, 1f),
        DieType.LIGHT to Color(1.00f, 0.95f, 0.65f, 1f),
        DieType.CRIT to Color(1.00f, 0.45f, 0.00f, 1f),
        DieType.COMBO to Color(0.25f, 0.50f, 0.95f, 1f),
        DieType.CURSE to Color(0.50f, 0.20f, 0.62f, 1f),
        DieType.HUNTER to Color(0.55f, 0.36f, 0.22f, 1f),
        DieType.GRAVITY to Color(0.28f, 0.36f, 0.70f, 1f),
        DieType.TIME to Color(0.55f, 0.95f, 0.72f, 1f),
        DieType.TRAP to Color(0.48f, 0.56f, 0.20f, 1f),
        DieType.QUAKE to Color(0.72f, 0.50f, 0.40f, 1f),
        DieType.REAPER to Color(0.42f, 0.44f, 0.52f, 1f),
        DieType.NOVA to Color(0.95f, 0.40f, 0.85f, 1f),
        DieType.LUCK to Color(0.60f, 0.88f, 0.28f, 1f),
        DieType.VAMPIRE to Color(0.62f, 0.10f, 0.22f, 1f),
    )

    /** Dice bright enough that white pips would vanish on them. */
    private val lightDice = setOf(
        DieType.ICE, DieType.ELECTRIC, DieType.MIRROR, DieType.LIGHT, DieType.TIME,
        DieType.LUCK, DieType.GROWTH, DieType.MINE,
    )

    private val labels = mapOf(
        DieType.FIRE to "Fogo",
        DieType.ICE to "Gelo",
        DieType.POISON to "Veneno",
        DieType.ELECTRIC to "Elétrico",
        DieType.WIND to "Vento",
        DieType.IRON to "Ferro",
        DieType.MINE to "Mina",
        DieType.SNIPER to "Sniper",
        DieType.MIRROR to "Espelho",
        DieType.GROWTH to "Cresc.",
        DieType.LIGHT to "Luz",
        DieType.CRIT to "Crítico",
        DieType.COMBO to "Combo",
        DieType.CURSE to "Maldição",
        DieType.HUNTER to "Caçador",
        DieType.GRAVITY to "Gravid.",
        DieType.TIME to "Tempo",
        DieType.TRAP to "Armad.",
        DieType.QUAKE to "Terrem.",
        DieType.REAPER to "Ceifador",
        DieType.NOVA to "Nova",
        DieType.LUCK to "Sorte",
        DieType.VAMPIRE to "Vampiro",
    )

    private val fullNames = mapOf(
        DieType.GROWTH to "Crescimento",
        DieType.GRAVITY to "Gravidade",
        DieType.TRAP to "Armadilha",
        DieType.QUAKE to "Terremoto",
        DieType.NOVA to "Supernova",
    )

    private val enemyColors = mapOf(
        EnemyKind.NORMAL to Color(0.85f, 0.85f, 0.85f, 1f),
        EnemyKind.FAST to Color(0.95f, 0.60f, 0.30f, 1f),
        EnemyKind.TANK to Color(0.55f, 0.45f, 0.40f, 1f),
        EnemyKind.BOSS_SILENCER to Color(0.45f, 0.32f, 0.68f, 1f),
        EnemyKind.BOSS_SWAPPER to Color(0.85f, 0.28f, 0.48f, 1f),
        EnemyKind.BOSS_REGEN to Color(0.32f, 0.66f, 0.38f, 1f),
    )

    private val enemyNames = mapOf(
        EnemyKind.NORMAL to "Soldado",
        EnemyKind.FAST to "Corredor",
        EnemyKind.TANK to "Tanque",
        EnemyKind.BOSS_SILENCER to "Silenciador",
        EnemyKind.BOSS_SWAPPER to "Trocador",
        EnemyKind.BOSS_REGEN to "Regenerador",
    )

    fun color(t: DieType): Color = dieColors.getValue(t)

    fun pipColor(t: DieType): Color = if (t in lightDice) Color(0.15f, 0.15f, 0.2f, 1f) else Color.WHITE

    /** Glyph colour: the die's own colour pushed toward its pip colour, so it reads as a watermark. */
    fun glyphColor(t: DieType, strength: Float = 0.45f): Color = Color(color(t)).lerp(pipColor(t), strength)

    fun color(k: EnemyKind): Color = enemyColors.getValue(k)

    fun label(t: DieType): String = labels.getValue(t)

    /** Unabbreviated name, for places with room for it. */
    fun fullName(t: DieType): String = fullNames[t] ?: label(t)

    fun enemyName(k: EnemyKind): String = enemyNames.getValue(k)

    fun enemyRadius(k: EnemyKind): Float = when {
        k.isBoss -> 28f
        k == EnemyKind.TANK -> 22f
        else -> 16f
    }

    /** Polygon sides of the enemy body: runners are arrowheads, tanks hexagons, the rest round. */
    fun enemySides(k: EnemyKind): Int = when (k) {
        EnemyKind.FAST -> 3
        EnemyKind.TANK -> 6
        else -> 24
    }
}
