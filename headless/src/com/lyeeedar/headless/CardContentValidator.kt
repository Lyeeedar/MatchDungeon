package com.lyeeedar.headless

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.Level
import com.lyeeedar.Card.CardContent.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.asGdxArray
import com.lyeeedar.Util.directory

class CardContentValidator
{
	var hadErrors = false

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Card Content Validator      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		for (path in XmlData.enumeratePaths("", "CardContent").toList())
		{
			try
			{
				parseXml(path)
			}
			catch (e: Exception)
			{
				System.err.println("Failed to load '${path}'!")
				throw e
			}
		}

		if (hadErrors)
		{
			throw Exception("Invalid cards!")
		}
	}

	fun parseXml( path: String)
	{
		val content = CardContent.load(path.replace(".xml", ""))

		val state = walkTree(content, content.root.node, CurrentState(), Array())
		if (!state.markedComplete)
		{
			state.warnings.add("CardContent is never marked complete!")
		}

		for (line in state.warnings)
		{
			System.err.println(line)
		}

		for (line in state.errors)
		{
			System.err.println(line)
		}

		if (state.warnings.size > 0)
		{
			System.err.println("CardContent $path had one or more warnings!")
		}

		if (state.errors.size > 0)
		{
			System.err.println("CardContent $path had one or more errors!")
			hadErrors = true
		}

		if (state.warnings.size == 0 && state.errors.size == 0)
		{
			println("Successfully validated $path")
		}
	}

	fun walkTree(cardContent: CardContent, node: CardContentNode, state: CurrentState, path: Array<CardContentNode>): CurrentState
	{
		if (path.contains(node))
		{
			return state
		}
		path.add(node)

		for (action in node.actions)
		{
			if (action is CardContentActionBranch)
			{
				val newState = state.copy()

				for (branch in action.branches)
				{
					if (branch.hasNode)
					{
						val nextNode = branch.node
						val childState = walkTree(cardContent, nextNode, newState.copy(), path.asGdxArray())
						state.errors.addAll(childState.errors)
						state.warnings.addAll(childState.warnings)

						if (childState.markedComplete)
						{
							state.markedComplete = true
						}
					}
				}
			}
			else if (action is CardContentActionChoice)
			{
				val newState = state.copy()

				for (branch in action.choices)
				{
					if (branch.node != null)
					{
						val nextNode = branch.node!!
						val childState = walkTree(cardContent, nextNode, newState.copy(), path.asGdxArray())
						state.errors.addAll(childState.errors)
						state.warnings.addAll(childState.warnings)

						if (childState.markedComplete)
						{
							state.markedComplete = true
						}
					}
				}
			}
			else if (action is CardContentActionDefine)
			{

			}
			else if (action is CardContentActionFadeOut)
			{

			}
			else if (action is CardContentActionClearFadeOut)
			{

			}
			else if (action is CardContentActionFlash)
			{

			}
			else if (action is CardContentActionLine)
			{

			}
			else if (action is CardContentActionMarkCompleted)
			{
				state.markedComplete = true
			}
			else if (action is CardContentActionMatch3)
			{
				try
				{
					Level.load(cardContent.path.directory() + "/" + action.levelFile)
				}
				catch (e: Exception)
				{
					state.errors.add("Failed to load level '" + action.levelFile + "'!")
				}

				val nextNodes = Array<CardContentNode>()
				if (action.successNode != null) nextNodes.add(action.successNode)
				if (action.failureNode != null) nextNodes.add(action.failureNode)

				val newState = state.copy()

				for (nextNode in nextNodes)
				{
					val childState = walkTree(cardContent, nextNode, newState.copy(), path.asGdxArray())
					state.errors.addAll(childState.errors)
					state.warnings.addAll(childState.warnings)

					if (childState.markedComplete)
					{
						state.markedComplete = true
					}
				}
			}
			else if (action is CardContentActionNode)
			{
				val newState = state.copy()

				val nextNode = action.node
				val childState = walkTree(cardContent, nextNode, newState.copy(), path.asGdxArray())
				state.errors.addAll(childState.errors)
				state.warnings.addAll(childState.warnings)

				if (childState.markedComplete)
				{
					state.markedComplete = true
				}
			}
			else if (action is CardContentActionRewards)
			{

			}
			else if (action is CardContentActionShop)
			{

			}
			else if (action is CardContentActionSpend)
			{

			}
			else if (action is CardContentActionChanceCards)
			{
				for (chance in action.chanceNodes)
				{
					if (chance.hasNode)
					{
						val childState = walkTree(cardContent, chance.node, state.copy(), path.asGdxArray())
						state.errors.addAll(childState.errors)
						state.warnings.addAll(childState.warnings)

						if (childState.markedComplete)
						{
							state.markedComplete = true
						}
					}
				}
			}
			else
			{
				throw Exception("Unknown CardContent action type: '" + action::class.java.simpleName + "'!")
			}
		}

		return state
	}
}

class CurrentState()
{
	val errors = Array<String>()
	val warnings = Array<String>()

	var markedComplete: Boolean = false

	fun copy(): CurrentState
	{
		val state = CurrentState()

		return state
	}
}