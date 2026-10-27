package com.iagothz.mergetd.android

import android.content.Intent
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.iagothz.mergetd.ui.MergeTdGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opened from the installer's "Open" button, the launcher icon would start a second copy of the game
        // on top of the running one; send the user back to the running one instead.
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action == Intent.ACTION_MAIN) {
            finish()
            return
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(MergeTdGame(), config)
    }
}
