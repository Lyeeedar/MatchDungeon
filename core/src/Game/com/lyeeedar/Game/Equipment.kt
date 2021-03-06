package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import java.util.*

class Equipment(val path: String)
{
	lateinit var nameID: String
	lateinit var descriptionID: String
	lateinit var icon: Sprite
	var cost: Int = 0
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	var ability: Ability? = null

	val name: String
		get() = Localisation.getText(nameID, "Equipment")
	val description: String
		get() = Localisation.getText(descriptionID, "Equipment")

	lateinit var slot: EquipmentSlot

	fun getCard(other: Equipment?, showAsPlus: Boolean): CardWidget
	{
		return CardWidget.createCard(name, Localisation.getText("equipment", "UI"), icon.copy(), createTable(other, showAsPlus), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, this)
	}

	fun createTable(other: Equipment?, showAsPlus: Boolean): Table
	{
		val table = Table()
		table.defaults().growX()

		table.add(Table()).grow()
		table.row()

		val descLabel = Label(description, Statics.skin, "card")
		descLabel.setWrap(true)
		table.add(descLabel)
		table.row()

		if (statistics.any { it != 0f } || (other != null && other.statistics.any{ it != 0f }))
		{
			table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f).expandY()
			table.row()

			table.add(Label(Localisation.getText("statistics", "UI"), Statics.skin, "cardtitle"))
			table.row()

			if (other != null)
			{
				table.add(Statistic.createTable(statistics, Statistic.Companion.DisplayType.COMPARISON, other.statistics)).growX()
				table.row()
			}
			else if (showAsPlus)
			{
				table.add(Statistic.createTable(statistics, Statistic.Companion.DisplayType.MODIFIER)).growX()
				table.row()
			}
			else
			{
				table.add(Statistic.createTable(statistics, Statistic.Companion.DisplayType.FLAT)).growX()
				table.row()
			}
		}

		if (ability != null || (other?.ability != null))
		{
			table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f).expandY()
			table.row()

			table.add(Label(Localisation.getText("ability", "UI"), Statics.skin, "cardtitle"))
			table.row()

			if (other?.ability != null)
			{
				val otherAbLabel = Label("-" + other.ability!!.name, Statics.skin, "cardwhite")
				otherAbLabel.color = Color(0.5f, 0f, 0f, 1f)

				val abilityTable = Table()
				abilityTable.add(otherAbLabel)

				abilityTable.add(SpriteWidget(other.icon, 32f, 32f))

				val infoButton = Button(Statics.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					FullscreenTable.createCard(other.ability!!.getCard(), infoButton.localToStageCoordinates(Vector2()))
				}
				abilityTable.add(infoButton).size(24f).expandX().right().pad(0f, 10f, 0f, 0f)

				table.add(abilityTable)
				table.row()
			}

			if (ability != null)
			{
				val abilityTable = Table()
				abilityTable.add(Label(ability!!.name, Statics.skin, "card"))
				abilityTable.add(SpriteWidget(icon, 32f, 32f))

				val infoButton = Button(Statics.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					FullscreenTable.createCard(ability!!.getCard(), infoButton.localToStageCoordinates(Vector2()))
				}
				abilityTable.add(infoButton).size(24f).expandX().right().pad(0f, 10f, 0f, 0f)

				table.add(abilityTable).growX()
				table.row()
			}
		}

		table.add(Table()).grow()
		table.row()

		return table
	}

	fun parse(xml: XmlData)
	{
		nameID = xml.get("Name")
		descriptionID = xml.get("Description")
		cost = xml.getInt("Cost", 100)
		icon = AssetManager.loadSprite(xml.getChildByName("Icon")!!)

		Statistic.parse(xml.getChildByName("Statistics")!!, statistics)

		val abilityEl = xml.getChildByName("Ability")
		if (abilityEl != null)
		{
			ability = Ability.load(abilityEl)
		}

		slot = EquipmentSlot.valueOf(xml.name.toUpperCase(Locale.ENGLISH))
	}

	fun copy(): Equipment = load(path)

	fun save(output: Output)
	{
		output.writeString(path)

		if (ability != null)
		{
			output.writeInt(ability!!.remainingUsages)
		}
	}

	companion object
	{
		fun load(path: String): Equipment
		{
			val xml = getXml(path)

			val equipment = Equipment(path)
			equipment.parse(xml)
			return equipment
		}

		fun load(input: Input): Equipment
		{
			val path = input.readString()
			val equip = load(path)

			if (equip.ability != null)
			{
				equip.ability!!.remainingUsages = input.readInt()
			}

			return equip
		}
	}
}