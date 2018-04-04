package com.lyeeedar

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.*

// ----------------------------------------------------------------------
enum class BlendMode constructor(val src: Int, val dst: Int)
{
	MULTIPLICATIVE(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
	ADDITIVE(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
}

// ----------------------------------------------------------------------
enum class Gender
{
	MALE,
	FEMALE
}

// ----------------------------------------------------------------------
enum class Sin constructor(val colour: Colour)
{
	ANGER(Colour(0.78f, 0.46f, 0.46f, 1f)),
	SLOTH(Colour(0.8f, 0.69f, 0.52f, 1f)),
	ENVY(Colour(0.1f, 0.93f, 1f, 1f)),
	LUST(Colour(0.78f, 0.31f, 0.7f, 1f)),
	GREED(Colour(0.25f, 0.61f, 0.29f, 1f)),
	PRIDE(Colour(1f, 0.91f, 0f, 1f));

	companion object
	{
		val Values = Sin.values()
	}
}

// ----------------------------------------------------------------------
enum class Rarity
{
	COMMON,
	UNCOMMON,
	RARE,
	MYSTICAL,
	LEGENDARY;


	companion object
	{

		val Values = Rarity.values()
	}
}

// ----------------------------------------------------------------------
enum class Direction private constructor(val x: Int, val y: Int, val identifier: String)
{
	CENTER(0, 0, "C"),
	NORTH(0, 1, "N"),
	SOUTH(0, -1, "S"),
	EAST(1, 0, "E"),
	WEST(-1, 0, "W"),
	NORTHEAST(1, 1, "NE"),
	NORTHWEST(-1, 1, "NW"),
	SOUTHEAST(1, -1, "SE"),
	SOUTHWEST(-1, -1, "SW");

	val angle: Float // In degrees
	lateinit var clockwise: Direction
		private set
	lateinit var anticlockwise: Direction
		private set
	var isCardinal = false
		private set

	val cardinalClockwise: Direction
			get() = clockwise.clockwise
	val cardinalAnticlockwise: Direction
			get() = anticlockwise.anticlockwise

	init
	{
		angle = vectorToAngle(x.toFloat(), y.toFloat())
	}

	val opposite: Direction
		get() = getDirection(x * -1, y * -1)

	companion object
	{
		init
		{
			// Setup neighbours
			CENTER.clockwise = CENTER
			CENTER.anticlockwise = CENTER

			NORTH.anticlockwise = NORTHWEST
			NORTH.clockwise = NORTHEAST

			NORTHEAST.anticlockwise = NORTH
			NORTHEAST.clockwise = EAST

			EAST.anticlockwise = NORTHEAST
			EAST.clockwise = SOUTHEAST

			SOUTHEAST.anticlockwise = EAST
			SOUTHEAST.clockwise = SOUTH

			SOUTH.anticlockwise = SOUTHEAST
			SOUTH.clockwise = SOUTHWEST

			SOUTHWEST.anticlockwise = SOUTH
			SOUTHWEST.clockwise = WEST

			WEST.anticlockwise = SOUTHWEST
			WEST.clockwise = NORTHWEST

			NORTHWEST.anticlockwise = WEST
			NORTHWEST.clockwise = NORTH

			// Setup is cardinal
			NORTH.isCardinal = true
			SOUTH.isCardinal = true
			EAST.isCardinal = true
			WEST.isCardinal = true
		}

		val CardinalValues = arrayOf(NORTH, EAST, SOUTH, WEST)
		val DiagonalValues = arrayOf(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHWEST)
		val Values = Direction.values()

		fun getDirection(point: Point): Direction
		{
			return getDirection(point.x, point.y)
		}

		fun getDirection(path: kotlin.Array<Vector2>): Direction
		{
			val x = path.last().x - path.first().x
			val y = path.last().y - path.first().y

			return getDirection(x.toInt(), y.toInt())
		}

		fun getDirection(dir: FloatArray): Direction
		{
			val x = if (dir[0] < 0) -1 else if (dir[0] > 0) 1 else 0
			val y = if (dir[1] < 0) -1 else if (dir[1] > 0) 1 else 0

			return getDirection(x, y)
		}

		fun getDirection(dir: IntArray): Direction
		{
			return getDirection(dir[0], dir[1])
		}

		fun getDirection(dx: Int, dy: Int): Direction
		{
			var dx = dx
			var dy = dy
			dx = MathUtils.clamp(dx, -1, 1)
			dy = MathUtils.clamp(dy, -1, 1)

			var d = CENTER

			for (dir in Values)
			{
				if (dir.x == dx && dir.y == dy)
				{
					d = dir
					break
				}
			}

			return d
		}

		fun getCardinalDirection(p1: Point, p2: Point): Direction
		{
			return getCardinalDirection(p1.x - p2.x, p1.y - p2.y)
		}

		fun getCardinalDirection(dx: Int, dy: Int): Direction
		{
			if (dx == 0 && dy == 0)
			{
				return CENTER
			}

			if (Math.abs(dx) > Math.abs(dy))
			{
				if (dx < 0)
				{
					return WEST
				} else
				{
					return EAST
				}
			} else
			{
				if (dy < 0)
				{
					return SOUTH
				} else
				{
					return NORTH
				}
			}
		}

		fun getDirection(p1: Point, p2: Point): Direction
		{
			return getDirection(p2.x - p1.x, p2.y - p1.y)
		}

		fun buildCone(dir: Direction, start: Point, range: Int): Array<Point>
		{
			val hitTiles = Array<Point>()

			val anticlockwise = dir.anticlockwise
			val clockwise = dir.clockwise

			val acwOffset = Point.obtain().set(dir.x - anticlockwise.x, dir.y - anticlockwise.y)
			val cwOffset = Point.obtain().set(dir.x - clockwise.x, dir.y - clockwise.y)

			hitTiles.add(Point.obtain().set(start.x + anticlockwise.x, start.y + anticlockwise.y))

			hitTiles.add(Point.obtain().set(start.x + dir.x, start.y + dir.y))

			hitTiles.add(Point.obtain().set(start.x + clockwise.x, start.y + clockwise.y))

			for (i in 2..range)
			{
				val acx = start.x + anticlockwise.x * i
				val acy = start.y + anticlockwise.y * i

				val nx = start.x + dir.x * i
				val ny = start.y + dir.y * i

				val cx = start.x + clockwise.x * i
				val cy = start.y + clockwise.y * i

				// add base tiles
				hitTiles.add(Point.obtain().set(acx, acy))
				hitTiles.add(Point.obtain().set(nx, ny))
				hitTiles.add(Point.obtain().set(cx, cy))

				// add anticlockwise - mid
				for (ii in 1..range)
				{
					val px = acx + acwOffset.x * ii
					val py = acy + acwOffset.y * ii

					hitTiles.add(Point.obtain().set(px, py))
				}

				// add mid - clockwise
				for (ii in 1..range)
				{
					val px = cx + cwOffset.x * ii
					val py = cy + cwOffset.y * ii

					hitTiles.add(Point.obtain().set(px, py))
				}
			}

			acwOffset.free()
			cwOffset.free()

			return hitTiles
		}
	}
}

// ----------------------------------------------------------------------
enum class ElementType
{
	PHYSICAL,

	ELEMENTAL,
	CORRUPTION,
	PURIFICATION;

	lateinit var weakTo: ElementType
	lateinit var strongAgainst: ElementType

	companion object
	{
		init
		{
			PHYSICAL.weakTo = ELEMENTAL
			PHYSICAL.strongAgainst = PURIFICATION

			ELEMENTAL.weakTo = CORRUPTION
			ELEMENTAL.strongAgainst = PHYSICAL

			CORRUPTION.weakTo = PURIFICATION
			CORRUPTION.strongAgainst = ELEMENTAL

			PURIFICATION.weakTo = PHYSICAL
			PURIFICATION.strongAgainst = CORRUPTION
		}

		val Values = ElementType.values()

		fun getElementMap(defaultValue: Float = 0f): FastEnumMap<ElementType, Float>
		{
			val map: FastEnumMap<ElementType, Float> = FastEnumMap(ElementType::class.java)

			for (elem in Values)
			{
				map[elem] = defaultValue
			}

			return map
		}

		fun load(xml: XmlData, existing: FastEnumMap<ElementType, Float>? = null): FastEnumMap<ElementType, Float>
		{
			var map = existing

			if (map == null)
			{
				map = getElementMap(0f)
			}

			for (i in 0 until xml.childCount)
			{
				val el = xml.getChild(i)
				val elem = valueOf(el.name.toUpperCase())

				map[elem] = el.text.toFloat()
			}

			return map
		}
	}
}

// ----------------------------------------------------------------------
enum class EquipmentSlot
{
	WEAPON,
	ARMOUR,

	ACCESSORY1,
	ACCESSORY2,
	ACCESSORY3;

	companion object
	{
		val Values = EquipmentSlot.values()
	}
}

// ----------------------------------------------------------------------
enum class SpaceSlot
{
	FLOOR,
	FLOORDETAIL,
	WALL,
	WALLDETAIL,
	BELOWENTITY,
	ENTITY,
	ABOVEENTITY,
	LIGHT;


	companion object
	{

		val Values = SpaceSlot.values()
		val BasicValues = arrayOf(FLOOR, FLOORDETAIL, WALL, WALLDETAIL)
		val EntityValues = arrayOf(BELOWENTITY, ENTITY, ABOVEENTITY)
	}
}
