package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 07-Aug-16.
 */

class CompletionConditionNone() : AbstractCompletionCondition()
{
	override fun createTable(grid: Grid): Table = Table()

	override fun getDescription(grid: Grid): Table = Table()

	override fun attachHandlers(grid: Grid)
	{

	}

	override fun isCompleted(): Boolean = false

	override fun parse(xml: XmlData)
	{
	}
}