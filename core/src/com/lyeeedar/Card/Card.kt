package com.lyeeedar.Card

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Global
import com.lyeeedar.Rarity
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set
import ktx.collections.toGdxArray

class Card(val path: String, val nodes: Array<CardNode>, val root: CardNode)
{
	var current: CardNode = root

	companion object
	{
		fun load(path: String) : Card
		{
			val xml = getXml(path)

			val nodeMap = ObjectMap<String, CardNode>()

			val rootNode = xml.get("Root")
			val nodesEl = xml.getChildByName("Nodes")!!
			for (nodeEl in nodesEl.children())
			{
				val cardNode = CardNode()
				cardNode.parse(nodeEl)

				nodeMap[cardNode.guid] = cardNode
			}

			for (node in nodeMap.values())
			{
				node.resolve(nodeMap)
			}

			val card = Card(path, nodeMap.values().toGdxArray(), nodeMap[rootNode])
			return card
		}
	}
}

class CardNode
{
	lateinit var guid: String

	lateinit var name: String
	lateinit var description: String
	lateinit var rarity: Rarity
	lateinit var content: String

	var nextNode: CardNodeWrapper? = null

	fun getCard(): CardWidget
	{
		return CardWidget(createTable(), this)
	}

	fun createTable(): Table
	{
		val table = Table()

		val title = Label(name, Global.skin, "cardtitle")
		table.add(title).expandX().center().pad(10f, 0f, 0f, 0f)
		table.row()

		val descLabel = Label(description, Global.skin, "card")
		descLabel.setWrap(true)
		table.add(descLabel).grow().pad(0f, 10f, 0f, 10f)

		return table
	}

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		name = xmlData.get("Name")
		description = xmlData.get("Description")
		rarity = Rarity.valueOf(xmlData.get("Rarity").toUpperCase())
		content = xmlData.get("Content")

		val nextEl = xmlData.getChildByName("Next")
		if (nextEl != null) nextNode = CardNodeWrapper(nextEl.text)
	}

	fun resolve(nodeMap: ObjectMap<String, CardNode>)
	{
		nextNode?.resolve(nodeMap)
	}

	data class CardNodeWrapper(val guid: String)
	{
		lateinit var node: CardNode

		fun resolve(nodeMap: ObjectMap<String, CardNode>)
		{
			node = nodeMap[guid]
		}
	}
}