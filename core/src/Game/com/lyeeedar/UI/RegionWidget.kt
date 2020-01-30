package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Game.Region
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics

class RegionWidget(val region: Region) : Widget()
{
	var tileSize = 32f
		set(value)
		{
			field = value
			renderer.tileSize = value
		}

	val TILE = 0
	val SPREADER = TILE+1
	val ORB = SPREADER+1
	val EFFECT = ORB+1

	val renderer: SortedRenderer by lazy{ SortedRenderer(tileSize, region.grid.width.toFloat(), region.grid.height.toFloat(), EFFECT + 1, true) }

	override fun invalidate()
	{
		super.invalidate()

		val w = width / (region.grid.width.toFloat() - 2f)
		val h = (height - 16f) / (region.grid.height.toFloat() - 2f)

		tileSize = Math.min(w, h)

		renderedStatic = false
	}

	var renderedStatic = false
	var renderY = 0f
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val trace = Statics.performanceTracer.getTrace("RegionWidgetRender")
		trace.start()

		batch.end()
		batch.color = Color.WHITE

		val xp = this.x + (this.width / 2f) - (((region.grid.width - 2) * tileSize) / 2f) - tileSize
		val yp = this.y + (this.height / 2f) - (((region.grid.height - 2) * tileSize) / 2f) - tileSize
		renderY = yp

		if (!renderedStatic)
		{
			renderer.beginStatic()

			for (x in 0 until region.grid.width)
			{
				for (y in 0 until region.grid.height)
				{
					val tile = region.grid[x, y]

					val xi = x.toFloat()
					val yi = (region.grid.height - 1) - y.toFloat()

					val rendererSprite = tile.sprite
					if (!rendererSprite.hasChosenSprites)
					{
						rendererSprite.chooseSprites()
					}

					val sprite = rendererSprite.chosenSprite
					if (sprite != null)
					{
						renderer.queueSprite(sprite, xi, yi)
					}

					val tilingSprite = rendererSprite.chosenTilingSprite
					if (tilingSprite != null)
					{
						renderer.queueSprite(tilingSprite, xi, yi)
					}
				}
			}

			renderer.endStatic()
			renderedStatic = true
		}

		val delta = Gdx.app.graphics.deltaTime
		renderer.begin(delta, xp, yp, Colour.WHITE)
		renderer.end(batch)

		batch.begin()

		trace.stop()
	}
}