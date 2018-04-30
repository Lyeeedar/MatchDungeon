package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData

class CardContentActionLine : AbstractCardContentAction()
{
	lateinit var text: String

	override fun parse(xmlData: XmlData)
	{
		text = xmlData.get("MultilineString")
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		val scrollingTextLabel = CardContentScreen.text
		if (scrollingTextLabel.text.toString() == text)
		{
			if (scrollingTextLabel.isComplete)
			{
				return true
			}
			else
			{
				scrollingTextLabel.isComplete = true
				return false
			}
		}

		if (scrollingTextLabel.text.toString() != text)
		{
			scrollingTextLabel.setText(text)
		}

		return false
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}