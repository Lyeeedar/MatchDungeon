package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData

class CardContentActionNode : AbstractCardContentAction()
{
	lateinit var key: String
	lateinit var node: CardContentNode

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		CardContent.CardContentStack.add(CardContentNodeState(node))
		return true
	}

	override fun parse(xml: XmlData)
	{
		key = xml.text
	}

	override fun resolve(nodes: ObjectMap<String, CardContentNode>)
	{
		node = nodes[key]
	}
}