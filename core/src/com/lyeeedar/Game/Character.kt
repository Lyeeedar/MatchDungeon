package com.lyeeedar.Game

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Character(val path: String)
{
	lateinit var name: String
	lateinit var description: String
	lateinit var sprite: Sprite
	val baseStatistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	val emptySlot = AssetManager.loadSprite("Icons/Empty")

	fun getCard(): CardWidget
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(Sprite(sprite.textures[0]), 48f, 48f)).expandX().right().pad(5f).padRight(25f)
		titleStack.add(iconTable)
		titleStack.add(Label(name, Global.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()
		val descLabel = Label(description, Global.skin, "card")
		descLabel.setWrap(true)
		table.add(descLabel)
		table.row()

		table.add(Seperator(Global.skin, false))
		table.row()

		table.add(Label("Statistics", Global.skin, "cardtitle"))
		table.row()

		for (stat in Statistic.Values)
		{
			val statVal = baseStatistics[stat] ?: 0

			val statTable = Table()
			statTable.add(Label(stat.toString().toLowerCase().capitalize() + ": ", Global.skin, "card"))
			statTable.add(Label(statVal.toString(), Global.skin, "card"))

			table.add(statTable)
			table.row()
		}

		table.add(Seperator(Global.skin, false))
		table.row()

		table.add(Label("Equipment", Global.skin, "cardtitle"))
		table.row()

		for (slot in EquipmentSlot.Values)
		{
			val equipment = equipment[slot]

			if (equipment == null)
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label("None", Global.skin, "card"))
				equipTable.add(SpriteWidget(emptySlot, 32f, 32f)).size(32f).expandX().right()
			}
			else
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label(equipment.name, Global.skin, "card"))

				val iconStack = Stack()
				iconStack.add(SpriteWidget(emptySlot, 32f, 32f))
				iconStack.add(SpriteWidget(equipment.icon, 32f, 32f))
				equipTable.add(iconStack).size(32f).expandX().right()

				val infoButton = Button(Global.skin, "info")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {

				}
				equipTable.add(infoButton).pad(0f, 12f, 0f, 12f)
			}
		}

		val basicTable = Table()
		basicTable.add(Label(name, Global.skin, "cardtitle")).expandX().center()
		basicTable.row()
		basicTable.add(SpriteWidget(Sprite(sprite.textures[0]), 64f, 64f)).grow()
		basicTable.row()

		return CardWidget(basicTable, table, AssetManager.loadTextureRegion("GUI/CharacterCardback")!!, this)
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
	}
}