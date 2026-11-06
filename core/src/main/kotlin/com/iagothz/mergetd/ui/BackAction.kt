package com.iagothz.mergetd.ui

/** What kind of screen the back key is pressed on. [BLOCKING] screens (the cloud conflict) ignore it. */
enum class ScreenKind { HOME, TAB, SUBSCREEN, MATCH, GAME_OVER, BLOCKING }

/** The screen's state as far as the back key cares: a window over it, the pause, a pending confirmation. */
data class BackState(
    val kind: ScreenKind,
    val overlayOpen: Boolean = false,
    val paused: Boolean = false,
    val confirming: Boolean = false,
)

enum class BackAction { CLOSE_OVERLAY, OPEN_PAUSE, RESUME, CANCEL_CONFIRM, TO_PARENT, TO_HOME, EXIT, NOTHING }

/** Android back, one rule per situation: undo the innermost thing, go up one level, and only leave from Início. */
fun backAction(s: BackState): BackAction = when {
    s.kind == ScreenKind.BLOCKING -> BackAction.NOTHING
    s.overlayOpen -> BackAction.CLOSE_OVERLAY
    s.kind == ScreenKind.MATCH -> when {
        s.confirming -> BackAction.CANCEL_CONFIRM
        s.paused -> BackAction.RESUME
        else -> BackAction.OPEN_PAUSE
    }
    s.kind == ScreenKind.GAME_OVER -> BackAction.TO_HOME
    s.kind == ScreenKind.SUBSCREEN -> BackAction.TO_PARENT
    s.kind == ScreenKind.TAB -> BackAction.TO_HOME
    else -> BackAction.EXIT
}
