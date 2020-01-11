package com.lyeeedar.Board.GridUpdate

import com.lyeeedar.Board.Grid
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.removeFromTile
import com.lyeeedar.Components.sinkable
import com.lyeeedar.Systems.GridSystem

class SinkUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	fun sink(grid: Grid): Boolean
	{
		var complete = true

		for (x in 0 until grid.width)
		{
			var y = grid.height-1
			while (y >= 0 && grid.grid[x, y].isPit)
			{
				y--
			}

			if (y == -1) continue

			val tile = grid.grid[x, y]

			val contents = tile.contents
			val sink = contents?.sinkable()
			if (sink != null)
			{
				contents.pos().removeFromTile(contents)

				grid.onSunk(contents)

				complete = false
			}
		}

		return complete
	}

	// ----------------------------------------------------------------------
	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		return sink(gridSystem.grid!!)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(gridSystem: GridSystem)
	{

	}
}