package com.lyeeedar.Game

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.asGdxArray

enum class Chance private constructor(val eqn: String, val uiString: String)
{
	VERYLOW("chance(1,5)", "very low"),
	LOW("chance(1,4)", "low"),
	MEDIUM("chance(1,3)", "medium"),
	HIGH("chance(1,2)", "high"),
	ALWAYS("1", "certain");

	fun evaluate(): Boolean = eqn.evaluate() != 0f
}

abstract class AbstractReward
{
	lateinit var chance: Chance

	abstract fun parse(xmlData: XmlData)
	abstract fun reward(): Array<CardWidget>
	abstract fun isValid(): Boolean
	abstract fun cardIcon(): TextureRegion

	companion object
	{
		fun load(xmlData: XmlData): AbstractReward
		{
			val reward: AbstractReward = when (xmlData.name.toUpperCase())
			{
				"CARD" -> CardReward()
				"MONEY" -> MoneyReward()
				"EQUIPMENT" -> EquipmentReward()
				"QUEST" -> QuestReward()
				"CHARACTER" -> CharacterReward()
				"STATISTICS" -> StatisticsReward()
				else -> throw RuntimeException("Invalid reward type: " + xmlData.name)
			}

			reward.chance = Chance.valueOf(xmlData.get("Chance", "Always")!!.toUpperCase())

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
		Statistic.parse(xmlData, statsTable)
	}

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		for (stat in Statistic.Values)
		{
			val statVal = statsTable[stat]
			if (statVal != 0f)
			{
				val table = Table()
				table.add(Label(stat.toString().capitalize(), Global.skin, "cardtitle"))
				table.row()
				table.add(Label(statVal.toString(), Global.skin, "cardtitle"))
				table.row()

				val card = CardWidget(table, table, AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!, null)
				card.canZoom = false
				card.addPick("Take", {

					Global.player.statistics[stat] = (Global.player.statistics[stat] ?: 0f) + statVal

					val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/key_ornate")

					val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

					val dstTable = CardScreen.instance.playerSlot
					val dst = dstTable.localToStageCoordinates(Vector2())

					Mote(src, dst, sprite, 32f, {

					}, 0.75f)
				})

				output.add(card)
			}
		}

		return output
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
			cardWidget.addPick("", {
				Global.deck.encounters.add(card)

				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				Mote(src, dst, sprite, 32f, {
				}, 0.75f)
			})

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
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
			cardWidget.addPick("", {
				Global.deck.quests.add(quest)

				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				Mote(src, dst, sprite, 32f, {
				}, 0.75f)
			})

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
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
			cardWidget.addPick("", {
				Global.deck.characters.add(c)

				val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

				val src = cardWidget.localToStageCoordinates(Vector2(cardWidget.width / 2f, cardWidget.height / 2f))

				val dstTable = CardScreen.instance.playerSlot
				val dst = dstTable.localToStageCoordinates(Vector2())

				Mote(src, dst, sprite, 32f, {
				}, 0.75f)
			})

			cardWidget.canZoom = false

			output.add(cardWidget)
		}

		return output
	}
}

class MoneyReward : AbstractReward()
{
	var amount: Int = 0

	override fun isValid(): Boolean = true

	override fun parse(xmlData: XmlData)
	{
		amount = xmlData.getInt("Count")
	}

	override fun cardIcon(): TextureRegion = AssetManager.loadTextureRegion("GUI/MoneyCardback")!!

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val table = Table()

		val title = Label("Gold", Global.skin, "cardtitle")
		table.add(title).expandX().center().padTop(10f)
		table.row()

		val amountLbl = Label(amount.toString(), Global.skin, "cardtitle")
		table.add(amountLbl).expandX().center().padTop(10f)
		table.row()

		val card = CardWidget(table, table, AssetManager.loadTextureRegion("GUI/MoneyCardback")!!, null)
		card.addPick("Take", {

			Global.player.gold += amount

			val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold")

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.playerSlot
			val dst = dstTable.localToStageCoordinates(Vector2())

			Mote(src, dst, sprite, 32f, {
			}, 0.75f)
		})
		card.canZoom = false

		output.add(card)

		return output
	}
}

class EquipmentReward : AbstractReward()
{
	enum class EquipmentRewardType
	{
		ANY,
		WEAPON,
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
		rewardType = EquipmentRewardType.valueOf(xmlData.get("Type", "Any")!!.toUpperCase())

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

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.WEAPON || rewardType == EquipmentRewardType.MAINHAND)
			{
				types.add(EquipmentSlot.MAINHAND)
			}

			if (rewardType == EquipmentRewardType.ANY || rewardType == EquipmentRewardType.WEAPON || rewardType == EquipmentRewardType.OFFHAND)
			{
				types.add(EquipmentSlot.OFFHAND)
			}

			val validEquipment = Global.player.deck.equipment.filter { types.contains(it.slot) }
			if (!validEquipment.isEmpty())
			{
				equipment = validEquipment.asGdxArray().random()
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
			}
		}

		val equipped = Global.player.getEquipment(equipment.slot)

		val card = equipment.getCard(equipped)
		card.addPick("Equip", {
			Global.player.equipment[equipment.slot] = equipment

			val sprite = equipment.icon.copy()

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.getSlot(equipment.slot)
			val dst = dstTable.localToStageCoordinates(Vector2())

			Mote(src, dst, sprite, 32f, {
				CardScreen.instance.updateEquipment()
			}, 0.75f)
		})
		card.addPick("Discard", {

		})

		output.add(card)

		return output
	}
}