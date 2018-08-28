package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.lyeeedar.Game.Quest
import com.lyeeedar.Global
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.UI.addClickListener

class QuestSelectionScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	override fun create()
	{

	}

	val scrollTable = Table()
	val editButton = TextButton("Edit Deck", Global.skin)

	fun setup()
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		mainTable.clear()
		scrollTable.clear()

		val scrollPane = ScrollPane(scrollTable, Global.skin)
		scrollPane.setFadeScrollBars(false)
		scrollPane.setScrollingDisabled(true, false)
		scrollPane.setForceScroll(false, true)

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.8f

		for (quest in Global.deck.quests)
		{
			val card = quest.getCard()
			card.addPick("Embark", {
				val screen = Global.game.getTypedScreen<QuestScreen>()!!
				quest.current = quest.root
				quest.state = Quest.QuestState.INPROGRESS
				Global.player = Global.deck.getPlayer()
				screen.setup(quest)
				screen.swapTo()
			})
			card.setSize(cardWidth, cardHeight)
			card.setFacing(true, false)

			scrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center()
			scrollTable.row()
		}

		mainTable.add(scrollPane).growX().expandY().bottom()
		mainTable.row()

		editButton.addClickListener {
			val screen = Global.game.getTypedScreen<DeckScreen>()!!
			screen.setup()
			screen.swapTo()
		}
		mainTable.add(editButton).expand().right().bottom().pad(10f)
		mainTable.row()
	}

	override fun show()
	{
		super.show()

		val tutorial = Tutorial("QuestSelection")
		tutorial.addDelay(1f)
		tutorial.addPopup("This is the quest selection screen.", Any())
		tutorial.addPopup("You can pick a quest from those you have unlocked here. Just tap the card you want, then tap 'embark'.", scrollTable)
		tutorial.addPopup("You can edit your deck of cards here. This will allow you to customise your quest experience.", editButton)
		tutorial.show()
	}

	override fun doRender(delta: Float)
	{

	}

	companion object
	{
		lateinit var instance: QuestSelectionScreen
	}
}