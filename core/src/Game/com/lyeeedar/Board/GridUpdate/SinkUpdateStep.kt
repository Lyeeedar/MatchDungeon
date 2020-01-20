package com.lyeeedar.Board.GridUpdate

import com.lyeeedar.Board.Grid
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.removeFromTile
import com.lyeeedar.Components.sinkable
import com.lyeeedar.Util.Statics

class SinkUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	fun sink(grid: Grid): Boolean
	{
		val trace = Statics.crashReporter.getTrace("Sink")
		trace.start()

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

				grid.replay.logAction("Sinking sinkable at (${tile.toShortString()})")

				complete = false
			}
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
		return sink(grid)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(grid: Grid)
	{

	}
}