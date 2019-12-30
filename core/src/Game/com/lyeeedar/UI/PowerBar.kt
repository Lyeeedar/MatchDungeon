package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Event0Arg

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
	val drain = AssetManager.loadTextureRegion("GUI/power_drain")

	val pipPadding = 0f

	val powerPerPip = 15
	var maxPower = 225

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

			if (value < field)
			{
				powerDrain += field - value
			}
			else
			{
				val diff = value - field
				powerDrain -= diff
				if (powerDrain < 0) powerDrain = 0
			}

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
	var powerDrain = 0
	var drainAccumulator = 0f

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
		batch.color = Color.WHITE

		val numPips = (maxPower.toFloat() / powerPerPip.toFloat()).toInt()
		val pw = pipWidth

		val minipipWidth = pw / powerPerPip
		val minipipHeight = height * 0.2f

		var powerCounter = 0

		if (powerDrain > 0)
		{
			drainAccumulator += Gdx.app.graphics.deltaTime

			while (drainAccumulator > 0.05f && powerDrain > 0)
			{
				drainAccumulator -= 0.05f

				powerDrain--
			}

			drainAccumulator = 0f
		}

		for (i in 0 until numPips)
		{
			val powerDiff = power - powerCounter

			val sprite = if(powerDiff >= powerPerPip) full else empty

			val xpos = x + pw * i + pipPadding * i
			batch.draw(sprite, xpos, y + minipipHeight + 1, pw, height - minipipHeight)

			for (pi in 0 until powerPerPip)
			{
				val ps = when {
					pi < powerDiff -> full
					pi < powerDiff + powerDrain -> drain
					else -> empty
				}

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