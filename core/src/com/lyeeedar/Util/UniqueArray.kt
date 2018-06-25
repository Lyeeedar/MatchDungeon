package com.lyeeedar.Util

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet

class UniqueArray<T>(val uniqueFun: (T) -> Int) : Iterable<T>
{
	val backingArray = Array<T>()
	val uniqueSet = IntSet()

	fun add(item: T)
	{
		val key = uniqueFun(item)
		if (!uniqueSet.contains(key))
		{
			backingArray.add(item)
			uniqueSet.add(key)
		}
	}

	fun remove(item: T)
	{
		val key = uniqueFun(item)
		uniqueSet.remove(key)

		backingArray.removeValue(item, true)
	}

	fun clear()
	{
		backingArray.clear()
		uniqueSet.clear()
	}

	override fun iterator(): Iterator<T>
	{
		return backingArray.iterator()
	}
}