package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.CompletionCondition.CompletionConditionCustomOrb
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.GridUpdate.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Statistic
import com.lyeeedar.UI.FullscreenMessage
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*

class Grid(val width: Int, val height: Int, val level: Level)
{
	// ----------------------------------------------------------------------
	val grid: Array2D<Tile> = Array2D(width, height ){ x, y -> Tile(x, y, this) }
	val spawnCount: Array2D<Int> = Array2D(width, height + 1){ _, _ -> 0 }

	// ----------------------------------------------------------------------
	val defaultAnimSpeed = 0.15f
	val animSpeedUpMultiplier = 0.975f
	var animSpeedMultiplier = 1f
	val animSpeed: Float
		get() = defaultAnimSpeed * animSpeedMultiplier

	var matchCount = 0

	// ----------------------------------------------------------------------
	val refillSprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f)

	// ----------------------------------------------------------------------
	var lastSwapped: Point = Point.MINUS_ONE

	// ----------------------------------------------------------------------
	val onTurn = Event0Arg()
	val onTime = Event1Arg<Float>()
	val onPop = Event2Arg<Entity, Float>()
	val onSunk = Event1Arg<Entity>()
	val onDamaged = Event1Arg<Entity>()
	val onSpawn = Event1Arg<Entity>()
	val onAttacked = Event1Arg<Entity>()

	// ----------------------------------------------------------------------
	val hitEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()

	// ----------------------------------------------------------------------
	var gainedBonusPower = false
	val poppedSpreaders = ObjectSet<String>()

	// ----------------------------------------------------------------------
	// Grid state
	val monsterTiles = Array<Tile>()
	val friendlyTiles = Array<Tile>()
	val sinkableTiles = Array<Tile>()
	val breakableTiles = Array<Tile>()
	val sinkPathTiles = Array<Tile>()
	val notSinkPathTiles = Array<Tile>()
	val basicOrbTiles = Array<Tile>()
	val attackTiles = Array<Tile>()
	val namedOrbTiles = Array<Tile>()

	// ----------------------------------------------------------------------
	var inTurn = true

	// ----------------------------------------------------------------------
	var noMatchTimer = 0f
	var matchHint: ValidMove? = null

	// ----------------------------------------------------------------------
	var noValidMoves = false

	// ----------------------------------------------------------------------
	var dragStart: Point = Point.MINUS_ONE
	var toSwap: Pair<Point, Point>? = null

	// ----------------------------------------------------------------------
	val cascade = CascadeUpdateStep()
	val match = MatchUpdateStep()
	val sink = SinkUpdateStep()
	val detonate = DetonateUpdateStep()
	val updateGrid = UpdateGridUpdateStep()
	val updateGridState = UpdateGridStateUpdateStep()
	val cleanup = OnTurnCleanupUpdateStep()

	val updateSteps: kotlin.Array<AbstractUpdateStep>

	// ----------------------------------------------------------------------
	var activeAbility: Ability? = null
		set(value)
		{
			if (level.isVictory || level.isDefeat) return

			field = value

			if (value == null)
			{
				for (tile in grid)
				{
					tile.isSelected = false
				}
			}
			else
			{
				dragStart = Point.MINUS_ONE

				if (value.targets == 0)
				{
					value.activate(this)
					inTurn = true
					field = null
				}
			}
		}

	// ----------------------------------------------------------------------
	init
	{
		updateSteps = arrayOf(
			cascade,
			match,
			sink,
			detonate,
			updateGridState,
			updateGrid,
			cleanup)

		onPop += fun (entity: Entity, delay: Float) : Boolean {

			val matchable = entity.matchable() ?: return false
			if (!matchable.skipPowerOrb)
			{
				if (Global.resolveInstantly)
				{
					PowerBar.instance.power++
					if (!gainedBonusPower)
					{
						gainedBonusPower = true

						val gain = level.player.getStat(Statistic.POWERGAIN).toInt()
						for (i in 0 until gain)
						{
							PowerBar.instance.power++
						}
					}
				}
				else
				{
					val pos = GridWidget.instance.pointToScreenspace(entity.pos().tile!!)
					val dst = PowerBar.instance.getOrbDest()
					val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/crystal_sky")

					if (dst != null)
					{
						Future.call({ spawnMote(pos, dst, sprite.copy(), 32f, { PowerBar.instance.power++ }) }, delay)

						if (!gainedBonusPower)
						{
							gainedBonusPower = true

							val gain = level.player.getStat(Statistic.POWERGAIN).toInt()
							for (i in 0 until gain)
							{
								val dst = PowerBar.instance.getOrbDest() ?: break
								Future.call({ spawnMote(pos, dst, sprite.copy(), 32f, { PowerBar.instance.power++ }) }, delay)
							}
						}
					}
				}
			}

			return false
		}
	}

	// ----------------------------------------------------------------------
	fun hasAnim(): Boolean
	{
		var hasAnim = false
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val tile = grid[x, y]

				if (tile.delayedActions.size > 0)
				{
					hasAnim = true
					break
				}

				for (effect in tile.effects)
				{
					if (effect is ParticleEffect)
					{
						if (effect.isShortened)
						{
							if (effect.time < effect.lifetime * 0.5f)
							{
								hasAnim = true
								break
							}
						}
						else if (effect.blocked())
						{
							hasAnim = true
							break
						}
						else if (effect.animation != null)
						{
							hasAnim = true
							break
						}
					}
					else
					{
						hasAnim = true
						break
					}
				}

				val swappable = tile.contents?.swappable()
				val renderable = tile.contents?.renderableOrNull()
				if (swappable != null && renderable?.renderable?.animation != null)
				{
					hasAnim = true
					break
				}

				if (hasAnim)
				{
					break
				}
			}

			if (hasAnim)
			{
				break
			}
		}

		return hasAnim
	}

	// ----------------------------------------------------------------------
	fun refill()
	{
		val tempgrid: Array2D<Tile> = Array2D(width, height ){ x, y -> Tile(x, y, this) }
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				tempgrid[x, y].contents = grid[x, y].contents
			}
		}

		fill(true)

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val oldcontents = tempgrid[x, y].contents
				if (oldcontents?.matchable() == null || oldcontents.matchable()!!.desc.isNamed)
				{
					grid[x, y].contents = tempgrid[x, y].contents
				}
				else if (oldcontents.matchable() != null)
				{
					val newmatchable = grid[x, y].contents!!.matchable()!!
					grid[x, y].contents = tempgrid[x, y].contents
					val oldmatchable = grid[x, y].contents!!.matchable()!!

					val delay = grid[x, y].taxiDist(Point.ZERO).toFloat() * 0.1f

					if (Global.resolveInstantly)
					{
						oldmatchable.setDesc(newmatchable.desc, oldcontents)
					}
					else
					{
						Future.call(
							{
								val sprite = refillSprite.copy()

								oldmatchable.setDesc(newmatchable.desc, oldcontents)

								grid[x, y].effects.add(sprite)
							}, delay + 0.2f)
					}

				}
			}
		}

		inTurn = true
	}

	// ----------------------------------------------------------------------
	fun fill(orbOnly: Boolean)
	{
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val tile = grid[x, y]
				if (tile.canHaveOrb && tile.contents?.damageable() == null && tile.contents?.healable() == null)
				{
					val toSpawn = if (orbOnly) createOrb(OrbDesc.getRandomOrb(level)) else level.spawnOrb()

					val toSpawnMatchable = toSpawn.matchable()
					if (toSpawnMatchable != null)
					{
						val valid = OrbDesc.getValidOrbs(level)

						if (!orbOnly)
						{
							for (v in level.victoryConditions)
							{
								if (v is CompletionConditionCustomOrb)
								{
									if (Random.random.nextFloat() < v.orbChance)
									{
										valid.add(OrbDesc.getNamedOrb(v.targetOrbName))
									}
								}
							}
						}

						val l1 = tile(x - 1, y)?.contents?.matchable()?.desc
						val l2 = tile(x - 2, y)?.contents?.matchable()?.desc
						val u1 = tile(x, y - 1)?.contents?.matchable()?.desc
						val u2 = tile(x, y - 2)?.contents?.matchable()?.desc

						if (l1?.key == l2?.key)
						{
							valid.removeValue(l1, true)
						}
						if (u1?.key == u2?.key)
						{
							valid.removeValue(u1, true)
						}

						toSpawnMatchable.setDesc(valid.random(), toSpawn)
						if (toSpawnMatchable.isChanger) toSpawnMatchable.nextDesc = OrbDesc.getRandomOrb(level, toSpawnMatchable.desc)
					}

					grid[x, y].contents = toSpawn
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	fun activateAbility()
	{
		if (level.isVictory || level.isDefeat) return

		activeAbility!!.activate(this)
		activeAbility = null

		inTurn = true
	}

	// ----------------------------------------------------------------------
	fun select(newSelection: Point)
	{
		if (hasAnim() || level.isVictory || level.isDefeat) return

		if (activeAbility != null)
		{
			val newTile = tile(newSelection) ?: return
			if (!activeAbility!!.targetter.isValid(newTile, activeAbility!!.data)) return

			if (newTile.isSelected)
			{
				newTile.isSelected = false
				activeAbility!!.selectedTargets.removeValue(newTile, true)
			}
			else if (activeAbility!!.selectedTargets.size < activeAbility!!.targets)
			{
				newTile.isSelected = true
				activeAbility!!.selectedTargets.add(newTile)
			}
		}
		else
		{
			dragStart = newSelection
		}
	}

	// ----------------------------------------------------------------------
	fun dragEnd(selection: Point)
	{
		if (level.isVictory || level.isDefeat) return

		if (selection != dragStart && dragStart.dist(selection) == 1)
		{
			toSwap = Pair(dragStart, selection)
			dragStart = Point.MINUS_ONE
		}
	}

	// ----------------------------------------------------------------------
	fun clearDrag()
	{
		dragStart = Point.MINUS_ONE
	}

	// ----------------------------------------------------------------------
	fun update(deltaTime: Float)
	{
		// process tiles
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val tile = grid[x, y]

				if (tile.delayedActions.size > 0)
				{
					val itr = tile.delayedActions.iterator()
					while (itr.hasNext())
					{
						val action = itr.next()
						action.delay -= deltaTime

						if (action.delay <= 0)
						{
							action.function.invoke()
							itr.remove()
						}
					}
				}
			}
		}

		for (step in updateSteps)
		{
			step.doUpdateRealTime(this, deltaTime)
		}

		if (!hasAnim())
		{
			for (step in updateSteps)
			{
				val completed = step.doUpdate(this)
				if (!completed)
				{
					return
				}
			}

			if (inTurn)
			{
				for (step in updateSteps)
				{
					if (!step.wasRunThisTurn)
					{
						step.wasRunThisTurn = true
						step.doTurn(this)
						return
					}
				}

				onTurn()

				for (step in updateSteps)
				{
					step.wasRunThisTurn = false
				}
			}
			else
			{
				if (level.isVictory || level.isDefeat)
				{
					level.complete()
				}
				else
				{
					updatePlayerInput(deltaTime)
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	fun updatePlayerInput(delta: Float)
	{
		if (FullscreenMessage.instance == null)
		{
			if (activeAbility == null && matchHint == null)
			{
				noValidMoves = true
			}
			else
			{
				noValidMoves = false

				if (activeAbility != null) noMatchTimer = 0f
				else noMatchTimer += delta

				// handle input
				if (toSwap != null)
				{
					val swapSuccess = swap()
					if (swapSuccess) inTurn = true
				}

				if (Tutorial.current == null) onTime(delta)
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun swap(): Boolean
	{
		val oldTile = tile(toSwap!!.first)
		val newTile = tile(toSwap!!.second)

		toSwap = null

		if (oldTile == null || newTile == null) return false

		if (oldTile.spreader?.effect == Spreader.SpreaderEffect.SEAL || newTile.spreader?.effect == Spreader.SpreaderEffect.SEAL) return false

		val oldEntity = oldTile.contents ?: return false
		val newEntity = newTile.contents ?: return false

		val oldSwap = oldEntity.swappable() ?: return false
		val newSwap = newEntity.swappable() ?: return false

		val oldSpecial = oldEntity.special()?.special
		val newSpecial = newEntity.special()?.special

		if (!oldSwap.canMove || !newSwap.canMove) return false

		oldSwap.cascadeCount = -1
		newSwap.cascadeCount = -1

		if (oldSpecial != null || newSpecial != null)
		{
			val merged = newSpecial?.merge(oldEntity) ?: oldSpecial?.merge(newEntity)
			if (merged != null)
			{
				addSpecial(newEntity, merged)

				val sprite = oldEntity.renderable().renderable.copy()
				sprite.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile, true)), Interpolation.linear)
				newTile.effects.add(sprite)

				oldTile.contents = null

				merged.armed = true
				newEntity.add(MarkedForDeletionComponent.obtain())

				lastSwapped = newTile
				matchHint = null

				return true
			}
		}

		oldTile.contents = newEntity
		newTile.contents = oldEntity

		oldEntity.pos().tile = newTile
		newEntity.pos().tile = oldTile

		val matches = match.findMatches(this)
		for (match in matches) match.free()
		if (matches.size == 0)
		{
			oldTile.contents = oldEntity
			newTile.contents = newEntity

			oldEntity.pos().tile = oldTile
			newEntity.pos().tile = newTile

			var dir = Direction.getDirection(oldTile, newTile)
			if (dir.y != 0) dir = dir.opposite
			oldEntity.renderable().renderable.animation = BumpAnimation.obtain().set(animSpeed, dir)

			if (Global.resolveInstantly)
			{
				throw RuntimeException("Swap was not valid!")
			}

			return false
		}
		else
		{
			lastSwapped = newTile
			matchHint = null

			oldEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile)).invertY(), Interpolation.linear)
			newEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(oldTile.getPosDiff(newTile)).invertY(), Interpolation.linear)
			return true
		}
	}

	// ----------------------------------------------------------------------
	fun damage(tile: Tile, damageableEntity: Entity, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f)
	{
		val damageable = damageableEntity.damageable()!!

		var targetDam = if (!damageable.damSources.contains(damSource)) 1f + bonusDam else 1f
		var damReduction = damageable.remainingReduction.toFloat()

		damReduction -= pierce
		if (damReduction < 0f)
		{
			damReduction = 0f
		}

		damReduction -= targetDam

		if (damReduction < 0)
		{
			targetDam = -damReduction
			damageable.remainingReduction = 0
		}
		else
		{
			targetDam = 0f
			damageable.remainingReduction = damReduction.ciel()
		}

		damageable.hp -= targetDam
		if (damSource != null) damageable.damSources.add(damSource)
		val hit = hitEffect.copy()
		hit.renderDelay = delay
		tile.effects.add(hit)
		onDamaged(damageableEntity)

		if (damageable.hp <= 0)
		{
			damageableEntity.add(MarkedForDeletionComponent.obtain().set(delay))
		}

		if (damageable.isCreature)
		{
			val vampiric = Global.player.getStat(Statistic.VAMPIRICSTRIKES, withChoaticNature = true)
			val die = level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()
			if (die != null)
			{
				die.fractionalHp += targetDam * vampiric
			}
		}
	}

	// ----------------------------------------------------------------------
	fun pop(point: Point, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f, skipPowerOrb: Boolean = false)
	{
		pop(point.x, point.y , delay, damSource, bonusDam, pierce, skipPowerOrb)
	}

	// ----------------------------------------------------------------------
	fun pop(x: Int, y: Int, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f, skipPowerOrb: Boolean = false)
	{
		val tile = tile(x, y) ?: return

		if (tile.hasPlate)
		{
			tile.plateStrength--
			val hit = hitEffect.copy()
			hit.renderDelay = delay
			tile.effects.add(hit)
		}

		if (tile.spreader != null && damSource !is Spreader)
		{
			val spreader = tile.spreader!!

			poppedSpreaders.add(spreader.nameKey)

			tile.spreader = null

			val hit = hitEffect.copy()
			hit.renderDelay = delay
			tile.effects.add(hit)
		}

		if (tile.contents?.healable() != null)
		{
			val friendly = tile.contents!!.healable()!!
			if (friendly.hp < friendly.maxhp)
			{
				friendly.hp++
				val healSprite = AssetManager.loadParticleEffect("Heal").getParticleEffect()
				healSprite.colour = Colour.GREEN
				tile.effects.add(healSprite)
			}

			return
		}
		else if (tile.contents?.damageable() != null)
		{
			damage(tile, tile.contents!!, delay, damSource, bonusDam, pierce)
			return
		}

		val contents = tile.contents
		val swappable = contents?.swappable() ?: return
		if (contents.hasComponent(MarkedForDeletionComponent::class.java)) return // already completed, dont do it again

		if (swappable.sealed)
		{
			swappable.sealCount--
			val hit = hitEffect.copy()
			hit.renderDelay = delay
			tile.effects.add(hit)
			return
		}

		if (contents.special() != null)
		{
			if (!swappable.sealed)
			{
				contents.special()!!.special.armed = true
			}
			else
			{
				return
			}
		}

		contents.add(MarkedForDeletionComponent.obtain().set(delay))

		val matchable = contents.matchable()
		if (matchable != null)
		{
			matchable.skipPowerOrb = skipPowerOrb

			val sprite = matchable.desc.death.copy()
			sprite.colour = contents.renderable().renderable.colour
			sprite.renderDelay = delay
			sprite.isShortened = true

			tile.effects.add(sprite)
		}

		if (contents.monsterEffect() != null)
		{
			contents.remove(MonsterEffectComponent::class.java)
		}
	}

	// ----------------------------------------------------------------------
	fun tile(point: Point): Tile? = tile(point.x, point.y)

	// ----------------------------------------------------------------------
	fun tile(x: Int, y:Int): Tile?
	{
		if (x >= 0 && y >= 0 && x < width && y < height) return grid[x, y]
		else return null
	}

	// ----------------------------------------------------------------------
	fun getTileClamped(point: Point) = getTile(MathUtils.clamp(point.x, 0, width-1), MathUtils.clamp(point.y, 0, height-1))!!

	// ----------------------------------------------------------------------
	fun getTile(point: Point) = getTile(point.x, point.y)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, ox:Int, oy:Int) = getTile(point.x + ox, point.y + oy)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, o: Point) = getTile(point.x + o.x, point.y + o.y)

	// ----------------------------------------------------------------------
	fun getTile(x: Int, y: Int, dir: Direction) = getTile(x + dir.x, y + dir.y)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, dir: Direction) = getTile(point.x + dir.x, point.y + dir.y)

	// ----------------------------------------------------------------------
	fun getTile(x: Int, y: Int): Tile? = grid[x, y, null]
}

data class Match(val p1: Point, val p2: Point, var used: Boolean = false)
{
	fun length() = p1.dist(p2) + 1
	fun points() = p1.rangeTo(p2)
	fun direction() = Direction.getDirection(p1, p2)
	fun free()
	{
		p1.free()
		p2.free()
	}
}
