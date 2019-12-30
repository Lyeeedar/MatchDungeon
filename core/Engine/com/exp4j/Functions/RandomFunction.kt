package com.exp4j.Functions

import com.badlogic.gdx.utils.Pool

class RandomFunction : SeededFunction("rnd", 1)
{
	override fun apply(vararg arg0: Double): Double
	{
		return ran.nextFloat() * arg0[0]
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

		val pool = object : Pool<RandomFunction>() {
			override fun newObject(): RandomFunction
			{
				return RandomFunction()
			}
		}

		fun obtain(): RandomFunction
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}

class SignFunction : SeededFunction("sign", 0)
{
	override fun apply(vararg args: Double): Double
	{
		return if (ran.nextBoolean()) 1.0 else -1.0
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

		val pool = object : Pool<SignFunction>() {
			override fun newObject(): SignFunction
			{
				return SignFunction()
			}
		}

		fun obtain(): SignFunction
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}