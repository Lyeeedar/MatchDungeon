package com.lyeeedar.Util

import com.badlogic.gdx.utils.Pool
import squidpony.squidmath.LightRNG

class Random
{
	companion object
	{
		val random = LightRNG()

		private val pool = object : Pool<LightRNG>() {
			override fun newObject(): LightRNG
			{
				return LightRNG()
			}

		}

		fun obtainTS(seed: Long): LightRNG
		{
			synchronized(pool)
			{
				val item = pool.obtain()
				item.setSeed(seed)
				return item
			}
		}

		fun freeTS(ran: LightRNG)
		{
			synchronized(pool)
			{
				pool.free(ran)
			}
		}

		fun sign(): Float
		{
			return if (random.nextBoolean()) 1.0f else -1.0f
		}

		fun randomWeighted(): Float
		{
			val ran = random()
			return ran * ran
		}

		fun random(): Float
		{
			return random.nextFloat()
		}

		fun random(max: Float): Float
		{
			return random.nextFloat() * max
		}

		fun random(min: Float, max: Float): Float
		{
			return random.nextFloat() * (max - min) + min
		}

		fun random(min: Int, max: Int): Int
		{
			return random.nextInt(min, max)
		}

		fun random(max: Int): Int
		{
			val ranVal = random.nextInt(max)
			if (ranVal > max) throw Exception("Random broke!")
			return ranVal
		}
	}
}

fun LightRNG.freeTS()
{
	Random.freeTS(this)
}

fun LightRNG.nextFloat(value: Float) = this.nextFloat() * value