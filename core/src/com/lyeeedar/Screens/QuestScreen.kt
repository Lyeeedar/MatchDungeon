package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.lyeeedar.Game.Quest
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.addClickListener

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
		currentQuest = quest
	}

	fun updateButtons()
	{
		mainTable.clear()
		val cards = currentQuest.current.getCards()

		for (card in cards)
		{
			val button = TextButton(card.current.name, Global.skin)
			button.addClickListener {
				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Global.game.switchScreen(MainGame.ScreenEnum.CARD)
			}

			mainTable.add(button)
			mainTable.row()
		}
	}

	override fun doRender(delta: Float)
	{

	}

	companion object
	{
		lateinit var instance: QuestScreen
	}
}