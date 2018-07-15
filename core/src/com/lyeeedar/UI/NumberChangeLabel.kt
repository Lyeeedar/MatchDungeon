package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Util.Future

class NumberChangeLabel(caption: String, skin: Skin, style: String = "default") : Table(skin)
{
	var value: Int = 0
		set(value)
		{
			val diff = value - field
			currentChange += diff
			Future.call({ currentChange -= diff }, 1f)

			field = value
		}

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