package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.Direction
import com.lyeeedar.Game.AbstractReward
import com.lyeeedar.Game.Quest
import com.lyeeedar.Game.QuestNode
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.AssetManager
import ktx.actors.then
import ktx.collections.toGdxArray

class QuestScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	override fun create()
	{
		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)

		debugConsole.register("LoadCard", "LoadCard cardName", fun (args, console): Boolean {
			if (args.size != 1)
			{
				console.error("Invalid number of arguments!")
				return false
			}

			val card = Global.deck.encounters.backingArray.firstOrNull { it.current.name.toLowerCase() == args[0].toLowerCase() }
			if (card == null)
			{
				console.error("Invalid card name!")
				return false
			}

			val cardScreen = CardScreen.instance
			cardScreen.setup(card, currentQuest)
			Global.game.switchScreen(MainGame.ScreenEnum.CARD)

			Global.player.deck.encounters.removeValue(card, true)
			currentQuest.questCards.removeValue(card, true)

			return true
		})
	}

	lateinit var currentQuest: Quest
	fun setup(quest: Quest)
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		currentQuest = quest

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(quest.theme.backgroundTile))).tint(Color.DARK_GRAY)

		updateQuest()
	}

	var chosenQuestCard: CardWidget? = null
	val cardWidgets = Array<CardWidget>()
	fun updateQuest()
	{
		for (widget in cardWidgets)
		{
			widget.remove()
		}
		cardWidgets.clear()

		currentQuest.run()
		if (currentQuest.current == null)
		{
			completeQuest()
			return
		}

		val cards = (currentQuest.current as QuestNode).getCards()

		// create widgets
		for (card in cards)
		{
			val widget = card.current.getCard()
			widget.data = card

			widget.addPick("Choose", {
				chosenQuestCard = it

				for (w in cardWidgets)
				{
					w.clickable = false
				}
			})

			cardWidgets.add(widget)
			stage.addActor(widget)
		}

		CardWidget.layoutCards(cardWidgets, Direction.CENTER)
	}

	fun completeQuest()
	{
		Global.stage.addActor(greyOutTable)
		updateRewards()
	}

	var grouped: Array<Array<AbstractReward>> = Array()
	var currentGroup = Array<CardWidget>()
	val greyOutTable = Table()
	fun updateRewards()
	{
		if (currentGroup.size == 0)
		{
			if (grouped.size == 0)
			{
				val bronze = currentQuest.bronzeRewards.filter { it.isValid() }.toGdxArray()
				val silver = currentQuest.silverRewards.filter { it.isValid() }.toGdxArray()
				val gold = currentQuest.goldRewards.filter { it.isValid() }.toGdxArray()

				if (currentQuest.state.ordinal >= Quest.QuestState.BRONZE.ordinal && !currentQuest.gotBronze && bronze.size > 0)
				{
					grouped = bronze.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
				}
				else if (currentQuest.state.ordinal >= Quest.QuestState.SILVER.ordinal && !currentQuest.gotSilver && silver.size > 0)
				{
					grouped = silver.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
				}
				else if (currentQuest.state.ordinal >= Quest.QuestState.GOLD.ordinal && !currentQuest.gotGold && gold.size > 0)
				{
					grouped = gold.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
				}
				else
				{
					greyOutTable.remove()
					QuestSelectionScreen.instance.setup()
					QuestSelectionScreen.instance.swapTo()
				}
			}

			if (grouped.size > 0)
			{
				val chosen = grouped.removeIndex(0)
				currentGroup = chosen.flatMap { it.reward() }.filter { it != null }.map { it!! }.toGdxArray()

				for (card in currentGroup)
				{
					for (pick in card.pickFuns)
					{
						val oldFun = pick.pickFun
						pick.pickFun = {
							oldFun(it)
							currentGroup.removeValue(card, true)
							if (currentGroup.size == 0)
							{
								updateRewards()
							}

							card.remove()
						}
					}

					Global.stage.addActor(card)
				}

				if (currentGroup.size > 0)
				{
					CardWidget.layoutCards(currentGroup, Direction.CENTER)
				}
				else
				{
					updateRewards()
				}
			}
		}
	}

	override fun doRender(delta: Float)
	{
		if (chosenQuestCard != null && chosenQuestCard!!.actions.size == 0)
		{
			for (widget in cardWidgets)
			{
				if (widget != chosenQuestCard)
				{
					val sequence = fadeOut(0.3f) then removeActor()
					widget.addAction(sequence)
				}
			}

			val sequence = delay(0.5f) then lambda {
				val card = chosenQuestCard!!.data as Card

				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Global.game.switchScreen(MainGame.ScreenEnum.CARD)

				Global.player.deck.encounters.removeValue(card, true)
				currentQuest.questCards.removeValue(card, true)

				chosenQuestCard = null
			} then removeActor()
			chosenQuestCard!!.addAction(sequence)

		}
	}

	companion object
	{
		lateinit var instance: QuestScreen
	}
}