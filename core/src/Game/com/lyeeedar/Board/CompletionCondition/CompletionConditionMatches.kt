package com.lyeeedar.Board.CompletionCondition

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntIntMap
import com.badlogic.gdx.utils.IntMap
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Components.matchable
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.get
import ktx.collections.set

/**
 * Created by Philip on 13-Jul-16.
 */

class CompletionConditionMatches(): AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	val toBeMatched = IntIntMap()
	val sprites = IntMap<Sprite>()
	val table = Table()

	override fun createTable(grid: Grid): Table
	{
		rebuildWidget()

		return table
	}

	fun rebuildWidget()
	{
		table.clear()

		var row = Table()
		var counter = 0

		for (entry in toBeMatched.entries())
		{
			val sprite = sprites[entry.key]
			val count = entry.value

			row.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

			if (count == 0)
			{
				row.add(SpriteWidget(tick, 24f, 24f))
			}
			else
			{
				row.add(Label("$count", Statics.skin))
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

	override fun attachHandlers(grid: Grid)
	{
		val entries = toBeMatched.toList()
		toBeMatched.clear()
		for (entry in entries)
		{
			val valid = Array<Int>()
			for (orb in OrbDesc.getValidOrbs(grid.level)) if (!toBeMatched.containsKey(orb.key)) valid.add(orb.key)

			if (valid.size > 0)
			{
				toBeMatched[valid.random(grid.ran)] = entry.value
			}
		}

		grid.onPop += fun(orb: Entity, delay: Float) : Boolean {
			val matchable = orb.matchable()!!
			if (toBeMatched.containsKey(matchable.desc.key))
			{
				var count = toBeMatched[matchable.desc.key]
				if (count > 0) count--
				toBeMatched[matchable.desc.key] = count

				rebuildWidget()
			}

			return false
		}

		for (entry in toBeMatched.entries())
		{
			sprites.put(entry.key, OrbDesc.getOrb(entry.key).sprite)
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("MatchComplete")
					tutorial.addPopup("This is the count of orbs of each colour you need to match to win.", table)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean
	{
		var done = true
		for (entry in toBeMatched.entries())
		{
			if (entry.value > 0)
			{
				done = false
				break
			}
		}

		return done
	}

	override fun parse(xml: XmlData)
	{
		for (i in 0..xml.childCount-1)
		{
			val el = xml.getChild(i)
			val count = el.text.toInt()

			toBeMatched[i] = count
		}
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Match", Statics.skin))

		for (entry in toBeMatched.entries())
		{
			val sprite = sprites[entry.key]
			val count = entry.value

			table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)
			table.add(Label("x$count", Statics.skin))
		}

		return table
	}
}