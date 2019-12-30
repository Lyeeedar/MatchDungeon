package com.exp4j.Functions

import com.badlogic.gdx.utils.Pool

/**
 * Created by Philip on 03-Jan-16.
 */
class ChanceFunction : SeededFunction("chance", 2)
{
	override fun apply(vararg arg0: Double): Double
	{
		val chanceVal = ran.nextFloat() * arg0[1]
		return (if (chanceVal <= arg0[0]) 1 else 0).toDouble()
	}

	override fun free()
	{
		synchronized(pool)
		{
			pool.free(this)
		}
	}

	companion object
	{
		val pool = object : Pool<ChanceFunction>() {
			override fun newObject(): ChanceFunction
			{
				return ChanceFunction()
			}

		}

		fun obtain(): ChanceFunction
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}

class ProbabilityFunction : SeededFunction("probability", 1)
{
	override fun apply(vararg arg0: Double): Double
	{
		return (if (ran.nextFloat() <= arg0[0]) 1 else 0).toDouble()
	}

	override fun free()
	{
		synchronized(pool)
		{
			pool.free(this)
		}
	}

	companion object
	{
		val pool = object : Pool<ProbabilityFunction>() {
			override fun newObject(): ProbabilityFunction
			{
				return ProbabilityFunction()
			}

		}

		fun obtain(): ProbabilityFunction
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}
