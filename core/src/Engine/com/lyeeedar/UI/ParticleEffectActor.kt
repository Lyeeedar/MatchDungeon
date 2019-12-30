package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.lerp
import com.lyeeedar.Util.min

class ParticleEffectActor(val effect: ParticleEffect, val removeOnCompletion: Boolean = true, var completionFunc: (() -> Unit)? = null) : Widget()
{
	init
	{
		touchable = Touchable.disabled
	}

	private val tempVec2 = Vector2()

	var acted = false
	override fun act(delta: Float)
	{
		acted = true

		var lx = x + width / 2
		var ly = y + height / 2

		if (effect.lockPosition)
		{

		}
		else
		{
			if (effect.facing.x != 0)
			{
				lx = x + effect.size[1].toFloat() * 0.5f * width
				ly = y + effect.size[0].toFloat() * 0.5f * height
			}
			else
			{
				if (effect.isCentered)
				{
					lx = x + 0.5f * width
					ly = y + 0.5f * height
				}
				else
				{
					lx = x + effect.size[0].toFloat() * 0.5f * width
					ly = y + effect.size[1].toFloat() * 0.5f * height
				}
			}

			effect.setPosition(lx / width, ly / height)
		}

		effect.update(delta)

		if (effect.completed && effect.complete())
		{
			completionFunc?.invoke()

			if (removeOnCompletion)
			{
				remove()
			}
			else
			{
				effect.start()
			}
		}

		if (effect.faceInMoveDirection)
		{
			val angle = com.lyeeedar.Util.getRotation(effect.lastPos, tempVec.set(lx, ly))
			effect.rotation = angle
			effect.lastPos.set(lx, ly)
		}
		else
		{
			effect.rotation = rotation
		}

		super.act(delta)
	}

	val tempVec = Vector2()
	val tempCol = Colour()
	val tempCol2 = Colour()
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (!acted)
		{
			act(0f)
		}

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val actorCol = tempCol2.set(color.r, color.g, color.b, color.a * parentAlpha)
		val animCol = effect.animation?.renderColour() ?: Colour.WHITE

		for (emitter in effect.emitters)
		{
			for (particle in emitter.particles)
			{
				var px = 0f
				var py = 0f

				if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
				{
					tempVec.set(emitter.currentOffset)
					tempVec.scl(emitter.size)
					tempVec.rotate(emitter.rotation)

					px += (emitter.position.x + tempVec.x)
					py += (emitter.position.y + tempVec.y)
				}

				for (pdata in particle.particles)
				{
					val keyframe1 = pdata.keyframe1
					val keyframe2 = pdata.keyframe2
					val alpha = pdata.keyframeAlpha

					val tex1 = keyframe1.texture[pdata.texStream]
					val tex2 = keyframe2.texture[pdata.texStream]

					val col = tempCol.set(keyframe1.colour[pdata.colStream]).lerp(keyframe2.colour[pdata.colStream], alpha)
					col.a = keyframe1.alpha[pdata.alphaStream].lerp(keyframe2.alpha[pdata.alphaStream], alpha)

					val size = keyframe1.size[pdata.sizeStream].lerp(keyframe2.size[pdata.sizeStream], alpha, pdata.ranVal)

					var w = width
					var h = height
					if (particle.maintainAspectRatio)
					{
						w = min(width, height)
						h = w
					}

					var sizex = if (particle.sizeMode == Particle.SizeMode.YONLY) w else size * w
					var sizey = if (particle.sizeMode == Particle.SizeMode.XONLY) h else size * h

					if (particle.allowResize)
					{
						sizex *= emitter.size.x
						sizey *= emitter.size.y
					}

					val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

					col.mul(actorCol).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					var drawx = (tempVec.x + px) * width - sizex * 0.5f
					var drawy = (tempVec.y + py) * height - sizey * 0.5f

					when (particle.sizeOrigin)
					{
						Particle.SizeOrigin.CENTER -> { }
						Particle.SizeOrigin.BOTTOM -> {
							drawy += sizey*0.5f
						}
						Particle.SizeOrigin.TOP -> {
							drawy -= sizey*0.5f
						}
						Particle.SizeOrigin.LEFT -> {
							drawx += sizex*0.5f
						}
						Particle.SizeOrigin.RIGHT -> {
							drawx -= sizex*0.5f
						}
					}

					if (particle.blendKeyframes)
					{
						val tex1Index = tex1.toInt()
						val tex2Index = min(particle.textures[pdata.texStream].size-1, tex1Index+1)

						val texture1 = particle.textures[pdata.texStream][tex1Index].second
						val texture2 = particle.textures[pdata.texStream][tex2Index].second
						val blendAlpha = tex1.lerp(tex2, pdata.keyframeAlpha)

						batch.setColor(col.r, col.g, col.b, col.a * (1f - blendAlpha))
						batch.draw(texture1, drawx, drawy, sizex*0.5f, sizey*0.5f, sizex, sizey, 1f, 1f, rotation)

						batch.setColor(col.r, col.g, col.b, col.a * blendAlpha)
						batch.draw(texture2, drawx, drawy, sizex*0.5f, sizey*0.5f, sizex, sizey, 1f, 1f, rotation)
					}
					else
					{
						val texture1 = particle.textures[pdata.texStream][tex1.toInt()].second

						batch.setColor(col.toFloatBits())
						batch.draw(texture1, drawx, drawy, sizex*0.5f, sizey*0.5f, sizex, sizey, 1f, 1f, rotation)
					}
				}
			}
		}

		super.draw(batch, parentAlpha)
	}

	override fun drawDebug(shapes: ShapeRenderer)
	{
		effect.debug(shapes, 0f, 0f, width, true, true, true)

		super.drawDebug(shapes)
	}
}