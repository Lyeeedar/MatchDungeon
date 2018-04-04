package com.lyeeedar.Util

import squidpony.squidmath.LightRNG

data class Range(val min: Float, val max: Float)
{
	fun getValue(ran: LightRNG): Float
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