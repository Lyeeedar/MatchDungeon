package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Mote
import com.lyeeedar.Global
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.Util.XmlData

class CompletionConditionDie : AbstractCompletionCondition()
{
	lateinit var hpLabel: Label
	var maxHP: Int = 1
	var hp = 1

	override fun attachHandlers(grid: Grid)
	{
		maxHP = grid.level.player.maxhp
		hp = maxHP

		grid.onAttacked += fun(c): Boolean {

			val sprite = c.sprite.copy()
			val dst = hpLabel.localToStageCoordinates(Vector2())
			val src = GridWidget.instance.pointToScreenspace(c)

			Mote(src, dst, sprite, GridWidget.instance.tileSize, { if (hp > 0) hp--; hpLabel.setText("$hp/$maxHP") })

			return false
		}
	}

	override fun isCompleted(): Boolean = hp <= 0

	override fun parse(xml: XmlData)
	{

	}

	override fun createTable(grid: Grid): Table
	{
		val table = Table()
		hpLabel = Label("$hp/$maxHP", Global.skin)
		table.add(hpLabel)

		return table
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Don't let your hp reach 0.", Global.skin))

		return table
	}
}