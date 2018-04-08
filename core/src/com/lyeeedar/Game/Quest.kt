package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set
import ktx.collections.toGdxArray

class Quest(val path: String, val nodes: Array<QuestNode>, val root: QuestNode)
{
	var current: QuestNode = root

	companion object
	{
		fun load(path: String) : Quest
		{
			val xml = getXml("Quests/$path")

			val nodeMap = ObjectMap<String, QuestNode>()

			val rootNode = xml.get("Root")
			val nodesEl = xml.getChildByName("Nodes")!!
			for (nodeEl in nodesEl.children())
			{
				val questNode = QuestNode()
				questNode.parse(nodeEl)

				nodeMap[questNode.guid] = questNode
			}

			for (node in nodeMap.values())
			{
				node.resolve(nodeMap)
			}

			val quest = Quest(path, nodeMap.values().toGdxArray(), nodeMap[rootNode])
			return quest
		}
	}
}

class QuestNode
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

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		type = QuestNodeType.valueOf(xmlData.get("Type"))

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