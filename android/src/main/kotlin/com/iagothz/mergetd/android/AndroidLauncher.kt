package com.iagothz.mergetd.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.gms.games.PlayGamesSdk
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
            // 4x MSAA: everything is drawn with ShapeRenderer, so edges would be jagged without it.
            numSamples = 4
        }
        // Initializing the SDK triggers Google's automatic sign-in (and, with no account on the device, its
        // add-account screen), so it only happens while the player keeps the cloud on; PlayGamesCloud does it
        // on demand if the cloud is turned on later.
        if (getSharedPreferences("merge-td", MODE_PRIVATE).getBoolean("sync.enabled", true)) PlayGamesSdk.initialize(this)
        val game = MergeTdGame(
            shareText = { text ->
                runOnUiThread {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(send, null))
                }
            },
            cloudSave = PlayGamesCloud(this),
        )
        initialize(game, config)
        // Android 16 stops sending the back key to apps that target API 36 and uses this callback instead; on 13-15
        // the callback is ignored (no enableOnBackInvokedCallback in the manifest) and the key still arrives.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                Gdx.app.postRunnable { game.back() }
            }
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: NullPointerException) {
            // Finished in onCreate before initialize(): libGDX has no keyboard-height provider to close.
        }
    }
}
