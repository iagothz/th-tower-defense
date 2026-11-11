package com.iagothz.mergetd.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.iagothz.mergetd.log.FileLogSink
import com.iagothz.mergetd.log.GdxLogSink
import com.iagothz.mergetd.log.Log
import com.iagothz.mergetd.log.TeeLogSink
import com.iagothz.mergetd.ui.MergeTdGame
import java.io.File

fun main(args: Array<String>) {
    Log.sink = TeeLogSink(listOf(GdxLogSink, FileLogSink(File(System.getProperty("user.home"), ".merge-td/logs"))))
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Merge TD")
        setWindowedMode(405, 720)
        useVsync(true)
        setForegroundFPS(60)
        // 4x MSAA: everything is drawn with ShapeRenderer, so edges would be jagged without it.
        setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
    }
    Lwjgl3Application(MergeTdGame(devMode = "--dev" in args), config)
}
