package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.Game.Quest
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
		val padding = 20f
		val cardWidth = (Global.resolution.x - 3f * padding) / 2f
		val cardHeight = (Global.resolution.y - 3f * padding) / 2f

		val cards = currentQuest.current.getCards()

		// create widgets

		for (widget in cardWidgets)
		{
			widget.remove()
		}
		cardWidgets.clear()
		for (card in cards)
		{
			val widget = CardWidget(card.current.name, card.current.description, AssetManager.loadSprite("white"), "Choose", card, {
				chosenQuestCard = it
			})

			widget.width = cardWidth
			widget.height = cardHeight
			cardWidgets.add(widget)
			stage.addActor(widget)
		}

		if (cardWidgets.size == 1)
		{
			// center
			cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, Global.resolution.y / 2f - cardHeight / 2f)
		}
		else if (cardWidgets.size == 2)
		{
			// vertical alignment
			cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding * 2f + cardHeight)
			cardWidgets[1].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding)
		}
		else if (cardWidgets.size == 3)
		{
			// triangle, single card at top, 2 below
			cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding * 2f + cardHeight)
			cardWidgets[1].setPosition(padding, padding)
			cardWidgets[2].setPosition(padding * 2f + cardWidth, padding)
		}
		else if (cardWidgets.size == 4)
		{
			// even grid
			cardWidgets[0].setPosition(padding, padding * 2f + cardHeight)
			cardWidgets[1].setPosition(padding, padding)
			cardWidgets[2].setPosition(padding * 2f + cardWidth, padding)
			cardWidgets[3].setPosition(padding * 2f + cardWidth, padding * 2f + cardHeight)
		}

		// do animation
		val centerX = Global.resolution.x / 2f - cardWidth / 2f
		val centerY = Global.resolution.y / 2f - cardHeight / 2f

		var delay = 0.2f
		for (widget in cardWidgets)
		{
			val x = widget.x
			val y = widget.y

			widget.setPosition(centerX, centerY)
			widget.clickable = false

			val delayVal = delay
			delay += 0.04f
			val sequence = delay(delayVal) then moveTo(x, y, 0.2f) then delay(0.1f) then lambda { widget.flip(true); widget.clickable = true }
			widget.addAction(sequence)
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