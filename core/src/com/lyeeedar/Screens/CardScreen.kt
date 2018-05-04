package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
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
import com.lyeeedar.Util.directory

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

	var success = false

	fun setup(card: Card, quest: Quest)
	{
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
		currentContent = CardContent.load(currentCard.path.directory() + "/" + currentCard.current.content)
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

		contentTable.add(text).grow().pad(5f)
		contentTable.row()
		contentTable.add(buttonTable).pad(5f)

		mainTable.add(contentTable).grow()

		success = false

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

	fun advanceContent()
	{
		val completed = currentContent.advance(this)
		if (completed)
		{
			if (success)
			{
				val currentCardNode = currentCard.current
				currentCard.current = currentCardNode.nextNode?.node ?: currentCardNode
			}

			val currentQuestNode = currentQuest.current
			if (currentQuestNode.type == QuestNode.QuestNodeType.FIXED)
			{
				if (success)
				{
					currentQuest.current = currentQuestNode.successNode!!.node
				}
				else
				{
					currentQuest.current = currentQuestNode.failureNode!!.node
				}
			}
			else
			{
				currentQuest.current = currentQuestNode.nextNode!!.node
			}

			QuestScreen.instance.updateButtons()
			Global.game.switchScreen(MainGame.ScreenEnum.QUEST)
		}
		updateEquipment()
	}

	override fun doRender(delta: Float)
	{

	}

	companion object
	{
		lateinit var instance: CardScreen
	}
}