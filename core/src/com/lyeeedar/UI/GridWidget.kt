package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTime
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Orb
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Point

/**
 * Created by Philip on 05-Jul-16.
 */

class GridWidget(val grid: Grid) : Widget()
{
	var tileSize = 32f
		set(value)
		{
			field = value
			ground.tileSize = value
			floating.tileSize = value
		}

	val glow: Sprite = AssetManager.loadSprite("glow")
	val frame: Sprite = AssetManager.loadSprite("GUI/frame", colour = Colour(Color(0.6f, 0.7f, 0.9f, 0.6f)))
	val border: Sprite = AssetManager.loadSprite("GUI/border", colour = Colour(Color(0.6f, 0.9f, 0.6f, 0.6f)))
	val hp_full: Sprite = AssetManager.loadSprite("GUI/health_full")
	val hp_dr: Sprite = AssetManager.loadSprite("GUI/health_DR")
	val hp_damaged: Sprite = AssetManager.loadSprite("GUI/health_damaged")
	val hp_full_friendly: Sprite = AssetManager.loadSprite("GUI/health_full_green")
	val hp_full_summon: Sprite = AssetManager.loadSprite("GUI/health_full_blue")
	val hp_empty: Sprite = AssetManager.loadSprite("GUI/health_empty")
	val atk_full: Sprite = AssetManager.loadSprite("GUI/attack_full")
	val atk_empty: Sprite = AssetManager.loadSprite("GUI/attack_empty")
	val changer: Sprite = AssetManager.loadSprite("Oryx/Custom/items/changer", drawActualSize = true)

	val TILE = 0
	val ORB = 1
	val EFFECT = 2

	val ground = SortedRenderer(tileSize, grid.width.toFloat(), grid.height.toFloat(), 3, true)
	val floating = SortedRenderer(tileSize, grid.width.toFloat(), grid.height.toFloat(), 3, true)

	val tempCol = Colour()

	var hpLossTimer = 0f

	init
	{
		instance = this

		touchable = Touchable.enabled

		addListener(object : InputListener()
		{
			override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
			{
				val xp = x + ((grid.width * tileSize) / 2f) - (width / 2f)

				val sx = (xp / tileSize).toInt()
				val sy = (grid.height-1) - (y / tileSize).toInt()

				grid.select(Point(sx, sy))

				return true
			}

			override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
			{
				grid.clearDrag()

				super.touchUp(event, x, y, pointer, button)
			}

			override fun touchDragged (event: InputEvent?, x: Float, y: Float, pointer: Int)
			{
				val xp = x + ((grid.width * tileSize) / 2f) - (width / 2f)

				val sx = (xp / tileSize).toInt()
				val sy = (grid.height - 1) - (y / tileSize).toInt()

				val point = Point(sx, sy)

				if (point != grid.dragStart)
				{
					grid.dragEnd(point)
				}
			}

			override fun keyTyped(event: InputEvent?, character: Char): Boolean
			{
				if (character == 'd')
				{
					ground.debugDraw = !ground.debugDraw
				}

				return false
			}
		})

		atk_empty.baseScale = floatArrayOf(0.14f, 0.14f)
		atk_full.baseScale = floatArrayOf(0.14f, 0.14f)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		val xp = x + (width / 2f) - ((grid.width * tileSize) / 2f)

		return Vector2(xp + point.x * tileSize, renderY + ((grid.height - 1) - point.y) * tileSize)
	}

	override fun invalidate()
	{
		super.invalidate()

		val w = width / grid.width.toFloat()
		val h = (height - 16f) / grid.height.toFloat()

		tileSize = Math.min(w, h)
	}

	var renderY = 0f
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		val xp = this.x + (this.width / 2f) - ((grid.width * tileSize) / 2f)
		val yp = this.y
		renderY = yp

		ground.begin(Gdx.app.graphics.deltaTime, xp, yp)
		floating.begin(Gdx.app.graphics.deltaTime, xp, yp)

		if (grid.activeAbility == null)
		{
			batch!!.color = Color.WHITE
		}

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val orb = tile.swappable
				val block = tile.block
				val chest = tile.chest
				val monster = tile.monster
				val friendly = tile.friendly

				var tileColour = Colour.WHITE
				var orbColour = Colour.WHITE
				var blockColour = Colour.WHITE
				var monsterColour = Colour.WHITE

				if (grid.activeAbility != null)
				{
					if (grid.activeAbility!!.targetter.isValid(tile, grid.activeAbility!!.data))
					{
						if (grid.activeAbility!!.targetter.type == Targetter.Type.ORB)
						{
							tileColour = Colour.DARK_GRAY
							orbColour = Colour.WHITE
							blockColour = Colour.DARK_GRAY
							monsterColour = Colour.DARK_GRAY
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.BLOCK)
						{
							tileColour = Colour.DARK_GRAY
							orbColour = Colour.DARK_GRAY
							blockColour = Colour.WHITE
							monsterColour = Colour.DARK_GRAY
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.EMPTY)
						{
							tileColour = Colour.WHITE
							orbColour = Colour.DARK_GRAY
							blockColour = Colour.DARK_GRAY
							monsterColour = Colour.DARK_GRAY
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.MONSTER)
						{
							tileColour = Colour.DARK_GRAY
							orbColour = Colour.DARK_GRAY
							blockColour = Colour.DARK_GRAY
							monsterColour = Colour.WHITE
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.ATTACK)
						{
							tileColour = Colour.DARK_GRAY
							orbColour = Colour.WHITE
							blockColour = Colour.DARK_GRAY
							monsterColour = Colour.DARK_GRAY
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.TILE)
						{
							val col = if (tile.canHaveOrb) Colour.WHITE else Colour.DARK_GRAY

							tileColour = col
							orbColour = col
							blockColour = col
							monsterColour = col
						}
						else if (grid.activeAbility!!.targetter.type == Targetter.Type.SEALED)
						{
							tileColour = Colour.DARK_GRAY
							orbColour = if (orb != null && orb.sealed) Colour.WHITE else Colour.DARK_GRAY
							blockColour = Colour.DARK_GRAY
							monsterColour = Colour.DARK_GRAY
						}
					}
					else
					{
						tileColour = Colour.DARK_GRAY
						orbColour = Colour.DARK_GRAY
						blockColour = Colour.DARK_GRAY
						monsterColour = Colour.DARK_GRAY
					}
				}

				val xi = x.toFloat()
				val yi = (grid.height-1) - y.toFloat()

				var tileHeight = 0
				for (sprite in tile.sprites)
				{
					if (sprite.sprite != null)
					{
						ground.queueSprite(sprite.sprite!!, xi, yi, TILE, tileHeight, tileColour)
					}
					if (sprite.tilingSprite != null)
					{
						val tiling = sprite.tilingSprite!!
						ground.queueSprite(tiling, xi, yi, TILE, tileHeight, tileColour)
					}

					tileHeight++
				}

				if (tile.hasPlate)
				{
					ground.queueSprite(grid.level.theme.plate, xi, yi, TILE, tileHeight, tileColour)

					tileHeight++
				}

				if (chest != null)
				{
					ground.queueSprite(chest.sprite, xi, yi, TILE, tileHeight, tileColour)
				}

				for (effect in tile.effects)
				{
					if (effect is Sprite)
					{
						if (effect.completed)
						{
							tile.effects.removeValue(effect, true)
						}
						else
						{
							floating.queueSprite(effect, xi, yi, EFFECT, 0)
						}
					}
					else if (effect is ParticleEffect)
					{
						if (effect.completed)
						{
							tile.effects.removeValue(effect, true)
						}
						else
						{
							floating.queueParticle(effect, xi, yi, EFFECT, 0)
						}
					}
				}

				if (orb != null)
				{
					ground.queueSprite(orb.sprite, xi, yi, ORB, 1, orbColour)

					if (orb.sealed)
					{
						ground.queueSprite(orb.sealSprite, xi, yi, ORB, 2, orbColour)
					}

					if (orb is Orb && orb.isChanger)
					{
						if (orb.sprite.visible && (orb.sprite.showBeforeRender || orb.sprite.renderDelay <= 0))
						{
							val offset = orb.sprite.animation?.renderOffset(false)

							var xii = xi
							var yii = yi

							if (offset != null)
							{
								xii += offset[0]
								yii += offset[1]
							}

							tempCol.set(orbColour).mul(orb.nextDesc!!.sprite.colour)

							var scaleX = orb.sprite.baseScale[0]
							var scaleY = orb.sprite.baseScale[1]

							val scale = orb.sprite.animation?.renderScale()

							if (scale != null)
							{
								scaleX *= scale[0]
								scaleY *= scale[1]
							}

							ground.queueSprite(changer, xii, yii, ORB, 2, tempCol, scaleX = scaleX, scaleY = scaleY)
						}
					}

					if (orb is Orb && orb.sprite.renderDelay <= 0)
					{
						if (orb.armed != null)
						{
							ground.queueSprite(glow, xi, yi, ORB, 0)
						}

						if (orb.hasAttack)
						{
							val cx = xi + (orb.sprite.animation?.renderOffset(false)?.get(0) ?: 0f)
							val cy = yi + 0.15f + (orb.sprite.animation?.renderOffset(false)?.get(1) ?: 0f)

							val currentPoint = Vector2(0f, 0.4f)

							val maxdots = 10
							val degreesStep = 360f / maxdots
							for (i in 0 until maxdots)
							{
								val sprite = if (i < orb.attackTimer) atk_full else atk_empty

								floating.queueSprite(sprite, cx + currentPoint.x, cy + currentPoint.y, ORB, 2, orbColour)

								currentPoint.rotate(degreesStep)
							}
						}
					}
				}

				if (monster != null && tile == monster.tiles[0, monster.size-1])
				{
					monster.sprite.size[0] = monster.size
					monster.sprite.size[1] = monster.size
					ground.queueSprite(monster.sprite, xi, yi, ORB, 1, monsterColour)

					// do hp bar
					val solidSpaceRatio = 0.12f // 20% free space
					val space = monster.size.toFloat()
					val pips = monster.maxhp + monster.damageReduction
					val spacePerPip = space / pips.toFloat()
					val spacing = spacePerPip * solidSpaceRatio
					val solid = spacePerPip - spacing

					for (i in 0 until pips)
					{
						val sprite = when {
							i < monster.hp -> hp_full
							i < monster.hp + monster.remainingReduction -> hp_dr
							i < monster.hp + monster.lostHP -> hp_damaged
							else -> hp_empty
						}
						floating.queueSprite(sprite, xi+i*spacePerPip, yi+0.1f, ORB, 2, width = solid, height = 0.15f)
					}
				}

				if (friendly != null && tile == friendly.tiles[0, friendly.size-1])
				{
					friendly.sprite.size[0] = friendly.size
					friendly.sprite.size[1] = friendly.size
					ground.queueSprite(friendly.sprite, xi, yi, ORB, 1, orbColour)

					// do hp bar
					val solidSpaceRatio = 0.12f // 20% free space
					val space = friendly.size.toFloat()
					val pips = friendly.maxhp + friendly.damageReduction
					val spacePerPip = space / pips.toFloat()
					val spacing = spacePerPip * solidSpaceRatio
					val solid = spacePerPip - spacing

					val fullHp = if (friendly.isSummon) hp_full_summon else hp_full_friendly

					for (i in 0 until pips)
					{
						val sprite = when {
							i < friendly.hp -> fullHp
							i < friendly.hp + friendly.remainingReduction -> hp_dr
							i < friendly.hp + friendly.lostHP -> hp_damaged
							else -> hp_empty
						}
						floating.queueSprite(sprite, xi+i*spacePerPip, yi+0.1f, ORB, 2, width = solid, height = 0.15f)
					}
				}

				if (block != null)
				{
					ground.queueSprite(block.sprite, xi, yi, ORB, 1, blockColour)
				}

				if (tile.isSelected)
				{
					ground.queueSprite(frame, xi, yi, ORB, 0)
				}

				val waitTime = if (grid.level.defeatConditions.any{ it is CompletionConditionTime }) 3f else 8f
				if (grid.noMatchTimer > waitTime && grid.matchHint != null)
				{
					if (tile == grid.matchHint!!.first || tile == grid.matchHint!!.second)
					{
						ground.queueSprite(border, xi, yi, ORB, 0)
					}
				}
			}
		}

		ground.flush(batch!!)
		floating.flush(batch)
	}

	companion object
	{
		lateinit var instance: GridWidget
	}
}