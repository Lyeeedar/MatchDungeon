package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.special
import com.lyeeedar.Systems.GridSystem

class DetonateUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	private fun detonate(gridSystem: GridSystem): Boolean
	{
		val grid = gridSystem.grid!!

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
						special.special.armed = false
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

			special.special.apply(tile, grid)

			contents.remove(special.javaClass)
			complete = false
		}

		if (!complete)
		{
			grid.matchCount++
			grid.animSpeedMultiplier *= grid.animSpeedUpMultiplier

			val point = tilesToDetonate.random()
			gridSystem.match.displayMatchMessage(grid, point!!)
		}

		return complete
	}

	// ----------------------------------------------------------------------
	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		return detonate(gridSystem)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(gridSystem: GridSystem)
	{

	}
}