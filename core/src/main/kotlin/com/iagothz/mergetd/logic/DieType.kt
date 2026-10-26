package com.iagothz.mergetd.logic

enum class DieType(val id: String, val attacks: Boolean = true) {
    FIRE("fire"),
    ICE("ice"),
    POISON("poison"),
    ELECTRIC("electric"),
    WIND("wind"),
    IRON("iron"),
    MINE("mine", attacks = false),
    SNIPER("sniper"),
    MIRROR("mirror", attacks = false),
    GROWTH("growth"),
    ;

    companion object {
        fun byId(id: String): DieType? = entries.firstOrNull { it.id == id }
    }
}
