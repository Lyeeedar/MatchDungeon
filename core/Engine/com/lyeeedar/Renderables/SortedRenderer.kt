package com.lyeeedar.Renderables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BigMesh
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.*
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray
import squidpony.squidmath.LightRNG


/**
 * Created by Philip on 04-Jul-16.
 */

// ----------------------------------------------------------------------
class SortedRenderer(var tileSize: Float, val width: Float, val height: Float, val layers: Int, val alwaysOnscreen: Boolean) : Disposable
{
	private var batchID: Int = random.nextInt()

	private val tempVec = Vector2()
	private val tempVec2 = Vector2()
	private val tempVec3 = Vector3()
	private val tempCol = Colour()
	private val bitflag = EnumBitflag<Direction>()

	private val startingArraySize = 128
	private var spriteArray = Array<RenderSprite?>(startingArraySize) { null }
	private var queuedSprites = 0

	private val tilingMap: IntMap<ObjectSet<Long>> = IntMap()

	private val setPool: Pool<ObjectSet<Long>> = object : Pool<ObjectSet<Long>>() {
		override fun newObject(): ObjectSet<Long>
		{
			return ObjectSet()
		}
	}

	private val basicLights = com.badlogic.gdx.utils.Array<Light>()
	private val shadowLights = com.badlogic.gdx.utils.Array<Light>()

	private var screenShakeRadius: Float = 0f
	private var screenShakeAccumulator: Float = 0f
	private var screenShakeSpeed: Float = 0f
	private var screenShakeAngle: Float = 0f
	private var screenShakeLocked: Boolean = false

	private val MAX_INDEX = 5
	private val MAX_LAYER = layers
	private val NUM_BLENDS = BlendMode.values().size

	private val BLEND_BLOCK_SIZE = 1
	private val INDEX_BLOCK_SIZE = BLEND_BLOCK_SIZE * NUM_BLENDS
	private val LAYER_BLOCK_SIZE = INDEX_BLOCK_SIZE * MAX_INDEX
	private val X_BLOCK_SIZE = LAYER_BLOCK_SIZE * MAX_LAYER
	private val Y_BLOCK_SIZE = X_BLOCK_SIZE * width.toInt()

	private var delta: Float = 0f

	private var inBegin = false
	private var inStaticBegin = false
	private var offsetx: Float = 0f
	private var offsety: Float = 0f

	private val ambientLight = Colour()

	// ----------------------------------------------------------------------
	private class VertexBuffer
	{
		var offset = -1
		var count = -1
		lateinit var texture: Texture
		var blendSrc: Int = -1
		var blendDst: Int = -1

		init
		{

		}

		fun reset(blendSrc: Int, blendDst: Int, texture: Texture): VertexBuffer
		{
			this.blendSrc = blendSrc
			this.blendDst = blendDst
			this.texture = texture
			count = 0
			offset = 0

			return this
		}
	}

	// ----------------------------------------------------------------------
	private val mesh: BigMesh
	private val staticMesh: BigMesh
	private var currentBuffer: VertexBuffer? = null
	private val vertices: FloatArray
	private var currentVertexCount = 0
	private val staticBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val queuedBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private lateinit var shader: ShaderProgram
	private var shaderLightNum: Int = 0
	private var shaderShadowLightNum: Int = 0
	private var shaderRegionsPerLight: IntArray = IntArray(0)
	private var lightPosRange: FloatArray
	private var lightColourBrightness: FloatArray
	private var lightShadowPosRange: FloatArray
	private var lightShadowColourBrightness: FloatArray
	private var lightShadowRegions: FloatArray
	private val combinedMatrix: Matrix4 = Matrix4()

	private val executor = LightweightThreadpool(3)

	// ----------------------------------------------------------------------
	private val bufferPool: Pool<VertexBuffer> = object : Pool<VertexBuffer>() {
		override fun newObject(): VertexBuffer
		{
			return VertexBuffer()
		}
	}

	// ----------------------------------------------------------------------
	init
	{
		mesh = BigMesh(false, maxSprites * 4, maxSprites * 6,
					   VertexAttribute(VertexAttributes.Usage.Position, 4, ShaderProgram.POSITION_ATTRIBUTE),
					   VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 4, ShaderProgram.TEXCOORD_ATTRIBUTE),
					   VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
					   VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_additionalData") // blendalpha, islit, alpharef, brightness
					  )

		staticMesh = BigMesh(true, maxSprites * 4, maxSprites * 6,
							 VertexAttribute(VertexAttributes.Usage.Position, 4, ShaderProgram.POSITION_ATTRIBUTE),
							 VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 4, ShaderProgram.TEXCOORD_ATTRIBUTE),
							 VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
							 VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_additionalData") // blendalpha, islit, alpharef, brightness
					  )

		val len = maxSprites * 6
		val indices = IntArray(len)
		var j = 0
		var i = 0
		while (i < len)
		{
			indices[i] = j
			indices[i + 1] = j + 1
			indices[i + 2] = j + 2
			indices[i + 3] = j + 2
			indices[i + 4] = j + 3
			indices[i + 5] = j
			i += 6
			j += 4
		}
		mesh.setIndices(indices)
		staticMesh.setIndices(indices)

		vertices = FloatArray(maxVertices)

		shaderLightNum = 10
		lightPosRange = FloatArray(shaderLightNum * 3)
		lightColourBrightness = FloatArray(shaderLightNum * 4)
		lightShadowPosRange = FloatArray(0)
		lightShadowColourBrightness = FloatArray(0)
		lightShadowRegions = FloatArray(0)
		shader = createShader(shaderLightNum, shaderShadowLightNum, IntArray(0))
	}

	override fun dispose()
	{
		mesh.dispose()
		staticMesh.dispose()
	}

	// ----------------------------------------------------------------------
	fun begin(deltaTime: Float, offsetx: Float, offsety: Float, ambientLight: Colour)
	{
		if (inBegin) throw Exception("Begin called again before flush!")

		this.ambientLight.set(ambientLight)
		delta = deltaTime
		this.offsetx = offsetx
		this.offsety = offsety
		inBegin = true
	}

	// ----------------------------------------------------------------------
	fun beginStatic()
	{
		if (inBegin) throw Exception("BeginStatic called within begin!")
		if (inStaticBegin) throw Exception("BeginStatic called BeginStatic!")

		for (buffer in staticBuffers)
		{
			bufferPool.free(buffer)
		}
		staticBuffers.clear()

		delta = 0f
		inStaticBegin = true
	}

	// ----------------------------------------------------------------------
	fun end(batch: Batch)
	{
		if (!inBegin) throw Exception("End called before begin!")

		flush(batch)

		inBegin = false
	}

	// ----------------------------------------------------------------------
	fun endStatic(batch: Batch)
	{
		if (!inStaticBegin) throw Exception("EndStatic called before beginstatic!")

		flush(batch)

		inStaticBegin = false
	}

	// ----------------------------------------------------------------------
	fun setScreenShake(amount: Float, speed: Float)
	{
		screenShakeRadius = amount
		screenShakeSpeed = speed
	}

	// ----------------------------------------------------------------------
	fun lockScreenShake()
	{
		screenShakeLocked = true
	}

	// ----------------------------------------------------------------------
	fun unlockScreenShake()
	{
		screenShakeLocked = false
	}

	// ----------------------------------------------------------------------
	private fun requestRender(blendSrc: Int, blendDst: Int, texture: Texture, drawFun: (vertices: FloatArray, offset: Int) -> Unit)
	{
		if (currentBuffer == null)
		{
			currentBuffer = bufferPool.obtain()
			currentBuffer!!.reset(blendSrc, blendDst, texture)
			currentBuffer!!.offset = currentVertexCount
		}

		var buffer = currentBuffer!!
		if (buffer.blendSrc != blendSrc || buffer.blendDst != blendDst || buffer.texture != texture)
		{
			queuedBuffers.add(currentBuffer)
			buffer = bufferPool.obtain()
			buffer.reset(blendSrc, blendDst, texture)
			buffer.offset = currentVertexCount

			currentBuffer = buffer
		}

		val offset = currentVertexCount
		buffer.count += verticesASprite
		currentVertexCount += verticesASprite

		if (currentVertexCount == maxVertices) throw Exception("Too many vertices queued!")

		executor.addJob {
			drawFun.invoke(vertices, offset)
		}
	}

	// ----------------------------------------------------------------------
	private fun storeStatic()
	{
		executor.awaitAllJobs()

		queuedBuffers.add(currentBuffer!!)
		currentBuffer = null

		staticBuffers.addAll(queuedBuffers)
		queuedBuffers.clear()

		staticMesh.setVertices(vertices, 0, currentVertexCount)
		currentVertexCount = 0
	}

	// ----------------------------------------------------------------------
	private fun waitOnRender()
	{
		if (currentBuffer == null && staticBuffers.size == 0 && queuedBuffers.size == 0) return

		var rebuildShader = false

		if (basicLights.size > shaderLightNum)
		{
			shaderLightNum = basicLights.size
			rebuildShader = true

			lightPosRange = FloatArray(shaderLightNum * 3)
			lightColourBrightness = FloatArray(shaderLightNum * 4)
		}

		val sortedShadowLights = shadowLights.filter { it.cache.anyClear() }.sortedBy { if (shadowMode == ShadowMode.TILE) it.cache.currentCastRegions.size else it.cache.opaqueRegions.size }.toGdxArray()

		if (sortedShadowLights.size != shaderShadowLightNum)
		{
			shaderShadowLightNum = sortedShadowLights.size
			rebuildShader = true

			val shadowPosRangeSize = if (shadowMode == ShadowMode.TILE) 4 else 3
			lightShadowPosRange = FloatArray(shaderShadowLightNum * shadowPosRangeSize)
			lightShadowColourBrightness = FloatArray(shaderShadowLightNum * 4)
		}

		val regionsPerLight = IntArray(shaderShadowLightNum)
		var regionsDifferent = false
		if (Statics.collisionGrid != null)
		{
			var i = 0
			for (light in sortedShadowLights)
			{
				val numCount: Int
				if (shadowMode == ShadowMode.TILE)
				{
					numCount = light.cache.currentCastRegions.size
				}
				else
				{
					numCount = light.cache.opaqueRegions.size
				}

				if (i < shaderRegionsPerLight.size)
				{
					val shaderNumCount = shaderRegionsPerLight[i]
					if (numCount > shaderNumCount || numCount < shaderNumCount * 0.75f)
					{
						regionsDifferent = true
					}
				}

				regionsPerLight[i++] = numCount
			}
		}

		if (regionsPerLight.size != shaderRegionsPerLight.size || regionsDifferent)
		{
			rebuildShader = true
			shaderRegionsPerLight = regionsPerLight
			lightShadowRegions = FloatArray(regionsPerLight.sum() * 4)
		}

		var i = 0
		for (light in basicLights)
		{
			lightPosRange[(i*3)+0] = light.pos.x * tileSize + offsetx
			lightPosRange[(i*3)+1] = light.pos.y * tileSize + offsety
			lightPosRange[(i*3)+2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)

			lightColourBrightness[(i*4)+0] = light.colour.r
			lightColourBrightness[(i*4)+1] = light.colour.g
			lightColourBrightness[(i*4)+2] = light.colour.b
			lightColourBrightness[(i*4)+3] = light.brightness

			i++
		}

		while (i < shaderLightNum)
		{
			lightPosRange[(i*3)+0] = -1f
			lightPosRange[(i*3)+1] = -1f
			lightPosRange[(i*3)+2] = -1f

			i++
		}

		var shadowCacheOffset = 0
		i = 0
		for (light in sortedShadowLights)
		{
			lightShadowColourBrightness[(i*4)+0] = light.colour.r
			lightShadowColourBrightness[(i*4)+1] = light.colour.g
			lightShadowColourBrightness[(i*4)+2] = light.colour.b
			lightShadowColourBrightness[(i*4)+3] = light.brightness

			val regionsToSet: com.badlogic.gdx.utils.Array<PointRect>
			if (shadowMode == ShadowMode.TILE)
			{
				regionsToSet = light.cache.currentCastRegions

				lightShadowPosRange[(i * 4) + 0] = light.pos.x * tileSize + offsetx
				lightShadowPosRange[(i * 4) + 1] = light.pos.y * tileSize + offsety
				lightShadowPosRange[(i * 4) + 2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)
				lightShadowPosRange[(i * 4) + 3] = if (light.cache.regionsVisible) 2.0f else -2.0f
			}
			else
			{
				regionsToSet = light.cache.opaqueRegions

				lightShadowPosRange[(i * 3) + 0] = light.pos.x * tileSize + offsetx
				lightShadowPosRange[(i * 3) + 1] = light.pos.y * tileSize + offsety
				lightShadowPosRange[(i * 3) + 2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)
			}

			val cacheStart = shadowCacheOffset
			var minx = 0f
			var miny = 0f
			var maxx = 0f
			var maxy = 0f
			for (region in regionsToSet)
			{
				minx = region.x * tileSize
				miny = region.y * tileSize
				maxx = minx + region.width * tileSize
				maxy = miny + region.height * tileSize

				lightShadowRegions[shadowCacheOffset++] = minx
				lightShadowRegions[shadowCacheOffset++] = miny
				lightShadowRegions[shadowCacheOffset++] = maxx
				lightShadowRegions[shadowCacheOffset++] = maxy
			}

			while (shadowCacheOffset < cacheStart + shaderRegionsPerLight[i] * 4)
			{
				lightShadowRegions[shadowCacheOffset++] = minx
				lightShadowRegions[shadowCacheOffset++] = miny
				lightShadowRegions[shadowCacheOffset++] = maxx
				lightShadowRegions[shadowCacheOffset++] = maxy
			}

			i++
		}

		while (i < shaderShadowLightNum)
		{
			if (shadowMode == ShadowMode.TILE)
			{
				lightShadowPosRange[(i * 4) + 0] = -1f
				lightShadowPosRange[(i * 4) + 1] = -1f
				lightShadowPosRange[(i * 4) + 2] = -1f
				lightShadowPosRange[(i * 4) + 3] = -1f
			}
			else
			{
				lightShadowPosRange[(i * 3) + 0] = -1f
				lightShadowPosRange[(i * 3) + 1] = -1f
				lightShadowPosRange[(i * 3) + 2] = -1f
			}

			i++
		}

		if (rebuildShader)
		{
			shader = createShader(shaderLightNum, shaderShadowLightNum, shaderRegionsPerLight)
		}

		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glDepthMask(false)
		shader.begin()

		shader.setUniformMatrix("u_projTrans", combinedMatrix)
		shader.setUniformf("u_offset", offsetx, offsety)
		shader.setUniformi("u_texture", 0)
		shader.setUniformf("u_ambient", ambientLight.vec3())

		if (shaderShadowLightNum > 0 || !smoothLighting)
		{
			shader.setUniformf("u_tileSize", tileSize)
		}

		shader.setUniform3fv("u_lightPosRange", lightPosRange, 0, lightPosRange.size)
		shader.setUniform4fv("u_lightColourBrightness", lightColourBrightness, 0, lightColourBrightness.size)

		if (shaderShadowLightNum > 0)
		{
			if (shadowMode == ShadowMode.TILE)
			{
				shader.setUniform4fv("u_shadowedLightPosRangeMode", lightShadowPosRange, 0, lightShadowPosRange.size)
			}
			else
			{
				shader.setUniform3fv("u_shadowedLightPosRange", lightShadowPosRange, 0, lightShadowPosRange.size)
			}

			shader.setUniform4fv("u_shadowedLightColourBrightness", lightShadowColourBrightness, 0, lightShadowColourBrightness.size)
			shader.setUniform4fv("u_shadowedLightRegions", lightShadowRegions, 0, lightShadowRegions.size)
		}

		executor.awaitAllJobs()

		if (currentBuffer != null)
		{
			queuedBuffers.add(currentBuffer!!)
			currentBuffer = null
		}

		var lastBlendSrc = -1
		var lastBlendDst = -1
		var lastTexture: Texture? = null
		var currentOffset = 0

		fun drawBuffer(buffer: VertexBuffer, mesh: BigMesh)
		{
			if (buffer.texture != lastTexture)
			{
				buffer.texture.bind()
				lastTexture = buffer.texture
			}

			if (buffer.blendSrc != lastBlendSrc || buffer.blendDst != lastBlendDst)
			{
				Gdx.gl.glBlendFunc(buffer.blendSrc, buffer.blendDst)

				lastBlendSrc = buffer.blendSrc
				lastBlendDst = buffer.blendDst
			}

			val spritesInBuffer = buffer.count / (4 * vertexSize)
			val drawCount = spritesInBuffer * 6
			mesh.render(shader, GL20.GL_TRIANGLES, currentOffset, drawCount)
			currentOffset += drawCount
		}

		if (staticBuffers.size > 0)
		{
			staticMesh.bind(shader)

			for (buffer in staticBuffers)
			{
				drawBuffer(buffer, staticMesh)
			}

			staticMesh.unbind(shader)
		}

		if (queuedBuffers.size > 0)
		{
			currentOffset = 0
			mesh.setVertices(vertices, 0, currentVertexCount)
			mesh.bind(shader)

			for (buffer in queuedBuffers)
			{
				drawBuffer(buffer, mesh)
				bufferPool.free(buffer)
			}
			queuedBuffers.clear()

			mesh.unbind(shader)
		}

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
		shader.end()

		currentVertexCount = 0
	}

	// ----------------------------------------------------------------------
	private fun queueRenderJobs()
	{
		for (i in 0 until queuedSprites)
		{
			val rs = spriteArray[i]!!

			var sprite = rs.sprite
			if (rs.tilingSprite != null)
			{
				bitflag.clear()
				for (dir in Direction.Values)
				{
					val hash = Point.getHashcode(rs.px, rs.py, dir)
					val keys = tilingMap[hash]

					if (keys?.contains(rs.tilingSprite!!.checkID) != true)
					{
						bitflag.setBit(dir)
					}
				}

				sprite = rs.tilingSprite!!.getSprite(bitflag)
			}

			var texture = rs.texture?.texture

			if (sprite != null)
			{
				texture = sprite.currentTexture.texture
			}

			requestRender(rs.blend.src, rs.blend.dst, texture!!, { vertices: FloatArray, offset: Int ->
				val localx = rs.x
				val localy = rs.y
				val localw = rs.width * tileSize
				val localh = rs.height * tileSize

				val colour = rs.colour

				if (sprite != null)
				{
					val renderCol = sprite.getRenderColour()
					if (!renderCol.isWhite()) colour.mul(renderCol)

					sprite.render(vertices, offset, colour, localx, localy, localw, localh, rs.scaleX, rs.scaleY, rs.rotation, rs.isLit)
				}

				if (rs.texture != null)
				{
					doDraw(vertices, offset,
													rs.texture!!, rs.nextTexture ?: rs.texture!!, colour,
													localx, localy, 0.5f, 0.5f, 1f, 1f, localw * rs.scaleX, localh * rs.scaleY, rs.rotation, rs.flipX, rs.flipY,
													0f, rs.blendAlpha, rs.alphaRef, rs.isLit, false)
				}
			} )
		}
	}

	// ----------------------------------------------------------------------
	private fun cleanup()
	{
		// clean up
		for (i in 0 until queuedSprites)
		{
			val rs = spriteArray[i]!!
			rs.free()
		}

		batchID = random.nextInt()
		Particle.generateBrownianVectors()

		for (entry in tilingMap)
		{
			setPool.free(entry.value)
		}
		tilingMap.clear()

		basicLights.clear()
		shadowLights.clear()

		if (queuedSprites < spriteArray.size / 4)
		{
			spriteArray = spriteArray.copyOf(spriteArray.size / 4)
		}

		queuedSprites = 0
	}

	// ----------------------------------------------------------------------
	private fun flush(batch: Batch)
	{
		// Begin prerender work
		executor.addJob {
			// sort
			spriteArray.sortWith(compareBy{ it?.comparisonVal ?: 0 }, 0, queuedSprites)

			// do screen shake
			if ( screenShakeRadius > 2 )
			{
				screenShakeAccumulator += delta

				while ( screenShakeAccumulator >= screenShakeSpeed )
				{
					screenShakeAccumulator -= screenShakeSpeed
					screenShakeAngle += (150 + Random.random() * 60)

					if (!screenShakeLocked)
					{
						screenShakeRadius *= 0.9f
					}
				}

				offsetx += Math.sin( screenShakeAngle.toDouble() ).toFloat() * screenShakeRadius
				offsety += Math.cos( screenShakeAngle.toDouble() ).toFloat() * screenShakeRadius
			}
		}

		for (light in basicLights)
		{
			executor.addJob {
				light.update(delta)
			}
		}

		for (light in shadowLights)
		{
			executor.addJob {
				light.update(delta)
			}
		}

		executor.awaitAllJobs()

		// begin rendering
		queueRenderJobs()

		if (inStaticBegin)
		{
			storeStatic()
		}
		else
		{
			combinedMatrix.set(batch.projectionMatrix).mul(batch.transformMatrix)
			waitOnRender()
		}

		cleanup()
	}

	// ----------------------------------------------------------------------
	private fun getComparisonVal(x: Float, y: Float, layer: Int, index: Int, blend: BlendMode) : Float
	{
		if (index > MAX_INDEX-1) throw RuntimeException("Index too high! $index >= $MAX_INDEX!")
		if (layer > MAX_LAYER-1) throw RuntimeException("Layer too high! $layer >= $MAX_LAYER!")

		val yBlock = y * Y_BLOCK_SIZE * -1f
		val xBlock = x * X_BLOCK_SIZE * -1f
		val lBlock = layer * LAYER_BLOCK_SIZE
		val iBlock = index * INDEX_BLOCK_SIZE
		val bBlock = blend.ordinal * BLEND_BLOCK_SIZE

		return yBlock + xBlock + lBlock + iBlock + bBlock
	}

	// ----------------------------------------------------------------------
	fun update(renderable: Renderable, deltaTime: Float? = null)
	{
		if (renderable.batchID != batchID) renderable.update(deltaTime ?: delta)
		renderable.batchID = batchID
	}

	// ----------------------------------------------------------------------
	fun queue(renderable: Renderable, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f)
	{
		if (renderable is Sprite) queueSprite(renderable, ix, iy, layer, index, colour, width, height)
		else if (renderable is TilingSprite) queueSprite(renderable, ix, iy, layer, index, colour, width, height)
		else if (renderable is ParticleEffect) queueParticle(renderable, ix, iy, layer, index, colour, width, height)
		else throw Exception("Unknown renderable type! " + renderable.javaClass)
	}

	// ----------------------------------------------------------------------
	private fun storeRenderSprite(renderSprite: RenderSprite)
	{
		if (queuedSprites == spriteArray.size-1)
		{
			spriteArray = spriteArray.copyOf(spriteArray.size * 2)
		}

		spriteArray[queuedSprites] = renderSprite

		queuedSprites++
	}

	// ----------------------------------------------------------------------
	fun queueParticle(effect: ParticleEffect, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")

		var lx = ix
		var ly = iy

		if (effect.lockPosition)
		{

		}
		else
		{
			if (effect.facing.x != 0)
			{
				lx = ix + effect.size[1].toFloat() * 0.5f
				ly = iy + effect.size[0].toFloat() * 0.5f
			}
			else
			{
				if (effect.isCentered)
				{
					lx = ix + 0.5f
					ly = iy + 0.5f
				}
				else
				{
					lx = ix + effect.size[0].toFloat() * 0.5f
					ly = iy + effect.size[1].toFloat() * 0.5f
				}
			}

			effect.setPosition(lx, ly)
		}

		update(effect)

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val posOffset = effect.animation?.renderOffset(false)
		lx += (posOffset?.get(0) ?: 0f)
		ly += (posOffset?.get(1) ?: 0f)

		if (effect.faceInMoveDirection)
		{
			val angle = getRotation(effect.lastPos, tempVec.set(lx, ly))
			effect.rotation = angle
			effect.lastPos.set(lx, ly)
		}

		if (effect.light != null)
		{
			addLight(effect.light!!, lx + 0.5f, ly + 0.5f)
		}

		//val scale = effect.animation?.renderScale()?.get(0) ?: 1f
		val animCol = effect.animation?.renderColour() ?: Colour.WHITE

		for (emitter in effect.emitters)
		{
			for (particle in emitter.particles)
			{
				var px = 0f
				var py = 0f

				if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
				{
					tempVec.set(emitter.currentOffset)
					tempVec.scl(emitter.size)
					tempVec.rotate(emitter.rotation)

					px += (emitter.position.x + tempVec.x)
					py += (emitter.position.y + tempVec.y)
				}

				for (pdata in particle.particles)
				{
					val keyframe1 = pdata.keyframe1
					val keyframe2 = pdata.keyframe2
					val alpha = pdata.keyframeAlpha

					val tex1 = keyframe1.texture[pdata.texStream]
					val tex2 = keyframe2.texture[pdata.texStream]

					val col = tempCol.set(keyframe1.colour[pdata.colStream]).lerp(keyframe2.colour[pdata.colStream], alpha)
					col.a = keyframe1.alpha[pdata.alphaStream].lerp(keyframe2.alpha[pdata.alphaStream], alpha)

					val size = keyframe1.size[pdata.sizeStream].lerp(keyframe2.size[pdata.sizeStream], alpha, pdata.ranVal)

					var w = width
					var h = height
					if (particle.maintainAspectRatio)
					{
						w = min(width, height)
						h = w
					}

					var sizex = if (particle.sizeMode == Particle.SizeMode.YONLY) w else size * w
					var sizey = if (particle.sizeMode == Particle.SizeMode.XONLY) h else size * h

					if (particle.allowResize)
					{
						sizex *= emitter.size.x
						sizey *= emitter.size.y
					}

					val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

					col.mul(colour).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					var drawx = tempVec.x + px
					var drawy = tempVec.y + py

					when (particle.sizeOrigin)
					{
						Particle.SizeOrigin.CENTER -> { }
						Particle.SizeOrigin.BOTTOM -> {
							drawy += sizey*0.5f
						}
						Particle.SizeOrigin.TOP -> {
							drawy -= sizey*0.5f
						}
						Particle.SizeOrigin.LEFT -> {
							drawx += sizex*0.5f
						}
						Particle.SizeOrigin.RIGHT -> {
							drawx -= sizex*0.5f
						}
					}

					val localx = drawx * tileSize + offsetx
					val localy = drawy * tileSize + offsety
					val localw = sizex * tileSize
					val localh = sizey * tileSize

					if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) continue

					val comparisonVal = getComparisonVal((drawx-sizex*0.5f-1f).floor().toFloat(), (drawy-sizey*0.5f-1f).floor().toFloat(), layer, index, particle.blend)

					val tex1Index = tex1.toInt()
					val texture1 = particle.textures[pdata.texStream][tex1Index].second

					val rs = RenderSprite.obtain().set(null, null, texture1, drawx * tileSize, drawy * tileSize, tempVec.x, tempVec.y, col, sizex, sizey, rotation, 1f, 1f, effect.flipX, effect.flipY, particle.blend, lit, comparisonVal)

					if (particle.blendKeyframes)
					{
						val tex2Index = min(particle.textures[pdata.texStream].size-1, tex1Index+1)
						val texture2 = particle.textures[pdata.texStream][tex2Index].second
						val blendAlpha = tex1.lerp(tex2, pdata.keyframeAlpha)

						rs.nextTexture = texture2
						rs.blendAlpha = blendAlpha
					}
					rs.alphaRef = keyframe1.alphaRef[pdata.alphaRefStream].lerp(keyframe2.alphaRef[pdata.alphaRefStream], alpha)

					storeRenderSprite(rs)
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun addToMap(tilingSprite: TilingSprite, ix: Float, iy: Float)
	{
		// Add to map
		val hash = Point.getHashcode(ix.toInt(), iy.toInt())
		var keys = tilingMap[hash]
		if (keys == null)
		{
			keys = setPool.obtain()
			keys.clear()

			tilingMap[hash] = keys
		}
		keys.add(tilingSprite.checkID)
	}

	// ----------------------------------------------------------------------
	fun addLight(light: Light, ix: Float, iy: Float)
	{
		light.pos.set(ix, iy)

		if (Statics.collisionGrid != null && light.hasShadows && shadowMode != ShadowMode.NONE)
		{
			shadowLights.add(light)
		}
		else
		{
			basicLights.add(light)
		}
	}

	// ----------------------------------------------------------------------
	fun queueSprite(tilingSprite: TilingSprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")

		update(tilingSprite)

		if (!tilingSprite.visible) return
		if (tilingSprite.renderDelay > 0 && !tilingSprite.showBeforeRender)
		{
			return
		}

		var lx = ix
		var ly = iy

		var x = ix * tileSize
		var y = iy * tileSize

		if ( tilingSprite.animation != null )
		{
			val offset = tilingSprite.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}
		}

		addToMap(tilingSprite, ix, iy)

		if (tilingSprite.light != null)
		{
			addLight(tilingSprite.light!!, lx + 0.5f, ly + 0.5f)
		}

		// check if onscreen
		if (!alwaysOnscreen && !isSpriteOnscreen(tilingSprite, x, y, width, height)) return

		val comparisonVal = getComparisonVal(lx, ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, tilingSprite, null, x, y, ix, iy, colour, width, height, 0f, 1f, 1f, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	fun queueSprite(sprite: Sprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")

		update(sprite)

		if (!sprite.visible) return
		if (sprite.renderDelay > 0 && !sprite.showBeforeRender)
		{
			return
		}

		var lx = ix
		var ly = iy

		var x = ix * tileSize
		var y = iy * tileSize

		var rotation = 0f

		var lScaleX = sprite.baseScale[0] * scaleX
		var lScaleY = sprite.baseScale[1] * scaleY

		if ( sprite.animation != null )
		{
			val offset = sprite.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}

			rotation = sprite.animation?.renderRotation() ?: 0f

			val scale = sprite.animation!!.renderScale()
			if (scale != null)
			{
				lScaleX *= scale[0]
				lScaleY *= scale[1]
			}
		}

		if (sprite.drawActualSize)
		{
			val widthRatio = width / 32f
			val regionWidth = sprite.currentTexture.regionWidth.toFloat()
			val trueWidth = regionWidth * widthRatio
			val widthOffset = (trueWidth - width) / 2

			lx -= widthOffset
		}

		lx = lx + 0.5f - (0.5f * lScaleX)
		ly = ly + 0.5f - (0.5f * lScaleY)

		if (sprite.faceInMoveDirection)
		{
			val angle = getRotation(sprite.lastPos, tempVec.set(x, y))
			sprite.rotation = angle
			sprite.lastPos.set(x, y)
		}

		if (sprite.light != null)
		{
			addLight(sprite.light!!, ix + 0.5f, iy + 0.5f)
		}

		// check if onscreen
		if (!alwaysOnscreen && !isSpriteOnscreen(sprite, x, y, width, height, scaleX, scaleY)) return

		val comparisonVal = getComparisonVal(lx, ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(sprite, null, null, x, y, ix, iy, colour, width, height, rotation, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	fun queueTexture(texture: TextureRegion, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, sortX: Float? = null, sortY: Float? = null, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")

		val lx = ix - width
		val ly = iy - height

		val x = ix * tileSize
		val y = iy * tileSize

		// check if onscreen

		val localx = x + offsetx
		val localy = y + offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, null, texture, x, y, ix, iy, colour, width, height, 0f, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	private fun isSpriteOnscreen(sprite: Sprite, x: Float, y: Float, width: Float, height: Float, scaleX: Float = 1f, scaleY: Float = 1f): Boolean
	{
		var localx = x + offsetx
		var localy = y + offsety
		var localw = width * tileSize * sprite.size[0]
		var localh = height * tileSize * sprite.size[1]

		var scaleX = sprite.baseScale[0] * scaleX
		var scaleY = sprite.baseScale[1] * scaleY

		if (sprite.animation != null)
		{
			val scale = sprite.animation!!.renderScale()
			if (scale != null)
			{
				scaleX *= scale[0]
				scaleY *= scale[1]
			}
		}

		if (sprite.drawActualSize)
		{
			val texture = sprite.textures.items[sprite.texIndex]

			val widthRatio = localw / 32f
			val heightRatio = localh / 32f

			val regionWidth = sprite.referenceSize ?: texture.regionWidth.toFloat()
			val regionHeight = sprite.referenceSize ?: texture.regionHeight.toFloat()

			val trueWidth = regionWidth * widthRatio
			val trueHeight = regionHeight * heightRatio

			val widthOffset = (trueWidth - localw) / 2f

			localx -= widthOffset
			localw = trueWidth
			localh = trueHeight
		}

		if (sprite.rotation != 0f && sprite.fixPosition)
		{
			val offset = Sprite.getPositionCorrectionOffsets(x, y, localw / 2.0f, localh / 2.0f, localw, localh, scaleX, scaleY, sprite.rotation, tempVec3)
			localx -= offset.x
			localy -= offset.y
		}

		if (scaleX != 1f)
		{
			val newW = localw * scaleX
			val diff = newW - localw

			localx -= diff * 0.5f
			localw = newW
		}
		if (scaleY != 1f)
		{
			val newH = localh * scaleY
			val diff = newH - localh

			localy -= diff * 0.5f
			localh = newH
		}

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return false

		return true
	}

	// ----------------------------------------------------------------------
	private fun isSpriteOnscreen(sprite: TilingSprite, x: Float, y: Float, width: Float, height: Float): Boolean
	{
		val localx = x + offsetx
		val localy = y + offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return false

		return true
	}

	// ----------------------------------------------------------------------
	companion object
	{
		enum class ShadowMode
		{
			NONE,
			TILE,
			SMOOTH
		}

		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		private val smoothLighting = true
		private val shadowMode: ShadowMode = ShadowMode.TILE
		private val random = LightRNG()

		public const val vertexSize = 4 + 4 + 1 + 1
		private const val maxSprites = 10000
		public const val verticesASprite = vertexSize * 4
		private const val maxVertices = maxSprites * vertexSize

		private val cachedShaders = ObjectMap<String, ShaderProgram>()

		fun createShader(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): ShaderProgram
		{
			val key = numLights.toString() + numShadowLights.toString() + regionsPerLight.map { it.toString() }.joinToString()

			val existing = cachedShaders[key]
			if (existing != null)
			{
				return existing
			}

			val vertexShader = getVertexOptimised()
			val fragmentShader = getFragmentOptimised(numLights, numShadowLights, regionsPerLight)

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			cachedShaders[key] = shader

			return shader
		}

		fun getVertexUnoptimised(): String
		{
			val vertexShader = """
attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.TEXCOORD_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec4 a_additionalData;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

varying vec4 v_color;
varying vec2 v_spritePos;
varying vec2 v_pixelPos;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying float v_blendAlpha;
varying float v_isLit;
varying float v_alphaRef;
varying float v_brightness;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos = vec4(worldPos.x, worldPos.y, 0.0, 1.0);

	v_pixelPos = worldPos;
	v_spritePos = ${ShaderProgram.POSITION_ATTRIBUTE}.zw;
	v_texCoords1 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.xy;
	v_texCoords2 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.zw;
	v_blendAlpha = a_additionalData.x;
	v_isLit = float(a_additionalData.y == 0.0);
	v_alphaRef = a_additionalData.z;
	v_brightness = a_additionalData.w;
	gl_Position = u_projTrans * truePos;
}
"""

			return vertexShader
		}

		fun getVertexOptimised(): String
		{
			val androidDefine = if (Statics.android) "#define LOWP lowp" else "#define LOWP"

			val vertexShader = """
$androidDefine

attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.TEXCOORD_ATTRIBUTE};
attribute LOWP vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute LOWP vec4 a_additionalData;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

varying LOWP vec4 v_color;
varying vec4 v_pos;
varying vec4 v_texCoords;
varying LOWP vec4 v_additionalData;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos;
	truePos.xy = worldPos;
	truePos.zw = vec2(0.0, 1.0);

	v_pos.xy = worldPos;
	v_pos.zw = ${ShaderProgram.POSITION_ATTRIBUTE}.zw;
	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE};
	v_additionalData = a_additionalData;

	gl_Position = u_projTrans * truePos;
}
"""

			return vertexShader
		}

		fun getFragmentUnoptimised(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): String
		{
			val shadowDefine =
				if (numShadowLights > 0)
				{
					when (shadowMode)
					{
						ShadowMode.NONE -> ""
						ShadowMode.SMOOTH -> "#define SMOOTHSHADOWS 1"
						ShadowMode.TILE -> "#define TILESHADOWS 1"
					}
				}
				else ""

			var regionsDefine = "const vec2 regionSizes[${regionsPerLight.size}] = vec2[${regionsPerLight.size}]("

			var currentRegionIndex = 0
			for (region in regionsPerLight)
			{
				if (currentRegionIndex != 0)
				{
					regionsDefine += ", "
				}

				regionsDefine += "vec2($currentRegionIndex, $region)"
				currentRegionIndex += region
			}

			regionsDefine += ");"

			val tileLightingDefine = if (!smoothLighting) "#define TILELIGHTING 1" else ""
			val fragmentShader = """

$shadowDefine
$tileLightingDefine

varying vec4 v_color;
varying vec2 v_spritePos;
varying vec2 v_pixelPos;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying float v_blendAlpha;
varying float v_isLit;
varying float v_alphaRef;
varying float v_brightness;

uniform float u_tileSize;

uniform vec3 u_ambient;

uniform vec3 u_lightPosRange[$numLights];
uniform vec4 u_lightColourBrightness[$numLights];

#ifdef SMOOTHSHADOWS
uniform vec3 u_shadowedLightPosRange[$numShadowLights];
uniform vec4 u_shadowedLightColourBrightness[$numShadowLights];
$regionsDefine
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

#ifdef TILESHADOWS
uniform vec4 u_shadowedLightPosRangeMode[$numShadowLights];
uniform vec4 u_shadowedLightColourBrightness[$numShadowLights];
$regionsDefine
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

uniform sampler2D u_texture;

// ------------------------------------------------------
float calculateLightStrength(vec3 posRange)
{
	vec2 pos = posRange.xy;
	float rangeSq = posRange.z;

	vec2 pixelPos = v_pixelPos;

#ifdef TILELIGHTING
	pixelPos = (floor(v_spritePos / u_tileSize)) * u_tileSize;
	pos = (floor(pos / u_tileSize)) * u_tileSize;
#endif

	vec2 diff = pos - pixelPos;
	float distSq = (diff.x * diff.x) + (diff.y * diff.y);

	float lightStrength = step(distSq, rangeSq);
	float alpha = 1.0 - (distSq / rangeSq);

	return lightStrength * alpha;
}

// ------------------------------------------------------
vec3 calculateLight(int index)
{
	vec3 posRange = u_lightPosRange[index];
	vec4 colourBrightness = u_lightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRange);

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}

#ifdef TILESHADOWS

// ------------------------------------------------------
bool insideBox(vec2 v, vec2 bottomLeft, vec2 topRight)
{
    vec2 s = step(bottomLeft, v) - step(topRight, v);
    return s.x * s.y != 0.0;
}

// ------------------------------------------------------
bool isPointVisible(int index, vec2 point, bool regionsAreLit)
{
	bool inRegion = false;
	for (int i = 0; i < regionSizes[index].y; i++)
	{
		vec4 region = u_shadowedLightRegions[regionSizes[index].x + i];
		inRegion = inRegion || insideBox(point, region.xy, region.zw);
	}

	return (regionsAreLit == inRegion);
}

// ------------------------------------------------------
vec3 calculateShadowLight(int index)
{
	vec4 posRangeMode = u_shadowedLightPosRangeMode[index];
	vec4 colourBrightness = u_shadowedLightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRangeMode.xyz);

	vec2 pixelPos = v_pixelPos;
	pixelPos.y = v_spritePos.y + min(pixelPos.y - v_spritePos.y, u_tileSize*0.9);

	float multiplier = float(isPointVisible(index, pixelPos, posRangeMode.w > 0.0));

	lightStrength *= multiplier;

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}

#endif

#ifdef SMOOTHSHADOWS

// ------------------------------------------------------
float rayBoxIntersect ( vec2 rpos, vec2 rdir, vec2 vmin, vec2 vmax )
{
	float t0 = (vmin.x - rpos.x) * rdir.x;
	float t1 = (vmax.x - rpos.x) * rdir.x;
	float t2 = (vmin.y - rpos.y) * rdir.y;
	float t3 = (vmax.y - rpos.y) * rdir.y;

	float t4 = max(min(t0, t1), min(t2, t3));
	float t5 = min(max(t0, t1), max(t2, t3));

	float t6 = (t5 < 0.0 || t4 > t5) ? -1.0 : t4;
	return t6;
}

// ------------------------------------------------------
float insideBox(vec2 v, vec2 bottomLeft, vec2 topRight)
{
    vec2 s = step(bottomLeft, v) - step(topRight, v);
    return s.x * s.y;
}

// ------------------------------------------------------
bool isPointVisible(int index, vec2 point)
{
	vec3 posRange = u_shadowedLightPosRange[index];

	vec2 baseTile = (floor(v_spritePos / u_tileSize)) * u_tileSize;
	vec2 lightTile = (floor(posRange.xy / u_tileSize)) * u_tileSize;

	vec2 pixelPos = v_pixelPos;
	pixelPos.y = v_spritePos.y + min(pixelPos.y - v_spritePos.y, u_tileSize*0.9);

	vec2 diff = point - pixelPos;
	float rayLen = length(diff);
	vec2 rdir = 1.0 / (diff / rayLen);

	float collided = 0.0;
	float collidedOverride = 0.0;
	for (int i = 0; i < regionSizes[index].y; i++)
	{
		vec4 occluder = u_shadowedLightRegions[regionSizes[index].x + i];
		float intersect = rayBoxIntersect(pixelPos, rdir, occluder.xy, occluder.zw);

		collided += float(intersect > 0.0 && intersect < rayLen);

		occluder.xy = (floor(occluder.xy / u_tileSize)) * u_tileSize;
		collidedOverride += insideBox(baseTile, occluder.xy, occluder.zw);
	}

	return collided == 0.0 || collidedOverride > 0.0;
}

// ------------------------------------------------------
vec3 calculateShadowLight(int index)
{
	vec3 posRange = u_shadowedLightPosRange[index];
	vec4 colourBrightness = u_shadowedLightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRange);

	float multiplier = float(isPointVisible(index, posRange.xy));

	lightStrength *= multiplier;

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}
#endif

// ------------------------------------------------------
void main()
{
	vec4 col1 = texture2D(u_texture, v_texCoords1);
	vec4 col2 = texture2D(u_texture, v_texCoords2);

	vec4 outCol = mix(col1, col2, v_blendAlpha);

	vec3 lightCol = u_ambient;

	for (int i = 0; i < $numLights; i++)
	{
		lightCol += calculateLight(i);
	}

#ifdef SMOOTHSHADOWS
	for (int i = 0; i < $numShadowLights; i++)
	{
		lightCol += calculateShadowLight(i);
	}
#endif

#ifdef TILESHADOWS
	for (int i = 0; i < $numShadowLights; i++)
	{
		lightCol += calculateShadowLight(i);
	}
#endif

	lightCol = mix(vec3(1.0, 1.0, 1.0), lightCol, v_isLit);

	if (v_color.a * outCol.a < v_alphaRef)
	{
		outCol = vec4(0.0, 0.0, 0.0, 0.0);
	}

	vec4 objCol = vec4(v_color.rgb * v_brightness * 255.0, v_color.a);

	vec4 finalCol = clamp(objCol * outCol * vec4(lightCol, 1.0), 0.0, 1.0);
	gl_FragColor = finalCol;
}
"""

			return fragmentShader
		}

		fun getFragmentOptimised(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): String
		{
			val androidDefine = if (Statics.android) "#define LOWP lowp\nprecision mediump float;" else "#define LOWP"

			val shadowDefine =
				if (numShadowLights > 0)
				{
					when (shadowMode)
					{
						ShadowMode.NONE -> ""
						ShadowMode.SMOOTH -> "#define SMOOTHSHADOWS 1"
						ShadowMode.TILE -> "#define TILESHADOWS 1"
					}
				}
				else ""

			val tileLightingDefine = if (!smoothLighting) "#define TILELIGHTING 1" else ""
			var fragmentShader = """

$shadowDefine
$tileLightingDefine
$androidDefine

varying LOWP vec4 v_color;
varying vec4 v_pos;
varying vec4 v_texCoords;
varying LOWP vec4 v_additionalData;

uniform float u_tileSize;

uniform LOWP vec3 u_ambient;

uniform vec3 u_lightPosRange[$numLights];
uniform LOWP vec4 u_lightColourBrightness[$numLights];

#ifdef SMOOTHSHADOWS
uniform vec3 u_shadowedLightPosRange[$numShadowLights];
uniform LOWP vec4 u_shadowedLightColourBrightness[$numShadowLights];
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

#ifdef TILESHADOWS
uniform vec4 u_shadowedLightPosRangeMode[$numShadowLights];
uniform LOWP vec4 u_shadowedLightColourBrightness[$numShadowLights];
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

uniform sampler2D u_texture;

// ------------------------------------------------------
void main ()
{
	LOWP vec3 lightCol = u_ambient;

#ifdef TILELIGHTING
	vec2 spriteTile = floor(v_pos.zw / u_tileSize) * u_tileSize;
#endif

"""

			fragmentShader += "//########## Basic Lighting ##########//\n"
			for (i in 0 until numLights)
			{
				fragmentShader += """
	vec3 posRange_$i = u_lightPosRange[$i];
	LOWP vec4 colourBrightness_$i = u_lightColourBrightness[$i];

#ifdef TILELIGHTING
	vec2 diff_$i = posRange_$i.xy - spriteTile;
#else
	vec2 diff_$i = posRange_$i.xy - v_pos.xy;
#endif

	float len_$i = (diff_$i.x * diff_$i.x) + (diff_$i.y * diff_$i.y);
	lightCol = lightCol + (colourBrightness_$i.rgb * (colourBrightness_$i.a * (1.0 - (len_$i / posRange_$i.z)) * float(posRange_$i.z >= len_$i)));

"""
			}

			if (shadowMode == ShadowMode.SMOOTH)
			{
				if (numShadowLights > 0)
				{
				fragmentShader += """
//########## Shadow Lighting ##########//

	vec2 shadowPixelPos;
	shadowPixelPos.x = v_pixelPos.x;
	shadowPixelPos.y = (v_spritePos.y + min((v_pixelPos.y - v_spritePos.y), (u_tileSize * 0.9)));

				"""

					var regionIndex = 0
					for (li in 0 until numShadowLights)
					{
						fragmentShader += """
	vec3 shadowPosRange_$li = u_shadowedLightPosRange[$li];
	LOWP vec4 shadowColourBrightness_$li = u_shadowedLightColourBrightness[$li];

#ifdef TILELIGHTING
	vec2 shadowDiff_$li = (shadowPosRange_$li.xy - spriteTile);
#else
	vec2 shadowDiff_$li = (shadowPosRange_$li.xy - v_pos.xy);
#endif
	float shadowLen_$li = ((shadowDiff_$li.x * shadowDiff_$li.x) + (shadowDiff_$li.y * shadowDiff_$li.y));

	LOWP float shadowLightStrength_$li = (float((shadowPosRange_$li.z >= shadowLen_$li)) * (1.0 - (shadowLen_$li / shadowPosRange_$li.z)));

	vec2 rayDiff_$li = (shadowPosRange_$li.xy - shadowPixelPos);
	float rayLen_$li = sqrt(dot(rayDiff_$li, rayDiff_$li));

	LOWP vec2 rdir_$li = (1.0/((rayDiff_$li / rayLen_$li)));
	LOWP float collided_$li = 0.0;
	LOWP float collidedOverride_$li = 0.0;

	vec2 occluderPixel_${li};
	vec2 tmax_${li};
	vec2 tmin_${li};
						"""

						for (oi in 0 until regionsPerLight[li])
						{
							fragmentShader += """

	vec4 occluder_${li}_$oi = u_shadowedLightRegions[${regionIndex++}];

	tmin_${li} = (occluder_${li}_$oi.xy - shadowPixelPos) * rdir_$li;
	tmax_${li} = (occluder_${li}_$oi.zw - shadowPixelPos) * rdir_$li;

	float t4_${li}_$oi = max(min(tmin_${li}.x, tmax_${li}.x), min(tmin_${li}.y, tmax_${li}.y));
	float t5_${li}_$oi = min(max(tmin_${li}.x, tmax_${li}.x), max(tmin_${li}.y, tmax_${li}.y));

	LOWP float intersection_${li}_$oi = (t5_${li}_$oi < 0.0 || t4_${li}_$oi > t5_${li}_$oi) ? -1.0 : t4_${li}_$oi;

	collided_$li = collided_$li + float((intersection_${li}_$oi > 0.0) && (intersection_${li}_$oi < rayLen_$li));

	LOWP vec2 inBox_${li}_$oi = (vec2(greaterThanEqual(baseTile, occluder_${li}_$oi.xy)) - vec2(greaterThanEqual(baseTile, occluder_${li}_$oi.zw)));
	collidedOverride_$li = (collidedOverride_$li + (inBox_${li}_$oi.x * inBox_${li}_$oi.y));

	"""
						}

						fragmentShader += """
	shadowLightStrength_$li *= float(((collided_$li == 0.0) || (collidedOverride_$li > 0.0)));
	lightCol = (lightCol + ((shadowColourBrightness_$li.xyz * shadowColourBrightness_$li.w) * shadowLightStrength_$li));

						"""
					}
				}
			}
			else if (shadowMode == ShadowMode.TILE)
			{
				if (numShadowLights > 0)
				{
					fragmentShader += """
//########## Shadow Lighting ##########//

	vec2 shadowPixelPos;
	shadowPixelPos.x = v_pos.x;
	shadowPixelPos.y = v_pos.w + min(v_pos.y - v_pos.w, u_tileSize * 0.9);
	"""
					var regionIndex = 0
					for (li in 0 until numShadowLights)
					{
						fragmentShader += """
	vec4 shadowPosRangeMode_$li = u_shadowedLightPosRangeMode[$li];
	LOWP vec4 shadowColourBrightness_$li = u_shadowedLightColourBrightness[$li];

#ifdef TILELIGHTING
	vec2 shadowDiff_$li = shadowPosRangeMode_$li.xy - spriteTile;
#else
	vec2 shadowDiff_$li = shadowPosRangeMode_$li.xy - v_pos.xy;
#endif
	float shadowLen_$li = (shadowDiff_$li.x * shadowDiff_$li.x) + (shadowDiff_$li.y * shadowDiff_$li.y);

	LOWP float shadowLightStrength_$li = float(shadowPosRangeMode_$li.z >= shadowLen_$li) * (1.0 - (shadowLen_$li / shadowPosRangeMode_$li.z));

	LOWP float inRegion_$li = 0.0;
						"""

						for (oi in 0 until regionsPerLight[li])
						{
							fragmentShader += """

	vec4 region_${li}_$oi = u_shadowedLightRegions[${regionIndex++}];

	LOWP vec2 inBox_${li}_$oi = vec2(greaterThanEqual(shadowPixelPos, region_${li}_$oi.xy)) - vec2(greaterThanEqual(shadowPixelPos, region_${li}_$oi.zw));
	inRegion_$li = inRegion_$li + (inBox_${li}_$oi.x * inBox_${li}_$oi.y);

	"""
						}

						fragmentShader += """

	shadowLightStrength_$li = shadowLightStrength_$li * float(shadowPosRangeMode_$li.w > 0.0 == inRegion_$li > 0.0);
	lightCol = lightCol + (shadowColourBrightness_$li.xyz * (shadowColourBrightness_$li.w * shadowLightStrength_$li));

						"""
					}
				}
			}

			fragmentShader += """

//########## final composite ##########//
	LOWP vec4 lightCol4;
	lightCol4.rgb = mix(vec3(1.0, 1.0, 1.0), lightCol, 1.0 - v_additionalData.y);
	lightCol4.a = 1.0;

	LOWP vec4 objCol = vec4(v_color.rgb * (v_additionalData.w * 255.0), v_color.a);

	LOWP vec4 outCol = clamp(objCol * mix(texture2D(u_texture, v_texCoords.xy), texture2D(u_texture, v_texCoords.zw), v_additionalData.x) * lightCol4, 0.0, 1.0);
	outCol *= float(outCol.a > v_additionalData.z); // apply alpharef

	gl_FragColor = outCol;
}
				"""

			return fragmentShader
		}
	}
}

// ----------------------------------------------------------------------
class RenderSprite(val parentBlock: RenderSpriteBlock, val parentBlockIndex: Int) : Comparable<RenderSprite>
{
	internal var px: Int = 0
	internal var py: Int = 0
	internal val colour: Colour = Colour(1f, 1f, 1f, 1f)
	internal var sprite: Sprite? = null
	internal var tilingSprite: TilingSprite? = null
	internal var texture: TextureRegion? = null
	internal var nextTexture: TextureRegion? = null
	internal var blendAlpha = 0f
	internal var x: Float = 0f
	internal var y: Float = 0f
	internal var width: Float = 1f
	internal var height: Float = 1f
	internal var rotation: Float = 0f
	internal var scaleX: Float = 1f
	internal var scaleY: Float = 1f
	internal var flipX: Boolean = false
	internal var flipY: Boolean = false
	internal var blend: BlendMode = BlendMode.MULTIPLICATIVE
	internal var isLit: Boolean = true
	internal var alphaRef: Float = 0f

	val tempColour = Colour()
	val tlCol = Colour()
	val trCol = Colour()
	val blCol = Colour()
	val brCol = Colour()

	internal var comparisonVal: Float = 0f

	// ----------------------------------------------------------------------
	operator fun set(sprite: Sprite?, tilingSprite: TilingSprite?, texture: TextureRegion?,
					 x: Float, y: Float,
					 ix: Float, iy: Float,
					 colour: Colour,
					 width: Float, height: Float,
					 rotation: Float,
					 scaleX: Float, scaleY: Float,
					 flipX: Boolean, flipY: Boolean,
					 blend: BlendMode, lit: Boolean,
					 comparisonVal: Float): RenderSprite
	{
		this.px = ix.toInt()
		this.py = iy.toInt()
		this.colour.set(colour)
		this.sprite = sprite
		this.tilingSprite = tilingSprite
		this.texture = texture
		this.x = x
		this.y = y
		this.width = width
		this.height = height
		this.comparisonVal = comparisonVal
		this.blend = blend
		this.rotation = rotation
		this.scaleX = scaleX
		this.scaleY = scaleY
		this.flipX = flipX
		this.flipY = flipY
		this.isLit = lit
		this.blendAlpha = 0f
		this.alphaRef = 0f

		nextTexture = null

		return this
	}

	// ----------------------------------------------------------------------
	override fun compareTo(other: RenderSprite): Int
	{
		return comparisonVal.compareTo(other.comparisonVal)
	}

	// ----------------------------------------------------------------------
	internal fun free() = parentBlock.free(this)

	// ----------------------------------------------------------------------
	companion object
	{
		private var currentBlock: RenderSpriteBlock = RenderSpriteBlock.obtain()

		internal fun obtain(): RenderSprite
		{
			val rs = currentBlock.obtain()

			if (currentBlock.full())
			{
				currentBlock = RenderSpriteBlock.obtain()
			}

			return rs
		}
	}
}

// ----------------------------------------------------------------------
class RenderSpriteBlock
{
	private var count = 0
	private var index: Int = 0
	private val sprites = Array(blockSize) { RenderSprite(this, it) }

	internal inline fun full() = index == blockSize

	internal fun obtain(): RenderSprite
	{
		val sprite = sprites[index]
		index++
		count++

		return sprite
	}

	internal fun free(data: RenderSprite)
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
		public const val blockSize: Int = 128

		fun obtain(): RenderSpriteBlock
		{
			val block = pool.obtain()
			return block
		}

		private val pool: Pool<RenderSpriteBlock> = object : Pool<RenderSpriteBlock>() {
			override fun newObject(): RenderSpriteBlock
			{
				return RenderSpriteBlock()
			}
		}
	}
}