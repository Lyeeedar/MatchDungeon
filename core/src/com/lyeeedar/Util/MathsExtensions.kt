package com.lyeeedar.Util

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Path
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pools
import ktx.collections.gdxArrayOf



fun Vector2.lerp(targetx: Float, targety: Float, alpha: Float): Vector2
{
	val invAlpha = 1.0f - alpha
	this.x = x * invAlpha + targetx * alpha
	this.y = y * invAlpha + targety * alpha
	return this
}

fun Path<Vector2>.valueAt(time: Float): Vector2
{
	val vec2 = Vector2()
	this.valueAt(vec2, time)
	return vec2
}

fun max(vararg vals: Float): Float = vals.max()!!
fun min(vararg vals: Float): Float = vals.min()!!

fun max(vararg vals: Int): Int = vals.max()!!
fun min(vararg vals: Int): Int = vals.min()!!

class Smoothstep() : Interpolation()
{
	override fun apply(a: Float): Float = a * a * ( 3f - 2f * a )
}
val smoothStep = Smoothstep()

class Leap() : Interpolation()
{
	override fun apply(a: Float): Float
	{
		var t = a

		if (t <= 0.5f) return 2.0f * t * (1.0f - t)

		t -= 0.5f

		return 2.0f * t * t + 0.5f
	}
}
val leap = Leap()

inline fun vectorToAngle(x: Float, y: Float) : Float
{
	// basis vector 0,1
	val dot = 0f * x + 1f * y // dot product
	val det = 0f * y - 1f * x // determinant
	val angle = MathUtils.atan2(det, dot) * MathUtils.radiansToDegrees

	return angle
}

fun getRotation(p1: Point, p2: Point) : Float
{
	val vec = Pools.obtain(Vector2::class.java)
	vec.x = (p2.x - p1.x).toFloat()
	vec.y = (p2.y - p1.y).toFloat()
	vec.nor()

	val angle = vectorToAngle(vec.x, vec.y)

	Pools.free(vec)

	return angle
}

fun getRotation(p1: Vector2, p2: Vector2) : Float
{
	val vec = Pools.obtain(Vector2::class.java)
	vec.x = (p2.x - p1.x).toFloat()
	vec.y = (p2.y - p1.y).toFloat()
	vec.nor()

	val angle = vectorToAngle(vec.x, vec.y)

	Pools.free(vec)

	return angle
}

fun Int.abs() = Math.abs(this)

fun Float.pow(factor: Float) = Math.pow(this.toDouble(), factor.toDouble()).toFloat()
fun Float.sqrt() = Math.sqrt(this.toDouble()).toFloat()
fun Float.abs() = Math.abs(this)
fun Float.ciel() = MathUtils.ceil(this)
fun Float.floor() = MathUtils.floor(this)
fun Float.round() = MathUtils.round(this)
fun Float.clamp(min: Float, max: Float) = MathUtils.clamp(this, min , max)

inline fun Float.lerp(target: Float, alpha: Float) = this + (target - this) * alpha

fun Int.romanNumerals(): String
{
	return when (this)
	{
		1 -> "I"
		2 -> "II"
		3 -> "III"
		4 -> "IV"
		5 -> "V"
		6 -> "VI"
		7 -> "VII"
		8 -> "VIII"
		9 -> "IX"
		10 -> "X"
		11 -> "XI"
		12 -> "XII"
		13 -> "XIII"
		14 -> "XIV"
		15 -> "XV"
		16 -> "XVI"
		17 -> "XVII"
		18 -> "XVIII"
		19 -> "XIX"
		20 -> "XX"
		else -> "--"
	}
}

fun getCircleLineIntersectionPoint(pointA: Vector2, pointB: Vector2, center: Vector2, radius: Float): Array<Vector2>
{
	val baX = pointB.x - pointA.x
	val baY = pointB.y - pointA.y
	val caX = center.x - pointA.x
	val caY = center.y - pointA.y

	val a = baX * baX + baY * baY
	val bBy2 = baX * caX + baY * caY
	val c = caX * caX + caY * caY - radius * radius

	val pBy2 = bBy2 / a
	val q = c / a

	val disc = pBy2 * pBy2 - q
	if (disc < 0)
	{
		return Array()
	}

	val tmpSqrt = Math.sqrt(disc.toDouble()).toFloat()
	val abScalingFactor1 = -pBy2 + tmpSqrt
	val abScalingFactor2 = -pBy2 - tmpSqrt

	val p1 = Vector2(pointA.x - baX * abScalingFactor1, pointA.y - baY * abScalingFactor1)

	if (disc == 0.0f)
	{
		return gdxArrayOf(p1)
	}

	val p2 = Vector2(pointA.x - baX * abScalingFactor2, pointA.y - baY * abScalingFactor2)

	return gdxArrayOf(p1, p2)
}

fun doesLineIntersectCircle(pointA: Vector2, pointB: Vector2, center: Vector2, radius: Float): Boolean
{
	val baX = pointB.x - pointA.x
	val baY = pointB.y - pointA.y
	val caX = center.x - pointA.x
	val caY = center.y - pointA.y

	val a = baX * baX + baY * baY
	val bBy2 = baX * caX + baY * caY
	val c = caX * caX + caY * caY - radius * radius

	val pBy2 = bBy2 / a
	val q = c / a

	val disc = pBy2 * pBy2 - q
	return disc >= 0
}