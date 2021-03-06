package com.lyeeedar.Game

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.spawnMote
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.Storage
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.FrontTableSimple
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import java.util.*

enum class Chance private constructor(val chance: Float, val colour: Colour)
{
	VERYLOW(0.1f, Colour(124, 115, 98, 255)),
	LOW(0.25f, Colour(237, 154, 21, 255)),
	MEDIUM(0.5f, Colour(200, 200, 200, 255)),
	HIGH(0.75f, Colour(255, 238, 163, 255)),
	ALWAYS(1f, Colour(249, 209, 27, 255));

	val niceName: String
		get()
		{
			return when(this)
			{
				VERYLOW -> Localisation.getText("chance.verylow", "UI")
				LOW -> Localisation.getText("chance.low", "UI")
				MEDIUM -> Localisation.getText("chance.medium", "UI")
				HIGH -> Localisation.getText("chance.high", "UI")
				ALWAYS -> Localisation.getText("chance.certain", "UI")
			}
		}

	fun evaluate(): Boolean = Random.random() <= chance + Global.player.getStat(Statistic.LUCK)
}

abstract class AbstractReward
{
	lateinit var chance: Chance

	abstract fun parse(xmlData: XmlData)
	abstract fun reward(): Array<CardWidget>
	abstract fun isValid(): Boolean
	abstract fun cardIcon(): TextureRegion
	abstract fun niceName(): String

	companion object
	{
		fun load(xmlData: XmlData): AbstractReward
		{
			val reward: AbstractReward = when (xmlData.name.toUpperCase(Locale.ENGLISH))
			{
				"CARD" -> CardReward()
				"MONEY" -> MoneyReward()
				"EQUIPMENT" -> EquipmentReward()
				"QUEST" -> QuestReward()
				"CHARACTER" -> CharacterReward()
				"STATISTICS" -> StatisticsReward()
				"BUFF" -> BuffReward()
				"ITEM" -> ItemReward()
				else -> throw RuntimeException("Invalid reward type: " + xmlData.name)
			}

			reward.chance = Chance.valueOf(xmlData.get("Chance", "Always")!!.toUpperCase(Locale.ENGLISH))

			reward.parse(xmlData)
			return reward
		}
	}
}

class StatisticsReward : AbstractReward()
{
	val statsTable = FastEnumMap<Statistic, Float>(Statistic::class.java)

	override fun isValid(): Boolean = true

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!

	override fun parse(xmlData: XmlData)
	{
		val statsEl = xmlData.getChildByName("Statistics")!!

		Statistic.parse(statsEl, statsTable)
	}

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		for (stat in Statistic.Values)
		{
			val statVal = statsTable[stat]
			if (statVal != 0f)
			{
				Global.player.statistics[stat] = (Global.player.statistics[stat] ?: 0f) + statVal

				val card = CardWidget.createCard(stat.niceName, Localisation.getText("statistic", "UI"), stat.icon.copy(), Table(), AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!, topText = statVal.toString())
				card.canZoom = false
				card.addPick("") {

					val sprite = stat.icon.copy()

					val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

					val dstTable = CardScreen.instance.playerSlot
					val dst = dstTable.localToStageCoordinates(Vector2())

					spawnMote(src, dst, sprite, 64f, {

					}, 0.75f, adjustDuration = false)
				}

				output.add(card)
			}
		}

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("statistic", "UI")
	}
}

class CardReward : AbstractReward()
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

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/CardCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		if (isValid())
		{
			val card = Card.load(cardPath)

			val cardWidget = card.current.getCard()

			Global.deck.encounters.add(card)
			Global.deck.newencounters.add(card)

			Global.deck.hasNewEncounters = true

			cardWidget.addPick("") {
				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				spawnMote(src, dst, sprite, 64f, {
				}, 0.75f, adjustDuration = false)
			}

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("encounter", "UI")
	}
}

class QuestReward : AbstractReward()
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

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/QuestCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		if (isValid())
		{
			val quest = Quest.load(questPath)

			val cardWidget = quest.getCard()

			Global.deck.quests.add(quest)
			Global.deck.newquests.add(quest)

			cardWidget.addPick("") {

				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				spawnMote(src, dst, sprite, 64f, {
				}, 0.75f, adjustDuration = false)
			}

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("quest", "UI")
	}
}

class CharacterReward : AbstractReward()
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

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/CharacterCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		if (isValid())
		{
			val c = Character.load(path)

			val cardWidget = c.getCard()

			Global.deck.characters.add(c)
			Global.deck.newcharacters.add(c)

			Global.deck.hasNewCharacters = true

			cardWidget.addPick("") {

				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				spawnMote(src, dst, sprite, 64f, {
				}, 0.75f, adjustDuration = false)
			}

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("character", "UI")
	}
}

class MoneyReward : AbstractReward()
{
	lateinit var amountEqn: String

	override fun isValid(): Boolean = true

	override fun parse(xmlData: XmlData)
	{
		amountEqn = xmlData.get("Count").toLowerCase(Locale.ENGLISH)
	}

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/MoneyCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val amount = amountEqn.evaluate(Global.getVariableMap()).toInt()
		val bonus = (Global.player.getStat(Statistic.BONUSGOLD) * amount).toInt()

		var amountStr = amount.toString()
		if (bonus > 0)
		{
			amountStr += "+$bonus"
		}

		val table = CardWidget.createFrontTable(FrontTableSimple(Localisation.getText("gold", "UI"), Localisation.getText("gold", "UI"), AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), AssetManager.loadSprite("GUI/MoneyCardback"), amountStr))

		val card = CardWidget(table, Table(), AssetManager.loadTextureRegion("GUI/MoneyCardback")!!, null)
		card.addPick(Localisation.getText("take", "UI")) {

			Global.player.gold += amount + bonus

			val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold")

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.playerSlot
			val dst = dstTable.localToStageCoordinates(Vector2())

			spawnMote(src, dst, sprite, 64f, {
				CardScreen.instance.updateEquipment()
			}, 0.75f, adjustDuration = false)
		}
		card.canZoom = false

		output.add(card)

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("gold", "UI")
	}
}

class BuffReward : AbstractReward()
{
	lateinit var buffXml: XmlData

	override fun isValid(): Boolean = true

	override fun parse(xmlData: XmlData)
	{
		buffXml = xmlData.getChildByName("Buff")!!
	}

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/BuffCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val buff = Buff.load(buffXml)
		buff.remainingDuration += (buff.remainingDuration * Global.player.getStat(Statistic.BUFFDURATION)).toInt()
		buff.remainingDuration++ // add one cause as soon as the level ends itll be decreased by 1

		val existing = Global.player.buffs.firstOrNull{ it.data.nameID != buff.data.nameID }
		if (existing != null)
		{
			existing.remainingDuration = buff.remainingDuration
		}
		else
		{
			Global.player.buffs.add(buff)
		}

		val card = buff.getCard()
		card.addPick("") {

			val sprite = buff.data.icon.copy()

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.playerSlot
			val dst = dstTable.localToStageCoordinates(Vector2())

			spawnMote(src, dst, sprite, 64f, {
			}, 0.75f, adjustDuration = false)
		}
		card.canZoom = false

		output.add(card)

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("buff", "UI")
	}
}

class EquipmentReward : AbstractReward()
{
	enum class EquipmentRewardType
	{
		ANY,
		HANDS,
		ARMOUR,
		MAINHAND,
		OFFHAND,
		HEAD,
		BODY
	}

	var fromDeck: Boolean = false
	lateinit var rewardType: EquipmentRewardType

	var unlock: Boolean = false
	lateinit var equipmentPath: String

	override fun isValid(): Boolean = true

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!

	override fun parse(xmlData: XmlData)
	{
		fromDeck = xmlData.getBoolean("FromDeck", false)
		rewardType = EquipmentRewardType.valueOf(xmlData.get("Type", "Any")!!.toUpperCase(Locale.ENGLISH))

		unlock = xmlData.getBoolean("Unlock", false)
		equipmentPath = xmlData.get("Equipment", "")!!
	}

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val equipment: Equipment

		if (fromDeck)
		{
			val types = Array<EquipmentSlot>()

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.ARMOUR || rewardType == EquipmentRewardType.BODY)
			{
				types.add(EquipmentSlot.BODY)
			}

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.ARMOUR || rewardType == EquipmentRewardType.HEAD)
			{
				types.add(EquipmentSlot.HEAD)
			}

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.HANDS || rewardType == EquipmentRewardType.MAINHAND)
			{
				types.add(EquipmentSlot.MAINHAND)
			}

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.HANDS || rewardType == EquipmentRewardType.OFFHAND)
			{
				types.add(EquipmentSlot.OFFHAND)
			}

			val equipped = Global.player.getEquippedSet()
			val validEquipment = Global.player.deck.equipment.filter { types.contains(it.slot) && !equipped.contains(it.path) }
			if (!validEquipment.isEmpty())
			{
				val biasedArray = Array<Equipment>()
				for (equip in validEquipment)
				{
					if (Global.player.equipment[equip.slot] == null)
					{
						biasedArray.add(equip)
						biasedArray.add(equip)
					}
					else
					{
						biasedArray.add(equip)
					}
				}

				val chosen = biasedArray.random()
				equipment = Equipment.Companion.load(chosen.path) // load a copy
			}
			else
			{
				return output
			}
		}
		else
		{
			equipment = Equipment.Companion.load(equipmentPath)
			if (unlock && Global.deck.equipment.firstOrNull { it.path == equipmentPath } == null)
			{
				Global.deck.equipment.add(equipment)
				Global.deck.newequipment.add(equipment)

				Global.deck.hasNewEquipment = true
			}
		}

		val equipped = Global.player.equipment[equipment.slot]

		val card = equipment.getCard(equipped, true)
		card.addPick(Localisation.getText("equip", "UI")) {
			Global.player.equipment[equipment.slot] = equipment

			val sprite = equipment.icon.copy()

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.getSlot(equipment.slot)
			val dst = dstTable.localToStageCoordinates(Vector2())

			spawnMote(src, dst, sprite, 64f, {
				CardScreen.instance.updateEquipment()
			}, 0.75f, adjustDuration = false)
		}

		val sellAmount = equipment.cost / 4
		card.addPick(Localisation.getText("sell", "UI") + " ($sellAmount)") {
			Global.player.gold += sellAmount

			val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold")

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.playerSlot
			val dst = dstTable.localToStageCoordinates(Vector2())

			spawnMote(src, dst, sprite, 64f, {
				CardScreen.instance.updateEquipment()
			}, 0.75f, adjustDuration = false)
		}

		output.add(card)

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("equipment", "UI")
	}
}

class ItemReward : AbstractReward()
{
	lateinit var name: String
	lateinit var icon: Sprite
	lateinit var key: String
	lateinit var value: String
	lateinit var storage: Storage

	override fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name")
		icon = AssetManager.loadSprite(xmlData.getChildByName("Icon")!!)

		key = xmlData.get("Key").toLowerCase(Locale.ENGLISH)
		value = xmlData.get("Value").toLowerCase(Locale.ENGLISH)
		storage = Storage.valueOf(xmlData.get("Storage").toUpperCase(Locale.ENGLISH))
	}

	override fun isValid(): Boolean = true

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/ItemCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		if (isValid())
		{
			val flags = when (storage){
				Storage.CARD -> Global.cardflags
				Storage.QUEST -> Global.questflags
				Storage.GLOBAL -> Global.globalflags
			}

			flags.flags.put(key, value.evaluate(Global.getVariableMap()))

			val table = CardWidget.createFrontTable(FrontTableSimple(name, Localisation.getText("item", "UI"), icon.copy()))

			val cardWidget = CardWidget(table, Table(), AssetManager.loadTextureRegion("GUI/ItemCardback")!!, null)
			cardWidget.canZoom = false
			cardWidget.addPick("") {
				val sprite = icon.copy()

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				spawnMote(src, dst, sprite, 64f, {
				}, 0.75f, adjustDuration = false)
			}

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
	}

	override fun niceName(): String
	{
		return Localisation.getText("item", "UI")
	}
}