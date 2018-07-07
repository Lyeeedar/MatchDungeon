package com.lyeeedar.Card

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Card.CardContent.CardContentActionRewards
import com.lyeeedar.Game.AbstractReward
import com.lyeeedar.Game.Chance
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpawnWeight
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.directory
import com.lyeeedar.Util.getXml
import ktx.collections.set
import ktx.collections.toGdxArray

class Card(val path: String, val nodes: Array<CardNode>, val root: CardNode)
{
	var current: CardNode = root

	init
	{
		for (node in nodes)
		{
			node.parent = this
		}
	}

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
	lateinit var parent: Card

	lateinit var guid: String

	lateinit var name: String
	lateinit var description: String
	lateinit var spawnWeight: SpawnWeight
	lateinit var content: String

	var nextNode: CardNodeWrapper? = null

	fun getCard(): CardWidget
	{
		return CardWidget(createTable(false), createTable(true), AssetManager.loadTextureRegion("GUI/CardCardback")!!, this)
	}

	fun createTable(detail: Boolean): Table
	{
		val table = Table()

		val title = Label(name, Global.skin, "cardtitle")
		table.add(title).expandX().center().pad(10f, 0f, 0f, 0f)
		table.row()

		val rewardsTable = Table()
		table.add(rewardsTable).growX()
		table.row()

		val rawRewards = getPossibleRewards()

		data class RewardData(val rewardClass: Class<AbstractReward>, val icon: TextureRegion, var chance: Chance)

		val rewardData = Array<RewardData>()
		for (reward in rawRewards)
		{
			val existing = rewardData.firstOrNull { it.rewardClass == reward.javaClass }
			if (existing != null)
			{
				if (reward.chance.ordinal > existing.chance.ordinal)
				{
					existing.chance = reward.chance
				}
			}
			else
			{
				rewardData.add(RewardData(reward.javaClass, reward.cardIcon(), reward.chance))
			}
		}

		var needsrow = false
		val sorted = rewardData.sortedBy { it.rewardClass.toString() }
		for (reward in sorted)
		{
			val icon = reward.icon
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)
			val name = reward.rewardClass.simpleName.toString().replace("Reward", "").capitalize()
			val chance = reward.chance.uiString

			val stack = Stack()
			stack.touchable = Touchable.enabled

			stack.add(SpriteWidget(AssetManager.loadSprite("GUI/RewardChanceBorder", colour = reward.chance.colour), 64f, 64f))
			stack.add(widget)

			stack.addTapToolTip("Have a $chance chance to gain $name rewards.")
			rewardsTable.add(stack).expandX().center().pad(10f)

			if (needsrow)
			{
				rewardsTable.row()
				needsrow = false
			}
			else
			{
				needsrow = true
			}
		}

		if (detail)
		{
			val descLabel = Label(description, Global.skin, "card")
			descLabel.setWrap(true)
			table.add(descLabel).grow().pad(0f, 10f, 0f, 10f)
		}

		return table
	}

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		name = xmlData.get("Name")
		description = xmlData.get("Description")
		spawnWeight = SpawnWeight.valueOf(xmlData.get("SpawnWeighting", "Any")!!.toUpperCase())
		content = xmlData.get("Content")

		val nextEl = xmlData.getChildByName("Next")
		if (nextEl != null) nextNode = CardNodeWrapper(nextEl.text)
	}

	fun getPossibleRewards(): Array<AbstractReward>
	{
		val content = getContent()
		val actions = content.nodes.values().flatMap { it.actions }
		val rewardNodes = actions.filter { it is CardContentActionRewards }.map { it as CardContentActionRewards }

		val rawRewards = rewardNodes.flatMap { it.rewards }
		val validRewards = rawRewards.filter { it.isValid() }

		return validRewards.toGdxArray()
	}

	fun getContent(): CardContent
	{
		return CardContent.load(parent.path.directory() + "/" + content)
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