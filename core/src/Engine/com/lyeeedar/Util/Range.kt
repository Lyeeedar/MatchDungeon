package com.lyeeedar.Util

import squidpony.squidmath.LightRNG

class Range(val min: Float, val max: Float)
{
	constructor(point: Point) : this(point.x.toFloat(), point.y.toFloat())

	fun getValue(ran: LightRNG = Random.random): Float
	{
		return min + ran.nextFloat() * (max - min)
	}

	companion object
	{
		fun parse(text: String): Range
		{
			if (text.contains(','))
			{
				val split = text.split(',')
				return Range(split[0].toFloat(), split[1].toFloat())
			}
			else
			{
				val v = text.toFloat()
				return Range(v, v)
			}
		}
	}
}

class IntRange(val min: Int, val max: Int)
{
	constructor(point: Point) : this(point.x, point.y)

	fun getValue(ran: LightRNG = Random.random): Int
	{
		return (min + (ran.nextFloat() * (max - min).toFloat())).toInt()
	}

	companion object
	{
		fun parse(text: String): IntRange
		{
			if (text.contains(','))
			{
				val split = text.split(',')
				return IntRange(split[0].toInt(), split[1].toInt())
			}
			else
			{
				val v = text.toInt()
				return IntRange(v, v)
			}
		}
	}
}