package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.*
import com.lyeeedar.Util.Statics

class DetonateUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	private fun detonate(grid: Grid): Boolean
	{
		val trace = Statics.performanceTracer.getTrace("Detonate")
		trace.start()

		var complete = true

		val tilesToDetonate = Array<Tile>()

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val special = tile.contents?.special() ?: continue

				if (special.special.armed)
				{
					if (tile.spreader != null)
					{
						special.special.setArmed(false, tile.contents!!)
					}
					else
					{
						tilesToDetonate.add(tile)
					}
				}
			}
		}

		for (tile in tilesToDetonate)
		{
			val contents = tile.contents!!
			val special = contents.special()!!

			grid.replay.logAction("detonating special ${special.special} at (${tile.x},${tile.y})")

			special.special.apply(tile, grid)

			contents.removeComponent(ComponentType.Special)
			contents.markForDeletion(0f, "detonated")
			complete = false
		}

		if (!complete)
		{
			grid.matchCount++
			grid.animSpeedMultiplier *= grid.animSpeedUpMultiplier

			val point = tilesToDetonate.random()
			grid.match.displayMatchMessage(grid, point!!)
		}

		trace.stop()

		return complete
	}

	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{

	}

	// ----------------------------------------------------------------------
	override fun doUpdate(grid: Grid): Boolean
	{
		return detonate(grid)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(grid: Grid)
	{

	}
}