package com.lyeeedar.Game

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.*
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Screens.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Save
{
	companion object
	{
		val kryo: Kryo by lazy { initKryo() }
		fun initKryo(): Kryo
		{
			val kryo = Kryo()
			kryo.isRegistrationRequired = false

			kryo.registerGdxSerialisers()
			kryo.registerLyeeedarSerialisers()

			return kryo
		}

		fun save()
		{
			if (doingLoad) return

			val outputFile = Gdx.files.local("save.dat")

			val output: Output
			try
			{
				output = Output(GZIPOutputStream(outputFile.write(false)))
			}
			catch (e: Exception)
			{
				e.printStackTrace()
				return
			}

			// Obtain all data

			// Save all data
			Global.deck.save(output)
			Global.player.save(kryo, output)
			Global.globalflags.save(kryo, output)
			Global.levelflags.save(kryo, output)
			Global.settings.save(kryo, output)

			val currentScreen = Global.game.currentScreenEnum
			output.writeInt(currentScreen.ordinal)

			if (currentScreen == MainGame.ScreenEnum.QUESTSELECTION)
			{

			}
			else if (currentScreen == MainGame.ScreenEnum.DECK)
			{

			}
			else
			{
				val questScreen = Global.game.getTypedScreen<QuestScreen>()!!
				output.writeInt(questScreen.currentQuest.path.hashCode())

				if (currentScreen == MainGame.ScreenEnum.QUEST)
				{

				}
				else
				{
					val cardScreen = Global.game.getTypedScreen<CardScreen>()!!
					output.writeInt(cardScreen.currentCard.path.hashCode())

					cardScreen.currentContent.save(kryo, output)
				}
			}

			output.close()
		}

		var doingLoad = false
		fun load(): Boolean
		{
			doingLoad = true
			var input: Input? = null

			try
			{
				val saveFileHandle = Gdx.files.local("save.dat")
				if (!saveFileHandle.exists())
				{
					doingLoad = false
					return false
				}

				input = Input(GZIPInputStream(saveFileHandle.read()))

				// Load all data
				val deck = GlobalDeck.load(input)
				val player = Player.load(kryo, input, deck)
				val globalFlags = GameStateFlags.load(kryo, input)
				val levelFlags = GameStateFlags.load(kryo, input)
				val settings = Settings.load(kryo, input)

				val currentScreen = MainGame.ScreenEnum.values()[input.readInt()]

				// If successful set data to active objects
				Global.deck = deck
				Global.player = player
				Global.globalflags = globalFlags
				Global.levelflags = levelFlags
				Global.settings = settings

				if (currentScreen == MainGame.ScreenEnum.QUESTSELECTION)
				{
					val screen = Global.game.getTypedScreen<QuestSelectionScreen>()!!
					screen.setup()
					screen.swapTo()
				}
				else if (currentScreen == MainGame.ScreenEnum.DECK)
				{
					val screen = Global.game.getTypedScreen<DeckScreen>()!!
					screen.setup()
					screen.swapTo()
				}
				else
				{
					val currentQuestHash = input.readInt()
					val currentQuest = Quest.load(XmlData.enumeratePaths("", "Quest").map { it.replace("Quests/", "").replace(".xml", "") }.first { it.hashCode() == currentQuestHash })

					val questScreen = Global.game.getTypedScreen<QuestScreen>()!!
					questScreen.setup(currentQuest)

					if (currentScreen == MainGame.ScreenEnum.QUEST)
					{
						questScreen.swapTo()
					}
					else
					{
						val currentCardHash = input.readInt()
						val currentCard = Card.Companion.load(XmlData.enumeratePaths ("", "Card").first{ it.replace(".xml", "").hashCode() == currentCardHash })

						val content = CardContent.load(kryo, input)

						val cardScreen = Global.game.getTypedScreen<CardScreen>()!!
						cardScreen.setup(currentCard, currentQuest, false, content)

						if (currentScreen == MainGame.ScreenEnum.CARD)
						{
							cardScreen.swapTo()
						}
						else
						{
							val gridScreen = Global.game.getTypedScreen<GridScreen>()!!

							gridScreen.swapTo()
						}
					}
				}
			}
			catch (e: Exception)
			{
				throw e
				doingLoad = false
				return false
				//e.printStackTrace()
			}
			finally
			{
				input?.close()
			}

			doingLoad = false
			return true
		}
	}
}