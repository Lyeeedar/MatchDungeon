package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Board.Grid
import com.lyeeedar.Global
import com.lyeeedar.Global.Companion.skin
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 13-Jul-16.
 */

class CompletionConditionTurns(): AbstractCompletionCondition()
{
	var maxTurnCount = 30
	var turnCount: Int = maxTurnCount
	val blinkTable = Table()

	lateinit var label: Label

	override fun createTable(grid: Grid): Table
	{
		label = Label("$turnCount\nTurns", skin)
		label.setAlignment(Align.center)

		val stack = Stack()
		stack.add(blinkTable)
		stack.add(label)

		val table = Table()
		table.defaults().pad(10f)
		table.add(stack)

		return table
	}

	override fun parse(xml: XmlData)
	{
		maxTurnCount = xml.getInt("Turns")
		turnCount = maxTurnCount
	}

	override fun attachHandlers(grid: Grid)
	{
		maxTurnCount += ((Global.player.getStat(Statistic.BONUSTIME) / 100f) * maxTurnCount).toInt()
		turnCount = maxTurnCount

		grid.onTurn +=
				{
					turnCount--
					label.setText("$turnCount\nTurns")

					if (turnCount <= maxTurnCount * 0.25f && blinkTable.children.size == 0)
					{
						val blinkSprite = AssetManager.loadSprite("Particle/glow")
						blinkSprite.colour = Colour.RED.copy().a(0.5f)
						blinkSprite.animation = ExpandAnimation.obtain().set(1f, 0.5f, 2f, false, true)
						val actor = SpriteWidget(blinkSprite, 32f, 32f)
						blinkTable.add(actor).grow()
					}

					false
				}

		Future.call(
				{
					val tutorial = Tutorial("Turns")
					tutorial.addPopup("This is your remaining turn count. When it reaches 0 you will fail the level.", label)
					tutorial.show()
				}, 0.5f)
	}

	override fun isCompleted(): Boolean = turnCount <= 0

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Within $turnCount turns.", Global.skin))

		return table
	}
}
