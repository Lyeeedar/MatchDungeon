package com.lyeeedar.Renderables.Particle

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.cos
import com.badlogic.gdx.math.MathUtils.sin
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Pools
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Statics.Companion.collisionGrid
import ktx.collections.toGdxArray
import ktx.math.div

/**
 * Created by Philip on 14-Aug-16.
 */

class ParticleKeyframe(
		val time: Float,
		val texture: kotlin.Array<Float>,
		val colour: kotlin.Array<Colour>,
		val alpha: kotlin.Array<Float>,
		val alphaRef: kotlin.Array<Float>,
		val rotationSpeed: kotlin.Array<Range>,
		val size: kotlin.Array<Range>)
{
	constructor() : this(0f, emptyArray<Float>(), emptyArray<Colour>(), emptyArray<Float>(), emptyArray<Float>(), emptyArray<Range>(), emptyArray<Range>())
}

class Particle(val emitter: Emitter)
{
	enum class CollisionAction
	{
		NONE,
		SLIDE,
		BOUNCE,
		DIE
	}

	enum class SizeOrigin
	{
		CENTER,
		BOTTOM,
		TOP,
		LEFT,
		RIGHT
	}

	enum class SizeMode
	{
		BOTHAXIS,
		XONLY,
		YONLY
	}

	private val moveVec = Vector2()
	private val oldPos = Vector2()
	private val normal = Vector2()
	private val reflection = Vector2()
	private val temp = Vector2()
	private val temp2 = Vector2()
	private val collisionList = Array<Direction>(false, 16)

	val particles = Array<ParticleData>(false, 16)

	var allowResize: Boolean = true
	var maintainAspectRatio: Boolean = false
	lateinit var sizeOrigin: SizeOrigin
	lateinit var sizeMode: SizeMode
	lateinit var lifetime: Range
	lateinit var blend: BlendMode
	var drag = 0f
	var brownian = 0f
	var velocityAligned = false
	lateinit var collision: CollisionAction
	var blendKeyframes = false
	var keyframes: kotlin.Array<ParticleKeyframe> = emptyArray()
	lateinit var textures: kotlin.Array<kotlin.Array<Pair<String, TextureRegion>>>

	fun particleCount() = particles.size
	fun complete() = particles.size == 0

	fun simulate(delta: Float, gravity: Float)
	{
		var particleI = 0

		val itr = particles.iterator()
		while (itr.hasNext())
		{
			particleI++

			val particle = itr.next()
			particle.life += delta
			val life = particle.life
			if (life > lifetime.v2)
			{
				itr.remove()
				particle.free()
			}
			else
			{
				var keyframeIndex = particle.keyframeIndex
				while (keyframeIndex < keyframes.size-1)
				{
					if (life >= keyframes[keyframeIndex+1].time)
					{
						keyframeIndex++
					}
					else
					{
						break
					}
				}

				val keyframe1 = keyframes[keyframeIndex]
				val keyframe2: ParticleKeyframe
				val alpha: Float

				if (keyframeIndex < keyframes.size-1)
				{
					keyframe2 = keyframes[keyframeIndex+1]
					alpha = (life - keyframe1.time) / (keyframe2.time - keyframe1.time)
				}
				else
				{
					keyframe2 = keyframes[keyframeIndex]
					alpha = 0f
				}

				particle.keyframeIndex = keyframeIndex
				particle.keyframe1 = keyframe1
				particle.keyframe2 = keyframe2
				particle.keyframeAlpha = alpha

				val velocity = particle.velocity

				if (velocityAligned)
				{
					particle.rotation = vectorToAngle(velocity.x, velocity.y)
				}
				else
				{
					var rotation = keyframe1.rotationSpeed[particle.rotStream].lerp(keyframe2.rotationSpeed[particle.rotStream], alpha, particle.ranVal)

					if (emitter.particleEffect.flipX && emitter.particleEffect.flipY)
					{

					}
					else if (emitter.particleEffect.flipX || emitter.particleEffect.flipY)
					{
						rotation *= -1f
					}

					particle.rotation += rotation * delta
				}

				temp.set(velocity).scl(drag * delta)
				velocity.sub(temp)

				velocity.y += gravity * delta

				if (brownian > 0f)
				{
					val direction = temp2.set(velocity)
					val length = velocity.len()

					if (length != 0f) direction.div(length)

					val brownianSampleVal = (particleI + largePrime).rem(numBrownianVectors)

					val impulseVector = temp.set(brownianVectors[brownianSampleVal])

					direction.lerp(impulseVector, brownian * delta)
					direction.nor()

					velocity.set(direction).scl(length)
				}

				moveVec.set(velocity).scl(delta)

				oldPos.set(particle.position)

				particle.position.add(moveVec)

				if (collision != CollisionAction.NONE && collisionGrid != null)
				{
					val aabb = getBoundingBox(particle)

					if (checkColliding(aabb, collisionGrid!!))
					{
						if (collision == CollisionAction.DIE)
						{
							itr.remove()
							particle.free()
						}
						else if (collision == CollisionAction.BOUNCE || collision == CollisionAction.SLIDE)
						{
							// calculate average collision normal
							normal.x = collisionList.sumBy { it.x }.toFloat()
							normal.y = collisionList.sumBy { it.y }.toFloat()
							normal.nor()

							// reflect vector around normal
							val reflected = reflection.set(moveVec).sub(temp.set(normal).scl(2 * moveVec.dot(normal)))

							// handle based on collision action
							if (collision == CollisionAction.BOUNCE)
							{
								particle.position.set(oldPos)
								velocity.set(reflected)
							}
							else
							{
								val yaabb = getBoundingBox(particle, temp.set(particle.position.x, oldPos.y))
								val xaabb = getBoundingBox(particle, temp.set(oldPos.x, particle.position.y))

								// negate y
								if (!checkColliding(yaabb, collisionGrid!!))
								{
									particle.position.y = oldPos.y
								}
								// negate x
								else if (!checkColliding(xaabb, collisionGrid!!))
								{
									particle.position.x = oldPos.x
								}
								// negate both
								else
								{
									particle.position.set(oldPos)
								}
							}
						}
						else
						{
							throw NotImplementedError("Forgot to add code to deal with collision action")
						}
					}

					Pools.free(aabb)
				}
			}
		}
	}

	fun callCollisionFunc(func: (x: Int, y: Int) -> Unit)
	{
		for (particle in particles)
		{
			val aabb = getBoundingBox(particle)

			for (x in aabb.x.toInt()..(aabb.x+aabb.width).toInt())
			{
				for (y in aabb.y.toInt()..(aabb.y + aabb.height).toInt())
				{
					func(x, y)
				}
			}

			Pools.free(aabb)
		}
	}

	fun checkColliding(aabb: Rectangle, collisionGrid: Array2D<Boolean>): Boolean
	{
		collisionList.clear()

		for (x in aabb.x.toInt()..(aabb.x+aabb.width).toInt())
		{
			for (y in aabb.y.toInt()..(aabb.y+aabb.height).toInt())
			{
				if (collisionGrid.tryGet(x, y, false)!!)
				{
					// calculate collision normal

					val wy = (aabb.width + 1f) * ((aabb.y+aabb.height*0.5f) - (y+0.5f))
					val hx = (aabb.height + 1f) * ((aabb.x+aabb.width*0.5f) - (x+0.5f))

					var dir: Direction

					if (wy > hx)
					{
						if (wy > -hx)
						{
							/* top */
							dir = Direction.SOUTH
						}
						else
						{
							/* left */
							dir = Direction.WEST
						}
					}
					else
					{
						if (wy > -hx)
						{
							/* right */
							dir = Direction.EAST
						}
						else
						{
							/* bottom */
							dir = Direction.NORTH
						}
					}

					collisionList.add(dir)
				}
			}
		}

		return collisionList.size > 0
	}

	fun getBoundingBox(particle: ParticleData, overridePos: Vector2? = null): Rectangle
	{
		val keyframe1 = particle.keyframe1
		val keyframe2 = particle.keyframe2
		val alpha = particle.keyframeAlpha

		val scale = keyframe1.size[particle.sizeStream].lerp(keyframe2.size[particle.sizeStream], alpha, particle.ranVal)
		val scaleX = if (sizeMode == SizeMode.YONLY) 1f else scale
		val scaleY = if (sizeMode == SizeMode.XONLY) 1f else scale

		val sx = scaleX * emitter.size.x
		val sy = scaleY * emitter.size.y

		val x = overridePos?.x ?: particle.position.x
		val y = overridePos?.y ?: particle.position.y

		var actualx = x
		var actualy = y

		if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
		{
			temp.set(emitter.currentOffset)
			temp.scl(emitter.size)

			if (emitter.particleEffect.flipX)
			{
				temp.x *= -1
			}
			if (emitter.particleEffect.flipY)
			{
				temp.y *= -1
			}

			temp.rotate(emitter.rotation)

			val ex = temp.x + emitter.position.x
			val ey = temp.y + emitter.position.y

			temp.set(x, y)
			temp.rotate(emitter.rotation + emitter.emitterRotation)

			actualx = ex + temp.x
			actualy = ey + temp.y
		}

		var drawX = actualx
		var drawY = actualy

		when(sizeOrigin)
		{
			SizeOrigin.CENTER -> {
				drawX -= sx*0.5f
				drawY -= sy*0.5f
			}
			SizeOrigin.BOTTOM -> {
				drawX -= sx*0.5f
			}
			SizeOrigin.TOP -> {
				drawX -= sx*0.5f
				drawY -= sy
			}
			SizeOrigin.LEFT -> {
				drawY -= sy*0.5f
			}
			SizeOrigin.RIGHT -> {
				drawX -= sx
				drawY -= sy*0.5f
			}
		}

		return Pools.obtain(Rectangle::class.java).set(drawX, drawY, sx, sy)
	}

	fun spawn(position: Vector2, velocity: Vector2, rotation: Float)
	{
		val particle = ParticleData.obtain().set(
				position, velocity,
				rotation, (lifetime.v2 - lifetime.v1) * Random.random(),
				0, keyframes[0], keyframes[0], 0f,
				Random.random(keyframes[0].texture.size-1),
				Random.random(keyframes[0].colour.size-1),
				Random.random(keyframes[0].alpha.size-1),
				Random.random(keyframes[0].alphaRef.size-1),
				Random.random(keyframes[0].rotationSpeed.size-1),
				Random.random(keyframes[0].size.size-1),
				Random.random())

		particles.add(particle)
	}

	fun store(kryo: Kryo, output: Output)
	{
		output.writeFloat(lifetime.v1)
		output.writeFloat(lifetime.v2)
		output.writeInt(blend.ordinal)
		output.writeInt(collision.ordinal)
		output.writeFloat(drag)
		output.writeBoolean(velocityAligned)
		output.writeBoolean(allowResize)
		output.writeBoolean(maintainAspectRatio)
		output.writeInt(sizeOrigin.ordinal)
		output.writeInt(sizeMode.ordinal)
		output.writeFloat(brownian)
		output.writeBoolean(blendKeyframes)

		output.writeInt(textures.size)
		for (stream in textures)
		{
			output.writeInt(stream.size)
			for (tex in stream)
			{
				output.writeString(tex.first)
			}
		}

		output.writeInt(keyframes.size)
		output.writeInt(keyframes[0].texture.size)
		output.writeInt(keyframes[0].colour.size)
		output.writeInt(keyframes[0].alpha.size)
		output.writeInt(keyframes[0].alphaRef.size)
		output.writeInt(keyframes[0].rotationSpeed.size)
		output.writeInt(keyframes[0].size.size)

		for (keyframe in keyframes)
		{
			output.writeFloat(keyframe.time)

			for (tex in keyframe.texture)
			{
				output.writeFloat(tex)
			}

			for (colour in keyframe.colour)
			{
				kryo.writeObject(output, colour)
			}

			for (alpha in keyframe.alpha)
			{
				output.writeFloat(alpha)
			}

			for (alphaRef in keyframe.alphaRef)
			{
				output.writeFloat(alphaRef)
			}

			for (rotationSpeed in keyframe.rotationSpeed)
			{
				output.writeFloat(rotationSpeed.v1)
				output.writeFloat(rotationSpeed.v2)
			}

			for (size in keyframe.size)
			{
				output.writeFloat(size.v1)
				output.writeFloat(size.v2)
			}
		}
	}

	fun restore(kryo: Kryo, input: Input)
	{
		lifetime = Range(input.readFloat(), input.readFloat())
		blend = BlendMode.values()[input.readInt()]
		collision = CollisionAction.values()[input.readInt()]
		drag = input.readFloat()
		velocityAligned = input.readBoolean()
		allowResize = input.readBoolean()
		maintainAspectRatio = input.readBoolean()
		sizeOrigin = SizeOrigin.values()[input.readInt()]
		sizeMode = SizeMode.values()[input.readInt()]
		brownian = input.readFloat()
		blendKeyframes = input.readBoolean()

		val numTextureStreams = input.readInt()
		textures = kotlin.Array(numTextureStreams) { ii ->
			val numTextures = input.readInt()
			kotlin.Array<Pair<String, TextureRegion>>(numTextures)
			{ i ->
				val oldTexName = input.readString()
				val desc = emitter.particleEffect.description.getTexture(oldTexName)
				if (desc != null)
				{
					if (desc.blendMode != null)
					{
						blend = desc.blendMode
					}

					Pair(desc.newName, AssetManager.loadTextureRegion(desc.newName)!!)
				}
				else
				{
					Pair(oldTexName, AssetManager.loadTextureRegion(oldTexName)!!)
				}
			}
		}

		val numKeyframes = input.readInt()
		val numTextureBlendStreams = input.readInt()
		val numColourStreams = input.readInt()
		val numAlphaStreams = input.readInt()
		val numAlphaRefStreams = input.readInt()
		val numRotationSpeedStreams = input.readInt()
		val numSizeStreams = input.readInt()

		keyframes = kotlin.Array<ParticleKeyframe>(numKeyframes) { i -> ParticleKeyframe() }
		for (i in 0 until numKeyframes)
		{
			keyframes[i] = ParticleKeyframe(
					input.readFloat(),
					kotlin.Array<Float>(numTextureBlendStreams) { i -> input.readFloat() },
					kotlin.Array<Colour>(numColourStreams) { i -> kryo.readObject(input, Colour::class.java) },
					kotlin.Array<Float>(numAlphaStreams) { i -> input.readFloat() },
					kotlin.Array<Float>(numAlphaRefStreams) { i -> input.readFloat() },
					kotlin.Array<Range>(numRotationSpeedStreams) { i -> Range(input.readFloat(), input.readFloat()) },
					kotlin.Array<Range>(numSizeStreams) { i -> Range(input.readFloat(), input.readFloat()) }
										   )
		}
	}

	companion object
	{
		val numBrownianVectors = 30
		val brownianVectors = kotlin.Array<Vector2>(numBrownianVectors) { i -> Vector2() }
		val largePrime = 829
		fun generateBrownianVectors()
		{
			for (i in 0 until numBrownianVectors)
			{
				val azimuth = Random.random() * MathUtils.PI2
				brownianVectors[i].set(cos(azimuth), sin(azimuth))
			}
		}

		fun load(xml: XmlData, emitter: Emitter): Particle
		{
			val particle = Particle(emitter)

			particle.lifetime = Range(xml.get("Lifetime"))
			particle.blend = BlendMode.valueOf(xml.get("BlendMode", "Additive")!!.toUpperCase())
			particle.collision = CollisionAction.valueOf(xml.get("Collision", "None")!!.toUpperCase())
			particle.drag = xml.getFloat("Drag", 0f)
			particle.velocityAligned = xml.getBoolean("VelocityAligned", false)
			particle.allowResize = xml.getBoolean("AllowResize", true)
			particle.maintainAspectRatio = xml.getBoolean("MaintainAspectRatio", false)
			particle.sizeOrigin = SizeOrigin.valueOf(xml.get("SizeOrigin", "Center")!!.toUpperCase())
			particle.sizeMode = SizeMode.valueOf(xml.get("SizeMode", "BothAxis")!!.toUpperCase())
			particle.brownian = xml.getFloat("Brownian", 0f)

			particle.blendKeyframes = xml.getBoolean("BlendKeyframes", false)
			val loopKeyframes = xml.getBoolean("LoopKeyframes", false)
			val loopDuration = xml.getFloat("LoopDuration", 0f)

			// Load timelines
			val texBlend = LerpTimeline()
			val texture = StepTimeline<Pair<String, TextureRegion>>()
			val colour = ColourTimeline()
			val alpha = LerpTimeline()
			val alphaRef = LerpTimeline()
			val rotationSpeed = RangeLerpTimeline()
			val size = RangeLerpTimeline()

			val textureEls = xml.getChildByName("TextureKeyframes")
			if (textureEls != null)
			{
				texture.parse(textureEls, { Pair(it, AssetManager.loadTextureRegion(it) ?: throw RuntimeException("Failed to find texture" + " $it!")) }, particle.lifetime.v2)
			}
			else
			{
				texture[0, 0f] = Pair("white", AssetManager.loadTextureRegion("white")!!)
			}

			if (loopKeyframes && loopDuration > 0f)
			{
				for (stream in texture.streams)
				{
					val originalKeyframes = stream.toGdxArray()
					val loopLength = loopDuration / particle.lifetime.v2
					val keyframeDuration = loopLength / originalKeyframes.size

					stream.clear()
					var time = 0f
					var index = 0
					while (time < 1f)
					{
						val keyframe = Pair(time, originalKeyframes[index].second)
						stream.add(keyframe)

						time += keyframeDuration

						index += 1
						if (index == originalKeyframes.size)
						{
							index = 0
						}
					}
				}
			}

			for (s in 0 until texture.streams.size)
			{
				val stream = Array<Pair<Float, Float>>()
				texBlend.streams.add(stream)
				for (k in 0 until texture.streams[s].size)
				{
					val key = texture.streams[s][k]
					stream.add(Pair(key.first, k.toFloat()))
				}
			}
			particle.textures = kotlin.Array(texture.streams.size) { i -> kotlin.Array(texture.streams[i].size) { ii -> texture.streams[i][ii].second} }

			val colourEls = xml.getChildByName("ColourKeyframes")
			if (colourEls != null)
			{
				colour.parse(colourEls, { AssetManager.loadColour(it) }, particle.lifetime.v2)
			}
			else
			{
				colour[0, 0f] = Colour(1f, 1f, 1f, 1f)
			}

			val alphaEls = xml.getChildByName("AlphaKeyframes")
			if (alphaEls != null)
			{
				alpha.parse(alphaEls, { it.toFloat() }, particle.lifetime.v2)
			}
			else
			{
				alpha[0, 0f] = 1f
			}

			val alphaRefEls = xml.getChildByName("AlphaRefKeyframes")
			if (alphaRefEls != null)
			{
				alphaRef.parse(alphaRefEls, { it.toFloat() }, particle.lifetime.v2)
			}
			else
			{
				alphaRef[0, 0f] = 0f
			}

			val rotationSpeedEls = xml.getChildByName("RotationSpeedKeyframes")
			if (rotationSpeedEls != null)
			{
				rotationSpeed.parse(rotationSpeedEls, { Range(it) }, particle.lifetime.v2)
			}
			else
			{
				rotationSpeed[0, 0f] = Range(0f, 0f)
			}

			val sizeEls = xml.getChildByName("SizeKeyframes")
			if (sizeEls != null)
			{
				size.parse(sizeEls, { Range(it) }, particle.lifetime.v2)
			}
			else
			{
				size[0, 0f] = Range(1f, 1f)
			}

			// Make map of times
			val times = ObjectSet<Float>()
			for (keyframe in texture.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in colour.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in alpha.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in alphaRef.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in rotationSpeed.streams.flatMap { it }) { times.add(keyframe.first) }
			for (keyframe in size.streams.flatMap { it }) { times.add(keyframe.first) }

			// Sample timelines at each time to make keyframes
			val keyframes = kotlin.Array<ParticleKeyframe>(times.size) { i -> ParticleKeyframe() }
			var keyframeI = 0
			for (time in times.sortedBy { it })
			{
				val texBlendArr = kotlin.Array<Float>(texBlend.streams.size) { i -> texBlend.valAt(i, time) }
				val colourArr = kotlin.Array<Colour>(colour.streams.size) { i -> colour.valAt(i, time).copy() }
				val alphaArr = kotlin.Array<Float>(alpha.streams.size) { i -> alpha.valAt(i, time) }
				val alphaRefArr = kotlin.Array<Float>(alphaRef.streams.size) { i -> alphaRef.valAt(i, time) }
				val rotationSpeedArr = kotlin.Array<Range>(rotationSpeed.streams.size) { i -> rotationSpeed.valAt(i, time).copy() }
				val sizeArr = kotlin.Array<Range>(size.streams.size) { i -> size.valAt(i, time).copy() }

				val keyframe = ParticleKeyframe(
						time,
						texBlendArr,
						colourArr,
						alphaArr,
						alphaRefArr,
						rotationSpeedArr,
						sizeArr)
				keyframes[keyframeI++] = keyframe
			}
			particle.keyframes = keyframes

			return particle
		}
	}
}

class ParticleData(val position: Vector2, val velocity: Vector2,
						var rotation: Float, var life: Float,
						var keyframeIndex: Int, var keyframe1: ParticleKeyframe, var keyframe2: ParticleKeyframe, var keyframeAlpha: Float,
						var texStream: Int, var colStream: Int, var alphaStream: Int, var alphaRefStream: Int, var rotStream: Int, var sizeStream: Int,
						var ranVal: Float,
						val parentBlock: ParticleBlock, val parentBlockIndex: Int)
{
	constructor(parentBlock: ParticleBlock, parentBlockIndex: Int): this(Vector2(), Vector2(0f, 1f), 0f, 0f, 0, ParticleKeyframe(), ParticleKeyframe(), 0f, 0, 0, 0, 0, 0, 0, 0f, parentBlock, parentBlockIndex)

	fun set(position: Vector2, velocity: Vector2, rotation: Float, life: Float, keyframeIndex: Int, keyframe1: ParticleKeyframe, keyframe2: ParticleKeyframe, keyframeAlpha: Float, texStream: Int, colStream: Int, alphaStream: Int, alphaRefStream: Int, rotStream: Int, sizeStream: Int, ranVal: Float): ParticleData
	{
		this.position.set(position)
		this.velocity.set(velocity)
		this.life = life
		this.rotation = rotation
		this.keyframeIndex = keyframeIndex
		this.keyframe1 = keyframe1
		this.keyframe2 = keyframe2
		this.keyframeAlpha = keyframeAlpha
		this.texStream = texStream
		this.colStream = colStream
		this.alphaStream = alphaStream
		this.alphaRefStream = alphaRefStream
		this.rotStream = rotStream
		this.sizeStream = sizeStream
		this.ranVal = ranVal
		return this
	}

	var obtained: Boolean = false
	companion object
	{
		var currentBlock: ParticleBlock = ParticleBlock()

		@JvmStatic fun obtain(): ParticleData
		{
			val particle = currentBlock.obtain()

			if (currentBlock.index == ParticleBlock.blockSize)
			{
				currentBlock = ParticleBlock.pool.obtain()
			}

			return particle
		}
	}

	fun free()
	{
		if (obtained)
		{
			parentBlock.free(this)
			obtained = false
		}
	}
}

class ParticleBlock
{
	var count: Int = 0
	var index: Int = 0
	val particles = Array(blockSize) { ParticleData(this, it) }

	fun obtain(): ParticleData
	{
		val particle = particles[index]

		if (particle.obtained) throw RuntimeException()

		particle.obtained = true
		particle.life = 0f

		index++
		count++

		return particle
	}

	fun free(data: ParticleData)
	{
		count--

		if (count == 0 && index == blockSize)
		{
			pool.free(this)
			index = 0
		}
	}

	companion object
	{
		public const val blockSize = 64

		public val pool: Pool<ParticleBlock> = object : Pool<ParticleBlock>() {
			override fun newObject(): ParticleBlock
			{
				return ParticleBlock()
			}
		}
	}
}
