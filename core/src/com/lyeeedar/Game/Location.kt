package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Location
{
	lateinit var name: String
	lateinit var exclusivityKey: String
	lateinit var image: Texture
	lateinit var position: Point
	lateinit var theme: Theme

	companion object
	{
		fun load(path: String): Location
		{
			val xml = getXml("Locations/$path")

			val location = Location()

			location.name = xml.get("Name")
			location.exclusivityKey = xml.get("ExclusivityKey")
			if (location.exclusivityKey.isBlank()) location.exclusivityKey = location.name

			location.image = AssetManager.loadTexture("Sprites/" + xml.getChildByName("Background")!!.get("File"))!!

			val posSplit = xml.get("Position").split(",")
			location.position = Point(posSplit[0].toInt(), posSplit[1].toInt())

			location.theme = Theme.load(xml.getChildByName("Theme")!!)

			return location
		}
	}
}

class Theme
{
	lateinit var floor: SpriteWrapper
	lateinit var wall: SpriteWrapper
	lateinit var pit: SpriteWrapper

	lateinit var chestFull: Sprite
	lateinit var chestEmpty: Sprite
	lateinit var coin: Sprite
	lateinit var plate: Sprite
	val blockSprites = Array<Sprite>()
	val sealSprites = Array<Sprite>()
	val shieldSprites = Array<Sprite>()

	val spawnList = Array<String>()

	companion object
	{
		fun load(path: String): Theme
		{
			val xml = getXml(path)
			return load(xml)
		}

		fun load(xml: XmlData): Theme
		{
			val theme = Theme()

			theme.floor = SpriteWrapper.load(xml.getChildByName("Floor")!!)
			theme.wall = SpriteWrapper.load(xml.getChildByName("Wall")!!)
			theme.pit = SpriteWrapper.load(xml.getChildByName("Pit")!!)

			val chestEl = xml.getChildByName("Chest")!!
			theme.chestFull = AssetManager.loadSprite(chestEl.getChildByName("Full")!!)
			theme.chestEmpty = AssetManager.loadSprite(chestEl.getChildByName("Empty")!!)
			theme.coin = AssetManager.loadSprite(xml.getChildByName("Coin")!!)
			theme.plate = AssetManager.loadSprite(xml.getChildByName("Plate")!!)

			val blockEls = xml.getChildByName("Block")!!
			for (i in 0 until blockEls.childCount)
			{
				theme.blockSprites.add(AssetManager.loadSprite(blockEls.getChild(i)))
			}

			val sealEls = xml.getChildByName("Seal")!!
			for (i in 0 until sealEls.childCount)
			{
				theme.sealSprites.add(AssetManager.loadSprite(sealEls.getChild(i)))
			}

			val shieldEls = xml.getChildByName("Shield")!!
			for (i in 0 until shieldEls.childCount)
			{
				theme.shieldSprites.add(AssetManager.loadSprite(shieldEls.getChild(i)))
			}

			val spawnsEl = xml.getChildByName("SpawnWeights")
			if (spawnsEl != null)
			{
				for (el in spawnsEl.children)
				{
					val split = el.text.split(",")

					for (i in 0 until split[1].toInt())
					{
						theme.spawnList.add(split[0])
					}
				}
			}
			else
			{
				theme.spawnList.add("Orb")
			}

			return theme
		}
	}
}