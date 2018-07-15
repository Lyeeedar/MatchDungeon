package com.lyeeedar.Game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.Statistic
import com.lyeeedar.UI.FullscreenTable
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap

class Player(val baseCharacter: Character, val deck: PlayerDeck)
{
	var gold: Int = 0

	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	fun getStat(statistic: Statistic): Int
	{
		return getStatRaw(statistic).toInt()
	}

	fun getStatRaw(statistic: Statistic): Float
	{
		var stat = baseCharacter.baseStatistics[statistic] ?: 0f
		stat += statistics[statistic] ?: 0f

		for (slot in EquipmentSlot.Values)
		{
			val equip = getEquipment(slot)
			if (equip != null)
			{
				stat += equip.statistics[statistic] ?: 0f
			}
		}

		return stat
	}

	fun getEquipment(equipmentSlot: EquipmentSlot): Equipment?
	{
		return equipment[equipmentSlot] ?: baseCharacter.equipment[equipmentSlot]
	}

	fun getEquippedSet(): ObjectSet<String>
	{
		val output = ObjectSet<String>()

		for (slot in EquipmentSlot.Values)
		{
			val equip = getEquipment(slot) ?: continue
			output.add(equip.path)
		}

		return output
	}

	fun createTable(): Table
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(baseCharacter.sprite.copy(), 64f, 64f)).left().pad(5f)
		titleStack.add(iconTable)
		titleStack.add(Label(baseCharacter.name, Global.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()

		val descLabel = Label(baseCharacter.description, Global.skin, "card")
		descLabel.setWrap(true)

		table.add(descLabel).growX()
		table.row()

		table.add(Seperator(Global.skin))
		table.row()

		table.add(Label("Statistics", Global.skin, "cardtitle"))
		table.row()

		for (stat in Statistic.Values)
		{
			val basestat = (baseCharacter.baseStatistics[stat] ?: 0f) + (statistics[stat] ?: 0f)
			val truestat = getStatRaw(stat)

			val diff = truestat - basestat
			val diffStr: String
			if (diff > 0)
			{
				diffStr = "[GREEN]+$diff[]"
			}
			else if (diff < 0)
			{
				diffStr = "[RED]$diff[]"
			}
			else
			{
				diffStr = ""
			}

			val statTable = Table()
			statTable.add(Label(stat.toString().toLowerCase().capitalize() + ":", Global.skin, "card")).expandX().left()
			statTable.add(Label(truestat.toInt().toString() + " (" + basestat + diffStr + ")", Global.skin, "card"))

			table.add(statTable).growX()
			table.row()
		}

		table.add(Seperator(Global.skin, false))
		table.row()

		table.add(Label("Equipment", Global.skin, "cardtitle"))
		table.row()

		val emptySlot = AssetManager.loadSprite("Icons/Empty")

		for (slot in EquipmentSlot.Values)
		{
			val equipment = getEquipment(slot)

			if (equipment == null)
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label("None", Global.skin,"card"))
				equipTable.add(SpriteWidget(emptySlot, 32f, 32f)).size(32f).expandX().right()
			}
			else
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label(equipment.name, Global.skin, "card"))

				val infoButton = Button(Global.skin, "info")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					val t = equipment.createTable(null, false)

					FullscreenTable.createCard(t, infoButton.localToStageCoordinates(Vector2(12f, 12f)))
				}
				equipTable.add(infoButton).size(24f).pad(0f, 12f, 0f, 12f).expandX().right()

				val iconStack = Stack()
				iconStack.add(SpriteWidget(emptySlot, 32f, 32f))
				iconStack.add(SpriteWidget(equipment.icon, 32f, 32f))
				equipTable.add(iconStack).size(32f)
			}
		}

		return table
	}
}

class PlayerDeck
{
	val encounters = Array<Card>()
	val equipment = Array<Equipment>()

	fun copy(): PlayerDeck
	{
		val deck = PlayerDeck()
		deck.encounters.addAll(encounters)
		deck.equipment.addAll(equipment)

		return deck
	}
}