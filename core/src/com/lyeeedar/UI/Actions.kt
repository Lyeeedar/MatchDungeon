package com.lyeeedar.UI

import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.Action
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.getRotation
import com.lyeeedar.Util.lerp

class ShakeAction(val amount: Float, val speed: Float, val duration: Float) : Action()
{
	var time = 0f

	var shakeRadius = amount
	var shakeAccumulator = 0f
	var shakeAngle = 0f

	var offsetx = 0f
	var offsety = 0f

	override fun act(delta: Float): Boolean
	{
		time += delta
		shakeAccumulator += delta
		while ( shakeAccumulator >= speed )
		{
			shakeAccumulator -= speed
			shakeAngle += (150 + Random.random() * 60)
		}

		target.moveBy(-offsetx, -offsety)

		offsetx = Math.sin( shakeAngle.toDouble() ).toFloat() * shakeRadius
		offsety = Math.cos( shakeAngle.toDouble() ).toFloat() * shakeRadius

		target.moveBy(offsetx, offsety)

		return time >= duration
	}
}

fun shake(amount: Float, speed: Float, duration: Float): ShakeAction = ShakeAction(amount, speed, duration)

class LambdaAction(val lambda: ()->Unit) : Action()
{
	override fun act(delta: Float): Boolean
	{
		lambda.invoke()

		return true
	}
}

fun lambda(lambda: ()->Unit): LambdaAction = LambdaAction(lambda)

class MoteAction(val path: Path<Vector2>, val duration: Float, val interpolation: Interpolation, val alignToMovement: Boolean) : Action()
{
	constructor(src: Vector2, dst: Vector2, duration: Float, interpolation: Interpolation, alignToMovement: Boolean) : this(Bezier<Vector2>(), duration, interpolation, alignToMovement)
	{
		val bezier = path as Bezier<Vector2>

		val dir = Vector2().setToRandomDirection()
		val p0 = src.cpy()
		val p1 = Vector2().set(dir).scl(50f + MathUtils.random(125).toFloat()).add(src)
		val p2 = Vector2().set(src).lerp(dst, 0.8f)
		val p3 = dst.cpy()

		bezier.set(p0, p1, p2, p3)
	}

	var time: Float = 0f

	val newPos = Vector2()
	override fun act(delta: Float): Boolean
	{
		time += delta

		val currentPos = Vector2(target.x, target.y)

		val alpha = time / duration
		val interpAlpha = interpolation.apply(alpha)
		path.valueAt(newPos, interpAlpha)

		target.setPosition(newPos.x, newPos.y)

		if (alignToMovement)
		{
			target.rotation = getRotation(currentPos, newPos)
		}

		return time >= duration
	}
}

fun mote(path: Path<Vector2>, duration: Float, interpolation: Interpolation = Interpolation.linear, alignToMovement: Boolean = true): MoteAction
{
	return MoteAction(path, duration, interpolation, alignToMovement)
}

fun mote(src: Vector2, dst: Vector2, duration: Float, interpolation: Interpolation = Interpolation.linear, alignToMovement: Boolean = true): MoteAction
{
	return MoteAction(src, dst, duration, interpolation, alignToMovement)
}

class WobbleAction(val angleStart: Float, val angleEnd: Float, val period: Float, val duration: Float) : Action()
{
	var positive = true
	var currentProgression = 0f
	var time: Float = 0f

	override fun act(delta: Float): Boolean
	{
		time += delta

		currentProgression += delta
		while (currentProgression >= period)
		{
			currentProgression -= period
			positive = !positive
		}

		val angle = angleStart.lerp(angleEnd, time / duration)

		target.rotation = if (positive) (-angle).lerp(angle, currentProgression / period) else angle.lerp(-angle, currentProgression / period)

		if (time >= duration)
		{
			target.rotation = 0f
		}

		return time >= duration
	}
}

fun wobble(angleStart: Float, angleEnd: Float, period: Float, duration: Float) = WobbleAction(angleStart, angleEnd, period, duration)