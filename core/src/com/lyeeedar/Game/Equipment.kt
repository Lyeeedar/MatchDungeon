package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Equipment(val path: String)
{
	lateinit var name: String
	lateinit var description: String
	lateinit var icon: Sprite
	var cost: Int = 0
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	var ability: Ability? = null

	lateinit var slot: EquipmentSlot

	fun getCard(other: Equipment?, showAsPlus: Boolean): CardWidget
	{
		val basicTable = Table()
		basicTable.add(Label(name, Global.skin, "cardtitle")).expandX().center()
		basicTable.row()
		basicTable.add(SpriteWidget(icon.copy(), 64f, 64f)).grow()
		basicTable.row()

		val card = CardWidget(basicTable, createTable(other, showAsPlus), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, this)
		return card
	}

	fun createTable(other: Equipment?, showAsPlus: Boolean): Table
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(icon, 64f, 64f)).expandX().right().pad(5f)
		titleStack.add(iconTable)
		titleStack.add(Label(name, Global.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()
		val descLabel = Label(description, Global.skin, "card")
		descLabel.setWrap(true)
		table.add(descLabel)
		table.row()

		if (statistics.any { it != 0f } || (other != null && other.statistics.any{ it != 0f }))
		{
			table.add(Seperator(Global.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
			table.row()

			table.add(Label("Statistics", Global.skin, "cardtitle"))
			table.row()

			for (stat in Statistic.Values)
			{
				val statVal = statistics[stat] ?: 0f

				val statTable = Table()
				statTable.add(Label(stat.toString().toLowerCase().capitalize() + ": ", Global.skin, "card")).expandX().left()
				statTable.add(Label(statVal.toString(), Global.skin, "card"))

				var add = false

				if (other != null)
				{
					val otherStatVal = other.statistics[stat] ?: 0f

					if (otherStatVal != 0f || statVal != 0f)
					{
						add = true
					}

					if (otherStatVal == statVal)
					{

					}
					else if (otherStatVal < statVal)
					{
						val diff = statVal - otherStatVal
						val diffLabel = Label("+" + diff.toString(), Global.skin, "cardwhite")
						diffLabel.color = Color.GREEN
						statTable.add(diffLabel)
					}
					else if (statVal < otherStatVal)
					{
						val diff = otherStatVal - statVal
						val diffLabel = Label("-" + diff.toString(), Global.skin, "cardwhite")
						diffLabel.color = Color.RED
						statTable.add(diffLabel)
					}
				}
				else
				{
					if (statVal != 0f)
					{
						add = true
					}

					if (showAsPlus)
					{
						val diff = statVal
						val diffLabel = Label("+" + diff.toString(), Global.skin, "cardwhite")
						diffLabel.color = Color.GREEN
						statTable.add(diffLabel)
					}
				}

				if (add)
				{
					table.add(statTable)
					table.row()
				}
			}
		}

		if (ability != null || (other?.ability != null))
		{
			table.add(Seperator(Global.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
			table.row()

			table.add(Label("Ability", Global.skin, "cardtitle"))
			table.row()

			if (other?.ability != null)
			{
				val otherAbLabel = Label("-" + other.ability!!.name, Global.skin, "cardwhite")
				otherAbLabel.color = Color.RED

				val abilityTable = Table()
				abilityTable.add(otherAbLabel)

				abilityTable.add(SpriteWidget(other.ability!!.icon, 32f, 32f))

				val infoButton = Button(Global.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					val t = other.ability!!.createTable()

					FullscreenTable.createCard(t, infoButton.localToStageCoordinates(Vector2()))
				}
				abilityTable.add(infoButton).size(24f).expandX().right().pad(0f, 10f, 0f, 0f)

				table.add(abilityTable)
				table.row()
			}

			if (ability != null)
			{
				val abilityTable = Table()
				abilityTable.add(Label(ability!!.name, Global.skin, "card"))
				abilityTable.add(SpriteWidget(ability!!.icon, 32f, 32f))

				val infoButton = Button(Global.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					val t = ability!!.createTable()

					FullscreenTable.createCard(t, infoButton.localToStageCoordinates(Vector2()))
				}
				abilityTable.add(infoButton).size(24f).expandX().right().pad(0f, 10f, 0f, 0f)

				table.add(abilityTable).growX()
				table.row()
			}
		}

		return table
	}

	fun parse(xml: XmlData)
	{
		name = xml.get("Name")
		description = xml.get("Description")
		cost = xml.getInt("Cost", 100)
		icon = AssetManager.loadSprite(xml.getChildByName("Icon")!!)

		Statistic.parse(xml.getChildByName("Statistics")!!, statistics)

		val abilityEl = xml.getChildByName("Ability")
		if (abilityEl != null)
		{
			ability = Ability.load(abilityEl)
		}

		slot = EquipmentSlot.valueOf(xml.name.toUpperCase())
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
	}
}