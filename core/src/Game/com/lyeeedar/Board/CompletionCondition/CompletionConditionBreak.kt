package com.lyeeedar.Board.CompletionCondition

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.Grid
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.set

class CompletionConditionBreak : AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	var blocks = ObjectSet<Entity>()
	var blockMap = ObjectMap<String, Int>()

	val table = Table()

	override fun attachHandlers(grid: Grid)
	{
		for (tile in grid.grid)
		{
			if (tile.contents?.archetype()?.archetype == EntityArchetype.BLOCK)
			{
				blocks.add(tile.contents!!)
			}
			else if (tile.contents?.container()?.containedEntity?.archetype()?.archetype == EntityArchetype.BLOCK)
			{
				blocks.add(tile.contents!!.container()!!.containedEntity!!)
			}
		}

		grid.onDamaged += fun(c) : Boolean {
			rebuildWidget()
			return false
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("BlockComplete")
					tutorial.addPopup("This is the count of blocks you need to break to win.", table)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean = blocks.filter { it.damageable()!!.hp > 0 }.count() == 0

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
			val sprite = block.renderable().renderable as Sprite

			if (!blockMap.containsKey(sprite.fileName))
			{
				blockMap[sprite.fileName] = 0
			}

			var count = blockMap[sprite.fileName]
			if (block.damageable()!!.hp > 0)
			{
				count++
			}
			blockMap[sprite.fileName] = count
		}

		var row = Table()
		var counter = 0

		for (block in blockMap)
		{
			val sprite = AssetManager.loadSprite(block.key)
			sprite.drawActualSize = true
			val count = block.value

			row.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

			if (count == 0)
			{
				row.add(SpriteWidget(tick, 24f, 24f))
			}
			else
			{
				row.add(Label(" x $count", Statics.skin))
			}

			counter++
			if (counter == 2)
			{
				counter = 0
				table.add(row).expand().fill()
				table.row()
				row = Table()
			}
		}

		table.add(row)
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Break all blocks.", Statics.skin))

		return table
	}
}