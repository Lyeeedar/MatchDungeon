package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Global
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.*

class DeckScreen : AbstractScreen()
{
	override fun create()
	{

	}

	var didEdit = false
	fun setup()
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		didEdit = false

		createMainScreen()
	}

	val encounterDeckSize = 30
	val equipmentDeckSize = 20

	fun isDeckInvalid(): Boolean = containsTooManyEncounters() || containsTooFewEncounters() || containsTooManyEquipment() || containsTooFewEquipment()

	fun containsTooManyEncounters() = Global.deck.playerDeck.encounters.size > encounterDeckSize
	fun containsTooFewEncounters() = Global.deck.playerDeck.encounters.size < encounterDeckSize

	fun containsTooManyEquipment() = Global.deck.playerDeck.equipment.size > equipmentDeckSize
	fun containsTooFewEquipment() = Global.deck.playerDeck.equipment.size < equipmentDeckSize

	fun createMainScreen()
	{
		mainTable.clear()

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.5f, 0.5f, 0.5f, 1f))

		mainTable.add(Label(Localisation.getText("character", "UI"), Statics.skin, "title")).expandX().center().padTop(20f)
		mainTable.row()
		mainTable.add(Seperator(Statics.skin)).growX()
		mainTable.row()

		val cardHeight = (Statics.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Statics.resolution.x.toFloat() * 0.8f

		val charCard = Global.player.baseCharacter.getCard()
		charCard.name = "Character"
		charCard.addPick("") {
			createCharacterScreen()
		}
		charCard.canZoom = false
		charCard.canPickFaceDown = true

		charCard.height = cardHeight
		charCard.width = cardWidth

		val charStack = Stack()
		charStack.add(charCard)

		if (Global.deck.hasNewCharacters)
		{
			val newTable = Table()
			val newLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
			newTable.add(newLabel).expand().top().pad(3f)

			charStack.add(newTable)
		}

		val charTable = Table()
		charTable.add(charStack).grow().width(Value.percentWidth(0.5f, charTable))
		val charDetails = Table()
		charTable.add(charDetails).grow().width(Value.percentWidth(0.5f, charTable))

		mainTable.add(charTable).growX().center().height(cardHeight)

		mainTable.row()

		mainTable.add(Label(Localisation.getText("encounter", "UI"), Statics.skin, "title")).expandX().center()
		mainTable.row()
		mainTable.add(Seperator(Statics.skin)).growX()
		mainTable.row()

		val deckCard = CardWidget(Table(), Table(), AssetManager.loadTextureRegion("GUI/CardCardback")!!, null)
		deckCard.name = "Encounters"
		deckCard.addPick("") {
			createEncounterScreen()
		}
		deckCard.canZoom = false
		deckCard.canPickFaceDown = true
		deckCard.height = cardHeight

		val cardStack = Stack()
		cardStack.add(deckCard)

		if (Global.deck.hasNewEncounters)
		{
			val newTable = Table()
			val newLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
			newTable.add(newLabel).expand().top().pad(3f)

			cardStack.add(newTable)
		}

		val encountersTable = Table()
		encountersTable.add(cardStack).grow().width(Value.percentWidth(0.5f, encountersTable))
		val encountersDetails = Table()
		encountersTable.add(encountersDetails).grow().width(Value.percentWidth(0.5f, encountersTable))

		encountersDetails.add(Label(Global.deck.playerDeck.encounters.size.toString() + " / $encounterDeckSize", Statics.skin))
		encountersDetails.row()

		if (containsTooManyEncounters())
		{
			val label = Label(Localisation.getText("deckscreen.toomanyencounters", "UI"), Statics.skin)
			label.setWrap(true)
			label.color = Color.RED

			encountersDetails.add(label).growX()
			encountersDetails.row()
		}

		if (containsTooFewEncounters())
		{
			val label = Label(Localisation.getText("deckscreen.toofewencounters", "UI"), Statics.skin)
			label.setWrap(true)
			label.color = Color.RED

			encountersDetails.add(label).growX()
			encountersDetails.row()
		}

		mainTable.add(encountersTable).growX().center().height(cardHeight)
		mainTable.row()

		mainTable.add(Label(Localisation.getText("equipment", "UI"), Statics.skin, "title")).expandX().center()
		mainTable.row()
		mainTable.add(Seperator(Statics.skin)).growX()
		mainTable.row()

		val equipCard = CardWidget(Table(), Table(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, null)
		equipCard.name = "Equipment"
		equipCard.addPick("") {
			createEquipmentScreen()
		}
		equipCard.canZoom = false
		equipCard.canPickFaceDown = true
		equipCard.height = cardHeight

		val equipStack = Stack()
		equipStack.add(equipCard)

		if (Global.deck.hasNewEquipment)
		{
			val newTable = Table()
			val newLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
			newTable.add(newLabel).expand().top().pad(3f)

			equipStack.add(newTable)
		}

		val equipTable = Table()
		equipTable.add(equipStack).grow().width(Value.percentWidth(0.5f, equipTable))
		val equipDetails = Table()
		equipTable.add(equipDetails).grow().width(Value.percentWidth(0.5f, equipTable))

		equipDetails.add(Label(Global.deck.playerDeck.equipment.size.toString() + " / $equipmentDeckSize", Statics.skin))
		equipDetails.row()

		if (containsTooManyEquipment())
		{
			val label = Label(Localisation.getText("deckscreen.toomuchequipment", "UI"), Statics.skin)
			label.setWrap(true)
			label.color = Color.RED

			equipDetails.add(label).growX()
			equipDetails.row()
		}

		if (containsTooFewEquipment())
		{
			val label = Label(Localisation.getText("deckscreen.toofewequipment", "UI"), Statics.skin)
			label.setWrap(true)
			label.color = Color.RED

			equipDetails.add(label).growX()
			equipDetails.row()
		}

		mainTable.add(equipTable).growX().center().height(cardHeight)
		mainTable.row()

		if (isDeckInvalid())
		{
			val returnButton = TextButton(Localisation.getText("deckscreen.return", "UI"), Statics.skin)
			returnButton.name = "ReturnInvalid"
			returnButton.color = Color.DARK_GRAY
			mainTable.add(returnButton).expandX().right().pad(10f).expandY().bottom()
			mainTable.row()
		}
		else
		{
			val returnButton = TextButton(Localisation.getText("deckscreen.return", "UI"), Statics.skin)
			returnButton.name = "Return"
			returnButton.addClickListener {

				val screen = Statics.game.getTypedScreen<QuestSelectionScreen>()!!
				screen.setup()
				screen.swapTo()
			}
			mainTable.add(returnButton).expandX().right().pad(10f).expandY().bottom()
			mainTable.row()
		}
	}

	fun createCharacterScreen()
	{
		Global.deck.hasNewCharacters = false

		mainTable.clear()

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.5f, 0.5f, 0.5f, 1.0f))

		val cardHeight = (Statics.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Statics.resolution.x.toFloat() * 0.8f

		val title = Label(Localisation.getText("deckscreen.characterselection", "UI"), Statics.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		val bodyTable = Table()
		mainTable.add(bodyTable).grow()
		mainTable.row()

		val leftCard = Global.deck.chosenCharacter.getCard()
		leftCard.setSize(cardWidth, cardHeight)
		leftCard.setFacing(true, false)
		bodyTable.add(leftCard).grow().center().height(cardHeight).pad(10f).uniform()

		bodyTable.add(Seperator(Statics.skin, true)).growY()

		val scrollTable = Table()
		val scrollPane = ScrollPane(scrollTable, Statics.skin)
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
				card.addPick(Localisation.getText("deckscreen.pickcharacter", "UI")) {
					Global.deck.chosenCharacter = char
					createCharacterScreen()

					if (!didEdit)
					{
						didEdit = true

						val bundle = Statics.analytics.getParamBundle()
						Statics.analytics.customEvent("editted_deck", bundle)
					}
				}
				card.setFacing(true, false)
				scrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				scrollTable.row()
			}
		}

		val returnButton = TextButton(Localisation.getText("deckscreen.return", "UI"), Statics.skin)
		returnButton.name = "Return"
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	fun createEncounterScreen()
	{
		Global.deck.hasNewEncounters = false

		mainTable.clear()

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.5f, 0.5f, 0.5f, 1.0f))

		val cardHeight = (Statics.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Statics.resolution.x.toFloat() * 0.3f

		val title = Label(Localisation.getText("deckscreen.encounterselection", "UI"), Statics.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(2f)
		mainTable.row()

		mainTable.add(Label(Global.deck.playerDeck.encounters.size.toString() + " / $encounterDeckSize", Statics.skin))
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(2f)
		mainTable.row()

		val bodyTable = Table()
		mainTable.add(bodyTable).grow()
		mainTable.row()

		// Left stuff
		val leftScrollTable = Table()
		val leftScrollPane = ScrollPane(leftScrollTable, Statics.skin)
		leftScrollPane.setForceScroll(false, true)
		leftScrollPane.setFadeScrollBars(false)
		leftScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(leftScrollPane).grow().uniform()

		val used = ObjectSet<Card>()
		val deckShops = Array<CardWidget>()
		val deckEncounters = Array<CardWidget>()

		for (enc in Global.deck.playerDeck.encounters)
		{
			val card = enc.current.getCard()

			card.setSize(cardWidth, cardHeight)
			card.addPick(Localisation.getText("deckscreen.remove", "UI")) {
				Global.deck.playerDeck.encounters.removeValue(enc, true)
				createEncounterScreen()

				if (!didEdit)
				{
					didEdit = true

					val bundle = Statics.analytics.getParamBundle()
					Statics.analytics.customEvent("editted_deck", bundle)
				}
			}
			card.setFacing(true, false)

			if (enc.current.isShop)
			{
				deckShops.add(card)
			}
			else
			{
				deckEncounters.add(card)
			}

			used.add(enc)
		}

		if (deckShops.size > 0)
		{
			leftScrollTable.add(Label(Localisation.getText("deckscreen.shops", "UI"), Statics.skin, "title"))
			leftScrollTable.row()

			for (card in deckShops)
			{
				leftScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				leftScrollTable.row()
			}
		}

		if (deckEncounters.size > 0)
		{
			leftScrollTable.add(Label(Localisation.getText("deckscreen.encounters", "UI"), Statics.skin, "title"))
			leftScrollTable.row()

			for (card in deckEncounters)
			{
				leftScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				leftScrollTable.row()
			}
		}

		bodyTable.add(Seperator(Statics.skin, true)).growY()

		// Right stuff
		val rightScrollTable = Table()
		val rightScrollPane = ScrollPane(rightScrollTable, Statics.skin)
		rightScrollPane.setForceScroll(false, true)
		rightScrollPane.setFadeScrollBars(false)
		rightScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(rightScrollPane).grow().uniform()

		val unusedShops = Array<CardWidget>()
		val unusedEncounters = Array<CardWidget>()

		for (enc in Global.deck.encounters)
		{
			if (!used.contains(enc))
			{
				val card = enc.current.getCard()

				card.setSize(cardWidth, cardHeight)
				card.addPick(Localisation.getText("deckscreen.add", "UI")) {
					Global.deck.playerDeck.encounters.add(enc)
					createEncounterScreen()

					if (!didEdit)
					{
						didEdit = true

						val bundle = Statics.analytics.getParamBundle()
						Statics.analytics.customEvent("editted_deck", bundle)
					}
				}
				card.setFacing(true, false)

				if (enc.current.isShop)
				{
					unusedShops.add(card)
				}
				else
				{
					unusedEncounters.add(card)
				}
			}
		}

		if (unusedShops.size > 0)
		{
			rightScrollTable.add(Label(Localisation.getText("deckscreen.shops", "UI"), Statics.skin, "title"))
			rightScrollTable.row()

			for (card in unusedShops)
			{
				rightScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				rightScrollTable.row()
			}
		}

		if (unusedEncounters.size > 0)
		{
			rightScrollTable.add(Label(Localisation.getText("deckscreen.encounters", "UI"), Statics.skin, "title"))
			rightScrollTable.row()

			for (card in unusedEncounters)
			{
				rightScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
				rightScrollTable.row()
			}
		}

		val returnButton = TextButton(Localisation.getText("deckscreen.return", "UI"), Statics.skin)
		returnButton.name = "Return"
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	fun createEquipmentScreen()
	{
		Global.deck.hasNewEquipment = false

		mainTable.clear()

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.5f, 0.5f, 0.5f, 1.0f))

		val cardHeight = (Statics.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Statics.resolution.x.toFloat() * 0.3f

		val title = Label(Localisation.getText("deckscreen.equipmentselection", "UI"), Statics.skin, "title")
		mainTable.add(title).expandX().center().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(2f)
		mainTable.row()

		mainTable.add(Label(Global.deck.playerDeck.equipment.size.toString() + " / $equipmentDeckSize", Statics.skin))
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(2f)
		mainTable.row()

		val bodyTable = Table()

		mainTable.add(bodyTable).grow()
		mainTable.row()

		// Left stuff
		val leftScrollTable = Table()
		val leftScrollPane = ScrollPane(leftScrollTable, Statics.skin)
		leftScrollPane.setForceScroll(false, true)
		leftScrollPane.setFadeScrollBars(false)
		leftScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(leftScrollPane).grow().uniform()

		val groupedDeckEquipment = FastEnumMap<EquipmentSlot, Array<CardWidget>>(EquipmentSlot::class.java)

		val used = ObjectSet<Equipment>()
		for (equip in Global.deck.playerDeck.equipment)
		{
			val card = equip.getCard(null, false)
			card.setSize(cardWidth, cardHeight)
			card.addPick(Localisation.getText("deckscreen.remove", "UI")) {
				Global.deck.playerDeck.equipment.removeValue(equip, true)
				createEquipmentScreen()

				if (!didEdit)
				{
					didEdit = true

					val bundle = Statics.analytics.getParamBundle()
					Statics.analytics.customEvent("editted_deck", bundle)
				}
			}
			card.setFacing(true, false)

			var array = groupedDeckEquipment[equip.slot]
			if (array == null)
			{
				array = Array()
				groupedDeckEquipment[equip.slot] = array
			}
			array.add(card)

			used.add(equip)
		}

		for (slot in EquipmentSlot.Values)
		{
			val array = groupedDeckEquipment[slot]
			if (array != null)
			{
				leftScrollTable.add(Label(slot.toString().neaten(), Statics.skin, "title"))
				leftScrollTable.row()

				for (card in array)
				{
					leftScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
					leftScrollTable.row()
				}
			}
		}

		bodyTable.add(Seperator(Statics.skin, true)).growY()

		// Right stuff
		val rightScrollTable = Table()
		val rightScrollPane = ScrollPane(rightScrollTable, Statics.skin)
		rightScrollPane.setForceScroll(false, true)
		rightScrollPane.setFadeScrollBars(false)
		rightScrollPane.setScrollingDisabled(true, false)
		bodyTable.add(rightScrollPane).grow().uniform()

		val groupedUnusedEquipment = FastEnumMap<EquipmentSlot, Array<CardWidget>>(EquipmentSlot::class.java)

		for (equip in Global.deck.equipment)
		{
			if (!used.contains(equip))
			{
				val card = equip.getCard(null, false)
				card.setSize(cardWidth, cardHeight)
				card.addPick(Localisation.getText("deckscreen.add", "UI")) {
					Global.deck.playerDeck.equipment.add(equip)
					createEquipmentScreen()

					if (!didEdit)
					{
						didEdit = true

						val bundle = Statics.analytics.getParamBundle()
						Statics.analytics.customEvent("editted_deck", bundle)
					}
				}
				card.setFacing(true, false)

				var array = groupedUnusedEquipment[equip.slot]
				if (array == null)
				{
					array = Array()
					groupedUnusedEquipment[equip.slot] = array
				}
				array.add(card)
			}
		}

		for (slot in EquipmentSlot.Values)
		{
			val array = groupedUnusedEquipment[slot]
			if (array != null)
			{
				rightScrollTable.add(Label(slot.toString().neaten(), Statics.skin, "title"))
				rightScrollTable.row()

				for (card in array)
				{
					rightScrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(5f)
					rightScrollTable.row()
				}
			}
		}

		val returnButton = TextButton(Localisation.getText("deckscreen.return", "UI"), Statics.skin)
		returnButton.name = "Return"
		returnButton.addClickListener {
			createMainScreen()
		}

		mainTable.add(returnButton).expandX().right().pad(10f)
	}

	override fun doRender(delta: Float)
	{

	}
}
