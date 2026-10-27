package com.iagothz.mergetd.logic

import kotlin.math.pow

object WaveSpawner {
    private val BOSSES = listOf(EnemyKind.BOSS_SILENCER, EnemyKind.BOSS_SWAPPER, EnemyKind.BOSS_REGEN)

    fun isBossWave(n: Int): Boolean = n % Balance.BOSS_EVERY == 0

    fun waveSize(n: Int): Int = minOf(Balance.MAX_WAVE_SIZE, 8 + 2 * n)

    /** Waves from [current] until the next boss wave (10 right after a boss). */
    fun wavesUntilBoss(current: Int): Int = Balance.BOSS_EVERY - current % Balance.BOSS_EVERY

    /** How many of each kind wave [n] brings, in order of first appearance; what the HUD previews. */
    fun preview(n: Int): Map<EnemyKind, Int> = composition(n).groupingBy { it }.eachCount()

    fun composition(n: Int): List<EnemyKind> {
        if (isBossWave(n)) return listOf(BOSSES[(n / Balance.BOSS_EVERY - 1) % BOSSES.size])
        return List(waveSize(n)) { i ->
            when {
                n >= 5 && i % 5 == 4 -> EnemyKind.TANK
                n >= 3 && i % 3 == 2 -> EnemyKind.FAST
                else -> EnemyKind.NORMAL
            }
        }
    }

    fun enemyHp(n: Int, kind: EnemyKind): Float =
        Balance.BASE_HP * Balance.HP_GROWTH.pow(n - 1) * kind.hpMult
}
