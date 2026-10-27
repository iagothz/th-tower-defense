package com.iagothz.mergetd.logic

/** [wildcard] dice pair with any type of the same pips: the Mirror rolls, the Boost keeps the other's type. */
enum class DieType(val id: String, val attacks: Boolean = true, val wildcard: Boolean = false) {
    FIRE("fire"),
    ICE("ice"),
    POISON("poison"),
    ELECTRIC("electric"),
    WIND("wind"),
    IRON("iron"),
    MINE("mine", attacks = false),
    SNIPER("sniper"),
    MIRROR("mirror", wildcard = true),
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
    CLONE("clone"),
    BOOST("boost", wildcard = true),
    ;

    companion object {
        fun byId(id: String): DieType? = entries.firstOrNull { it.id == id }
    }
}
