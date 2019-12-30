package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Direction
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.ciel
import com.lyeeedar.Util.lerp

class FixedVector2(val x: Float, val y: Float)
{
	constructor(vec: Vector2) : this(vec.x, vec.y)

	fun lerp(other: FixedVector2, alpha: Float, store: Vector2)
	{
		store.x = x.lerp(other.x, alpha)
		store.y = y.lerp(other.y, alpha)
	}
}

class EmitterKeyframe(
	val time: Float = 0f,
	val offset: FixedVector2 = FixedVector2(0f, 0f),
	val emissionRate: Float = 0f,
	val size: Float = 1f)
{

}

class Emitter(val particleEffect: ParticleEffect)
{
	val MAX_DELTA = 1f / 15f // dont update faster than 15fps

	enum class EmissionType
	{
		ABSOLUTE,
		ACCUMULATED
	}

	enum class SimulationSpace
	{
		LOCAL,
		WORLD
	}

	enum class EmissionShape
	{
		CIRCLE,
		BOX,
		CONE
	}

	enum class EmissionArea
	{
		INTERIOR,
		BORDER,
		CENTER
	}

	enum class EmissionDirection
	{
		RADIAL,
		RANDOM,
		UP,
		DOWN,
		LEFT,
		RIGHT
	}

	class SpawnInformation(val pos: Vector2, val dir: Vector2)
	private val spawnInfo = SpawnInformation(Vector2(), Vector2())
	private val temp = Vector2()

	val particles: Array<Particle> = Array(false, 1)
	val effectors: Array<Effector> = Array(false, 1)

	val position = Vector2()
	var rotation: Float = 0f
	val size: Vector2 = Vector2(1f, 1f)

	lateinit var type: EmissionType
	lateinit var simulationSpace: SimulationSpace

	var keyframeIndex: Int = 0
	lateinit var keyframe1: EmitterKeyframe
	lateinit var keyframe2: EmitterKeyframe
	var keyframeAlpha: Float = 0f
	var keyframes: kotlin.Array<EmitterKeyframe> = emptyArray()
	var singleBurst = false
	lateinit var particleSpeed: Range
	lateinit var particleRotation: Range
	lateinit var shape: EmissionShape
	var angle: Float = 0f
	var width: Float = 0f
	var height: Float = 0f
	var emitterRotation: Float = 0f
	lateinit var area: EmissionArea
	lateinit var dir: EmissionDirection
	var gravity: Float = 0f
	var isCollisionEmitter: Boolean = false
	var isBlockingEmitter: Boolean = true
	var killParticlesOnStop: Boolean = false
	var emissionStart = 0f

	var time: Float = 0f

	var emissionAccumulator: Float = 0f

	var emitted = false
	var stopped = false

	var currentSize = 1f
	val currentOffset = Vector2()
	val tempOffset = Vector2()

	fun lifetime() = keyframes.last().time + particles.maxBy { it.lifetime.v2 }!!.lifetime.v2
	fun complete(): Boolean
	{
		if (singleBurst)
		{
			return emitted && particles.all { it.complete() }
		}
		else
		{
			return (time >= keyframes.last().time || stopped) && particles.all { it.complete() }
		}
	}

	fun stop() { stopped = true }
	fun start() { stopped = false }

	fun killParticles()
	{
		for (particle in particles)
		{
			for (pdata in particle.particles)
			{
				pdata.free()
			}
			particle.particles.clear()
		}
	}

	fun update(delta: Float)
	{
		time += delta

		val scaledDelta = Math.min(delta, MAX_DELTA)

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

		keyframe1.offset.lerp(keyframe2.offset, keyframeAlpha, currentOffset)
		currentSize = keyframe1.size.lerp(keyframe2.size, keyframeAlpha)

		if (!stopped || (singleBurst && !emitted))
		{
			val duration = keyframes.last().time
			val rate = keyframe1.emissionRate.lerp(keyframe2.emissionRate, keyframeAlpha)

			if (duration == 0f || (singleBurst && !emitted) || time <= duration)
			{
				if (type == EmissionType.ABSOLUTE || singleBurst)
				{
					if (singleBurst)
					{
						if (!emitted && time >= emissionStart)
						{
							emitted = true

							val toSpawn = Math.max(0f, rate - particles.sumBy { it.particleCount() }).ciel()
							for (i in 1..toSpawn)
							{
								spawn()
							}
						}
					}
					else
					{
						emitted = true

						val toSpawn = Math.max(0f, rate - particles.sumBy { it.particleCount() }).ciel()
						for (i in 1..toSpawn)
						{
							spawn()
						}
					}
				}
				else
				{
					emissionAccumulator += scaledDelta * rate
					emitted = true

					while (emissionAccumulator > 1f)
					{
						emissionAccumulator -= 1f
						spawn()
					}
				}
			}
		}

		for (i in 0 until effectors.size)
		{
			val effector = effectors[i]
			effector.update(scaledDelta)
		}

		for (i in 0 until particles.size)
		{
			val particle = particles[i]
			particle.simulate(scaledDelta, gravity)
		}

		if (!stopped)
		{
			val duration = keyframes.last().time
			val rate = keyframe1.emissionRate.lerp(keyframe2.emissionRate, keyframeAlpha)

			if (duration == 0f || time <= duration)
			{
				if (type == EmissionType.ABSOLUTE && !singleBurst)
				{
					val toSpawn = Math.max(0f, rate - particles.sumBy { it.particleCount() }).ciel()
					for (i in 1..toSpawn)
					{
						spawn()
					}
				}
			}
		}
	}

	fun spawn()
	{
		val spawnInfo = when (shape)
		{
			EmissionShape.CIRCLE -> spawnCircle()
			EmissionShape.BOX -> spawnBox()
			EmissionShape.CONE -> spawnCone()
			else -> throw RuntimeException("Invalid emitter shape! $shape")
		}
		val spawnPos = spawnInfo.pos
		val spawnDir = spawnInfo.dir

		val velocity = when (dir)
		{
			EmissionDirection.RADIAL -> spawnDir.set(spawnInfo.dir)
			EmissionDirection.RANDOM -> spawnDir.set(spawnInfo.dir)
			EmissionDirection.UP -> spawnDir.set(Direction.NORTH.x.toFloat(), Direction.NORTH.y.toFloat())
			EmissionDirection.DOWN -> spawnDir.set(Direction.SOUTH.x.toFloat(), Direction.SOUTH.y.toFloat())
			EmissionDirection.LEFT -> spawnDir.set(Direction.WEST.x.toFloat(), Direction.WEST.y.toFloat())
			EmissionDirection.RIGHT -> spawnDir.set(Direction.EAST.x.toFloat(), Direction.EAST.y.toFloat())
			else -> throw RuntimeException("Invalid emitter direction type! $dir")
		}

		val speed = particleSpeed.lerp(Random.random())
		var localRot = particleRotation.lerp(Random.random()) + rotation
		val offset = tempOffset.set(currentOffset)

		if (particleEffect.flipX)
		{
			offset.x *= -1f
			spawnPos.x *= -1f
			velocity.x *= -1f
		}

		if (particleEffect.flipY)
		{
			offset.y *= -1f
			spawnPos.y *= -1f
			velocity.y *= -1f
		}

		if (particleEffect.flipX && particleEffect.flipY)
		{

		}
		else if (particleEffect.flipX || particleEffect.flipY)
		{
			localRot *= -1f
		}

		if (simulationSpace == SimulationSpace.WORLD)
		{
			spawnPos.rotate(rotation)
			spawnPos.add(position)

			// rotate offset
			temp.set(offset).scl(size).rotate(rotation)
			spawnPos.add(temp)

			velocity.scl(size)

			velocity.rotate(rotation)
		}
		else
		{
			// just add offset
			spawnPos.add(offset)
		}

		velocity.scl(speed)

		// pick random particle
		val particle = particles.random()
		particle.spawn(spawnPos, velocity, localRot)
	}

	fun spawnCone(): SpawnInformation
	{
		val angle = angle
		val width = width * currentSize * size.x
		val height = height * currentSize * size.y

		val ranVal = Random.random() - 0.5f
		val chosenAngle = ranVal * angle
		val h = when (area)
		{
			EmissionArea.INTERIOR -> Random.random() * height
			EmissionArea.BORDER -> height
			EmissionArea.CENTER -> 0f
		}

		temp.set(0f, h)
		temp.rotate(chosenAngle)
		temp.x += ranVal * width

		temp.rotate(emitterRotation)

		spawnInfo.pos.set(temp)
		spawnInfo.dir.set(0f, 1f)

		if (dir == EmissionDirection.RADIAL)
		{
			spawnInfo.dir.rotate(chosenAngle*-1f).nor()
		}
		else if (dir == EmissionDirection.RANDOM)
		{
			spawnInfo.dir.rotate((Random.random() - 0.5f) * angle).nor()
		}

		return spawnInfo
	}

	fun spawnCircle(): SpawnInformation
	{
		val width = width * currentSize * size.x
		val height = height * currentSize * size.y

		if (area == EmissionArea.INTERIOR)
		{
			val ranVal = Random.random()
			val sqrtRanVal = Math.sqrt(ranVal.toDouble()).toFloat()
			val phi = Random.random() * (2f * MathUtils.PI)
			val x = sqrtRanVal * MathUtils.cos(phi) * (width / 2f)
			val y = sqrtRanVal * MathUtils.sin(phi) * (height / 2f)

			temp.set(x, y)
		}
		else if (area == EmissionArea.BORDER)
		{
			val phi = Random.random() * (2f * MathUtils.PI)
			val x = MathUtils.cos(phi) * (width / 2f)
			val y = MathUtils.sin(phi) * (height / 2f)

			temp.set(x, y)
		}
		else if (area == EmissionArea.CENTER)
		{
			temp.set(0f, 0f)
		}
		else throw RuntimeException("Invalid emitter area type $area")

		spawnInfo.pos.set(temp)

		if (dir == EmissionDirection.RANDOM)
		{
			spawnInfo.dir.setToRandomDirection()
		}
		else if (dir == EmissionDirection.RADIAL)
		{
			spawnInfo.dir.set(temp).nor()
		}

		return spawnInfo
	}

	fun spawnBox(): SpawnInformation
	{
		val width = width * currentSize * size.x
		val height = height * currentSize * size.y

		if (area == EmissionArea.BORDER)
		{
			val w2 = width/2f
			val h2 = height/2f
			val p1 = Vector2(-w2, h2) // top left
			val p2 = Vector2(w2, h2) // top right
			val p3 = Vector2(w2, -h2) // bottom right
			val p4 = Vector2(-w2, -h2) // bottom left
			val points = arrayOf(p1, p2, p3, p4)
			val dists = floatArrayOf(width, height, width, height)
			for (i in 1 until dists.size) dists[i] += dists[i - 1]

			val totalDist = dists.last()
			val chosenDst = Random.random() * totalDist

			var i = 0
			while (i < dists.size)
			{
				if (dists[i] > chosenDst)
				{
					break
				}

				i++
			}
			if (i == points.size) i = points.size-1

			val delta = dists[i] - chosenDst
			val start = points[i]
			val end = if (i+1 == points.size) points[0] else points[i+1]
			val diff = start.dst(end)

			temp.set(start).lerp(end, delta / diff)
		}
		else if (area == EmissionArea.INTERIOR)
		{
			val x = Random.random() * width - (width / 2f)
			val y = Random.random() * height - (height / 2f)

			temp.set(x, y)
		}
		else if (area == EmissionArea.CENTER)
		{
			temp.set(0f, 0f)
		}
		else throw RuntimeException("Invalid emitter area type $area")

		temp.rotate(emitterRotation)

		spawnInfo.pos.set(temp)

		if (dir == EmissionDirection.RANDOM)
		{
			spawnInfo.dir.setToRandomDirection()
		}
		else if (dir == EmissionDirection.RADIAL)
		{
			spawnInfo.dir.set(temp).nor()
		}

		return spawnInfo
	}

	fun callCollisionFunc(func: (x: Int, y: Int) -> Unit)
	{
		if (!isCollisionEmitter) return

		for (particle in particles) particle.callCollisionFunc(func)
	}

	fun store(kryo: Kryo, output: Output)
	{
		output.writeInt(type.ordinal)
		output.writeInt(simulationSpace.ordinal)
		output.writeInt(shape.ordinal)
		output.writeFloat(angle)
		output.writeFloat(width)
		output.writeFloat(height)
		output.writeFloat(emitterRotation)
		output.writeInt(area.ordinal)
		output.writeInt(dir.ordinal)
		output.writeFloat(particleSpeed.v1)
		output.writeFloat(particleSpeed.v2)
		output.writeFloat(particleRotation.v1)
		output.writeFloat(particleRotation.v2)
		output.writeFloat(gravity)
		output.writeBoolean(isCollisionEmitter)
		output.writeBoolean(isBlockingEmitter)
		output.writeBoolean(killParticlesOnStop)
		output.writeFloat(emissionStart)

		output.writeInt(keyframes.size)
		for (keyframe in keyframes)
		{
			output.writeFloat(keyframe.time)

			output.writeFloat(keyframe.offset.x)
			output.writeFloat(keyframe.offset.y)

			output.writeFloat(keyframe.emissionRate)

			output.writeFloat(keyframe.size)
		}

		output.writeBoolean(singleBurst)

		output.writeInt(particles.size)
		for (particle in particles)
		{
			particle.store(kryo, output)
		}

		output.writeInt(effectors.size)
		for (effector in effectors)
		{
			effector.store(kryo, output)
		}
	}

	fun restore(kryo: Kryo, input: Input)
	{
		type = EmissionType.values()[input.readInt()]
		simulationSpace = SimulationSpace.values()[input.readInt()]
		shape = EmissionShape.values()[input.readInt()]
		angle = input.readFloat()
		width = input.readFloat()
		height = input.readFloat()
		emitterRotation = input.readFloat()
		area = EmissionArea.values()[input.readInt()]
		dir = EmissionDirection.values()[input.readInt()]
		particleSpeed = Range(input.readFloat(), input.readFloat())
		particleRotation = Range(input.readFloat(), input.readFloat())
		gravity = input.readFloat()
		isCollisionEmitter = input.readBoolean()
		isBlockingEmitter = input.readBoolean()
		killParticlesOnStop = input.readBoolean()
		emissionStart = input.readFloat()

		val numKeyframes = input.readInt()
		keyframes = kotlin.Array<EmitterKeyframe>(numKeyframes) { i -> EmitterKeyframe() }

		for (i in 0 until numKeyframes)
		{
			val time = input.readFloat()
			val offsetx = input.readFloat()
			val offsety = input.readFloat()
			val rate = input.readFloat()
			val size = input.readFloat()

			keyframes[i] = EmitterKeyframe(time, FixedVector2(offsetx, offsety), rate, size)
		}

		singleBurst = input.readBoolean()

		val numParticles = input.readInt()
		for (i in 0 until numParticles)
		{
			val particle = Particle(this)
			particle.restore(kryo, input)
			particles.add(particle)
		}

		val numEffectors = input.readInt()
		for (i in 0 until numEffectors)
		{
			val effector = Effector(this)
			effector.restore(kryo, input)
			effectors.add(effector)
		}
	}

	companion object
	{
		fun load(xml: XmlData, particleEffect: ParticleEffect): Emitter?
		{
			if (!xml.getBoolean("Enabled", true)) return null

			val emitter = Emitter(particleEffect)

			emitter.type = EmissionType.valueOf(xml.get("Type", "Absolute")!!.toUpperCase())
			emitter.simulationSpace = SimulationSpace.valueOf(xml.get("Space", "World")!!.toUpperCase())
			emitter.shape = EmissionShape.valueOf(xml.get("Shape", "Box")!!.toUpperCase())

			emitter.width = xml.getFloat("Width", 0f)
			if (emitter.width == 0f && emitter.shape != EmissionShape.CONE) emitter.width = 0.001f
			emitter.height = xml.getFloat("Height", 0f)
			if (emitter.height == 0f) emitter.height = 0.001f
			emitter.angle = xml.getFloat("Angle", 0f)
			if (emitter.angle == 0f && emitter.shape == EmissionShape.CONE)
			{
				emitter.angle = emitter.width
				emitter.width = 0f
			}

			emitter.emitterRotation = xml.getFloat("Rotation", 0f)
			emitter.area = EmissionArea.valueOf(xml.get("Area", "Interior")!!.toUpperCase())
			emitter.dir = EmissionDirection.valueOf(xml.get("Direction", "Radial")!!.toUpperCase())
			emitter.particleSpeed = Range(xml.get("ParticleSpeed"))
			emitter.particleRotation = Range(xml.get("ParticleRotation"))
			emitter.gravity = xml.getFloat("Gravity", 0f)
			emitter.isCollisionEmitter = xml.getBoolean("IsCollisionEmitter", false)
			emitter.isBlockingEmitter = xml.getBoolean("IsBlockingEmitter", true)
			emitter.killParticlesOnStop = xml.getBoolean("KillParticlesOnStop", false)

			// load timelines
			val offset = VectorTimeline()
			val offsetEl = xml.getChildByName("Offset")
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

			val emissionRate = LerpTimeline()
			val rateEls = xml.getChildByName("RateKeyframes")!!
			emissionRate.parse(rateEls, { it.toFloat() })

			val sizeMultiplier = LerpTimeline()
			val sizeEls = xml.getChildByName("SizeMultiplier")
			if (sizeEls != null)
			{
				sizeMultiplier.parse(sizeEls, { it.toFloat() })
			}
			else
			{
				sizeMultiplier.streams.add(Array())
				sizeMultiplier.streams[0].add(Pair(0f, 1f))
			}

			emitter.emissionStart = emissionRate.streams[0][0].first

			// Make map of times
			val times = ObjectSet<Float>()
			for (keyframe in offset.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in emissionRate.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in sizeMultiplier.streams.flatMap { it }) { times.add(keyframe.first) }

			val keyframes = kotlin.Array<EmitterKeyframe>(times.size) { i -> EmitterKeyframe() }
			var keyframeI = 0
			for (time in times.sortedBy { it })
			{
				val keyframe = EmitterKeyframe(
					time,
					FixedVector2(offset.valAt(0, time)),
					emissionRate.valAt(0, time),
					sizeMultiplier.valAt(0, time))
				keyframes[keyframeI++] = keyframe
			}
			emitter.keyframes = keyframes
			emitter.keyframe1 = keyframes[0]
			emitter.keyframe2 = keyframes[0]

			emitter.singleBurst = xml.getBoolean("SingleBurst", false)

			val particlesEl = xml.getChildByName("Particles")!!
			for (i in 0 until particlesEl.childCount)
			{
				val el = particlesEl.getChild(i)
				val particle = Particle.load(el, emitter)
				emitter.particles.add(particle)
			}

			val effectorsEl = xml.getChildByName("Effectors")
			if (effectorsEl != null)
			{
				for (i in 0 until effectorsEl.childCount)
				{
					val el = effectorsEl.getChild(i)
					val effector = Effector.load(el, emitter)
					emitter.effectors.add(effector)
				}
			}

			return emitter
		}
	}
}