package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.Direction
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.asGdxArray
import ktx.collections.toGdxArray

class CardContentActionRewards : AbstractCardContentAction()
{
	val rewards = Array<AbstractReward>()

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children)
		{
			rewards.add(AbstractReward.load(el))
		}
	}

	var grouped: Array<Array<AbstractReward>> = Array()
	var currentGroup = Array<CardWidget>()
	val greyOutTable = Table()
	var awaitingAdvance = false

	init
	{
		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (currentGroup.size > 0)
		{
			// do nothing
		}
		else
		{
			// advance

			if (!awaitingAdvance && grouped.size == 0)
			{
				grouped = rewards.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
				Global.stage.addActor(greyOutTable)
				awaitingAdvance = true
			}

			if (grouped.size > 0)
			{
				val chosen = grouped.removeIndex(0)
				currentGroup = chosen.map { it.reward() }.filter { it != null }.map { it!! }.toGdxArray()

				for (card in currentGroup)
				{
					for (pick in card.pickFuns)
					{
						val oldFun = pick.pickFun
						pick.pickFun = {
							oldFun(it)
							currentGroup.removeValue(card, true)
							if (currentGroup.size == 0)
							{
								CardContent.advance(CardContentScreen)
							}

							card.remove()
						}
					}

					Global.stage.addActor(card)
				}

				if (currentGroup.size > 0)
				{
					CardWidget.layoutCards(currentGroup, Direction.CENTER)
				}
				else
				{
					CardContent.advance(CardContentScreen)
				}
			}
		}

		val complete = grouped.size == 0 && currentGroup.size == 0
		if (complete)
		{
			greyOutTable.remove()
		}

		return complete
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

abstract class AbstractReward
{
	abstract fun parse(xmlData: XmlData)
	abstract fun reward(): CardWidget?

	companion object
	{
		fun load(xmlData: XmlData): AbstractReward
		{
			val reward: AbstractReward = when (xmlData.name)
			{
				"Card" -> CardReward()
				"Money" -> MoneyReward()
				"Equipment" -> EquipmentReward()
				else -> throw RuntimeException("Invalid reward type: " + xmlData.name)
			}

			reward.parse(xmlData)
			return reward
		}
	}
}

class CardReward : AbstractReward()
{
	lateinit var cardPath: String

	override fun parse(xmlData: XmlData)
	{
		cardPath = xmlData.get("File")
	}

	override fun reward(): CardWidget?
	{
		val card = Card.load(cardPath)

		val table = card.current.createTable()
		val cardWidget = CardWidget(table, null)
		cardWidget.addPick("", {
			Global.deck.encounters.add(card)
		})

		cardWidget.canZoom = false

		return cardWidget
	}
}

class MoneyReward : AbstractReward()
{
	var amount: Int = 0

	override fun parse(xmlData: XmlData)
	{
		amount = xmlData.getInt("Count")
	}

	override fun reward(): CardWidget?
	{
		val table = Table()

		val title = Label("Gold", Global.skin, "cardtitle")
		table.add(title).expandX().center().padTop(10f)
		table.row()

		val amountLbl = Label(amount.toString(), Global.skin, "cardtitle")
		table.add(amountLbl).expandX().center().padTop(10f)
		table.row()

		val card = CardWidget(table, null)
		card.addPick("Take", {
			Global.player.gold += amount
		})
		card.canZoom = false

		return card
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
		equipmentPath = xmlData.get("Equipment")
	}

	override fun reward(): CardWidget?
	{
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
				return null
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
		val table = equipment.createTable(equipped)

		val card = CardWidget(table, null)
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

		return card
	}
}