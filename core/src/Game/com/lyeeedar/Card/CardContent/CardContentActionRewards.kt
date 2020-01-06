package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.AbstractReward
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.XmlData
import ktx.collections.toGdxArray

class CardContentActionRewards : AbstractCardContentAction()
{
	val rewards = Array<AbstractReward>()

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children)
		{
			rewards.add(AbstractReward.load(el))
		}
	}

	var displayingRewards = false
	var awaitingAdvance = false

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (!displayingRewards && !awaitingAdvance)
		{
			displayingRewards = true

			val chosenRewards = rewards.filter { it.chance.evaluate() }.flatMap { it.reward() }.toGdxArray()

			CardWidget.displayLoot(chosenRewards, CardWidget.Companion.LootAnimation.CHEST) {
				awaitingAdvance = true
				CardContent.CardContentStack.last().index++
				CardContentScreen.advanceContent()
			}
		}

		val complete = displayingRewards && awaitingAdvance
		if (complete)
		{
			displayingRewards = false
			awaitingAdvance = false
		}

		return complete
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

