package com.lyeeedar.Board.GridUpdate

import com.lyeeedar.Board.Grid

abstract class AbstractUpdateStep
{
	abstract fun doUpdateRealTile(grid: Grid, deltaTime: Float)

	abstract fun doUpdate(grid: Grid): Boolean

	var wasRunThisTurn = false
	abstract fun doTurn(grid: Grid)
}