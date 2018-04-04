package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Mote
import com.lyeeedar.Global
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 22-Jul-16.
 */

class CompletionConditionSink() : AbstractCompletionCondition()
{
	var count = 5

	lateinit var label: Label

	override fun attachHandlers(grid: Grid)
	{
		grid.onSunk += {

			val sprite = it.sprite.copy()
			val dst = label.localToStageCoordinates(Vector2())
			val src = GridWidget.instance.pointToScreenspace(it)

			Mote(src, dst, sprite, GridWidget.instance.tileSize, { if (count > 0) count--; label.setText("$count") })

			false
		}
	}

	override fun isCompleted(): Boolean = count == 0

	override fun parse(xml: XmlData)
	{
		count = xml.getInt("Count")
	}

	override fun createTable(grid: Grid): Table
	{
		val table = Table()

		val sprite = grid.level.theme.coin.copy()
		label = Label("$count", Global.skin)

		table.add(SpriteWidget(sprite, 24f, 24f))
		table.add(label)

		return table
	}

	override fun getDescription(grid: Grid) : Table
	{
		val table = Table()

		val sprite = grid.level.theme.coin.copy()

		table.add(Label("Move $count ", Global.skin))
		table.add(SpriteWidget(sprite, 24f, 24f))
		table.add(Label(" to the bottom.", Global.skin))

		return table
	}
}