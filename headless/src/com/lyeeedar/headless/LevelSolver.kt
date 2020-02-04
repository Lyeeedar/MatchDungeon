package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Array
import com.kryo.deserialize
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTime
import com.lyeeedar.Components.markedForDeletion
import com.lyeeedar.Components.renderable
import com.lyeeedar.Game.*
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.Util.*
import org.mockito.Mockito
import java.io.File
import kotlin.collections.random

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

object BruteForceLevelTester
{
	@JvmStatic fun main(arg: kotlin.Array<String>)
	{
		Gdx.gl = Mockito.mock(GL20::class.java)
		Gdx.gl20 = Mockito.mock(GL20::class.java)
		Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))

		try
		{
			LevelSolver().bruteForceLevelTester()
		}
		finally
		{
			Gdx.app.exit()
		}
	}
}

class LevelSolver
{
	var disableOutput = false

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

		val file = Gdx.files.local("crashedLevelReplay")
		val replay = Replay.loadFromString(file.readString())

		println("Replaying level ${replay.levelPath} variant ${replay.variant}")

		Global.globalflags = deserialize(replay.globalflags, GameStateFlags::class.java)
		Global.questflags = deserialize(replay.questflags, GameStateFlags::class.java)
		Global.cardflags = deserialize(replay.cardflags, GameStateFlags::class.java)
		Global.deck = deserialize(replay.globalDeck, GlobalDeck::class.java)
		Global.player = deserialize(replay.player, Player::class.java)

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

	fun bruteForceLevelTester()
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Brute Force Level Tester      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		Global.resolveInstantly = true
		disableOutput = true

		val themes = XmlData.enumeratePaths("", "Theme").toList()
		val characters = XmlData.enumeratePaths("", "Character").toList()

		val gridScreen = GridScreen()

		var totalI = 0
		var crashCount = 0

		val paths = XmlData.enumeratePaths("", "Level").toList()
		var pathI = 0

		var lastTime = System.currentTimeMillis()
		while(true)
		{
			val path = paths[pathI]

			val levels = Level.load(path)
			for (i in 0 until levels.size)
			{
				val level = levels[i]

				val theme = Theme.load(themes.random())
				val seed = Random.random.nextLong()

				val character = Character.load(characters.random().replace("Characters/", ""))
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

				try
				{
					solve(level.grid)
				}
				catch (ex: Exception)
				{
					val firstExceptionLine = ex.toString().split('\n').first()
 					val exceptionFolder = firstExceptionLine.toCharArray().filter { it.isLetter() }.joinToString("")
					val filename = path.filename(false)

					val file = Gdx.files.local("$exceptionFolder/${filename}_${i}_$crashCount")
					file.parent().mkdirs()
					file.writeString(level.grid.replay.compressToString(), false)

					crashCount++
				}
			}

			pathI++
			if (pathI == paths.size)
			{
				pathI = 0
			}

			val printInterval = 20
			if (pathI.rem(printInterval) == 0)
			{
				val currentTime = System.currentTimeMillis()
				val diffSeconds = (currentTime - lastTime) / 1000f
				val levelsASecond = printInterval / diffSeconds

				println("Done: $totalI, crashed: $crashCount, Levels/Second: $levelsASecond")

				lastTime = System.currentTimeMillis()
			}

			totalI++
		}
	}

	fun checkSolvability()
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Solveability Check      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		Global.resolveInstantly = true
		Global.godMode = true

		val themes = XmlData.enumeratePaths("", "Theme").toList()
		val gridScreen = GridScreen()

		val paths = XmlData.enumeratePaths("", "Level").toList()
		var pathI = 0
		for (path in paths)
		{
			pathI++
			println("")
			println("-----------------------------------------------------------------------------")
			println("    $pathI / ${paths.size}")
			println("")

			val levels = Level.load(path)

			for (i in 0 until levels.size)
			{
				val theme = Theme.load(themes.random())
				val seed = Random.random.nextLong()
				fun createLevel(level: Level)
				{
					val character = Character.load("Peasant")
					val player = Player(character, PlayerDeck())
					player.statistics[Statistic.MATCHDAMAGE] = 4f
					player.statistics[Statistic.PIERCE] = 1f
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
					solve(levels[i].grid)
					println("Level solved.")
				}
				catch (ex: Exception)
				{
					if (ex.message?.contains("Level completed in under 5 moves") == true)
					{

					}
					else
					{
						println("Solving level '$path' variant '$i' crashed!")

						val file = Gdx.files.local("crashedLevelReplay")
						file.writeString(levels[i].grid.replay.compressToString(), false)

						throw ex
					}
				}
			}
		}

		println("")
		println("All levels solvable")

		Global.godMode = false
		Global.resolveInstantly = false
	}

	class LevelDifficulty(val level: String, val variant: Int, val difficultyRating: Float, val successRates: FloatArray)
	fun determineDifficulty()
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Difficulty Checker      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val powerPerLevel = FastEnumMap<Statistic, Float>(Statistic::class.java)
		powerPerLevel[Statistic.MATCHDAMAGE] = 0.3f
		powerPerLevel[Statistic.ABILITYDAMAGE] = 0.5f
		powerPerLevel[Statistic.POWERGAIN] = 1f
		powerPerLevel[Statistic.HEALTH] = 2f
		powerPerLevel[Statistic.PIERCE] = 0.1f
		powerPerLevel[Statistic.AEGIS] = 0.2f
		powerPerLevel[Statistic.HASTE] = 0.02f
		powerPerLevel[Statistic.REGENERATION] = 0.02f

		val attemptsPerLevel = 10
		val powerLevelsToTest = 10

		Global.resolveInstantly = true
		disableOutput = true

		val levelDifficulties = Array<LevelDifficulty>()

		val themes = XmlData.enumeratePaths("", "Theme").toList()
		val gridScreen = GridScreen()

		val paths = XmlData.enumeratePaths("", "Level").toList()
		var pathI = 0
		for (path in paths)
		{
			pathI++
			println("")
			println("-----------------------------------------------------------------------------")
			println("    $path    ")
			println("    $pathI / ${paths.size}")
			println("")

			val levels = Level.load(path)

			for (variant in 0 until levels.size)
			{
				val theme = Theme.load(themes.random())
				val seed = Random.random.nextLong()
				fun createLevel(level: Level, powerLevel: Int)
				{
					val character = Character.load("Peasant")
					val player = Player(character, PlayerDeck())

					for (stat in Statistic.Values)
					{
						val statVal = powerPerLevel[stat] ?: continue
						player.statistics[stat] = (player.statistics[stat] ?: 0f) + statVal * powerLevel
					}

					Global.player = player
					Global.deck = GlobalDeck()
					Global.deck.chosenCharacter = character
					Global.deck.characters.add(character)

					level.create(theme, player, {}, {}, seed, variant)

					for (cond in level.victoryConditions)
					{
						cond.createTable(level.grid)
					}
					for (cond in level.defeatConditions)
					{
						cond.createTable(level.grid)
					}
				}

				val successRate = FloatArray(powerLevelsToTest)
				for (powerLevel in 0 until powerLevelsToTest)
				{
					val attemptSuccess = BooleanArray(attemptsPerLevel)
					for (attempt in 0 until attemptsPerLevel)
					{
						val levels = Level.load(path)

						createLevel(levels[variant], powerLevel)

						var success = false
						try
						{
							success = solve(levels[variant].grid)
						}
						catch (ex: Exception)
						{
							if (ex.message?.contains("Level completed in under 5 moves") == true)
							{
								success = true
							}
							else
							{
								val file = Gdx.files.local("crashedLevelReplay")
								file.writeString(levels[variant].grid.replay.compressToString(), false)

								throw ex
							}
						}

						attemptSuccess[attempt] = success
					}

					var successTotal = 0f
					for (attempt in 0 until attemptsPerLevel)
					{
						if (attemptSuccess[attempt])
						{
							successTotal += 1f
						}
					}
					successTotal /= attemptsPerLevel.toFloat()

					successRate[powerLevel] = successTotal
					println("Solving level '$path' variant '$variant' with power level '$powerLevel'. SuccessRate: '${successRate[powerLevel]}'")
				}

				var difficultyRating = 0f
				for (i in 0 until powerLevelsToTest)
				{
					difficultyRating += successRate[i]
				}
				difficultyRating = powerLevelsToTest - difficultyRating

				levelDifficulties.add(LevelDifficulty(path, variant, difficultyRating, successRate))

				println("Completed: $path, Difficulty: $difficultyRating")
			}
		}

		var output = "<LevelDifficulties>\n"
		for (level in levelDifficulties.sortedBy { it.difficultyRating })
		{
			output += "\t<Level name=\"${level.level}\" variant=\"${level.variant}\" rating=\"${level.difficultyRating}\">\n"

			for (i in 0 until powerLevelsToTest)
			{
				output += "\t\t<Difficulty$i>${level.successRates[i]}</Difficulty$i>\n"
			}

			output += "\t</Level>\n"
		}
		output += "</LevelDifficulties>"
		File("LevelDifficulties.xml").writeText(output)

		println("")
		println("All levels tested")

		Global.resolveInstantly = false
	}

	fun attemptAllLevels(testResolve: Boolean)
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

		val themes = XmlData.enumeratePaths("", "Theme").toList()
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
				val theme = Theme.load(themes.random())
				val seed = Random.random.nextLong()
				fun createLevel(level: Level)
				{
					val character = Character.load("Peasant")
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

					val file = Gdx.files.local("crashedLevelReplay")
					file.writeString(levels[i].grid.replay.compressToString(), false)

					throw ex
				}

				if (testResolve)
				{
					val levelGrid = levels[i].grid.grid.toString()

					createLevel(resolveLevels[i])

					try
					{
						println("")
						println("Resolving level '$path' variant '$i'")
						val victory = resolve(resolveLevels[i].grid, levels[i].grid.replay.moves)
						println("Level solved. Victory=$victory")

						if (levelGrid != resolveLevels[i].grid.grid.toString())
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
		}

		println("")
		println("Successful: $success / $total")

		Global.resolveInstantly = false
	}

	fun solve(grid: Grid): Boolean
	{
		return solveLevel(grid, fun (moveCount: Int) {
			makeMove(grid, moveCount > 200)
		})
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

			if (moveCount > 10 && moveCount.rem(20) == 0 && !disableOutput)
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
		if (!disableOutput) println("MoveCount: $moveCount")

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
				val hasSinking = grid.replay.moves.any { it.gridActionLog.any { it.contains("Sinking") } }
				val damageCount = grid.replay.moves.sumBy { it.gridActionLog.filter { it.contains("damage") }.count() }

				if (hasDetonations || hasSinking || damageCount > 3)
				{
					if (!disableOutput) System.err.println("Level took less than 5 turns but its probably alright")
				}
				else
				{
					throw RuntimeException("Level completed in under 5 moves. This seems suspicious!")
				}
			}
		}

		if (grid.level.isDefeat && !disableOutput)
		{
			println("Defeat reason: " + grid.level.defeatConditions.first{ it.isCompleted() }.javaClass.typeName.split("CompletionCondition").last())
		}

		return grid.level.isVictory
	}

	val updateRate = 1f / 30f // 30fps
	fun completeTurn(grid: Grid)
	{
		var updateCount = 0
		var completeCount = 0
		while (completeCount < 3)
		{
			grid.update(updateRate)
			Future.update(updateRate)

			for (tile in grid.grid)
			{
				tile.effects.clear()

				tile.contents?.renderable()?.renderable?.animation = null

				tile.contents?.markedForDeletion()?.deletionEffectDelay = 0f
			}

			Mote.clear()

			for (label in grid.match.messageList)
			{
				label.remove()
			}

			updateCount++

			if (updateCount > 10 && updateCount.rem(100) == 0 && !disableOutput)
			{
				println("UpdateCount: $updateCount")
			}

			if (updateCount > 1000)
			{
				throw RuntimeException("Turn got stuck in infinite update loop!")
			}

			if (grid.inTurn || grid.isUpdating || grid.delayedActions.size > 0 || grid.hasAnim)
			{
				completeCount = 0
			}
			else
			{
				completeCount++
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
			if (grid.isUpdating || grid.inTurn || grid.delayedActions.size > 0)
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

		if (print && !disableOutput)
		{
			println("Making move $move")
		}

		makeMove(grid, historyMove)
	}

	fun makeMove(grid: Grid, historyMove: HistoryMove)
	{
		if (grid.inTurn || grid.isUpdating || grid.delayedActions.size > 0)
		{
			throw Exception("Grid not completed!")
		}

		val levelState = grid.grid.toString()
		if (levelState != historyMove.levelSnapshot)
		{
			throw RuntimeException("History move doesnt match grid state!\nLevel:\n$levelState\n\nHistory:\n${historyMove.levelSnapshot}")
		}

		if (historyMove.refill)
		{
			grid.refill()

			if (grid.isUpdating)
			{
				throw Exception("Grid not completed!")
			}

			return
		}
		else if (historyMove.swapStart != null)
		{
			grid.dragStart = historyMove.swapStart!!
			grid.dragEnd(historyMove.swapEnd!!)

			if (grid.isUpdating || grid.delayedActions.size > 0)
			{
				throw Exception("Grid not completed!")
			}

			try
			{
				grid.update(0f)

				if (grid.isUpdating || grid.delayedActions.size > 0)
				{
					throw RuntimeException("Grid started updating!")
				}
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
