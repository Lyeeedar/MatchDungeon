package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.TextureDrawable
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData
import ktx.actors.then

class CardContentActionFadeOut : AbstractCardContentAction()
{
	lateinit var colour: Colour
	var duration: Float = 0f

	val flashTexture = AssetManager.loadTexture("Sprites/white.png")!!

	var allowAdvance = false
	var awaitingCompletion = false

	override fun parse(xmlData: XmlData)
	{
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
		duration = xmlData.getFloat("Duration")
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (allowAdvance)
		{
			awaitingCompletion = false
			allowAdvance = false
			return true
		}

		if (awaitingCompletion)
		{
			return false
		}

		val table = Table()
		table.background = TextureDrawable(flashTexture).tint(colour.color())
		val sequence = Actions.alpha(0f) then Actions.fadeIn(duration) then lambda { allowAdvance = true;  CardContentScreen.advanceContent() }
		table.addAction(sequence)
		table.setFillParent(true)

		CardContentScreen.stage.addActor(table)
		CardContentScreen.fullScreenColour = table

		awaitingCompletion = true
		return false
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}