package com.lyeeedar.Card

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
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
import com.lyeeedar.Util.*
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

	fun save(output: Output)
	{
		output.writeString(path)
		output.writeString(current.guid)

		for (node in nodes)
		{
			output.writeBoolean(node.hasBeenPlayed)
		}
	}

	companion object
	{
		fun load(path: String): Card
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

		fun load(input: Input): Card
		{
			val path = input.readString()
			val currentGuid = input.readString()

			val card = load(path)
			card.current = card.nodes.first { it.guid == currentGuid }

			for (node in card.nodes)
			{
				node.hasBeenPlayed = input.readBoolean()
			}

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
	var isShop = false
	lateinit var content: String

	var hasBeenPlayed = false

	var nextNode: CardNodeWrapper? = null

	fun getCard(): CardWidget
	{
		return CardWidget(createTable(false), createTable(true), AssetManager.loadTextureRegion("GUI/CardCardback")!!, this)
	}

	fun createTable(detail: Boolean): Table
	{
		val wrapperTable = Table()

		val wrapperStack = Stack()
		wrapperTable.add(wrapperStack).grow()

		val table = Table()
		wrapperStack.add(table)

		val title = Label(name, Global.skin, "cardtitle")
		table.add(title).expandX().center().pad(10f, 0f, 0f, 0f)
		table.row()

		val rewardsTable = Table()
		table.add(rewardsTable).growX()
		table.row()

		if (isShop)
		{
			val icon = AssetManager.loadTextureRegion("Oryx/uf_split/uf_items/satchel")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)

			val stack = Stack()
			stack.touchable = Touchable.enabled

			stack.add(SpriteWidget(AssetManager.loadSprite("GUI/RewardChanceBorder", colour = Colour(Color.OLIVE)), 64f, 64f))
			stack.add(widget)

			stack.addTapToolTip("This encounter contains a shop.")
			rewardsTable.add(stack).expandX().center().pad(10f)
		}
		else
		{
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
		}

		if (detail)
		{
			val descLabel = Label(description, Global.skin, "card")
			descLabel.setWrap(true)
			table.add(descLabel).grow().pad(0f, 10f, 0f, 10f)
			table.row()
		}

		if (nextNode != null)
		{
			val icon = AssetManager.loadTextureRegion("Oryx/uf_split/uf_items/book_latch")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)

			val stack = Stack()
			stack.touchable = Touchable.enabled

			//stack.add(SpriteWidget(AssetManager.loadSprite("GUI/RewardChanceBorder", colour = Chance.ALWAYS.colour), 64f, 64f))
			stack.add(widget)

			stack.addTapToolTip("Completing this encounter will cause it to advance.")
			table.add(stack).expandX().center().pad(5f).padBottom(32f)
		}

		if (!hasBeenPlayed)
		{
			val newTable = Table()
			val newLabel = Label("New", Global.skin)
			newTable.add(newLabel).expand().top().left().pad(3f)

			wrapperStack.add(newTable)
		}

		return wrapperTable
	}

	fun fillWithDefaults()
	{
		guid = ""
		name = ""
		description = ""
		spawnWeight = SpawnWeight.ANY
		content = ""
	}

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		name = xmlData.get("Name")
		name = name.replace(":", ":\n")

		description = xmlData.get("Description")
		spawnWeight = SpawnWeight.valueOf(xmlData.get("SpawnWeighting", "Any")!!.toUpperCase())
		isShop = xmlData.getBoolean("IsShop", false)
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
		if (name == "" && description == "")
		{
			return CardContent.load(content)
		}

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