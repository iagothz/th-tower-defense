package com.iagothz.mergetd.ui

/** Which vibration a frame's sounds deserve: short and soft for a merge, long and hard for a leak. */
object Haptics {
    data class Pulse(val millis: Int, val amplitude: Int)

    val MERGE = Pulse(15, 60)

    private val BY_SOUND = mapOf(
        "leak" to Pulse(80, 255),
        "gameover" to Pulse(120, 255),
        "bosskill" to Pulse(70, 255),
        "max" to Pulse(40, 180),
        "record" to Pulse(60, 160),
        "boss" to Pulse(50, 130),
        "deny" to Pulse(20, 90),
    )

    /** The strongest pulse among [names], or null when nothing in the frame is worth a buzz. */
    fun pulse(names: Set<String>): Pulse? =
        names.mapNotNull { BY_SOUND[it] ?: MERGE.takeIf { _ -> it in SoundPlanner.MERGES } }
            .maxByOrNull { it.amplitude * 1000 + it.millis }
}
