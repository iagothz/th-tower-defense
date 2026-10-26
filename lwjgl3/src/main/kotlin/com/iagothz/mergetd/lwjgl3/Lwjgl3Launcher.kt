package com.iagothz.mergetd.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.iagothz.mergetd.ui.MergeTdGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Merge TD")
        setWindowedMode(405, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(MergeTdGame(), config)
}
