package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData

class CardContentActionRewards : AbstractCardContentAction()
{
	lateinit var amount: String

	override fun parse(xmlData: XmlData)
	{
		amount = xmlData.get("Amount", "1")!!
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		val trueAmount = amount.evaluate(Global.flags).toInt()
		Global.player.gold += trueAmount

		return true
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}