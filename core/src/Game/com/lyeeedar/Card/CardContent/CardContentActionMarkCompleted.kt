package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import java.util.*

class CardContentActionMarkCompleted : AbstractCardContentAction()
{
	lateinit var state: CardContent.CardContentState
	var key: String? = null

	override fun parse(xmlData: XmlData)
	{
		state = CardContent.CardContentState.valueOf(xmlData.get("State", "Success")!!.toUpperCase(Locale.ENGLISH))
		key = xmlData.get("Key", null)
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		CardContent.state = state
		CardContent.customKey = key

		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}