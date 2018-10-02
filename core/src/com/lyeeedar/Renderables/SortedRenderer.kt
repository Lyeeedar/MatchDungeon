package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.HDRColourSpriteBatch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.RadixSort.Companion.MOST_SIGNIFICANT_BYTE_INDEX
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.*
import ktx.collections.set
import squidpony.squidmath.LightRNG


/**
 * Created by Philip on 04-Jul-16.
 */

class SortedRenderer(var tileSize: Float, val width: Float, val height: Float, val layers: Int, val alwaysOnscreen: Boolean)
{
	var batchID: Int = 0

	val tempVec = Vector2()
	val tempPoint = Point()
	val tempCol = Colour()
	val tempCol2 = Colour()
	val bitflag = EnumBitflag<Direction>()

	val startingArraySize = 128
	var spriteArray = Array<RenderSprite?>(startingArraySize) { null }
	var queuedSprites = 0

	var tilingMap: ObjectMap<Point, ObjectSet<Long>> = ObjectMap()

	val setPool: Pool<ObjectSet<Long>> = object : Pool<ObjectSet<Long>>() {
		override fun newObject(): ObjectSet<Long>
		{
			return ObjectSet()
		}

	}

	var screenShakeRadius: Float = 0f
	var screenShakeAccumulator: Float = 0f
	var screenShakeSpeed: Float = 0f
	var screenShakeAngle: Float = 0f
	var screenShakeLocked: Boolean = false

	val BLENDMODES = BlendMode.values().size
	val MAX_INDEX = 6 * BLENDMODES
	val X_BLOCK_SIZE = layers * MAX_INDEX
	val Y_BLOCK_SIZE = X_BLOCK_SIZE * width.toInt()
	val MAX_Y_BLOCK_SIZE = Y_BLOCK_SIZE * height.toInt()
	val MAX_X_BLOCK_SIZE = X_BLOCK_SIZE * width.toInt()

	var delta: Float = 0f

	var debugDrawSpeed = 1.0f
	var debugDrawAccumulator = 0.0f
	var debugDraw = false
	var debugDrawIndex = 0
	var inDebugFrame = false
	var debugDrawList = com.badlogic.gdx.utils.Array<RenderSprite>()

	var inBegin = false
	var offsetx: Float = 0f
	var offsety: Float = 0f

	// ----------------------------------------------------------------------
	fun begin(deltaTime: Float, offsetx: Float, offsety: Float)
	{
		if (inBegin) throw Exception("Begin called again before flush!")

		delta = deltaTime
		this.offsetx = offsetx
		this.offsety = offsety
		inBegin = true
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
	fun flush(batch: Batch)
	{
		if (!inBegin) throw Exception("Flush called before begin!")

		// sort
		RadixSort.sort(spriteArray, spriteArray.copyOfRange(0, queuedSprites), 0, 0, queuedSprites, MOST_SIGNIFICANT_BYTE_INDEX)
		//spriteArray.sort(0, queuedSprites)

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

		fun draw(rs: RenderSprite)
		{
			val localx = rs.x + offsetx
			val localy = rs.y + offsety
			val localw = rs.width * tileSize
			val localh = rs.height * tileSize

			batch.setBlendFunction(rs.blend.src, rs.blend.dst)

			if (batch is HDRColourSpriteBatch) batch.setColor(rs.colour)
			else batch.setColor(rs.colour.toFloatBits())

			rs.sprite?.render(batch, localx, localy, localw, localh, rs.scaleX, rs.scaleY, rs.rotation)

			if (rs.tilingSprite != null)
			{
				bitflag.clear()
				for (dir in Direction.Values)
				{
					tempPoint.set(rs.point).plusAssign(dir)
					val keys = tilingMap[tempPoint]

					if (keys?.contains(rs.tilingSprite!!.checkID) != true)
					{
						bitflag.setBit(dir)
					}
				}

				val sprite = rs.tilingSprite!!.getSprite(bitflag)
				sprite.render(batch, localx, localy, localw, localh, rs.scaleX, rs.scaleY )
			}

			if (rs.texture != null)
			{
				if (rs.nextTexture != null)
				{
					drawBlend(batch, rs.texture!!, rs.nextTexture!!, rs.blendAlpha, localx, localy, 0.5f, 0.5f, 1f, 1f, localw * rs.scaleX, localh * rs.scaleY, rs.rotation, rs.flipX, rs.flipY, 0f)
				}
				else
				{
					com.lyeeedar.Util.draw(batch, rs.texture!!, localx, localy, 0.5f, 0.5f, 1f, 1f, localw * rs.scaleX, localh * rs.scaleY, rs.rotation, rs.flipX, rs.flipY, 0f)
				}
			}

			if (rs.ninePatch != null)
			{
				val w = localw * rs.scaleX
				val h = localh * rs.scaleY

				rs.ninePatch!!.draw(batch, localx - w/2f, localy - h/2f, 0.5f, 0.5f, w, h, 1f, 1f, rs.rotation)
			}
		}

		if (debugDraw)
		{
			for (rs in debugDrawList)
			{
				draw(rs)
			}

			val drawSpeed = if (debugDrawSpeed < 0) -1.0f / debugDrawSpeed else debugDrawSpeed

			debugDrawAccumulator += drawSpeed

			while (debugDrawIndex < queuedSprites && debugDrawAccumulator > 1.0f)
			{
				if (debugDraw) inDebugFrame = true

				val rs = spriteArray[debugDrawIndex++]!!

				draw(rs)

				debugDrawList.add(rs)

				debugDrawAccumulator -= 1.0f
				if (debugDrawAccumulator < 1.0f)
				{
					break
				}
			}
		}
		else
		{
			for (i in 0 until queuedSprites)
			{
				val rs = spriteArray[i]!!

				draw(rs)

				rs.free()
			}
		}

		if (!debugDraw || debugDrawIndex == queuedSprites-1)
		{
			inDebugFrame = false

			for (rs in debugDrawList) rs.free()
			debugDrawList.clear()
			debugDrawIndex = 0

			batchID = random.nextInt()
			Particle.generateBrownianVectors()

			for (entry in tilingMap)
			{
				entry.key.free()
				setPool.free(entry.value)
			}
			tilingMap.clear()

			if (queuedSprites < spriteArray.size / 4)
			{
				spriteArray = spriteArray.copyOf(spriteArray.size / 4)
			}

			queuedSprites = 0
		}

		inBegin = false

		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
	}

	// ----------------------------------------------------------------------
	fun getComparisonVal(x: Int, y: Int, layer: Int, index: Int, blend: BlendMode) : Int
	{
		if (index > MAX_INDEX-1) throw RuntimeException("Index too high! $index >= $MAX_INDEX!")
		if (layer > layers-1) throw RuntimeException("Layer too high! $index >= $layers!")

		val yBlock = MAX_Y_BLOCK_SIZE - y * Y_BLOCK_SIZE
		val xBlock = (MAX_X_BLOCK_SIZE - x * X_BLOCK_SIZE)
		val lBlock = layer * MAX_INDEX
		val iBlock = index * BLENDMODES

		return yBlock + xBlock + lBlock + iBlock + blend.ordinal
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
	fun storeRenderSprite(renderSprite: RenderSprite)
	{
		if (queuedSprites == spriteArray.size-1)
		{
			spriteArray = spriteArray.copyOf(spriteArray.size * 2)
		}

		spriteArray[queuedSprites++] = renderSprite
	}

	// ----------------------------------------------------------------------
	fun queueParticle(effect: ParticleEffect, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f)
	{
		if (!inBegin) throw Exception("Queue called before begin!")

		if (debugDraw && inDebugFrame) return

		if (effect.lockPosition)
		{

		}
		else if (effect.facing.x != 0)
		{
			effect.setPosition(ix + effect.size[1].toFloat() * 0.5f, iy + effect.size[0].toFloat() * 0.5f)
		}
		else
		{
			if (effect.isCentered)
			{
				effect.setPosition(ix + 0.5f, iy + 0.5f)
			}
			else
			{
				effect.setPosition(ix + effect.size[0].toFloat() * 0.5f, iy + effect.size[1].toFloat() * 0.5f)
			}
		}

		update(effect)

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val x = ix
		val y = iy

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
					tempVec.set(emitter.offset.valAt(0, emitter.time))
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
					var sizex = size * width
					var sizey = size * height

					if (particle.allowResize)
					{
						sizex *= emitter.size.x
						sizey *= emitter.size.y
					}

					val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

					col.mul(colour).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					val drawx = tempVec.x + px
					val drawy = tempVec.y + py

					val localx = drawx * tileSize + offsetx
					val localy = drawy * tileSize + offsety
					val localw = sizex * tileSize
					val localh = sizey * tileSize

					if (localx + localw < 0 || localx > Global.stage.width || localy + localh < 0 || localy > Global.stage.height) continue

					val comparisonVal = getComparisonVal((drawx-sizex*0.5f).toInt(), (drawy-sizey*0.5f).toInt(), layer, index, particle.blend)

					val rs = RenderSprite.obtain().set( null, null, tex1.second, null, drawx * tileSize, drawy * tileSize, tempVec.x, tempVec.y, col, sizex, sizey, rotation, 1f, 1f, effect.flipX, effect.flipY, particle.blend, comparisonVal )

					if (particle.blendKeyframes)
					{
						rs.nextTexture = tex2.second
						rs.blendAlpha = alpha
					}

					storeRenderSprite(rs)
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	fun addToMap(tilingSprite: TilingSprite, ix: Float, iy: Float)
	{
		// Add to map
		val point = Point.obtain().set(ix.toInt(), iy.toInt())
		var keys = tilingMap[point]
		if (keys == null)
		{
			keys = setPool.obtain()
			keys.clear()
		}
		keys.add(tilingSprite.checkID)
		tilingMap[point] = keys
	}

	// ----------------------------------------------------------------------
	fun queueSprite(tilingSprite: TilingSprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f)
	{
		if (!inBegin) throw Exception("Queue called before begin!")

		if (debugDraw && inDebugFrame) return

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

		// check if onscreen
		if (!alwaysOnscreen && !isSpriteOnscreen(tilingSprite, x, y, width, height)) return

		val comparisonVal = getComparisonVal(lx.toInt(), ly.toInt(), layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, tilingSprite, null, null, x, y, ix, iy, colour, width, height, 0f, 1f, 1f, false, false, BlendMode.MULTIPLICATIVE, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	fun queueSprite(sprite: Sprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f)
	{
		if (!inBegin) throw Exception("Queue called before begin!")

		if (debugDraw && inDebugFrame) return

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

		// check if onscreen
		if (!alwaysOnscreen && !isSpriteOnscreen(sprite, x, y, width, height, scaleX, scaleY)) return

		val comparisonVal = getComparisonVal(lx.toInt(), ly.toInt(), layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(sprite, null, null, null, x, y, ix, iy, colour, width, height, rotation, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	fun queueTexture(texture: TextureRegion, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, sortX: Float? = null, sortY: Float? = null)
	{
		if (!inBegin) throw Exception("Queue called before begin!")

		if (debugDraw && inDebugFrame) return

		val lx = ix - width
		val ly = iy - height

		val x = ix * tileSize
		val y = iy * tileSize

		// check if onscreen

		val localx = x + offsetx
		val localy = y + offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Global.stage.width || localy + localh < 0 || localy > Global.stage.height) return

		val comparisonVal = getComparisonVal((sortX ?: lx).toInt(), (sortY ?: ly).toInt(), layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, null, texture, null, x, y, ix, iy, colour, width, height, 0f, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, comparisonVal)

		storeRenderSprite(rs)
	}

	// ----------------------------------------------------------------------
	fun queueNinepatch(ninePatch: NinePatch, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f)
	{
		if (!inBegin) throw Exception("Queue called before begin!")

		if (debugDraw && inDebugFrame) return

		val lx = ix - width/2
		val ly = iy - height/2

		val x = ix * tileSize
		val y = iy * tileSize

		// check if onscreen

		val localx = x + offsetx
		val localy = y + offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Global.stage.width || localy + localh < 0 || localy > Global.stage.height) return

		val comparisonVal = getComparisonVal(lx.toInt(), ly.toInt(), layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, null, null, ninePatch, x, y, ix, iy, colour, width, height, 0f, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, comparisonVal)

		storeRenderSprite(rs)
	}


	// ----------------------------------------------------------------------
	fun isSpriteOnscreen(sprite: Sprite, x: Float, y: Float, width: Float, height: Float, scaleX: Float = 1f, scaleY: Float = 1f): Boolean
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
			val offset = Sprite.getPositionCorrectionOffsets(x, y, localw / 2.0f, localh / 2.0f, localw, localh, scaleX, scaleY, sprite.rotation)
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

		if (localx + localw < 0 || localx > Global.stage.width || localy + localh < 0 || localy > Global.stage.height) return false

		return true
	}

	// ----------------------------------------------------------------------
	fun isSpriteOnscreen(sprite: TilingSprite, x: Float, y: Float, width: Float, height: Float): Boolean
	{
		val localx = x + offsetx
		val localy = y + offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Global.stage.width || localy + localh < 0 || localy > Global.stage.height) return false

		return true
	}

	// ----------------------------------------------------------------------
	companion object
	{
		val random = LightRNG()
	}
}

// ----------------------------------------------------------------------
class RenderSprite(val parentBlock: RenderSpriteBlock, val parentBlockIndex: Int) : Comparable<RenderSprite>
{
	val point = Point()
	val colour: Colour = Colour(1f, 1f, 1f, 1f)
	var sprite: Sprite? = null
	var tilingSprite: TilingSprite? = null
	var texture: TextureRegion? = null
	var nextTexture: TextureRegion? = null
	var ninePatch: NinePatch? = null
	var blendAlpha = 0f
	var x: Float = 0f
	var y: Float = 0f
	var width: Float = 1f
	var height: Float = 1f
	var rotation: Float = 0f
	var scaleX: Float = 1f
	var scaleY: Float = 1f
	var flipX: Boolean = false
	var flipY: Boolean = false
	var blend: BlendMode = BlendMode.MULTIPLICATIVE

	var comparisonVal: Int = 0

	// ----------------------------------------------------------------------
	operator fun set(sprite: Sprite?, tilingSprite: TilingSprite?, texture: TextureRegion?, ninePatch: NinePatch?,
					 x: Float, y: Float,
					 ix: Float, iy: Float,
					 colour: Colour,
					 width: Float, height: Float,
					 rotation: Float,
					 scaleX: Float, scaleY: Float,
					 flipX: Boolean, flipY: Boolean,
					 blend: BlendMode,
					 comparisonVal: Int): RenderSprite
	{
		this.point.x = ix.toInt()
		this.point.y = iy.toInt()
		this.colour.r = colour.r
		this.colour.g = colour.g
		this.colour.b = colour.b
		this.colour.a = colour.a
		this.sprite = sprite
		this.tilingSprite = tilingSprite
		this.texture = texture
		this.ninePatch = ninePatch
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

		nextTexture = null

		return this
	}

	// ----------------------------------------------------------------------
	override fun compareTo(other: RenderSprite): Int
	{
		return comparisonVal.compareTo(other.comparisonVal)
	}

	// ----------------------------------------------------------------------
	fun free() = parentBlock.free(this)

	// ----------------------------------------------------------------------
	companion object
	{
		var currentBlock: RenderSpriteBlock = RenderSpriteBlock.obtain()

		fun obtain(): RenderSprite
		{
			val rs = currentBlock.obtain()

			if (currentBlock.index == RenderSpriteBlock.blockSize)
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
	var count = 0
	var index: Int = 0
	val sprites = Array(blockSize) { RenderSprite(this, it) }

	fun obtain(): RenderSprite
	{
		val sprite = sprites[index]
		index++
		count++

		return sprite
	}

	fun free(data: RenderSprite)
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

// ----------------------------------------------------------------------
class RadixSort
{
	companion object
	{
		/**
		 * The byte index of the most significant byte in each 32-bit integer.
		 */
		public const val MOST_SIGNIFICANT_BYTE_INDEX = 3

		/**
		 * The mask for manipulating the sign bit.
		 */
		private const val SIGN_BIT_MASK = -0x80000000

		/**
		 * The amount of bits per byte.
		 */
		private const val BITS_PER_BYTE = 8

		/**
		 * The mask for extracting the bucket index.
		 */
		private const val EXTRACT_BYTE_MASK = 0xff

		/**
		 * The amount of buckets considered for sorting.
		 */
		private const val BUCKET_AMOUNT = 256

		private const val QUICKSORT_THRESHOLD = 128

		public fun sort(
				source: Array<RenderSprite?>, target: Array<RenderSprite?>,
				sourceOffset: Int, targetOffset: Int, rangeLength: Int,
				byteIndex: Int)
		{
			if (rangeLength < QUICKSORT_THRESHOLD)
			{
				source.sort(sourceOffset, sourceOffset + rangeLength)

				if (byteIndex and 1 == 0)
				{
					System.arraycopy(source, sourceOffset, target, targetOffset, rangeLength)
				}

				return
			}

			val bucketSizeMap = IntArray(BUCKET_AMOUNT)

			// Count the size of each bucket.
			for (i in sourceOffset until sourceOffset + rangeLength)
			{
				bucketSizeMap[getBucketIndex(source[i]!!.comparisonVal, byteIndex)]++
			}

			// Compute the map mapping each bucket to its beginning index.
			val startIndexMap = IntArray(BUCKET_AMOUNT)

			for (i in 1 until BUCKET_AMOUNT)
			{
				startIndexMap[i] = startIndexMap[i - 1] + bucketSizeMap[i - 1]
			}

			// The map mapping each bucket index to amount of elements already put
			// in the bucket.
			val processedMap = IntArray(BUCKET_AMOUNT)

			for (i in sourceOffset until sourceOffset + rangeLength)
			{
				val element = source[i]
				val bucket = getBucketIndex(element!!.comparisonVal, byteIndex)
				target[targetOffset + startIndexMap[bucket] +
					   processedMap[bucket]++] = element
			}

			if (byteIndex > 0)
			{
				// Recursively sort the buckets.
				for (i in 0 until BUCKET_AMOUNT)
				{
					if (bucketSizeMap[i] != 0)
					{
						sort(target,
							 source,
							 targetOffset + startIndexMap[i],
							 sourceOffset + startIndexMap[i],
							 bucketSizeMap[i],
							 byteIndex - 1)
					}
				}
			}
		}

		/**
		 * Returns the bucket index for `element` when considering
		 * `byteIndex`th byte within the element. The indexing starts from
		 * the least significant bytes.
		 *
		 * @param element   the element for which to compute the bucket index.
		 * @param byteIndex the index of the byte to be considered.
		 * @return the bucket index.
		 */
		private fun getBucketIndex(element: Int, byteIndex: Int): Int
		{
			var result = element
			if (byteIndex == MOST_SIGNIFICANT_BYTE_INDEX)
			{
				result = result xor SIGN_BIT_MASK
			}
			return result.ushr(byteIndex * BITS_PER_BYTE) and EXTRACT_BYTE_MASK
		}
	}
}