package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.NumberUtils
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.HSLColour

/**
 * Created by Philip on 30-Mar-16.
 */

const val componentRange = 100
const val bRange = componentRange
const val gRange = bRange * componentRange
const val rRange = gRange * componentRange

class Colour()
{
	var fixed: Boolean = false

	internal var r: Float = 0f
	internal var g: Float = 0f
	internal var b: Float = 0f
	internal var a: Float = 0f

	constructor(col: Float, fixed: Boolean = false) : this()
	{
		set(col)
		this.fixed = fixed
	}

	constructor(col: Color, fixed: Boolean = false) : this()
	{
		set(col.r, col.g, col.b, col.a)
		this.fixed = fixed
	}

	constructor(col: Colour, fixed: Boolean = false) : this()
	{
		set(col.r, col.g, col.b, col.a)
		this.fixed = fixed
	}

	constructor(r: Float, g:Float, b:Float, a:Float, fixed: Boolean = false) : this()
	{
		set(r, g, b, a)
		this.fixed = fixed
	}

	constructor(r: Int, g:Int, b:Int, a:Int, fixed: Boolean = false) : this()
	{
		set(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f)
		this.fixed = fixed
	}

	fun copy(): Colour = Colour(this)

	fun isWhite(): Boolean = r == 1f && g == 1f && b == 1f && a == 1f

	fun r(r: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.r = r
		return this
	}

	fun g(g: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.g = g
		return this
	}

	fun b(b: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.b = b
		return this
	}

	fun a(a: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.a = a
		return this
	}

	fun set(other: Colour): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r = other.r
		g = other.g
		b = other.b
		a = other.a

		this.cachedR = other.cachedR
		this.cachedG = other.cachedG
		this.cachedB = other.cachedB
		this.cachedA = other.cachedA
		this.cachedFB = other.cachedFB

		this.scaledCachedR = other.scaledCachedR
		this.scaledCachedG = other.scaledCachedG
		this.scaledCachedB = other.scaledCachedB
		this.scaledCachedA = other.scaledCachedA
		this.scaledCachedFB.set(other.scaledCachedFB)

		return this
	}

	fun set(col: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r = col
		g = col
		b = col
		a = col

		return this
	}

	fun set(col: Color, packed: Float? = null): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r = col.r
		g = col.g
		b = col.b
		a = col.a

		if (packed != null)
		{
			cachedR = r
			cachedG = g
			cachedB = b
			cachedA = a
			cachedFB = packed
		}

		return this
	}

	fun set(r: Float, g:Float, b:Float, a:Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.r = r
		this.g = g
		this.b = b
		this.a = a

		return this
	}

	fun clamp()
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r = r.clamp(0f, 1f)
		g = g.clamp(0f, 1f)
		b = b.clamp(0f, 1f)
		a = a.clamp(0f, 1f)
	}

	fun mul(other: Colour) : Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		timesAssign(other)
		return this
	}

	fun mul(r: Float, g: Float, b: Float, a: Float): Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.r *= r
		this.g *= g
		this.b *= b
		this.a *= a
		return this
	}

	fun mul(value: Float): Colour
	{
		return mul(value, value, value, 1f)
	}

	operator fun times(other: Colour): Colour
	{
		val col = Colour(this)
		col.r *= other.r
		col.g *= other.g
		col.b *= other.b
		col.a *= other.a

		return col
	}

	operator fun times(other: Float): Colour
	{
		val col = Colour(this)
		col.r *= other
		col.g *= other
		col.b *= other
		col.a *= other

		return col
	}


	operator fun timesAssign(other: Colour)
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r *= other.r
		g *= other.g
		b *= other.b
		a *= other.a
	}

	operator fun timesAssign(other: Color)
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r *= other.r
		g *= other.g
		b *= other.b
		a *= other.a
	}

	operator fun timesAssign(alpha: Float)
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r *= alpha
		g *= alpha
		b *= alpha
		a *= alpha
	}

	operator fun plusAssign(other: Colour)
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r += other.r
		g += other.g
		b += other.b
		a += other.a
	}

	operator fun divAssign(value: Float)
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		r /= value
		g /= value
		b /= value
		a /= value
	}

	fun lerp(target: Colour, t: Float) : Colour
	{
		if (fixed) throw Exception("Tried to modify fixed colour!")

		this.r += t * (target.r - this.r)
		this.g += t * (target.g - this.g)
		this.b += t * (target.b - this.b)
		this.a += t * (target.a - this.a)

		return this
	}

	fun lerpHSV(target: Colour, t: Float): Colour
	{
		val start = HSLColour(this)
		val end = HSLColour(target)

		val lerped = start.lerp(end, t)

		return lerped.toRGB()
	}

	fun toFloatBits() : Float
	{
		if (cachedR == r && cachedG == g && cachedB == b && cachedA == a) return cachedFB
		else
		{
			val r = r.clamp(0f, 1f)
			val g = g.clamp(0f, 1f)
			val b = b.clamp(0f, 1f)
			val a = a.clamp(0f, 1f)

			val intBits = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
			cachedFB = NumberUtils.intToFloatColor(intBits)

			cachedR = this.r
			cachedB = this.b
			cachedG = this.g
			cachedA = this.a

			return cachedFB
		}
	}
	var cachedR: Float = -1f
	var cachedG: Float = -1f
	var cachedB: Float = -1f
	var cachedA: Float = -1f
	var cachedFB: Float = -1f

	fun toScaledFloatBits() : Vector2
	{
		if (scaledCachedR == r && scaledCachedG == g && scaledCachedB == b && scaledCachedA == a) return scaledCachedFB
		else
		{
			val mag = max(r, g, b).clamp(1f, 254f)

			val r = (r / mag).clamp(0f, 1f)
			val g = (g / mag).clamp(0f, 1f)
			val b = (b / mag).clamp(0f, 1f)
			val a = a.clamp(0f, 1f)

			if (mag != 1f)
			{

			}

			val intBits = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
			val fb = NumberUtils.intToFloatColor(intBits)

			scaledCachedFB.set(fb, mag)

			scaledCachedR = this.r
			scaledCachedG = this.b
			scaledCachedB = this.g
			scaledCachedA = this.a

			return scaledCachedFB
		}
	}
	var scaledCachedR: Float = -1f
	var scaledCachedG: Float = -1f
	var scaledCachedB: Float = -1f
	var scaledCachedA: Float = -1f
	val scaledCachedFB = Vector2()

	fun vec3(): Vector3 = Vector3(r, g, b)

	fun color() : Color
	{
		return Color(r, g, b, a)
	}

	override fun equals(other: Any?): Boolean
	{
		if (other !is Colour) return false

		if (other.r != r) return false
		if (other.g != g) return false
		if (other.b != b) return false
		if (other.a != a) return false

		return true
	}

	override fun hashCode(): Int
	{
		return ((r * rRange + g * gRange + b * bRange + a) * 255).toInt()
	}

	fun freeTS()
	{
		synchronized(pool)
		{
			pool.free(this)
		}
	}

	companion object
	{
		internal val BLACK = Colour(Color.BLACK, true)
		internal val WHITE = Colour(Color.WHITE, true)
		internal val LIGHT_GRAY = Colour(Color.LIGHT_GRAY, true)
		internal val DARK_GRAY = Colour(Color.DARK_GRAY, true)
		internal val GOLD = Colour(Color.GOLD, true)
		internal val BLUE = Colour(Color.BLUE, true)
		internal val GREEN = Colour(Color.GREEN, true)
		internal val RED = Colour(Color.RED, true)
		internal val CYAN = Colour(Color.CYAN, true)
		internal val PINK = Colour(Color.PINK, true)
		internal val ORANGE = Colour(Color.ORANGE, true)
		internal val YELLOW = Colour(Color.YELLOW, true)
		internal val TRANSPARENT = Colour(0f, 0f, 0f, 0f, true)

		fun random(s: Float = 0.9f, l: Float = 0.7f): Colour
		{
			val hsl = HSLColour(Random.random(), s, l, 1.0f)
			return hsl.toRGB()
		}

		val pool: Pool<Colour> = object : Pool<Colour>() {
			override fun newObject(): Colour
			{
				return Colour()
			}
		}

		fun obtainTS(): Colour
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}