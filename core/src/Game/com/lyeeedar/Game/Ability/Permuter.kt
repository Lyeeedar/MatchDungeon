package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.matchable
import com.lyeeedar.Direction
import com.lyeeedar.Util.DataValue
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.random

enum class PermuterType
{
	SINGLE,
	ALLOFTYPE,
	NOFTYPE,
	COLUMN,
	ROW,
	CROSS,
	BLOCK,
	DIAMOND,
	RANDOM,
	CONE
}

class Permuter : XmlDataClass()
{
	lateinit var type: PermuterType

	@DataValue(visibleIf = "Type == NOFTYPE || Type == RANDOM")
	var count: Int = 1

	@DataValue(visibleIf = "Type == COLUMN || Type == ROW || Type == CROSS || Type == CONE")
	var range: Int = 99999

	@DataValue(visibleIf = "Type == BLOCK || Type == DIAMOND")
	var aoe: Int = 1

	override fun load(xmlData: XmlData) {

	}
}

fun Permuter.permute(tile: Tile, grid: Grid, selectedTargets: Array<Tile>, ability: Ability?, source: Tile?): Sequence<Tile>
{
	return when(type)
	{
		PermuterType.SINGLE -> sequenceOf(tile)

		PermuterType.ALLOFTYPE -> {
			return grid.grid.filter {
				val key = tile.contents!!.matchable()!!.desc.key
				it.contents?.matchable()?.desc?.key == key
			}
		}

		PermuterType.NOFTYPE -> {
			val key = tile.contents!!.matchable()!!.desc.key
			return grid.grid.filter{ it.contents?.matchable()?.desc?.key == key }.random(count, grid.ran)
		}

		PermuterType.COLUMN -> {
			return grid.grid.filter{ it.x == tile.x && it.dist(tile) <= range }
		}

		PermuterType.ROW -> {
			return grid.grid.filter{ it.y == tile.y && it.dist(tile) <= range }
		}

		PermuterType.CROSS -> {
			return grid.grid.filter{ (it.y == tile.y || it.x == tile.x) && it.dist(tile) <= range }
		}

		PermuterType.BLOCK -> {
			return grid.grid.filter{ it.taxiDist(tile) <= aoe }
		}

		PermuterType.DIAMOND -> {
			return grid.grid.filter{ it.dist(tile) <= aoe }
		}

		PermuterType.RANDOM -> {
			if (ability != null && selectedTargets.size > 0 && ability.effect.type == Effect.Type.CONVERT)
			{
				val selectedType = selectedTargets[0].contents!!.matchable()!!.desc
				return grid.grid.filter{ it.contents?.matchable() != null && it.contents!!.matchable()!!.desc != selectedType }.random(count, grid.ran)
			}

			return grid.grid.filter{ ability?.targetter?.isValid(it) ?: it.canHaveOrb }.random(count, grid.ran)
		}

		PermuterType.CONE -> {
			val dir = Direction.getCardinalDirection(source!!, tile)
			val cone = Direction.buildCone(dir, source, range)

			return cone.mapNotNull { if (grid.grid.inBounds(it)) grid.grid.get(it) else null }.asSequence()
		}

		else -> throw Exception("Invalid permuter type $type")
	}
}