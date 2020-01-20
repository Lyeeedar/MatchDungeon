package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.XmlData
import java.util.*

abstract class AbstractCompletionCondition
{
	abstract fun attachHandlers(grid: Grid)
	abstract fun isCompleted(): Boolean
	abstract fun parse(xml: XmlData)
	abstract fun createTable(grid: Grid): Table
	abstract fun getDescription(grid: Grid): Table

	companion object
	{
		fun load(xml: XmlData): AbstractCompletionCondition
		{
			val obj = get(xml.getAttribute("meta:RefKey"))
			obj.parse(xml)

			return obj
		}

		private fun get(name: String): AbstractCompletionCondition
		{
			val uname = name.toUpperCase(Locale.ENGLISH)
			val instance = when(uname)
			{
				"NONE" -> CompletionConditionNone()

			// Defeat
				"DIE" -> CompletionConditionDie()
				"TURN", "TURNS" -> CompletionConditionTurns()
				"TIME" -> CompletionConditionTime()

			// Victory
				"KILL" -> CompletionConditionKill()
				"MATCH", "MATCHES" -> CompletionConditionMatches()
				"SINK" -> CompletionConditionSink()
				"PLATE" -> CompletionConditionPlate()
				"CUSTOMORB" -> CompletionConditionCustomOrb()
				"BREAK" -> CompletionConditionBreak()

			// ARGH everything broke
				else -> throw RuntimeException("Invalid completion condition type: $name")
			}

			return instance
		}
	}
}