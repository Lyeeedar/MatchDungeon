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

class CompletionConditionTime(): AbstractCompletionCondition()
{
	var time: Float = 60f

	lateinit var label: Label

	override fun createTable(grid: Grid): Table
	{
		val t = time.toInt()
		label = Label("$t\nSeconds", skin)
		label.setAlignment(Align.center)

		val table = Table()
		table.defaults().pad(10f)
		table.add(label)

		return table
	}

	override fun parse(xml: XmlData)
	{
		time = xml.getInt("Seconds").toFloat()
	}

	override fun attachHandlers(grid: Grid)
	{
		grid.onTime +=
				{
					time -= it
					val t = time.toInt()
					label.setText("$t\nSeconds")
					false
				}
	}

	override fun isCompleted(): Boolean = time <= 0

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		val t = time.toInt()
		table.add(Label("Within $t seconds.", Global.skin))

		return table
	}
}
