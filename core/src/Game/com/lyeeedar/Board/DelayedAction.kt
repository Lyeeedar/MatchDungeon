package com.lyeeedar.Board

import com.badlogic.gdx.utils.Pool

class DelayedAction() : Comparable<DelayedAction>
{
	lateinit var function: (tile: Tile) -> Unit
	var delay: Float = 0f
	lateinit var target: Tile

	fun set(function: (tile: Tile)-> Unit, delay: Float, target: Tile): DelayedAction
	{
		this.function = function
		this.delay = delay
		this.target = target

		return this
	}

	override fun compareTo(other: DelayedAction): Int
	{
		return delay.compareTo(other.delay)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DelayedAction> = object : Pool<DelayedAction>() {
			override fun newObject(): DelayedAction
			{
				return DelayedAction()
			}

		}

		@JvmStatic fun obtain(): DelayedAction
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}