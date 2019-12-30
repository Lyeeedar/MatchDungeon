package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.doesLineIntersectCircle
import com.lyeeedar.Util.lerp

class EffectorKeyframe(
	val time: Float = 0f,
	val offset: FixedVector2 = FixedVector2(0f, 0f),
	val strength: Range = Range(0f, 0f))

class Effector(val emitter: Emitter)
{
	enum class EffectorType
	{
		POINT
	}

	lateinit var type: EffectorType
	var time: Float = 0f

	var keyframeIndex: Int = 0
	lateinit var keyframe1: EffectorKeyframe
	lateinit var keyframe2: EffectorKeyframe
	var keyframeAlpha: Float = 0f
	var keyframes: kotlin.Array<EffectorKeyframe> = emptyArray()

	var sinkRadius: Float = 0f

	val offset = Vector2()

	val tempOffset = Vector2()
	val temp = Vector2()

	fun update(delta: Float)
	{
		time += delta

		var keyframeIndex = keyframeIndex
		while (keyframeIndex < keyframes.size-1)
		{
			if (time >= keyframes[keyframeIndex+1].time)
			{
				keyframeIndex++
			}
			else
			{
				break
			}
		}

		keyframe1 = keyframes[keyframeIndex]
		//val keyframe2: EmitterKeyframe
		val alpha: Float

		if (keyframeIndex < keyframes.size-1)
		{
			keyframe2 = keyframes[keyframeIndex+1]
			alpha = (time - keyframe1.time) / (keyframe2.time - keyframe1.time)
		}
		else
		{
			keyframe2 = keyframes[keyframeIndex]
			alpha = 0f
		}

		keyframeAlpha = alpha

		keyframe1.offset.lerp(keyframe2.offset, keyframeAlpha, offset)

		for (i in 0 until emitter.particles.size)
		{
			val particle = emitter.particles[i]

			for (i in 0 until particle.particles.size)
			{
				val particleData = particle.particles[i]

				when (type)
				{
					EffectorType.POINT -> doPointEffector(delta, particleData)
				}

				val strength = keyframe1.strength.lerp(particleData.ranVal).lerp(keyframe2.strength.lerp(particleData.ranVal), keyframeAlpha)
				if (strength != 0f)
				{
					val withinSink: Boolean = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
					{
						particleData.position.dst2(offset) <= sinkRadius * sinkRadius
					}
					else
					{
						val offset = tempOffset.set(offset)
						temp.set(offset).scl(emitter.size).rotate(emitter.rotation)
						offset.set(emitter.position).add(temp)

						val sinkRadius = sinkRadius * emitter.size.x
						particleData.position.dst2(offset) <= sinkRadius * sinkRadius
					}

					if (withinSink || doesLineIntersectCircle(particleData.position, temp.set(particleData.velocity).scl(delta).add(particleData.position), offset, sinkRadius))
					{
						particleData.life = particle.lifetime.v2
					}
				}
			}
		}
	}

	fun doPointEffector(delta: Float, particleData: ParticleData)
	{
		val offset = tempOffset.set(offset)

		if (emitter.particleEffect.flipX)
		{
			offset.x *= -1f
		}

		if (emitter.particleEffect.flipY)
		{
			offset.y *= -1f
		}

		if (emitter.simulationSpace == Emitter.SimulationSpace.WORLD)
		{
			// rotate offset
			temp.set(offset).scl(emitter.size).rotate(emitter.rotation)
			offset.set(emitter.position).add(temp)
		}

		val vec = temp.set(particleData.position).sub(offset).nor()

		val strength = keyframe1.strength.lerp(particleData.ranVal).lerp(keyframe2.strength.lerp(particleData.ranVal), keyframeAlpha)

		val impulse = vec.scl(delta * strength)
		impulse.scl(emitter.size)

		particleData.velocity.add(impulse)
	}

	fun store(kryo: Kryo, output: Output)
	{
		output.writeInt(type.ordinal, true)

		output.writeInt(keyframes.size)
		for (keyframe in keyframes)
		{
			output.writeFloat(keyframe.time)

			output.writeFloat(keyframe.offset.x)
			output.writeFloat(keyframe.offset.y)

			output.writeFloat(keyframe.strength.v1)
			output.writeFloat(keyframe.strength.v2)
		}

		output.writeFloat(sinkRadius)
	}

	fun restore(kryo: Kryo, input: Input)
	{
		type = EffectorType.values()[input.readInt(true)]

		val numKeyframes = input.readInt()
		keyframes = kotlin.Array<EffectorKeyframe>(numKeyframes) { i -> EffectorKeyframe() }

		for (i in 0 until numKeyframes)
		{
			val time = input.readFloat()
			val offsetx = input.readFloat()
			val offsety = input.readFloat()
			val strengthMin = input.readFloat()
			val strengthMax = input.readFloat()

			keyframes[i] = EffectorKeyframe(time, FixedVector2(offsetx, offsety), Range(strengthMin, strengthMax))
		}

		sinkRadius = input.readFloat()
	}

	companion object
	{
		fun load(xmlData: XmlData, emitter: Emitter): Effector
		{
			val effector = Effector(emitter)
			effector.type = EffectorType.valueOf(xmlData.get("Type", "Point")!!.toUpperCase())

			// load timelines
			val offset = VectorTimeline()
			val offsetEl = xmlData.getChildByName("Offset")
			if (offsetEl != null)
			{
				offset.parse(offsetEl, fun(off: String): Vector2 {
					val split = off.split(",")
					val x = split[0].toFloat()
					val y = split[1].toFloat()
					return Vector2(x, y)
				})

				if (offset.streams.size == 0)
				{
					offset.streams.add(Array())
					offset.streams[0].add(Pair(0f, Vector2()))
				}
			}
			else
			{
				offset.streams.add(Array())
				offset.streams[0].add(Pair(0f, Vector2()))
			}

			val strength = RangeLerpTimeline()
			val strengthEl = xmlData.getChildByName("Strength")
			if (strengthEl != null)
			{
				strength.parse(strengthEl, fun(off: String): Range {
					val split = off.split("|")
					val x = split[0].toFloat()
					val y = split[1].toFloat()
					return Range(x, y)
				})

				if (strength.streams.size == 0)
				{
					strength.streams.add(Array())
					strength.streams[0].add(Pair(0f, Range(0f, 0f)))
				}
			}
			else
			{
				strength.streams.add(Array())
				strength.streams[0].add(Pair(0f, Range(0f, 0f)))
			}

			// Make map of times
			val times = ObjectSet<Float>()
			for (keyframe in offset.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in strength.streams.flatMap { it }) { times.add(keyframe.first) }

			val keyframes = kotlin.Array<EffectorKeyframe>(times.size) { i -> EffectorKeyframe() }
			var keyframeI = 0
			for (time in times.sortedBy { it })
			{
				val keyframe = EffectorKeyframe(
					time,
					FixedVector2(offset.valAt(0, time)),
					strength.valAt(0, time).copy())
				keyframes[keyframeI++] = keyframe
			}
			effector.keyframes = keyframes
			effector.keyframe1 = keyframes[0]
			effector.keyframe2 = keyframes[0]

			effector.sinkRadius = xmlData.getFloat("SinkRadius", 0f)

			return effector
		}
	}
}