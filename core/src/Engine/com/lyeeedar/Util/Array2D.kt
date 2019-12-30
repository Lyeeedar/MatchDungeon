package com.lyeeedar.Util

import com.badlogic.gdx.math.MathUtils.clamp
import com.lyeeedar.Direction
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by Philip on 08-Apr-16.
 */

class Array2D<T> (val xSize: Int, val ySize: Int, val array: Array<Array<T>>): Sequence<T> {

	val width: Int
		get() = xSize

	val height: Int
		get() = ySize

	companion object {

		inline operator fun <reified T> invoke() = Array2D(0, 0, Array(0, { emptyArray<T>() }))

		inline operator fun <reified T> invoke(xWidth: Int, yHeight: Int) =
				Array2D(xWidth, yHeight, Array(xWidth, { arrayOfNulls<T>(yHeight) }))

		inline operator fun <reified T> invoke(xWidth: Int, yHeight: Int, operator: (Int, Int) -> (T)): Array2D<T> {
			val array = Array(xWidth, {
				val x = it
				Array(yHeight, {operator(x, it)})})
			return Array2D(xWidth, yHeight, array)
		}
	}

	internal inline fun inBounds(point: Point) = inBounds(point.x, point.y)

	internal inline fun inBounds(x: Int, y: Int) = x in 0..(xSize - 1) && y in 0..(ySize - 1)

	internal inline fun tryGet(x:Int, y:Int, fallback:T?): T?
	{
		if (!inBounds(x, y)) return fallback
		else return this[x, y]
	}

	internal inline fun tryGet(x:Int, y:Int, dir: Direction, fallback:T?): T?
	{
		val x = x + dir.x
		val y = y + dir.y
		if (!inBounds(x, y)) return fallback
		else return this[x, y]
	}

	operator fun get(x: Int, y: Int, fallback:T?): T? = tryGet(x, y, fallback)

	fun getClamped(x: Int, y: Int): T {
		val x = clamp(x, 0, width-1)
		val y = clamp(y, 0, height-1)
		return array[x][y]
	}

	operator fun get(x: Int, y: Int): T {
		return array[x][y]
	}

	operator fun set(x: Int, y: Int, t: T) {
		array[x][y] = t
	}

	operator fun get(p: Point): T {
		return array[p.x][p.y]
	}

	operator fun get(p: Point, dir: Direction): T {
		return array[p.x + dir.x][p.y + dir.y]
	}

	operator fun set(p: Point, t: T) {
		array[p.x][p.y] = t
	}

	inline fun forEach(operation: (T) -> Unit) {
		array.forEach { it.forEach { operation.invoke(it) } }
	}

	inline fun forEachIndexed(operation: (x: Int, y: Int, T) -> Unit) {
		array.forEachIndexed { x, p -> p.forEachIndexed { y, t -> operation.invoke(x, y, t) } }
	}

	fun get(p: Point, range: Int): Sequence<T>
	{
		return buildSequence {
			val minx = max(p.x - range, 0)
			val miny = max(p.y - range, 0)
			val maxx = min(p.x + range, width-1)
			val maxy = min(p.y + range, height-1)

			for (x in minx..maxx)
			{
				for (y in miny..maxy)
				{
					yield(array[x][y])
				}
			}
		}
	}

	override operator fun iterator(): Iterator<T> =  Array2DIterator(this)

	class Array2DIterator<T>(val array: Array2D<T>): Iterator<T>
	{
		var x = 0
		var y = 0

		override fun hasNext(): Boolean = x < array.xSize

		override fun next(): T
		{
			val el = array[x, y]

			y++
			if (y == array.ySize)
			{
				y = 0
				x++
			}

			return el
		}

	}

	override fun toString(): String
	{
		var string = ""

		for (y in 0..ySize-1)
		{
			for (x in 0..xSize-1)
			{
				string += this[x,y].toString() + " "
			}
			string += "\n\n"
		}

		return string
	}
}