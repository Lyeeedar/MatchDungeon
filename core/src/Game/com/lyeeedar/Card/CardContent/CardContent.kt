package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set

class CardContent(val path: String)
{
	enum class CardContentState
	{
		INPROGRESS,
		SUCCESS,
		FAILURE,
		CUSTOM
	}

	var state = CardContentState.INPROGRESS
	var customKey: String? = null

	val CardContentStack = Array<CardContentNodeState>()
	lateinit var root: CardContentActionNode
	val nodes = ObjectMap<String, CardContentNode>()

	fun advance(CardContentScreen: CardScreen, canStart: Boolean): Boolean
	{
		if (CardContentStack.size == 0 && canStart)
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

			Statics.crashReporter.logDebug("Attempting to advance $action")
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
		val rootEl = xml.getChildByName("Root")!!
		val root = CardContentActionNode()
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

	fun save(kryo: Kryo, output: Output)
	{
		output.writeString(path)
		output.writeInt(state.ordinal)

		if (state == CardContentState.CUSTOM)
		{
			output.writeString(customKey)
		}

		output.writeInt(CardContentStack.size)
		for (item in CardContentStack)
		{
			output.writeString(item.node.guid)
			output.writeInt(item.index)
		}
	}

	companion object
	{
		fun load(path: String): CardContent
		{
			val CardContent = CardContent(path)
			CardContent.parse(getXml(path))
			return CardContent
		}

		fun load(kryo: Kryo, input: Input): CardContent
		{
			val path = input.readString()

			val content = load(path)
			content.state = CardContentState.values()[input.readInt()]

			if (content.state == CardContentState.CUSTOM)
			{
				content.customKey = input.readString()
			}

			val stackSize = input.readInt()
			for (i in 0 until stackSize)
			{
				val guid = input.readString()
				val index = input.readInt()

				val node = content.nodes[guid]

				val item = CardContentNodeState(node)
				item.index = index
				content.CardContentStack.add(item)
			}

			return content
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