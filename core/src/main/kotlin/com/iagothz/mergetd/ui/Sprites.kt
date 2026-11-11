package com.iagothz.mergetd.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * Rendered sprite sheets (assets/sprites). The fire kitten sheets (fire_catN_idle / fire_catN_attack for N pips)
 * have one row per facing, in [FireCats.ROWS] order, and one column per frame; the fireball and the impact are
 * single rows.
 */
class Sprites : Disposable {
    private val textures = mutableListOf<Texture>()

    private fun sheet(name: String, cols: Int, rows: Int): List<List<TextureRegion>> {
        val t = Texture(Gdx.files.internal("sprites/$name.png")).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        textures += t
        val w = t.width / cols
        val h = t.height / rows // frames need not be square
        return List(rows) { r -> List(cols) { c -> TextureRegion(t, c * w, r * h, w, h) } }
    }

    private class CatSheets(val idle: List<List<TextureRegion>>, val attack: List<List<TextureRegion>>)

    /**
     * Each pip level's sheets load the first time a kitten of that level shows up ([prepareCat]), outside any
     * SpriteBatch pass: creating a texture binds it, which a batch in progress would not notice.
     */
    private val cats = HashMap<Int, CatSheets>()

    /** Frame size and foot of each kitten level (each is cropped around the cat on its own). */
    val catMetrics: CatMetrics = CatMetrics.parse(Gdx.files.internal("sprites/fire_cats.properties").readString("UTF-8"))
    private val fireball = sheet("fireball", FIREBALL_FRAMES, 1)[0]
    private val impact = sheet("fire_impact", FireCats.BOOM_FRAMES, 1)[0]

    fun prepareCat(pips: Int) {
        cats.getOrPut(pips) {
            CatSheets(
                sheet("fire_cat${pips}_idle", FireCats.IDLE_FRAMES, FireCats.ROWS.size),
                sheet("fire_cat${pips}_attack", FireCats.ATTACK_FRAMES, FireCats.ROWS.size),
            )
        }
    }

    fun cat(pips: Int, f: FireCats.Frame): TextureRegion {
        val s = cats.getValue(pips)
        return (if (f.attacking) s.attack else s.idle)[f.row][f.index]
    }

    fun fireball(clock: Float): TextureRegion = fireball[(clock * FIREBALL_FPS).toInt() % FIREBALL_FRAMES]
    fun impact(index: Int): TextureRegion = impact[index]

    override fun dispose() = textures.forEach { it.dispose() }

    companion object {
        /** Fire dice up to this many pips have kitten art (one pair of sheets per level). */
        const val CAT_PIPS = 7
        private const val FIREBALL_FRAMES = 6
        private const val FIREBALL_FPS = 14f

        /** The fireball's bright core in its frame (from the left, from the bottom); the tail trails to the left. */
        const val BALL_CORE_X = 0.746f
        const val BALL_CORE_Y = 1f - 0.498f
    }
}
