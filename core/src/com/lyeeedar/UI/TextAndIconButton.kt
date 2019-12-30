package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class TextAndIconButton(val text: String, val icon: Sprite, val font: BitmapFont) : Widget()
{
	var background: TextureRegion? = null

	private val scaling = Scaling.fit
	private val align = Align.center
	private var imageX: Float = 0f
	private var imageY: Float = 0f
	private var imageWidth: Float = 0f
	private var imageHeight: Float = 0f

	val borderedCircle = AssetManager.loadTextureRegion("borderedcircle")!!
	val notificationImg = AssetManager.loadTextureRegion("Icons/generic_notification")!!
	val redColour = Colour(0.85f, 0f, 0f, 1f)

	var hasNotification = false

	init
	{
		touchable = Touchable.enabled
	}

	override fun layout()
	{
		val regionWidth = icon.currentTexture.regionWidth.toFloat()
		val regionHeight = icon.currentTexture.regionHeight.toFloat()

		val size = scaling.apply(regionWidth, regionHeight, width, height)
		imageWidth = size.x
		imageHeight = size.y

		imageX = (width / 2 - imageWidth / 2).toInt().toFloat()
		imageY = (height / 2 - imageHeight / 2).toInt().toFloat()
	}

	override fun act(delta: Float)
	{
		icon.update(delta)
		super.act(delta)
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (background != null)
		{
			batch.draw(background!!, x, y, width, height)
		}

		// maintain aspect ratio

		batch.draw(icon.currentTexture, x + imageX, y + imageY, imageWidth, imageHeight)
		font.draw(batch, text, x, y + height * 0.3f, width, Align.center, false)

		if (hasNotification)
		{
			batch.setColor(redColour)
			batch.draw(borderedCircle, x + width - 16f, y + 8f, 8f, 8f)
			batch.setColor(Colour.WHITE)
			batch.draw(notificationImg, x + width - 16f, y + 8f, 8f, 8f)
		}

		super.draw(batch, parentAlpha)
	}
}