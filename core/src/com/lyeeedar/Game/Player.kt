package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Statistic
import com.lyeeedar.Util.FastEnumMap

class Player(val baseCharacter: Character)
{
	var gold: Int = 0

	val statistics = FastEnumMap<Statistic, Int>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	val deck = Deck()

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
}

class Deck
{
	val encounters = Array<Card>()
	val equipment = Array<Equipment>()
}