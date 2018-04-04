package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Board.Grid
import com.lyeeedar.Global
import com.lyeeedar.Global.Companion.skin
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 13-Jul-16.
 */

class CompletionConditionTurns(): AbstractCompletionCondition()
{
	var turnCount: Int = 30

	lateinit var label: Label

	override fun createTable(grid: Grid): Table
	{
		label = Label("$turnCount\nTurns", skin)
		label.setAlignment(Align.center)

		val table = Table()
		table.defaults().pad(10f)
		table.add(label)

		return table
	}

	override fun parse(xml: XmlData)
	{
		turnCount = xml.getInt("Turns")
	}

	override fun attachHandlers(grid: Grid)
	{
		grid.onTurn +=
				{
					turnCount--
					label.setText("$turnCount\nTurns")
					false
				}
	}

	override fun isCompleted(): Boolean = turnCount <= 0

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Within $turnCount turns.", Global.skin))

		return table
	}
}
