package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.isEmpty
import ktx.collections.set

class CardContent
{
	val CardContentStack = Array<CardContentNodeState>()
	lateinit var root: CardContentActionBranch
	val nodes = ObjectMap<String, CardContentNode>()

	fun advance(CardContentScreen: CardScreen): Boolean
	{
		if (CardContentStack.size == 0)
		{
			root.advance(this, CardContentScreen)
		}

		while (true)
		{
			if (CardContentStack.size == 0) break
			val current = CardContentStack.last()
			if (current.index == current.node.actions.size)
			{
				CardContentStack.removeValue(current, true)
				continue
			}

			val action = current.node.actions[current.index]

			val advance = action.advance(this, CardContentScreen)
			if (advance)
			{
				current.index++
				if (current.index == current.node.actions.size)
				{
					CardContentStack.removeValue(current, true)
				}
			}
			else
			{
				break
			}
		}

		return CardContentStack.isEmpty()
	}

	fun parse(xml: XmlData)
	{
		val rootEl = xml.getChildByName("Branch")!!
		val root = CardContentActionBranch()
		root.parse(rootEl)
		this.root = root

		val nodesEl = xml.getChildByName("Nodes")!!

		for (el in nodesEl.children())
		{
			val node = CardContentNode()
			node.parse(el)

			nodes[node.guid] = node
		}

		root.resolve(nodes)
		for (node in nodes.values())
		{
			node.resolve(nodes)
		}
	}

	companion object
	{
		fun load(path: String): CardContent
		{
			val CardContent = CardContent()
			CardContent.parse(getXml(path))
			return CardContent
		}
	}
}

data class CardContentNodeState(val node: CardContentNode)
{
	var index = 0 // the action to be executed next
}

class CardContentNode
{
	lateinit var guid: String
	val actions = Array<AbstractCardContentAction>()

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")
		for (child in xmlData.children)
		{
			actions.add(AbstractCardContentAction.load(child))
		}
	}

	fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{
		for (action in actions)
		{
			action.resolve(nodeMap)
		}
	}
}