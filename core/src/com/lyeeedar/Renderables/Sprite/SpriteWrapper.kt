package com.lyeeedar.Renderables.Sprite

import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 06-Jul-16.
 */

class SpriteWrapper
{
	var sprite: Sprite? = null
	var tilingSprite: TilingSprite? = null

	val spriteVariants = com.badlogic.gdx.utils.Array<Pair<Float, Sprite>>()
	val tilingSpriteVariants = com.badlogic.gdx.utils.Array<Pair<Float, TilingSprite>>()

	var chosenSprite: Sprite? = null
	var chosenTilingSprite: TilingSprite? = null
	var hasChosenSprites = false

	fun chooseSprites()
	{
		chosenSprite = sprite
		if (spriteVariants.size > 0)
		{
			val rand = Random.random()
			var total = 0f
			for (variant in spriteVariants)
			{
				total += variant.first
				if (rand <= total)
				{
					chosenSprite = variant.second
					break
				}
			}
		}

		chosenTilingSprite = tilingSprite
		if (tilingSpriteVariants.size > 0)
		{
			val rand = Random.random()
			var total = 0f
			for (variant in tilingSpriteVariants)
			{
				total += variant.first
				if (rand <= total)
				{
					chosenTilingSprite = variant.second
					break
				}
			}
		}

		hasChosenSprites = true
	}

	fun copy(): SpriteWrapper
	{
		val wrapper = SpriteWrapper()
		wrapper.sprite = sprite?.copy()
		wrapper.tilingSprite = tilingSprite?.copy()
		wrapper.spriteVariants.addAll(spriteVariants)
		wrapper.tilingSpriteVariants.addAll(tilingSpriteVariants)
		return wrapper
	}

	companion object
	{
		fun load(xml: XmlData): SpriteWrapper
		{
			var spriteEl = xml.getChildByName("Sprite")
			var tilingEl = xml.getChildByName("TilingSprite")

			if (spriteEl == null && tilingEl == null)
			{
				if (xml.name == "Sprite") spriteEl = xml
				if (xml.name == "TilingSprite") tilingEl = xml
			}

			val wrapper = SpriteWrapper()
			if (spriteEl != null) wrapper.sprite = AssetManager.loadSprite(spriteEl)
			if (tilingEl != null) wrapper.tilingSprite = AssetManager.loadTilingSprite(tilingEl)

			val spriteVariantsEl = xml.getChildByName("SpriteVariants")
			if (spriteVariantsEl != null)
			{
				for (el in spriteVariantsEl.children)
				{
					val sprite = AssetManager.loadSprite(el.getChildByName("Sprite")!!)
					val weight = el.getFloat("Chance")

					wrapper.spriteVariants.add(Pair(weight, sprite))
				}
			}

			val tilingSpriteVariantsEl = xml.getChildByName("TilingSpriteVariants")
			if (tilingSpriteVariantsEl != null)
			{
				for (el in tilingSpriteVariantsEl.children)
				{
					val sprite = AssetManager.loadTilingSprite(el.getChildByName("TilingSprite")!!)
					val weight = el.getFloat("Chance")

					wrapper.tilingSpriteVariants.add(Pair(weight, sprite))
				}
			}

			return wrapper
		}
	}
}