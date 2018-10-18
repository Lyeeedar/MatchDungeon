package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.actors.then
import ktx.collections.toGdxArray

enum class CardChance
{
	GREATPOSITIVE,
	POSITIVE,
	NEGATIVE,
	GREATNEGATIVE
}

class CardContentActionChanceCards : AbstractCardContentAction()
{
	val chances = Array<CardChance>()
	val chanceNodes = Array<ChanceNode>()
	var shuffleSpeedMultiplier: Float = 1f
	val greyOutTable = Table()
	var awaitingAdvance = false

	init
	{
		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)
	}

	override fun parse(xmlData: XmlData)
	{
		val chancesEl = xmlData.getChildByName("ChanceCards")!!
		for (el in chancesEl.children())
		{
			chances.add(CardChance.valueOf(el.text.toUpperCase()))
		}

		if (chances.size != 4)
		{
			throw Exception("Invalid number of chance cards! Needs 4 and got " + chances.size)
		}

		shuffleSpeedMultiplier = xmlData.getFloat("ShuffleSpeedMultiplier", 1f)

		val nodesEl = xmlData.getChildByName("Nodes")!!
		for (el in nodesEl.children())
		{
			val chance = CardChance.valueOf(el.get("Chance").toUpperCase())
			var key = el.get("Node", null)
			var hasNode = true

			if (key == null)
			{
				hasNode = false
				key = ""
			}

			chanceNodes.add(ChanceNode(chance, key, hasNode))
		}
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (awaitingAdvance)
		{
			return false
		}

		val chanceCardback = AssetManager.loadTextureRegion("GUI/ChanceCardback")!!
		val cards = Array<CardWidget>()

		CardContentScreen.stage.addActor(greyOutTable)

		// spawn cards
		for (chance in chances)
		{
			val cardFront = when (chance)
			{
				CardChance.GREATPOSITIVE -> AssetManager.loadTextureRegion("GUI/GreatPositiveChance")!!
				CardChance.POSITIVE -> AssetManager.loadTextureRegion("GUI/PositiveChance")!!
				CardChance.NEGATIVE -> AssetManager.loadTextureRegion("GUI/NegativeChance")!!
				CardChance.GREATNEGATIVE -> AssetManager.loadTextureRegion("GUI/GreatNegativeChance")!!
			}

			val frontTable = Table()
			frontTable.add(SpriteWidget(Sprite(cardFront), 64f, 64f)).grow()

			val frontDetailTable = Table()
			frontDetailTable.add(SpriteWidget(Sprite(cardFront), 64f, 64f)).grow()

			val widget = CardWidget(frontTable, frontDetailTable, chanceCardback, chance)
			cards.add(widget)
			widget.clickable = false
			widget.canZoom = false
			widget.addPick("click", {
				for (card in cards)
				{
					if (card != widget)
					{
						card.remove()
					}
				}

				widget.setFacing(true, true)
				widget.clickable = false

				Future.call({
								widget.remove()
								greyOutTable.remove()

								CardContent.CardContentStack.last().index++

								val node = chanceNodes.firstOrNull { it.chance == chance }
								if (node != null && node.hasNode)
								{
									CardContent.CardContentStack.add(CardContentNodeState(node.node))
								}

								awaitingAdvance = false
								CardContentScreen.advanceContent()
							}, 1f)

			})
			CardContentScreen.stage.addActor(widget)
		}

		// place in grid
		CardWidget.layoutCards(cards, Direction.CENTER, animate = false)

		// store grid positions
		val positions = cards.map { Vector2(it.x, it.y) }.toGdxArray()

		// Reset cards to center for animation
		val areaWidth = Global.resolution.x.toFloat()
		val areaHeight = Global.resolution.y.toFloat()

		val padding = 20f
		val cardWidth = (areaWidth - 3f * padding) / 2f
		val cardHeight = (areaHeight - 3f * padding) / 2f

		val startX = areaWidth / 2f - cardWidth / 2f
		val startY = areaHeight / 2f - cardHeight / 2f

		// animate to positions
		var delay = 0.2f
		for (widget in cards)
		{
			val x = widget.x
			val y = widget.y

			widget.setPosition(startX , startY)

			val delayVal = delay
			delay += 0.04f
			val sequence = Actions.delay(delayVal) then Actions.moveTo(x, y, 0.2f) then Actions.delay(0.1f) then lambda { widget.setFacing(true, true) }
			widget.addAction(sequence)
		}
		delay += 0.7f

		// wait second
		delay += 1f

		// flip over
		for (widget in cards)
		{
			val sequence = Actions.delay(delay) then lambda { widget.setFacing(false, true) }
			widget.addAction(sequence)
		}
		delay += 0.8f

		// shuffle
		val shuffleSpeed = 0.5f * (shuffleSpeedMultiplier + Global.player.getStat(Statistic.LUCK, true))
		for (round in 0 until 6)
		{
			val tempPositions = kotlin.Array<CardWidget?>(4) { null }
			for (i in 0 until 4)
			{
				val card = cards[i]
				val validPositions = Array<Int>()
				for (ii in 0 until 4)
				{
					if (tempPositions[ii] == null)
					{
						if (ii == i)
						{
							validPositions.add(ii)
						}
						else
						{
							for (weight in 0 until 3)
							{
								validPositions.add(ii)
							}
						}
					}
				}

				val chosenPos = validPositions.random()
				tempPositions[chosenPos] = card

				val sequence = Actions.delay(delay) then moveTo(positions[chosenPos].x, positions[chosenPos].y, shuffleSpeed)
				card.addAction(sequence)
			}

			delay += shuffleSpeed + 0.1f
		}

		// allow picking
		for (widget in cards)
		{
			val sequence = Actions.delay(delay) then lambda { widget.canPickFaceDown = true; widget.clickable = true }
			widget.addAction(sequence)
		}

		awaitingAdvance = true
		return false
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{
		for (branch in chanceNodes)
		{
			if (branch.hasNode)
			{
				branch.node = nodeMap[branch.key]
			}
		}
	}
}

data class ChanceNode(val chance: CardChance, val key: String, val hasNode: Boolean)
{
	lateinit var node: CardContentNode
}