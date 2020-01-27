package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.Quest
import com.lyeeedar.UI.LanguageSelectorWidget
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics
import java.util.*

class NewUserScreen : AbstractScreen()
{
	val confirmButton = TextButton("", Statics.skin)
	val languageSelector = LanguageSelectorWidget(Statics.skin)

	override fun create()
	{
		confirmButton.name = "Confirm"

		val systemLanguageCode = Locale.getDefault().language
		languageSelector.selectedLanguage = languageSelector.languages.firstOrNull { it.code == systemLanguageCode } ?: languageSelector.languages[0]

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.5f, 0.5f, 0.5f, 1.0f))
		val contentTable = Table()
		contentTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/shadowborder"))

		contentTable.add(Table()).expand()
		contentTable.row()

		contentTable.add(languageSelector).growX().height(200f).pad(10f)
		contentTable.row()

		contentTable.add(confirmButton).growX().pad(10f)
		contentTable.row()

		contentTable.add(Table()).expand()
		contentTable.row()

		mainTable.add(contentTable).grow()

		confirmButton.addClickListener {
			Statics.language = languageSelector.selectedLanguage.code

			val quest = Quest.load("Intro/Intro")

			Statics.game.getTypedScreen<QuestScreen>()?.setup(quest)
			Statics.game.getTypedScreen<DeckScreen>()?.setup()
			Statics.game.getTypedScreen<QuestSelectionScreen>()?.setup()

			QuestScreen.instance.swapTo()
		}
	}

	override fun doRender(delta: Float)
	{
		confirmButton.setText(Localisation.getText("confirm", "UI", languageSelector.selectedLanguage.code))
	}
}