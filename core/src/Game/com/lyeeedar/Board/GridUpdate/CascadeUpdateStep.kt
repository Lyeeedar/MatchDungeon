package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.SpreaderEffect
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.UnsmoothedPath

class CascadeUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	enum class SearchState
	{
		NOTLOOKING,
		LOOKING,
		PLACED
	}

	// ----------------------------------------------------------------------
	private fun cascade(grid: Grid): Boolean
	{
		val trace = Statics.performanceTracer.getTrace("Cascade")
		trace.start()

		for (x in 0 until grid.width) for (y in 0 until grid.height)
		{
			grid.spawnCount[x, y] = 0

			val tile = grid.grid[x, y]
			val swappable = tile.contents?.swappable()
			if (swappable != null)
			{
				if (swappable.cascadeCount == -1)
				{
					swappable.cascadeCount = 1
				}
				else
				{
					swappable.cascadeCount = 0
				}
			}
		}

		var cascadeComplete = false

		var cascadeCount = 0

		while (!cascadeComplete)
		{
			cascadeComplete = true

			for (x in 0 until grid.width)
			{
				val done = cascadeColumn(grid, x, cascadeCount)
				if (!done) cascadeComplete = false
			}

			cascadeCount++
		}

		cascadeComplete = makeAnimations(grid)

		trace.stop()

		return cascadeComplete
	}

	// ----------------------------------------------------------------------
	private fun pullOrbDown(grid: Grid, x: Int, currentY: Int, emptyTile: Tile, cascadeCount: Int): Boolean
	{
		var didMove = false

		// if gap found read up until solid / spawner
		var found: Tile? = null

		for (searchY in currentY downTo -1)
		{
			val stile = if (searchY >= 0) grid.grid[x, searchY] else null
			if (stile == null)
			{
				found = emptyTile
				break
			}
			else if (!stile.canHaveOrb && !stile.isPit && stile.contents == null)
			{
				break
			}
			else if (stile.spreader?.data?.effect == SpreaderEffect.SEAL)
			{
				break
			}
			else if (stile.contents != null)
			{
				val oentity = stile.contents!!

				val swappable = oentity.swappable()
				if (swappable != null && swappable.canMove)
				{
					found = stile
					break
				}

				val spawner = oentity.orbSpawner()
				if (spawner != null)
				{
					found = stile
					break
				}

				if (swappable == null || !swappable.canMove)
				{
					break
				}
			}
		}

		// pull solid / spawn new down
		if (found != null)
		{
			var entity: Entity? = null

			// we searched to the top, spawn a new orb at the top
			if (found == emptyTile)
			{
				entity = grid.level.spawnOrb()

				val swappable = entity.swappable()!!

				swappable.movePoints.add(Point(x, -1))
				swappable.spawnCount = grid.spawnCount[x, 0]

				grid.spawnCount[x, 0]++

				grid.onSpawn(entity)
			}
			// we found a spawner, spawn a new orb from it
			else if (found.contents?.orbSpawner() != null)
			{
				val spawner = found.contents!!.orbSpawner()!!
				val spawned = spawner.spawn(grid, found.contents!!)
				if (spawned != null)
				{
					entity = spawned

					val swappable = spawned.swappable()!!

					swappable.movePoints.add(Point(x, found.y))
					swappable.spawnCount = grid.spawnCount[x, found.y + 1]

					grid.spawnCount[x, found.y + 1]++

					grid.onSpawn(spawned)
				}
			}
			// else pull the found entity (a swappable) down
			else
			{
				entity = found.contents!!
				entity.pos()?.removeFromTile(entity)

				val swappable = entity.swappable()!!

				if (swappable.movePoints.size == 0) swappable.movePoints.add(found)
			}

			// if we found an entity, then move it
			if (entity != null)
			{
				entity.pos()?.tile = emptyTile
				entity.pos()?.addToTile(entity)

				val swappable = entity.swappable()!!

				swappable.movePoints.add(emptyTile)
				swappable.cascadeCount = cascadeCount

				didMove = true
			}
		}

		return didMove
	}

	// ----------------------------------------------------------------------
	private fun cascadeColumn(grid: Grid, x: Int, cascadeCount: Int) : Boolean
	{
		var complete = true

		var currentY = grid.height-1
		while (currentY >= 0)
		{
			val tile = grid.grid[x, currentY]

			// read up column, find first gap
			if (tile.canHaveOrb && tile.contents == null)
			{
				val didMove = pullOrbDown(grid, x, currentY, tile, cascadeCount)
				if (didMove)
				{
					complete = false
				}
			}

			currentY--
		}

		// walk down column
		// each block with a clear, push 1 orb into the top from a neighbour

		if (complete)
		{
			var lookingForOrb = SearchState.NOTLOOKING
			for (currentY in 0 until grid.height)
			{
				val tile = grid.grid[x, currentY]

				// if the tile can have an orb, but is empty, begin looking
				if (tile.canHaveOrb && tile.contents == null)
				{
					if (lookingForOrb == SearchState.NOTLOOKING)
					{
						lookingForOrb = SearchState.LOOKING
					}
				}
				// else if we reach a tile that cant have an orb,
				else if (!tile.canHaveOrb)
				{
					lookingForOrb = SearchState.NOTLOOKING
				}
				// else if we find a non empty tile, stop looking
				else if (tile.contents != null)
				{
					lookingForOrb = SearchState.NOTLOOKING
				}

				if (lookingForOrb == SearchState.LOOKING)
				{
					// check neighbours for orb
					val diagL = grid.tile(x - 1, currentY - 1)
					val diagLBelow = grid.tile(x - 1, currentY)
					val diagR = grid.tile(x + 1, currentY - 1)
					val diagRBelow = grid.tile(x + 1, currentY)

					// check if column block connects to the bottom of the grid
					var connectsToBottom = true
					for (y in currentY until grid.height)
					{
						val tile = grid.tile(x, y) ?: continue
						if (!tile.canHaveOrb && !tile.isPit)
						{
							connectsToBottom = false
							break
						}
					}

					fun tileValid(tile: Tile?, tileBelow: Tile?): Boolean
					{
						// if no contents, not valid
						val contents = tile?.contents ?: return false

						// if contents has no swappable, not valid
						if (contents.swappable() == null) return false

						// if contents is a sinkable, and this column does not connect to the bottom, not valid
						if (contents.sinkable() != null && !connectsToBottom) return false

						// if tile has a spreader with a seal effect, not valid
						if (tile.spreader != null && tile.spreader!!.data.effect == SpreaderEffect.SEAL) return false

						// if the tile to the left is empty, and can take an orb, then not valid
						if (tileBelow != null && tileBelow.canHaveOrb && tileBelow.contents == null) return false

						return true
					}

					val diagLValid = tileValid(diagL, diagLBelow)
					val diagRValid = tileValid(diagR, diagRBelow)

					if (diagLValid || diagRValid)
					{
						fun pullIn(othertile: Tile)
						{
							val orb = othertile.contents!!
							orb.pos()?.removeFromTile(orb)

							val swappable = orb.swappable()!!

							if (swappable.movePoints.size == 0) swappable.movePoints.add(othertile)

							orb.pos()?.tile = tile
							orb.pos()?.addToTile(orb)

							swappable.cascadeCount = cascadeCount
							swappable.movePoints.add(tile)

							complete = false
						}

						// if found one, pull in and set to 2
						if (diagLValid && diagRValid)
						{
							if (grid.ran.nextBoolean())
							{
								pullIn(diagL!!)
							}
							else
							{
								pullIn(diagR!!)
							}
						}
						else if (diagLValid)
						{
							pullIn(diagL!!)
						}
						else
						{
							pullIn(diagR!!)
						}

						lookingForOrb = SearchState.PLACED
					}
				}
			}
		}

		return complete
	}

	// ----------------------------------------------------------------------
	private fun makeAnimations(grid: Grid): Boolean
	{
		if (Global.resolveInstantly) return true

		var doneAnimation = true

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val orb = grid.grid[x, y].contents ?: continue
				val swappable = orb.swappable() ?: continue
				val renderable = orb.renderable() ?: continue

				if (swappable.movePoints.size > 0)
				{
					val firstIsNull = swappable.spawnCount >= 0

					val pathPoints = Array(swappable.movePoints.size){ i -> Vector2(swappable.movePoints[i].x.toFloat(), swappable.movePoints[i].y.toFloat()) }
					for (point in pathPoints)
					{
						point.x -= pathPoints.last().x
						point.y = pathPoints.last().y - point.y
					}

					val path = UnsmoothedPath(pathPoints)

					renderable.renderable.animation = MoveAnimation.obtain().set(grid.animSpeed + pathPoints.size * grid.animSpeed, path, Interpolation.ExpIn(2f, 5f))
					renderable.renderable.renderDelay = swappable.spawnCount * grid.animSpeed
					swappable.spawnCount = -1

					if (firstIsNull)
					{
						renderable.renderable.animation = ExpandAnimation.obtain().set(grid.animSpeed)
						renderable.renderable.showBeforeRender = false
					}

					swappable.movePoints.clear()

					doneAnimation = false
				}
			}
		}

		return doneAnimation
	}

	// ----------------------------------------------------------------------
	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{

	}

	// ----------------------------------------------------------------------
	override fun doUpdate(grid: Grid): Boolean
	{
		return cascade(grid)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(grid: Grid)
	{

	}
}