package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.Array
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

	lateinit var permute: (tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?) -> Sequence<Tile>

	init
	{
		permute = when(type)
		{
			Type.SINGLE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?) = sequenceOf(tile)
			Type.ALLOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?) = grid.grid.filter{ val key = data["TYPE"]?.hashCode() ?: tile.orb!!.desc.key; it.orb?.desc?.key == key }
			Type.NOFTYPE -> fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> { val type = data["TYPE"].hashCode(); val count = data["COUNT"].toString().toInt(); return grid.grid.filter{ it.orb?.desc?.key == type }.random(count) }

			Type.COLUMN ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> {
				val range = data["RANGE", "99999"]!!.toString().toInt()
				return grid.grid.filter{ it.x == tile.x && it.dist(tile) <= range }
			}

			Type.ROW ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> {
				val range = data["RANGE", "99999"]!!.toString().toInt()
				return grid.grid.filter{ it.y == tile.y && it.dist(tile) <= range }
			}

			Type.CROSS ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> {
				val range = data["RANGE", "99999"]!!.toString().toInt()
				return grid.grid.filter{ (it.y == tile.y || it.x == tile.x) && it.dist(tile) <= range }
			}

			Type.BLOCK ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> { val dst = data["AOE"].toString().toInt(); return grid.grid.filter{ it.taxiDist(tile) <= dst } }
			Type.DIAMOND ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> { val dst = data["AOE"].toString().toInt(); return grid.grid.filter{ it.dist(tile) <= dst } }

			Type.RANDOM ->  fun(tile: Tile, grid: Grid, data: ObjectMap<String, Any>, selectedTargets: Array<Tile>, ability: Ability?): Sequence<Tile> {
				val count = data["COUNT"].toString().toInt()

				if (ability != null && selectedTargets.size > 0 && ability.effect.type == Effect.Type.CONVERT)
				{
					val selectedType = selectedTargets[0].orb!!.desc
					return grid.grid.filter{ it.orb != null && it.orb!!.desc != selectedType }.random(count)
				}

				return grid.grid.filter{ it.canHaveOrb }.random(count)
			}

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
			Type.COLUMN -> {
				val range = data["RANGE", null]
				if (range != null)
				{
					"select all tiles in column within $range"
				}
				else
				{
					"select entire column"
				}
			}
			Type.ROW -> {
				val range = data["RANGE", null]
				if (range != null)
				{
					"select all tiles in row within $range"
				}
				else
				{
					"select entire row"
				}
			}
			Type.CROSS -> {
				val range = data["RANGE", null]
				if (range != null)
				{
					"select all tiles in row and column within $range"
				}
				else
				{
					"select entire row and column"
				}
			}
			Type.BLOCK -> { val dst = data["AOE"].toString().toInt(); "select block of size $dst" }
			Type.DIAMOND ->  { val dst = data["AOE"].toString().toInt(); "select diamond of size $dst" }
			Type.RANDOM -> { val count = data["COUNT"].toString().toInt(); "select $count randomly" }
		}
	}
}