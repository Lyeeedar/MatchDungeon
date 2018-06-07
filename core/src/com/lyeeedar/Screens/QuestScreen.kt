package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.Quest
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.AssetManager

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

	fun updateQuest()
	{
		mainTable.clear()
		val cards = currentQuest.current.getCards()

		for (card in cards)
		{
			val widget = CardWidget(card.current.name, card.current.description, AssetManager.loadSprite("white"), "Choose", {
				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Global.game.switchScreen(MainGame.ScreenEnum.CARD)

				Global.player.deck.encounters.removeValue(card, true)
				currentQuest.questCards.removeValue(card, true)
			})

			widget.setFacing(true, false)

			mainTable.add(widget).size(200f)
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