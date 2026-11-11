package com.iagothz.mergetd.logic

import com.iagothz.mergetd.log.Log
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** A TRAP mine waiting on the path; [owner] is credited with its kills. */
class Trap(val distance: Float, val damage: Float, val owner: Die?, val radius: Float = Balance.TRAP_RADIUS)

/** [challengeDay] is set for a daily challenge (its day since the epoch); it changes only how the result is booked. */
class Match(
    val deck: List<DieType>,
    private val rng: Random = Random.Default,
    val map: GameMap = GameMap.DEFAULT,
    val challengeDay: Int? = null,
    val mode: GameMode = GameMode.NORMAL,
    /** The RNG seed this match started from, when known; what a match code reproduces. */
    val seed: Int? = null,
    /** Started from a code: map and deck were dictated, so it books no map or deck record. */
    val locked: Boolean = false,
    /** Handicaps chosen before the match; they scale the score. */
    val mutators: Set<Mutator> = emptySet(),
    /** The weekly challenge this match is, if any (weeks since 1970); booked against that week's score. */
    val week: Int? = null,
    /** Permanent levels from the account (see [DieLevels]); absent means 1. */
    val dieLevels: Map<DieType, Int> = emptyMap(),
    /** A free-play board for trying things out: waves start only on request and leaks cost no life. Never saved or booked. */
    val sandbox: Boolean = false,
) {
    /** What a code needs to replay this match; null for a match without a known seed. */
    val setup: MatchSetup? get() = seed?.let { MatchSetup(map, mode, it, deck, mutators) }

    /** The mutator score so far: wave × the product of the active mutators. */
    val score: Int get() = Mutator.score(wave, mutators)

    /** What is still to come this wave plus what is on the path, by kind. */
    fun previewOfCurrentWave(): Map<EnemyKind, Int> = (spawnQueue + enemyList.map { it.kind }).groupingBy { it }.eachCount()

    val board = Board()
    val path: EnemyPath get() = map.path
    var sp = Balance.START_SP * (if (mode == GameMode.SURVIVAL) Balance.SURVIVAL_SP else 1)
    var lives = if (mode == GameMode.SURVIVAL) 1 else Balance.START_LIVES
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

    /** Pause between waves; Rápido cuts it. */
    val waveDelay: Float get() = Balance.WAVE_DELAY * (if (mode == GameMode.RUSH) Balance.RUSH_DELAY else 1f)
    private var countdown = waveDelay
    private val spawnQueue = ArrayDeque<EnemyKind>()
    private var spawnTimer = 0f
    private var accumulator = 0f

    val isGameOver: Boolean get() = lives <= 0
    val buyCost: Int get() = Balance.BUY_BASE + Balance.BUY_STEP * purchases

    /** Seconds until the next wave starts; 0 while a wave is running. */
    val waveCountdown: Float get() = if (inWave || sandbox) 0f else countdown

    /** Whether a wave is still spawning or has enemies on the path. */
    val waveRunning: Boolean get() = inWave

    fun powerLevel(t: DieType): Int = powerLevels[t] ?: 1

    fun powerUpCost(t: DieType): Int? = Balance.POWER_COSTS.getOrNull(powerLevel(t) - 1)

    fun dieLevel(t: DieType): Int = dieLevels[t] ?: 1

    /** Everything that scales the die's numbers: the match's power-ups times the account's die level. */
    fun powerMult(t: DieType): Float = Balance.powerMult(powerLevel(t)) * DieLevels.mult(dieLevel(t))

    /** Seconds a Growth die needs per pip right now: its power-up levels make it grow faster. */
    fun growthInterval(): Float = Balance.GROWTH_INTERVAL / powerMult(DieType.GROWTH)

    /** Attack-speed factor for the die in [cell] from active LIGHT dice next to it; a 7-pip Light also reaches diagonals. */
    fun speedMultiplier(cell: Cell): Float {
        fun light(dr: Int, dc: Int, diagonal: Boolean): Int {
            val c = Cell(cell.row + dr, cell.col + dc)
            if (c.row !in 0 until Balance.ROWS || c.col !in 0 until Balance.COLS) return 0
            val d = board[c] ?: return 0
            if (d.type != DieType.LIGHT || d.disabled || (diagonal && d.pips < Balance.MAX_PIPS)) return 0
            return d.pips
        }
        val lightPips = light(-1, 0, false) + light(1, 0, false) + light(0, -1, false) + light(0, 1, false) +
            light(-1, -1, true) + light(-1, 1, true) + light(1, -1, true) + light(1, 1, true)
        return 1 + Balance.LIGHT_SPEED_PER_PIP * lightPips
    }

    /** Synergies switched on by the dice standing (and not silenced) on the board. */
    fun synergies(): Set<Synergy> = Synergy.active(board.occupied().map { it.second }.filter { !it.disabled }.map { it.type })

    /** Board-wide bonuses: crit chance and multiplier, COMBO count, synergies, strongest damage for a 7-pip Mirror. */
    fun attackBonus(): AttackBonus {
        val dice = board.occupied().map { it.second }
        val active = dice.filter { !it.disabled }
        val syn = synergies()
        val critPips = active.filter { it.type == DieType.CRIT }.sumOf { it.pips }
        val dazzle = if (Synergy.DAZZLE in syn) Balance.DAZZLE_CRIT else 0f
        return AttackBonus(
            critChance = minOf(Balance.CRIT_MAX, Balance.CRIT_CHANCE_PER_PIP * critPips + dazzle),
            comboCount = dice.count { it.type == DieType.COMBO } + dice.count { it.type == DieType.COMBO && it.pips >= Balance.MAX_PIPS },
            critMult = if (active.any { it.type == DieType.CRIT && it.pips >= Balance.MAX_PIPS }) Balance.CRIT_MULT_MAX else Balance.CRIT_MULT,
            synergies = syn,
            strongestDamage = active.filter { it.type.attacks && it.type != DieType.MIRROR }
                .maxOfOrNull { Combat.baseDamage(it.type, it.pips, powerMult(it.type)) } ?: 0f,
        )
    }

    fun canBuy(): Boolean = !isGameOver && sp >= buyCost && board.emptyCells().isNotEmpty()

    /** Whether a bought die could go on [cell]: the match affords one and the cell is empty. */
    fun canBuyAt(cell: Cell): Boolean = !isGameOver && sp >= buyCost && board[cell] == null

    /** Buys a one-pip die of a random deck type on an empty cell picked at random (the bots' way). */
    fun buyDie(): Boolean {
        if (!canBuy()) return false
        return buyDie(board.emptyCells().random(rng))
    }

    /** Buys a one-pip die of a random deck type on [cell], the player's pick. */
    fun buyDie(cell: Cell): Boolean {
        if (!canBuyAt(cell)) return false
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
        events += GameEvent.Merged(result, board[result]?.pips ?: 0)
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
        line("mode", mode.id)
        line("seed", seed ?: "")
        line("locked", locked)
        line("mutators", Mutator.mask(mutators))
        line("week", week ?: -1)
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
        line("levels", dieLevels.entries.filter { it.value > 1 }.sortedBy { it.key }.joinToString(",") { "${it.key.id}:${it.value}" })
        line("queue", spawnQueue.joinToString(",") { it.name })
        for ((c, d) in board.occupied()) {
            line("die", listOf(c.row, c.col, d.type.id, d.pips, d.cooldown, d.timer, d.disabledFor, d.stacks).joinToString(","))
        }
        for (e in enemyList) {
            val fields = listOf(e.kind.name, e.maxHp, e.hp, e.distance, e.slow, e.slowTime, e.abilityTimer, e.curse, e.curseTime, e.stunTime, e.stunImmune, e.pushed, e.stolen, e.armor) +
                e.poison.map { "${it.dps}:${it.remaining}" }
            line("enemy", fields.joinToString(","))
        }
        for (t in trapList) line("trap", "${t.distance},${t.damage},${t.radius}")
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

    // Sandbox: the board is the player's to arrange, and waves come when asked for.

    /** Puts a die of [type] with [pips] (clamped to 1..MAX_PIPS) on [cell], replacing whatever was there. Sandbox only. */
    fun place(cell: Cell, type: DieType, pips: Int): Boolean {
        if (!sandbox) return false
        board[cell] = Die(type, pips.coerceIn(1, Balance.MAX_PIPS))
        events += GameEvent.DieBought(cell)
        return true
    }

    /** Takes the die off [cell]. Sandbox only. */
    fun remove(cell: Cell): Boolean {
        if (!sandbox || board[cell] == null) return false
        board[cell] = null
        return true
    }

    fun moveDie(from: Cell, to: Cell): Boolean {
        if (!sandbox || from == to || board[from] == null) return false
        board.swap(from, to)
        return true
    }

    /** Empties the board. Sandbox only. */
    fun clearBoard() {
        if (!sandbox) return
        for (c in board.cells()) board[c] = null
        trapList.clear()
    }

    /** Sends wave [n] right now, dropping whatever is on the path. Sandbox only. */
    fun sendWave(n: Int) {
        if (!sandbox) return
        debugStartWave(n.coerceAtLeast(1))
    }

    /** Stops the wave: clears the path and the queue without paying rewards. Sandbox only. */
    fun stopWave() {
        if (!sandbox) return
        enemyList.clear()
        spawnQueue.clear()
        inWave = false
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

    /** Loses a life as if a soldier had walked through, for testing the leak feedback. */
    /** Puts one [kind] on the path right now, for tests. */
    internal fun devSpawn(kind: EnemyKind): Enemy {
        spawn(kind)
        return enemyList.last()
    }

    internal fun devLeak() {
        lives = maxOf(0, lives - 1)
        events += GameEvent.Leaked(EnemyKind.NORMAL)
    }

    private fun startWave(n: Int) {
        wave = n
        inWave = true
        spawnQueue.clear()
        spawnQueue.addAll(WaveSpawner.composition(n, bossOrder, mode, Mutator.SWARM in mutators))
        spawnTimer = 0f
        val boss = spawnQueue.firstOrNull()?.takeIf { it.isBoss }
        events += GameEvent.WaveStarted(n, boss != null, WaveSpawner.special(n), boss)
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
            if (sandbox) return
            countdown -= dt
            if (countdown <= 0f) startWave(wave + 1)
            return
        }
        spawnTimer -= dt
        if (spawnQueue.isNotEmpty() && spawnTimer <= 0f) {
            spawn(spawnQueue.removeFirst())
            spawnTimer += Balance.SPAWN_INTERVAL * (if (mode == GameMode.RUSH) Balance.RUSH_SPAWN else 1f)
        }
        if (spawnQueue.isEmpty() && enemyList.isEmpty()) {
            inWave = false
            countdown = waveDelay
            events += GameEvent.WaveCleared(wave)
        }
    }

    private fun spawn(kind: EnemyKind) {
        val modeHp = when {
            mode == GameMode.BOSS_RUSH && kind.isBoss -> Balance.BOSS_RUSH_HP
            mode == GameMode.RUSH -> Balance.RUSH_HP
            else -> 1f
        }
        val toughness = if (Mutator.TOUGH in mutators) Balance.TOUGH_HP else 1f
        enemyList += Enemy(kind, WaveSpawner.enemyHp(wave, kind) * WaveSpawner.hpScale(wave) * modeHp * toughness).apply {
            if (kind == EnemyKind.BOSS_ARMORED) armor = Balance.ARMOR_FRACTION * WaveSpawner.enemyHp(wave, EnemyKind.NORMAL)
            speedScale = (if (mode == GameMode.RUSH) Balance.RUSH_SPEED else 1f) * (if (Mutator.SWIFT in mutators) Balance.SWIFT_SPEED else 1f)
        }
    }

    /** Healers mend their neighbours; a Commander hastes its escort. Both work by distance along the path. */
    private fun auras(dt: Float) {
        for (e in enemyList) e.hasted = false
        for (src in enemyList) {
            if (!src.alive) continue
            when (src.kind) {
                EnemyKind.HEALER, EnemyKind.ELITE -> for (e in enemyList) {
                    val rate = if (src.kind == EnemyKind.ELITE) Balance.ELITE_HEAL_PER_SEC else Balance.HEALER_PER_SEC
                    if (e !== src && e.alive && abs(e.distance - src.distance) <= Balance.HEALER_RADIUS) {
                        e.hp = minOf(e.maxHp, e.hp + rate * e.maxHp * dt)
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
                // A Treasure that gets away costs nothing but the reward.
                if (e.kind.lifeDamage > 0 && !sandbox) {
                    lives = maxOf(0, lives - e.kind.lifeDamage)
                    events += GameEvent.Leaked(e.kind)
                }
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
                        val maxed = if (die.pips >= Balance.MAX_PIPS) 2 else 1
                        sp += (Balance.MINE_SP_PER_PIP * die.pips * powerMult(die.type)).roundToInt() * maxed
                    }
                }
                DieType.BOOST -> if (die.pips >= Balance.MAX_PIPS) {
                    // A 7-pip Boost hands out a free power level now and then.
                    die.timer += dt
                    if (die.timer >= Balance.BOOST_INTERVAL) {
                        die.timer -= Balance.BOOST_INTERVAL
                        deck.filter { powerUpCost(it) != null }.randomOrNull(rng)?.let { t ->
                            powerLevels[t] = powerLevel(t) + 1
                            events += GameEvent.PoweredUp(t)
                        }
                    }
                }
                DieType.GROWTH -> {
                    die.timer += dt
                    val interval = growthInterval()
                    // Each pip it earns, it turns into one of the deck's types; it may roll Growth and keep going.
                    if (die.timer >= interval && die.pips < Balance.MAX_PIPS) {
                        board[cell] = Die(deck.random(rng), die.pips + 1).apply { disabledFor = die.disabledFor }
                        continue
                    } else if (die.timer >= interval) {
                        // At 7 pips it plants a new 1-pip die from the deck instead.
                        die.timer -= interval
                        board.emptyCells().randomOrNull(rng)?.let { empty ->
                            board[empty] = Die(deck.random(rng), 1)
                            events += GameEvent.DieBought(empty)
                        }
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
            val shot = Combat.attack(die, Layout.cellCenter(cell), powerMult(die.type), enemyList, path, bonus, rng)
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
        val maxed = die.pips >= Balance.MAX_PIPS
        if (trapList.count { it.owner === die } >= (if (maxed) Balance.TRAP_MAX_PER_DIE_MAX else Balance.TRAP_MAX_PER_DIE)) return false
        val at = minOf(leader.distance + Balance.TRAP_AHEAD, path.length - 1f)
        val radius = Balance.TRAP_RADIUS * (if (maxed) Balance.TRAP_MAX_RADIUS else 1f)
        trapList += Trap(at, Combat.baseDamage(DieType.TRAP, die.pips, powerMult(DieType.TRAP)), die, radius)
        return true
    }

    private fun triggerTraps() {
        val iter = trapList.iterator()
        val trigger = Balance.TRAP_TRIGGER * (if (Synergy.AMBUSH in synergies()) Balance.AMBUSH_TRIGGER else 1f)
        while (iter.hasNext()) {
            val trap = iter.next()
            if (enemyList.none { it.alive && it.kind != EnemyKind.GHOST && abs(it.distance - trap.distance) <= trigger }) continue
            iter.remove()
            blast(path.positionAt(trap.distance), trap.radius, trap.damage, trap.owner, DieType.TRAP)
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
                val reward = if (Mutator.POOR in mutators) (e.kind.reward * Balance.POOR_REWARD).roundToInt() else e.kind.reward
                sp += reward * (if (mode == GameMode.SURVIVAL) Balance.SURVIVAL_SP else 1) + e.stolen
                events += GameEvent.Killed(e.kind, at, e.lastHit?.type)
                if (e.kind == EnemyKind.BOSS_SPLITTER) {
                    for (side in listOf(-1f, 1f)) {
                        enemyList += Enemy(EnemyKind.TANK, e.maxHp * Balance.SPLITTER_FRACTION).apply {
                            distance = (e.distance + side * 20f).coerceIn(0f, path.length - 1f)
                        }
                    }
                }
                val killer = e.lastHit ?: continue
                when (killer.type) {
                    DieType.VAMPIRE -> {
                        val maxed = killer.pips >= Balance.MAX_PIPS
                        sp += Balance.VAMPIRE_SP_PER_PIP * killer.pips * (if (maxed) 2 else 1)
                        killer.stacks++
                        if (maxed && killer.stacks % Balance.VAMPIRE_LIFE_KILLS == 0) lives = minOf(Balance.MAX_LIVES, lives + 1)
                    }
                    DieType.HUNTER -> if (killer.stacks < Balance.hunterCap(killer.pips)) killer.stacks++
                    DieType.NOVA -> {
                        val boost = if (killer.pips >= Balance.MAX_PIPS) Balance.NOVA_MAX else 1f
                        blast(at, Balance.NOVA_RADIUS * boost, Combat.baseDamage(DieType.NOVA, killer.pips, powerMult(DieType.NOVA)) * boost, killer, DieType.NOVA)
                    }
                    else -> Unit
                }
            }
        }
    }

    companion object {
        private const val SNAPSHOT_VERSION = 4

        /** An ordinary match whose seed is drawn from [rng] and remembered, so it can be shared as a code. */
        fun seeded(deck: List<DieType>, map: GameMap, mode: GameMode, mutators: Set<Mutator> = emptySet(), dieLevels: Map<DieType, Int> = emptyMap(), rng: Random = Random.Default): Match {
            val seed = rng.nextInt()
            return Match(deck, Random(seed), map, mode = mode, seed = seed, mutators = mutators, dieLevels = dieLevels)
        }

        /** Rebuilds a match from [snapshot] text; null if the text is damaged or from another version. */
        fun restore(text: String, rng: Random = Random.Default): Match? = try {
            parse(text, rng)
        } catch (e: RuntimeException) {
            Log.warn("match", "Saved match rejected", mapOf("chars" to text.length), e)
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
            val mode = scalars["mode"]?.let { requireNotNull(GameMode.byId(it)) { "unknown mode $it" } } ?: GameMode.NORMAL
            val seed = scalars["seed"]?.takeIf { it.isNotEmpty() }?.toInt()
            val locked = scalars["locked"]?.toBooleanStrict() ?: false
            val mutators = Mutator.fromMask(scalars["mutators"]?.toInt() ?: 0)
            val week = scalars["week"]?.toInt()?.takeIf { it >= 0 }
            // Saves from before die levels have no "levels" line: every die at 1.
            val levels = (scalars["levels"] ?: "").split(",").filter { it.isNotEmpty() }.associate { p ->
                val (id, level) = p.split(":")
                type(id) to level.toInt()
            }
            val m = Match(scalars.getValue("deck").split(",").map(::type), rng, map, daily, mode, seed, locked, mutators, week, levels)
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
                        speedScale = (if (mode == GameMode.RUSH) Balance.RUSH_SPEED else 1f) * (if (Mutator.SWIFT in mutators) Balance.SWIFT_SPEED else 1f)
                        for (stack in f.drop(14)) {
                            val (dps, remaining) = stack.split(":")
                            poison += PoisonStack(dps.toFloat(), remaining.toFloat())
                        }
                    }
                    "trap" -> m.trapList += Trap(f[0].toFloat(), f[1].toFloat(), null, f[2].toFloat())
                }
            }
            return m
        }
    }
}
