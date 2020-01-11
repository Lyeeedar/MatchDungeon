package com.lyeeedar.Board.GridUpdate

import com.lyeeedar.Systems.GridSystem

abstract class AbstractUpdateStep
{
	abstract fun doUpdate(gridSystem: GridSystem): Boolean

	var wasRunThisTurn = false
	abstract fun doTurn(gridSystem: GridSystem)
}