package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Theme
import com.lyeeedar.Card.Card
import com.lyeeedar.Global
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class Quest(val path: String)
{
	val nodes: Array<QuestNode>
	val root: QuestNode
	val questCards: Array<Card> = Array()

	lateinit var current: QuestNode

	val theme: Theme

	init
	{
		val rawPath = "Quests/$path"
		val xml = getXml(rawPath)

		theme = Theme.Companion.load("Themes/" + xml.get("Theme"))

		val questCardsEl = xml.getChildByName("QuestCards")
		if (questCardsEl != null)
		{
			for (questCardEl in questCardsEl.children())
			{
				val card = Card.load(rawPath.directory() + "/" + questCardEl.text)
				questCards.add(card)
			}
		}

		val nodeMap = ObjectMap<String, QuestNode>()

		val rootNode = xml.get("Root")
		val nodesEl = xml.getChildByName("Nodes")!!
		for (nodeEl in nodesEl.children())
		{
			val questNode = QuestNode(this)
			questNode.parse(nodeEl)

			nodeMap[questNode.guid] = questNode
		}

		for (node in nodeMap.values())
		{
			node.resolve(nodeMap)
		}

		nodes = nodeMap.values().toGdxArray()
		root = nodeMap[rootNode]

		current = root
	}

	companion object
	{
		fun load(path: String) : Quest
		{
			val quest = Quest(path)


			return quest
		}
	}
}

class QuestNode(val quest: Quest)
{
	enum class QuestNodeType
	{
		FIXED,
		DECK
	}

	enum class CompletionState
	{
		NONE,
		SUCCESS,
		FAILURE
	}

	lateinit var guid: String

	var type: QuestNodeType = QuestNodeType.FIXED
	lateinit var fixedEventString: String
	var allowDeckCards = false
	var allowQuestCards = false

	var nextNode: QuestNodeWrapper? = null
	var successNode: QuestNodeWrapper? = null
	var failureNode: QuestNodeWrapper? = null

	var state: CompletionState = CompletionState.NONE

	fun getCards(): Array<Card>
	{
		val output = Array<Card>()

		if (type == QuestNodeType.FIXED)
		{
			output.add(Card.load(("Quests/" + quest.path).directory() + "/" + fixedEventString))
		}
		else
		{
			val pool = Array<Card>()
			if (allowDeckCards)
			{
				pool.addAll(Global.player.deck.encounters)
			}
			if (allowQuestCards)
			{
				pool.addAll(quest.questCards)
			}

			for (i in 0 until 4)
			{
				output.add(pool.removeRandom(Random.random))
			}
		}

		return output
	}

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		type = QuestNodeType.valueOf(xmlData.get("Type").toUpperCase())

		if (type == QuestNodeType.FIXED)
		{
			fixedEventString = xmlData.get("FixedEvent")

			val successEl = xmlData.getChildByName("Success")
			if (successEl != null) successNode = QuestNodeWrapper(successEl.text)

			val failureEl = xmlData.getChildByName("Failure")
			if (failureEl != null) failureNode = QuestNodeWrapper(failureEl.text)
		}
		else
		{
			allowDeckCards = xmlData.getBoolean("AllowDeckCards", false)
			allowQuestCards = xmlData.getBoolean("AllowQuestCards", false)

			val nextEl = xmlData.getChildByName("Next")
			if (nextEl != null) nextNode = QuestNodeWrapper(nextEl.text)
		}
	}

	fun resolve(nodeMap: ObjectMap<String, QuestNode>)
	{
		nextNode?.resolve(nodeMap)
		successNode?.resolve(nodeMap)
		failureNode?.resolve(nodeMap)
	}

	data class QuestNodeWrapper(val guid: String)
	{
		lateinit var node: QuestNode

		fun resolve(nodeMap: ObjectMap<String, QuestNode>)
		{
			node = nodeMap[guid]
		}
	}
}