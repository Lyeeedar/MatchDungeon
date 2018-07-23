package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Match5
import com.lyeeedar.Board.Tile

/**
 * Created by Philip on 21-Jul-16.
 */

class Targetter(val type: Type)
{
	enum class Type
	{
		BASICORB,
		ORB,
		SPECIAL,
		BLOCK,
		EMPTY,
		SEALED,
		MONSTER,
		ATTACK,
		TILE,
		NAMEDTILE
	}

	lateinit var isValid: (tile: Tile, data: ObjectMap<String, String>) -> Boolean

	init
	{
		isValid = when(type)
		{
			Type.BASICORB -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.orb != null && !tile.orb!!.hasAttack && tile.orb!!.special == null && !tile.orb!!.sealed && tile.spreader == null
			Type.ORB -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.orb != null && !tile.orb!!.hasAttack && tile.orb?.special !is Match5 && tile.spreader == null
			Type.SPECIAL -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.orb?.special != null && tile.spreader == null
			Type.BLOCK -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.block != null && tile.spreader == null
			Type.EMPTY -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.contents == null && tile.canHaveOrb && tile.spreader == null
			Type.SEALED -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.swappable?.sealed ?: false && tile.spreader == null
			Type.MONSTER ->  fun(tile: Tile, data: ObjectMap<String, String>) = tile.monster != null && tile.spreader == null
			Type.ATTACK ->  fun(tile: Tile, data: ObjectMap<String, String>) = tile.orb?.hasAttack ?: false && tile.spreader == null
			Type.TILE -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.canHaveOrb
			Type.NAMEDTILE -> fun(tile: Tile, data: ObjectMap<String, String>) = tile.nameKey == data["TILENAME"]
			else -> throw Exception("Invalid targetter type $type")
		}
	}

	fun popAction(): String
	{
		return when (type)
		{
			Type.BASICORB -> "pop"
			Type.ORB -> "pop"
			Type.SPECIAL -> "pop"
			Type.BLOCK -> "break"
			Type.EMPTY -> "pop"
			Type.SEALED -> "break"
			Type.MONSTER -> "strike"
			Type.ATTACK -> "block"
			Type.TILE -> "pop"
			Type.NAMEDTILE -> "pop"
		}
	}
}