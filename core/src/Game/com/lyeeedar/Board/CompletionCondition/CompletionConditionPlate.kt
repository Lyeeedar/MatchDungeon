package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.Entity
import com.lyeeedar.Game.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData

class CompletionConditionPlate : AbstractCompletionCondition()
{
	var remaining = -1
	lateinit var label: Label

	override fun attachHandlers(grid: Grid)
	{
		remaining = grid.grid.count(Tile::hasPlate)

		grid.onTurn += {
			remaining = grid.grid.count(Tile::hasPlate)
			label.setText(" x $remaining")
			false
		}

		grid.onPop += fun (orb: Entity, delay: Float ) : Boolean {
			remaining = grid.grid.count(Tile::hasPlate)
			label.setText(" x $remaining")

			return false
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("PlateComplete")
					tutorial.addPopup(Localisation.getText("completioncondition.plate.tutorial", "UI"), label)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean = remaining == 0

	override fun parse(xml: XmlData)
	{
	}

	override fun createTable(grid: Grid): Table
	{
		val table = Table()

		val sprite = grid.level.theme.plate.copy()
		label = Label(" x $remaining", Statics.skin)

		table.add(SpriteWidget(sprite, 24f, 24f))
		table.add(label)

		return table
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label(Localisation.getText("completioncondition.plate.description", "UI") + " $remaining ", Statics.skin))
		table.add(SpriteWidget(grid.level.theme.plate.copy(), 24f, 24f))

		return table
	}
}