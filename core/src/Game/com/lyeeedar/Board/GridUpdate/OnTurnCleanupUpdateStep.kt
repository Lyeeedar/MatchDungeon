package com.lyeeedar.Board.GridUpdate

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.AlphaAnimation
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.randomize
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray

class OnTurnCleanupUpdateStep : AbstractUpdateStep()
{
	val deletedEntities = ObjectSet<Entity>()

	// ---------------------------------------------------------------------
	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{
		deletedEntities.clear()

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val contents = tile.contents ?: continue

				if (deletedEntities.contains(contents)) continue
				deletedEntities.add(contents)

				if (contents.isMarkedForDeletion())
				{
					processDeletion(contents, grid, tile, deltaTime)
				}
			}
		}

		if (grid.noValidMoves)
		{
			grid.matchHint = findBestMove(grid)
		}

		EntityPool.flushFreedEntities()
	}

	// ---------------------------------------------------------------------
	private fun processDeletion(entity: Entity, grid: Grid, tile: Tile, deltaTime: Float)
	{
		val markedForDeletion = entity.markedForDeletion()!!
		if (markedForDeletion.deletionEffectDelay > 0)
		{
			markedForDeletion.deletionEffectDelay -= deltaTime

			if (markedForDeletion.deletionEffectDelay > 0)
			{
				return
			}
		}

		var doRemove = true

		val pos = entity.posOrNull()
		val renderable = entity.renderableOrNull()
		val tile = if (pos != null) pos.tile ?: grid.getTileClamped(pos.position) else grid.grid[0, 0]

		val damageableComponent = entity.damageable()
		if (pos != null && damageableComponent != null && doRemove)
		{
			val death = damageableComponent.deathEffect.copy()
			death.size[0] = pos.size
			death.size[1] = pos.size

			tile.effects.add(death)

			if (entity.isMonster())
			{
				var logAction = "deleting monster ${entity.niceName()} at (${tile.toShortString()}) because (${entity.markedForDeletion()!!.reason})"

				val necroticAura = Global.player.getStat(Statistic.NECROTICAURA)
				if (necroticAura != 0f)
				{
					val die = grid.level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()
					if (die != null)
					{
						die.fractionalHp += necroticAura
					}
				}

				val monsterAI = entity.ai()?.ai as? MonsterAI
				if (monsterAI != null)
				{
					val rootDesc = monsterAI.desc.originalDesc ?: monsterAI.desc
					if (rootDesc.stages.size > 0)
					{
						val currentStage = if (monsterAI.desc.originalDesc == null) -1 else rootDesc.stages.indexOf(monsterAI.desc)

						if (currentStage < rootDesc.stages.size - 1)
						{
							val nextDesc = rootDesc.stages[currentStage + 1]
							val monster = nextDesc.getEntity(monsterAI.difficulty, false, grid)
							monster.pos().setTile(monster, tile)

							logAction += " spawning next monster stage"
						}
					}
				}

				grid.replay.logAction(logAction)
			}
		}

		val containerComponent = entity.container()
		if (pos != null && containerComponent != null && containerComponent.containedEntity != null && doRemove)
		{
			val centity = containerComponent.containedEntity!!
			centity.pos().tile = tile
			centity.pos().addToTile(centity)
		}

		val special = entity.special()
		if (special != null && doRemove)
		{
			if (special.special.armed)
			{
				doRemove = false
			}
		}

		val monsterEffect = entity.monsterEffect()
		if (pos != null && renderable != null && monsterEffect != null && doRemove)
		{
			if (renderable.renderable.animation == null)
			{
				monsterEffect.monsterEffect.apply(grid, tile)
			}
			else
			{
				doRemove = false
			}
		}

		val matchableComponent = entity.matchable()
		if (pos != null && renderable != null && matchableComponent != null && doRemove)
		{
			if (renderable.renderable.animation == null)
			{
				grid.onPop(entity, matchableComponent.deletionEffectDelay)

				if (matchableComponent.deletionEffectDelay >= 0.2f)
				{
					val sprite = renderable.renderable.copy()
					sprite.renderDelay = matchableComponent.deletionEffectDelay - 0.2f
					sprite.showBeforeRender = true
					sprite.animation = AlphaAnimation.obtain().set(0.2f, 1f, 0f, sprite.colour)
					tile.effects.add(sprite)
				}
			}
			else
			{
				doRemove = false
			}
		}

		if (doRemove)
		{
			pos?.removeFromTile(entity)
			entity.free()

			if (tile.contents == entity)
			{
				tile.contents = null
			}
		}
	}

	// ---------------------------------------------------------------------
	override fun doUpdate(grid: Grid): Boolean
	{
		grid.animSpeedMultiplier = 1f

		return true
	}

	// ---------------------------------------------------------------------
	override fun doTurn(grid: Grid)
	{
		for (tile in grid.grid)
		{
			if (tile.contents != null)
			{
				val damageableComponent = tile.contents!!.damageable()
				val positionComponent = tile.contents!!.pos()

				if (damageableComponent != null && positionComponent.tile == tile)
				{
					damageableComponent.damSources.clear()
					damageableComponent.remainingReduction = damageableComponent.damageReduction
				}
			}
		}

		for (buff in Global.player.levelbuffs.toGdxArray())
		{
			buff.remainingDuration--
			if (buff.remainingDuration <= 0)
			{
				Global.player.levelbuffs.removeValue(buff, true)
			}
		}

		for (debuff in Global.player.leveldebuffs.toGdxArray())
		{
			debuff.remainingDuration--
			if (debuff.remainingDuration <= 0)
			{
				Global.player.leveldebuffs.removeValue(debuff, true)
			}
		}

		if (!Global.resolveInstantly)
		{
			GridScreen.instance.updateBuffTable()
		}

		grid.gainedBonusPower = false
		grid.poppedSpreaders.clear()
		grid.animSpeedMultiplier = 1f
		grid.matchCount = 0
		grid.noMatchTimer = 0f
		grid.inTurn = false
		grid.matchHint = findBestMove(grid)
	}

	// ---------------------------------------------------------------------
	class MovePriority(val targetTiles: Array<Tile>, val direct: Boolean)

	// ---------------------------------------------------------------------
	fun findBestMove(grid: Grid): ValidMove?
	{
		val validMoves = findValidMoves(grid)
		if (validMoves.size == 0) return null

		val movePriorities = Array<MovePriority>()

		// blocks
		if (grid.level.victoryConditions.any{ it is CompletionConditionBreak || it is CompletionConditionSink })
		{
			if (grid.breakableTiles.size > 0)
			{
				movePriorities.add(MovePriority(grid.breakableTiles, false))
			}
		}

		// spreaders
		val spreaderTiles = grid.grid.filter { it.spreader != null }.toList().toGdxArray()
		if (spreaderTiles.size > 0)
		{
			movePriorities.add(MovePriority(spreaderTiles, false))
		}

		// plates
		if (grid.level.victoryConditions.any { it is CompletionConditionPlate })
		{
			val plateTiles = grid.grid.filter { it.hasPlate }.toList().toGdxArray()
			if (plateTiles.size > 0)
			{
				movePriorities.add(MovePriority(plateTiles, true))
			}
		}

		// sinkables
		if (grid.level.victoryConditions.any{ it is CompletionConditionSink })
		{
			if (grid.sinkPathTiles.size > 0)
			{
				movePriorities.add(MovePriority(grid.sinkPathTiles, true))
			}
		}

		// if under half health, prioritise clearing attacks
		if (grid.level.defeatConditions.any{ it is CompletionConditionDie })
		{
			val die = grid.level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()
			if (die != null && die.hp < die.maxHP * 0.5f)
			{
				if (grid.attackTiles.size > 0)
				{
					movePriorities.add(MovePriority(grid.attackTiles, true))
				}
			}
		}

		// monsters
		if (grid.level.victoryConditions.any{ it is CompletionConditionKill })
		{
			if (grid.monsterTiles.size > 0)
			{
				movePriorities.add(MovePriority(grid.monsterTiles, false))
			}
		}

		// attacks
		if (grid.attackTiles.size > 0)
		{
			movePriorities.add(MovePriority(grid.attackTiles, true))
		}

		// attempt direct matches
		for (priority in movePriorities)
		{
			// attempt to fulfill the requirements directly
			var bestMove: ValidMove? = null
			var bestCount = 0

			for (move in validMoves.randomize())
			{
				var count = 0
				for (point in move.points)
				{
					val allowedDist = if (priority.direct) 0 else 1
					if (priority.targetTiles.any { it.taxiDist(point) <= allowedDist })
					{
						count++
					}
				}

				if (count > bestCount)
				{
					bestCount = count
					bestMove = move
				}
			}

			if (bestMove != null)
			{
				return bestMove
			}
		}

		// get a close enough match
		var bestMove: ValidMove? = null
		var bestDist = Int.MAX_VALUE
		for (priority in movePriorities)
		{
			for (move in validMoves.randomize())
			{
				for (point in move.points)
				{
					val minDist = priority.targetTiles.map { it.taxiDist(point) }.min()!!
					if (minDist < bestDist)
					{
						bestDist = minDist
						bestMove = move
					}
				}
			}
		}
		if (bestMove != null)
		{
			return bestMove
		}

		// try to match based on orb descs
		if (grid.level.victoryConditions.any{ it is CompletionConditionMatches || it is CompletionConditionCustomOrb })
		{
			val keys = Array<Int>()

			val matches = grid.level.victoryConditions.filterIsInstance<CompletionConditionMatches>().firstOrNull()
			if (matches != null)
			{
				for (match in matches.toBeMatched)
				{
					if (match.value > 0)
					{
						keys.add(match.key)
					}
				}
			}

			val custom = grid.level.victoryConditions.filterIsInstance<CompletionConditionCustomOrb>().firstOrNull()
			if (custom != null)
			{
				if (!custom.isCompleted())
				{
					val key = OrbDesc.getNamedOrb(custom.targetOrbName).key
					keys.add(key)
				}
			}

			for (key in keys)
			{
				for (swap in validMoves.randomize())
				{
					var matchingCount = 0
					for (point in swap.points)
					{
						val tile = grid.tile(point) ?: continue
						val contents = tile.contents ?: continue
						val matchable = contents.matchable() ?: continue

						if (matchable.desc.key == key)
						{
							matchingCount++
						}
					}

					if (matchingCount >= 2)
					{
						return swap
					}
				}
			}
		}

		// random
		return validMoves.maxBy { it.swapStart.y }
	}

	// ----------------------------------------------------------------------
	private fun findValidMoves(grid: Grid) : Array<ValidMove>
	{
		val validMoves = Array<ValidMove>()

		// find all 2 matches
		val matches = grid.match.findMatches(grid.grid, 2)

		fun getTileKey(point: Point, direction: Direction): Int
		{
			val tile = grid.getTile(point, direction) ?: return -1
			if (tile.spreader != null) return -1
			val swappable = tile.contents?.swappable() ?: return -1
			val matchable = tile.contents?.matchable() ?: return -1
			if (!swappable.canMove) return -1

			return matchable.desc.key
		}

		for (match in matches)
		{
			// check the 3 tiles around each end to see if it contains one of the correct colours
			val dir = match.direction()
			val key = grid.grid[match.p1].contents!!.matchable()!!.desc.key

			fun checkSurrounding(point: Point, dir: Direction, key: Int): Pair<Point, Point>?
			{
				val targetTile = grid.tile(point)
				if (targetTile?.contents?.swappable()?.canMove != true || targetTile.spreader != null) return null

				// check + dir
				if (getTileKey(point, dir) == key) return Pair(point, point+dir)
				if (getTileKey(point, dir.cardinalClockwise) == key) return Pair(point, point+dir.cardinalClockwise)
				if (getTileKey(point, dir.cardinalAnticlockwise) == key) return Pair(point, point+dir.cardinalAnticlockwise)

				return null
			}

			// the one before first is at first-dir
			val beforeFirst = match.p1 + dir.opposite
			val beforeFirstPair = checkSurrounding(beforeFirst, dir.opposite, key)
			if (beforeFirstPair != null)
			{
				val validMove = ValidMove(beforeFirstPair.first, beforeFirstPair.second, gdxArrayOf(beforeFirst, match.p1, match.p2), "AfterEndLeft")
				validMoves.add(validMove)
			}

			val afterSecond = match.p2 + dir
			val afterSecondPair = checkSurrounding(afterSecond, dir, key)
			if (afterSecondPair != null)
			{
				val validMove = ValidMove(afterSecondPair.first, afterSecondPair.second, gdxArrayOf(afterSecond, match.p1, match.p2), "AfterEndRight")
				validMoves.add(validMove)
			}
		}

		for (match in matches) match.free()

		// check diamond pattern
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.tile(x, y) ?: continue
				if (tile.spreader != null) continue
				val swappable = tile.contents?.swappable() ?: continue
				if (!swappable.canMove) continue

				for (dir in Direction.CardinalValues)
				{
					val key = getTileKey(tile, dir)
					if (key != -1)
					{
						val k1 = getTileKey(tile, dir.cardinalClockwise)
						val k2 = getTileKey(tile, dir.cardinalAnticlockwise)

						if (key == k1 && key == k2)
						{
							val p1 = Point(x, y)
							val p2 = tile + dir.cardinalClockwise
							val p3 = tile + dir.cardinalAnticlockwise
							val validMove = ValidMove(p1, p1 + dir, gdxArrayOf(p1, p2, p3), "Diamond")
							validMoves.add(validMove)
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
				if (grid.grid[x, y].spreader != null) continue
				val contents = grid.grid[x, y].contents ?: continue
				val special = contents.special() ?: continue
				for (dir in Direction.CardinalValues)
				{
					val tile = grid.tile(x + dir.x, y + dir.y) ?: continue
					if (tile.spreader != null) continue
					val tileSpecial = tile.contents?.special() ?: continue
					if (special.special.merge(tile.contents!!) != null || tileSpecial.special.merge(contents) != null)
					{
						val p1 = Point(x, y)
						val p2 = Point(x + dir.x, y + dir.y)

						val validMove = ValidMove(p1, p2, gdxArrayOf(p1, p2), "SpecialMerge")
						validMoves.add(validMove)
					}
				}
			}
		}

		return validMoves
	}
}

class ValidMove(val swapStart: Point, val swapEnd: Point, val points: Array<Point>, val name: String)