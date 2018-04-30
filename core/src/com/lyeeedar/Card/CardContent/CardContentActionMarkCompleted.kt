package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData

class CardContentActionMarkCompleted : AbstractCardContentAction()
{
	override fun parse(xmlData: XmlData)
	{

	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		CardContentScreen.success = true
		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}