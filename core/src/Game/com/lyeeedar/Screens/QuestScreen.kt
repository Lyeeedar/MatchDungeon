package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.delay
import com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Card.CardNode
import com.lyeeedar.Direction
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.*
import com.lyeeedar.ScreenEnum
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import ktx.actors.then
import ktx.collections.toGdxArray
import java.util.*

class QuestScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	val statsTable = Table()
	val headSlot = Table()
	val mainhandSlot = Table()
	val offhandSlot = Table()
	val bodySlot = Table()
	val playerSlot = Table()
	lateinit var goldLabel: Label
	lateinit var questProgressWidget: QuestProgressWidget

	val cardsTable = Table()

	override fun create()
	{
		cardsTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/shadowborder"))

		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)

		goldLabel = Label("Gold: 0", Statics.skin)

		headSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		mainhandSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		offhandSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		bodySlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))

		// build equipment
		val equipmentTable = Table()
		equipmentTable.defaults().size(32f).uniform()
		equipmentTable.add(headSlot)
		equipmentTable.add(mainhandSlot)
		equipmentTable.add(offhandSlot)
		equipmentTable.add(bodySlot)

		// body, gold
		val topTable = Table()

		topTable.add(playerSlot).size(32f)
		topTable.add(goldLabel).expandX().left()

		// build stats
		statsTable.add(topTable).growX()
		statsTable.row()
		statsTable.add(equipmentTable).growX()

		statsTable.addClickListener {
			FullscreenTable.createCard(Global.player.getCard(), statsTable.localToStageCoordinates(Vector2()))
		}

		questProgressWidget = QuestProgressWidget()

		val nonCard = Table()
		nonCard.pad(10f)
		nonCard.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))

		val statsAndAbandon = Table()
		statsAndAbandon.add(statsTable).expand().left()


		if (Statics.settings.get("CompletedIntro", false))
		{
			val abandonQuestButton = TextButton("Abandon Quest", Statics.skin)
			abandonQuestButton.addClickListener {

				MessageBox(
					"Abandon Quest",
					"Are you sure you want to abandon the quest and return to the quest selection screen? You will lose any progress made, but keep any rewards you have earned.",
					Pair("Continue Quest", {
						val bundle = Statics.analytics.getBundle()
						bundle.setString("choice", "Continue")
						Statics.analytics.customEvent("abandon_quest", bundle)
					}),
					Pair("Abandon Quest", {
						val bundle = Statics.analytics.getBundle()
						bundle.setString("choice", "Abandon")
						Statics.analytics.customEvent("abandon_quest", bundle)

						currentQuest.state = Quest.QuestState.FAILURE; completeQuest()
					}))
			}
			statsAndAbandon.add(abandonQuestButton).expand().right().bottom()
		}

		nonCard.add(statsAndAbandon).growX().pad(10f)
		nonCard.row()

		nonCard.add(Seperator(Statics.skin)).growX().pad(0f, 10f, 0f, 10f)
		nonCard.row()

		nonCard.add(questProgressWidget).growX().height(20f).pad(2f)
		nonCard.row()

		nonCard.add(Seperator(Statics.skin)).growX().pad(0f, 10f, 0f, 10f)
		nonCard.row()

		mainTable.add(nonCard).growX()
		mainTable.row()

		mainTable.add(cardsTable).grow()

		if (Statics.debug)
		{
			debugConsole.register("LoadCard", "LoadCard cardName", fun(args, console): Boolean
			{
				if (args.isEmpty())
				{
					console.error("Invalid number of arguments!")
					return false
				}

				var card = Global.deck.encounters.backingArray.firstOrNull { it.current.name.toLowerCase(Locale.ENGLISH) == args[0].toLowerCase(Locale.ENGLISH) }
				if (card == null)
				{
					val cardPath = XmlData.existingPaths!!.firstOrNull { it.toLowerCase(Locale.ENGLISH).endsWith(args[0].toLowerCase(Locale.ENGLISH) + ".xml") }
					if (cardPath == null)
					{
						console.error("Invalid card name!")
						return false
					}

					val xml = getXml(cardPath)
					if (xml.name == "CardContent")
					{
						val node = CardNode()
						node.fillWithDefaults()
						node.content = cardPath

						val nodes = Array<CardNode>()
						nodes.add(node)
						card = Card("", null, nodes, node)
					}
					else
					{
						card = Card.load(cardPath)
					}
				}

				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Statics.game.switchScreen(ScreenEnum.CARD)

				Global.player.deck.encounters.removeValue(card, true)
				currentQuest.questCards.removeValue(card, true)

				return true
			})

			debugConsole.register("CompleteQuest", "", fun(args, console): Boolean
			{
				if (args[0].toLowerCase(Locale.ENGLISH) == "gold")
				{
					currentQuest.state = Quest.QuestState.GOLD
					completeQuest()
				}
				else if (args[0].toLowerCase(Locale.ENGLISH) == "silver")
				{
					currentQuest.state = Quest.QuestState.SILVER
					completeQuest()
				}
				else if (args[0].toLowerCase(Locale.ENGLISH) == "bronze")
				{
					currentQuest.state = Quest.QuestState.BRONZE
					completeQuest()
				}
				else if (args[0].toLowerCase(Locale.ENGLISH) == "fail")
				{
					currentQuest.state = Quest.QuestState.FAILURE
					completeQuest()
				}

				return true
			})

			debugConsole.register("PrintFlags", "", fun(args, console): Boolean
			{
				console.write("Global:")
				for (flag in Global.globalflags.flags)
				{
					console.write(flag.key + ": " + flag.value)
				}

				console.write("Quest:")
				for (flag in Global.questflags.flags)
				{
					console.write(flag.key + ": " + flag.value)
				}

				console.write("Card:")
				for (flag in Global.cardflags.flags)
				{
					console.write(flag.key + ": " + flag.value)
				}

				return true
			})

			debugConsole.register("Equip", "", fun(args, console): Boolean
			{
				val equipmentName = args[0]
				var equipment = Global.deck.equipment.firstOrNull { it.path.toLowerCase(Locale.ENGLISH).endsWith(equipmentName.toLowerCase(Locale.ENGLISH)) || it.name.toLowerCase(Locale.ENGLISH) == equipmentName.toLowerCase(Locale.ENGLISH) }
				if (equipment == null)
				{
					val equipmentPath = XmlData.existingPaths!!.firstOrNull { it.toLowerCase(Locale.ENGLISH).endsWith(equipmentName.toLowerCase(Locale.ENGLISH) + ".xml") }
					if (equipmentPath != null)
					{
						equipment = Equipment.Companion.load(equipmentPath)
					}
				}

				if (equipment == null)
				{
					console.error("Invalid equipment name!")
					return false
				}

				Global.player.equipment[equipment.slot] = equipment
				updateEquipment()

				return true
			})
		}
	}

	lateinit var currentQuest: Quest
	fun setup(quest: Quest)
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		quest.played = true

		questProgressWidget.quest = quest

		currentQuest = quest

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(quest.currentTheme.backgroundTile))).tint(Color.DARK_GRAY)

		updateEquipment()
		updateQuest()
	}

	var chosenQuestCard: CardWidget? = null
	val cardWidgets = Array<CardWidget>()
	var needsLayout = false
	fun updateQuest()
	{
		val chaoticNature = Global.player.getStat(Statistic.CHAOTICNATURE, false)
		if (chaoticNature != 0f)
		{
			for (stat in Statistic.Values)
			{
				val value = Global.player.getStat(stat, false)
				if (value != stat.min && value != 0f)
				{
					if (Random.random.nextBoolean())
					{
						Global.player.choaticNature[stat] = value * chaoticNature
					}
					else
					{
						Global.player.choaticNature[stat] = value * chaoticNature * -1f
					}
				}
				else
				{
					Global.player.choaticNature[stat] = 0f
				}
			}
		}

		for (widget in cardWidgets)
		{
			widget.remove()
		}
		cardWidgets.clear()

		currentQuest.run()
		if (currentQuest.current == null)
		{
			completeQuest()
			Save.save()
			return
		}

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(currentQuest.currentTheme.backgroundTile))).tint(Color(0.5f, 0.5f, 0.5f, 1f))

		val cards = (currentQuest.current as QuestNode).getCards()
		if (cards.size == 0)
		{
			val currentQuestNode = currentQuest.current as QuestNode
			currentQuest.current = currentQuestNode.getNext(CardContent.CardContentState.SUCCESS, null)

			updateQuest()
			Save.save()
			return
		}

		// create widgets
		for (card in cards)
		{
			val widget = card.current.getCard()
			widget.data = card

			widget.addPick("Choose") {
				chosenQuestCard = it

				for (w in cardWidgets)
				{
					w.clickable = false
				}
			}

			cardWidgets.add(widget)
			stage.addActor(widget)
		}

		needsLayout = true
	}

	fun updateEquipment()
	{
		playerSlot.clear()
		playerSlot.add(SpriteWidget(Global.player.baseCharacter.sprite, 32f, 32f)).grow()

		val createFun = fun(slot: EquipmentSlot, tableSlot: Table)
		{
			tableSlot.clearChildren()
			tableSlot.clearListeners()

			val equip = Global.player.equipment[slot]
			if (equip != null)
			{
				val stack = Stack()
				val widget = SpriteWidget(equip.icon, 32f, 32f)

				stack.add(widget)

				if (equip.ability != null)
				{
					if (equip.ability!!.maxUsages > 0 && !equip.ability!!.resetUsagesPerLevel)
					{
						val label = Label(equip.ability!!.remainingUsages.toString(), Statics.skin)
						stack.add(label)
					}
				}

				tableSlot.add(stack).grow()
			}
		}

		createFun(EquipmentSlot.HEAD, headSlot)
		createFun(EquipmentSlot.BODY, bodySlot)
		createFun(EquipmentSlot.MAINHAND, mainhandSlot)
		createFun(EquipmentSlot.OFFHAND, offhandSlot)

		goldLabel.setText("Gold: " + Global.player.gold)
	}

	fun completeQuest()
	{
		for (card in cardWidgets)
		{
			card.remove()
		}
		cardWidgets.clear()
		cardsTable.clear()

		chosenQuestCard = null

		stage.addActor(greyOutTable)

		val bundle = Statics.analytics.getBundle()
		bundle.setString("state", currentQuest.state.toString())
		bundle.setString("quest_name", currentQuest.title)
		Statics.analytics.customEvent("complete_quest", bundle)

		val text = if (currentQuest.state == Quest.QuestState.FAILURE) "Quest Failed!" else "Quest Completed!\n${currentQuest.state.toString().toLowerCase(Locale.ENGLISH).capitalize()}"
		val table = Table()
		table.add(Label(text, Statics.skin, "cardtitle"))

		val card = CardWidget(CardWidget.createFrontTable(FrontTableComplex("Complete", "Quest", table)), Table(), AssetManager.loadTextureRegion("blank")!!, null)
		card.canZoom = false
		card.setFacing(true, false)
		card.addPick("") {
			card.remove()
			updateRewards()
		}

		cardWidgets.add(card)

		stage.addActor(card)

		needsLayout = true
	}

	override fun show()
	{
		super.show()

		val tutorial = Tutorial("QuestScreen")
		tutorial.addDelay(1f)
		tutorial.addPopup("This is the quest screen.", Any())
		tutorial.addPopup("Here you can choose what you want to encounter next. Tap a card to view more details about it, and to select it.", cardsTable)
		tutorial.addPopup("Tap here to see your current character information, including statistics and equipment.", statsTable)
		tutorial.addPopup("Here you can see your current gold. You can use this to buy equipment in shops, or during encounters.", goldLabel)
		tutorial.show()
	}

	val greyOutTable = Table()
	fun updateRewards()
	{
		val doGoldReward = fun() {
			if (currentQuest.state.ordinal >= Quest.QuestState.GOLD.ordinal && !currentQuest.gotGold)
			{
				val gold = currentQuest.goldRewards.filter { it.isValid() }.flatMap{ it.reward() }.toGdxArray()

				if (gold.size > 0)
				{
					for (card in gold)
					{
						card.canZoom = false
					}

					CardWidget.displayLoot(gold, CardWidget.Companion.LootAnimation.COIN_GOLD) {
						needsAdvance = true
					}
				}
				else
				{
					needsAdvance = true
				}
			}
			else
			{
				needsAdvance = true
			}
		}

		val doSilverReward = fun() {
			if (currentQuest.state.ordinal >= Quest.QuestState.SILVER.ordinal && !currentQuest.gotSilver)
			{
				val silver = currentQuest.silverRewards.filter { it.isValid() }.flatMap{ it.reward() }.toGdxArray()

				if (silver.size > 0)
				{
					for (card in silver)
					{
						card.canZoom = false
					}

					CardWidget.displayLoot(silver, CardWidget.Companion.LootAnimation.COIN_SILVER) {
						doGoldReward()
					}
				}
				else
				{
					doGoldReward()
				}
			}
			else
			{
				doGoldReward()
			}
		}

		val doBronzeReward = fun() {
			if (currentQuest.state.ordinal >= Quest.QuestState.BRONZE.ordinal && !currentQuest.gotBronze)
			{
				val bronze = currentQuest.bronzeRewards.filter { it.isValid() }.flatMap{ it.reward() }.toGdxArray()

				if (bronze.size > 0)
				{
					for (card in bronze)
					{
						card.canZoom = false
					}

					CardWidget.displayLoot(bronze, CardWidget.Companion.LootAnimation.COIN_BRONZE) {
						doSilverReward()
					}
				}
				else
				{
					doSilverReward()
				}
			}
			else
			{
				doSilverReward()
			}
		}

		val doQuestUnlocks = fun() {

			val questunlocks = Array<CardWidget>()
			for (reward in Global.deck.newcharacters)
			{
				questunlocks.add(reward.getCard())
			}
			Global.deck.newcharacters.clear()

			for (reward in Global.deck.newencounters)
			{
				questunlocks.add(reward.current.getCard())
			}
			Global.deck.newencounters.clear()

			for (reward in Global.deck.newequipment)
			{
				questunlocks.add(reward.getCard(null, false))
			}
			Global.deck.newequipment.clear()

			for (reward in Global.deck.newquests)
			{
				questunlocks.add(reward.getCard())
			}
			Global.deck.newquests.clear()

			if (questunlocks.size > 0)
			{
				for (card in questunlocks)
				{
					card.canZoom = false
					card.addPick("") {  }
				}

				CardWidget.displayLoot(questunlocks, CardWidget.Companion.LootAnimation.CHEST) {
					doBronzeReward()
				}
			}
			else
			{
				doBronzeReward()
			}
		}

		doQuestUnlocks()
	}

	var needsAdvance = false
	override fun doRender(delta: Float)
	{
		if (needsAdvance && Mote.motes.size == 0)
		{
			needsAdvance = false

			Global.deck.newcharacters.clear()
			Global.deck.newencounters.clear()
			Global.deck.newequipment.clear()
			Global.deck.newquests.clear()

			QuestSelectionScreen.instance.setup()
			QuestSelectionScreen.instance.swapTo()
			greyOutTable.remove()
		}

		if (needsLayout && cardsTable.width != 0f)
		{
			CardWidget.layoutCards(cardWidgets, Direction.CENTER, cardsTable, flip = true)
			updateEquipment()
			needsLayout = false
		}

		if (chosenQuestCard != null && chosenQuestCard!!.actions.size == 0)
		{
			for (widget in cardWidgets)
			{
				if (widget != chosenQuestCard)
				{
					widget.dissolve(CardWidget.DissolveType.BURN, 0.6f, 2f)
				}
			}

			val delay = if (cardWidgets.size == 1) 0.2f else 0.8f

			val sequence = delay(delay) then lambda {
				val card = chosenQuestCard!!.data as Card

				Statics.logger.logDebug("Choosing card ${card.current.name}")

				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Statics.game.switchScreen(ScreenEnum.CARD)

				if (!card.current.isShop)
				{
					Global.player.deck.encounters.removeValue(card, true)
					currentQuest.questCards.removeValue(card, true)
					currentQuest.themeCards.removeValue(card, true)
				}

				chosenQuestCard = null
			} then removeActor()
			chosenQuestCard!!.addAction(sequence)

		}
	}

	companion object
	{
		lateinit var instance: QuestScreen
	}
}