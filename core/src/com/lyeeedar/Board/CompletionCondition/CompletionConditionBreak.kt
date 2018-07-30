package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Block
import com.lyeeedar.Board.Grid
import com.lyeeedar.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class CompletionConditionBreak : AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	var blocks = Array<Block>()
	var blockMap = ObjectMap<String, Int>()

	val table = Table()

	override fun attachHandlers(grid: Grid)
	{
		for (tile in grid.grid)
		{
			if (tile.block != null || tile.container?.contents is Block)
			{
				val block = tile.block ?: tile.container!!.contents as Block
				if (!blocks.contains(block, true))
				{
					blocks.add(block)
				}
			}
		}

		grid.onDamaged += fun(c) : Boolean {
			rebuildWidget()
			return false
		}

		Future.call(
				{
					val tutorial = Tutorial("BlockComplete")
					tutorial.addPopup("This is the count of blocks you need to break to win.", table)
					tutorial.show()
				}, 0.5f)
	}

	override fun isCompleted(): Boolean = blocks.filter { it.hp > 0 }.count() == 0

	override fun parse(xml: XmlData)
	{
	}

	override fun createTable(grid: Grid): Table
	{
		rebuildWidget()

		return table
	}

	fun rebuildWidget()
	{
		table.clear()

		blockMap.clear()

		for (block in blocks)
		{
			if (!blockMap.containsKey(block.sprite.fileName))
			{
				blockMap[block.sprite.fileName] = 0
			}

			var count = blockMap[block.sprite.fileName]
			if (block.hp > 0)
			{
				count++
			}
			blockMap[block.sprite.fileName] = count
		}

		for (block in blockMap)
		{
			val sprite = AssetManager.loadSprite(block.key)
			sprite.drawActualSize = true
			val count = block.value

			table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

			if (count == 0)
			{
				table.add(SpriteWidget(tick, 24f, 24f))
			}
			else
			{
				table.add(Label(" x $count", Global.skin))
			}

			table.row()
		}
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Break all blocks.", Global.skin))

		return table
	}
}