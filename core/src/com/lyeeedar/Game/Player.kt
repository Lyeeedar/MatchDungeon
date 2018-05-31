package com.lyeeedar.Game

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.Statistic
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.FastEnumMap

class Player(val baseCharacter: Character)
{
	var gold: Int = 0

	val statistics = FastEnumMap<Statistic, Int>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	val deck = Global.deck.copy()

	fun getStat(statistic: Statistic): Int
	{
		var stat = baseCharacter.baseStatistics[statistic] ?: 0
		stat += statistics[statistic] ?: 0

		for (slot in EquipmentSlot.Values)
		{
			val equip = getEquipment(slot)
			if (equip != null)
			{
				stat += equip.statistics[statistic] ?: 0
			}
		}

		return stat
	}

	fun getEquipment(equipmentSlot: EquipmentSlot): Equipment?
	{
		return equipment[equipmentSlot] ?: baseCharacter.equipment[equipmentSlot]
	}

	fun createTable(): Table
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(baseCharacter.sprite.copy(), 64f, 64f)).left().pad(5f)
		titleStack.add(iconTable)
		titleStack.add(Label(baseCharacter.name, Global.skin, "title"))

		table.add(titleStack).growX()
		table.row()

		val descLabel = Label(baseCharacter.description, Global.skin)
		descLabel.setWrap(true)

		table.add(descLabel).growX()
		table.row()

		table.add(Seperator(Global.skin))
		table.row()

		table.add(Label("Statistics", Global.skin, "title"))
		table.row()

		for (stat in Statistic.Values)
		{
			val basestat = baseCharacter.baseStatistics[stat] ?: 0
			val truestat = getStat(stat)

			val diff = truestat - basestat
			val diffStr: String
			if (diff > 0)
			{
				diffStr = "[GREEN]+$diff[]"
			}
			else if (diff < 0)
			{
				diffStr = "[RED]-$diff[]"
			}
			else
			{
				diffStr = ""
			}

			val str = stat.toString().toLowerCase().capitalize() + ": " + truestat + " (" + basestat + diffStr + ")"
			table.add(Label(str, Global.skin))
			table.row()
		}

		return table
	}
}

class Deck
{
	val encounters = Array<Card>()
	val equipment = Array<Equipment>()

	fun copy(): Deck
	{
		val deck = Deck()
		deck.encounters.addAll(encounters)
		deck.equipment.addAll(equipment)

		return deck
	}
}