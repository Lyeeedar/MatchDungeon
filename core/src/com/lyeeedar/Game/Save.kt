package com.lyeeedar.Game

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.*
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Screens.DeckScreen
import com.lyeeedar.Screens.QuestScreen
import com.lyeeedar.Screens.QuestSelectionScreen
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
				}
			}

			output.close()
		}

		fun load(): Boolean
		{
			var input: Input? = null

			try
			{
				input = Input(GZIPInputStream(Gdx.files.local("save.dat").read()))

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
					val currentQuest = deck.quests.uniqueMap.get(currentQuestHash)

					val questScreen = Global.game.getTypedScreen<QuestScreen>()!!
					questScreen.setup(currentQuest)

					if (currentScreen == MainGame.ScreenEnum.QUEST)
					{
						questScreen.swapTo()
					}
					else
					{
						val currentCardHash = input.readInt()
						val currentCard = deck.encounters.uniqueMap.get(currentCardHash)

						val cardScreen = Global.game.getTypedScreen<CardScreen>()!!
						cardScreen.setup(currentCard, currentQuest, false)

						cardScreen.swapTo()
					}
				}
			}
			catch (e: Exception)
			{
				return false
				//e.printStackTrace()
			}
			finally
			{
				input?.close()
			}

			return true
		}
	}
}