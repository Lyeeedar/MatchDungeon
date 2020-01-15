package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Level
import com.lyeeedar.Game.Global
import com.lyeeedar.ScreenEnum
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.directory

class CardContentActionMatch3 : AbstractCardContentAction()
{
	lateinit var levelFile: String
	var level: Level? = null

	lateinit var successNodeGuid: String
	lateinit var failureNodeGuid: String

	var successNode: CardContentNode? = null
	var failureNode: CardContentNode? = null

	override fun parse(xmlData: XmlData)
	{
		levelFile = xmlData.get("Level")
		successNodeGuid = xmlData.get("Success", "")!!
		failureNodeGuid = xmlData.get("Failure", "")!!
	}

	override fun advance(cardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (level == null)
		{
			level = Level.load(cardContent.path.directory() + "/" + levelFile).random()
			level!!.create(CardContentScreen.currentQuest.currentTheme, Global.player,
						   {
							   level = null
							   cardContent.CardContentStack.last().index++

							   if (successNode != null)
							   {
								   cardContent.CardContentStack.add(CardContentNodeState(successNode!!))
							   }

							   Global.player.levelbuffs.clear()
							   Global.player.leveldebuffs.clear()

							   Statics.game.switchScreen(ScreenEnum.CARD)
							   CardContentScreen.advanceContent()
						   },
						   {
							   level = null
							   cardContent.CardContentStack.last().index++

							   if (failureNode != null)
							   {
								   cardContent.CardContentStack.add(CardContentNodeState(failureNode!!))
							   }

							   Global.player.levelbuffs.clear()
							   Global.player.leveldebuffs.clear()

							   Statics.game.switchScreen(ScreenEnum.CARD)
							   CardContentScreen.advanceContent()
						   })
			val screen = Statics.game.getScreen(ScreenEnum.GRID) as GridScreen
			screen.updateLevel(level!!, Global.player)
			Statics.game.switchScreen(screen)
		}

		return false
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{
		if (!successNodeGuid.isBlank())
		{
			successNode = nodeMap[successNodeGuid]
		}
		if (!failureNodeGuid.isBlank())
		{
			failureNode = nodeMap[failureNodeGuid]
		}
	}
}