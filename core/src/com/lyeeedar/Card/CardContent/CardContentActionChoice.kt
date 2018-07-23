package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Global.Companion.skin
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.XmlData
import ktx.actors.then

class CardContentActionChoice : AbstractCardContentAction()
{
	val choices = Array<Choice>()

	var built = false

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (built) return false

		val responsesTable = CardContentScreen.buttonTable
		responsesTable.clear()
		responsesTable.center()

		var delay = 0f
		for (choice in choices)
		{
			val response = TextButton(choice.text, skin, "responseButton")
			response.label.setWrap(true)
			responsesTable.add(response).growX().pad(5f, 0f, 5f, 0f).height(75f)
			responsesTable.row()

			val seq = Actions.alpha(0f) then Actions.delay(delay) then Actions.fadeIn(0.3f)
			response.addAction(seq)

			delay += 0.1f

			response.addClickListener {
				responsesTable.clear()
				CardContent.CardContentStack.last().index++
				built = false

				if (!choice.key.isBlank())
				{
					CardContent.CardContentStack.add(CardContentNodeState(choice.node!!))
				}
				CardContentScreen.advanceContent()
			}
		}

		built = true
		return false
	}

	override fun parse(xml: XmlData)
	{
		val choicesEl = xml.getChildByName("Choices")!!
		for (el in choicesEl.children())
		{
			val text = el.get("Text")
			val key = el.get("Node", "")!!

			choices.add(Choice(text, key))
		}
	}

	override fun resolve(nodes: ObjectMap<String, CardContentNode>)
	{
		for (choice in choices)
		{
			if (!choice.key.isBlank())
			{
				choice.node = nodes[choice.key]
			}
		}
	}
}

data class Choice(val text: String, val key: String)
{
	var node: CardContentNode? = null
}