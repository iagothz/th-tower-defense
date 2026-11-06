package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class BackActionTest {
    @Test
    fun overlayWinsOverEveryKind() {
        for (k in listOf(ScreenKind.HOME, ScreenKind.TAB, ScreenKind.SUBSCREEN)) {
            assertEquals(BackAction.CLOSE_OVERLAY, backAction(BackState(k, overlayOpen = true)), "$k")
        }
    }

    @Test
    fun matchRules() {
        assertEquals(BackAction.OPEN_PAUSE, backAction(BackState(ScreenKind.MATCH)))
        assertEquals(BackAction.RESUME, backAction(BackState(ScreenKind.MATCH, paused = true)))
        assertEquals(BackAction.CANCEL_CONFIRM, backAction(BackState(ScreenKind.MATCH, paused = true, confirming = true)))
    }

    @Test
    fun screensWithoutOverlays() {
        assertEquals(BackAction.TO_HOME, backAction(BackState(ScreenKind.GAME_OVER)))
        assertEquals(BackAction.TO_PARENT, backAction(BackState(ScreenKind.SUBSCREEN)))
        assertEquals(BackAction.TO_HOME, backAction(BackState(ScreenKind.TAB)))
        assertEquals(BackAction.EXIT, backAction(BackState(ScreenKind.HOME)))
    }

    @Test
    fun blockingIgnoresEverything() {
        assertEquals(BackAction.NOTHING, backAction(BackState(ScreenKind.BLOCKING)))
        assertEquals(BackAction.NOTHING, backAction(BackState(ScreenKind.BLOCKING, overlayOpen = true, paused = true)))
    }
}
