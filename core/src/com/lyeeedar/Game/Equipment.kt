package com.lyeeedar.Game

import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Equipment
{
	lateinit var name: String
	lateinit var description: String
	lateinit var icon: Sprite
	val statistics = FastEnumMap<Statistic, Int>(Statistic::class.java)
	var ability: Ability? = null

	lateinit var slot: EquipmentSlot

	fun parse(xml: XmlData)
	{
		name = xml.get("Name")
		description = xml.get("Description")
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
			val xml = getXml("Equipment/$path")
			return load(xml)
		}

		fun load(xml: XmlData): Equipment
		{
			val equipment = Equipment()
			equipment.parse(xml)
			return equipment
		}
	}
}