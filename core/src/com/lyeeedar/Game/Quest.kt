package com.lyeeedar.Game

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.Theme
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpawnWeight
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class Quest(val path: String)
{
	enum class QuestState
	{
		INPROGRESS,
		FAILURE,
		BRONZE,
		SILVER,
		GOLD
	}

	var state = QuestState.INPROGRESS

	val nodes: Array<AbstractQuestNode>
	val root: AbstractQuestNode
	val questCards: Array<Card> = Array()

	var current: AbstractQuestNode? = null

	val theme: Theme

	val bronzeRewards = Array<AbstractReward>()
	val silverRewards = Array<AbstractReward>()
	val goldRewards = Array<AbstractReward>()

	val themeCards = Array<Card>()

	var gotBronze = false
	var gotSilver = false
	var gotGold = false

	val title: String
	val description: String

	var shopCounter = 2

	init
	{
		val rawPath = "Quests/$path"
		val xml = getXml(rawPath)

		title = xml.get("Title", "")!!
		description = xml.get("Description", "")!!

		val themeName = xml.get("Theme")
		theme = Theme.Companion.load("Themes/$themeName")

		for (themeCardPath in XmlData.enumeratePaths("Cards/$themeName", "Card"))
		{
			val card = Card.load(themeCardPath)
			themeCards.add(card)
		}

		val questCardsEl = xml.getChildByName("QuestCards")
		if (questCardsEl != null)
		{
			for (questCardEl in questCardsEl.children())
			{
				val card = Card.load(rawPath.directory() + "/" + questCardEl.text)
				questCards.add(card)
			}
		}

		val bronzeRewardsEl = xml.getChildByName("BronzeRewards")
		if (bronzeRewardsEl != null)
		{
			for (el in bronzeRewardsEl.children)
			{
				bronzeRewards.add(AbstractReward.load(el))
			}
		}

		val silverRewardsEl = xml.getChildByName("SilverRewards")
		if (silverRewardsEl != null)
		{
			for (el in silverRewardsEl.children)
			{
				silverRewards.add(AbstractReward.load(el))
			}
		}

		val goldRewardsEl = xml.getChildByName("GoldRewards")
		if (goldRewardsEl != null)
		{
			for (el in goldRewardsEl.children)
			{
				goldRewards.add(AbstractReward.load(el))
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

	fun createTable(detail: Boolean): Table
	{
		val table = Table()

		val title = Label(title, Global.skin, "cardtitle")
		table.add(title).expandX().center().pad(10f, 0f, 0f, 0f)
		table.row()

		val rewardsTable = Table()
		table.add(rewardsTable).growX().center()
		table.row()

		if (!gotBronze && bronzeRewards.size > 0)
		{
			val icon = AssetManager.loadTextureRegion("Oryx/uf_split/uf_items/coin_copper")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)
			widget.addTapToolTip("Can gain a bronze level reward.")
			rewardsTable.add(widget).expandX().center().pad(10f)
		}

		if (!gotSilver && silverRewards.size > 0)
		{
			val icon = AssetManager.loadTextureRegion("Oryx/uf_split/uf_items/coin_silver")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)
			widget.addTapToolTip("Can gain a silver level reward.")
			rewardsTable.add(widget).expandX().center().pad(10f)
		}

		if (!gotGold && goldRewards.size > 0)
		{
			val icon = AssetManager.loadTextureRegion("Oryx/uf_split/uf_items/coin_gold")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)
			widget.addTapToolTip("Can gain a gold level reward.")
			rewardsTable.add(widget).expandX().center().pad(10f)
		}

		if (detail)
		{
			val descLabel = Label(description, Global.skin, "card")
			descLabel.setWrap(true)
			table.add(descLabel).grow().pad(0f, 10f, 0f, 10f)
		}

		return table
	}

	fun getCard(): CardWidget
	{
		return CardWidget(createTable(false), createTable(true), AssetManager.loadTextureRegion("GUI/QuestCardback")!!, this)
	}

	fun run()
	{
		current = current?.run()
		if (current == null && state == QuestState.INPROGRESS)
		{
			state = QuestState.FAILURE
		}
	}

	fun getQuestPosition(): SpawnWeight
	{
		var averageCurrentPoint: Int? = null
		var averageRun: Int? = null

		val visited = ObjectSet<AbstractQuestNode>()

		fun recursiveWalk(currentNode: AbstractQuestNode, currentRun: Int)
		{
			if (visited.contains(currentNode))
			{
				return
			}
			visited.add(currentNode)

			if (currentNode == current)
			{
				if (averageCurrentPoint == null)
				{
					averageCurrentPoint = currentRun
				}
				else
				{
					var nextVal = averageCurrentPoint!!

					nextVal += currentRun
					nextVal /= 2

					averageCurrentPoint = nextVal
				}
			}

			if (currentNode is CompleteQuest)
			{
				if (averageRun == null)
				{
					averageRun = currentRun
				}
				else
				{
					var nextVal = averageRun!!

					nextVal += currentRun
					nextVal /= 2

					averageRun = nextVal
				}
			}
			else
			{
				if (currentNode is Branch)
				{
					for (branch in currentNode.branches)
					{
						recursiveWalk(branch.node, currentRun+1)
					}
				}
				else if (currentNode is QuestNode)
				{
					if (currentNode.successNode != null) recursiveWalk(currentNode.successNode!!.node, currentRun+1)
					if (currentNode.failureNode != null) recursiveWalk(currentNode.failureNode!!.node, currentRun+1)

					for (custom in currentNode.customNodes)
					{
						recursiveWalk(custom.node, currentRun+1)
					}
				}
			}
		}

		recursiveWalk(root, 0)

		val third = averageCurrentPoint!! / 3

		if (averageCurrentPoint!! <= third) return SpawnWeight.START
		else if (averageCurrentPoint!! <= 2*third) return SpawnWeight.MIDDLE
		else return SpawnWeight.END
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
				"DEFINE" -> Define(quest, guid)
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
	var allowThemeCards = false

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
			val spawnWeight = quest.getQuestPosition()

			val pool = Array<Card>()
			val shops = Array<Card>()
			if (allowDeckCards)
			{
				for (card in Global.player.deck.encounters)
				{
					val pool = if (card.current.isShop) shops else pool

					if (card.current.spawnWeight.subWeights.contains(spawnWeight))
					{
						// make it 3x more likely
						for (i in 0 until 3)
						{
							pool.add(card)
						}
					}
					else
					{
						pool.add(card)
					}
				}
			}
			if (allowQuestCards)
			{
				for (card in quest.questCards)
				{
					val pool = if (card.current.isShop) shops else pool

					if (card.current.spawnWeight.subWeights.contains(spawnWeight))
					{
						// make it 3x more likely
						for (i in 0 until 3)
						{
							pool.add(card)
						}
					}
					else
					{
						pool.add(card)
					}
				}
			}
			if (allowThemeCards)
			{
				for (card in quest.themeCards)
				{
					val pool = if (card.current.isShop) shops else pool

					if (card.current.spawnWeight.subWeights.contains(spawnWeight))
					{
						// make it 3x more likely
						for (i in 0 until 3)
						{
							pool.add(card)
						}
					}
					else
					{
						pool.add(card)
					}
				}
			}

			if (pool.size > 0)
			{
				while (output.size < 4 && pool.size > 0)
				{
					val picked = pool.removeRandom(Random.random)
					if (!output.contains(picked))
					{
						output.add(picked)
					}
				}
			}

			if (quest.shopCounter <= 0 && !output.any { it.current.isShop } && shops.size > 0 && Global.player.gold > 25)
			{
				// add a shop
				if (output.size == 4)
				{
					output.removeRandom(Random.random)
				}

				output.add(shops.random())

				quest.shopCounter = Random.random(2) + 1
			}
			else
			{
				quest.shopCounter--
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
			allowThemeCards = xmlData.getBoolean("AllowThemeCards", false)
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
			val cond = el.get("Condition")
			val guid = el.get("Node")

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
			if (branch.condition.evaluate(Global.getVariableMap()) != 0f)
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
		state = Quest.QuestState.valueOf(xmlData.get("State", "Bronze")!!.toUpperCase())
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

class Define(quest: Quest, guid: String) : AbstractQuestNode(quest, guid)
{
	lateinit var next: QuestNode.QuestNodeWrapper

	lateinit var key: String
	lateinit var value: String
	var isGlobal: Boolean = false

	override fun resolve(nodeMap: ObjectMap<String, AbstractQuestNode>)
	{
		next.resolve(nodeMap)
	}

	override fun run(): QuestNode?
	{
		val newVal = value.evaluate(Global.getVariableMap())

		if (isGlobal)
		{
			Global.globalflags.flags.put(key, newVal)
		}
		else
		{
			Global.levelflags.flags.put(key, newVal)
		}

		return next.node.run()
	}

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
		value = xmlData.get("Value")
		isGlobal = xmlData.getBoolean("IsGlobal", false)

		val nextID = xmlData.get("Next")
		next = QuestNode.QuestNodeWrapper(nextID)
	}
}