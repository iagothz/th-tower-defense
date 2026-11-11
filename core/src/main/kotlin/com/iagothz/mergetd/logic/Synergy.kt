package com.iagothz.mergetd.logic

/** A bonus that switches on while both dice types stand on the board (not silenced). */
enum class Synergy(val a: DieType, val b: DieType, val title: String, val description: String) {
    BLAZE(DieType.FIRE, DieType.WIND, "Incêndio", "raio da explosão do Ignis +50%"),
    SHATTER(DieType.ICE, DieType.QUAKE, "Estilhaço", "Terremoto dá dano ×2 em inimigos lentos"),
    PLAGUE(DieType.POISON, DieType.CURSE, "Praga", "veneno +50% de dano por segundo"),
    DAZZLE(DieType.LIGHT, DieType.CRIT, "Ofuscar", "+10% de chance de crítico"),
    SHOCK(DieType.ELECTRIC, DieType.TIME, "Choque", "cada salto do raio congela 0,2 s"),
    PIERCE(DieType.IRON, DieType.SNIPER, "Perfurante", "Ferro e Sniper ignoram armadura e dão +25% em chefes"),
    PREDATOR(DieType.HUNTER, DieType.VAMPIRE, "Predador", "Caçador e Vampiro +25% de dano"),
    AMBUSH(DieType.GRAVITY, DieType.TRAP, "Emboscada", "raio de disparo das minas ×2"),
    ;

    fun needs(t: DieType): Boolean = t == a || t == b

    companion object {
        /** The synergies whose two types are both in [types]. */
        fun active(types: Collection<DieType>): Set<Synergy> {
            val present = types.toSet()
            return entries.filter { it.a in present && it.b in present }.toSet()
        }

        /** What a deck can switch on at all. */
        fun forDeck(deck: List<DieType>): Set<Synergy> = active(deck)
    }
}
