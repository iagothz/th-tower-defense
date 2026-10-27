package com.iagothz.mergetd.logic

/** Geometry of the 720×1280 virtual world (origin bottom-left, y up). */
object Layout {
    const val WORLD_W = 720f
    const val WORLD_H = 1280f
    const val CELL = 104f
    const val GAP = 8f
    const val GRID_X = 84f
    const val GRID_Y = 464f

    /** The default map's path; a running match uses [Match.path], which depends on its [GameMap]. */
    val PATH: EnemyPath get() = GameMap.DEFAULT.path

    fun cellCenter(c: Cell): Vec2 = Vec2(
        GRID_X + c.col * (CELL + GAP) + CELL / 2,
        GRID_Y + (Balance.ROWS - 1 - c.row) * (CELL + GAP) + CELL / 2,
    )

    /** The cell whose square contains [p], or null when [p] is outside the grid or in a gap. */
    fun cellAt(p: Vec2): Cell? {
        val lx = p.x - GRID_X
        val ly = p.y - GRID_Y
        if (lx < 0f || ly < 0f) return null
        val col = (lx / (CELL + GAP)).toInt()
        val rowFromBottom = (ly / (CELL + GAP)).toInt()
        if (col >= Balance.COLS || rowFromBottom >= Balance.ROWS) return null
        if (lx - col * (CELL + GAP) > CELL || ly - rowFromBottom * (CELL + GAP) > CELL) return null
        return Cell(Balance.ROWS - 1 - rowFromBottom, col)
    }
}
