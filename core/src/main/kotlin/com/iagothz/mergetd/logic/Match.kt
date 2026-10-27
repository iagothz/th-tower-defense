package com.iagothz.mergetd.logic

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** A TRAP mine waiting on the path; [owner] is credited with its kills. */
class Trap(val distance: Float, val damage: Float, val owner: Die?)

/** [challengeDay] is set for a daily challenge (its day since the epoch); it changes only how the result is booked. */
class Match(
    val deck: List<DieType>,
    private val rng: Random = Random.Default,
    val map: GameMap = GameMap.DEFAULT,
    val challengeDay: Int? = null,
) {
    val board = Board()
    val path: EnemyPath get() = map.path
    var sp = Balance.START_SP
    var lives = Balance.START_LIVES
    var wave = 0
        private set

    private val enemyList = mutableListOf<Enemy>()
    /** Which boss comes on waves 10, 20, 30...: shuffled once per match, kept in the save. */
    var bossOrder: List<EnemyKind> = WaveSpawner.DEFAULT_BOSS_ORDER.shuffled(rng)
        private set
    val enemies: List<Enemy> get() = enemyList

    private val shots = mutableListOf<ShotEvent>()
    /** Shots fired during the last [update] call. */
    val lastShots: List<ShotEvent> get() = shots

    private val events = mutableListOf<GameEvent>()

    private val trapList = mutableListOf<Trap>()
    val traps: List<Trap> get() = trapList

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

    /** Attack-speed factor for the die in [cell] from active LIGHT dice next to it. */
    fun speedMultiplier(cell: Cell): Float {
        val neighbours = listOf(
            Cell(cell.row - 1, cell.col), Cell(cell.row + 1, cell.col),
            Cell(cell.row, cell.col - 1), Cell(cell.row, cell.col + 1),
        ).filter { it.row in 0 until Balance.ROWS && it.col in 0 until Balance.COLS }
        val lightPips = neighbours.mapNotNull { board[it] }
            .filter { it.type == DieType.LIGHT && !it.disabled }
            .sumOf { it.pips }
        return 1 + Balance.LIGHT_SPEED_PER_PIP * lightPips
    }

    /** Board-wide bonuses: crit chance from active CRIT dice and the number of COMBO dice. */
    fun attackBonus(): AttackBonus {
        val dice = board.occupied().map { it.second }
        val critPips = dice.filter { it.type == DieType.CRIT && !it.disabled }.sumOf { it.pips }
        return AttackBonus(
            critChance = minOf(Balance.CRIT_MAX, Balance.CRIT_CHANCE_PER_PIP * critPips),
            comboCount = dice.count { it.type == DieType.COMBO },
        )
    }

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
        if (isGameOver) return false
        val result = board.resultCell(from, to)
        if (!board.merge(from, to, deck, rng)) return false
        events += GameEvent.Merged(result)
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
        line("map", map.id)
        line("daily", challengeDay ?: -1)
        line("bosses", bossOrder.joinToString(",") { it.name })
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
            line("die", listOf(c.row, c.col, d.type.id, d.pips, d.cooldown, d.timer, d.disabledFor, d.stacks).joinToString(","))
        }
        for (e in enemyList) {
            val fields = listOf(e.kind.name, e.maxHp, e.hp, e.distance, e.slow, e.slowTime, e.abilityTimer, e.curse, e.curseTime, e.stunTime, e.stunImmune, e.pushed, e.stolen, e.armor) +
                e.poison.map { "${it.dps}:${it.remaining}" }
            line("enemy", fields.joinToString(","))
        }
        for (t in trapList) line("trap", "${t.distance},${t.damage}")
    }

    internal fun debugStartWave(n: Int) {
        enemyList.clear()
        startWave(n)
    }

    internal fun debugAddEnemy(e: Enemy) {
        enemyList += e
    }

    /** Spawns one [kind] exactly as the current wave would. */
    internal fun debugSpawn(kind: EnemyKind) = spawn(kind)

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
        spawnQueue.addAll(WaveSpawner.composition(n, bossOrder))
        spawnTimer = 0f
        events += GameEvent.WaveStarted(n, WaveSpawner.isBossWave(n), WaveSpawner.special(n))
    }

    private fun step(dt: Float) {
        updateSpawner(dt)
        auras(dt)
        moveEnemies(dt)
        triggerTraps()
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
            spawn(spawnQueue.removeFirst())
            spawnTimer += Balance.SPAWN_INTERVAL
        }
        if (spawnQueue.isEmpty() && enemyList.isEmpty()) {
            inWave = false
            countdown = Balance.WAVE_DELAY
        }
    }

    private fun spawn(kind: EnemyKind) {
        enemyList += Enemy(kind, WaveSpawner.enemyHp(wave, kind) * WaveSpawner.hpScale(wave)).apply {
            if (kind == EnemyKind.BOSS_ARMORED) armor = Balance.ARMOR_FRACTION * WaveSpawner.enemyHp(wave, EnemyKind.NORMAL)
        }
    }

    /** Healers mend their neighbours; a Commander hastes its escort. Both work by distance along the path. */
    private fun auras(dt: Float) {
        for (e in enemyList) e.hasted = false
        for (src in enemyList) {
            if (!src.alive) continue
            when (src.kind) {
                EnemyKind.HEALER -> for (e in enemyList) {
                    if (e !== src && e.alive && abs(e.distance - src.distance) <= Balance.HEALER_RADIUS) {
                        e.hp = minOf(e.maxHp, e.hp + Balance.HEALER_PER_SEC * e.maxHp * dt)
                    }
                }
                EnemyKind.BOSS_COMMANDER -> for (e in enemyList) {
                    if (e !== src && abs(e.distance - src.distance) <= Balance.COMMANDER_RADIUS) e.hasted = true
                }
                else -> Unit
            }
        }
    }

    private fun moveEnemies(dt: Float) {
        val iter = enemyList.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            e.tickEffects(dt)
            if (!e.alive) continue
            e.distance += e.speed() * dt
            if (e.distance >= path.length) {
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
                EnemyKind.BOSS_THIEF -> {
                    val taken = (sp * Balance.THIEF_FRACTION).roundToInt()
                    if (taken > 0) {
                        sp -= taken
                        e.stolen += taken
                        events += GameEvent.Stolen(taken)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun updateDice(dt: Float) {
        val bonus = attackBonus()
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
                    // Each pip it earns, it turns into one of the deck's types; it may roll Growth and keep going.
                    if (die.timer >= Balance.GROWTH_INTERVAL && die.pips < Balance.MAX_PIPS) {
                        board[cell] = Die(deck.random(rng), die.pips + 1).apply { disabledFor = die.disabledFor }
                        continue
                    } else if (die.timer >= Balance.GROWTH_INTERVAL) {
                        die.timer -= Balance.GROWTH_INTERVAL
                    }
                }
                else -> Unit
            }
            if (die.type.attacks) attack(cell, die, dt, bonus)
        }
    }

    private fun attack(cell: Cell, die: Die, dt: Float, bonus: AttackBonus) {
        die.cooldown -= dt
        if (die.cooldown > 0f) return
        val fired = if (die.type == DieType.TRAP) {
            plantTrap(die)
        } else {
            val shot = Combat.attack(die, Layout.cellCenter(cell), powerLevel(die.type), enemyList, path, bonus, rng)
            if (shot != null) {
                shots += shot
                shot.damage.forEach { (enemy, amount) -> events += GameEvent.Hit(enemy, amount, shot.crit, shot.type) }
            }
            shot != null
        }
        if (fired) {
            die.cooldown += Combat.attackInterval(die.type, die.pips) / speedMultiplier(cell)
        } else {
            die.cooldown = 0f
        }
    }

    /** Plants a mine ahead of the leading enemy, up to the per-die cap. */
    private fun plantTrap(die: Die): Boolean {
        val leader = enemyList.filter { it.alive }.maxByOrNull { it.distance } ?: return false
        if (trapList.count { it.owner === die } >= Balance.TRAP_MAX_PER_DIE) return false
        val at = minOf(leader.distance + Balance.TRAP_AHEAD, path.length - 1f)
        trapList += Trap(at, Combat.baseDamage(DieType.TRAP, die.pips, powerLevel(DieType.TRAP)), die)
        return true
    }

    private fun triggerTraps() {
        val iter = trapList.iterator()
        while (iter.hasNext()) {
            val trap = iter.next()
            if (enemyList.none { it.alive && it.kind != EnemyKind.GHOST && abs(it.distance - trap.distance) <= Balance.TRAP_TRIGGER }) continue
            iter.remove()
            blast(path.positionAt(trap.distance), Balance.TRAP_RADIUS, trap.damage, trap.owner, DieType.TRAP)
        }
    }

    /** Area damage around [center], credited to [by]. */
    private fun blast(center: Vec2, radius: Float, damage: Float, by: Die?, type: DieType) {
        events += GameEvent.Blast(center, radius, type)
        for (e in enemyList) {
            if (!e.alive || path.positionAt(e.distance).dst(center) > radius) continue
            // Ghosts walk over mines; a Supernova still catches them.
            if (type == DieType.TRAP && e.kind == EnemyKind.GHOST) continue
            events += GameEvent.Hit(e, e.takeDamage(damage, by), type = type)
        }
    }

    /** Pays rewards and kill effects; repeats while Supernova explosions keep killing. */
    private fun removeDead() {
        while (true) {
            val dead = enemyList.filter { !it.alive }
            if (dead.isEmpty()) return
            enemyList.removeAll { !it.alive }
            for (e in dead) {
                val at = path.positionAt(e.distance)
                sp += e.kind.reward + e.stolen
                events += GameEvent.Killed(e.kind, at)
                if (e.kind == EnemyKind.BOSS_SPLITTER) {
                    for (side in listOf(-1f, 1f)) {
                        enemyList += Enemy(EnemyKind.TANK, e.maxHp * Balance.SPLITTER_FRACTION).apply {
                            distance = (e.distance + side * 20f).coerceIn(0f, path.length - 1f)
                        }
                    }
                }
                val killer = e.lastHit ?: continue
                when (killer.type) {
                    DieType.VAMPIRE -> sp += Balance.VAMPIRE_SP_PER_PIP * killer.pips
                    DieType.HUNTER -> if (killer.stacks < Balance.hunterCap(killer.pips)) killer.stacks++
                    DieType.NOVA -> blast(at, Balance.NOVA_RADIUS, Combat.baseDamage(DieType.NOVA, killer.pips, powerLevel(DieType.NOVA)), killer, DieType.NOVA)
                    else -> Unit
                }
            }
        }
    }

    companion object {
        private const val SNAPSHOT_VERSION = 3

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

            // Saves from before maps have no "map" line and were always played on the default map.
            val map = scalars["map"]?.let { requireNotNull(GameMap.byId(it)) { "unknown map $it" } } ?: GameMap.DEFAULT
            val daily = scalars["daily"]?.toInt()?.takeIf { it >= 0 }
            val m = Match(scalars.getValue("deck").split(",").map(::type), rng, map, daily)
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
            m.bossOrder = list("bosses").map { EnemyKind.valueOf(it) }
            for ((key, value) in entries) {
                val f = value.split(",")
                when (key) {
                    "die" -> m.board[Cell(f[0].toInt(), f[1].toInt())] = Die(type(f[2]), f[3].toInt()).apply {
                        cooldown = f[4].toFloat()
                        timer = f[5].toFloat()
                        disabledFor = f[6].toFloat()
                        stacks = f[7].toInt()
                    }
                    "enemy" -> m.enemyList += Enemy(EnemyKind.valueOf(f[0]), f[1].toFloat()).apply {
                        hp = f[2].toFloat()
                        distance = f[3].toFloat()
                        slow = f[4].toFloat()
                        slowTime = f[5].toFloat()
                        abilityTimer = f[6].toFloat()
                        curse = f[7].toFloat()
                        curseTime = f[8].toFloat()
                        stunTime = f[9].toFloat()
                        stunImmune = f[10].toFloat()
                        pushed = f[11].toFloat()
                        stolen = f[12].toInt()
                        armor = f[13].toFloat()
                        for (stack in f.drop(14)) {
                            val (dps, remaining) = stack.split(":")
                            poison += PoisonStack(dps.toFloat(), remaining.toFloat())
                        }
                    }
                    "trap" -> m.trapList += Trap(f[0].toFloat(), f[1].toFloat(), null)
                }
            }
            return m
        }
    }
}
