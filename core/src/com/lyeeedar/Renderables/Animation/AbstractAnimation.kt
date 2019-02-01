package com.lyeeedar.Renderables.Animation

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.UnsmoothedPath
import com.lyeeedar.Util.XmlData

abstract class AbstractAnimation
{
	var startDelay = 0f

	var isBlocking = true

	abstract fun renderOffset(screenPositionMode: Boolean): FloatArray?
	abstract fun renderScale(): FloatArray?
	abstract fun renderColour(): Colour?
	abstract fun renderRotation(): Float?

	abstract fun duration(): Float
	abstract fun time(): Float
	fun remaining() = Math.max(duration() - time(), 0f)

	abstract fun update(delta: Float): Boolean
	abstract fun parse(xml: XmlData)

	abstract fun free()

	abstract fun copy(): AbstractAnimation

	companion object
	{
		fun load(xml:XmlData): AbstractAnimation
		{
			val uname = xml.name.toUpperCase()
			val c = getClass(uname)
			val instance = ClassReflection.getConstructor(c).newInstance() as AbstractAnimation

			instance.parse(xml)

			return instance
		}

		fun getClass(name: String): Class<out AbstractAnimation>
		{
			val type = when(name) {
				"MOVE" -> MoveAnimation::class.java
				"BOUNCE" -> BumpAnimation::class.java
				"BLINK" -> BlinkAnimation::class.java

			// ARGH everything broke
				else -> throw RuntimeException("Invalid sprite animation type: $name")
			}

			return type
		}
	}
}

abstract class AbstractAnimationDefinition
{
	var speedMultiplier: Float = 0f

	abstract fun parse(xmlData: XmlData)
	abstract fun getAnimation(duration: Float, path: Array<Vector2>): AbstractAnimation

	companion object
	{
		fun load(xmlData: XmlData): AbstractAnimationDefinition
		{
			val def = when(xmlData.getAttribute("meta:RefKey").toUpperCase())
			{
				"MOVEANIMATION" -> MoveAnimationDefinition()
				"LEAPANIMATION" -> LeapAnimationDefinition()
				else -> throw Exception("Unknown animation definition type '" + xmlData.getAttribute("meta:RefKey") + "'!")
			}

			def.parse(xmlData)
			def.speedMultiplier = xmlData.getFloat("SpeedMultiplier")

			return def
		}
	}
}

class MoveAnimationDefinition : AbstractAnimationDefinition()
{
	override fun getAnimation(duration: Float, path: Array<Vector2>): AbstractAnimation
	{
		return MoveAnimation.obtain().set(duration * speedMultiplier, UnsmoothedPath(path), Interpolation.linear)
	}

	override fun parse(xmlData: XmlData)
	{

	}
}

class LeapAnimationDefinition : AbstractAnimationDefinition()
{
	var height: Float = 0f

	override fun getAnimation(duration: Float, path: Array<Vector2>): AbstractAnimation
	{
		return LeapAnimation.obtain().set(duration * speedMultiplier, path, height)
	}

	override fun parse(xmlData: XmlData)
	{
		height = xmlData.getFloat("Height")
	}
}
