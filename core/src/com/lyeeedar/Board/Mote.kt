package com.lyeeedar.Board

import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteEffectActor

class Mote(src: Vector2, dst: Vector2, sprite: Sprite, spriteSize: Float, completionFunc: (() -> Unit)? = null) : SpriteEffectActor(sprite, spriteSize, spriteSize, Vector2(), { completionFunc?.invoke(); moteCount-- })
{
	init
	{
		val dir = Vector2().setToRandomDirection()
		val p0 = src.cpy()
		val p1 = Vector2().set(dir).scl(50f + MathUtils.random(125).toFloat()).add(src)
		val p2 = Vector2().set(src).lerp(dst, 0.8f)
		val p3 = dst.cpy()

		val path = Bezier(p0, p1, p2, p3)
		sprite.animation = MoveAnimation.obtain().set(1.5f, path, Interpolation.exp5)

		moteCount++
	}


	companion object
	{
		var moteCount = 0
	}
}
