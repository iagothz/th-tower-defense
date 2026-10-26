package com.iagothz.mergetd.ui

import com.badlogic.gdx.Game
import com.badlogic.gdx.utils.ScreenUtils

class MergeTdGame : Game() {
    override fun create() {}

    override fun render() {
        ScreenUtils.clear(0.08f, 0.09f, 0.12f, 1f)
        super.render()
    }
}
