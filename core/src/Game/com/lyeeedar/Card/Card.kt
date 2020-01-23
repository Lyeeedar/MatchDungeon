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
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpawnWeight
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.*
import ktx.collections.gdxArrayOf
import ktx.collections.set
import ktx.collections.toGdxArray
import java.util.*

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

enum class CardSource private constructor(val icon: Sprite, val colourTint: Colour)
{
	DECK(AssetManager.loadSprite("GUI/CardCardback"), Colour.WHITE),
	CHARACTER(AssetManager.loadSprite("GUI/CharacterCardback"), Colour(0.9f, 1f, 0.9f, 1f)),
	QUEST(AssetManager.loadSprite("GUI/QuestCardback"), Colour(1f, 0.9f, 0.9f, 1f)),
	LOCATION(AssetManager.loadGrayscaleSprite("Oryx/Custom/terrain/flag_complete_4"), Colour(0.9f, 0.9f, 1f, 1f));

	val text: String
		get()
		{
			return when(this)
			{
				DECK -> Localisation.getText("cardsource.deck", "UI")
				CHARACTER -> Localisation.getText("cardsource.character", "UI")
				QUEST -> Localisation.getText("cardsource.quest", "UI")
				LOCATION -> Localisation.getText("cardsource.location", "UI")
			}
		}
}

class CardNode
{
	lateinit var parent: Card

	lateinit var guid: String

	lateinit var nameID: String
	lateinit var descriptionID: String
	lateinit var spawnWeight: SpawnWeight
	var isShop = false
	var hiddenRewards = false
	lateinit var content: String

	var hasBeenPlayed = false
	var cardSource = CardSource.DECK

	var nextNode: CardNodeWrapper? = null

	fun getCard(): CardWidget
	{
		val colour = cardSource.colourTint
		val descriptors: Array<Pair<Sprite, String?>> = gdxArrayOf(Pair(cardSource.icon, cardSource.text))
		return CardWidget.createCard(
				Localisation.getText(nameID, "Card").replace(':', '\n'),
				Localisation.getText("encounter", "UI"),
				AssetManager.loadSprite("GUI/CardCardback"),
				createTable(false),
				createTable(true),
				this,
				colour,
				descriptors = descriptors)
	}

	fun createTable(detail: Boolean): Table
	{
		val wrapperTable = Table()

		val wrapperStack = Stack()
		wrapperTable.add(wrapperStack).grow()

		val table = Table()
		wrapperStack.add(table)

		table.add(Table()).grow()
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

			stack.addTapToolTip(Localisation.getText("card.containsshop", "UI"))
			rewardsTable.add(stack).expandX().center().pad(10f)
		}
		else if (hiddenRewards)
		{
			val icon = AssetManager.loadTextureRegion("GUI/UnknownCardback")!!
			val widget = SpriteWidget(Sprite(icon), 64f, 64f)

			val stack = Stack()
			stack.touchable = Touchable.enabled

			stack.add(SpriteWidget(AssetManager.loadSprite("GUI/RewardChanceBorder", colour = Colour.DARK_GRAY), 64f, 64f))
			stack.add(widget)

			stack.addTapToolTip(Localisation.getText("card.unknownreward", "UI"))
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

				val stack = Stack()
				stack.touchable = Touchable.enabled

				stack.add(SpriteWidget(AssetManager.loadSprite("GUI/RewardChanceBorder", colour = reward.chance.colour), 64f, 64f))
				stack.add(widget)

				var template = Localisation.getText("card.rewardchance", "UI")
				template = template.replace("{Chance}", reward.chance.niceName)
				template = template.replace("{Reward}", reward.niceName)

				stack.addTapToolTip(template)
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
			val descLabel = Label(Localisation.getText(descriptionID, "Card"), Statics.skin, "card")
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

			stack.addTapToolTip(Localisation.getText("card.willadvance", "UI"))
			table.add(stack).expandX().center().pad(5f).padBottom(32f)
		}

		table.add(Table()).grow()
		table.row()

		if (!hasBeenPlayed && (cardSource == CardSource.DECK || cardSource == CardSource.CHARACTER))
		{
			val newTable = Table()
			val newLabel = Label(Localisation.getText("new", "UI"), Statics.skin)
			newTable.add(newLabel).expand().top().left().pad(3f)

			wrapperStack.add(newTable)
		}

		return wrapperTable
	}

	fun fillWithDefaults()
	{
		guid = ""
		nameID = ""
		descriptionID = ""
		spawnWeight = SpawnWeight.ANY
		content = ""
	}

	fun parse(xmlData: XmlData)
	{
		guid = xmlData.getAttribute("GUID")

		nameID = xmlData.get("Name")
		descriptionID = xmlData.get("Description")

		spawnWeight = SpawnWeight.valueOf(xmlData.get("SpawnWeighting", "Any")!!.toUpperCase(Locale.ENGLISH))
		isShop = xmlData.getBoolean("IsShop", false)
		hiddenRewards = xmlData.getBoolean("HiddenRewards", false)
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
		if (nameID == "" && descriptionID == "")
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