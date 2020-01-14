package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Tile
import com.lyeeedar.Board.createSinkable
import com.lyeeedar.Board.spawnMote
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class CompletionConditionSink() : AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	var count = 5

	val table = Table()

	val sinkableMap = ObjectMap<String, SinkableData>()
	lateinit var grid: Grid

	override fun attachHandlers(grid: Grid)
	{
		this.grid = grid
		grid.onSunk += {

			val sprite = it.renderable().renderable.copy() as Sprite
			val dst = table.localToStageCoordinates(Vector2(table.width / 2f, table.height / 2f))
			val src = GridWidget.instance.pointToScreenspace(it.pos().tile!!)

			spawnMote(src, dst, sprite, GridWidget.instance.tileSize, {
				sinkableMap[sprite.fileName].count = max(0, sinkableMap[sprite.fileName].count-1)
				rebuildWidget()
			})

			false
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("Sink")
					tutorial.addPopup("This is the count of items you need to get to the bottom of the board to win. If you can't see them on the board then they'll be in chests, inside blocks or in enemies.", table)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean = sinkableMap.all { it.value.count == 0 }

	override fun parse(xml: XmlData)
	{
		count = xml.getInt("Count")
	}

	override fun createTable(grid: Grid): Table
	{
		this.grid = grid

		for (tile in grid.grid)
		{
			val sinkable = tile.getContentsOrContainer { it.sinkable() != null } ?: continue

			val data: SinkableData
			if (sinkableMap.containsKey(sinkable.sprite()!!.fileName))
			{
				data = sinkableMap[sinkable.sprite()!!.fileName]
			}
			else
			{
				data = SinkableData(sinkable.sprite()!!, 0)
				sinkableMap[sinkable.sprite()!!.fileName] = data
			}

			data.count++
		}

		val total = sinkableMap.map { it.value.count }.sum()
		if (total < count)
		{
			// Check if level has chests and make up the rest with coins
			val coins = count - total

			if (grid.grid.any { it.getContentsOrContainer { it.orbSpawner()?.canSpawnSinkables == true } != null })
			{
				// no need to do anything fancy, the chests will spawn them
			}
			else
			{
				// add coins to the grid randomly
				val valid = grid.grid.filter {
					it.x > 2 && it.x < grid.grid.width-2 && it.y > 2 && it.y < grid.grid.height-2 &&
					it.canHaveOrb &&
					it.contents!!.isBasicOrb()
				}.toSet().toGdxArray()

				for (i in 0 until coins)
				{
					var chosen: Tile? = null
					while (valid.size > 0)
					{
						chosen = valid.removeRandom(Random.random)
						if (chosen.contents?.isBasicOrb() == true)
						{
							break
						}
					}

					if (chosen != null)
					{
						chosen.contents = createSinkable(grid.level.theme.coin.copy())
					}
				}
			}

			sinkableMap[grid.level.theme.coin.fileName] = SinkableData(grid.level.theme.coin, coins)
		}
		else
		{
			count = total
		}

		rebuildWidget()
		return table
	}

	fun rebuildWidget()
	{
		table.clear()
		var row = Table()
		var counter = 0

		for (pair in sinkableMap)
		{
			val sprite = pair.value.sprite.copy()
			val count = pair.value.count

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

	override fun getDescription(grid: Grid) : Table
	{
		val table = Table()

		// This is the default
		var sprite = grid.level.theme.coin.copy()

		// check if any sinkables exist in the level already, use their sprite instead if they do
		for (tile in grid.sinkableTiles)
		{
			val sinkable = tile.getContentsOrContainer { it.sinkable() != null }
			sprite = sinkable!!.sprite()!!.copy()
		}

		table.add(Label("Move $count ", Statics.skin))
		table.add(SpriteWidget(sprite, 24f, 24f))
		table.add(Label(" to the bottom.", Statics.skin))

		return table
	}
}

data class SinkableData(val sprite: Sprite, var count: Int)