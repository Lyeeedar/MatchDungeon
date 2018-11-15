package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTime
import com.lyeeedar.Game.Ability.Effect
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.ciel
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
			ground.tileSize = value
			floating.tileSize = value
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

	val ground = SortedRenderer(tileSize, grid.width.toFloat(), grid.height.toFloat(), EFFECT+1, true)
	val floating = SortedRenderer(tileSize, grid.width.toFloat(), grid.height.toFloat(), EFFECT+1, true)

	val shapeRenderer = ShapeRenderer()

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

			floating.queueSprite(sprite, x, y, ORB, 2, width = solid, height = 0.15f)
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
				val swappable = tile.swappable
				val block = tile.block
				val container = tile.container
				val chest = tile.chest
				val monster = tile.monster
				val friendly = tile.friendly
				val spreader  = tile.spreader

				var tileColour = Colour.WHITE
				var orbColour = Colour.WHITE
				var blockColour = Colour.WHITE
				var monsterColour = Colour.WHITE

				if (grid.activeAbility != null)
				{
					if (grid.activeAbility!!.effect.type != Effect.Type.BUFF && grid.activeAbility!!.targetter.isValid (tile, grid.activeAbility!!.data))
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
							orbColour = if (swappable != null && swappable.sealed) Colour.WHITE else Colour.DARK_GRAY
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

				val groundSprite = tile.groundSprite
				if (groundSprite != null)
				{
					if (!groundSprite.hasChosenSprites)
					{
						groundSprite.chooseSprites()
					}

					val sprite = groundSprite.chosenSprite
					if (sprite != null)
					{
						ground.queueSprite(sprite, xi, yi, TILE, tileHeight, tileColour)
					}

					val tilingSprite = groundSprite.chosenTilingSprite
					if (tilingSprite != null)
					{
						ground.queueSprite(tilingSprite, xi, yi, TILE, tileHeight, tileColour)
					}

					tileHeight++
				}

				val wallSprite = tile.wallSprite
				if (wallSprite != null)
				{
					if (!wallSprite.hasChosenSprites)
					{
						wallSprite.chooseSprites()
					}

					val sprite = wallSprite.chosenSprite
					if (sprite != null)
					{
						ground.queueSprite(sprite, xi, yi, TILE, tileHeight, tileColour)
					}

					val tilingSprite = wallSprite.chosenTilingSprite
					if (tilingSprite != null)
					{
						ground.queueSprite(tilingSprite, xi, yi, TILE, tileHeight, tileColour)
					}

					tileHeight++
				}

				if (tile.hasPlate)
				{
					ground.queueSprite(grid.level.theme.plate, xi, yi, TILE, tileHeight, tileColour)

					if ( !grid.inTurn )
					{
						val tutorial = Tutorial("Plate")
						tutorial.addPopup("This is a plate. Match on top of this to break it.", getRect(tile))
						tutorial.show()
					}

					tileHeight++
				}

				if (chest != null)
				{
					ground.queueSprite(chest.sprite, xi, yi, TILE, tileHeight, tileColour)

					if (chest.numToSpawn > 0 && !grid.inTurn && !Global.settings.get("Chest", false))
					{
						val tutorial = Tutorial("Chest")
						tutorial.addPopup("This is a chest. Match in the tiles beneath this to spawn coins. When there are no more coins to spawn, it will appear empty.", getRect(tile))
						tutorial.show()
					}
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

				if (swappable != null)
				{
					ground.queueSprite(swappable.sprite, xi, yi, ORB, 1, orbColour)

					if (swappable.sealed)
					{
						ground.queueSprite(swappable.sealSprite, xi, yi, ORB, 2, orbColour)

						if (!Global.settings.get("Seal", false) && !grid.inTurn )
						{
							val tutorial = Tutorial("Seal")
							tutorial.addPopup("This orb has been sealed. It won't move until the seal is broken. To break the seal use the orb in a match.", getRect(swappable))
							tutorial.show()
						}
					}

					if (swappable is Orb && swappable.isChanger)
					{
						if (swappable.sprite.visible && (swappable.sprite.showBeforeRender || swappable.sprite.renderDelay <= 0))
						{
							val offset = swappable.sprite.animation?.renderOffset(false)

							var xii = xi
							var yii = yi

							if (offset != null)
							{
								xii += offset[0]
								yii += offset[1]
							}

							if (swappable.nextDesc == null)
							{
								swappable.nextDesc = Orb.getRandomOrb(grid.level, swappable.desc)
							}

							tempCol.set(orbColour).mul(swappable.nextDesc!!.sprite.colour)

							var scaleX = swappable.sprite.baseScale[0]
							var scaleY = swappable.sprite.baseScale[1]

							val scale = swappable.sprite.animation?.renderScale()

							if (scale != null)
							{
								scaleX *= scale[0]
								scaleY *= scale[1]
							}

							ground.queueSprite(changer, xii, yii, ORB, 2, tempCol, scaleX = scaleX, scaleY = scaleY)
						}
					}

					if (swappable is Special && swappable.sprite.renderDelay <= 0)
					{
						if (swappable.armed)
						{
							ground.queueSprite(glow, xi, yi, ORB, 0)
						}
					}

					if (swappable is MonsterEffect && swappable.sprite.renderDelay <= 0 && swappable.delayDisplay <= 0f)
					{
						val cx = xi + (swappable.sprite.animation?.renderOffset(false)?.get(0) ?: 0f)
						val cy = yi + 0.15f + (swappable.sprite.animation?.renderOffset(false)?.get(1) ?: 0f)

						val currentPoint = Vector2(0f, 0.4f)

						val maxdots = 10
						val degreesStep = 360f / maxdots
						for (i in 0 until maxdots)
						{
							val sprite = if (i < swappable.timer) atk_full else atk_empty

							floating.queueSprite(sprite, cx + currentPoint.x, cy + currentPoint.y, ORB, 2, orbColour)

							currentPoint.rotate(degreesStep)
						}

						if (!Global.settings.get("Attack", false) && !grid.inTurn )
						{
							val tutorial = Tutorial("Attack")
							tutorial.addPopup("This is an attack. The pips surrounding the skull indicate the turns remaining until it activates.", getRect(swappable))
							tutorial.addPopup("Match it like a normal orb to remove it from the board. If you fail to remove it then you will lose 1 hp", getRect(swappable))
							tutorial.show()
						}
					}

					if (swappable is Sinkable)
					{
						if (!Global.settings.get("Sinkable", false) && !grid.inTurn )
						{
							val tutorial = Tutorial("Sinkable")
							tutorial.addPopup("This is a sinkable item. If you move it to the bottom of the board you will successfully sink it.", getRect(tile, true))
							tutorial.show()
						}
					}
				}

				if (monster != null && tile == monster.tiles[0, monster.size-1])
				{
					monster.sprite.size[0] = monster.size
					monster.sprite.size[1] = monster.size
					ground.queueSprite(monster.sprite, xi, yi, ORB, 1, monsterColour)

					// do hp bar
					val maxY = drawHPBar(monster.size.toFloat(), monster.hp, monster.lostHP, monster.remainingReduction, monster.maxhp, monster.damageReduction, monster.immune, xi, yi, hp_full)

					val rootDesc = monster.desc.originalDesc ?: monster.desc
					if (rootDesc.stages.size > 0)
					{
						val currentStage = if (monster.desc.originalDesc == null) -1 else rootDesc.stages.indexOf(monster.desc)
						var x = xi
						val y = maxY
						val size = 0.15f

						for (i in 0 until rootDesc.stages.size+1)
						{
							val sprite = when
							{
								i < rootDesc.stages.size-currentStage -> stage_full
								else -> stage_empty
							}

							floating.queueSprite(sprite, x, y, ORB, 2, width = size, height = size)

							x += size
						}

						if (!Global.settings.get("MonsterStages", false) && !grid.inTurn)
						{
							val tutorial = Tutorial("MonsterStages")
							val tiles: com.badlogic.gdx.utils.Array<Point> = monster.tiles.toList().toGdxArray()
							tutorial.addPopup("This enemy has multiple stages, indicated by the orbs above its hp bar. When its hp bar is empty it will mutate into a new creature, so watch out!", getRect(tiles))
							tutorial.show()
						}
					}

					if (!Global.settings.get("Monster", false) && !grid.inTurn )
					{
						val tutorial = Tutorial("Monster")
						val tiles: com.badlogic.gdx.utils.Array<Point> = monster.tiles.toList().toGdxArray()
						tutorial.addPopup("This is an enemy. The red bar beneath it is its remaining health. Match in the tiles surrounding it to damage it.", getRect(tiles))
						tutorial.show()
					}

					if (monster.damageReduction > 0 && !grid.inTurn  && !Global.settings.get("DR", false))
					{
						val tutorial = Tutorial("DR")
						val tiles: com.badlogic.gdx.utils.Array<Point> = monster.tiles.toList().toGdxArray()
						tutorial.addPopup("This enemy has damage resistance, represented by the grey pips on its health bar. At the end of each turn it will replenish those pips, so focus on those big hits!", getRect(tiles))
						tutorial.show()
					}
				}

				if (friendly != null && tile == friendly.tiles[0, friendly.size-1])
				{
					friendly.sprite.size[0] = friendly.size
					friendly.sprite.size[1] = friendly.size
					ground.queueSprite(friendly.sprite, xi, yi, ORB, 1, orbColour)

					// do hp bar
					val fullHp = if (friendly.isSummon) hp_full_summon else hp_full_friendly
					drawHPBar(friendly.size.toFloat(), friendly.hp, friendly.lostHP, friendly.remainingReduction, friendly.maxhp, friendly.damageReduction, friendly.immune, xi, yi, fullHp)

					if (!Global.settings.get("Friendly", false) && !grid.inTurn )
					{
						val tutorial = Tutorial("Friendly")
						val tiles: com.badlogic.gdx.utils.Array<Point> = friendly.tiles.toList().toGdxArray()
						tutorial.addPopup("This is a friendly ally. Match in the surrounding tiles to replenish its health.", getRect(tiles))
						tutorial.show()
					}
				}

				if (block != null)
				{
					ground.queueSprite(block.sprite, xi, yi, ORB, 1, blockColour)

					// do hp bar
					if (block.hp < block.maxhp || block.alwaysShowHP)
					{
						drawHPBar(1f, block.hp, block.lostHP, block.remainingReduction, block.maxhp, block.damageReduction, block.immune, xi, yi, hp_neutral)
					}

					if (!Global.settings.get("Block", false) && !grid.inTurn )
					{
						val tutorial = Tutorial("Block")
						tutorial.addPopup("This is a block. Match in the tiles surrounding it to break it.", getRect(tile, true))
						tutorial.show()
					}
				}

				if (container != null)
				{
					ground.queueSprite(container.sprite, xi, yi, ORB, 1, blockColour)

					// do hp bar
					if (container.hp < container.maxhp || container.alwaysShowHP)
					{
						drawHPBar(1f, container.hp, container.lostHP, container.remainingReduction, container.maxhp, container.damageReduction, container.immune, xi, yi, hp_neutral)
					}
				}

				if (spreader != null)
				{
					val level = if (spreader.renderAbove) floating else ground

					if (spreader.spriteWrapper != null)
					{
						if (spreader.spriteWrapper!!.sprite != null)
						{
							level.queueSprite(spreader.spriteWrapper!!.sprite!!, xi, yi, SPREADER, 0, orbColour)
						}
						if (spreader.spriteWrapper!!.tilingSprite != null)
						{
							level.queueSprite(spreader.spriteWrapper!!.tilingSprite!!, xi, yi, SPREADER, 0, orbColour)
						}
					}

					if (spreader.particleEffect != null)
					{
						level.queueParticle(spreader.particleEffect!!, xi, yi, SPREADER, 1, orbColour)
					}

					if (!Global.settings.get("Spreader", false) && !grid.inTurn )
					{
						val tutorial = Tutorial("Spreader")
						tutorial.addPopup("This is a spreading field. Match in the tiles surrounding it to remove it and stop it spreading this turn.", getRect(tile))
						tutorial.show()
					}
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