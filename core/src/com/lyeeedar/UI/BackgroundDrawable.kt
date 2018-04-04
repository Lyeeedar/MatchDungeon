package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable

open class BackgroundDrawable : BaseDrawable, TransformDrawable
{
	var texture: Texture? = null

	var parallaxFactor = 0f // -1 -> 1

	/** Creates an uninitialized TextureRegionDrawable. The texture region must be set before use.  */
	constructor()
	{
	}

	constructor(texture: Texture)
	{
		this.texture = texture
	}

	constructor(drawable: BackgroundDrawable) : super(drawable)
	{
		texture = drawable.texture
	}

	override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float)
	{
		val xratio = texture!!.width / width
		val yratio = texture!!.height / height

		val useX = xratio < yratio

		var newWidth: Float
		var newHeight: Float
		if (useX)
		{
			newWidth = texture!!.width / xratio
			newHeight = texture!!.height / xratio
		}
		else
		{
			newWidth = texture!!.width / yratio
			newHeight = texture!!.height / yratio
		}

		var xdiff = newWidth - width

		if (xdiff < 50)
		{
			val factor = 1f + (50.0f / newWidth)
			newWidth *= factor
			newHeight *= factor
			xdiff = newWidth - width
		}

		val ydiff = newHeight - height

		batch!!.draw(this.texture, x - (xdiff / 3) - (xdiff / 3) * parallaxFactor, y - (ydiff / 2), newWidth, newHeight)
	}

	override fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float)
	{
		throw NotImplementedError()
	}
}