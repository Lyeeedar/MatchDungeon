package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Tile
import com.lyeeedar.Util.random

/**
 * Created by Philip on 21-Jul-16.
 */

class Permuter(val type: Type)
{
	enum class Type
	{
		SINGLE,
		ALLOFTYPE,
		NOFTYPE,
		COLUMN,
		ROW,
		CROSS,
		BLOCK,
		DIAMOND,
		RANDOM
	}

	lateinit var permute: (tile: Tile, grid: Grid, data: ObjectMap<String, String>) -> Sequence<Tile>

	init
	{
		permute = when(type)
		{
			Type.SINGLE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>) = sequenceOf(tile)
			Type.ALLOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>) = grid.grid.filter{ val key = data["TYPE"]?.hashCode() ?: tile.orb!!.key; it.orb?.key == key }
			Type.NOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>): Sequence<Tile> { val type = data["TYPE"].hashCode(); val count = data["COUNT"].toInt(); return grid.grid.filter{ it.orb?.key == type }.random(count) }
			Type.COLUMN ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>) = grid.grid.filter{ it.x == tile.x }
			Type.ROW ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>) = grid.grid.filter{ it.y == tile.y }
			Type.CROSS ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>) = grid.grid.filter{ it.y == tile.y || it.x == tile.x }
			Type.BLOCK ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>): Sequence<Tile> { val dst = data["AOE"].toInt(); return grid.grid.filter{ it.taxiDist(tile) <= dst } }
			Type.DIAMOND ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>): Sequence<Tile> { val dst = data["AOE"].toInt(); return grid.grid.filter{ it.dist(tile) <= dst } }
			Type.RANDOM ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, String>): Sequence<Tile> { val count = data["COUNT"].toInt(); return grid.grid.filter{ it.canHaveOrb }.random(count) }
			else -> throw Exception("Invalid permuter type $type")
		}
	}
}