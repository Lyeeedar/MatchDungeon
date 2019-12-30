package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.max
import com.lyeeedar.Util.prettyPrint
import java.lang.Math.abs

class NumberChangeLabel(caption: String, skin: Skin, style: String = "default") : Table(skin)
{
	var value: Int = 0
		set(value)
		{
			val diff = value - field
			currentChange += diff

			currentMaxChange = max(abs(currentChange), currentMaxChange)

			val changeToken = Any()
			Future.call({ currentChange -= diff; changeTokens.removeValue(changeToken, true) }, 1f, changeToken)
			changeTokens.add(changeToken)

			field = value
		}
	val changeTokens = Array<Any>()

	private var currentValue = 0f
	private var currentMaxChange = 0

	private var currentChange = 0
	private var lastChange = 0

	private val captionLabel: Label = Label(caption, skin, style)
	private val numberLabel: Label = Label("0", skin, style)
	private val changeLabel: Label = Label("", skin, style)

	init
	{
		add(captionLabel)
		add(numberLabel)
		add(changeLabel)
	}

	fun complete()
	{
		currentValue = value.toFloat()
		currentMaxChange = 0

		numberLabel.setText(currentValue.toInt().prettyPrint())
		currentChange = 0
		lastChange = 0
		changeLabel.setText("")

		for (token in changeTokens)
		{
			Future.cancel(token)
		}
		changeTokens.clear()
	}

	override fun act(delta: Float)
	{
		super.act(delta)

		if (currentValue.toInt() != value)
		{
			// cover max change in 1.5 seconds
			val changeRate = currentMaxChange / 1.5f
			val change = changeRate * delta

			if (currentValue < value)
			{
				currentValue += change
				if (currentValue > value)
				{
					currentValue = value.toFloat()
				}
			}
			else if (currentValue > value)
			{
				currentValue -= change
				if (currentValue < value)
				{
					currentValue = value.toFloat()
				}
			}

			numberLabel.setText(currentValue.toInt().prettyPrint())
		}

		if (currentChange != lastChange)
		{
			if (currentChange > 0)
			{
				changeLabel.setText("[GREEN]+${currentChange.prettyPrint()}[]")
			}
			else if (currentChange < 0)
			{
				changeLabel.setText("[RED]${currentChange.prettyPrint()}[]")
			}
			else
			{
				changeLabel.setText("")
			}

			lastChange = currentChange
		}
	}
}