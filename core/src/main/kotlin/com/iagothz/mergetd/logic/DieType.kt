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
    MIRROR("mirror"),
    GROWTH("growth"),
    LIGHT("light"),
    CRIT("crit"),
    COMBO("combo"),
    CURSE("curse"),
    HUNTER("hunter"),
    GRAVITY("gravity"),
    TIME("time"),
    TRAP("trap"),
    QUAKE("quake"),
    REAPER("reaper"),
    NOVA("nova"),
    LUCK("luck"),
    VAMPIRE("vampire"),
    ;

    companion object {
        fun byId(id: String): DieType? = entries.firstOrNull { it.id == id }
    }
}
