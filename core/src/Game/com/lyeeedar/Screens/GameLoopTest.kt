package com.lyeeedar.Screens

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.lyeeedar.Game.Global
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Statics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class GameLoopTest(val completionCallback: ()->Unit)
{
	val delay = 1000L

	fun run()
	{
		Statics.test = true

		GlobalScope.launch {
			while (Statics.game.currentScreen == null)
			{
				delay(1000)
			}
			delay(delay)

			invokeOnMainThread {
				Global.newGame ()
			}

			testLanguageSelection()
			testCompleteQuest()
			testEditDeck()
			testNewQuest()
			testGrid()
			testCompleteCard()

			println("###################################################################")
			println("Test completed successfully")
			println("###################################################################")

			delay(2000)
			completionCallback()
		}
	}

	private suspend fun testLanguageSelection()
	{
		println("---------------------------------------------------------------")
		println("Testing Language Selection")
		println("")

		waitUntilActorVisible("Language_en")
		getActor("Language_de")!!.click()

		waitUntilActorVisible("languageWarning")

		delay(delay)
		getActor("Language_en")!!.click()

		delay(delay)
		getActor("Confirm")!!.click()

		delay(delay)
		waitUntilActorVisible("Tutorial")
		clickThroughTutorial()

		println("")
		println("Language Selection Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun testCompleteQuest()
	{
		println("---------------------------------------------------------------")
		println("Testing Completing Quest")
		println("")

		waitUntilVisibleAndClick("Card0")
		waitUntilVisibleAndClick("Choose")

		clickThroughCardContent()
		waitUntilVisibleAndClick("Choice1")

		clickThroughCardContent()
		waitUntilVisibleAndClick("Choice1")

		waitUntilVisibleAndClick("QuestComplete")
		takeAllRewards()

		waitUntilActorVisible("Tutorial")
		clickThroughTutorial()

		println("")
		println("Completing Quest Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun testEditDeck()
	{
		println("---------------------------------------------------------------")
		println("Testing Edit Deck")
		println("")

		waitUntilVisibleAndClick("EditDeck")

		waitUntilVisibleAndClick("Character")
		waitUntilVisibleAndClick("Return")

		waitUntilVisibleAndClick("Encounters")
		waitUntilVisibleAndClick("Return")

		waitUntilVisibleAndClick("Equipment")
		waitUntilVisibleAndClick("Return")

		waitUntilVisibleAndClick("Return")

		println("")
		println("Edit Deck Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun testNewQuest()
	{
		println("---------------------------------------------------------------")
		println("Testing New Quest")
		println("")

		waitUntilVisibleAndClick("Levelling Up")
		delay(delay)

		waitUntilVisibleAndClick("Embark")
		delay(delay)

		waitUntilVisibleAndClick("Card0")
		delay(delay)

		waitUntilVisibleAndClick("Choose")
		delay(delay)

		clickThroughCardContent(true)

		waitUntilActorVisible("Tutorial")
		clickThroughTutorial()

		println("")
		println("New Quest Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun testGrid()
	{
		println("---------------------------------------------------------------")
		println("Testing Grid")
		println("")

		waitUntilActorVisible("GridWidget")

		while (true)
		{
			val gridWidget = getActor("GridWidget") as? GridWidget ?: break
			val grid = gridWidget.grid

			while (grid.inTurn || grid.isUpdating)
			{
				delay(500)
			}

			clickThroughTutorial()

			invokeOnMainThread {
				val bestMove = grid.cleanup.findBestMove(grid)
				if (bestMove != null)
				{
					grid.select(bestMove.swapStart)
					grid.dragEnd(bestMove.swapEnd)
				}
			}

			delay(500)
		}

		println("")
		println("Grid Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun testCompleteCard()
	{
		println("---------------------------------------------------------------")
		println("Testing Complete Card")
		println("")

		clickThroughCardContent()
		delay(delay)
		takeAllRewards()

		println("")
		println("Complete Card Succeeded")
		println("---------------------------------------------------------------")
	}

	private suspend fun clickThroughTutorial()
	{
		while (true)
		{
			delay(delay)

			val tutorial = getActor("Tutorial")

			if (tutorial == null)
			{
				break
			}
			else
			{
				tutorial.click()
			}
		}
	}

	private suspend fun clickThroughCardContent(skipChoice: Boolean = false)
	{
		waitUntilActorVisible("ContentTable")

		while (true)
		{
			delay(delay)

			val actor = getActor("ContentTable")

			if (actor == null)
			{
				break
			}
			else
			{
				val buttonTable = (getActor("ButtonTable") as? Table) ?: break
				if (buttonTable.children.size > 0)
				{
					if (skipChoice)
					{
						waitUntilVisibleAndClick("Choice0")
						delay(delay)
					}
					else
					{
						break
					}
				}

				actor.click()
			}
		}
	}

	private suspend fun takeAllRewards()
	{
		waitUntilActorVisible("Reward0")

		var i = 0
		while (true)
		{
			delay(delay)
			val actor = getActor("Reward$i") ?: break
			actor.click()

			i++
		}
	}

	private suspend fun waitUntilVisibleAndClick(name: String)
	{
		waitUntilActorVisible(name)
		delay(delay)
		getActor(name)!!.click()
	}

	private suspend fun waitUntilActorVisible(name: String)
	{
		val start = System.currentTimeMillis()
		while (true)
		{
			if (getActor(name) != null)
			{
				break
			}

			val current = System.currentTimeMillis()
			val diff = current - start

			if (diff > 20000) // 20 seconds
			{
				invokeOnMainThread {
					throw RuntimeException("Widget $name never appeared!")
				}
				throw RuntimeException("Widget $name never appeared!")
			}
		}
	}

	private suspend fun getActor(name: String): Actor?
	{
		return invokeOnMainThreadAndReturn {
			getAllActors().firstOrNull { it.name?.toLowerCase(Locale.ENGLISH) == name.toLowerCase(Locale.ENGLISH) }
		}
	}

	private fun getAllActors(): Sequence<Actor>
	{
		return sequence {
			for (actor in Statics.stage.actors)
			{
				if (actor == null) continue

				for (actor in getAllActors(actor))
				{
					yield(actor)
				}
			}
		}
	}

	private fun getAllActors(actor: Actor): Sequence<Actor>
	{
		return sequence {
			yield(actor)

			if (actor is WidgetGroup)
			{
				for (child in actor.children)
				{
					if (child == null) continue

					for (actor in getAllActors(child))
					{
						yield(actor)
					}
				}
			}
		}
	}
}

suspend fun Actor.click()
{
	invokeOnMainThread {
		val stageCoords = this.localToStageCoordinates(Vector2(this.width / 2f, this.height / 2f))

		val eventDown = InputEvent()
		eventDown.stageX = stageCoords.x
		eventDown.stageY = stageCoords.y
		eventDown.type = InputEvent.Type.touchDown
		this.fire(eventDown)

		val eventUp = InputEvent()
		eventUp.stageX = stageCoords.x
		eventUp.stageY = stageCoords.y
		eventUp.type = InputEvent.Type.touchUp
		this.fire(eventUp)
	}
}

suspend fun invokeOnMainThread(func: ()->Unit)
{
	val blocker = CompletableDeferred<Int>()

	Future.call(
		{
			func()
			blocker.complete(0)
		}, 0f)

	blocker.await()
}

suspend fun <T> invokeOnMainThreadAndReturn(func: () -> T): T
{
	val blocker = CompletableDeferred<T>()

	Future.call(
		{
			blocker.complete(func())
		}, 0f)

	return blocker.await()
}