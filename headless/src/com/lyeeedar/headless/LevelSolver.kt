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
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Level Solver      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		Global.resolveInstantly = true

		val theme = Theme.load("Themes/City")
		val gridScreen = GridScreen()

		val paths = XmlData.enumeratePaths("", "Level").toList()
		for (path in paths)
		{
			if (path.contains("CombatTraining"))
			{
				var i = 0
			}

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

			makeMove(grid, moveCount > 200)

			var updateCount = 0
			while (grid.inTurn || grid.isUpdating)
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

				updateCount++

				if (updateCount > 10 && updateCount.rem(20) == 0)
				{
					println("UpdateCount: $updateCount, CurrentStep: " + grid.currentStep)
				}

				if (updateCount > 1000)
				{
					throw RuntimeException("Turn got stuck in infinite update loop! CurrentStep: " + grid.currentStep)
				}
				if (updateCount > 100)
				{
					if (!grid.DEBUG_match)
					{
						println("ENABLE DEBUG")
					}

					grid.DEBUG_match = true
					grid.DEBUG_matchDeleted = true
					println(grid.currentStep)
				}
			}
			grid.DEBUG_match = false

			if (moveCount > 10 && moveCount.rem(20) == 0)
			{
				println("MoveCount: $moveCount")
			}

			if (moveCount > 1000)
			{
				throw RuntimeException("Level took over 1000 moves, something is wrong")
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

	fun makeMove(grid: Grid, print: Boolean)
	{
		if (PowerBar.instance.power == PowerBar.instance.maxPower)
		{
			grid.refill()
			return
		}

		var move = ""

		while (grid.isUpdating)
		{
			grid.update(1000f)
			Future.update(1000f)
		}

		val bestMove = grid.matchHint
		if (bestMove == null)
		{
			grid.refill()
			move = "refill"
		}
		else
		{
			move = bestMove.name

			grid.dragStart = bestMove.swapStart
			grid.dragEnd(bestMove.swapEnd)
		}

		if (print)
		{
			println("Making move $move")
		}

		try
		{
			grid.update(1000f)
			Future.update(1000f)
		}
		catch (ex: java.lang.RuntimeException)
		{
			if (ex.message?.contains("Swap") == true)
			{
				throw RuntimeException("Swap $move was not a valid swap!")
			}
			else
			{
				throw ex
			}
		}

		if (!grid.inTurn)
		{
			throw RuntimeException("Made a move ($move) but not in turn! Updating: " + grid.isUpdating)
		}
	}
}