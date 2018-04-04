package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Global
import com.lyeeedar.Util.*

/**
 * Created by Philip on 19-Jul-16.
 */

class PowerBar() : Widget()
{
	init
	{
		instance = this
	}

	val powerChanged = Event0Arg()

	val empty = AssetManager.loadTextureRegion("GUI/power_empty")
	val full = AssetManager.loadTextureRegion("GUI/power_full")

	val pipPadding = 0f

	val powerPerPip = 10
	var maxPower = 100

	val pipWidth: Float
		get()
		{
			val numPips = (maxPower / powerPerPip).toFloat()
			val widthWithoutPadding = width - numPips * pipPadding
			return widthWithoutPadding / numPips
		}

	var tempPower = 0

	var pips: Int
		set(value)
		{
			val oldCount = pips
			val diff = oldCount - value
			val asPower = diff * powerPerPip

			power -= asPower
		}
		get() = (power.toFloat() / powerPerPip.toFloat()).toInt()

	var power = 0
		set(value)
		{
			tempPower--

			if (value < maxPower)
			{
				field = value
			}
			else
			{
				field = maxPower
			}

			powerChanged()
		}

	fun getOrbDest(): Vector2?
	{
		if (power+tempPower >= maxPower)
		{
			return null
		}

		val dy = -0.5f

		val minipipWidth = pipWidth / powerPerPip
		val destPipVal = (power + tempPower).toFloat()

		val dx = destPipVal * minipipWidth

		tempPower++

		return localToStageCoordinates(Vector2(dx, dy))
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (batch == null) return

		val numPips = (maxPower.toFloat() / powerPerPip.toFloat()).toInt()
		val pw = pipWidth

		val minipipWidth = pw / powerPerPip
		val minipipHeight = height * 0.2f

		var powerCounter = 0

		for (i in 0..numPips-1)
		{
			val powerDiff = power - powerCounter

			val sprite = if(powerDiff >= powerPerPip) full else empty

			val xpos = x + pw * i + pipPadding * i
			batch.draw(sprite, xpos, y + minipipHeight + 1, pw, height - minipipHeight)

			for (pi in 0..powerPerPip-1)
			{
				val ps = if(pi < powerDiff) full else empty
				batch.draw(ps, xpos + pi * minipipWidth, y, minipipWidth, minipipHeight)
			}

			powerCounter += powerPerPip
		}
	}

	companion object
	{
		lateinit var instance: PowerBar
	}
}