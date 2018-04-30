package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData

class CardContentActionDefine : AbstractCardContentAction()
{
	lateinit var key: String
	lateinit var value: String

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
		value = xmlData.get("Value")
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		Global.flags.flags.put(key, value.evaluate(Global.flags.flags))
		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}