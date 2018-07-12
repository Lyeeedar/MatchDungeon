package com.lyeeedar.Board

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future

abstract class Damageable
{
	var damageReduction: Int = 0
		set(value)
		{
			field = value
			remainingReduction = value
		}
	var remainingReduction: Int = 0

	var hp: Int = 1
		set(value)
		{
			if (value < field)
			{
				val loss = field - value
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
			if (field < 0) field = 0
		}

	var lostHP: Int = 0

	var maxhp: Int = 1
		set(value)
		{
			field = value
			hp = value
		}

	val damSources = ObjectSet<Any>()

	abstract var sprite: Sprite
}