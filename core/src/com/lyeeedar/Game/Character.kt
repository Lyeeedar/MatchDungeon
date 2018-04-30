package com.lyeeedar.Game

import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Character
{
	lateinit var name: String
	lateinit var description: String
	lateinit var sprite: Sprite
	val baseStatistics = FastEnumMap<Statistic, Int>(Statistic::class.java)
	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

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

			val character = Character()
			character.parse(xml)

			return character
		}
	}
}