package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.XmlData

class Symbol(var char: Char) : IPathfindingTile
{
	var content: XmlData? = null
	var placerHashCode: Int = -1
	var locked = false

	fun write(data: Symbol, overwrite: Boolean = false)
	{
		content = data.content ?: content
		char = data.char
	}

	fun copy(): Symbol
	{
		val symbol = Symbol(char)
		symbol.write(this)
		return symbol
	}

	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		return true
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?): Int
	{
		return 0
	}

	companion object
	{
		fun load(xmlData: XmlData, symbolTable: ObjectMap<Char, Symbol>) : Symbol
		{
			val char = xmlData.get("Character")[0]
			val extends = xmlData.get("Extends", "")?.firstOrNull()

			val symbol = if (extends != null) symbolTable[extends].copy() else Symbol(char)
			symbol.char = char

			symbol.content = xmlData.getChildByName("Content") ?: symbol.content

			return symbol
		}
	}
}