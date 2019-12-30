package com.lyeeedar.Util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.BlendMode
import com.lyeeedar.Renderables.Animation.AbstractAnimation
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.LightAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Particle.TextureOverride
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import java.util.*

class AssetManager
{
	companion object
	{
		private val loadedFonts = HashMap<String, BitmapFont>()

		@JvmOverloads fun loadFont(name: String, size: Int, colour: Color = Color.WHITE, borderWidth: Int = 1, borderColour: Color = Color.BLACK, shadow: Boolean = false): BitmapFont?
		{
			val key = name + size + colour.toString() + borderWidth + borderColour.toString()

			if (loadedFonts.containsKey(key))
			{
				return loadedFonts[key]
			}

			val fgenerator = FreeTypeFontGenerator(Gdx.files.internal(name))
			val parameter = FreeTypeFontParameter()
			parameter.size = size
			parameter.borderWidth = borderWidth.toFloat()
			parameter.kerning = true
			parameter.borderColor = borderColour
			parameter.borderStraight = true
			parameter.color = colour

			if (shadow)
			{
				parameter.shadowOffsetX = -1
				parameter.shadowOffsetY = 1
			}

			val font = fgenerator.generateFont(parameter)
			font.data.markupEnabled = true
			fgenerator.dispose() // don't forget to dispose to avoid memory leaks!

			loadedFonts.put(key, font)

			return font
		}

		private val loadedSounds = HashMap<String, Sound?>()

		fun loadSound(path: String): Sound?
		{
			if (loadedSounds.containsKey(path))
			{
				return loadedSounds[path]
			}

			var file = Gdx.files.internal("Sounds/$path.mp3")
			if (!file.exists())
			{
				file = Gdx.files.internal("Sounds/$path.ogg")

				if (!file.exists())
				{
					loadedSounds.put(path, null)
					return null
				}
			}

			val sound = Gdx.audio.newSound(file)

			loadedSounds.put(path, sound)

			return sound
		}

		private val prepackedAtlas = TextureAtlas(Gdx.files.internal("CompressedData/SpriteAtlas.atlas")!!)

		private val loadedTextureRegions = HashMap<String, TextureRegion?>()

		@JvmStatic fun loadTextureRegion(path: String): TextureRegion?
		{
			if (loadedTextureRegions.containsKey(path))
			{
				return loadedTextureRegions[path]
			}

			var atlasName = path
			if (atlasName.startsWith("Sprites/")) atlasName = atlasName.replaceFirst("Sprites/".toRegex(), "")
			atlasName = atlasName.replace(".png", "")

			val region = prepackedAtlas.findRegion(atlasName)
			if (region != null)
			{
				val textureRegion = TextureRegion(region)
				loadedTextureRegions.put(path, textureRegion)
				return textureRegion
			}
			else
			{
				loadedTextureRegions.put(path, null)
				return null
			}
		}

		private val loadedTextures = HashMap<String, Texture?>()

		fun loadTexture(path: String, filter: TextureFilter = TextureFilter.Linear, wrapping: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge): Texture?
		{
			var rawpath = path
			if (!rawpath.endsWith(".png")) rawpath += ".png"

			val path = "CompressedData/" + rawpath.hashCode() + ".ktx"

			if (loadedTextures.containsKey(path))
			{
				return loadedTextures[path]
			}

			val file = Gdx.files.internal(path)
			if (!file.exists())
			{
				loadedTextures.put(path, null)
				return null
			}

			val region = Texture(path)
			region.setFilter(filter, filter)
			region.setWrap(wrapping, wrapping)
			loadedTextures.put(path, region)

			return region
		}

		fun loadParticleEffect(name: String, colour: Colour = Colour.WHITE, flipX: Boolean = false, flipY: Boolean = false, scale: Float = 1f, useFacing: Boolean = true, timeMultiplier: Float = 1f, killOnAnimComplete: Boolean = false): ParticleEffectDescription
		{
			val effect = ParticleEffectDescription(name)
			effect.colour.set(colour)
			effect.flipX = flipX
			effect.flipY = flipY
			effect.scale = scale
			effect.useFacing = useFacing
			effect.timeMultiplier = timeMultiplier
			effect.killOnAnimComplete = killOnAnimComplete
			return effect
		}

		fun loadParticleEffect(xml:XmlData): ParticleEffectDescription
		{
			val effectXml: XmlData
			val overridesEl: XmlData?
			if (xml.getChildByName("Name") == null)
			{
				// its a template
				effectXml = xml.getChildByName("Base")!!
				overridesEl = xml.getChildByName("Overrides")
			}
			else
			{
				effectXml = xml
				overridesEl = null
			}

			val effect = ParticleEffectDescription(effectXml.get("Name"))

			val colourElement = effectXml.getChildByName("Colour")
			var colour = Colour(1f, 1f, 1f, 1f)
			if (colourElement != null)
			{
				colour = loadColour(colourElement)
			}

			effect.colour.set(colour)

			effect.flipX = effectXml.getBoolean("FlipX", false)
			effect.flipY = effectXml.getBoolean("FlipY", false)
			effect.scale = effectXml.getFloat("Scale", 1f)
			effect.useFacing = effectXml.getBoolean("UseFacing", true)
			effect.timeMultiplier = effectXml.getFloat("TimeMultiplier", 1f)
			effect.killOnAnimComplete = effectXml.getBoolean("KillOnAnimComplete", false)

			if (overridesEl != null)
			{
				for (overrideEl in overridesEl.children)
				{
					val texName = overrideEl.get("Name")
					val overrideName = overrideEl.getChildByName("Texture")!!.get("File")
					val blendModeStr = overrideEl.get("BlendMode", "Current")!!
					val blendMode = if (blendModeStr != "Current") BlendMode.valueOf(blendModeStr.toUpperCase()) else null

					effect.textureOverrides.add(TextureOverride(texName, overrideName, blendMode))
				}
			}

			return effect
		}

		fun loadSprite(name: String, drawActualSize: Boolean): Sprite
		{
			return loadSprite(name, 0.5f, Colour(1f, 1f, 1f, 1f), drawActualSize)
		}

		fun loadSprite(name: String, updateTime: Float, reverse: Boolean): Sprite
		{
			return loadSprite(name, updateTime, Colour(1f, 1f, 1f, 1f), false, reverse)
		}

		@JvmOverloads fun loadSprite(name: String, updateTime: Float = 0.5f, colour: Colour = Colour(1f, 1f, 1f, 1f), drawActualSize: Boolean = false, reverse: Boolean = false, light: Light? = null): Sprite
		{
			var updateTime = updateTime
			val textures = Array<TextureRegion>(false, 1, TextureRegion::class.java)

			// Try 0 indexed sprite
			var i = 0
			while (true)
			{
				val tex = loadTextureRegion("Sprites/" + name + "_" + i + ".png")

				if (tex == null)
				{
					break
				} else
				{
					textures.add(tex)
				}

				i++
			}

			// Try 1 indexed sprite
			if (textures.size == 0)
			{
				i = 1
				while (true)
				{
					val tex = loadTextureRegion("Sprites/" + name + "_" + i + ".png")

					if (tex == null)
					{
						break
					} else
					{
						textures.add(tex)
					}

					i++
				}
			}

			// Try sprite without indexes
			if (textures.size == 0)
			{
				val tex = loadTextureRegion("Sprites/$name.png")

				if (tex != null)
				{
					textures.add(tex)
				}
			}

			if (textures.size == 0)
			{
				throw RuntimeException("Cant find any textures for $name!")
			}

			if (reverse)
			{
				textures.reverse()
			}

			if (updateTime <= 0)
			{
				updateTime = 0.5f
			}

			val sprite = Sprite(name, updateTime, textures, colour, drawActualSize)
			sprite.light = light

			return sprite
		}

		fun tryLoadSpriteWithResources(xml:XmlData, resources: ObjectMap<String,XmlData>): Sprite
		{
			if (xml.childCount == 0) return loadSprite(resources[xml.text])
			else return loadSprite(xml)
		}

		fun tryLoadSprite(xml:XmlData?): Sprite?
		{
			if (xml == null) return null
			else if (xml.childCount == 0) return null
			else return loadSprite(xml)
		}

		fun loadSprite(xml:XmlData): Sprite
		{
			val colourElement = xml.getChildByName("Colour")
			var colour = Colour(1f, 1f, 1f, 1f)
			if (colourElement != null)
			{
				colour = loadColour(colourElement)
			}

			val sprite = loadSprite(
					xml.get("Name"),
					xml.getFloat("UpdateRate", 0f),
					colour,
					xml.getBoolean("DrawActualSize", false))

			sprite.repeatDelay = xml.getFloat("RepeatDelay", 0f)
			sprite.frameBlend = xml.getBoolean("Blend", false)

			sprite.disableHDR = xml.getBoolean("DisableHDR", false)

			if (xml.getBoolean("RandomStart", false))
			{
				sprite.texIndex = Random.random(sprite.textures.size)
				sprite.animationAccumulator = Random.random(sprite.animationDelay)
			}

			val animationElement = xml.getChildByName("Animation")
			if (animationElement != null)
			{
				sprite.animation = AbstractAnimation.load(animationElement.getChild(0))
			}

			val lightEl = xml.getChildByName("Light")
			if (lightEl != null)
			{
				sprite.light = loadLight(lightEl)
			}

			return sprite
		}

		fun loadSprite(xml:XmlData, texture: TextureRegion): Sprite
		{
			val colourElement = xml.getChildByName("Colour")
			var colour = Colour(1f, 1f, 1f, 1f)
			if (colourElement != null)
			{
				colour = loadColour(colourElement)
			}

			val textures = Array<TextureRegion>(false, 1, TextureRegion::class.java)
			textures.add(texture)

			var updateTime = xml.getFloat("UpdateRate", 0f)

			if (updateTime <= 0)
			{
				updateTime = 0.5f
			}

			val sprite = Sprite(xml.get("Name", "")!!,
					updateTime,
					textures,
					colour,
					xml.getBoolean("DrawActualSize", false))

			sprite.repeatDelay = xml.getFloat("RepeatDelay", 0f)
			sprite.disableHDR = xml.getBoolean("DisableHDR", false)

			val animationElement = xml.getChildByName("Animation")
			if (animationElement != null)
			{
				sprite.animation = AbstractAnimation.load(animationElement.getChild(0))
			}

			val lightEl = xml.getChildByName("Light")
			if (lightEl != null)
			{
				sprite.light = loadLight(lightEl)
			}

			return sprite
		}

		fun loadLight(xml: XmlData): Light
		{
			val light = Light()
			light.colour.set(loadColour(xml.getChildByName("Colour")!!))
			light.baseColour.set(light.colour)
			val brightness = xml.getFloat("Brightness")
			light.baseBrightness = brightness
			light.colour.mul(brightness, brightness, brightness, 1.0f)
			light.range = xml.getFloat("Range")
			light.baseRange = light.range
			light.hasShadows = xml.getBoolean("HasShadows", false)

			val animEl = xml.getChildByName("Animation")
			if (animEl != null)
			{
				light.anim = LightAnimation.load(animEl)
			}

			return light
		}

		fun loadColour(stringCol: String, colour: Colour = Colour()): Colour
		{
			val cols = stringCol.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			colour.r = java.lang.Float.parseFloat(cols[0]) / 255.0f
			colour.g = java.lang.Float.parseFloat(cols[1]) / 255.0f
			colour.b = java.lang.Float.parseFloat(cols[2]) / 255.0f
			colour.a = if (cols.size > 3) cols[3].toFloat() / 255.0f else 1f

			return colour
		}

		fun loadColour(xml:XmlData): Colour
		{
			return loadColour(xml.text)
		}

		fun loadTilingSprite(xml:XmlData): TilingSprite
		{
			return TilingSprite.load(xml)
		}

		fun loadLayeredSprite(xml: XmlData): Sprite
		{
			val paths = Array<String>(1)
			var drawActualSize = false

			val layers = xml.getChildByName("Layers")!!
			for (layer in layers.children)
			{
				val name = layer.get("Name")
				drawActualSize = drawActualSize || layer.getBoolean("DrawActualSize", false)

				paths.add(name)
			}

			val mergedName = paths.joinToString("+")
			val tex = loadTextureRegion("Sprites/$mergedName.png")
					  ?: throw RuntimeException("Cant find any textures for layered sprite $mergedName!")

			val sprite = Sprite(tex)
			sprite.drawActualSize = drawActualSize

			val lightEl = xml.getChildByName("Light")
			if (lightEl != null)
			{
				sprite.light = loadLight(lightEl)
			}

			return sprite
		}

		fun loadDirectionalSprite(xml:XmlData, size: Int = 1): DirectionalSprite
		{
			val directionalSprite = DirectionalSprite()

			val anims = xml.getChildByName("Animations")!!
			for (i in 0 until anims.childCount)
			{
				val el = anims.getChild(i)
				val name = el.get("Name").toLowerCase()
				val up = AssetManager.loadSprite(el.getChildByName("Up")!!)
				val down = AssetManager.loadSprite(el.getChildByName("Down")!!)

				up.size[0] = size
				up.size[1] = size

				down.size[0] = size
				down.size[1] = size

				directionalSprite.addAnim(name, up, down)
			}

			return directionalSprite
		}

		fun loadRenderable(xml:XmlData): Renderable
		{
			val type = xml.getAttribute("meta:RefKey", null)?.toUpperCase() ?: xml.name.toUpperCase()

			return when(type)
			{
				"SPRITE" -> AssetManager.loadSprite(xml)
				"PARTICLEEFFECT", "PARTICLE", "PARTICLEEFFECTTEMPLATE" -> AssetManager.loadParticleEffect(xml).getParticleEffect()
				"TILINGSPRITE" -> AssetManager.loadTilingSprite(xml)
				else -> throw Exception("Unknown renderable type '$type'!")
			};
		}
	}
}