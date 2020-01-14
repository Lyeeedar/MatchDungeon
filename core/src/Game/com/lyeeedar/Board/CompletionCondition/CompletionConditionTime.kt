package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Board.Grid
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*

/**
 * Created by Philip on 13-Jul-16.
 */

class CompletionConditionTime(): AbstractCompletionCondition()
{
	var time: Float = 60f
	var maxTime: Float = 60f

	lateinit var label: Label
	val blinkTable = Table()

	override fun createTable(grid: Grid): Table
	{
		val t = time.toInt()
		label = Label("$t\nSeconds", Statics.skin)
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
		maxTime = xml.getInt("Seconds").toFloat()
		time = maxTime
	}

	override fun attachHandlers(grid: Grid)
	{
		maxTime += (Global.player.getStat(Statistic.HASTE) * maxTime).toInt()
		time = maxTime

		grid.onTime +=
				{
					time -= it
					val t = time.toInt()
					label.setText("$t\nSeconds")

					if (time <= maxTime * 0.25f && blinkTable.children.size == 0)
					{
						val blinkSprite = AssetManager.loadSprite("Particle/glow")
						blinkSprite.colour = Colour.RED.copy().a(0.5f)
						blinkSprite.animation = ExpandAnimation.obtain().set(1f, 0.5f, 2f, false, true)
						val actor = SpriteWidget(blinkSprite, 32f, 32f)
						blinkTable.add(actor).grow()
					}

					false
				}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("Time")
					tutorial.addPopup("This is your remaining time. When it reaches 0 you will fail the level. It will only decrease between animating a turn, so act fast!", label)
					tutorial.show()
				}, 0.5f)
		}
	}

	override fun isCompleted(): Boolean = time <= 0

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		val t = time.toInt()
		table.add(Label("Within $t seconds.", Statics.skin))

		return table
	}
}
