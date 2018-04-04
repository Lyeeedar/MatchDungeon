package com.lyeeedar.Renderables.Animation

import com.badlogic.gdx.utils.reflect.ClassReflection
import com.lyeeedar.Util.Colour
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
