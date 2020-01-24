package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Tile
import com.lyeeedar.Board.isMonster
import com.lyeeedar.Components.*

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
		SPREADER,
		NAMEDTILE
	}

	lateinit var isValid: (tile: Tile, data: ObjectMap<String, Any>) -> Boolean

	init
	{
		isValid = when(type)
		{
			Type.BASICORB -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.isBasicOrb() == true && !tile.contents!!.swappable()!!.sealed && tile.spreader == null

			Type.ORB -> fun(tile: Tile, data: ObjectMap<String, Any>) = (tile.contents?.matchable() != null || tile.contents?.special() != null) && tile.spreader == null

			Type.SPECIAL -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.special() != null && tile.spreader == null

			Type.BLOCK -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.damageable() != null && tile.contents?.ai() == null && tile.spreader == null

			Type.EMPTY -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents == null && tile.canHaveOrb && tile.spreader == null

			Type.SEALED -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.swappable()?.sealed == true && tile.spreader == null

			Type.MONSTER ->  fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.isMonster() == true && tile.spreader == null

			Type.ATTACK ->  fun(tile: Tile, data: ObjectMap<String, Any>) = tile.contents?.monsterEffect() != null && tile.spreader == null

			Type.TILE -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.canHaveOrb

			Type.SPREADER ->  fun(tile: Tile, data: ObjectMap<String, Any>) = tile.spreader?.nameKey == data["SPREADERNAME"]

			Type.NAMEDTILE -> fun(tile: Tile, data: ObjectMap<String, Any>) = tile.nameKey == data["TILENAME"]

			else -> throw Exception("Invalid targetter type $type")
		}
	}
}