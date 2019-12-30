package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin

class ScrollingTextLabel(text: String, val skin: Skin, style: String = "default") : Label(text, skin, style)
{
	init
	{
		setWrap(true)
	}

	override fun setText(newText: CharSequence?)
	{
		super.setText(newText)
		currentTextIndex = 0
		textAccumulator = 0f
	}

	private var currentTextIndex: Int = 0
	private var textAccumulator: Float = 0f

	var isComplete: Boolean
		get() = currentTextIndex == text.count()
		set(value)
		{
			if (value)
			{
				currentTextIndex = text.count()
			}
		}

	override fun act(delta: Float)
	{
		if (currentTextIndex != text.count())
		{
			textAccumulator += delta
			while (textAccumulator > 0.02f)
			{
				textAccumulator -= 0.02f
				currentTextIndex++
				if (currentTextIndex > text.count())
				{
					currentTextIndex = text.count()
					break
				}
			}
		}

		super.act(delta)
	}

	private val tempColour: Color = Color()
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (currentTextIndex == 0) return

		validate()
		val color = tempColour.set(color)
		color.a *= parentAlpha
		if (style.background != null)
		{
			batch!!.setColor(color.r, color.g, color.b, color.a)
			style.background.draw(batch, x, y, width, height)
		}
		if (style.fontColor != null) color.mul(style.fontColor)
		val cache = bitmapFontCache
		cache.tint(color)
		cache.setPosition(x, y)
		cache.draw(batch, 0, currentTextIndex)
	}
}