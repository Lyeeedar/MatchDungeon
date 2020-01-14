package com.lyeeedar.Board.CompletionCondition

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Components.matchable
import com.lyeeedar.Game.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*

class CompletionConditionCustomOrb : AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	lateinit var targetOrbName: String
	var matchCount: Int = 0
	var orbChance: Float = 0f

	val table = Table()

	override fun attachHandlers(grid: Grid)
	{
		grid.onPop += fun(orb: Entity, delay: Float) : Boolean {

			if (orb.matchable()?.desc?.name == targetOrbName)
			{
				if (matchCount > 0) matchCount--
				rebuildWidget()
			}

			return false
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("CustomOrbComplete")
					tutorial.addPopup("This is the count you need to match to win.", table)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean
	{
		return matchCount == 0
	}

	override fun parse(xml: XmlData)
	{
		matchCount = xml.getInt("Count")
		targetOrbName = xml.get("OrbName")
		orbChance = xml.getFloat("Chance")
	}

	override fun createTable(grid: Grid): Table
	{
		rebuildWidget()

		return table
	}

	fun rebuildWidget()
	{
		table.clear()

		val targetDesc = OrbDesc.getNamedOrb(targetOrbName)
		val sprite = targetDesc.sprite

		table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

		if (matchCount == 0)
		{
			table.add(SpriteWidget(tick, 24f, 24f))
		}
		else
		{
			table.add(Label(" x $matchCount", Statics.skin))
		}
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Match", Statics.skin))

		val targetDesc = OrbDesc.getNamedOrb(targetOrbName)
		val sprite = targetDesc.sprite

		table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)
		table.add(Label("x$matchCount", Statics.skin))

		return table
	}
}