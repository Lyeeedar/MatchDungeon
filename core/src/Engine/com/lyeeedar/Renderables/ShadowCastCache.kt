package com.lyeeedar.Renderables

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Direction
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Statics.Companion.collisionGrid
import ktx.collections.isNotEmpty
import squidpony.squidgrid.FOV
import squidpony.squidgrid.Radius

class ShadowCastCache @JvmOverloads constructor(val fovType: Int = FOV.SHADOW)
{
	fun copy(): ShadowCastCache
	{
		val cache = ShadowCastCache(fovType)
		cache.lastrange = lastrange
		cache.lastx = lastx
		cache.lasty = lasty

		for (p in opaqueTiles)
		{
			cache.opaqueTiles.add(p.copy())
		}

		for (p in currentShadowCast)
		{
			cache.currentShadowCast.add(p.copy())
		}

		return cache
	}

	private val fov: FOV = FOV(fovType)

	var lastrange: Int = -1
		private set
	var lastx: Int = -Int.MAX_VALUE
		private set
	var lasty: Int = -Int.MAX_VALUE
		private set
	val opaqueTiles = com.badlogic.gdx.utils.Array<Point>()
	val clearTiles = com.badlogic.gdx.utils.Array<Point>()
	val currentShadowCast = com.badlogic.gdx.utils.Array<Point>()
	val invCurrentShadowCast = com.badlogic.gdx.utils.Array<Point>()
	val opaqueRegions = com.badlogic.gdx.utils.Array<PointRect>()

	var regionsVisible = false
	val currentCastRegions = com.badlogic.gdx.utils.Array<PointRect>()

	fun anyOpaque() = opaqueTiles.size > 0
	fun anyClear() = clearTiles.size > 0

	fun updateCurrentCastRegions(rawOutput: Array<DoubleArray>)
	{
		val collisionGrid = collisionGrid!!

		val visible = com.badlogic.gdx.utils.Array<Point>()
		val visibleOOB = com.badlogic.gdx.utils.Array<Point>()
		val invisible = com.badlogic.gdx.utils.Array<Point>()
		val invisibleOOB = com.badlogic.gdx.utils.Array<Point>()

		for (ix in 0 until lastrange * 2 + 1)
		{
			for (iy in 0 until lastrange * 2 + 1)
			{
				val gx = ix + lastx - lastrange
				val gy = iy + lasty - lastrange

				if (collisionGrid.inBounds(gx, gy))
				{
					val oob = Vector2.dst2(gx.toFloat(), gy.toFloat(), lastx.toFloat(), lasty.toFloat()) > (lastrange * lastrange).toFloat()

					if (rawOutput[ix][iy] > 0)
					{
						val point = Point.obtainTS().set(gx, gy)

						if (oob)
						{
							visibleOOB.add(point)
						}
						else
						{
							visible.add(point)
						}
					}
					else
					{
						val point = Point.obtainTS().set(gx, gy)

						if (oob)
						{
							invisibleOOB.add(point)
						}
						else
						{
							invisible.add(point)
						}
					}
				}
			}
		}

		val points: com.badlogic.gdx.utils.Array<Point>
		val pointsOOB: com.badlogic.gdx.utils.Array<Point>
		if (visible.size > invisible.size)
		{
			points = invisible
			pointsOOB = invisibleOOB
			regionsVisible = false
		}
		else
		{
			points = visible
			pointsOOB = visibleOOB
			regionsVisible = true
		}

		currentCastRegions.clear()

		val remainingSet = ObjectSet<Point>()
		remainingSet.addAll(points)

		val oobSet = ObjectSet<Point>()
		oobSet.addAll(pointsOOB)
		oobSet.addAll(points)

		while (remainingSet.size > 0)
		{
			val current = remainingSet.asSequence().random()!!
			remainingSet.remove(current)

			val rect = PointRect(current.x, current.y, 1, 1)

			val collided = FastEnumMap<Direction, Boolean>(Direction::class.java)

			val edgePoints = com.badlogic.gdx.utils.Array<Point>()
			while (true)
			{
				var moved = false
				for (dir in Direction.CardinalValues)
				{
					if (collided[dir] == true) continue

					rect.grow(dir, 1)

					rect.getEdge(dir, edgePoints)
					var hadCollision = false
					for (point in edgePoints)
					{
						if (!remainingSet.contains(point) && !oobSet.contains(point))
						{
							hadCollision = true
						}
					}

					if (!hadCollision)
					{
						moved = true
						for (point in edgePoints)
						{
							remainingSet.remove(point)
						}
					}
					else
					{
						collided[dir] = true
						rect.shrink(dir.opposite, 1)
					}

					Point.freeAllTS(edgePoints)
					edgePoints.clear()
				}

				if (!moved)
				{
					break
				}
			}

			currentCastRegions.add(rect)
		}

		Point.freeAllTS(visible)
		Point.freeAllTS(invisible)
		Point.freeAllTS(visibleOOB)
		Point.freeAllTS(invisibleOOB)
	}

	fun updateOpaqueRegions()
	{
		opaqueRegions.clear()

		val tileSet = ObjectSet<Point>()
		for (tile in opaqueTiles)
		{
			tileSet.add(tile)
		}

		val tempPoint = Point.obtainTS()
		while (tileSet.isNotEmpty())
		{
			val sourceTile = tileSet.asSequence().first()
			tileSet.remove(sourceTile)

			var chosenDir: Direction = Direction.CENTER
			for (dir in Direction.CardinalValues)
			{
				val newPoint = tempPoint.set(sourceTile)
				newPoint.plusAssign(dir)
				if (tileSet.contains(newPoint))
				{
					chosenDir = dir
					break
				}
			}

			if (chosenDir != Direction.CENTER)
			{
				val end1 = Point.obtainTS().set(sourceTile)
				val newPoint = Point.obtainTS()
				while (true)
				{
					newPoint.set(end1).plusAssign(chosenDir)
					if (!tileSet.contains(newPoint))
					{
						break
					}

					tileSet.remove(newPoint)
					end1.set(newPoint)
				}

				val end2 = Point.obtainTS().set(sourceTile)
				while (true)
				{
					newPoint.set(end2).minusAssign(chosenDir)
					if (!tileSet.contains(newPoint))
					{
						break
					}

					tileSet.remove(newPoint)
					end2.set(newPoint)
				}

				val minx = min(end1.x, end2.x)
				val miny = min(end1.y, end2.y)
				val maxx = max(end1.x, end2.x)
				val maxy = max(end1.y, end2.y)

				opaqueRegions.add(PointRect(minx, miny, (maxx - minx) + 1, (maxy - miny) + 1))

				end1.freeTS()
				end2.freeTS()
				newPoint.freeTS()
			}
			else
			{
				opaqueRegions.add(PointRect(sourceTile.x, sourceTile.y, 1, 1))
			}
		}

		tempPoint.freeTS()
	}

	fun getShadowCast(x: Int, y: Int, range: Int): com.badlogic.gdx.utils.Array<Point>
	{
		val collisionGrid = collisionGrid
		if (collisionGrid == null)
		{
			var recalculate = false

			if (x != lastx || y != lasty)
			{
				recalculate = true
			}
			else if (range != lastrange)
			{
				recalculate = true
			}

			if (recalculate)
			{
				Point.freeAllTS(currentShadowCast)
				currentShadowCast.clear()

				Point.freeAllTS(invCurrentShadowCast)
				invCurrentShadowCast.clear()

				for (ix in 0 until range * 2 + 1)
				{
					for (iy in 0 until range * 2 + 1)
					{
						val gx = ix + x - range
						val gy = iy + y - range

						val point = Point.obtainTS().set(gx, gy)
						currentShadowCast.add(point)
					}
				}

				lastx = x
				lasty = y
				lastrange = range
			}

			return currentShadowCast
		}


		var recalculate = false

		if (x != lastx || y != lasty)
		{
			recalculate = true
		}
		else if (range > lastrange)
		{
			recalculate = true
		}
		else
		{
			for (pos in opaqueTiles)
			{
				if (!collisionGrid.tryGet(pos.x, pos.y, true)!!)
				{
					recalculate = true // something has moved
					break
				}
			}

			if (!recalculate)
			{
				for (pos in clearTiles)
				{
					if (collisionGrid.tryGet(pos.x, pos.y, true)!!)
					{
						recalculate = true // something has moved
						break
					}
				}
			}
		}

		if (recalculate)
		{
			Point.freeAllTS(currentShadowCast)
			currentShadowCast.clear()

			Point.freeAllTS(invCurrentShadowCast)
			invCurrentShadowCast.clear()

			// build grid
			var anySolid = false
			val resistanceGrid = Array(range * 2 + 1) { DoubleArray(range * 2 + 1) }
			for (ix in 0 until range * 2 + 1)
			{
				for (iy in 0 until range * 2 + 1)
				{
					val gx = ix + x - range
					val gy = iy + y - range

					if (collisionGrid.inBounds(gx, gy))
					{
						resistanceGrid[ix][iy] = (if (collisionGrid[gx, gy]) 1 else 0).toDouble()
					}
					else
					{
						resistanceGrid[ix][iy] = 1.0
					}

					anySolid = anySolid || resistanceGrid[ix][iy] == 1.0
				}
			}

			var rawOutput: Array<DoubleArray>? = null
			if (anySolid)
			{
				rawOutput = fov.calculateFOV(resistanceGrid, range, range, range.toDouble() + 1, Radius.SQUARE)
			}

			for (ix in 0 until range * 2 + 1)
			{
				for (iy in 0 until range * 2 + 1)
				{
					val gx = ix + x - range
					val gy = iy + y - range

					if (collisionGrid.inBounds(gx, gy) && Vector2.dst2(gx.toFloat(), gy.toFloat(), x.toFloat(), y.toFloat()) <= (range * range).toFloat())
					{
						if ((!anySolid || rawOutput!![ix][iy] > 0))
						{
							val point = Point.obtainTS().set(gx, gy)
							currentShadowCast.add(point)
						}
						else
						{
							val point = Point.obtainTS().set(gx, gy)
							invCurrentShadowCast.add(point)
						}
					}
				}
			}

			// build list of clear/opaque
			opaqueTiles.clear()
			clearTiles.clear()

			for (pos in currentShadowCast)
			{
				if (pos.x < 0 || pos.y < 0 || pos.x >= collisionGrid.xSize || pos.y >= collisionGrid.ySize)
				{
					continue
				}

				if (collisionGrid[pos.x, pos.y])
				{
					opaqueTiles.add(pos)
				}
				else
				{
					clearTiles.add(pos)
				}
			}
			lastx = x
			lasty = y
			lastrange = range

			updateOpaqueRegions()

			if (anySolid)
			{
				updateCurrentCastRegions(rawOutput!!)
			}
		}

		return currentShadowCast
	}
}
