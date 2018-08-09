package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Card.Card
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Global
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager

class DeckScreen : AbstractScreen()
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

		createMainScreen()
	}

	fun createMainScreen()
	{
		mainTable.clear()

		mainTable.add(Label("Character", Global.skin, "title")).expandX().center()
		mainTable.row()
		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.8f

		val charCard = Global.player.baseCharacter.getCard()
		charCard.addPick("", {
			createCharacterScreen()
		})
		charCard.canZoom = false
		charCard.canPickFaceDown = true

		charCard.height = cardHeight
		charCard.width = cardWidth
		mainTable.add(charCard).growX().center().height(cardHeight)
		mainTable.row()

		mainTable.add(Label("Encounters", Global.skin, "title")).expandX().center()
		mainTable.row()
		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		val deckCard = CardWidget(Table(), Table(), AssetManager.loadTextureRegion("GUI/CardCardback")!!, null)
		deckCard.addPick("", {
			createEncounterScreen()
		})
		deckCard.canZoom = false
		deckCard.canPickFaceDown = true
		deckCard.height = cardHeight
		mainTable.add(deckCard).growX().center().height(cardHeight)
		mainTable.row()

		mainTable.add(Label("Equipment", Global.skin, "title")).expandX().center()
		mainTable.row()
		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		val equipCard = CardWidget(Table(), Table(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, null)
		equipCard.addPick("", {
			createEquipmentScreen()
		})
		equipCard.canZoom = false
		equipCard.canPickFaceDown = true
		equipCard.height = cardHeight
		mainTable.add(equipCard).growX().center().height(cardHeight)
		mainTable.row()

		val returnButton = TextButton("Return", Global.skin)
		returnButton.addClickListener {
			val screen = Global.game.getTypedScreen<QuestSelectionScreen>()!!
			screen.swapTo()
		}
		mainTable.add(returnButton).expandX().right().pad(10f).expandY().bottom()
		mainTable.row()
	}

	fun createCharacterScreen()
	{
		mainTable.clear()

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.8f

		val title = Label("Character Selection", Global.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		val bodyTable = Table()
		mainTable.add(bodyTable).grow()
		mainTable.row()

		val leftCard = Global.deck.chosenCharacter!!.getCard()
		leftCard.setSize(cardWidth, cardHeight)
		leftCard.setFacing(true, false)
		bodyTable.add(leftCard).grow().center().height(cardHeight).pad(10f).uniform()

		bodyTable.add(Seperator(Global.skin, true)).growY()

		val scrollTable = Table()
		val scrollPane = ScrollPane(scrollTable, Global.skin)
		scrollPane.setForceScroll(false, true)
		scrollPane.setFadeScrollBars(false)
		scrollPane.setScrollingDisabled(true, false)
		bodyTable.add(scrollPane).grow().uniform()

		for (char in Global.deck.characters)
		{
			if (char != Global.deck.chosenCharacter)
			{
				val card = char.getCard()
				card.setSize(cardWidth, cardHeight)
				card.addPick("Select", {
					Global.deck.chosenCharacter = char
					createCharacterScreen()
				})
				card.setFacing(true, false)
				scrollTable.add(card).height(cardHeight).expandX().center().pad(5f)
				scrollTable.row()
			}
		}

		val returnButton = TextButton("Return", Global.skin)
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	fun createEncounterScreen()
	{
		mainTable.clear()

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.3f

		val title = Label("Encounter Selection", Global.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		val bodyTable = Table()
		mainTable.add(bodyTable).grow()
		mainTable.row()

		// Left stuff
		val leftScrollTable = Table()
		val leftScrollPane = ScrollPane(leftScrollTable, Global.skin)
		leftScrollPane.setForceScroll(false, true)
		leftScrollPane.setFadeScrollBars(false)
		leftScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(leftScrollPane).grow().uniform()

		val used = ObjectSet<Card>()
		for (enc in Global.deck.playerDeck.encounters)
		{
			val card = enc.current.getCard()
			card.setSize(cardWidth, cardHeight)
			card.addPick("Remove", {
				Global.deck.playerDeck.encounters.removeValue(enc, true)
				createEncounterScreen()
			})
			card.setFacing(true, false)
			leftScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
			leftScrollTable.row()

			used.add(enc)
		}

		bodyTable.add(Seperator(Global.skin, true)).growY()

		// Right stuff
		val rightScrollTable = Table()
		val rightScrollPane = ScrollPane(rightScrollTable, Global.skin)
		rightScrollPane.setForceScroll(false, true)
		rightScrollPane.setFadeScrollBars(false)
		rightScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(rightScrollPane).grow().uniform()

		for (enc in Global.deck.encounters)
		{
			if (!used.contains(enc))
			{
				val card = enc.current.getCard()
				card.setSize(cardWidth, cardHeight)
				card.addPick("Add", {
					Global.deck.playerDeck.encounters.add(enc)
					createEncounterScreen()
				})
				card.setFacing(true, false)
				rightScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				rightScrollTable.row()
			}
		}

		val returnButton = TextButton("Return", Global.skin)
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	fun createEquipmentScreen()
	{
		mainTable.clear()

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.3f

		val title = Label("Equipment Selection", Global.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		val bodyTable = Table()

		mainTable.add(bodyTable).grow()
		mainTable.row()

		// Left stuff
		val leftScrollTable = Table()
		val leftScrollPane = ScrollPane(leftScrollTable, Global.skin)
		leftScrollPane.setForceScroll(false, true)
		leftScrollPane.setFadeScrollBars(false)
		leftScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(leftScrollPane).grow().uniform()

		val used = ObjectSet<Equipment>()
		for (equip in Global.deck.playerDeck.equipment)
		{
			val card = equip.getCard(null, false)
			card.setSize(cardWidth, cardHeight)
			card.addPick("Remove", {
				Global.deck.playerDeck.equipment.removeValue(equip, true)
				createEquipmentScreen()
			})
			card.setFacing(true, false)
			leftScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
			leftScrollTable.row()

			used.add(equip)
		}

		bodyTable.add(Seperator(Global.skin, true)).growY()

		// Right stuff
		val rightScrollTable = Table()
		val rightScrollPane = ScrollPane(rightScrollTable, Global.skin)
		rightScrollPane.setForceScroll(false, true)
		rightScrollPane.setFadeScrollBars(false)
		rightScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(rightScrollPane).grow().uniform()

		for (equip in Global.deck.equipment)
		{
			if (!used.contains(equip))
			{
				val card = equip.getCard(null, false)
				card.setSize(cardWidth, cardHeight)
				card.addPick("Add", {
					Global.deck.playerDeck.equipment.add(equip)
					createEquipmentScreen()
				})
				card.setFacing(true, false)
				rightScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				rightScrollTable.row()
			}
		}

		val returnButton = TextButton("Return", Global.skin)
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	override fun doRender(delta: Float)
	{

	}
}