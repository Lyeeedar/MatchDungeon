package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.Future

class NumberChangeLabel(caption: String, skin: Skin, style: String = "default") : Table(skin)
{
	var value: Int = 0
		set(value)
		{
			val diff = value - field
			currentChange += diff

			val changeToken = Any()
			Future.call({ currentChange -= diff; changeTokens.removeValue(changeToken, true) }, 1f, changeToken)
			changeTokens.add(changeToken)

			field = value
		}
	val changeTokens = Array<Any>()

	private var currentValue = 0
	private var currentChange = 0
	private var lastChange = 0

	private val captionLabel: Label = Label(caption, skin, style)
	private val numberLabel: Label = Label("0", skin, style)
	private val changeLabel: Label = Label("", skin, style)

	private var changeAccumulator = 0f
	private val changeSpeed = 0.01f

	init
	{
		add(captionLabel)
		add(numberLabel)
		add(changeLabel)
	}

	fun complete()
	{
		currentValue = value
		numberLabel.setText(currentValue.toString())
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

		if (currentValue != value)
		{
			changeAccumulator += delta
			while (changeAccumulator > changeSpeed && currentValue != value)
			{
				changeAccumulator -= changeSpeed

				if (currentValue < value)
				{
					currentValue++
				}
				else
				{
					currentValue--
				}
			}

			numberLabel.setText(currentValue.toString())
		}

		if (currentChange != lastChange)
		{
			if (currentChange > 0)
			{
				changeLabel.setText("[GREEN]+$currentChange[]")
			}
			else if (currentChange < 0)
			{
				changeLabel.setText("[RED]$currentChange[]")
			}
			else
			{
				changeLabel.setText("")
			}

			lastChange = currentChange
		}
	}
}