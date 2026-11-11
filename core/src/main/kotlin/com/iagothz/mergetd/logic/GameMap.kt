package com.iagothz.mergetd.logic

enum class Difficulty(val label: String) { EASY("Fácil"), NORMAL("Normal"), HARD("Difícil") }

/**
 * A map is a path around the board. The board, HUD and buttons never move, so every path threads the
 * free strips: x=44 and x=676 beside the board, y=880..1020 above it, y=245 below it.
 * [difficulty] is measured in the simulator with the default deck (see docs), not derived from length.
 */
enum class GameMap(val id: String, val title: String, val difficulty: Difficulty, points: List<Vec2>) {
    /** Up the left, across the top, down the right. */
    HORSESHOE(
        "horseshoe",
        "Ferradura",
        Difficulty.NORMAL,
        listOf(Vec2(44f, 180f), Vec2(44f, 856f), Vec2(676f, 856f), Vec2(676f, 180f)),
    ),

    /** Two passes across the top strip, then down the left. */
    SERPENT(
        "serpent",
        "Serpente",
        Difficulty.NORMAL,
        listOf(Vec2(44f, 1020f), Vec2(676f, 1020f), Vec2(676f, 880f), Vec2(44f, 880f), Vec2(44f, 180f)),
    ),

    /** Almost a full lap: from the top centre, down the right, under the board, up the left, back to the top. */
    RING(
        "ring",
        "Anel",
        Difficulty.EASY,
        listOf(Vec2(360f, 1020f), Vec2(676f, 1020f), Vec2(676f, 245f), Vec2(44f, 245f), Vec2(44f, 880f), Vec2(360f, 880f)),
    ),

    /** Across the top and straight down the right. */
    STRAIGHT(
        "straight",
        "Reta",
        Difficulty.HARD,
        listOf(Vec2(44f, 880f), Vec2(676f, 880f), Vec2(676f, 180f)),
    ),
    ;

    val path = EnemyPath(points)

    companion object {
        val DEFAULT = HORSESHOE

        fun byId(id: String): GameMap? = entries.firstOrNull { it.id == id }
    }
}
