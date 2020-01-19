package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.spawnMote
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.*
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*

class CardContentActionShop : AbstractCardContentAction()
{
	lateinit var merchant: Sprite
	val counter = AssetManager.loadSprite("Oryx/Custom/terrain/table", drawActualSize = true)
	val counter_papers = AssetManager.loadSprite("Oryx/Custom/terrain/table_papers", drawActualSize = true)
	val counter_large = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/table", drawActualSize = true)
	val counter_large_sold = AssetManager.loadSprite("Oryx/Custom/terrain/table_large_sold", drawActualSize = true)

	lateinit var costMultiplier: String
	val wares = Array<ShopWare>()

	override fun parse(xmlData: XmlData)
	{
		merchant = AssetManager.loadSprite(xmlData.getChildByName("ShopKeep")!!)
		costMultiplier = xmlData.get("CostMultiplier", "1")!!.toLowerCase()

		val itemsEl = xmlData.getChildByName("Items")!!
		for (itemEl in itemsEl.children)
		{
			wares.add(ShopWare.load(itemEl))
		}
	}

	var shopActive = false
	var doAdvance = false
	var resolved = false
	val itemsToBuy = Array2D<ShopWare?>(4, 3) { x,y -> null }
	val purchasesTable = Table()
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

			CardContentScreen.contentTable.clear()

			CardContentScreen.contentTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.4f, 0.4f, 0.4f, 1f))

			val merchantTable = Table()
			val wallTable = Table()
			wallTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/wall_stone_14")))

			val merchantRow = Table()
			merchantRow.add(SpriteWidget(merchant, 48f, 48f)).size(48f).expandX().center()
			val counterRow = Table()
			for (i in 0 until 4)
			{
				if (i == 1)
				{
					counterRow.add(SpriteWidget(counter_papers, 48f, 48f)).size(48f)
				}
				else
				{
					counterRow.add(SpriteWidget(counter, 48f, 48f)).size(48f)
				}
			}

			merchantTable.add(wallTable).height(48f).growX()
			merchantTable.row()
			merchantTable.add(merchantRow).growX()
			merchantTable.row()
			merchantTable.add(counterRow).growX()

			var x = 0
			var y = 0

			for (i in 0 until wares.size)
			{
				val ware = wares[i]

				if (!resolved)
				{
					ware.resolve(this)
				}

				if (ware.isValid())
				{
					itemsToBuy[x, y] = ware

					x++
					if (x == 4)
					{
						x = 0
						y++

						if (y == 4)
						{
							throw RuntimeException("Too many shop wares!")
						}
					}
				}
			}

			resolved = true

			CardContentScreen.contentTable.add(merchantTable).growX().top()
			CardContentScreen.contentTable.row()
			CardContentScreen.contentTable.add(purchasesTable).grow().padTop(20f).padBottom(20f)
			CardContentScreen.contentTable.row()

			val leaveButton = TextButton("Leave Shop", Statics.skin)
			leaveButton.addClickListener {
				doAdvance = true
				CardContentScreen.contentTable.clear()
				shopActive = false
				CardContentScreen.advanceContent()
			}
			CardContentScreen.contentTable.add(leaveButton).expandX().center().pad(10f)

			fillPurchasesTable()
		}

		return false
	}

	fun fillPurchasesTable()
	{
		val costMultiplier = this.costMultiplier.evaluate(Global.getVariableMap())
		val priceReductionMultiplier = 1.0f - Global.player.getStat(Statistic.PRICEREDUCTION)

		purchasesTable.clear()

		for (y in 0 until itemsToBuy.height)
		{
			for (x in 0 until itemsToBuy.width)
			{
				val purchaseStack = Stack()

				val ware = itemsToBuy[x, y]
				if (ware == null)
				{
					purchaseStack.addTable(SpriteWidget(counter_large_sold, 48f, 48f)).size(48f).expand().bottom()
					purchaseStack.addTable(Table()).size(48f).padBottom(40f).expand().bottom()
				}
				else
				{
					val cost = (ware.cost.toFloat() * costMultiplier * priceReductionMultiplier).toInt()

					purchaseStack.addTable(SpriteWidget(counter_large, 48f, 48f)).size(48f).expand().bottom()
					purchaseStack.addTable(ware.wareTable(48f)).size(48f).expand().bottom().padBottom(40f)

					val costTable = Table()
					costTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))

					val costLabel = Label(cost.prettyPrint(), Statics.skin)
					if (cost > Global.player.gold)
					{
						costLabel.setColor(0.85f, 0f, 0f, 1f)
					}

					costTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 16f, 16f))
					costTable.add(costLabel)

					purchaseStack.addTable(costTable).expand().bottom().padBottom(20f)

					purchaseStack.addClickListener {
						val card = ware.getCard()
						card.frontTable.clear()
						card.setFacing(faceup = true, animate = false)
						card.setPosition(purchaseStack.x + purchaseStack.width / 2f, purchaseStack.y + purchaseStack.height)
						card.setSize(48f, 48f)

						if (Global.player.gold >= ware.cost)
						{
							card.pickFuns.clear()
							card.addPick("Buy ($cost)") {
								Statics.crashReporter.logDebug("Purchasing ware $ware")

								Global.player.gold -= cost

								ware.reward()
								CardScreen.instance.updateEquipment()

								val sprite = AssetManager.loadSprite("Oryx/Custom/items/card")

								val src = card.localToStageCoordinates(Vector2(card.width / 2f, card.height / 2f))

								val dstTable = CardScreen.instance.playerSlot
								val dst = dstTable.localToStageCoordinates(Vector2())

								spawnMote(src, dst, sprite, 32f, {
								}, 0.75f)

								// Rebuild the ui
								itemsToBuy[x, y] = null
								fillPurchasesTable()
							}
						}

						card.collapseFun = {
							card.remove()
						}

						Statics.stage.addActor(card)

						card.focus()
					}
				}

				purchasesTable.add(purchaseStack).expand()
			}

			purchasesTable.row()
		}
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

abstract class ShopWare
{
	var cost: Int = 1
	var onPurchaseDefine: Define? = null

	abstract fun isValid(): Boolean
	abstract fun resolve(shop: CardContentActionShop)
	abstract fun parse(xmlData: XmlData)
	abstract fun getCard(): CardWidget
	abstract fun reward()
	abstract fun wareTable(size: Float): Table

	companion object
	{
		fun load(xmlData: XmlData): ShopWare
		{
			val ware: ShopWare = when(xmlData.name.toUpperCase())
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

class EquipmentWare : ShopWare()
{
	lateinit var type: EquipmentReward.EquipmentRewardType
	lateinit var equipmentPath: String

	var equipment: Equipment? = null

	override fun isValid(): Boolean = equipment != null && Global.player.equipment[equipment!!.slot]?.path != equipment!!.path

	override fun resolve(shop: CardContentActionShop)
	{
		if (equipmentPath.isBlank())
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
				equipment = Equipment.Companion.load(chosen.path)
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
		equipmentPath = xmlData.get("Equipment", "")!!

		if (equipmentPath.isBlank())
		{
			type = EquipmentReward.EquipmentRewardType.valueOf(xmlData.get("Type", "Any")!!.toUpperCase())
		}
	}

	override fun getCard(): CardWidget
	{
		return equipment!!.getCard(Global.player.equipment[equipment!!.slot], true)
	}

	override fun reward()
	{
		Global.player.equipment[equipment!!.slot] = equipment
	}

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 0.5f)))

		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))
		val tileFront = SpriteWidget(equipment!!.icon.copy(), size, size)
		equipmentStack.add(tileFront)

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}

class QuestWare : ShopWare()
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

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 1f)))

		val tileFront = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_latch"), size, size)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}

class CardWare : ShopWare()
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

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 1f)))

		val tileFront = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/card"), size, size)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}

class CharacterWare : ShopWare()
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

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 1f)))

		val tileFront = SpriteWidget(Sprite(character.sprite.textures[0]), size, size)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}

class StatisticWare : ShopWare()
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
		val t1: Table
		val t2: Table

		val modifiedStats = Array<Pair<Statistic, Float>>()
		for (stat in Statistic.Values)
		{
			val statVal = statistics[stat] ?: 0f

			if (statVal != 0f)
			{
				modifiedStats.add(Pair(stat, statVal))
			}
		}

		if (modifiedStats.size == 1)
		{
			val stat = modifiedStats[0].first
			val value = modifiedStats[0].second

			t1 = CardWidget.createFrontTable(FrontTableSimple(stat.niceName, "Statistic", stat.icon.copy(), AssetManager.loadSprite("GUI/StatisticsCardback"), value.toString()))
			t2 = CardWidget.createFrontTable(FrontTableSimple(stat.niceName, "Statistic", stat.icon.copy(), AssetManager.loadSprite("GUI/StatisticsCardback"), value.toString()))
		}
		else
		{
			val iconsTable = Table()
			val statsTable = Table()
			for (pair in modifiedStats)
			{
				val stat = pair.first
				val value = pair.second

				iconsTable.add(SpriteWidget(stat.icon.copy(), 64f, 64f)).grow()

				statsTable.add(Label(stat.niceName + ":", Statics.skin, "cardtitle"))
				statsTable.add(Label(value.toString(), Statics.skin, "cardtitle"))
				statsTable.row()
			}

			t1 = Table()
			t1.add(iconsTable).grow()
			t1.row()
			t1.add(statsTable)

			t2 = Table()
			t2.add(iconsTable).grow()
			t2.row()
			t2.add(statsTable)
		}

		return CardWidget(t1, t2, AssetManager.loadTextureRegion("GUI/StatisticsCardback")!!, null)
	}

	override fun reward()
	{
		for (stat in Statistic.Values)
		{
			val statVal = statistics[stat] ?: 0f

			if (statVal != 0f)
			{
				val currentStat = Global.player.statistics[stat] ?: 0f
				Global.player.statistics[stat] = currentStat + statVal
			}
		}
	}

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 0.5f)))

		val tileFront = SpriteWidget(Statistic.Values.first { statistics[it] != 0f }.icon.copy(), size, size)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}

class BuffWare : ShopWare()
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

	override fun wareTable(size: Float): Table
	{
		val equipmentStack = Stack()
		val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
		equipmentStack.add(tileBack)

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, 1f)))

		val tileFront = SpriteWidget(buff.icon.copy(), size, size)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}
}