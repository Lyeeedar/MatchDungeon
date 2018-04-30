package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Global
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.round

class CardContentActionBranch : AbstractCardContentAction()
{
	val branches = Array<BranchNode>()

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		val variables = Global.flags.flags
		for (branch in branches)
		{
			if (branch.condition.evaluate(variables).round() == 1)
			{
				if (branch.hasNode)
				{
					CardContent.CardContentStack.add(CardContentNodeState(branch.node))
				}

				break
			}
		}

		return true
	}

	override fun parse(xml: XmlData)
	{
		for (el in xml.children())
		{
			val condition = el.get("Condition", "1")!!
			var key = el.get("Node", null)
			var hasNode = true

			if (key == null)
			{
				hasNode = false
				key = ""
			}

			branches.add(BranchNode(condition, key, hasNode))
		}
	}

	override fun resolve(nodes: ObjectMap<String, CardContentNode>)
	{
		for (branch in branches)
		{
			if (branch.hasNode)
			{
				branch.node = nodes[branch.key]
			}
		}
	}
}

data class BranchNode(val condition: String, val key: String, val hasNode: Boolean)
{
	lateinit var node: CardContentNode
}