package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Game.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import java.util.*

enum class Storage
{
	CARD,
	QUEST,
	GLOBAL
}

class CardContentActionDefine : AbstractCardContentAction()
{
	lateinit var key: String
	lateinit var value: String
	lateinit var storage: Storage

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key").toLowerCase(Locale.ENGLISH)
		value = xmlData.get("Value").toLowerCase(Locale.ENGLISH)
		storage = Storage.valueOf(xmlData.get("Storage").toUpperCase(Locale.ENGLISH))
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		val flags = when (storage){
			Storage.CARD -> Global.cardflags
			Storage.QUEST -> Global.questflags
			Storage.GLOBAL -> Global.globalflags
		}

		flags.flags.put(key, value.evaluate(Global.getVariableMap()))

		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}