package com.lyeeedar.Util

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction

class PointRect()
{
	var x: Int = 0
	var y: Int = 0
	var width: Int = 0
	var height: Int = 0

	constructor(x: Int, y: Int, w: Int, h: Int) : this()
	{
		this.x = x
		this.y = y
		this.width = w
		this.height = h
	}

	val maxX: Int
		get() = x + width - 1
	val maxY: Int
		get() = y + height - 1

	fun grow(direction: Direction, amount: Int)
	{
		val dx = direction.x * amount
		val dy = direction.y * amount

		if (dx < 0)
		{
			x += dx
			width -= dx
		}
		else
		{
			width += dx
		}

		if (dy < 0)
		{
			y += dy
			height -= dy
		}
		else
		{
			height += dy
		}
	}

	fun shrink(direction: Direction, amount: Int)
	{
		val dx = direction.x * amount
		val dy = direction.y * amount

		if (dx > 0)
		{
			x += dx
			width -= dx
		}
		else
		{
			width += dx
		}

		if (dy > 0)
		{
			y += dy
			height -= dy
		}
		else
		{
			height += dy
		}
	}

	fun getEdge(direction: Direction, store: Array<Point>)
	{
		if (direction == Direction.NORTH)
		{
			for (x in x..maxX)
			{
				store.add(Point.obtainTS().set(x, maxY))
			}
		}
		else if (direction == Direction.SOUTH)
		{
			for (x in x..maxX)
			{
				store.add(Point.obtainTS().set(x, y))
			}
		}
		else if (direction == Direction.WEST)
		{
			for (y in y..maxY)
			{
				store.add(Point.obtainTS().set(x, y))
			}
		}
		else if (direction == Direction.EAST)
		{
			for (y in y..maxY)
			{
				store.add(Point.obtainTS().set(maxX, y))
			}
		}
		else
		{
			throw Exception("Unhandled direction")
		}
	}
}