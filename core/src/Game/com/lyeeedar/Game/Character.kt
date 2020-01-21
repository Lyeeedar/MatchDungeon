package com.lyeeedar.Game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*

class Character(val path: String)
{
	lateinit var name: String
	lateinit var description: String
	lateinit var sprite: Sprite
	val baseStatistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)
	val cards = Array<Card>()

	val emptySlot = AssetManager.loadSprite("Icons/Empty")

	fun getCard(): CardWidget
	{
		val table = Table()
		table.defaults().growX()

		table.add(Table()).grow()
		table.row()

		val descLabel = Label(description, Statics.skin, "card")
		descLabel.setWrap(true)
		table.add(descLabel)
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard")).expand().pad(3f, 0f, 3f, 0f)
		table.row()

		table.add(Label("Statistics", Statics.skin, "cardtitle"))
		table.row()

		table.add(Statistic.createTable(baseStatistics, Statistic.Companion.DisplayType.FLAT)).growX()
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard")).expand().pad(3f, 0f, 3f, 0f)
		table.row()

		table.add(Label("Equipment", Statics.skin, "cardtitle"))
		table.row()

		for (slot in EquipmentSlot.Values)
		{
			val equipment = equipment[slot]

			if (equipment == null)
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label("None", Statics.skin, "card"))
				equipTable.add(SpriteWidget(emptySlot, 32f, 32f)).size(32f).expandX().right()
			}
			else
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label(equipment.name, Statics.skin, "card"))

				val infoButton = Button(Statics.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					FullscreenTable.createCard(equipment.getCard(null, false), infoButton.localToStageCoordinates(Vector2(12f, 12f)))
				}
				equipTable.add(infoButton).size(24f).pad(0f, 12f, 0f, 12f).expandX().right()

				val iconStack = Stack()
				iconStack.add(SpriteWidget(emptySlot, 32f, 32f))
				iconStack.add(SpriteWidget(equipment.icon, 32f, 32f))
				equipTable.add(iconStack).size(32f)
			}
		}

		table.add(Table()).grow()
		table.row()

		return CardWidget.Companion.createCard(name, "Character", Sprite(sprite.textures[0]), table, AssetManager.loadTextureRegion("GUI/CharacterCardback")!!, this)
	}

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name")
		description = xmlData.get("Description")
		sprite = AssetManager.loadSprite(xmlData.getChildByName("Sprite")!!)
		Statistic.parse(xmlData.getChildByName("Statistics")!!, baseStatistics)

		val equipmentEl = xmlData.getChildByName("StartingEquipment")
		if (equipmentEl != null)
		{
			for (equipEl in equipmentEl.children())
			{
				val equip = Equipment.load(equipEl.text)
				equipment[equip.slot] = equip
			}
		}

		val cardsEl = xmlData.getChildByName("Cards")
		if (cardsEl != null)
		{
			for (el in cardsEl.children())
			{
				val card = Card.load(el.text)
				cards.add(card)
			}
		}
	}

	fun save(output: Output)
	{
		output.writeString(path)

		output.writeInt(cards.size)
		for (card in cards)
		{
			card.save(output)
		}
	}

	companion object
	{
		fun load(path: String): Character
		{
			val xml = getXml("Characters/$path")

			val character = Character(path)
			character.parse(xml)

			return character
		}

		fun load(input: Input): Character
		{
			val path = input.readString()

			val character = load(path)
			character.cards.clear()

			val numCards = input.readInt()
			for (i in 0 until numCards)
			{
				val card = Card.load(input)
				character.cards.add(card)
			}

			return character
		}
	}
}