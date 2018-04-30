package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Level
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Util.XmlData

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
		levelFile = xmlData.get("LevelFile")
		successNodeGuid = xmlData.get("Success", "")!!
		failureNodeGuid = xmlData.get("Failure", "")!!
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (level == null)
		{
			level = Level.load(levelFile).random()
			level!!.create(CardContentScreen.currentQuest.theme, Global.player,
						   {
							   CardContent.CardContentStack.last().index++
							   level = null

							   if (successNode != null)
							   {
								   CardContent.CardContentStack.add(CardContentNodeState(successNode!!))
							   }

							   Global.game.switchScreen(MainGame.ScreenEnum.CARD)
							   CardContent.advance(CardContentScreen)
						   },
						   {
							   CardContent.CardContentStack.last().index++
							   level = null

							   if (failureNode != null)
							   {
								   CardContent.CardContentStack.add(CardContentNodeState(failureNode!!))
							   }

							   Global.game.switchScreen(MainGame.ScreenEnum.CARD)
							   CardContent.advance(CardContentScreen)
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