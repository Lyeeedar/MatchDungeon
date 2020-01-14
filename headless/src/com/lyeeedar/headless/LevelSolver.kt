package com.lyeeedar.headless

import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Level
import com.lyeeedar.Board.Mote
import com.lyeeedar.Board.Theme
import com.lyeeedar.Components.renderable
import com.lyeeedar.Game.*
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData

class LevelSolver
{
	fun attemptAllLevels()
	{
		Global.resolveInstantly = true

		val theme = Theme.load("Themes/City")
		val gridScreen = GridScreen()

		for (path in XmlData.enumeratePaths("", "Level"))
		{
			val levels = Level.load(path)

			var i = 0
			for (level in levels)
			{
				val character = Character.load("Wizard")
				val player = Player(character, PlayerDeck())
				Global.player = player
				Global.deck = GlobalDeck()

				level.create(theme, player, {}, {})

				for (cond in level.victoryConditions)
				{
					cond.createTable(level.grid)
				}
				for (cond in level.defeatConditions)
				{
					cond.createTable(level.grid)
				}

				val grid = level.grid

				try
				{
					println("Solving level '$path' variant '$i'")
					val vistory = solve(grid)
					println("Level solved. Victory=$vistory")
				}
				catch (ex: Exception)
				{
					println("Solving level '$path' variant '$i' crashed!")

					throw ex
				}

				i++
			}
		}

		Global.resolveInstantly = false
	}

	fun solve(grid: Grid): Boolean
	{
		val powerBar = PowerBar()
		val gridWidget = GridWidget(grid)

		var moveCount = 0
		while (!grid.level.isVictory && !grid.level.isDefeat)
		{
			moveCount++

			makeMove(grid)
			grid.update(1000f)
			Future.update(1000f)

			if (!grid.inTurn)
			{
				throw RuntimeException("Made a move but not in turn!")
			}

			while (grid.inTurn)
			{
				grid.update(1000f)
				Future.update(1000f)

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

			if (moveCount > 10 && moveCount.rem(20) == 0)
			{
				println("MoveCount: $moveCount")
			}
		}
		println("MoveCount: $moveCount")

		if (moveCount == 0)
		{
			throw RuntimeException("Level completed without making any moves!")
		}

		if (grid.level.isDefeat)
		{
			println("Defeat reason: " + grid.level.defeatConditions.first{ it.isCompleted() }.javaClass.typeName.split("CompletionCondition").last())
		}

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