package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.Game.AbstractReward
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.Util.AssetManager
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

	var grouped: Array<Array<AbstractReward>> = Array()
	var currentGroup = Array<CardWidget>()
	val greyOutTable = Table()
	var awaitingAdvance = false

	init
	{
		greyOutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		greyOutTable.touchable = Touchable.enabled
		greyOutTable.setFillParent(true)
	}

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (currentGroup.size > 0)
		{
			// do nothing
		}
		else
		{
			// advance

			if (!awaitingAdvance && grouped.size == 0)
			{
				grouped = rewards.groupBy { it.javaClass }.map { it.value.toGdxArray() }.toGdxArray()
				Global.stage.addActor(greyOutTable)
				awaitingAdvance = true
			}

			if (grouped.size > 0)
			{
				val chosen = grouped.removeIndex(0)
				currentGroup = chosen.map { it.reward() }.filter { it != null }.map { it!! }.toGdxArray()

				for (card in currentGroup)
				{
					for (pick in card.pickFuns)
					{
						val oldFun = pick.pickFun
						pick.pickFun = {
							oldFun(it)
							currentGroup.removeValue(card, true)
							if (currentGroup.size == 0)
							{
								CardContent.advance(CardContentScreen)
							}

							card.remove()
						}
					}

					Global.stage.addActor(card)
				}

				if (currentGroup.size > 0)
				{
					CardWidget.layoutCards(currentGroup, Direction.CENTER)
				}
				else
				{
					CardContent.advance(CardContentScreen)
				}
			}
		}

		val complete = grouped.size == 0 && currentGroup.size == 0
		if (complete)
		{
			greyOutTable.remove()
		}

		return complete
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

