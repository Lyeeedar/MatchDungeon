package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.lyeeedar.Util.Statics

open class SpriteEffectActor(val sprite: Sprite, val w: Float, val h: Float, val pos: Vector2, var completionFunc: (() -> Unit)? = null): Actor()
{
	init
	{
		Statics.stage.addActor(this)
	}

	override fun act(delta: Float)
	{
		super.act(delta)
		val complete = sprite.update(delta)
		if (complete)
		{
			completionFunc?.invoke()
			remove()
		}
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		var x = pos.x
		var y = pos.y

		if ( sprite.animation != null )
		{
			val offset = sprite.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0]
				y += offset[1]
			}
		}

		sprite.render(batch as SpriteBatch, x, y, w, h)
	}
}
