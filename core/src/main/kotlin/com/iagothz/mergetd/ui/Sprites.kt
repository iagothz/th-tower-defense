package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * Rendered sprite sheets (assets/sprites). The fire kitten sheets have one row per facing, in [FireCats.ROWS]
 * order, and one column per frame; the fireball and the impact are single rows.
 */
class Sprites : Disposable {
    private val textures = mutableListOf<Texture>()

    private fun sheet(name: String, cols: Int, rows: Int): List<List<TextureRegion>> {
        val t = Texture(Gdx.files.internal("sprites/$name.png")).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        textures += t
        val w = t.width / cols
        val h = t.height / rows
        return List(rows) { r -> List(cols) { c -> TextureRegion(t, c * w, r * h, w, h) } }
    }

    private val catIdle = sheet("fire_cat_idle", FireCats.IDLE_FRAMES, FireCats.ROWS.size)
    private val catAttack = sheet("fire_cat_attack", FireCats.ATTACK_FRAMES, FireCats.ROWS.size)
    private val fireball = sheet("fireball", FIREBALL_FRAMES, 1)[0]
    private val impact = sheet("fire_impact", FireCats.BOOM_FRAMES, 1)[0]

    fun cat(f: FireCats.Frame): TextureRegion = (if (f.attacking) catAttack else catIdle)[f.row][f.index]
    fun fireball(clock: Float): TextureRegion = fireball[(clock * FIREBALL_FPS).toInt() % FIREBALL_FRAMES]
    fun impact(index: Int): TextureRegion = impact[index]

    override fun dispose() = textures.forEach { it.dispose() }

    companion object {
        private const val FIREBALL_FRAMES = 6
        private const val FIREBALL_FPS = 14f
        /** Where the kitten's feet are in its frame, from the bottom, as a fraction of the frame height. */
        const val CAT_FOOT = 1f - 0.828f
        /** The fireball's bright core in its frame (from the left, from the bottom); the tail trails to the left. */
        const val BALL_CORE_X = 0.746f
        const val BALL_CORE_Y = 1f - 0.498f
    }
}
