package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Orb
import com.lyeeedar.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.XmlData

class CompletionConditionBreak : AbstractCompletionCondition()
{
	var remaining = -1
	lateinit var label: Label

	override fun attachHandlers(grid: Grid)
	{
		remaining = grid.grid.count{ it.block != null }

		grid.onTurn += {
			remaining = grid.grid.count{ it.block != null }
			label.setText("$remaining")
			false
		}

		grid.onPop += fun(orb: Orb, delay: Float) : Boolean {
			remaining = grid.grid.count{ it.block != null }
			label.setText("$remaining")

			return false
		}
	}

	override fun isCompleted(): Boolean = remaining == 0

	override fun parse(xml: XmlData)
	{
	}

	override fun createTable(grid: Grid): Table
	{
		val table = Table()

		val sprite = grid.level.theme.blockSprites[0].copy()
		label = Label(" x $remaining", Global.skin)

		table.add(SpriteWidget(sprite, 24f, 24f))
		table.add(label)

		return table
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Break $remaining ", Global.skin))
		table.add(SpriteWidget(grid.level.theme.blockSprites[0].copy(), 24f, 24f))

		return table
	}
}