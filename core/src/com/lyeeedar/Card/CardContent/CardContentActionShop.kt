package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.*
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class CardContentActionShop : AbstractCardContentAction()
{
	lateinit var costMultiplier: String
	val wares = Array<ShopWares>()

	override fun parse(xmlData: XmlData)
	{
		costMultiplier = xmlData.get("CostMultiplier", "1")!!.toLowerCase()

		val itemsEl = xmlData.getChildByName("Items")!!
		for (itemEl in itemsEl.children)
		{
			wares.add(ShopWares.load(itemEl))
		}
	}

	var shopActive = false
	var doAdvance = false
	var resolved = false
	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (doAdvance)
		{
			doAdvance = false
			resolved = false
			return true
		}

		if (!shopActive)
		{
			shopActive = true

			CardContentScreen.buttonTable.clear()

			val waresTable = Table()

			val costMultiplier = this.costMultiplier.evaluate(Global.getVariableMap())
			val priceReductionMultiplier = 1.0f - Global.player.getStat(Statistic.PRICEREDUCTION)

			for (i in 0 until wares.size)
			{
				val currentWare = wares[i]

				if (!resolved)
				{
					currentWare.resolve(this)
				}

				if (currentWare.isValid())
				{
					val card = currentWare.getCard()
					card.setFacing(true, false)

					val cost = (currentWare.cost.toFloat() * costMultiplier * priceReductionMultiplier).toInt()

					if (Global.player.gold >= cost)
					{
						card.pickFuns.clear()
						card.addPick("Buy ($cost)", {
							Global.player.gold -= cost

							currentWare.reward()

							val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

							val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

							val dstTable = CardScreen.instance.playerSlot
							val dst = dstTable.localToStageCoordinates(Vector2())

							Mote(src, dst, sprite, 32f, {
							}, 0.75f)

							// Rebuild the ui
							shopActive = false
							advance(CardContent, CardContentScreen)
						})
					}

					val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
					val cardWidth = card.getWidthFromHeight(cardHeight)
					card.setSize(cardWidth, cardHeight)

					val table = Table()
					table.add(card).size(cardWidth, cardHeight).expand()
					table.row()

					val costLabel = Label("$cost", Global.skin)
					if (cost > Global.player.gold)
					{
						costLabel.color = Color.DARK_GRAY
					}

					table.add(costLabel).expandX().center()

					waresTable.add(table).pad(5f)
				}
			}

			resolved = true

			val scrollPane = ScrollPane(waresTable, Global.skin)
			scrollPane.setFadeScrollBars(false)
			scrollPane.setScrollingDisabled(false, true)
			scrollPane.setForceScroll(true, false)

			val leaveButton = TextButton("Leave", Global.skin)
			leaveButton.addClickListener {
				doAdvance = true
				CardContentScreen.buttonTable.clear()
				shopActive = false
				CardContentScreen.advanceContent()
			}

			val topTable = Table()
			topTable.add(Label("Gold: " + Global.player.gold, Global.skin))
			topTable.add(leaveButton).expandX().right().pad(10f)

			CardContentScreen.buttonTable.add(topTable).growX()
			CardContentScreen.buttonTable.row()

			CardContentScreen.buttonTable.add(Seperator(Global.skin)).growX()
			CardContentScreen.buttonTable.row()

			CardContentScreen.buttonTable.add(scrollPane).grow()
		}

		return false
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
	abstract fun getCard(): CardWidget
	abstract fun reward()

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
				"STATISTICS" -> StatisticWare()
				"BUFF" -> BuffWare()
				else -> throw Exception("Unknown ware type '" + xmlData.name.toUpperCase() + "'!")
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

	override fun isValid(): Boolean = equipment != null && Global.player.getEquipment(equipment!!.slot)?.path != equipment!!.path

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

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.HANDS || type == EquipmentReward.EquipmentRewardType.MAINHAND)
			{
				types.add(EquipmentSlot.MAINHAND)
			}

			if (type == EquipmentReward.EquipmentRewardType.ANY || type == EquipmentReward.EquipmentRewardType.HANDS || type == EquipmentReward.EquipmentRewardType.OFFHAND)
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

		cost = equipment!!.cost
	}

	override fun parse(xmlData: XmlData)
	{
		fromDeck = xmlData.getBoolean("FromDeck", true)
		type = EquipmentReward.EquipmentRewardType.valueOf(xmlData.get("Type", "Any")!!.toUpperCase())
		equipmentPath = xmlData.get("Equipment", "")!!
	}

	override fun getCard(): CardWidget
	{
		return equipment!!.getCard(Global.player.getEquipment(equipment!!.slot), true)
	}

	override fun reward()
	{
		Global.player.equipment[equipment!!.slot] = equipment
		CardScreen.instance.updateEquipment()
	}
}

class QuestWare : ShopWares()
{
	lateinit var questPath: String
	lateinit var quest: Quest

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
		quest = Quest.load(questPath)
	}

	override fun getCard(): CardWidget
	{
		return quest.getCard()
	}

	override fun reward()
	{
		Global.deck.quests.add(quest)
		Global.deck.newquests.add(quest)
	}
}

class CardWare : ShopWares()
{
	lateinit var cardPath: String
	lateinit var card: Card

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
		card = Card.load(cardPath)
	}

	override fun getCard(): CardWidget
	{
		return card.current.getCard()
	}

	override fun reward()
	{
		Global.deck.encounters.add(card)
		Global.deck.newencounters.add(card)
	}
}

class CharacterWare : ShopWares()
{
	lateinit var path: String
	lateinit var character: Character

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
		character = Character.load(path)
	}

	override fun getCard(): CardWidget
	{
		return character.getCard()
	}

	override fun reward()
	{
		Global.deck.characters.add(character)
		Global.deck.newcharacters.add(character)
	}
}

class StatisticWare : ShopWares()
{
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)

	override fun isValid(): Boolean = true

	override fun resolve(shop: CardContentActionShop)
	{

	}

	override fun parse(xmlData: XmlData)
	{
		val stats = xmlData.getChildByName("Statistics")!!
		Statistic.parse(stats, statistics)
	}

	override fun getCard(): CardWidget
	{
		val table = Table()
		for (stat in Statistic.Values)
		{
			val statVal = statistics[stat] ?: 0f

			if (statVal != 0f)
			{
				val diff = statVal
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
				statTable.add(Label(statVal.toString() + diffStr, Global.skin, "card"))
				statTable.addTapToolTip(stat.tooltip)

				table.add(statTable).growX()
				table.row()
			}
		}

		return CardWidget(table, table, AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!, null)
	}

	override fun reward()
	{
		for (stat in Statistic.Values)
		{
			val statVal = statistics[stat] ?: 0f
			Global.player.statistics[stat] = Global.player.statistics[stat] + statVal
		}
	}

}

class BuffWare : ShopWares()
{
	lateinit var buff: Buff

	override fun isValid(): Boolean = true

	override fun resolve(shop: CardContentActionShop)
	{

	}

	override fun parse(xmlData: XmlData)
	{
		val buffEl = xmlData.getChildByName("Buff")!!
		buff = Buff.load(buffEl)
	}

	override fun getCard(): CardWidget
	{
		return buff.getCard()
	}

	override fun reward()
	{
		Global.player.buffs.add(buff.copy())
	}
}