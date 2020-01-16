package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Array
import com.kryo.deserialize
import com.kryo.serialize
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTime
import com.lyeeedar.Components.renderable
import com.lyeeedar.Game.*
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import org.mockito.Mockito
import java.io.File

object CrashedLevelReplayer
{
	@JvmStatic fun main(arg: kotlin.Array<String>)
	{
		Gdx.gl = Mockito.mock(GL20::class.java)
		Gdx.gl20 = Mockito.mock(GL20::class.java)
		Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))

		try
		{
			LevelSolver().replayCrashedLevel()
		}
		finally
		{
			Gdx.app.exit()
		}
	}
}

class LevelSolver
{
	fun replayCrashedLevel()
	{
		Global.resolveInstantly = true

		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Crashed Level Replay      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val file = File("crashedLevelReplay")
		val replay = deserialize(file.readBytes()) as Replay

		println("Replaying level ${replay.levelPath} variant ${replay.variant}")

		Global.globalflags = deserialize(replay.globalflags) as GameStateFlags
		Global.questflags = deserialize(replay.questflags) as GameStateFlags
		Global.cardflags = deserialize(replay.cardflags) as GameStateFlags
		Global.deck = deserialize(replay.globalDeck) as GlobalDeck
		Global.player = deserialize(replay.player) as Player

		val theme = Theme.load(replay.questTheme)
		val levels = Level.load(replay.levelPath)
		val level = levels[replay.variant]
		level.create(theme, Global.player, {}, {}, replay.seed, replay.variant)

		for (cond in level.victoryConditions)
		{
			cond.createTable(level.grid)
		}
		for (cond in level.defeatConditions)
		{
			cond.createTable(level.grid)
		}

		resolve(level.grid, replay.moves, true)

		Global.resolveInstantly = false
	}

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
		var pathI = 0
		var success = 0
		var total = 0
		for (path in paths)
		{
			pathI++
			println("")
			println("-----------------------------------------------------------------------------")
			println("    $pathI / ${paths.size}")
			println("")

			val levels = Level.load(path)
			val resolveLevels = Level.load(path)

			for (i in 0 until levels.size)
			{
				val seed = Random.random.nextLong()
				fun createLevel(level: Level)
				{
					val character = Character.load("Adventurer")
					val player = Player(character, PlayerDeck())
					Global.player = player
					Global.deck = GlobalDeck()
					Global.deck.chosenCharacter = character
					Global.deck.characters.add(character)

					level.create(theme, player, {}, {}, seed, i)

					for (cond in level.victoryConditions)
					{
						cond.createTable(level.grid)
					}
					for (cond in level.defeatConditions)
					{
						cond.createTable(level.grid)
					}
				}

				createLevel(levels[i])

				try
				{
					println("")
					println("Solving level '$path' variant '$i'")
					val victory = solve(levels[i].grid)
					println("Level solved. Victory=$victory")

					if (victory)
					{
						success++
					}
					total++
				}
				catch (ex: Exception)
				{
					println("Solving level '$path' variant '$i' crashed!")

					val replay = serialize(levels[i].grid.replay)
					val file = File("crashedLevelReplay")
					file.writeBytes(replay)

					throw ex
				}

				createLevel(resolveLevels[i])

				try
				{
					println("")
					println("Resolving level '$path' variant '$i'")
					val victory = resolve(resolveLevels[i].grid, levels[i].grid.replay.moves)
					println("Level solved. Victory=$victory")

					if (levels[i].grid.grid.toString() != resolveLevels[i].grid.grid.toString())
					{
						throw RuntimeException("History didnt give the same result!")
					}
				}
				catch (ex: Exception)
				{
					println("Resolving level '$path' variant '$i' crashed!")

					fun dumpReplayToDisk(replay: Replay, path: String)
					{
						File(path).writeText(replay.toString())
					}

					dumpReplayToDisk(levels[i].grid.replay, "original_solve.txt")
					dumpReplayToDisk(resolveLevels[i].grid.replay, "resolve.txt")

					throw ex
				}
			}
		}

		println("")
		println("Successful: $success / $total")

		Global.resolveInstantly = false
	}

	fun solve(grid: Grid): Boolean
	{
		return solveLevel(grid, fun (moveCount: Int) { makeMove(grid, moveCount > 200)})
	}

	fun resolve(grid: Grid, moves: Array<HistoryMove>, print: Boolean = false): Boolean
	{
		return solveLevel(grid, fun (moveCount: Int) {
			if (print)
			{
				println("Move: $moveCount")
				println("")
				println(grid.grid.toString())
				println("")
				println("")
			}

			makeMove(grid, moves[moveCount])
		})
	}

	fun solveLevel(grid: Grid, moveFunc: (Int)->Unit): Boolean
	{
		val powerBar = PowerBar()
		val gridWidget = GridWidget(grid)

		var moveCount = 0
		while (!grid.level.isVictory && !grid.level.isDefeat)
		{
			completeTurn(grid)

			if (moveCount > 10 && moveCount.rem(20) == 0)
			{
				println("MoveCount: $moveCount")
			}

			if (moveCount > 1000)
			{
				throw RuntimeException("Level took over 1000 moves, something is wrong")
			}

			if (!grid.level.isVictory && !grid.level.isDefeat)
			{
				moveFunc(moveCount)
				moveCount++
			}
		}
		println("MoveCount: $moveCount")

		if (grid.level.isVictory || !grid.level.defeatConditions.any { it is CompletionConditionTime })
		{
			if (moveCount == 0)
			{
				throw RuntimeException("Level completed without making any moves!")
			}
			else if (moveCount < 5)
			{
				// check if it may be a valid level
				val hasDetonations = grid.replay.moves.any { it.gridActionLog.any { it.contains("detonating") } }

				if (moveCount < 3 || !hasDetonations)
				{
					throw RuntimeException("Level completed in under 5 moves. This seems suspicious!")
				}
				else
				{
					System.err.println("Level took less than 5 turns but its probably alright")
				}
			}
		}

		if (grid.level.isDefeat)
		{
			println("Defeat reason: " + grid.level.defeatConditions.first{ it.isCompleted() }.javaClass.typeName.split("CompletionCondition").last())
		}

		return grid.level.isVictory
	}

	fun completeTurn(grid: Grid)
	{
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
				println("UpdateCount: $updateCount")
			}

			if (updateCount > 1000)
			{
				throw RuntimeException("Turn got stuck in infinite update loop!")
			}
		}
	}

	fun makeMove(grid: Grid, print: Boolean)
	{
		var move = ""
		val historyMove: HistoryMove
		if (PowerBar.instance.power == PowerBar.instance.maxPower)
		{
			historyMove = HistoryMove(true, grid.grid.toString())
			PowerBar.instance.power = 0
			move = "refill"
		}
		else
		{
			if (grid.isUpdating || grid.inTurn)
			{
				throw RuntimeException("Grid was still updating when entering make move. Updating ${grid.isUpdating}. InTurn ${grid.inTurn}")
			}

			val bestMove = grid.cleanup.findBestMove(grid)
			if (bestMove == null)
			{
				historyMove = HistoryMove(true, grid.grid.toString())
				PowerBar.instance.power = 0
				move = "refill"
			}
			else
			{
				move = bestMove.name

				historyMove = HistoryMove(bestMove.swapStart, bestMove.swapEnd, grid.grid.toString())
			}
		}

		if (print)
		{
			println("Making move $move")
		}

		makeMove(grid, historyMove)
	}

	fun makeMove(grid: Grid, historyMove: HistoryMove)
	{
		val levelState = grid.grid.toString()
		if (levelState != historyMove.levelSnapshot)
		{
			throw RuntimeException("History move doesnt match grid state!\nLevel:\n$levelState\n\nHistory:\n${historyMove.levelSnapshot}")
		}

		if (historyMove.refill)
		{
			grid.refill()
			return
		}
		else if (historyMove.swapStart != null)
		{
			grid.dragStart = historyMove.swapStart!!
			grid.dragEnd(historyMove.swapEnd!!)

			try
			{
				grid.update(1000f)
				Future.update(1000f)
			}
			catch (ex: java.lang.RuntimeException)
			{
				if (ex.message?.contains("Swap") == true)
				{
					throw RuntimeException("Swap was not a valid swap!")
				}
				else
				{
					throw ex
				}
			}

			if (!grid.inTurn && !(grid.level.isVictory || grid.level.isDefeat))
			{
				throw RuntimeException("Made a move but not in turn! Updating: " + grid.isUpdating)
			}
		}
		else
		{
			throw RuntimeException("Abilities not supported by level solver!")
		}
	}
}