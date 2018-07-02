package com.lyeeedar.Game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
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

abstract class AbstractReward
{
	abstract fun parse(xmlData: XmlData)
	abstract fun reward(): Array<CardWidget>

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
				"STATISTICS" -> StatisticsReward()
				else -> throw RuntimeException("Invalid reward type: " + xmlData.name)
			}

			reward.parse(xmlData)
			return reward
		}
	}
}

class StatisticsReward : AbstractReward()
{
	val statsTable = FastEnumMap<Statistic, Int>(Statistic::class.java)

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
			if (statVal != 0)
			{
				val table = Table()
				table.add(Label(stat.toString().capitalize(), Global.skin, "cardtitle"))
				table.row()
				table.add(Label(statVal.toString(), Global.skin, "cardtitle"))
				table.row()

				val card = CardWidget(table, table, AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!, null)
				card.canZoom = false
				card.addPick("Take", {
					Global.player.statistics[stat] = (Global.player.statistics[stat] ?: 0) + statVal
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

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val card = Card.load(cardPath)

		val cardWidget = card.current.getCard()
		cardWidget.addPick("", {
			Global.deck.encounters.add(card)
		})

		cardWidget.canZoom = false

		output.add(cardWidget)

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

	override fun reward(): Array<CardWidget>
	{
		val output = Array<CardWidget>()

		val quest = Quest.load(questPath)

		val cardWidget = quest.getCard()
		cardWidget.addPick("", {
			Global.deck.quests.add(quest)
		})

		cardWidget.canZoom = false

		output.add(cardWidget)

		return output
	}
}

class MoneyReward : AbstractReward()
{
	var amount: Int = 0

	override fun parse(xmlData: XmlData)
	{
		amount = xmlData.getInt("Count")
	}

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
			if (unlock)
			{
				Global.deck.equipment.add(equipment)
			}
		}

		val equipped = Global.player.getEquipment(equipment.slot)

		val card = equipment.getCard(equipped)
		card.addPick("Equip", {
			val sprite = equipment.icon.copy()

			val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

			val dstTable = CardScreen.instance.getSlot(equipment.slot)
			val dst = dstTable.localToStageCoordinates(Vector2())

			Mote(src, dst, sprite, 32f, {
				Global.player.equipment[equipment.slot] = equipment
				CardScreen.instance.updateEquipment()
			}, 1f)
		})
		card.addPick("Discard", {

		})

		output.add(card)

		return output
	}
}