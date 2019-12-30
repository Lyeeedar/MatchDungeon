package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class PercentageBarWidget(var percentage: Float, var colour: Colour, val numPips: Int) : Widget()
{
	val white = AssetManager.loadTextureRegion("white")!!
	val hp_border = AssetManager.loadTextureRegion("Sprites/GUI/health_border.png")!!

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val totalWidth = width-10f

		val solidSpaceRatio = 0.05f
		val space = totalWidth
		val spacePerPip = space / numPips
		val spacing = spacePerPip * solidSpaceRatio
		val solid = spacePerPip - spacing

		batch.setColor(0f, 0f, 0f, parentAlpha)
		batch.draw(white, x+5f, y, totalWidth, height)

		batch.setColor(colour.r, colour.g, colour.b, colour.a * parentAlpha)
		batch.draw(white, x+5f, y, totalWidth*percentage, height)

		batch.setColor(1f, 1f, 1f, parentAlpha)
		for (i in 0 until numPips)
		{
			batch.draw(hp_border, x+5f+i*spacePerPip, y, solid, height)
		}
	}
}