package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Game.Quest
import com.lyeeedar.Game.QuestNode
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.ScrollingTextLabel
import com.lyeeedar.Util.directory

class CardScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	var fullScreenColour: Table? = null

	lateinit var currentCard: Card
	lateinit var currentQuest: Quest
	lateinit var currentContent: CardContent

	lateinit var text: ScrollingTextLabel
	lateinit var buttonTable: Table

	var success = false

	fun setup(card: Card, quest: Quest)
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		currentCard = card
		currentContent = CardContent.load(currentCard.path.directory() + "/" + currentCard.current.content)
		currentQuest = quest

		mainTable.clear()
		mainTable.add(Label(currentCard.current.name, Global.skin)).pad(10f)
		mainTable.row()
		mainTable.add(text).grow().pad(5f)
		mainTable.row()
		mainTable.add(buttonTable).pad(5f)

		mainTable.debug()

		success = false
		currentContent.advance(this)
	}

	override fun create()
	{
		val skin = Global.skin

		text = ScrollingTextLabel("", skin)
		buttonTable = Table()
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
	{
		val completed = currentContent.advance(this)
		if (completed)
		{
			if (success)
			{
				val currentCardNode = currentCard.current
				currentCard.current = currentCardNode.nextNode?.node ?: currentCardNode
			}

			val currentQuestNode = currentQuest.current
			if (currentQuestNode.type == QuestNode.QuestNodeType.FIXED)
			{
				if (success)
				{
					currentQuest.current = currentQuestNode.successNode!!.node
				}
				else
				{
					currentQuest.current = currentQuestNode.failureNode!!.node
				}
			}
			else
			{
				currentQuest.current = currentQuestNode.nextNode!!.node
			}

			QuestScreen.instance.updateButtons()
			Global.game.switchScreen(MainGame.ScreenEnum.QUEST)
		}

		return super.touchDown(screenX, screenY, pointer, button)
	}

	override fun doRender(delta: Float)
	{

	}

	companion object
	{
		lateinit var instance: CardScreen
	}
}