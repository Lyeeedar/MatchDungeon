package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pools
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.*
import ktx.collections.set


/**
 * Created by Philip on 14-Aug-16.
 */

class TextureOverride(val oldName: String, val newName: String, val blendMode: BlendMode?)

class ParticleEffectDescription(val path: String)
{
	var colour = Colour.WHITE.copy()
	var flipX = false
	var flipY = false
	var scale = 1f
	var useFacing = true
	var timeMultiplier = 1f
	var killOnAnimComplete = false

	val textureOverrides = Array<TextureOverride>()

	fun getParticleEffect(): ParticleEffect
	{
		val effect = ParticleEffect.load(path, this)
		effect.colour = colour
		effect.flipX = flipX
		effect.flipY = flipY
		effect.scale = scale
		effect.useFacing = useFacing
		effect.timeMultiplier = timeMultiplier
		effect.killOnAnimComplete = killOnAnimComplete

		return effect
	}

	fun getTexture(name: String): TextureOverride?
	{
		for (texOverride in textureOverrides)
		{
			if (texOverride.oldName == name)
			{
				return texOverride
			}
		}

		return null
	}
}

class ParticleEffect(val description: ParticleEffectDescription) : Renderable()
{
	private lateinit var loadPath: String

	var colour: Colour = Colour(Color.WHITE)

	var loop = true
	var completed = false
	var killOnAnimComplete = false
	private var warmupTime = 0f
	private var doneWarmup = false
	val emitters = Array<Emitter>(1)

	// local stuff
	val position = Vector2()
	var lockPosition = false
	var facing: Direction = Direction.NORTH
	var useFacing = true

	var scale = 1f

	var collisionFun: ((x: Int, y: Int) -> Unit)? = null

	var isShortened = false
	var timeMultiplier = 1f

	var particleLight: ParticleLight? = null

	override var light: Light?
		get() = particleLight?.light
		set(value) { throw Exception("Cannot set particle light!") }

	val time: Float
		get() = (animation?.time() ?: emitters.minBy { it.time }!!.time)

	val lifetime: Float
		get() = (animation?.duration() ?: emitters.maxBy { it.lifetime() }!!.lifetime())

	val blockinglifetime: Float
		get() = (animation?.duration() ?: emitters.filter { it.isBlockingEmitter }.maxBy { it.lifetime() }!!.lifetime())

	fun start()
	{
		for (emitter in emitters)
		{
			emitter.time = 0f
			emitter.emitted = false
			emitter.start()
		}
	}

	fun stop()
	{
		for (emitter in emitters)
		{
			emitter.stop()
			if (emitter.killParticlesOnStop)
			{
				emitter.killParticles()
			}
		}
	}

	override fun doUpdate(delta: Float): Boolean
	{
		val delta = delta * timeMultiplier

		var complete = false

		complete = animation?.update(delta) ?: false
		if (complete)
		{
			animation?.free()
			animation = null

			if (killOnAnimComplete)
			{
				stop()
			}
		}

		val posOffset = animation?.renderOffset(false)
		val x = position.x + (posOffset?.get(0) ?: 0f)
		val y = position.y + (posOffset?.get(1) ?: 0f)

		val scale = animation?.renderScale()
		val sx = size[0].toFloat() * (scale?.get(0) ?: 1f) * this.scale
		val sy = size[1].toFloat() * (scale?.get(1) ?: 1f) * this.scale

		for (emitter in emitters)
		{
			emitter.rotation = rotation
			emitter.position.set(x, y)
			emitter.size.x = sx
			emitter.size.y = sy
		}

		if (warmupTime > 0f && !doneWarmup)
		{
			doneWarmup = true
			val deltaStep = 1f / 15f // simulate at 15 fps
			val steps = (warmupTime / deltaStep).toInt()
			for (i in 0 until steps)
			{
				for (emitter in emitters) emitter.update(deltaStep)
			}
		}

		for (emitter in emitters) emitter.update(delta)

		if (collisionFun != null)
		{
			for (emitter in emitters) emitter.callCollisionFunc(collisionFun!!)
		}

		if (animation == null)
		{
			if (complete())
			{
				for (emitter in emitters) emitter.time = 0f
				complete = true
				completed = true

				if (!loop) stop()
				else for (emitter in emitters) emitter.emitted = false
			}
			else
			{
				complete = false
			}
		}

		particleLight?.update(time, max(sx, sy))

		return complete
	}

	fun complete() = emitters.all{ it.complete() }
	fun blocked() = emitters.any { it.isBlockingEmitter && !it.complete() }

	override val isBlocking: Boolean
		get() = blocked()

	fun setPosition(x: Float, y: Float)
	{
		position.set(x, y)
	}

	override fun doRender(batch: Batch, x: Float, y: Float, tileSize: Float)
	{

	}

	fun debug(shape: ShapeRenderer, offsetx: Float, offsety: Float, tileSize: Float, drawEmitter: Boolean, drawParticles: Boolean, drawEffectors: Boolean)
	{
		val posOffset = animation?.renderOffset(false)
		val x = position.x + (posOffset?.get(0) ?: 0f)
		val y = position.y + (posOffset?.get(1) ?: 0f)

		val worldx = x * tileSize + offsetx
		val worldy = y * tileSize + offsety

		// draw effect center
		shape.color = Color.CYAN
		shape.rect(worldx - 5f, worldy - 5f,10f, 10f)

		val temp = Pools.obtain(Vector2::class.java)
		val temp2 = Pools.obtain(Vector2::class.java)
		val temp3 = Pools.obtain(Vector2::class.java)
		val temp4 = Pools.obtain(Vector2::class.java)
		val temp5 = Pools.obtain(Vector2::class.java)

		// draw emitter volumes
		for (emitter in emitters)
		{
			shape.color = Color.GOLDENROD

			val emitterx = emitter.position.x * tileSize + offsetx
			val emittery = emitter.position.y * tileSize + offsety

			temp.set(emitter.currentOffset)
			temp.scl(emitter.size)
			temp.rotate(emitter.rotation)

			val ex = emitterx + temp.x * tileSize
			val ey = emittery + temp.y * tileSize

			val w = emitter.width * tileSize * emitter.size.x * emitter.currentSize
			val h = emitter.height * tileSize * emitter.size.y * emitter.currentSize

			val w2 = w * 0.5f
			val h2 = h * 0.5f

			if (!drawEmitter)
			{

			}
			else if (emitter.shape == Emitter.EmissionShape.BOX)
			{
				shape.rect(ex-w2, ey-h2, w2, h2, w, h, 1f, 1f, emitter.emitterRotation + rotation)
			}
			else if (emitter.shape == Emitter.EmissionShape.CIRCLE)
			{
				shape.ellipse(ex-w2, ey-h2, w, h, emitter.emitterRotation + rotation)
			}
			else if (emitter.shape == Emitter.EmissionShape.CONE)
			{
				val angleMin = -emitter.angle*0.5f
				val angleMax = emitter.angle*.5f

				val coreLeft = temp
				val coreRight = temp4
				val min = temp2
				val max = temp3
				val mid = temp5

				coreLeft.set(w2, 0f)
				coreLeft.rotate(emitter.emitterRotation)
				coreLeft.rotate(emitter.rotation)
				coreLeft.add(ex, ey)

				coreRight.set(-w2, 0f)
				coreRight.rotate(emitter.emitterRotation)
				coreRight.rotate(emitter.rotation)
				coreRight.add(ex, ey)

				min.set(0f, h)
				min.rotate(angleMin)
				min.x += w2
				min.rotate(emitter.emitterRotation)
				min.rotate(emitter.rotation)
				min.add(ex, ey)

				max.set(0f, h)
				max.rotate(angleMax)
				max.x -= w2
				max.rotate(emitter.emitterRotation)
				max.rotate(emitter.rotation)
				max.add(ex, ey)

				mid.set(0f, h)
				mid.rotate(emitter.emitterRotation)
				mid.rotate(emitter.rotation)
				mid.add(ex, ey)

				shape.line(coreLeft, coreRight)
				shape.line(coreLeft, min)
				shape.line(coreRight, max)
				shape.line(min, mid)
				shape.line(max, mid)
			}
			else
			{
				throw Exception("Unhandled emitter type '${emitter.shape}'!")
			}

			if (drawParticles)
			{
				shape.color = Color.PINK

				for (particle in emitter.particles)
				{
					var px = 0f
					var py = 0f

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
					{
						temp.set(emitter.currentOffset)
						temp.scl(emitter.size)
						temp.rotate(emitter.rotation)

						px += (emitter.position.x + temp.x)
						py += (emitter.position.y + temp.y)
					}

					for (pdata in particle.particles)
					{
						val keyframe1 = pdata.keyframe1
						val keyframe2 = pdata.keyframe2
						val alpha = pdata.keyframeAlpha

						val size = keyframe1.size[pdata.sizeStream].lerp(keyframe2.size[pdata.sizeStream], alpha, pdata.ranVal)
						var sizex = size
						var sizey = size

						if (particle.allowResize)
						{
							sizex *= emitter.size.x
							sizey *= emitter.size.y
						}

						sizex *= tileSize
						sizey *= tileSize

						val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

						temp.set(pdata.position)

						if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) temp.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

						val drawx = (temp.x + px) * tileSize + offsetx
						val drawy = (temp.y + py) * tileSize + offsety

						shape.rect(drawx - sizex / 2f, drawy - sizey / 2f, sizex / 2f, sizey / 2f, sizex, sizey, 1f, 1f, rotation)
					}
				}
			}

			if (drawEffectors)
			{
				shape.color = Color.GREEN

				for (i in 0 until emitter.effectors.size)
				{
					val effector = emitter.effectors[i]

					temp.set(effector.offset)
					temp.scl(emitter.size)
					temp.rotate(emitter.rotation)

					val ex = emitterx + temp.x * tileSize
					val ey = emittery + temp.y * tileSize
					shape.ellipse(ex - tileSize/2f, ey - tileSize/2f, tileSize, tileSize, emitter.emitterRotation + rotation)
				}
			}
		}

		Pools.free(temp)
		Pools.free(temp2)
		Pools.free(temp3)
		Pools.free(temp4)
		Pools.free(temp5)
	}

	override fun copy(): ParticleEffect
	{
		val effect = description.getParticleEffect()
		return effect
	}

	fun store(kryo: Kryo, output: Output)
	{
		output.writeString(loadPath)

		output.writeFloat(warmupTime)
		output.writeBoolean(loop)
		output.writeInt(emitters.size)

		for (emitter in emitters)
		{
			emitter.store(kryo, output)
		}

		output.writeBoolean(particleLight != null)
		if (particleLight != null)
		{
			particleLight!!.store(kryo, output)
		}
	}

	fun restore(kryo: Kryo, input: Input)
	{
		loadPath = input.readString()
		warmupTime = input.readFloat()
		loop = input.readBoolean()

		val numEmitters = input.readInt()
		for (i in 0 until numEmitters)
		{
			val emitter = Emitter(this)
			emitter.restore(kryo, input)
			emitters.add(emitter)
		}

		val hasLight = input.readBoolean()
		if (hasLight)
		{
			particleLight = ParticleLight()
			particleLight!!.restore(kryo, input)
		}
	}

	companion object
	{
		val kryo: Kryo by lazy { initKryo() }
		fun initKryo(): Kryo
		{
			val kryo = Kryo()
			kryo.isRegistrationRequired = false

			kryo.registerGdxSerialisers()
			kryo.registerLyeeedarSerialisers()

			return kryo
		}
		val storedMap = ObjectMap<String, ByteArray>()

		fun load(xml: XmlData, description: ParticleEffectDescription): ParticleEffect
		{
			val effect = ParticleEffect(description)

			effect.warmupTime = xml.getFloat("Warmup", 0f)
			effect.loop = xml.getBoolean("Loop", true)

			val emittersEl = xml.getChildByName("Emitters")!!
			for (i in 0 until emittersEl.childCount)
			{
				val el = emittersEl.getChild(i)
				val emitter = Emitter.load(el, effect) ?: continue
				effect.emitters.add(emitter)
			}

			val lightEl = xml.getChildByName("Light")
			if (lightEl != null)
			{
				effect.particleLight = ParticleLight.load(lightEl)
			}

			return effect
		}

		fun load(path: String, description: ParticleEffectDescription): ParticleEffect
		{
			if (storedMap.containsKey(path))
			{
				val bytes = storedMap[path]
				val input = Input(bytes, 0, bytes.size)

				val effect = ParticleEffect(description)
				effect.restore(kryo, input)

				return effect
			}
			else
			{
				val xml = getXml("Particles/$path")
				val effect = load(xml, description)
				effect.loadPath = path

				val output = Output(1024, -1)
				effect.store(kryo, output)
				storedMap[path] = output.buffer

				return load(path, description)
			}
		}
	}
}

class ParticleLightKeyframe(
	val time: Float = 0f,
	val colour: Colour = Colour(),
	val brightness: Float = 0f,
	val range: Float = 0f)
{

}

class ParticleLight
{
	val light = Light()

	var keyframeIndex: Int = 0
	lateinit var keyframe1: ParticleLightKeyframe
	lateinit var keyframe2: ParticleLightKeyframe
	var keyframeAlpha: Float = 0f
	var keyframes: kotlin.Array<ParticleLightKeyframe> = emptyArray()

	var hasShadows = false

	fun update(time: Float, size: Float)
	{
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

		val col = keyframe1.colour.lerp(keyframe2.colour, keyframeAlpha)
		light.colour.set(col)

		val brightness = keyframe1.brightness.lerp(keyframe2.brightness, keyframeAlpha)
		light.colour.mul(brightness, brightness, brightness, 1f)

		val range = (keyframe1.range.lerp(keyframe2.range, keyframeAlpha))
		light.range = range * size

		light.hasShadows = hasShadows
	}

	fun store(kryo: Kryo, output: Output)
	{
		output.writeInt(keyframes.size)
		for (keyframe in keyframes)
		{
			output.writeFloat(keyframe.time)

			output.writeFloat(keyframe.colour.r)
			output.writeFloat(keyframe.colour.g)
			output.writeFloat(keyframe.colour.b)
			output.writeFloat(keyframe.colour.a)

			output.writeFloat(keyframe.brightness)

			output.writeFloat(keyframe.range)
		}

		output.writeBoolean(hasShadows)
	}

	fun restore(kryo: Kryo, input: Input)
	{
		val numKeyframes = input.readInt()
		keyframes = kotlin.Array<ParticleLightKeyframe>(numKeyframes) { i -> ParticleLightKeyframe() }

		for (i in 0 until numKeyframes)
		{
			val time = input.readFloat()

			val r = input.readFloat()
			val g = input.readFloat()
			val b = input.readFloat()
			val a = input.readFloat()

			val brightness = input.readFloat()

			val range = input.readFloat()

			keyframes[i] = ParticleLightKeyframe(time, Colour(r, g, b, a), brightness, range)
		}

		hasShadows = input.readBoolean()
	}

	companion object
	{
		fun load(xml: XmlData): ParticleLight
		{
			val light = ParticleLight()

			val colour = ColourTimeline()
			val colourEls = xml.getChildByName("Colour")!!
			colour.parse(colourEls, { AssetManager.loadColour(it) })

			val brightness = LerpTimeline()
			val brightnessEls = xml.getChildByName("Brightness")!!
			brightness.parse(brightnessEls, { it.toFloat() })

			val range = LerpTimeline()
			val rangeEls = xml.getChildByName("Range")!!
			range.parse(rangeEls, { it.toFloat() })

			light.hasShadows = xml.getBoolean("HasShadows", false)

			// Make map of times
			val times = ObjectSet<Float>()
			for (keyframe in colour.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in brightness.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in range.streams.flatMap { it }) { times.add(keyframe.first) }

			val keyframes = kotlin.Array<ParticleLightKeyframe>(times.size) { i -> ParticleLightKeyframe() }
			var keyframeI = 0
			for (time in times.sortedBy { it })
			{
				val keyframe = ParticleLightKeyframe(
					time,
					colour.valAt(0, time),
					brightness.valAt(0, time),
					range.valAt(0, time))
				keyframes[keyframeI++] = keyframe
			}
			light.keyframes = keyframes
			light.keyframe1 = keyframes[0]
			light.keyframe2 = keyframes[0]

			return light
		}
	}
}