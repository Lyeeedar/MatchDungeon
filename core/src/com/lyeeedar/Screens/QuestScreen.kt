package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.Quest
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.addClickListener
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

		updateButtons()
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

				Global.player.deck.encounters.removeValue(card, true)
				currentQuest.questCards.removeValue(card, true)
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