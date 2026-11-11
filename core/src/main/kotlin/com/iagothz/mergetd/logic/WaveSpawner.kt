package com.iagothz.mergetd.logic

import kotlin.math.pow

/** Waves 5, 15, 25... are one of these, in this order, round and round. */
enum class SpecialWave(val title: String) { HORDE("Horda"), TANKS("Tanques"), RUSH("Corrida") }

object WaveSpawner {
    /** Boss order when a match hasn't shuffled its own (tests, previews outside a match). */
    val DEFAULT_BOSS_ORDER: List<EnemyKind> = EnemyKind.entries.filter { it.isBoss }

    fun isBossWave(n: Int): Boolean = n % Balance.BOSS_EVERY == 0

    fun waveSize(n: Int): Int = minOf(Balance.MAX_WAVE_SIZE, 8 + 2 * n)

    /** Waves from [current] until the next boss wave (10 right after a boss). */
    fun wavesUntilBoss(current: Int): Int = Balance.BOSS_EVERY - current % Balance.BOSS_EVERY

    fun special(n: Int): SpecialWave? {
        if (isBossWave(n) || n % Balance.SPECIAL_EVERY != 0) return null
        return SpecialWave.entries[(n / Balance.SPECIAL_EVERY - 1) / 2 % SpecialWave.entries.size]
    }

    /** HP multiplier for every enemy of wave [n]: a horde is many but frail. */
    fun hpScale(n: Int): Float = if (special(n) == SpecialWave.HORDE) Balance.HORDE_HP else 1f

    /** The enemies of wave [n] in spawn order; [swarm] (a mutator) adds 40% more to every wave but a boss's. */
    fun composition(n: Int, bosses: List<EnemyKind> = DEFAULT_BOSS_ORDER, mode: GameMode = GameMode.NORMAL, swarm: Boolean = false): List<EnemyKind> {
        if (mode == GameMode.BOSS_RUSH) return listOf(bosses[(n - 1) % bosses.size])
        if (isBossWave(n)) return listOf(bosses[(n / Balance.BOSS_EVERY - 1) % bosses.size])
        val base = when (special(n)) {
            SpecialWave.HORDE -> List(Balance.HORDE_SIZE) { EnemyKind.NORMAL }
            SpecialWave.TANKS -> List(waveSize(n) / 3) { EnemyKind.TANK }
            SpecialWave.RUSH -> List(waveSize(n)) { EnemyKind.FAST }
            null -> ordinary(n)
        }
        return if (swarm) base + base.take(base.size * Balance.SWARM_EXTRA_NUM / Balance.SWARM_EXTRA_DEN) else base
    }

    /** A plain wave: the mix by position, an Elite in front every third wave, a Treasure at the back every fourth. */
    private fun ordinary(n: Int): List<EnemyKind> {
        val list = MutableList(waveSize(n)) { i ->
            when {
                n >= Balance.GHOST_FROM_WAVE && i % 6 == 5 -> EnemyKind.GHOST
                n >= Balance.HEALER_FROM_WAVE && i % 7 == 6 -> EnemyKind.HEALER
                n >= 5 && i % 5 == 4 -> EnemyKind.TANK
                n >= 3 && i % 3 == 2 -> EnemyKind.FAST
                else -> EnemyKind.NORMAL
            }
        }
        if (n >= Balance.ELITE_FROM_WAVE && n % Balance.ELITE_EVERY == 0) list[0] = EnemyKind.ELITE
        if (n >= Balance.TREASURE_FROM_WAVE && (n - Balance.TREASURE_FROM_WAVE) % Balance.TREASURE_EVERY == 0) list += EnemyKind.TREASURE
        return list
    }

    /** How many of each kind wave [n] brings, in order of first appearance; what the HUD previews. */
    fun preview(n: Int, bosses: List<EnemyKind> = DEFAULT_BOSS_ORDER, mode: GameMode = GameMode.NORMAL, swarm: Boolean = false): Map<EnemyKind, Int> =
        composition(n, bosses, mode, swarm).groupingBy { it }.eachCount()

    fun enemyHp(n: Int, kind: EnemyKind): Float = Balance.BASE_HP * Balance.HP_GROWTH.pow(n - 1) * kind.hpMult
}
