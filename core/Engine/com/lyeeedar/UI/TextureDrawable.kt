package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable

open class TextureDrawable : BaseDrawable, TransformDrawable
{
	var texture: Texture? = null

	/** Creates an uninitialized TextureRegionDrawable. The texture region must be set before use.  */
	constructor()
	{
	}

	constructor(texture: Texture)
	{
		this.texture = texture
	}

	constructor(drawable: TextureDrawable) : super(drawable)
	{
		texture = drawable.texture
	}

	override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float)
	{
		batch!!.draw(this.texture, x, y, width, height)
	}

	override fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float)
	{
		throw NotImplementedError()
	}

	fun tint(tint: Color): SpriteDrawable
	{
		val sprite = Sprite(texture)
		sprite.color = tint
		sprite.setSize(minWidth, minHeight)
		val drawable = SpriteDrawable(sprite)
		drawable.leftWidth = leftWidth
		drawable.rightWidth = rightWidth
		drawable.topHeight = topHeight
		drawable.bottomHeight = bottomHeight
		return drawable
	}
}