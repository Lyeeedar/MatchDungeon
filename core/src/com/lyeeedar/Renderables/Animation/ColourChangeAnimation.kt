package com.lyeeedar.Renderables.Animation

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 31-Jul-16.
 */

class ColourChangeAnimation() : AbstractColourAnimation()
{
	private var duration: Float = 0f
	private var time: Float = 0f
	private val colour: Colour = Colour()
	private val targetColour: Colour = Colour()
	private val startColour: Colour = Colour()

	override fun duration(): Float = duration
	override fun time(): Float = time
	override fun renderColour(): Colour = colour

	override fun update(delta: Float): Boolean
	{
		time += delta

		val alpha = MathUtils.clamp(time / duration, 0f, 1f)

		colour.set(startColour).lerp(targetColour, alpha)

		if (time >= duration)
		{
			if (oneTime) return true
			else time -= duration
		}

		return false
	}

	fun set(start: Colour, target: Colour, duration: Float, oneTime: Boolean = true): ColourChangeAnimation
	{
		this.targetColour.set(target)
		this.startColour.set(start)
		this.duration = duration
		this.oneTime = oneTime

		this.time = 0f
		this.colour.set(start)

		return this
	}

	override fun parse(xml: XmlData)
	{
	}

	override fun copy(): AbstractAnimation = ColourChangeAnimation.obtain().set(startColour, targetColour, duration, oneTime)

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ColourChangeAnimation> = object : Pool<ColourChangeAnimation>() {
			override fun newObject(): ColourChangeAnimation
			{
				return ColourChangeAnimation()
			}

		}

		@JvmStatic fun obtain(): ColourChangeAnimation
		{
			val anim = ColourChangeAnimation.pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			anim.time = 0f
			return anim
		}
	}
	override fun free() { if (obtained) { ColourChangeAnimation.pool.free(this); obtained = false } }
}