package com.lyeeedar.Board.CompletionCondition

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.MonsterAI
import com.lyeeedar.Board.isMonster
import com.lyeeedar.Components.ai
import com.lyeeedar.Components.container
import com.lyeeedar.Components.damageable
import com.lyeeedar.Components.renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.set


class CompletionConditionKill() : AbstractCompletionCondition()
{
	var all = true
	var named: String? = null

	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	val monsters = ObjectSet<Entity>()
	val monsterCountMap = ObjectMap<String, Int>()
	val monsterSpriteMap = ObjectMap<String, Sprite>()

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

	override fun isCompleted(): Boolean = monsters.filter { it.damageable()!!.hp > 0 }.count() == 0

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
			val contents = tile.contents ?: continue

			var monsterEntity: Entity? = null
			if (contents.isMonster())
			{
				monsterEntity = contents
			}
			else if (contents.container()?.containedEntity?.isMonster() == true)
			{
				monsterEntity = contents.container()!!.containedEntity!!
			}

			if (monsterEntity != null)
			{
				val name = (monsterEntity.ai()!!.ai as MonsterAI).desc.name
				if (all || name == named)
				{
					monsters.add(monsterEntity)
				}
			}
		}

		table.clear()

		for (monster in monsterCountMap.keys().toList())
		{
			monsterCountMap[monster] = 0
		}

		for (monster in monsters)
		{
			val ai = monster.ai()!!.ai as MonsterAI

			val desc = ai.desc.originalDesc ?: ai.desc

			if (!monsterCountMap.containsKey(desc.name))
			{
				monsterCountMap[desc.name] = 0
			}

			var count = monsterCountMap[desc.name]
			if (monster.damageable()!!.hp > 0)
			{
				count++
			}
			monsterCountMap[desc.name] = count

			monsterSpriteMap[desc.name] = monster.renderable().renderable as Sprite
		}

		var row = Table()
		var counter = 0

		for (monster in monsterCountMap)
		{
			val sprite = monsterSpriteMap[monster.key].copy()
			val count = monster.value

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

		table.add(Label("Defeat all enemies.", Statics.skin))

		return table
	}
}