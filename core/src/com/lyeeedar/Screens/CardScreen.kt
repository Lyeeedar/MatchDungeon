package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Board.Mote
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Quest
import com.lyeeedar.Game.QuestNode
import com.lyeeedar.GameStateFlags
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.FullscreenTable
import com.lyeeedar.UI.ScrollingTextLabel
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager

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
	val equipmentTable = Table()
	val headSlot = Table()
	val mainhandSlot = Table()
	val offhandSlot = Table()
	val bodySlot = Table()
	val playerSlot = Table()

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

	fun setup(card: Card, quest: Quest)
	{
		readyToSwitch = false

		if (!created)
		{
			baseCreate()
			created = true
		}

		playerSlot.clear()
		playerSlot.add(SpriteWidget(Global.player.baseCharacter.sprite, 32f, 32f)).grow()
		playerSlot.addClickListener {
			val table = Global.player.createTable()

			FullscreenTable.createCloseable(table)
		}

		Global.levelflags = GameStateFlags()

		currentCard = card
		currentContent = currentCard.current.getContent()
		currentQuest = quest

		mainTable.clear()

		val titleStack = Stack()
		val equipTable = Table()
		equipTable.add(equipmentTable).expandX().right()
		titleStack.add(equipTable)

		val titleTable = Table()
		titleTable.add(Label(currentCard.current.name, Global.skin, "title")).expandX().center()
		titleStack.add(titleTable)

		mainTable.add(titleStack).growX().pad(10f)
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
		advanceContent()
	}

	override fun create()
	{
		val skin = Global.skin

		text = ScrollingTextLabel("", skin)

		headSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		mainhandSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		offhandSlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))
		bodySlot.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/TileBackground"))

		equipmentTable.defaults().width(32f).height(32f)
		equipmentTable.add(Table())
		equipmentTable.add(headSlot)
		equipmentTable.add(Table())
		equipmentTable.row()
		equipmentTable.add(mainhandSlot)
		equipmentTable.add(playerSlot)
		equipmentTable.add(offhandSlot)
		equipmentTable.row()
		equipmentTable.add(Table())
		equipmentTable.add(bodySlot)
		equipmentTable.add(Table())

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
				tableSlot.addClickListener {
					val table = equip.createTable(null)

					FullscreenTable.createCloseable(table)
				}
			}
		}

		createFun(EquipmentSlot.HEAD, headSlot)
		createFun(EquipmentSlot.BODY, bodySlot)
		createFun(EquipmentSlot.MAINHAND, mainhandSlot)
		createFun(EquipmentSlot.OFFHAND, offhandSlot)
	}

	var readyToSwitch = false
	fun advanceContent()
	{
		val completed = currentContent.advance(this)
		if (completed)
		{
			val state = currentContent.state
			val key = currentContent.customKey
			if (state != CardContent.CardContentState.FAILURE)
			{
				val currentCardNode = currentCard.current
				currentCard.current = currentCardNode.nextNode?.node ?: currentCardNode
			}

			if (state == CardContent.CardContentState.INPROGRESS)
			{
				throw Exception("Failed to mark the content as complete! Offending content: " + currentCard.current.name)
			}

			val currentQuestNode = currentQuest.current as QuestNode
			currentQuest.current = currentQuestNode.getNext(state, key)

			readyToSwitch = true
		}
		updateEquipment()
	}

	override fun doRender(delta: Float)
	{
		if (readyToSwitch && stage.actors.filter { it is Mote }.count() == 0)
		{
			readyToSwitch = false

			QuestScreen.instance.updateQuest()
			Global.game.switchScreen(MainGame.ScreenEnum.QUEST)
		}
	}

	companion object
	{
		lateinit var instance: CardScreen
	}
}