package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Monster
import com.lyeeedar.Board.MonsterDesc
import com.lyeeedar.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.collections.set


class CompletionConditionKill() : AbstractCompletionCondition()
{
	var all = true
	var named: String? = null

	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	var monsters = Array<Monster>()
	var monsterMap = ObjectMap<MonsterDesc, Int>()

	val table = Table()

	lateinit var grid: Grid

	override fun attachHandlers(grid: Grid)
	{
		this.grid = grid

		grid.onDamaged += fun(c) : Boolean {
			rebuildWidget()
			return false
		}

		grid.onTurn += fun() : Boolean {
			rebuildWidget()
			return false
		}

		Future.call(
				{
					val tutorial = Tutorial("KillComplete")
					tutorial.addPopup("This is the count of enemies you need to kill to win.", table)
					tutorial.show()
				}, 0.5f)
	}

	override fun isCompleted(): Boolean = monsters.filter { it.hp > 0 }.count() == 0

	override fun parse(xml: XmlData)
	{
		all = xml.getBoolean("All", true)
		if (!all)
		{
			named = xml.get("Named")
		}
	}

	override fun createTable(grid: Grid): Table
	{
		rebuildWidget()

		return table
	}

	fun rebuildWidget()
	{
		monsters.clear()
		for (tile in grid.grid)
		{
			val monster = tile.monster ?: tile.container?.contents as? Monster
			if (monster != null)
			{
				if (all || monster.desc.name == named)
				{
					if (!monsters.contains(monster, true))
					{
						monsters.add(monster)
					}
				}
			}
		}

		table.clear()

		for (monster in monsterMap.keys().toList())
		{
			monsterMap[monster] = 0
		}

		for (monster in monsters)
		{
			if (!monsterMap.containsKey(monster.desc))
			{
				monsterMap[monster.desc] = 0
			}

			var count = monsterMap[monster.desc]
			if (monster.hp > 0)
			{
				count++
			}
			monsterMap[monster.desc] = count
		}

		var row = Table()
		var counter = 0

		for (monster in monsterMap)
		{
			val sprite = monster.key.sprite.copy()
			val count = monster.value

			row.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

			if (count == 0)
			{
				row.add(SpriteWidget(tick, 24f, 24f))
			}
			else
			{
				row.add(Label(" x $count", Global.skin))
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

		table.add(Label("Defeat all enemies.", Global.skin))

		return table
	}
}