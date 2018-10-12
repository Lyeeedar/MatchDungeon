package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Quest
import com.lyeeedar.Global
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.addSpaces
import com.lyeeedar.Util.filename
import ktx.collections.set

class QuestSelectionScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	override fun create()
	{

	}

	val titleLabel = Label("", Global.skin, "title")

	val scrollTable = Table()
	val editButton = TextButton("Edit Deck", Global.skin)

	lateinit var leftButton: Button
	lateinit var rightButton: Button

	lateinit var leftNewLabel: Label
	lateinit var rightNewLabel: Label

	val themeOrder = Array<String>()
	val themeMap = ObjectMap<String, Array<Quest>>()
	var currentTheme: String = ""

	fun setup()
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		mainTable.clear()
		scrollTable.clear()

		leftButton = Button(Global.skin, "left")
		leftButton.addClickListener {
			var index = themeOrder.indexOf(currentTheme)
			if (index > 0)
			{
				index--
				currentTheme = themeOrder[index]
				updateQuestsTable()
			}
		}

		val leftNew = Table()
		leftNewLabel = Label("New", Global.skin)
		leftNewLabel.touchable = Touchable.disabled
		leftNew.add(leftNewLabel).expand().left().top().pad(3f)

		val rightNew = Table()
		rightNewLabel = Label("New", Global.skin)
		rightNewLabel.touchable = Touchable.disabled
		rightNew.add(rightNewLabel).expand().left().top().pad(3f)

		rightButton = Button(Global.skin, "right")
		rightButton.addClickListener {
			var index = themeOrder.indexOf(currentTheme)
			if (index < themeOrder.size-1)
			{
				index++
				currentTheme = themeOrder[index]
				updateQuestsTable()
			}
		}

		val scrollPane = ScrollPane(scrollTable, Global.skin)
		scrollPane.setFadeScrollBars(false)
		scrollPane.setScrollingDisabled(true, false)
		scrollPane.setForceScroll(false, true)

		themeOrder.clear()
		themeMap.clear()

		for (quest in Global.deck.quests)
		{
			val themeName = quest.theme.path.filename(false)
			if (themeMap.containsKey(themeName))
			{
				themeMap[themeName].add(quest)
			}
			else
			{
				themeOrder.add(themeName)

				val array = Array<Quest>()
				themeMap[themeName] = array

				array.add(quest)
			}
		}

		if (!themeOrder.contains(currentTheme))
		{
			currentTheme = themeOrder.firstOrNull() ?: ""
		}

		val leftButtonTable = Table()
		leftButtonTable.add(leftButton).size(32f).pad(10f)

		val rightButtonTable = Table()
		rightButtonTable.add(rightButton).size(32f).pad(10f)

		val leftButtonStack = Stack()
		leftButtonStack.add(leftButtonTable)
		leftButtonStack.add(leftNew)

		val rightButtonStack = Stack()
		rightButtonStack.add(rightButtonTable)
		rightButtonStack.add(rightNew)

		val titleTable = Table()
		titleTable.add(leftButtonStack).expandY().center().pad(20f)
		titleTable.add(titleLabel).expand().center()
		titleTable.add(rightButtonStack).expandY().center().pad(20f)

		mainTable.add(titleTable).growX().padTop(20f)
		mainTable.row()
		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		mainTable.add(scrollPane).growX().expandY().bottom()
		mainTable.row()

		val editButtonTable = Table()
		editButtonTable.add(editButton).pad(10f)

		val editStack = Stack()
		editStack.add(editButtonTable)

		editButton.addClickListener {
			val screen = Global.game.getTypedScreen<DeckScreen>()!!
			screen.setup()
			screen.swapTo()
		}

		if (Global.deck.hasNewEquipment || Global.deck.hasNewEncounters || Global.deck.hasNewCharacters)
		{
			val newTable = Table()
			val newLabel = Label("New", Global.skin)
			newTable.add(newLabel).expand().left().top().pad(3f)

			newLabel.touchable = Touchable.disabled

			editStack.add(newTable)
		}

		mainTable.add(editStack).expand().right().bottom()
		mainTable.row()

		updateQuestsTable()
	}

	fun updateQuestsTable()
	{
		if (currentTheme.isBlank()) { return }

		leftButton.color = Color.WHITE
		rightButton.color = Color.WHITE

		leftNewLabel.isVisible = false
		rightNewLabel.isVisible = false
		leftNewLabel.color = Color(0)
		rightNewLabel.color = Color(0)

		val currentIndex = themeOrder.indexOf(currentTheme)

		if (currentTheme == themeOrder.first())
		{
			leftButton.color = Color.DARK_GRAY
		}
		else
		{
			for (i in currentIndex - 1 downTo 0)
			{
				val theme = themeOrder[i]
				val quests = themeMap[theme]

				if (quests.any { !it.played })
				{
					leftNewLabel.isVisible = true
					leftNewLabel.color = Color.WHITE
					break
				}
			}
		}

		if (currentTheme == themeOrder.last())
		{
			rightButton.color = Color.DARK_GRAY
		}
		else
		{
			for (i in currentIndex until themeOrder.size)
			{
				val theme = themeOrder[i]
				val quests = themeMap[theme]

				if (quests.any{ !it.played })
				{
					rightNewLabel.isVisible = true
					rightNewLabel.color = Color.WHITE
					break
				}
			}
		}

		titleLabel.setText(currentTheme.addSpaces())

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(themeMap[currentTheme].first().theme.backgroundTile))).tint(Color.DARK_GRAY)

		scrollTable.clear()

		val cardHeight = (Global.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Global.resolution.x.toFloat() * 0.8f
		for (quest in themeMap[currentTheme])
		{
			val card = quest.getCard()
			card.addPick("Embark", {
				val screen = Global.game.getTypedScreen<QuestScreen>()!!
				quest.current = quest.root
				quest.state = Quest.QuestState.INPROGRESS
				Global.player = Global.deck.getPlayer()
				screen.setup(quest)
				screen.swapTo()
			})
			card.setSize(cardWidth, cardHeight)
			card.setFacing(true, false)

			scrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center()
			scrollTable.row()
		}
	}

	override fun show()
	{
		super.show()

		val tutorial = Tutorial("QuestSelection")
		tutorial.addDelay(1f)
		tutorial.addPopup("This is the quest selection screen.", Any())
		tutorial.addPopup("You can pick a quest from those you have unlocked here. Just tap the card you want, then tap 'embark'.", scrollTable)
		tutorial.addPopup("You can edit your deck of cards here. This will allow you to customise your quest experience.", editButton)
		tutorial.show()
	}

	override fun doRender(delta: Float)
	{

	}

	companion object
	{
		lateinit var instance: QuestSelectionScreen
	}
}