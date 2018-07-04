package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.lyeeedar.Global
import com.lyeeedar.UI.addClickListener

class QuestSelectionScreen : AbstractScreen()
{
	override fun create()
	{

	}

	fun setup()
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		mainTable.clear()

		val scrollTable = Table()
		val scrollPane = ScrollPane(scrollTable, Global.skin)
		scrollPane.setFadeScrollBars(false)
		scrollPane.setScrollingDisabled(false, true)
		scrollPane.setForceScroll(true, false)

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.8f

		for (quest in Global.deck.quests)
		{
			val card = quest.getCard()
			card.addPick("Embark", {
				val screen = Global.game.getTypedScreen<QuestScreen>()!!
				screen.setup(quest)
				screen.swapTo()
			})
			card.setSize(cardWidth, cardHeight)

			scrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center()
			scrollTable.row()
		}

		mainTable.add(scrollPane).growX().expandY().bottom()
		mainTable.row()

		val editButton = TextButton("Edit Deck", Global.skin)
		editButton.addClickListener {
			val screen = Global.game.getTypedScreen<DeckScreen>()!!
			screen.setup()
			screen.swapTo()
		}
		mainTable.add(editButton).expand().right().bottom().pad(10f)
		mainTable.row()
	}

	override fun doRender(delta: Float)
	{

	}
}