package com.lyeeedar.Game

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import com.lyeeedar.Util.toCharGrid
import ktx.collections.set

class Region
{
	lateinit var nameID: String
	lateinit var grid: Array2D<RegionTile>

	fun parse(xmlData: XmlData)
	{
		nameID = xmlData.get("Name")

		val charGrid = xmlData.getChildByName("Map")!!.toCharGrid()

		val symbolMap = ObjectMap<Char, RegionSymbol>()
		val symbolsEl = xmlData.getChildByName("Symbols")!!
		for (symbolEl in symbolsEl.children)
		{
			val extends = symbolEl.get("Extends", null)
			val symbol: RegionSymbol

			if (extends != null)
			{
				symbol = symbolMap[extends[0]].copy()
			}
			else
			{
				symbol = RegionSymbol(' ', SpriteWrapper())
			}

			symbol.char = symbolEl.get("Char")[0]

			val spriteEl = symbolEl.getChildByName("Sprite")
			if (spriteEl != null)
			{
				symbol.sprite = SpriteWrapper.load(spriteEl)
			}

			symbolMap[symbol.char] = symbol
		}

		grid = Array2D(charGrid.width, charGrid.height) { x, y -> RegionTile(symbolMap[charGrid[x, y]]) }
	}

	companion object
	{
		val regions: com.badlogic.gdx.utils.Array<Region> by lazy { loadRegions() }

		fun loadRegions(): com.badlogic.gdx.utils.Array<Region>
		{
			val xml = getXml("Regions/Regions")

			val output = com.badlogic.gdx.utils.Array<Region>()

			for (el in xml.children)
			{
				val region = load(el.text)
				output.add(region)
			}

			return output
		}

		fun load(xmlData: XmlData): Region
		{
			val region = Region()
			region.parse(xmlData)

			return region
		}

		fun load(path: String): Region
		{
			val xml = getXml("Regions/$path")
			return load(xml)
		}
	}
}

class RegionTile(val symbol: RegionSymbol)
{
	val sprite: SpriteWrapper = symbol.sprite.copy()
}

class RegionSymbol(var char: Char, var sprite: SpriteWrapper)
{
	fun copy(): RegionSymbol
	{
		return RegionSymbol(char, sprite)
	}
}