package com.lyeeedar.Util

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import squidpony.squidmath.LightRNG
import kotlin.coroutines.experimental.buildSequence

fun <T> kotlin.Array<T>.random(ran: LightRNG = Random.random): T = this[ran.nextInt(this.size)]
fun <T> List<T>.Array(ran: LightRNG = Random.random): T? = if (this.isEmpty()) null else this[ran.nextInt(this.size)]

fun <T> List<T>.random(ran: LightRNG = Random.random): T = this[ran.nextInt(this.size)]
fun <T> List<T>.randomOrNull(ran: LightRNG = Random.random): T? = if (this.isEmpty()) null else this[ran.nextInt(this.size)]

fun <T> com.badlogic.gdx.utils.Array<T>.tryGet(i: Int): T = this[MathUtils.clamp(i, 0, this.size-1)]
fun <T> com.badlogic.gdx.utils.Array<T>.random(ran: LightRNG = Random.random): T = this[ran.nextInt(this.size)]
fun <T> com.badlogic.gdx.utils.Array<T>.randomOrNull(ran: LightRNG = Random.random): T? = if (this.size == 0) null else this[ran.nextInt(this.size)]

fun <T> com.badlogic.gdx.utils.Array<T>.removeRandom(ran: LightRNG = Random.random): T
{
	val index = ran.nextInt(this.size)
	val item = this[index]
	this.removeIndex(index)

	return item
}
fun <T> com.badlogic.gdx.utils.Array<T>.addAll(collection: Sequence<T>)
{
	for (item in collection) this.add(item)
}
fun <T> Iterable<T>.asGdxArray(ordered: Boolean = true): com.badlogic.gdx.utils.Array<T> {
	val array = Array<T>(ordered, this.count())
	array.addAll(this.asSequence())
	return array
}
fun <T> Sequence<T>.asGdxArray(ordered: Boolean = true): com.badlogic.gdx.utils.Array<T> {
	return this.asIterable().asGdxArray(ordered)
}

fun <T> com.badlogic.gdx.utils.Array<T>.randomize(): Sequence<T>
{
	val sequence = this.asGdxArray(false)
	return buildSequence {
		while (sequence.size > 0)
		{
			yield(sequence.removeRandom(Random.random))
		}
	}
}

fun <T> Sequence<T>.randomize(): Sequence<T>
{
	val sequence = this.asGdxArray(false)
	return buildSequence {
		while (sequence.size > 0)
		{
			yield(sequence.removeRandom(Random.random))
		}
	}
}

fun <T> Sequence<T>.random(): T?
{
	val sequence = this.toList()
	val count = sequence.size
	if (count > 0)
	{
		return sequence[Random.random(count)]
	}
	else
	{
		return null
	}
}
fun <T> Sequence<T>.random(ran: LightRNG) = if (this.count() > 0) this.elementAt(ran.nextInt(this.count())) else null
inline fun <reified T> Sequence<T>.random(num: Int): Sequence<T>
{
	val array = Array<T>(this.count())
	for (item in this) array.add(item)

	val outArray = Array<T>(num)
	for (i in 0 until num)
	{
		if (array.size == 0) break
		outArray.add(array.removeRandom(Random.random))
	}

	return outArray.asSequence()
}
inline fun <reified T> Sequence<T>.random(num: Int, ran: LightRNG): Sequence<T>
{
	val array = Array<T>(this.count())
	for (item in this) array.add(item)

	val outArray = Array<T>(num)
	for (i in 0 until num)
	{
		if (array.size == 0) break
		outArray.add(array.removeRandom(ran))
	}

	return outArray.asSequence()
}
inline fun <reified T> Sequence<T>.weightedRandom(weightFun: (T) -> Int, ran: LightRNG = Random.random): T?
{
	if (this.count() == 0) return null

	val totalWeight = this.sumBy { weightFun(it) }

	if (totalWeight == 0) return null

	val chosen = ran.nextInt(totalWeight)

	var current = 0
	for (e in this)
	{
		val weight = weightFun(e)
		if (weight > 0 && chosen - current < weight)
		{
			return e
		}

		current += weight
	}

	return lastOrNull()
}

fun <T> Sequence<T>.sequenceEquals(other: Sequence<T>): Boolean
{
	if (this.count() != other.count()) return false

	for (item in this)
	{
		if (!other.contains(item)) return false
	}

	for (item in other)
	{
		if (!this.contains(item)) return false
	}

	return true
}

suspend fun <T> Iterable<T>.parallelForEach(func: (T) -> Unit)
{
	val jobs = Array<Job>(count())
	for (item in this)
	{
		val job = launch(CommonPool)
		{
			func(item)
		}
		jobs.add(job)
	}
	for (job in jobs) job.join()
}

operator fun <K> ObjectFloatMap<K>.set(key: K, value: Float)
{
	this.put(key, value)
}

fun <K> ObjectFloatMap(variables: Map<K, Float>): ObjectFloatMap<K>
{
	val map = ObjectFloatMap<K>()

	for (pair in variables)
	{
		map[pair.key] = pair.value
	}

	return map
}

fun <T, K> ObjectMap<T, K>.copy(): ObjectMap<T, K>
{
	val map = ObjectMap<T, K>()
	map.putAll(this)
	return map
}

fun <K> ObjectFloatMap<K>.copy(): ObjectFloatMap<K>
{
	val map = ObjectFloatMap<K>()
	map.putAll(this)
	return map
}