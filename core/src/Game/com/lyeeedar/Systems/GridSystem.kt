package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.Interpolation
import com.lyeeedar.Board.GridUpdate.*
import com.lyeeedar.Board.Spreader
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.UI.FullscreenMessage
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.UnsmoothedPath

class GridSystem : AbstractSystem(Family.all(PositionComponent::class.java).get())
{
	// ----------------------------------------------------------------------
	var inTurn = true
	var matchCount = 0

	// ----------------------------------------------------------------------
	var noMatchTimer = 0f
	var matchHint: Pair<Point, Point>? = null

	// ----------------------------------------------------------------------
	var noValidMoves = false

	// ----------------------------------------------------------------------
	var dragStart: Point = Point.MINUS_ONE
	var toSwap: Pair<Point, Point>? = null

	// ----------------------------------------------------------------------
	val defaultAnimSpeed = 0.15f
	val animSpeedUpMultiplier = 0.975f
	var animSpeedMultiplier = 1f
	val animSpeed: Float
		get() = defaultAnimSpeed * animSpeedMultiplier

	// ----------------------------------------------------------------------
	val cascade = CascadeUpdateStep()
	val match = MatchUpdateStep()
	val sink = SinkUpdateStep()
	val detonate = DetonateUpdateStep()
	val updateGrid = UpdateGridUpdateStep()
	val cleanup = OnTurnCleanupUpdateStep()

	val updateSteps: Array<AbstractUpdateStep>

	// ----------------------------------------------------------------------
	var activeAbility: Ability? = null
		set(value)
		{
			field = value

			if (value == null)
			{
				for (tile in grid!!.grid)
				{
					tile.isSelected = false
				}
			}
			else
			{
				dragStart = Point.MINUS_ONE

				if (value.targets == 0)
				{
					value.activate(grid!!)
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
			updateGrid,
			cleanup)
	}

	// ----------------------------------------------------------------------
	override fun doUpdate(deltaTime: Float)
	{
		// process tiles
		val grid = grid!!
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]

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

		// process entities
		for (entity in entities)
		{
			processEntityRealTime(entity, deltaTime)
		}

		// update grid
		updateGrid(deltaTime)
	}

	// ----------------------------------------------------------------------
	private fun processEntityRealTime(entity: Entity, deltaTime: Float)
	{
		val renderable = entity.renderableOrNull()

		val monsterEffect = entity.monsterEffect()
		if (renderable != null && monsterEffect != null)
		{
			if (monsterEffect.monsterEffect.delayDisplay > 0)
			{
				monsterEffect.monsterEffect.delayDisplay -= deltaTime

				if (monsterEffect.monsterEffect.delayDisplay <= 0)
				{
					val newSprite = monsterEffect.monsterEffect.actualSprite.copy()

					val matchable = entity.matchable()
					if (matchable != null)
					{
						newSprite.colour = matchable.desc.sprite.colour
					}

					renderable.renderable = newSprite
				}
			}
		}

	}

	// ----------------------------------------------------------------------
	private fun updateGrid(delta: Float)
	{
		val grid = grid!!

		if (!grid.hasAnim())
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
				updatePlayerInput(delta)
			}
		}
	}

	// ----------------------------------------------------------------------
	fun updatePlayerInput(delta: Float)
	{
		val grid = grid!!

		if (!grid.level.completed && FullscreenMessage.instance == null)
		{
			if (activeAbility == null) matchHint = findValidMove()
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

				if (Tutorial.current == null) grid.onTime(delta)
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun swap(): Boolean
	{
		val grid = grid!!

		val oldTile = grid.tile(toSwap!!.first)
		val newTile = grid.tile(toSwap!!.second)

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
			val merged = newSpecial?.merge(oldSwap) ?: oldSpecial?.merge(newSwap)
			if (merged != null)
			{
				var specialHolder = newEntity.special()
				if (specialHolder == null)
				{
					specialHolder = SpecialComponent.obtain()
					newEntity.add(specialHolder)
				}

				specialHolder.special = merged

				val sprite = oldEntity.renderable().renderable.copy()
				sprite.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile, true)), Interpolation.linear)
				newTile.effects.add(sprite)

				oldTile.contents = null

				merged.armed = true
				newEntity.add(MarkedForDeletionComponent.obtain())

				return false
			}
		}

		oldTile.contents = newEntity
		newTile.contents = oldEntity

		val matches = match.findMatches(grid)
		for (match in matches) match.free()
		if (matches.size == 0)
		{
			oldTile.contents = oldEntity
			newTile.contents = newEntity

			var dir = Direction.getDirection(oldTile, newTile)
			if (dir.y != 0) dir = dir.opposite
			oldEntity.renderable().renderable.animation = BumpAnimation.obtain().set(animSpeed, dir)
			return false
		}
		else
		{
			grid.lastSwapped = newTile

			oldEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile)).invertY(), Interpolation.linear)
			newEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(oldTile.getPosDiff(newTile)).invertY(), Interpolation.linear)
			return true
		}
	}

	// ----------------------------------------------------------------------
	private fun findValidMove() : Pair<Point, Point>?
	{
		val grid = grid!!

		// find all 2 matches
		val matches = match.findMatches(grid, 2)

		for (match in matches)
		{
			// check the 3 tiles around each end to see if it contains one of the correct colours
			val dir = match.direction()
			val key = grid.grid[match.p1].contents!!.matchable()!!.desc.key

			fun checkSurrounding(point: Point, dir: Direction, key: Int): Pair<Point, Point>?
			{
				val targetTile = grid.tile(point)
				if (targetTile?.contents?.swappable()?.canMove != true) return null

				fun canMatch(point: Point): Boolean
				{
					val tile = grid.tile(point) ?: return false
					val swappable = tile.contents?.swappable() ?: return false
					val matchable = tile.contents?.matchable() ?: return false
					if (!swappable.canMove) return false
					return matchable.desc.key == key
				}

				// check + dir
				if (canMatch(point + dir)) return Pair(point, point+dir)
				if (canMatch(point + dir.cardinalClockwise)) return Pair(point, point+dir.cardinalClockwise)
				if (canMatch(point + dir.cardinalAnticlockwise)) return Pair(point, point+dir.cardinalAnticlockwise)

				return null
			}

			// the one before first is at first-dir
			val beforeFirst = match.p1 + dir.opposite
			val beforeFirstPair = checkSurrounding(beforeFirst, dir.opposite, key)
			if (beforeFirstPair != null)
			{
				for (match in matches) match.free()
				return beforeFirstPair
			}

			val afterSecond = match.p2 + dir
			val afterSecondPair = checkSurrounding(afterSecond, dir, key)
			if (afterSecondPair != null)
			{
				for (match in matches) match.free()
				return afterSecondPair
			}
		}

		for (match in matches) match.free()

		fun getTileKey(x: Int, y: Int, dir: Direction): Int
		{
			val tile = grid.tile(x + dir.x, y + dir.y) ?: return -1
			val swappable = tile.contents?.swappable() ?: return -1
			val matchable = tile.contents?.matchable() ?: return -1
			if (!swappable.canMove) return -1

			return matchable.desc.key
		}

		// check diamond pattern
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.tile(x, y) ?: continue
				val swappable = tile.contents?.swappable() ?: continue
				if (!swappable.canMove) continue

				for (dir in Direction.CardinalValues)
				{
					val key = getTileKey(x, y, dir)
					if (key != -1)
					{
						val k1 = getTileKey(x, y, dir.cardinalClockwise)
						val k2 = getTileKey(x, y, dir.cardinalAnticlockwise)

						if (key == k1 && key == k2)
						{
							return Pair(Point(x, y), Point(x + dir.x, y + dir.y))
						}
					}
				}
			}
		}

		// check for special merges
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val special = grid.grid[x, y].contents?.special() ?: continue
				for (dir in Direction.CardinalValues)
				{
					val tile = grid.tile(x + dir.x, y + dir.y) ?: continue
					val tileSpecial = tile.contents?.special() ?: continue
					if (special.special.merge(tileSpecial.special) != null)
					{
						return Pair(Point(x, y), Point(x + dir.x, y + dir.y))
					}
				}
			}
		}

		// else no valid

		return null
	}

	// ----------------------------------------------------------------------
	override fun onTurn()
	{

	}
}