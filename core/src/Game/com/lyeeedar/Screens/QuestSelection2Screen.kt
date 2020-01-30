package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Game.Region
import com.lyeeedar.UI.RegionWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics

class QuestSelection2Screen : AbstractScreen()
{
	val titleLabel = Label("", Statics.skin, "title")

	val scrollTable = Table()
	val editButton = TextButton("", Statics.skin)

	lateinit var leftButton: Button
	lateinit var rightButton: Button

	lateinit var leftNewLabel: Label
	lateinit var rightNewLabel: Label

	var regions = Array<Region>()
	var currentRegion: Region? = null

	override fun create()
	{

	}

	fun setup()
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		editButton.setText(Localisation.getText("questselection.editdeck", "UI"))
		editButton.name = "EditDeck"

		mainTable.clear()
		scrollTable.clear()

		leftButton = Button(Statics.skin, "left")
		leftButton.addClickListener {
			var index = regions.indexOf(currentRegion)
			if (index > 0)
			{
				index--
				currentRegion = regions[index]
			}
		}

		val leftNew = Table()
		leftNewLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
		leftNewLabel.touchable = Touchable.disabled
		leftNew.add(leftNewLabel).expand().left().top().pad(3f)

		val rightNew = Table()
		rightNewLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
		rightNewLabel.touchable = Touchable.disabled
		rightNew.add(rightNewLabel).expand().left().top().pad(3f)

		rightButton = Button(Statics.skin, "right")
		rightButton.addClickListener {
			var index = regions.indexOf(currentRegion)
			if (index < regions.size-1)
			{
				index++
				currentRegion = regions[index]
			}
		}

		regions = Region.regions
		currentRegion = regions[0]

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

		val widget = RegionWidget(currentRegion!!)

		val regionTable = Table()
		regionTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/shadowborder"))
		regionTable.add(widget).grow()
		mainTable.add(regionTable).grow()
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

//		if (Global.deck.hasNewEquipment || Global.deck.hasNewEncounters || Global.deck.hasNewCharacters)
//		{
//			val newTable = Table()
//			val newLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
//			newTable.add(newLabel).expand().left().top().pad(3f)
//
//			newLabel.touchable = Touchable.disabled
//
//			editStack.add(newTable)
//		}

		val editTable = Table()
		editTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))
		editTable.add(editStack).expand().right().bottom()

		mainTable.add(editTable).growX().right().bottom()
		mainTable.row()
	}

	override fun doRender(delta: Float)
	{
		if (currentRegion == null)
		{
			setup()
		}
	}
}