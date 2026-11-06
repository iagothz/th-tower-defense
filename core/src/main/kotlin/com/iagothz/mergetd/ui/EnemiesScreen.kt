package com.iagothz.mergetd.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.iagothz.mergetd.logic.EnemyKind
import com.iagothz.mergetd.logic.Vec2

/** Every enemy and boss: shape, numbers, what it does, and how to beat it. Tap a row for the advice. */
class EnemiesScreen(game: MergeTdGame) : BaseScreen(game) {
    override val tab get() = Tab.DECK

    private val rows = EnemyKind.entries.mapIndexed { i, k -> k to Rectangle(24f, 1082f - i * 55f, 672f, 50f) }
    private val panel = Rectangle(24f, 200f, 672f, 150f)
    private val header = ScreenHeader(S["Inimigos"], back = true)
    private val tap = Tap<ScreenHeader>()
    private var selected: EnemyKind = EnemyKind.NORMAL

    override fun draw(delta: Float) {
        header.pressed = tap.current === header
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((k, r) in rows) {
            shapes.color = if (k == selected) Skin.ROW_SELECTED else Skin.PANEL
            shapes.roundedRect(r.x, r.y, r.width, r.height, 14f)
            val radius = Skin.enemyRadius(k) * 0.7f
            val cx = r.x + 40f
            val cy = r.y + r.height / 2
            shapes.color = Color(Skin.color(k)).mul(0.4f, 0.4f, 0.45f, 1f)
            shapes.polygon(cx, cy, radius + 3f, Skin.enemySides(k))
            shapes.color = Skin.color(k)
            shapes.polygon(cx, cy, radius, Skin.enemySides(k))
            if (k.isBoss) {
                shapes.color = Color(Skin.color(k)).lerp(Color.WHITE, 0.3f)
                shapes.circle(cx, cy, radius * 0.5f, 16)
            }
        }
        shapes.color = Skin.SHADOW
        shapes.roundedRect(panel.x + 3f, panel.y - 8f, panel.width, panel.height, 20f)
        shapes.color = Skin.PANEL
        shapes.roundedRect(panel.x, panel.y, panel.width, panel.height, 20f)
        shapes.color = Skin.color(selected)
        shapes.roundedRect(panel.x, panel.y + panel.height - 10f, panel.width, 10f, 5f)
        header.drawShapes(shapes)
        shapes.end()

        game.batch.begin()
        header.drawText(game.batch, game)
        for ((k, r) in rows) {
            game.batch.textLeft(game.smallFont, Skin.enemyName(k), r.x + 80f, r.y + r.height / 2 + 12f)
            game.tinyFont.color = Skin.MUTED
            game.batch.textLeft(game.tinyFont, description(k), r.x + 80f, r.y + r.height / 2 - 12f)
            game.batch.textRight(game.tinyFont, stats(k), r.x + r.width - 16f, r.y + r.height / 2 + 12f)
            game.tinyFont.color = Color.WHITE
        }
        game.smallFont.color = Skin.GOLD
        game.batch.textLeft(game.smallFont, S["Como vencer"], panel.x + 24f, panel.y + panel.height - 34f)
        game.smallFont.color = Color.WHITE
        game.tinyFont.draw(game.batch, Tips.forKiller(selected), panel.x + 24f, panel.y + panel.height - 62f, panel.width - 48f, Align.left, true)
        game.batch.end()
    }

    /** "vida ×3 · velocidade ×0,6 · 20 SP · tira 1" */
    private fun stats(k: EnemyKind): String =
        S.f("vida ×%s · vel. ×%s · %d SP · tira %d", DieInfo.num(k.hpMult), DieInfo.num(k.speedMult), k.reward, k.lifeDamage)

    private fun description(k: EnemyKind): String = if (k.isBoss) Skin.bossAbility(k) else S[DESCRIPTIONS.getValue(k)]

    override fun onTouchDown(p: Vec2) = tap.press(header.takeIf { it.backHit(p) })

    override fun onTouchCancelled() = tap.cancel()

    override fun onTouchUp(p: Vec2) {
        rows.firstOrNull { it.second.contains(p.x, p.y) }?.let { selected = it.first }
        if (tap.release(header.takeIf { it.backHit(p) }) === header) goBack()
    }

    fun goBack() {
        game.showDeck()
    }

    companion object {
        val DESCRIPTIONS = mapOf(
            EnemyKind.NORMAL to "O básico: vem em grupo.",
            EnemyKind.FAST to "Rápido e frágil.",
            EnemyKind.TANK to "Lento, com muita vida.",
            EnemyKind.HEALER to "Cura os inimigos ao redor.",
            EnemyKind.GHOST to "Ignora lentidão, atordoamento, empurrão e armadilhas.",
            EnemyKind.ELITE to "Grande, cura a escolta e vale 30 SP.",
            EnemyKind.TREASURE to "Foge rápido; não tira vida, mas vale 60 SP.",
        )
    }
}
