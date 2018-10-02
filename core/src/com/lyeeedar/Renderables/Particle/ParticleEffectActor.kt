package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.lyeeedar.Global
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Util.Colour

open class ParticleEffectActor(val particle: ParticleEffect, var completionFunc: (() -> Unit)? = null): Actor()
{
	val renderer: SortedRenderer
	val colour = Colour()

	init
	{
		Global.stage.addActor(this)
		renderer = SortedRenderer(1f, stage.width, stage.height, 1, true)
	}

	override fun act(delta: Float)
	{
		particle.setPosition(x, y)

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

		colour.set(color)
		colour.a *= parentAlpha

		renderer.begin(0f, 0f, 0f)
		renderer.queueParticle(particle, x, y, 0, 0, colour, width, height)
		renderer.flush(batch!!)
	}
}
