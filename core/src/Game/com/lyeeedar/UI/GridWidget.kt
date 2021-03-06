package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTime
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.MonsterAI
import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Ability.isValid
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Util.*
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray

/**
 * Created by Philip on 05-Jul-16.
 */

class GridWidget(val grid: Grid) : Widget()
{
	var tileSize = 32f
		set(value)
		{
			field = value
			renderer.tileSize = value
		}

	val glow: Sprite = AssetManager.loadSprite("glow")
	val frame: Sprite = AssetManager.loadSprite("GUI/frame", colour = Colour(Color(0.6f, 0.7f, 0.9f, 0.6f)))
	val border: Sprite = AssetManager.loadSprite("GUI/border", colour = Colour(Color(0.6f, 0.9f, 0.6f, 0.6f)))
	val hp_full: Sprite = AssetManager.loadSprite("GUI/health_full")
	val hp_dr: Sprite = AssetManager.loadSprite("GUI/health_DR")
	val hp_damaged: Sprite = AssetManager.loadSprite("GUI/health_damaged")
	val hp_neutral: Sprite = AssetManager.loadSprite("GUI/health_neutral")
	val hp_full_friendly: Sprite = AssetManager.loadSprite("GUI/health_full_green")
	val hp_full_summon: Sprite = AssetManager.loadSprite("GUI/health_full_blue")
	val hp_empty: Sprite = AssetManager.loadSprite("GUI/health_empty")
	val atk_full: Sprite = AssetManager.loadSprite("GUI/attack_full")
	val atk_empty: Sprite = AssetManager.loadSprite("GUI/attack_empty")
	val stage_full: Sprite = AssetManager.loadSprite("GUI/attack_full")
	val stage_empty: Sprite = AssetManager.loadSprite("GUI/attack_empty")
	val changer: Sprite = AssetManager.loadSprite("Oryx/Custom/items/changer", drawActualSize = true)

	val TILE = 0
	val SPREADER = TILE+1
	val ORB = SPREADER+1
	val EFFECT = ORB+1

	val renderer: SortedRenderer by lazy{ SortedRenderer(tileSize, grid.width.toFloat(), grid.height.toFloat(), EFFECT+1, true) }

	val tempCol = Colour()

	var hpLossTimer = 0f

	var renderedStatic = false

	init
	{
		name = "GridWidget"

		instance = this

		touchable = Touchable.enabled

		addListener(object : InputListener()
		{
			override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
			{
				val xp = x + ((grid.width * tileSize) / 2f) - (width / 2f)
				val yp = y + ((grid.height * tileSize) / 2f) - (height / 2f)

				val sx = (xp / tileSize).toInt()
				val sy = (grid.height-1) - (yp / tileSize).toInt()

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
				val yp = y + ((grid.height * tileSize) / 2f) - (height / 2f)

				val sx = (xp / tileSize).toInt()
				val sy = (grid.height - 1) - (yp / tileSize).toInt()

				val point = Point(sx, sy)

				if (point != grid.dragStart)
				{
					grid.dragEnd(point)
				}
			}

			override fun keyTyped(event: InputEvent?, character: Char): Boolean
			{
				return false
			}
		})

		atk_empty.baseScale = floatArrayOf(0.14f, 0.14f)
		atk_full.baseScale = floatArrayOf(0.14f, 0.14f)
	}

	fun getRect(point: Point, actualSize: Boolean = false): Rectangle
	{
		val array = com.badlogic.gdx.utils.Array<Point>()
		array.add(point)
		val rect = getRect(array)

		if (actualSize)
		{
			rect.height *= 1.5f
		}

		return rect
	}

	fun getRect(points: com.badlogic.gdx.utils.Array<Point>): Rectangle
	{
		var minx = Float.MAX_VALUE
		var miny = Float.MAX_VALUE
		var maxx = -Float.MAX_VALUE
		var maxy = -Float.MAX_VALUE

		for (point in points)
		{
			val screenSpace = pointToScreenspace(point)
			if (screenSpace.x < minx)
			{
				minx = screenSpace.x
			}
			if (screenSpace.y < miny)
			{
				miny = screenSpace.y
			}
			if (screenSpace.x + tileSize > maxx)
			{
				maxx = screenSpace.x + tileSize
			}
			if (screenSpace.y + tileSize > maxy)
			{
				maxy = screenSpace.y + tileSize
			}
		}

		return Rectangle(minx, miny, maxx - minx, maxy - miny)
	}

	fun getRect(entity: Entity): Rectangle
	{
		val pos = entity.pos()
		val tiles: com.badlogic.gdx.utils.Array<Point> = if (pos != null) pos.tiles.toList().toGdxArray() else gdxArrayOf(Point.ZERO)
		return getRect(tiles)
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

		renderedStatic = false
	}

	fun drawHPBar(space: Float, currentHp: Float, lostHP: Int, remainingReduction: Int, maxHp: Int, dr: Int, immune: Boolean, xi: Float, yi: Float, hp_full: Sprite): Float
	{
		// do hp bar
		val solidSpaceRatio = 0.12f // 20% free space

		val maxSingleLineHP = space * 15

		val pips = maxHp + dr

		var lines = 1
		var pipsPerLine = pips
		if (pips > maxSingleLineHP)
		{
			lines = 2
			pipsPerLine = pips / 2
		}

		val spacePerPip = space / pipsPerLine.toFloat()
		val spacing = spacePerPip * solidSpaceRatio
		val solid = spacePerPip - spacing

		val hp = currentHp.ciel()
		for (i in 0 until pips)
		{
			val sprite = when {
				immune -> hp_dr
				i < hp -> hp_full
				i < hp + remainingReduction -> hp_dr
				i < hp + lostHP -> hp_damaged
				else -> hp_empty
			}

			val y = if (i < pipsPerLine && lines > 1) yi+0.25f else yi+0.1f
			val x = if (i >= pipsPerLine && lines > 1) xi+(i-pipsPerLine)*spacePerPip else xi+i*spacePerPip

			val sortY = if (hp == maxHp) null else y.toInt()-2
			renderer.queueSprite(sprite, x, y, EFFECT, 2, width = solid, height = 0.15f, sortY = sortY)
		}

		if (lines > 1)
		{
			return yi+0.35f
		}
		else
		{
			return yi+0.25f
		}
	}

	var renderY = 0f
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val trace = Statics.performanceTracer.getTrace("GridWidgetRender")
		trace.start()

		batch.end()

		val xp = this.x + (this.width / 2f) - ((grid.width * tileSize) / 2f)
		val yp = this.y + (this.height / 2f) - ((grid.height * tileSize) / 2f)
		renderY = yp

		val delta = if (Global.resolveInstantly) 10f else Gdx.app.graphics.deltaTime * GridScreen.instance.timeMultiplier

		if (!renderedStatic)
		{
			renderedStatic = true

			renderer.beginStatic()

			for (x in 0 until grid.width)
			{
				for (y in 0 until grid.height)
				{
					val tile = grid.grid[x, y]

					val xi = x.toFloat()
					val yi = (grid.height - 1) - y.toFloat()

					val rendererSprite = tile.groundSprite
					if (rendererSprite != null)
					{
						if (!rendererSprite.hasChosenSprites)
						{
							rendererSprite.chooseSprites()
						}

						val sprite = rendererSprite.chosenSprite
						if (sprite != null)
						{
							renderer.queueSprite(sprite, xi, yi)
						}

						val tilingSprite = rendererSprite.chosenTilingSprite
						if (tilingSprite != null)
						{
							renderer.queueSprite(tilingSprite, xi, yi)
						}
					}
				}
			}

			renderer.endStatic()
		}

		val activeAbility = grid.activeAbility
		renderer.renderStatic = activeAbility == null
		renderer.begin(delta, xp, yp, Colour.WHITE)

		if (activeAbility == null)
		{
			batch.color = Color.WHITE
		}

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val spreader  = tile.spreader

				var tileColour = Colour.WHITE

				if (activeAbility != null)
				{
					val isValidTarget = activeAbility.data.targetter.isValid(tile)
					if (!isValidTarget)
					{
						tileColour = Colour.DARK_GRAY
					}
				}

				val xi = x.toFloat()
				val yi = (grid.height-1) - y.toFloat()

				var tileHeight = 0

				if (activeAbility != null)
				{
					val rendererSprite = tile.groundSprite
					if (rendererSprite != null)
					{
						renderer.queueSpriteWrapper(rendererSprite, xi, yi, TILE, tileHeight, tileColour)

						tileHeight++
					}
				}
				else
				{
					tileHeight++
				}

				val wallSprite = tile.wallSprite
				if (wallSprite != null)
				{
					renderer.queueSpriteWrapper(wallSprite, xi, yi, TILE, tileHeight, tileColour)

					tileHeight++
				}

				if (tile.hasPlate)
				{
					renderer.queueSprite(grid.level.theme.data.plate, xi, yi, TILE, tileHeight, tileColour)

					if ( !grid.inTurn )
					{
						val tutorial = Tutorial("Plate")
						tutorial.addPopup(Localisation.getText("plate.tutorial", "UI"), getRect(tile))
						tutorial.show()
					}

					tileHeight++
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
							renderer.queueSprite(effect, xi, yi, EFFECT, 0)
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
							renderer.queueParticle(effect, xi, yi, EFFECT, 0)
						}
					}
				}

				val contents = tile.contents
				if (contents != null && tile == (contents.pos()?.tile ?: tile))
				{
					val pos = contents.pos()
					val size = pos?.size ?: 1

					val renderable = contents.renderable()
					if (renderable != null)
					{
						renderable.renderable.size[0] = size
						renderable.renderable.size[1] = size

						renderer.queue(renderable.renderable, xi, yi, ORB, 1, tileColour)
					}

					var maxY = 0f
					val damageable = contents.damageable()
					if (damageable != null)
					{
						if (damageable.alwaysShowHP || damageable.hp < damageable.maxhp)
						{
							maxY = drawHPBar(
								size.toFloat(),
								damageable.hp,
								damageable.lostHP,
								damageable.remainingReduction,
								damageable.maxhp,
								damageable.damageReduction,
								damageable.immune,
								xi,
								yi,
								hp_full)
						}
					}

					val healable = contents.healable()
					if (healable != null)
					{
						val fullHp = if (healable.isSummon) hp_full_summon else hp_full_friendly
						maxY = drawHPBar(
							size.toFloat(),
							healable.hp,
							healable.lostHP,
							0,
							healable.maxhp,
							0,
							healable.immune,
							xi,
							yi,
							fullHp)
					}

					if (contents.ai()?.ai is MonsterAI)
					{
						val monsterAI = contents.ai()?.ai as? MonsterAI

						if (monsterAI != null)
						{
							val rootDesc = monsterAI.originalDesc
							if (rootDesc.stages.size > 0)
							{
								val currentStage = rootDesc.stages.indexOf(monsterAI.desc)
								var x = xi
								val y = maxY
								val size = 0.15f

								for (i in 0 until rootDesc.stages.size + 1)
								{
									val sprite = when
									{
										i < rootDesc.stages.size - currentStage -> stage_full
										else -> stage_empty
									}

									renderer.queueSprite(sprite, x, y, ORB, 2, width = size, height = size)

									x += size
								}
							}
						}
					}

					val swappable = contents.swappable()
					if (swappable != null)
					{
						if (swappable.sealed)
						{
							renderer.queueSprite(grid.level.theme.data.sealSprites.tryGet(swappable.sealCount), xi, yi, ORB, 2, tileColour)
						}
					}

					val contentsSprite = contents.sprite()

					val matchable = contents.matchable()
					if (matchable != null)
					{
						if (matchable.isChanger)
						{
							val sprite = contentsSprite
							if (sprite != null && sprite.visible && (sprite.showBeforeRender || sprite.renderDelay <= 0))
							{
								val offset = sprite.animation?.renderOffset(false)

								var xii = xi
								var yii = yi

								if (offset != null)
								{
									xii += offset[0]
									yii += offset[1]
								}

								var nextDesc = matchable.nextDesc
								if (nextDesc == null)
								{
									nextDesc = OrbDesc.getRandomOrb(grid.level)
									matchable.nextDesc = nextDesc
								}

								tempCol.set(tileColour).mul(nextDesc.sprite.colour)

								var scaleX = sprite.baseScale[0]
								var scaleY = sprite.baseScale[1]

								val scale = sprite.animation?.renderScale()

								if (scale != null)
								{
									scaleX *= scale[0]
									scaleY *= scale[1]
								}

								renderer.queueSprite(changer, xii, yii, ORB, 2, tempCol, scaleX = scaleX, scaleY = scaleY)
							}
						}
					}

					val special = contents.special()
					if (special != null && contentsSprite != null && contentsSprite.renderDelay <= 0)
					{
						if (special.special.armed)
						{
							val offset = contentsSprite.animation?.renderOffset(false)

							var xii = xi
							var yii = yi

							if (offset != null)
							{
								xii += offset[0]
								yii += offset[1]
							}

							renderer.queueSprite(glow, xii, yii, ORB, 0)
						}
					}

					val monsterEffect = contents.monsterEffect()
					if (monsterEffect  != null && contentsSprite != null && contentsSprite.renderDelay <= 0 && monsterEffect.monsterEffect.delayDisplay <= 0f)
					{
						val sprite = contentsSprite

						val cx = xi + (sprite.animation?.renderOffset(false)?.get(0) ?: 0f)
						val cy = yi + 0.15f + (sprite.animation?.renderOffset(false)?.get(1) ?: 0f)

						val currentPoint = Vector2(0f, 0.4f)

						val maxdots = 10
						val degreesStep = 360f / maxdots
						for (i in 0 until maxdots)
						{
							val sprite = if (i < monsterEffect.monsterEffect.timer) atk_full else atk_empty

							renderer.queueSprite(sprite, cx + currentPoint.x, cy + currentPoint.y, ORB, 2, tileColour)

							currentPoint.rotate(degreesStep)
						}
					}

					val tutorialComponent = contents.tutorial()
					if (tutorialComponent != null && !grid.inTurn)
					{
						val tutorial = tutorialComponent.displayTutorial?.invoke(grid, contents, this)
						tutorial?.show()
					}
				}

				if (spreader != null)
				{
					val level = if (spreader.data.renderAbove) renderer else renderer

					val spriteWrapper = spreader.spriteWrapper
					if (spriteWrapper != null)
					{
						val sprite = spriteWrapper.sprite
						if (sprite != null)
						{
							level.queueSprite(sprite, xi, yi, SPREADER, 0, tileColour)
						}

						val tilingSprite = spriteWrapper.tilingSprite
						if (tilingSprite != null)
						{
							level.queueSprite(tilingSprite, xi, yi, SPREADER, 0, tileColour)
						}
					}

					val particleEffect = spreader.particleEffect
					if (particleEffect != null)
					{
						level.queueParticle(particleEffect, xi, yi, SPREADER, 1, tileColour)
					}

					if (!Statics.settings.get("Spreader", false) && !grid.inTurn )
					{
						val tutorial = Tutorial("Spreader")
						tutorial.addPopup(Localisation.getText("spreader.tutorial", "UI"), getRect(tile))
						tutorial.show()
					}
				}

				if (tile.isSelected)
				{
					renderer.queueSprite(frame, xi, yi, ORB, 0)
				}

				val waitTime = if (grid.level.defeatConditions.any{ it is CompletionConditionTime }) 3f else 8f
				val matchHint = grid.matchHint
				if (grid.noMatchTimer > waitTime && matchHint != null)
				{
					if (tile == matchHint.swapStart || tile == matchHint.swapEnd)
					{
						renderer.queueSprite(border, xi, yi, ORB, 0)
					}
				}
			}
		}

		renderer.end(batch)

		batch.begin()

		trace.stop()
	}

	companion object
	{
		lateinit var instance: GridWidget
	}
}
