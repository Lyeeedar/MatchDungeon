package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.XmlData
import ktx.actors.then

class CardContentActionClearFadeOut : AbstractCardContentAction()
{
	var duration: Float = 0f

	var allowAdvance = false
	var awaitingCompletion = false

	override fun parse(xmlData: XmlData)
	{
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

		val table = CardContentScreen.fullScreenColour ?: return true

		val sequence = Actions.fadeOut(duration) then lambda { allowAdvance = true; CardContentScreen.advanceContent() } then Actions.removeActor()
		table.addAction(sequence)

		CardContentScreen.fullScreenColour = null

		awaitingCompletion = true
		return false
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}