package com.lyeeedar.headless

import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Mote
import com.lyeeedar.Components.renderable
import com.lyeeedar.Game.Global
import com.lyeeedar.UI.PowerBar

class LevelSolver
{
	fun solve(grid: Grid): Boolean
	{
		Global.resolveInstantly = true

		val powerBar = PowerBar()

		while (!grid.level.isVictory && !grid.level.isDefeat)
		{
			makeMove(grid)
			grid.update(1000f)

			while (grid.inTurn)
			{
				grid.update(1000f)

				for (tile in grid.grid)
				{
					tile.effects.clear()

					tile.contents?.renderable()?.renderable?.animation = null
				}

				Mote.clear()

				for (label in grid.match.messageList)
				{
					label.remove()
				}

				if (grid.hasAnim()) throw RuntimeException("Grid still has anim")
			}
		}

		Global.resolveInstantly = false

		return grid.level.isVictory
	}

	fun makeMove(grid: Grid)
	{
		if (PowerBar.instance.power == PowerBar.instance.maxPower)
		{
			grid.refill()
			return
		}

		val bestMove = grid.matchHint
		if (bestMove == null)
		{
			grid.refill()
		}
		else
		{
			grid.dragStart = bestMove.swapStart
			grid.dragEnd(bestMove.swapEnd)
		}
	}
}