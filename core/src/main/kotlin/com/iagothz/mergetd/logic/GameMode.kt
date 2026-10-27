package com.iagothz.mergetd.logic

/** How a match is played; records are kept per map and mode. Numbers live in [Balance]. */
enum class GameMode(val id: String, val title: String, val description: String) {
    NORMAL("normal", "Normal", "Ondas infinitas, chefe a cada 10"),
    BOSS_RUSH("bossrush", "Boss Rush", "Só chefes, um por onda, com menos vida"),
    SURVIVAL("survival", "Sobrevivência", "Uma vida só; SP em dobro"),
    RUSH("rush", "Rápido", "Ondas emendadas, inimigos mais rápidos e mais frágeis"),
    ;

    companion object {
        fun byId(id: String): GameMode? = entries.firstOrNull { it.id == id }
    }
}
