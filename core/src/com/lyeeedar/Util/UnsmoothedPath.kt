package com.lyeeedar.Util

import com.badlogic.gdx.math.Path
import com.badlogic.gdx.math.Vector2

/**
 * Created by Philip on 19-Jul-16.
 */

class UnsmoothedPath(val path: Array<Vector2>): Path<Vector2>
{
	val distPoints: FloatArray = FloatArray(path.size)

	init
	{
		for (i in 1 until path.size)
		{
			distPoints[i] = distPoints[i-1] + path[i-1].dst(path[i])
		}
	}

	fun invertY(): UnsmoothedPath
	{
		for (p in path)
		{
			p.y *= -1f
		}

		return this
	}

	/** @return The value of the path at t where 0<=t<=1 */
	override fun valueAt (out: Vector2, t: Float): Vector2
	{
		if (t >= 1f)
		{
			out.set(path[path.size-1])
			return out
		}

		if (t <= 0f)
		{
			out.set(path[0])
			return out
		}

		val targetDst = distPoints[distPoints.size-1] * t
		var i = 0
		while (distPoints[i] < targetDst) i++

		if (i == 0)
		{
			out.set(path[0])
			return out
		}
		else
		{
			val p1 = path[i-1]
			val p2 = path[i]

			val diff = distPoints[i] - distPoints[i-1]
			val alphaDiff = targetDst - distPoints[i-1]
			val a = alphaDiff / diff

			out.set(p1).lerp(p2, a)

			return out
		}
	}

	override fun locate(v: Vector2?): Float
	{
		throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun approxLength(samples: Int): Float
	{
		return distPoints.last()
	}

	override fun approximate(v: Vector2?): Float
	{
		throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun derivativeAt(out: Vector2?, t: Float): Vector2
	{
		throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}