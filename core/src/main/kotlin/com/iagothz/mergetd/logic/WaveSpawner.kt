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

    fun composition(n: Int, bosses: List<EnemyKind> = DEFAULT_BOSS_ORDER, mode: GameMode = GameMode.NORMAL): List<EnemyKind> {
        if (mode == GameMode.BOSS_RUSH) return listOf(bosses[(n - 1) % bosses.size])
        if (isBossWave(n)) return listOf(bosses[(n / Balance.BOSS_EVERY - 1) % bosses.size])
        when (special(n)) {
            SpecialWave.HORDE -> return List(Balance.HORDE_SIZE) { EnemyKind.NORMAL }
            SpecialWave.TANKS -> return List(waveSize(n) / 3) { EnemyKind.TANK }
            SpecialWave.RUSH -> return List(waveSize(n)) { EnemyKind.FAST }
            null -> Unit
        }
        return List(waveSize(n)) { i ->
            when {
                n >= Balance.GHOST_FROM_WAVE && i % 6 == 5 -> EnemyKind.GHOST
                n >= Balance.HEALER_FROM_WAVE && i % 7 == 6 -> EnemyKind.HEALER
                n >= 5 && i % 5 == 4 -> EnemyKind.TANK
                n >= 3 && i % 3 == 2 -> EnemyKind.FAST
                else -> EnemyKind.NORMAL
            }
        }
    }

    /** How many of each kind wave [n] brings, in order of first appearance; what the HUD previews. */
    fun preview(n: Int, bosses: List<EnemyKind> = DEFAULT_BOSS_ORDER, mode: GameMode = GameMode.NORMAL): Map<EnemyKind, Int> =
        composition(n, bosses, mode).groupingBy { it }.eachCount()

    fun enemyHp(n: Int, kind: EnemyKind): Float =
        Balance.BASE_HP * Balance.HP_GROWTH.pow(n - 1) * kind.hpMult
}
