package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Level
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Screens.GridScreen
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
			level!!.create(CardContentScreen.currentQuest.theme, Global.player,
						   {
							   cardContent.CardContentStack.last().index++
							   level = null

							   if (successNode != null)
							   {
								   cardContent.CardContentStack.add(CardContentNodeState(successNode!!))
							   }

							   Global.player.levelbuffs.clear()
							   Global.player.leveldebuffs.clear()

							   Global.game.switchScreen(MainGame.ScreenEnum.CARD)
							   CardContentScreen.advanceContent()
						   },
						   {
							   cardContent.CardContentStack.last().index++
							   level = null

							   if (failureNode != null)
							   {
								   cardContent.CardContentStack.add(CardContentNodeState(failureNode!!))
							   }

							   Global.player.levelbuffs.clear()
							   Global.player.leveldebuffs.clear()

							   Global.game.switchScreen(MainGame.ScreenEnum.CARD)
							   CardContentScreen.advanceContent()
						   })
			val screen = Global.game.getScreen(MainGame.ScreenEnum.GRID) as GridScreen
			screen.updateLevel(level!!, Global.player)
			Global.game.switchScreen(screen)
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