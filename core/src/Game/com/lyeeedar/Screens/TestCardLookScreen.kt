package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.Direction
import com.lyeeedar.Game.*
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics

class TestCardLookScreen : AbstractScreen()
{
	val cardTable = Table()
	val cards = Array<CardWidget>()

	override fun create()
	{
		val respawnButton = TextButton("Respawn cards", Statics.skin)
		respawnButton.addClickListener { spawnCards() }
		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.4f, 0.4f, 0.4f, 1f))
		mainTable.add(respawnButton)
		mainTable.row()
		mainTable.add(cardTable).grow()

		spawnCards()
	}

	fun spawnCards()
	{
		for (card in cards)
		{
			card.remove()
		}
		cards.clear()

		val quest = Quest.Companion.load("Dungeon/RatKing")
		cards.add(quest.getCard())

		val card = Card.Companion.load("Cards/Default/GuildQuest")
		cards.add(card.current.getCard())

		val equipment = Equipment.load("Equipment/Other/ShearScimitar")
		cards.add(equipment.getCard(null, true).addPick("Sell (500 gold)", {}).addPick("Buy", {}))

		val character = Character.load("Wizard")
		cards.add(character.getCard())

		val player = Player(character, PlayerDeck())
		Global.player = player
		Global.deck = GlobalDeck()
		cards.add(player.getCard())

		val moneyReward = MoneyReward()
		moneyReward.amountEqn = "589"
		cards.addAll(moneyReward.reward())

		val statsReward = StatisticsReward()
		statsReward.statsTable[Statistic.HEALTH] = 3.0f
		//cards.addAll(statsReward.reward())

		for (c in cards)
		{
			stage.addActor(c)
		}

		CardWidget.layoutCards(cards, Direction.CENTER, cardTable, flip = false)
	}

	override fun doRender(delta: Float)
	{

	}
}