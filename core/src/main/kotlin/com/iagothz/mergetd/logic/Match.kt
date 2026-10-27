package com.iagothz.mergetd.logic

import kotlin.math.roundToInt
import kotlin.random.Random

class Match(val deck: List<DieType>, private val rng: Random = Random.Default) {
    val board = Board()
    var sp = Balance.START_SP
    var lives = Balance.START_LIVES
    var wave = 0
        private set

    private val enemyList = mutableListOf<Enemy>()
    val enemies: List<Enemy> get() = enemyList

    private val shots = mutableListOf<ShotEvent>()
    /** Shots fired during the last [update] call. */
    val lastShots: List<ShotEvent> get() = shots

    private val events = mutableListOf<GameEvent>()

    private var purchases = 0
    private val powerLevels = mutableMapOf<DieType, Int>()

    private var inWave = false
    private var countdown = Balance.WAVE_DELAY
    private val spawnQueue = ArrayDeque<EnemyKind>()
    private var spawnTimer = 0f
    private var accumulator = 0f

    val isGameOver: Boolean get() = lives <= 0
    val buyCost: Int get() = Balance.BUY_BASE + Balance.BUY_STEP * purchases
    /** Seconds until the next wave starts; 0 while a wave is running. */
    val waveCountdown: Float get() = if (inWave) 0f else countdown

    fun powerLevel(t: DieType): Int = powerLevels[t] ?: 1

    fun powerUpCost(t: DieType): Int? = Balance.POWER_COSTS.getOrNull(powerLevel(t) - 1)

    fun powerMult(t: DieType): Float = Balance.powerMult(powerLevel(t))

    fun canBuy(): Boolean = !isGameOver && sp >= buyCost && board.emptyCells().isNotEmpty()

    fun buyDie(): Boolean {
        if (!canBuy()) return false
        val cell = board.emptyCells().random(rng)
        board[cell] = Die(deck.random(rng), 1)
        sp -= buyCost
        purchases++
        events += GameEvent.DieBought(cell)
        return true
    }

    fun canMerge(from: Cell, to: Cell): Boolean = !isGameOver && board.canMerge(from, to)

    fun merge(from: Cell, to: Cell): Boolean {
        if (isGameOver || !board.merge(from, to, deck, rng)) return false
        events += GameEvent.Merged(to)
        return true
    }

    fun powerUp(t: DieType): Boolean {
        if (isGameOver) return false
        val cost = powerUpCost(t) ?: return false
        if (sp < cost) return false
        sp -= cost
        powerLevels[t] = powerLevel(t) + 1
        events += GameEvent.PoweredUp(t)
        return true
    }

    /** Returns everything that happened since the last call (actions and updates alike) and clears it. */
    fun drainEvents(): List<GameEvent> {
        val out = events.toList()
        events.clear()
        return out
    }

    /** Runs fixed simulation steps; [dt] is clamped so a long hitch can't fast-forward the match. */
    fun update(dt: Float) {
        shots.clear()
        if (isGameOver) return
        accumulator += dt.coerceIn(0f, Balance.MAX_FRAME)
        while (accumulator >= Balance.STEP && !isGameOver) {
            step(Balance.STEP)
            accumulator -= Balance.STEP
        }
    }

    /** Plain-text save of everything [update] depends on except the RNG; read it back with [restore]. */
    fun snapshot(): String = buildString {
        fun line(key: String, value: Any) = append(key).append('=').append(value).append('\n')
        line("v", SNAPSHOT_VERSION)
        line("deck", DeckCodec.encode(deck))
        line("wave", wave)
        line("sp", sp)
        line("lives", lives)
        line("purchases", purchases)
        line("inWave", inWave)
        line("countdown", countdown)
        line("spawnTimer", spawnTimer)
        line("accumulator", accumulator)
        line("power", powerLevels.entries.joinToString(",") { "${it.key.id}:${it.value}" })
        line("queue", spawnQueue.joinToString(",") { it.name })
        for ((c, d) in board.occupied()) {
            line("die", listOf(c.row, c.col, d.type.id, d.pips, d.cooldown, d.timer, d.disabledFor).joinToString(","))
        }
        for (e in enemyList) {
            val fields = listOf(e.kind.name, e.maxHp, e.hp, e.distance, e.slow, e.slowTime, e.abilityTimer) +
                e.poison.map { "${it.dps}:${it.remaining}" }
            line("enemy", fields.joinToString(","))
        }
    }

    internal fun debugStartWave(n: Int) {
        enemyList.clear()
        startWave(n)
    }

    internal fun debugAddEnemy(e: Enemy) {
        enemyList += e
    }

    // Desktop dev-mode cheats (never wired in the Android build).

    internal fun devNextWave() = debugStartWave(wave + 1)

    internal fun devBossWave() = debugStartWave((wave / Balance.BOSS_EVERY + 1) * Balance.BOSS_EVERY)

    /** Kills every enemy; rewards are paid on the next step as usual. */
    internal fun devKillAll() = enemyList.forEach { it.hp = 0f }

    internal fun devAddSp(amount: Int) {
        sp += amount
    }

    internal fun devAddLife() {
        lives++
    }

    private fun startWave(n: Int) {
        wave = n
        inWave = true
        spawnQueue.clear()
        spawnQueue.addAll(WaveSpawner.composition(n))
        spawnTimer = 0f
        events += GameEvent.WaveStarted(n, WaveSpawner.isBossWave(n))
    }

    private fun step(dt: Float) {
        updateSpawner(dt)
        moveEnemies(dt)
        bossAbilities(dt)
        updateDice(dt)
        removeDead()
    }

    private fun updateSpawner(dt: Float) {
        if (!inWave) {
            countdown -= dt
            if (countdown <= 0f) startWave(wave + 1)
            return
        }
        spawnTimer -= dt
        if (spawnQueue.isNotEmpty() && spawnTimer <= 0f) {
            val kind = spawnQueue.removeFirst()
            enemyList += Enemy(kind, WaveSpawner.enemyHp(wave, kind))
            spawnTimer += Balance.SPAWN_INTERVAL
        }
        if (spawnQueue.isEmpty() && enemyList.isEmpty()) {
            inWave = false
            countdown = Balance.WAVE_DELAY
        }
    }

    private fun moveEnemies(dt: Float) {
        val iter = enemyList.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            e.tickEffects(dt)
            if (!e.alive) continue
            e.distance += e.speed() * dt
            if (e.distance >= Layout.PATH.length) {
                lives = maxOf(0, lives - e.kind.lifeDamage)
                events += GameEvent.Leaked(e.kind)
                iter.remove()
            }
        }
    }

    private fun bossAbilities(dt: Float) {
        for (e in enemyList) {
            if (!e.kind.isBoss || !e.alive) continue
            e.abilityTimer += dt
            if (e.abilityTimer < Balance.BOSS_ABILITY_INTERVAL) continue
            e.abilityTimer -= Balance.BOSS_ABILITY_INTERVAL
            when (e.kind) {
                EnemyKind.BOSS_SILENCER -> board.occupied()
                    .map { it.second }
                    .filter { !it.disabled }
                    .randomOrNull(rng)
                    ?.disabledFor = Balance.SILENCE_DURATION
                EnemyKind.BOSS_SWAPPER -> {
                    val cells = board.occupied().map { it.first }.shuffled(rng)
                    if (cells.size >= 2) board.swap(cells[0], cells[1])
                }
                else -> Unit
            }
        }
    }

    private fun updateDice(dt: Float) {
        for ((cell, die) in board.occupied()) {
            if (die.disabled) {
                die.disabledFor -= dt
                continue
            }
            when (die.type) {
                DieType.MINE -> {
                    die.timer += dt
                    if (die.timer >= Balance.MINE_INTERVAL) {
                        die.timer -= Balance.MINE_INTERVAL
                        sp += (Balance.MINE_SP_PER_PIP * die.pips * powerMult(die.type)).roundToInt()
                    }
                }
                DieType.GROWTH -> {
                    die.timer += dt
                    if (die.timer >= Balance.GROWTH_INTERVAL) {
                        die.timer -= Balance.GROWTH_INTERVAL
                        die.pips = minOf(Balance.MAX_PIPS, die.pips + 1)
                    }
                }
                else -> Unit
            }
            if (die.type.attacks) attack(cell, die, dt)
        }
    }

    private fun attack(cell: Cell, die: Die, dt: Float) {
        die.cooldown -= dt
        if (die.cooldown > 0f) return
        val shot = Combat.attack(die, Layout.cellCenter(cell), powerLevel(die.type), enemyList, Layout.PATH)
        if (shot == null) {
            die.cooldown = 0f
        } else {
            die.cooldown += Combat.attackInterval(die.type, die.pips)
            shots += shot
            shot.damage.forEach { (enemy, amount) -> events += GameEvent.Hit(enemy, amount) }
        }
    }

    private fun removeDead() {
        val iter = enemyList.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (!e.alive) {
                sp += e.kind.reward
                events += GameEvent.Killed(e.kind, Layout.PATH.positionAt(e.distance))
                iter.remove()
            }
        }
    }

    companion object {
        private const val SNAPSHOT_VERSION = 1

        /** Rebuilds a match from [snapshot] text; null if the text is damaged or from another version. */
        fun restore(text: String, rng: Random = Random.Default): Match? =
            try {
                parse(text, rng)
            } catch (e: RuntimeException) {
                null
            }

        private fun parse(text: String, rng: Random): Match {
            val entries = text.lines().filter { it.isNotBlank() }.map {
                val i = it.indexOf('=')
                require(i > 0) { "bad line: $it" }
                it.substring(0, i) to it.substring(i + 1)
            }
            val scalars = entries.toMap()
            require(scalars["v"]?.toInt() == SNAPSHOT_VERSION) { "unknown snapshot version" }
            fun type(id: String) = requireNotNull(DieType.byId(id)) { "unknown die $id" }
            fun list(key: String) = scalars.getValue(key).split(",").filter { it.isNotEmpty() }

            val m = Match(scalars.getValue("deck").split(",").map(::type), rng)
            m.wave = scalars.getValue("wave").toInt()
            m.sp = scalars.getValue("sp").toInt()
            m.lives = scalars.getValue("lives").toInt()
            m.purchases = scalars.getValue("purchases").toInt()
            m.inWave = scalars.getValue("inWave").toBooleanStrict()
            m.countdown = scalars.getValue("countdown").toFloat()
            m.spawnTimer = scalars.getValue("spawnTimer").toFloat()
            m.accumulator = scalars.getValue("accumulator").toFloat()
            for (p in list("power")) {
                val (id, level) = p.split(":")
                m.powerLevels[type(id)] = level.toInt()
            }
            m.spawnQueue.addAll(list("queue").map { EnemyKind.valueOf(it) })
            for ((key, value) in entries) {
                val f = value.split(",")
                when (key) {
                    "die" -> m.board[Cell(f[0].toInt(), f[1].toInt())] = Die(type(f[2]), f[3].toInt()).apply {
                        cooldown = f[4].toFloat()
                        timer = f[5].toFloat()
                        disabledFor = f[6].toFloat()
                    }
                    "enemy" -> m.enemyList += Enemy(EnemyKind.valueOf(f[0]), f[1].toFloat()).apply {
                        hp = f[2].toFloat()
                        distance = f[3].toFloat()
                        slow = f[4].toFloat()
                        slowTime = f[5].toFloat()
                        abilityTimer = f[6].toFloat()
                        for (stack in f.drop(7)) {
                            val (dps, remaining) = stack.split(":")
                            poison += PoisonStack(dps.toFloat(), remaining.toFloat())
                        }
                    }
                }
            }
            return m
        }
    }
}
