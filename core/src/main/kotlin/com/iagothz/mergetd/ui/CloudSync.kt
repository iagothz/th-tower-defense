package com.iagothz.mergetd.ui

/**
 * What to do with the local save and the cloud's: the one that moved ahead wins on its own, and only when both
 * moved since they last agreed does the player get asked. The local save remembers the cloud version it last
 * matched ([localVersion]) and whether its content changed since ([localChanged]).
 */
object CloudSync {
    sealed interface Action {
        data object Nothing : Action

        /** Upload the local save as this cloud version. */
        data class Upload(val version: Int) : Action

        /** Take the cloud save silently. */
        data object Adopt : Action

        /** Both moved: show the conflict screen. */
        data object Ask : Action
    }

    fun decide(localVersion: Int, localChanged: Boolean, cloudVersion: Int?): Action = when {
        cloudVersion == null -> Action.Upload(1)
        cloudVersion > localVersion -> if (localChanged) Action.Ask else Action.Adopt
        cloudVersion < localVersion || localChanged -> Action.Upload(localVersion + 1)
        else -> Action.Nothing
    }
}
