package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.lyeeedar.Global

open class ParticleEffectActor(val particle: ParticleEffect, val tileSize: Float, val pos: Vector2, var completionFunc: (() -> Unit)? = null): Actor()
{
	init
	{
		Global.stage.addActor(this)
	}

	override fun act(delta: Float)
	{
		super.act(delta)
		val complete = particle.update(delta)
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

		if ( particle.animation != null )
		{
			val offset = particle.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0]
				y += offset[1]
			}
		}

		particle.render(batch as SpriteBatch, x, y, tileSize)
	}
}
