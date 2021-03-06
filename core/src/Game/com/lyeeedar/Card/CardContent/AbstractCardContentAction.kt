package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import java.util.*

abstract class AbstractCardContentAction
{
	abstract fun parse(xmlData: XmlData)
	abstract fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	abstract fun resolve(nodeMap: ObjectMap<String, CardContentNode>)

	companion object
	{
		fun load(xmlData: XmlData): AbstractCardContentAction
		{
			val refKey = xmlData.name.toUpperCase(Locale.ENGLISH)
			val action = when(refKey)
			{
				"LINE" -> CardContentActionLine()
				"BRANCH" -> CardContentActionBranch()
				"DEFINE" -> CardContentActionDefine()
				"CHOICE" -> CardContentActionChoice()
				"MATCH3" -> CardContentActionMatch3()
				"FLASH" -> CardContentActionFlash()
				"MARKCOMPLETE" -> CardContentActionMarkCompleted()
				"NODE" -> CardContentActionNode()
				"REWARDS" -> CardContentActionRewards()
				"FADEOUT" -> CardContentActionFadeOut()
				"CLEARFADEOUT" -> CardContentActionClearFadeOut()
				"SPEND" -> CardContentActionSpend()
				"SHOP" -> CardContentActionShop()
				"CHANCECARDS" -> CardContentActionChanceCards()
				else -> throw RuntimeException("Unknown CardContent action type '$refKey'!")
			}

			action.parse(xmlData)
			return action
		}
	}
}