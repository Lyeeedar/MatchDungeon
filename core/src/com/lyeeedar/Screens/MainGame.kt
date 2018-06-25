package com.lyeeedar

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.lyeeedar.Screens.*
import com.lyeeedar.Util.Future
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.swing.JOptionPane

class MainGame : Game()
{
	val debugOverride = false

	enum class ScreenEnum
	{
		GRID,
		QUEST,
		CARD,
		DECK,
		QUESTSELECTION
	}

	private val screens = HashMap<ScreenEnum, AbstractScreen>()

	override fun create()
	{
		Global.applicationChanger.processResources()
		Global.setup()

		if (Global.android)
		{
//			val sw = StringWriter()
//			val handler = Thread.UncaughtExceptionHandler { myThread, e ->
//				val exceptionAsString = sw.toString()
//				JOptionPane.showMessageDialog(null, "A fatal error occurred:\n" + exceptionAsString, "An error occurred", JOptionPane.ERROR_MESSAGE)
//			}
//
//			Thread.currentThread().uncaughtExceptionHandler = handler
		}
		else if (Global.release)
		{
			val sw = StringWriter()
			val pw = PrintWriter(sw)

			val handler = Thread.UncaughtExceptionHandler { myThread, e ->
				e.printStackTrace(pw)
				val exceptionAsString = sw.toString()

				val file = Gdx.files.local("error.log")
				file.writeString(exceptionAsString, false)

				JOptionPane.showMessageDialog(null, "A fatal error occurred. Please send the error.log to me so that I can fix it.", "An error occurred", JOptionPane.ERROR_MESSAGE)

				e.printStackTrace()
			}

			Thread.currentThread().uncaughtExceptionHandler = handler
		}

		screens.put(ScreenEnum.GRID, GridScreen())
		screens.put(ScreenEnum.QUEST, QuestScreen())
		screens.put(ScreenEnum.CARD, CardScreen())
		screens.put(ScreenEnum.DECK, DeckScreen())
		screens.put(ScreenEnum.QUESTSELECTION, QuestSelectionScreen())

		Global.newGame()
	}

	fun switchScreen(screen: AbstractScreen)
	{
		this.setScreen(screen)
	}

	fun switchScreen(screen: ScreenEnum)
	{
		this.setScreen(screens[screen])
	}

	inline fun <reified T : AbstractScreen> getTypedScreen(): T?
	{
		for (screen in getAllScreens())
		{
			if (screen is T)
			{
				return screen
			}
		}

		return null
	}

	fun getAllScreens() = screens.values

	override fun setScreen(screen: Screen?)
	{
		val currentScreen = getScreen() as? AbstractScreen

		if (currentScreen != null)
		{
			currentScreen.fadeOutTransition(0.2f)

			Future.call(
					{
						super.setScreen(screen)
						val ascreen = screen as AbstractScreen
						ascreen.fadeInTransition(0.2f)
					}, 0.2f)
		}
		else
		{
			super.setScreen(screen)
		}
	}

	fun getScreen(screen: ScreenEnum): AbstractScreen = screens[screen]!!
}
