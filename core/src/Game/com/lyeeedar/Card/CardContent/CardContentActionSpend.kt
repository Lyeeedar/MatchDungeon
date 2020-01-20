package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.Util.XmlData
import java.util.*
import kotlin.math.max

class CardContentActionSpend : AbstractCardContentAction()
{
	lateinit var key: String
	lateinit var countEqn: String

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
		countEqn = xmlData.get("Count").toLowerCase(Locale.ENGLISH)
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		val count = countEqn.evaluate(Global.getVariableMap())

		if (key == "Money")
		{
			Global.player.gold = max(0, Global.player.gold - count.toInt())
		}

		if (EquipmentSlot.Values.map { it.toString() }.contains(key.toUpperCase(Locale.ENGLISH)))
		{
			val slot = EquipmentSlot.valueOf(key.toUpperCase(Locale.ENGLISH))
			Global.player.equipment[slot] = null
		}

		if (Statistic.Values.map { it.toString() }.contains(key.toUpperCase(Locale.ENGLISH)))
		{
			val slot = Statistic.valueOf(key.toUpperCase(Locale.ENGLISH))
			Global.player.statistics[slot] = max(0f, (Global.player.statistics[slot] ?: 0f) - count)
		}

		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}