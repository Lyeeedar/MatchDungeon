package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.GameStateFlags
import com.lyeeedar.Game.Global
import com.lyeeedar.Game.Quest
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.*
import ktx.collections.set
import java.util.*

class QuestSelectionScreen : AbstractScreen()
{
	init
	{
		instance = this
	}

	override fun create()
	{
		if (!Statics.release)
		{
			debugConsole.register("loadquest", "", fun(args, console): Boolean
			{
				if (args.size != 1)
				{
					console.error("Invalid number of arguments! Expected 1!")
					return false
				}

				val questPath = XmlData.enumeratePaths("Quests", "Quest").firstOrNull { it.toLowerCase(Locale.ENGLISH).contains(args[0].toLowerCase(Locale.ENGLISH)) }
				if (questPath == null)
				{
					console.error("Could not ind quest!")
					return false
				}

				val quest = Quest.load(questPath.replace("Quests/", ""))

				val screen = Statics.game.getTypedScreen<QuestScreen>()!!
				quest.current = quest.root
				quest.currentTheme = quest.theme
				quest.state = Quest.QuestState.INPROGRESS
				Global.player = Global.deck.getPlayer()
				Global.questflags = GameStateFlags()
				screen.setup(quest)
				screen.swapTo()

				return true
			})

		}
	}

	val titleLabel = Label("", Statics.skin, "title")

	val scrollTable = Table()
	val editButton = TextButton("Edit Deck", Statics.skin)

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

		leftButton = Button(Statics.skin, "left")
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
		leftNewLabel = Label("New", Statics.skin)
		leftNewLabel.touchable = Touchable.disabled
		leftNew.add(leftNewLabel).expand().left().top().pad(3f)

		val rightNew = Table()
		rightNewLabel = Label("New", Statics.skin)
		rightNewLabel.touchable = Touchable.disabled
		rightNew.add(rightNewLabel).expand().left().top().pad(3f)

		rightButton = Button(Statics.skin, "right")
		rightButton.addClickListener {
			var index = themeOrder.indexOf(currentTheme)
			if (index < themeOrder.size-1)
			{
				index++
				currentTheme = themeOrder[index]
				updateQuestsTable()
			}
		}

		val scrollPane = ScrollPane(scrollTable, Statics.skin)
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
		titleTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))
		titleTable.add(leftButtonStack).expandY().center().pad(20f)
		titleTable.add(titleLabel).expand().center()
		titleTable.add(rightButtonStack).expandY().center().pad(20f)

		mainTable.add(titleTable).growX()
		mainTable.row()

		val scrollPaneTable = Table()
		scrollPaneTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/shadowborder"))
		scrollPaneTable.add(scrollPane).grow()
		mainTable.add(scrollPaneTable).grow()
		mainTable.row()

		val editButtonTable = Table()
		editButtonTable.add(editButton).pad(10f)

		val editStack = Stack()
		editStack.add(editButtonTable)

		editButton.addClickListener {
			val screen = Statics.game.getTypedScreen<DeckScreen>()!!
			screen.setup()
			screen.swapTo()
		}

		if (Global.deck.hasNewEquipment || Global.deck.hasNewEncounters || Global.deck.hasNewCharacters)
		{
			val newTable = Table()
			val newLabel = Label("New", Statics.skin)
			newTable.add(newLabel).expand().left().top().pad(3f)

			newLabel.touchable = Touchable.disabled

			editStack.add(newTable)
		}

		val editTable = Table()
		editTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))
		editTable.add(editStack).expand().right().bottom()

		mainTable.add(editTable).growX().right().bottom()
		mainTable.row()

		updateQuestsTable()

		if (!Statics.settings.get("CompletedIntro", false))
		{
			Statics.analytics.tutorialEnd()
		}
		Statics.settings.set("CompletedIntro", true)
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
			for (i in currentIndex + 1 until themeOrder.size)
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

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(themeMap[currentTheme].first().theme.backgroundTile))).tint(Color(0.5f, 0.5f, 0.5f, 1.0f))

		scrollTable.clear()

		val cardHeight = (Statics.resolution.y.toFloat() * 0.7f) * 0.3f
		val cardWidth = Statics.resolution.x.toFloat() * 0.8f
		for (quest in themeMap[currentTheme])
		{
			val card = quest.getCard()
			card.addPick("Embark") {
				Statics.logger.logDebug("Embarking on quest ${quest.title}")

				val bundle = Statics.analytics.getParamBundle()
				bundle.setString("quest_name", quest.title)
				Statics.analytics.customEvent("start_quest", bundle)

				val screen = Statics.game.getTypedScreen<QuestScreen>()!!
				quest.resetCards()
				quest.current = quest.root
				quest.currentTheme = quest.theme
				quest.state = Quest.QuestState.INPROGRESS
				Global.player = Global.deck.getPlayer()
				Global.questflags = GameStateFlags()
				screen.setup(quest)
				screen.swapTo()
			}
			card.setSize(cardWidth, cardHeight)
			card.setFacing(faceup = true, animate = false)

			scrollTable.add(card).width(cardWidth).height(cardHeight).expandX().center().pad(15f)
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
