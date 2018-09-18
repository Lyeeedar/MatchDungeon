package com.lyeeedar.Util

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap

class UniqueArray<T>(val uniqueFun: (T) -> Int) : Iterable<T>
{
	val backingArray = Array<T>()
	val uniqueMap = IntMap<T>()

	val size: Int
		get() = backingArray.size

	fun add(item: T)
	{
		val key = uniqueFun(item)
		if (!uniqueMap.containsKey(key))
		{
			backingArray.add(item)
			uniqueMap.put(key, item)
		}
	}

	fun replace(item: T)
	{
		val key = uniqueFun(item)

		if (uniqueMap.containsKey(key))
		{
			val existing = uniqueMap[key]
			backingArray.removeValue(existing, true)

			backingArray.add(item)
			uniqueMap.put(key, item)
		}
	}

	fun addAll(items: Iterable<T>)
	{
		for (item in items)
		{
			add(item)
		}
	}

	fun remove(item: T)
	{
		val key = uniqueFun(item)
		uniqueMap.remove(key)

		backingArray.removeValue(item, true)
	}

	fun clear()
	{
		backingArray.clear()
		uniqueMap.clear()
	}

	override fun iterator(): Iterator<T>
	{
		return backingArray.iterator()
	}
}