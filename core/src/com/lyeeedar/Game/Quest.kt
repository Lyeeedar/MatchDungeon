package com.lyeeedar.Game

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.Theme
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Global
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class Quest(val path: String)
{
	enum class QuestState
	{
		INPROGRESS,
		SUCCESS,
		FAILURE
	}

	var state = QuestState.INPROGRESS

	val nodes: Array<AbstractQuestNode>
	val root: AbstractQuestNode
	val questCards: Array<Card> = Array()

	var current: AbstractQuestNode? = null

	val theme: Theme

	val rewards = Array<AbstractReward>()

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

		val rewardsEl = xml.getChildByName("Rewards")
		if (rewardsEl != null)
		{
			for (el in rewardsEl.children)
			{
				rewards.add(AbstractReward.load(el))
			}
		}

		val nodeMap = ObjectMap<String, AbstractQuestNode>()

		val rootNode = xml.get("Root")
		val nodesEl = xml.getChildByName("Nodes")!!
		for (nodeEl in nodesEl.children())
		{
			val questNode = AbstractQuestNode.parse(nodeEl, this)
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

	fun getCard(): CardWidget
	{
		return CardWidget(Table(), Table(), AssetManager.loadTextureRegion("GUI/QuestCardback")!!, this)
	}

	fun run()
	{
		current = current?.run()
		if (current == null && state == QuestState.INPROGRESS)
		{
			state = QuestState.FAILURE
		}
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

abstract class AbstractQuestNode(val quest: Quest, val guid: String)
{
	abstract fun parse(xmlData: XmlData)
	abstract fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
	abstract fun run(): QuestNode?

	companion object
	{
		fun parse(xmlData: XmlData, quest: Quest): AbstractQuestNode
		{
			val guid = xmlData.getAttribute("GUID")

			val node = when (xmlData.name.toUpperCase())
			{
				"QUESTNODE" -> QuestNode(quest, guid)
				"BRANCH" -> Branch(quest, guid)
				"COMPLETEQUEST" -> CompleteQuest(quest, guid)
				else -> throw Exception("Unknown quest node type '" + xmlData.name + "'!")
			}

			node.parse(xmlData)

			return node
		}
	}
}

class QuestNode(quest: Quest, guid: String) : AbstractQuestNode(quest, guid)
{
	enum class QuestNodeType
	{
		FIXED,
		DECK
	}

	var type: QuestNodeType = QuestNodeType.FIXED
	lateinit var fixedEventString: String
	var allowDeckCards = false
	var allowQuestCards = false

	var successNode: QuestNodeWrapper? = null
	var failureNode: QuestNodeWrapper? = null
	val customNodes = Array<QuestNodeWrapper>()

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
				if (pool.size == 0)
				{
					break
				}
			}
		}

		return output
	}

	override fun parse(xmlData: XmlData)
	{
		type = QuestNodeType.valueOf(xmlData.get("Type").toUpperCase())

		if (type == QuestNodeType.FIXED)
		{
			fixedEventString = xmlData.get("FixedEvent")
		}
		else
		{
			allowDeckCards = xmlData.getBoolean("AllowDeckCards", false)
			allowQuestCards = xmlData.getBoolean("AllowQuestCards", false)
		}

		val successEl = xmlData.getChildByName("Success")
		if (successEl != null) successNode = QuestNodeWrapper(successEl.text)

		val failureEl = xmlData.getChildByName("Failure")
		if (failureEl != null) failureNode = QuestNodeWrapper(failureEl.text)

		val customEls = xmlData.getChildByName("Custom")
		if (customEls != null)
		{
			for (el in customEls.children())
			{
				val key = el.get("Key")
				val guid = el.get("Node")

				customNodes.add(QuestNodeWrapper(guid, key))
			}
		}
	}

	override fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
	{
		successNode?.resolve(nodeMap)
		failureNode?.resolve(nodeMap)
		for (custom in customNodes)
		{
			custom.resolve(nodeMap)
		}
	}

	override fun run(): QuestNode?
	{
		return this
	}

	fun getNext(state: CardContent.CardContentState, key: String?): AbstractQuestNode?
	{
		if (state == CardContent.CardContentState.SUCCESS)
		{
			return successNode?.node
		}
		else if (state == CardContent.CardContentState.FAILURE)
		{
			return failureNode?.node
		}
		else if (state == CardContent.CardContentState.CUSTOM && key != null)
		{
			for (custom in customNodes)
			{
				if (custom.key!!.toUpperCase() == key.toUpperCase())
				{
					return custom.node
				}
			}
		}

		return null
	}

	data class QuestNodeWrapper(val guid: String, val key: String? = null)
	{
		lateinit var node: AbstractQuestNode

		fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
		{
			node = nodeMap[guid]
		}
	}
}

class Branch(quest: Quest, guid: String) : AbstractQuestNode(quest, guid)
{
	val branches = Array<BranchWrapper>()

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children())
		{
			val cond = xmlData.get("Condition")
			val guid = xmlData.get("Node")

			branches.add(BranchWrapper(guid, cond))
		}
	}

	override fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
	{
		for (branch in branches)
		{
			branch.resolve(nodeMap)
		}
	}

	override fun run(): QuestNode?
	{
		for (branch in branches)
		{
			if (branch.condition.evaluate(Global.flags) != 0f)
			{
				return branch.node.run()
			}
		}

		return null
	}

	data class BranchWrapper(val guid: String, val condition: String)
	{
		lateinit var node: AbstractQuestNode

		fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
		{
			node = nodeMap[guid]
		}
	}
}

class CompleteQuest(quest: Quest, guid: String) : AbstractQuestNode(quest, guid)
{
	lateinit var state: Quest.QuestState

	override fun parse(xmlData: XmlData)
	{
		state = Quest.QuestState.valueOf(xmlData.get("State", "Success")!!.toUpperCase())
	}

	override fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
	{

	}

	override fun run(): QuestNode?
	{
		quest.state = state
		return null
	}
}