package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Quest
import com.lyeeedar.Game.QuestNode
import com.lyeeedar.Game.Save
import com.lyeeedar.GameStateFlags
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.actors.then
import ktx.collections.toGdxArray

class CardScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	var fullScreenColour: Table? = null

	lateinit var currentCard: Card
	lateinit var currentQuest: Quest
	lateinit var currentContent: CardContent

	lateinit var text: ScrollingTextLabel
	val buttonTable = Table()
	val statsTable = Table()
	val headSlot = Table()
	val mainhandSlot = Table()
	val offhandSlot = Table()
	val bodySlot = Table()
	val playerSlot = Table()
	lateinit var goldLabel: NumberChangeLabel

	fun getSlot(slot: EquipmentSlot): Table
	{
		return when (slot)
		{
			EquipmentSlot.HEAD -> headSlot
			EquipmentSlot.OFFHAND -> offhandSlot
			EquipmentSlot.MAINHAND -> mainhandSlot
			EquipmentSlot.BODY -> bodySlot
		}
	}

	fun setup(card: Card, quest: Quest, resetLevelFlags: Boolean = true, content: CardContent? = null)
	{
		readyToSwitch = false

		if (!created)
		{
			baseCreate()
			created = true
		}

		playerSlot.clear()
		playerSlot.add(SpriteWidget(Global.player.baseCharacter.sprite, 32f, 32f)).grow()

		if (resetLevelFlags)
		{
			Global.levelflags = GameStateFlags()
		}

		currentCard = card
		currentContent = content?: currentCard.current.getContent()
		currentQuest = quest

		mainTable.clear()

		mainTable.add(statsTable).expandX().left().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX().pad(0f, 10f, 0f, 10f)
		mainTable.row()

		val contentTable = Table()
		contentTable.touchable = Touchable.enabled
		contentTable.addClickListener {
			advanceContent()
		}

		contentTable.add(text).grow().pad(5f).growX()
		contentTable.row()
		contentTable.add(buttonTable).pad(5f).growX()

		contentTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

		mainTable.add(contentTable).grow().pad(15f, 35f, 15f, 35f)
		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(quest.theme.backgroundTile))).tint(Color.DARK_GRAY)

		updateEquipment()
		advanceContent(true)
	}

	override fun create()
	{
		val skin = Global.skin

		text = ScrollingTextLabel("", skin)
		goldLabel = NumberChangeLabel("Gold: ", Global.skin)

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

		if (!Global.release)
		{
			debugConsole.register("AddGold", "", fun(args, console): Boolean
			{

				if (args.size != 1)
				{
					console.error("Invalid number of arguments! Expected 1!")
					return false
				}

				val value = args[0].toInt()

				Global.player.gold += value
				updateEquipment()

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

	fun updateEquipment()
	{
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

		goldLabel.value = Global.player.gold
	}

	var readyToSwitch = false
	fun advanceContent(canStart: Boolean = false)
	{
		if (currentContent.state != CardContent.CardContentState.INPROGRESS)
		{
			readyToSwitch = true
			return
		}

		val completed = currentContent.advance(this, canStart)
		if (completed)
		{
			val state = currentContent.state
			val key = currentContent.customKey

			var advancedCard = false
			if (state != CardContent.CardContentState.FAILURE)
			{
				val currentCardNode = currentCard.current
				val nextCardNode = currentCardNode.nextNode?.node

				if (nextCardNode != null)
				{
					currentCard.current = nextCardNode

					val prevCard = currentCardNode.getCard()
					val nextCard = nextCardNode.getCard()

					val transitionTable = Table()
					val transitionStack = Stack()
					transitionTable.add(transitionStack).grow()

					val prevDetailTable = prevCard.frontDetailTable
					val nextDetailTable = nextCard.frontDetailTable

					val prevSequence = alpha(1f) then delay(1f) then fadeOut(0.5f)
					val nextSequence = alpha(0f) then delay(1f) then fadeIn(0.5f)

					prevDetailTable.addAction(prevSequence)
					nextDetailTable.addAction(nextSequence)

					transitionStack.add(prevDetailTable)
					transitionStack.add(nextDetailTable)

					prevCard.frontTable.remove()
					val newCard = CardWidget(prevCard.frontTable, transitionTable, prevCard.backImage, null)
					newCard.setPosition(Global.resolution.x / 2f, Global.resolution.y / 2f)
					Global.stage.addActor(newCard)
					newCard.focus()
					newCard.collapseFun = {
						readyToSwitch = true
						newCard.remove()
					}

					advancedCard = true
				}
			}

			if (state == CardContent.CardContentState.INPROGRESS)
			{
				throw Exception("Failed to mark the content as complete! Offending content: " + currentCard.current.name)
			}

			val currentQuestNode = currentQuest.current as QuestNode
			currentQuest.current = currentQuestNode.getNext(state, key)

			if (!advancedCard)
			{
				readyToSwitch = true
			}
		}
		updateEquipment()

		Save.save()
	}

	override fun doRender(delta: Float)
	{
		if (readyToSwitch && stage.actors.filter { it is Mote }.count() == 0)
		{
			for (buff in Global.player.buffs.toGdxArray())
			{
				buff.remainingDuration--
				if (buff.remainingDuration <= 0)
				{
					Global.player.buffs.removeValue(buff, true)
				}
			}

			readyToSwitch = false

			goldLabel.complete()

			QuestScreen.instance.updateQuest()
			Global.game.switchScreen(MainGame.ScreenEnum.QUEST)
		}
	}

	override fun show()
	{
		super.show()
		goldLabel.complete()
	}

	companion object
	{
		lateinit var instance: CardScreen
	}
}