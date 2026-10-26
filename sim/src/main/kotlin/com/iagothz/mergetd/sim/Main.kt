package com.iagothz.mergetd.sim

import com.iagothz.mergetd.logic.DeckCodec
import com.iagothz.mergetd.logic.DieType
import java.io.File
import java.util.stream.Collectors
import java.util.stream.LongStream
import kotlin.random.Random

/**
 * Headless balance report.
 *   ./gradlew :sim:run --args="--games 300 --bot all --decks default"
 * Options: --games N, --bot simple|smart|all, --decks default|random, --seed S.
 */
fun main(args: Array<String>) {
    val opts = args.toList().chunked(2).associate { it[0].removePrefix("--") to it.getOrElse(1) { "" } }
    val games = opts["games"]?.toInt() ?: 300
    val seed = opts["seed"]?.toLong() ?: 1L
    val randomDecks = opts["decks"] == "random"
    val bots = when (val b = opts["bot"] ?: "all") {
        "all" -> BOTS.keys.toList()
        else -> listOf(b)
    }

    val csv = StringBuilder("bot,seed,deck,wave,killed_by\n")
    for (name in bots) {
        val bot = BOTS.getValue(name)
        val started = System.nanoTime()
        val results = LongStream.range(seed, seed + games).parallel().mapToObj { s ->
            val deck = if (randomDecks) DieType.entries.shuffled(Random(s)).take(DeckCodec.SIZE) else DeckCodec.DEFAULT
            Simulator.play(deck, bot, s)
        }.collect(Collectors.toList())
        val secs = (System.nanoTime() - started) / 1e9
        printReport(name, results, randomDecks, secs)
        results.forEach { r ->
            csv.append("$name,${r.seed},${DeckCodec.encode(r.deck).replace(',', ' ')},${r.wave},${r.killedBy ?: ""}\n")
        }
    }
    val out = File("sim/build/sim-results.csv")
    out.parentFile.mkdirs()
    out.writeText(csv.toString())
    println("CSV: ${out.path}")
}

private fun printReport(bot: String, results: List<GameResult>, randomDecks: Boolean, secs: Double) {
    val waves = results.map { it.wave }
    val deck = if (randomDecks) "sorteados" else DeckCodec.encode(DeckCodec.DEFAULT)
    println()
    println("== Bot: $bot | decks: $deck | ${results.size} partidas | ${"%.1f".format(secs)} s ==")
    println(
        "Onda final: p10 ${Stats.percentile(waves, 10)} | mediana ${Stats.percentile(waves, 50)} | " +
            "p90 ${Stats.percentile(waves, 90)} | maior ${waves.max()}",
    )
    println("Partidas encerradas na onda <= " + listOf(1, 3, 5, 10, 15, 20, 30).joinToString("  ") { w ->
        "$w: ${pct(Stats.deathRateBy(waves, w))}"
    })
    val killers = results.mapNotNull { it.killedBy }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    println("Inimigo que encerrou: " + killers.joinToString(" | ") { "${it.key} ${pct(it.value.toDouble() / results.size)}" })
    if (randomDecks) {
        println("Mediana da onda final com/sem cada tipo no deck:")
        for (t in DieType.entries) {
            val (with, without) = results.partition { t in it.deck }
            if (with.isEmpty() || without.isEmpty()) continue
            val w = Stats.percentile(with.map { it.wave }, 50)
            val wo = Stats.percentile(without.map { it.wave }, 50)
            println("  ${t.id.padEnd(9)} com $w | sem $wo | ${"%+d".format(w - wo)}")
        }
    }
}

private fun pct(x: Double) = "${"%.0f".format(x * 100)}%"
