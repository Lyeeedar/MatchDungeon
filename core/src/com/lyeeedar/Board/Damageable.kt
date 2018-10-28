package com.lyeeedar.Board

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.ciel

abstract class Damageable
{
	var immune = false

	var damageReduction: Int = 0
		set(value)
		{
			if (immune && value < field)
			{
				return
			}

			field = value
			remainingReduction = value
		}
	var remainingReduction: Int = 0

	var hp: Float = 1f
		set(value)
		{
			if (immune && value < field)
			{
				return
			}

			if (value < field)
			{
				val loss = field.ciel() - value.ciel()
				lostHP += loss

				var delay = 1f
				for (i in 0 until loss)
				{
					Future.call({ lostHP-- }, delay)
					delay += 0.2f
				}

				sprite.colourAnimation = BlinkAnimation.obtain().set(Colour(Color.RED), sprite.colour, 0.15f, true)
			}

			field = value
			if (field < 0f) field = 0f
		}

	var lostHP: Int = 0

	var maxhp: Int = 1
		set(value)
		{
			field = value
			hp = value.toFloat()
		}

	val damSources = ObjectSet<Any?>()

	abstract var sprite: Sprite
}