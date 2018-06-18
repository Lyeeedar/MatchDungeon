package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Orb
import com.lyeeedar.Global
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData

class CompletionConditionCustomOrb : AbstractCompletionCondition()
{
	val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	lateinit var targetOrbName: String
	var matchCount: Int = 0
	var orbChance: Float = 0f

	val table = Table()

	override fun attachHandlers(grid: Grid)
	{
		grid.onPop += fun(orb: Orb, delay: Float) : Boolean {

			if (orb.desc.name == targetOrbName)
			{
				if (matchCount > 0) matchCount--
				rebuildWidget()
			}

			return false
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

		val targetDesc = Orb.getNamedOrb(targetOrbName)
		val sprite = targetDesc.sprite

		table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)

		if (matchCount == 0)
		{
			table.add(SpriteWidget(tick, 24f, 24f))
		}
		else
		{
			table.add(Label(" x $matchCount", Global.skin))
		}
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Match", Global.skin))

		val targetDesc = Orb.getNamedOrb(targetOrbName)
		val sprite = targetDesc.sprite

		table.add(SpriteWidget(sprite, 24f, 24f)).padLeft(5f)
		table.add(Label("x$matchCount", Global.skin))

		return table
	}
}