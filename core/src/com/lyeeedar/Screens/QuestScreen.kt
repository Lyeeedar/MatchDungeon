package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Card.CardNode
import com.lyeeedar.Game.*
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.actors.then
import ktx.collections.toGdxArray

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
		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)

		goldLabel = Label("Gold: 0", Global.skin)

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
			val table = Global.player.createTable()

			FullscreenTable.createCard(table, statsTable.localToStageCoordinates(Vector2()))
		}

		questProgressWidget = QuestProgressWidget()

		mainTable.add(statsTable).expandX().left().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX().pad(0f, 10f, 0f, 10f)
		mainTable.row()

		mainTable.add(questProgressWidget).growX().height(20f).pad(2f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX().pad(0f, 10f, 0f, 10f)
		mainTable.row()

		mainTable.add(cardsTable).grow()

		if (!Global.release)
		{
			debugConsole.register("LoadCard", "LoadCard cardName", fun(args, console): Boolean
			{
				if (args.isEmpty())
				{
					console.error("Invalid number of arguments!")
					return false
				}

				var card = Global.deck.encounters.backingArray.firstOrNull { it.current.name.toLowerCase() == args[0].toLowerCase() }
				if (card == null)
				{
					val cardPath = XmlData.existingPaths!!.firstOrNull { it.toLowerCase().endsWith(args[0].toLowerCase() + ".xml") }
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
						card = Card("", nodes, node)
					}
					else
					{
						card = Card.load(cardPath)
					}
				}

				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Global.game.switchScreen(MainGame.ScreenEnum.CARD)

				Global.player.deck.encounters.removeValue(card, true)
				currentQuest.questCards.removeValue(card, true)

				return true
			})

			debugConsole.register("CompleteQuest", "", fun(args, console): Boolean
			{
				if (args[0].toLowerCase() == "gold")
				{
					currentQuest.state = Quest.QuestState.GOLD
					completeQuest()
				}
				else if (args[0].toLowerCase() == "silver")
				{
					currentQuest.state = Quest.QuestState.SILVER
					completeQuest()
				}
				else if (args[0].toLowerCase() == "bronze")
				{
					currentQuest.state = Quest.QuestState.BRONZE
					completeQuest()
				}
				else if (args[0].toLowerCase() == "fail")
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
				var equipment = Global.deck.equipment.firstOrNull { it.path.toLowerCase().endsWith(equipmentName.toLowerCase()) || it.name.toLowerCase() == equipmentName.toLowerCase() }
				if (equipment == null)
				{
					val equipmentPath = XmlData.existingPaths!!.firstOrNull { it.toLowerCase().endsWith(equipmentName.toLowerCase() + ".xml") }
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
		quest.currentTheme = quest.theme

		questProgressWidget.quest = quest

		currentQuest = quest

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(quest.theme.backgroundTile))).tint(Color.DARK_GRAY)

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

			widget.addPick("Choose", {
				chosenQuestCard = it

				for (w in cardWidgets)
				{
					w.clickable = false
				}
			})

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

			val equip = Global.player.getEquipment(slot)
			if (equip != null)
			{
				val widget = SpriteWidget(equip.icon, 32f, 32f)
				tableSlot.add(widget).grow()
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
		chosenQuestCard = null

		stage.addActor(greyOutTable)

		val table = Table()

		val text = if (currentQuest.state == Quest.QuestState.FAILURE) "Quest Failed!" else "Quest Completed!\n${currentQuest.state}"
		table.add(Label(text, Global.skin, "cardtitle"))

		val card = CardWidget(table, Table(), AssetManager.loadTextureRegion("blank")!!, null)
		card.canZoom = false
		card.setFacing(true, false)
		card.addPick("", {
			card.remove()
			currentGroup.clear()
			updateRewards()
		})

		currentGroup.add(card)

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

	var grouped: Array<Array<AbstractReward>> = Array()
	var currentGroup = Array<CardWidget>()
	var shownIntro = false
	val greyOutTable = Table()
	val questunlocks = Array<CardWidget>()
	fun updateRewards()
	{
		if (currentGroup.size == 0)
		{
			if (questunlocks.size > 0)
			{
				while (questunlocks.size > 0)
				{
					val card = questunlocks.removeIndex(questunlocks.size-1)
					card.canZoom = false
					card.setFacing(true, false)
					card.pickFuns.clear()
					card.addPick("", {
						currentGroup.removeValue(card, true)
						if (currentGroup.size == 0)
						{
							updateRewards()
						}

						card.remove()
					})

					currentGroup.add(card)
					Global.stage.addActor(card)

					if (currentGroup.size == 4)
					{
						break
					}
				}

				if (currentGroup.size > 0)
				{
					CardWidget.layoutCards(currentGroup, Direction.CENTER)
				}
				else
				{
					updateRewards()
				}
			}
			else if (grouped.size == 0)
			{
				val bronze = currentQuest.bronzeRewards.filter { it.isValid() }.toGdxArray()
				val silver = currentQuest.silverRewards.filter { it.isValid() }.toGdxArray()
				val gold = currentQuest.goldRewards.filter { it.isValid() }.toGdxArray()

				val spawnIntroCard = fun(name:String, icon:Sprite) {
					val table = Table()
					table.add(Label(name, Global.skin, "cardtitle"))
					table.row()
					table.add(SpriteWidget(icon, 64f, 64f)).expandX().center()

					val card = CardWidget(table, Table(), AssetManager.loadTextureRegion("white")!!, null)
					card.canZoom = false
					card.setFacing(true, false)
					card.addPick("", {
						card.remove()
						updateRewards()
					})

					val cards = Array<CardWidget>()
					cards.add(card)

					Global.stage.addActor(card)
					CardWidget.layoutCards(cards, Direction.CENTER, cardsTable, animate = false)
				}

				if (Global.deck.newcharacters.size > 0 || Global.deck.newencounters.size > 0 || Global.deck.newequipment.size > 0 || Global.deck.newquests.size > 0)
				{
					if (!shownIntro)
					{
						shownIntro = true

						spawnIntroCard("Quest Unlocks", AssetManager.loadSprite("blank", drawActualSize =  true))
					}
					else
					{
						shownIntro = false

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

						updateRewards()
						return
					}
				}
				else if (currentQuest.state.ordinal >= Quest.QuestState.BRONZE.ordinal && !currentQuest.gotBronze && bronze.size > 0)
				{
					if (!shownIntro)
					{
						shownIntro = true

						spawnIntroCard("Bronze Reward", AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_copper", drawActualSize =  true))
					}
					else
					{
						shownIntro = false
						grouped = bronze.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
						currentQuest.gotBronze = true
					}
				}
				else if (currentQuest.state.ordinal >= Quest.QuestState.SILVER.ordinal && !currentQuest.gotSilver && silver.size > 0)
				{
					if (!shownIntro)
					{
						shownIntro = true

						spawnIntroCard("Silver Reward", AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_silver", drawActualSize =  true))
					}
					else
					{
						shownIntro = false
						grouped = silver.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
						currentQuest.gotSilver = true
					}
				}
				else if (currentQuest.state.ordinal >= Quest.QuestState.GOLD.ordinal && !currentQuest.gotGold && gold.size > 0)
				{
					if (!shownIntro)
					{
						shownIntro = true

						spawnIntroCard("Gold Reward", AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold", drawActualSize =  true))
					}
					else
					{
						shownIntro = false
						grouped = gold.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
						currentQuest.gotGold = true
					}
				}
				else
				{
					needsAdvance = true
				}
			}

			if (grouped.size > 0)
			{
				val chosen = grouped.removeIndex(0)
				currentGroup = chosen.flatMap { it.reward() }.filter { it != null }.map { it!! }.toGdxArray()

				for (card in currentGroup)
				{
					card.canZoom = false
					card.pickFuns.clear()
					card.addPick("", {
						currentGroup.removeValue(card, true)
						if (currentGroup.size == 0)
						{
							Global.deck.newcharacters.clear()
							Global.deck.newencounters.clear()
							Global.deck.newequipment.clear()
							Global.deck.newquests.clear()
							updateRewards()
						}

						card.remove()
					})

					for (pick in card.pickFuns)
					{
						val oldFun = pick.pickFun
						pick.pickFun = {
							oldFun(it)

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
					updateRewards()
				}
			}
		}
	}

	var needsAdvance = false
	override fun doRender(delta: Float)
	{
		if (needsAdvance && Mote.motes.size == 0)
		{
			needsAdvance = false

			QuestSelectionScreen.instance.setup()
			QuestSelectionScreen.instance.swapTo()
			greyOutTable.remove()
		}

		if (needsLayout && cardsTable.width != 0f)
		{
			CardWidget.layoutCards(cardWidgets, Direction.CENTER, cardsTable)
			updateEquipment()
			needsLayout = false
		}

		if (chosenQuestCard != null && chosenQuestCard!!.actions.size == 0)
		{
			for (widget in cardWidgets)
			{
				if (widget != chosenQuestCard)
				{
					val sequence = fadeOut(0.3f) then removeActor()
					widget.addAction(sequence)
				}
			}

			val sequence = delay(0.5f) then lambda {
				val card = chosenQuestCard!!.data as Card

				val cardScreen = CardScreen.instance
				cardScreen.setup(card, currentQuest)
				Global.game.switchScreen(MainGame.ScreenEnum.CARD)

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