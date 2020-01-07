package com.lyeeedar.Board

import com.lyeeedar.Board.CompletionCondition.CompletionConditionTurns
import com.lyeeedar.Renderables.Sprite.Sprite

/**
 * Created by Philip on 22-Jul-16.
 */

class Chest(val spawnOrbs: Boolean = true, val theme: Theme)
{
	var numToSpawn = 0
	var spacingCounter = 0

	val sprite: Sprite
		get() = if (numToSpawn > 0) fullSprite else emptySprite

	val fullSprite = theme.chestFull.copy()
	val emptySprite = theme.chestEmpty.copy()

	fun attachHandlers(grid: Grid)
	{

	}

	fun spawn(grid: Grid): Swappable?
	{
		if (spawnOrbs)
		{
			if (numToSpawn <= 0) return grid.level.spawnOrb()

			// make sure we dont flood the board
			val coinsOnBoard = grid.grid.filter { it.sinkable != null }.count() + 1
			if (coinsOnBoard >= 7) return grid.level.spawnOrb()

			var chosenSpacing = 3
			val turnsCondition = grid.level.defeatConditions.filterIsInstance<CompletionConditionTurns>().firstOrNull()
			if (turnsCondition != null)
			{
				if (turnsCondition.turnCount < turnsCondition.maxTurnCount / 4)
				{
					chosenSpacing = 0
				}
				else if (turnsCondition.turnCount < (turnsCondition.maxTurnCount / 4) * 2)
				{
					chosenSpacing = 1
				}
				else if (turnsCondition.turnCount < (turnsCondition.maxTurnCount / 4) * 3)
				{
					chosenSpacing = 2
				}
				else
				{
					chosenSpacing = 3
				}
			}

			if (spacingCounter < chosenSpacing)
			{
				spacingCounter++
				return grid.level.spawnOrb()
			}
			else
			{
				spacingCounter = 0
				numToSpawn--
				return Sinkable(theme.coin.copy(), theme)
			}
		}
		else
		{
			if (numToSpawn <= 0) return null
			numToSpawn--
			return Sinkable(theme.coin.copy(), theme)
		}
	}
}