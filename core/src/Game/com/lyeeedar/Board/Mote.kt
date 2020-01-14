package com.lyeeedar.Board

import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteEffectActor

class Mote(src: Vector2, dst: Vector2, sprite: Sprite, spriteSize: Float, completionFun: (() -> Unit)? = null, animSpeed: Float = 1.5f, leap: Boolean = false) : SpriteEffectActor(sprite, spriteSize, spriteSize, Vector2())
{
	init
	{
		if (leap)
		{
			val len = src.dst(dst)

			val animDuration = animSpeed + len * 0.001f

			sprite.animation = LeapAnimation.obtain().setAbsolute(animDuration, src, dst, 1f + len * 0.25f)
			sprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
		}
		else
		{
			val dir = Vector2().setToRandomDirection()
			val p0 = src.cpy()
			val p1 = Vector2().set(dir).scl(50f + MathUtils.random(125).toFloat()).add(src)
			val p2 = Vector2().set(src).lerp(dst, 0.8f)
			val p3 = dst.cpy()

			val path = Bezier(p0, p1, p2, p3)
			sprite.animation = MoveAnimation.obtain().set(animSpeed, path, Interpolation.exp5)
		}

		sprite.faceInMoveDirection = true

		motes.add(this)
		completionFunc = { completionFun?.invoke(); motes.removeValue(this, true) }
	}

	companion object
	{
		val motes = com.badlogic.gdx.utils.Array<Mote>(false, 16)

		fun clear()
		{
			for (mote in motes)
			{
				mote.remove()
			}
			motes.clear()
		}
	}
}

fun spawnMote(src: Vector2, dst: Vector2, sprite: Sprite, spriteSize: Float, completionFun: (() -> Unit)? = null, animSpeed: Float = 1.5f, leap: Boolean = false)
{
	if (Global.resolveInstantly)
	{
		completionFun?.invoke()
	}
	else
	{
		Mote(src, dst, sprite, spriteSize, completionFun, animSpeed, leap)
	}
}
