package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.EquipmentReward
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.asGdxArray

class CardContentActionShop : AbstractCardContentAction()
{
	val wares = Array<ShopWares>()

	override fun parse(xmlData: XmlData)
	{

	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{

	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

abstract class ShopWares
{
	var cost: Int = 1
	var onPurchaseDefine: Define? = null

	abstract fun isValid(): Boolean
	abstract fun resolve(shop: CardContentActionShop)
	abstract fun parse(xmlData: XmlData)

	companion object
	{
		fun load(xmlData: XmlData): ShopWares
		{
			val ware: ShopWares = when(xmlData.name.toUpperCase())
			{
				"EQUIPMENT" -> EquipmentWare()
				"QUEST" -> QuestWare()
				"CARD" -> CardWare()
				"CHARACTER" -> CharacterWare()
				else -> throw Exception("Unknown are type '" + xmlData.name.toUpperCase() + "'!")
			}

			ware.cost = xmlData.getInt("Cost", 1)

			ware.parse(xmlData)

			val onPurchaseDefineEl = xmlData.getChildByName("OnPurchaseDefine")
			if (onPurchaseDefineEl != null)
			{
				ware.onPurchaseDefine = Define()
				ware.onPurchaseDefine!!.parse(onPurchaseDefineEl)
			}

			return ware
		}
	}
}

class Define
{
	lateinit var key: String
	lateinit var value: String
	var global: Boolean = false

	fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
		value = xmlData.get("Value")
		global = xmlData.getBoolean("Global", false)
	}
}

class EquipmentWare : ShopWares()
{
	var fromDeck = true
	lateinit var type: EquipmentReward.EquipmentRewardType
	lateinit var equipmentPath: String

	var equipment: Equipment? = null

	override fun isValid(): Boolean = equipment != null

	override fun resolve(shop: CardContentActionShop)
	{
		if (fromDeck)
		{
			val types = Array<EquipmentSlot>()

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.ARMOUR || type == EquipmentReward.EquipmentRewardType.BODY)
			{
				types.add(EquipmentSlot.BODY)
			}

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.ARMOUR || type == EquipmentReward.EquipmentRewardType.HEAD)
			{
				types.add(EquipmentSlot.HEAD)
			}

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.WEAPON || type == EquipmentReward.EquipmentRewardType.MAINHAND)
			{
				types.add(EquipmentSlot.MAINHAND)
			}

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.WEAPON || type == EquipmentReward.EquipmentRewardType.OFFHAND)
			{
				types.add(EquipmentSlot.OFFHAND)
			}

			val equipped = Global.player.getEquippedSet()
			val resolved = shop.wares.mapNotNull { (it as? EquipmentWare)?.equipment?.path }.toSet()
			val validEquipment = Global.player.deck.equipment.filter { types.contains(it.slot) && !equipped.contains(it.path) && !resolved.contains(it.path) }
			if (!validEquipment.isEmpty())
			{
				val biasedArray = Array<Equipment>()
				for (equip in validEquipment)
				{
					if (Global.player.getEquipment(equip.slot) == null)
					{
						biasedArray.add(equip)
						biasedArray.add(equip)
					}
					else
					{
						biasedArray.add(equip)
					}
				}

				equipment = biasedArray.random()
			}
		}
		else
		{
			equipment = Equipment.load(equipmentPath)
		}
	}

	override fun parse(xmlData: XmlData)
	{
		fromDeck = xmlData.getBoolean("FromDeck", false)
		type = EquipmentReward.EquipmentRewardType.valueOf(xmlData.get("Type", "Any")!!.toUpperCase())
		equipmentPath = xmlData.get("Equipment", "")!!
	}
}

class QuestWare : ShopWares()
{
	lateinit var questPath: String

	override fun parse(xmlData: XmlData)
	{
		questPath = xmlData.get("File")
	}

	override fun isValid(): Boolean
	{
		return Global.deck.quests.firstOrNull { it.path == questPath } == null
	}

	override fun resolve(shop: CardContentActionShop)
	{

	}
}

class CardWare : ShopWares()
{
	lateinit var cardPath: String

	override fun parse(xmlData: XmlData)
	{
		cardPath = xmlData.get("File")
	}

	override fun isValid(): Boolean
	{
		return Global.deck.encounters.firstOrNull { it.path == cardPath } == null
	}

	override fun resolve(shop: CardContentActionShop)
	{

	}
}

class CharacterWare : ShopWares()
{
	lateinit var path: String

	override fun parse(xmlData: XmlData)
	{
		path = xmlData.get("File")
	}

	override fun isValid(): Boolean
	{
		return Global.deck.characters.firstOrNull { it.path == path } == null
	}

	override fun resolve(shop: CardContentActionShop)
	{

	}
}