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

	lateinit var permute: (tile: Tile, grid: Grid, data: ObjectMap<String, Any>) -> Sequence<Tile>

	init
	{
		permute = when(type)
		{
			Type.SINGLE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>) = sequenceOf(tile)
			Type.ALLOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>) = grid.grid.filter{ val key = data["TYPE"]?.hashCode() ?: tile.orb!!.key; it.orb?.key == key }
			Type.NOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>): Sequence<Tile> { val type = data["TYPE"].hashCode(); val count = data["COUNT"].toString().toInt(); return grid.grid.filter{ it.orb?.key == type }.random(count) }
			Type.COLUMN ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>) = grid.grid.filter{ it.x == tile.x }
			Type.ROW ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>) = grid.grid.filter{ it.y == tile.y }
			Type.CROSS ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>) = grid.grid.filter{ it.y == tile.y || it.x == tile.x }
			Type.BLOCK ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>): Sequence<Tile> { val dst = data["AOE"].toString().toInt(); return grid.grid.filter{ it.taxiDist(tile) <= dst } }
			Type.DIAMOND ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>): Sequence<Tile> { val dst = data["AOE"].toString().toInt(); return grid.grid.filter{ it.dist(tile) <= dst } }
			Type.RANDOM ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>): Sequence<Tile> { val count = data["COUNT"].toString().toInt(); return grid.grid.filter{ it.canHaveOrb }.random(count) }
			else -> throw Exception("Invalid permuter type $type")
		}
	}

	fun toString(data: ObjectMap<String, Any>): String
	{
		return when (type)
		{
			Type.SINGLE -> "single"
			Type.ALLOFTYPE -> if (data["TYPE"] != null) "select all orbs of type " + data["TYPE"] else "select all orbs of chosen type"
			Type.NOFTYPE -> { val count = data["COUNT"].toString().toInt(); if (data["TYPE"] != null) "select $count orbs of type " + data["TYPE"] else "select $count orbs of chosen type" }
			Type.COLUMN -> "select entire column"
			Type.ROW -> "select entire row"
			Type.CROSS -> "select entire row and column"
			Type.BLOCK -> { val dst = data["AOE"].toString().toInt(); "select block of size $dst" }
			Type.DIAMOND ->  { val dst = data["AOE"].toString().toInt(); "select diamond of size $dst" }
			Type.RANDOM -> { val count = data["COUNT"].toString().toInt(); "select $count randomly" }
		}
	}
}