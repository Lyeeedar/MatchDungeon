package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Game.*
import com.lyeeedar.Util.AssetManager
import kotlin.math.min

class QuestProgressWidget() : Widget()
{
	lateinit var quest: Quest

	val empty = AssetManager.loadTextureRegion("GUI/health_empty")
	val shop = AssetManager.loadTextureRegion("GUI/health_damaged")
	val current = AssetManager.loadTextureRegion("GUI/health_full_green")
	val encounter = AssetManager.loadTextureRegion("GUI/health_full")

	val questPath = Array<QuestNode>()

	init
	{
		addClickListenerFull { inputEvent, clickx, clicky ->
			val sectionSize = width / questPath.size
			val boxSize = min(sectionSize - 8, height - 4)

			for (i in 0 until questPath.size)
			{
				val sx = sectionSize*i + (sectionSize - boxSize) / 2f
				val sy = height / 2f - boxSize / 2f

				if (clickx >= sx && clickx <= sx+boxSize)
				{
					when
					{
						questPath[i] == quest.current -> "The current encounter in the quest".showTooltip(inputEvent, clickx, clicky)
						questPath[i].isShop -> "An encounter containing a shop".showTooltip(inputEvent, clickx, clicky)
						questPath[i].type == QuestNode.QuestNodeType.FIXED -> "A quest encounter".showTooltip(inputEvent, clickx, clicky)
						else -> "A random encounter".showTooltip(inputEvent, clickx, clicky)
					}
				}
			}
		}
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (!questPath.contains(quest.current) && quest.current != null)
		{
			// We arent on the quest path, so update it
			questPath.clear()

			// find path to current
			val visitedNodes = ObjectSet<AbstractQuestNode>()
			val pathToCurrent = Array<AbstractQuestNode>()
			fun recursiveWalkToCurrent(current: AbstractQuestNode): Boolean
			{
				if (visitedNodes.contains(current))
				{
					return false
				}
				visitedNodes.add(current)

				if (current == quest.current)
				{
					return true
				}

				if (current is Branch)
				{
					for (branch in current.branches)
					{
						val found = recursiveWalkToCurrent(branch.node)
						if (found)
						{
							pathToCurrent.add(current)
							return true
						}
					}
				}
				else if (current is QuestNode)
				{
					if (current.successNode != null && recursiveWalkToCurrent(current.successNode!!.node))
					{
						pathToCurrent.add(current)
						return true
					}
					if (current.failureNode != null && recursiveWalkToCurrent(current.failureNode!!.node))
					{
						pathToCurrent.add(current)
						return true
					}

					for (custom in current.customNodes)
					{
						if (recursiveWalkToCurrent(custom.node))
						{
							pathToCurrent.add(current)
							return true
						}
					}
				}
				else if (current is Define)
				{
					if (recursiveWalkToCurrent(current.next.node))
					{
						pathToCurrent.add(current)
						return true
					}
				}
				else if (current is SetTheme)
				{
					if (recursiveWalkToCurrent(current.next.node))
					{
						pathToCurrent.add(current)
						return true
					}
				}
				else if (current is CompleteQuest)
				{

				}
				else
				{
					throw Exception("Unhandled quest node type '" + current.javaClass.name + "'!")
				}

				return false
			}
			recursiveWalkToCurrent(quest.root)
			pathToCurrent.reverse() // this will be the first path from root to current

			// find path from current to end
			visitedNodes.clear()
			val pathToEnd = Array<AbstractQuestNode>()
			fun recursiveWalkToEnd(current: AbstractQuestNode): Boolean
			{
				if (visitedNodes.contains(current))
				{
					return false
				}
				visitedNodes.add(current)

				if (current is CompleteQuest)
				{
					pathToEnd.add(current)
					return true
				}

				if (current is Branch)
				{
					for (branch in current.branches)
					{
						val found = recursiveWalkToEnd(branch.node)
						if (found)
						{
							pathToEnd.add(current)
							return true
						}
					}
				}
				else if (current is QuestNode)
				{
					if (current.successNode != null && recursiveWalkToEnd(current.successNode!!.node))
					{
						pathToEnd.add(current)
						return true
					}
					if (current.failureNode != null && recursiveWalkToEnd(current.failureNode!!.node))
					{
						pathToEnd.add(current)
						return true
					}

					for (custom in current.customNodes)
					{
						if (recursiveWalkToEnd(custom.node))
						{
							pathToEnd.add(current)
							return true
						}
					}
				}
				else if (current is Define)
				{
					if (recursiveWalkToEnd(current.next.node))
					{
						pathToEnd.add(current)
						return true
					}
				}
				else if (current is SetTheme)
				{
					if (recursiveWalkToEnd(current.next.node))
					{
						pathToEnd.add(current)
						return true
					}
				}
				else if (current is CompleteQuest)
				{

				}
				else
				{
					throw Exception("Unhandled quest node type '" + current.javaClass.name + "'!")
				}

				return false
			}
			recursiveWalkToEnd(quest.current!!)
			pathToEnd.reverse() // this will be the first path from current to an end

			for (node in pathToCurrent)
			{
				if (node is QuestNode)
				{
					questPath.add(node)
				}
			}

			for (node in pathToEnd)
			{
				if (node is QuestNode)
				{
					questPath.add(node)
				}
			}
		}

		// render path

		val sectionSize = width / questPath.size
		val boxSize = min(sectionSize - 8, height - 4)

		for (i in 0 until questPath.size)
		{
			val img = when
			{
				questPath[i] == quest.current -> current
				questPath[i].isShop -> shop
				questPath[i].type == QuestNode.QuestNodeType.FIXED -> encounter
				else -> empty
			}

			val sx = sectionSize*i + (sectionSize - boxSize) / 2f
			val sy = height / 2f - boxSize / 2f

			batch?.draw(img, x + sx, y + sy, boxSize, boxSize)
		}

		super.draw(batch, parentAlpha)
	}
}