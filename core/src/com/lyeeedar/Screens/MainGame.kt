package com.lyeeedar

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.lyeeedar.Game.Global
import com.lyeeedar.Game.Save
import com.lyeeedar.Screens.*
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Statics
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.swing.JOptionPane

class MainGame : Game()
{
	enum class ScreenEnum
	{
		GRID,
		QUEST,
		CARD,
		DECK,
		QUESTSELECTION,
		PARTICLEEDITOR,
		INVALID
	}

	private val screens = HashMap<ScreenEnum, AbstractScreen>()
	var currentScreen: AbstractScreen? = null
	val currentScreenEnum: ScreenEnum
		get()
		{
			for (se in ScreenEnum.values())
			{
				if (screens[se] == currentScreen)
				{
					return se
				}
			}
			return ScreenEnum.INVALID
		}

	override fun create()
	{
		Statics.applicationChanger.processResources()
		Statics.setup()

		if (Statics.android)
		{

		}
		else if (Statics.release)
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

		if (Statics.PARTICLE_EDITOR)
		{
			screens.put(ScreenEnum.PARTICLEEDITOR, ParticleEditorScreen())
			switchScreen(ScreenEnum.PARTICLEEDITOR)
		}
		else
		{
			screens.put(ScreenEnum.GRID, GridScreen())
			screens.put(ScreenEnum.QUEST, QuestScreen())
			screens.put(ScreenEnum.CARD, CardScreen())
			screens.put(ScreenEnum.DECK, DeckScreen())
			screens.put(ScreenEnum.QUESTSELECTION, QuestSelectionScreen())

			val success = Save.load()

			if (!success)
			{
				Global.newGame()
			}
		}

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
		if (currentScreen != null)
		{
			currentScreen!!.fadeOutTransition(0.2f)

			Future.call(
					{
						val ascreen = screen as AbstractScreen
						currentScreen = ascreen
						super.setScreen(screen)
						ascreen.fadeInTransition(0.2f)
					}, 0.2f)
		}
		else
		{
			currentScreen = screen as? AbstractScreen
			super.setScreen(screen)
		}
	}

	fun getScreen(screen: ScreenEnum): AbstractScreen = screens[screen]!!
}
