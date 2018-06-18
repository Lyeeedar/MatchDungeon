package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.Direction
import com.lyeeedar.Game.Quest
import com.lyeeedar.Game.QuestNode
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.AssetManager
import ktx.actors.then

class QuestScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	override fun create()
	{

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
		currentQuest.run()
		if (currentQuest.current == null)
		{
			return
		}

		val cards = (currentQuest.current as QuestNode).getCards()

		// create widgets

		for (widget in cardWidgets)
		{
			widget.remove()
		}
		cardWidgets.clear()
		for (card in cards)
		{
			val widget = CardWidget(card.current.createTable(), card)
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