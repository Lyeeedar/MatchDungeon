package com.lyeeedar.Renderables.Sprite

import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 06-Jul-16.
 */

class SpriteWrapper
{
	var sprite: Sprite? = null
	var tilingSprite: TilingSprite? = null

	fun copy(): SpriteWrapper
	{
		val wrapper = SpriteWrapper()
		wrapper.sprite = sprite?.copy()
		wrapper.tilingSprite = tilingSprite?.copy()
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

			return wrapper
		}
	}
}