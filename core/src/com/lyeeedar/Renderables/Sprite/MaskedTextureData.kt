package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

class MaskedTextureData()
{
	constructor(xmlData: XmlData) : this()
	{
		parse(xmlData)
	}

	lateinit var base: TextureRegion
	lateinit var glow: TextureRegion
	lateinit var mask: TextureRegion
	lateinit var layer1: TextureRegion
	lateinit var layer2: TextureRegion
	lateinit var layer3: TextureRegion

	fun parse(xmlData: XmlData)
	{
		base = AssetManager.loadTextureRegion(xmlData.get("Base"))!!
		glow = AssetManager.loadTextureRegion(xmlData.get("Glow"))!!
		mask = AssetManager.loadTextureRegion(xmlData.get("Mask"))!!
		layer1 = AssetManager.loadTextureRegion(xmlData.get("Layer1"))!!
		layer2 = AssetManager.loadTextureRegion(xmlData.get("Layer2"))!!
		layer3 = AssetManager.loadTextureRegion(xmlData.get("Layer3"))!!
	}
}