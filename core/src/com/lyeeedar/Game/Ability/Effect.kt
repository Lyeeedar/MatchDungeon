package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.*
import com.lyeeedar.Global
import com.lyeeedar.Statistic
import com.lyeeedar.Util.filename

/**
 * Created by Philip on 21-Jul-16.
 */

class Effect(val type: Type)
{
	enum class Type
	{
		POP,
		CONVERT,
		SUMMON,
		SPREADER,
		SUPERCHARGE,
		TEST
	}

	lateinit var apply: (tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>) -> Unit

	init
	{
		apply = when(type)
		{
			Type.POP -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>) { grid.pop(tile, delay, damSource = this, bonusDam = data["DAMAGE", "0"].toString().toInt() + grid.level.player.getStat(Statistic.ABILITYDAMAGE), pierce = grid.level.player.getStat(Statistic.PIERCE), skipPowerOrb = true) }

			Type.CONVERT -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>)
			{
				val orb = tile.orb ?: return

				val convertTo = data["CONVERTTO"]?.toString() ?: "Random"
				tile.orb = when(convertTo)
				{
					"RANDOM" -> Orb(Orb.getRandomOrb(grid.level), grid.level.theme)
					"SAME" -> Orb(originalTargets[0].orb!!.desc, grid.level.theme)
					else -> Orb(Orb.getOrb(convertTo), grid.level.theme)
				}
				tile.orb!!.grid = grid

				tile.orb!!.setAttributes(orb)
			}

			Type.SUMMON ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>) { Friendly.load(data["SUMMON"].toString(), true).setTile(tile, grid) }

			Type.SPREADER -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>)
			{
				val spreader = data["SPREADER"] as Spreader
				spreader.damage += Global.player.getStat(Statistic.ABILITYDAMAGE) / 3f
				tile.spreader = spreader.copy()
			}

			Type.SUPERCHARGE -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>)
			{
				val special = tile.special ?: return

				val mergeSpecial = DualMatch(special.desc, grid.level.theme)

				tile.special = special.merge(mergeSpecial) ?: mergeSpecial.merge(special) ?: special
				tile.special!!.armed = true
				tile.special!!.markedForDeletion = true
			}

			Type.TEST ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>) { val orb = tile.orb ?: return; tile.special = Match5(orb.desc, grid.level.theme) }
		}
	}

	fun toString(data: ObjectMap<String, Any>, them: String, popAction: String): String
	{
		return when(type)
		{
			Type.POP -> { val dam = data["DAMAGE", "0"].toString().toInt() + Global.player.getStat(Statistic.ABILITYDAMAGE) + 1; "$popAction $them, dealing $dam damage." }

			Type.CONVERT -> { val t = data["CONVERTTO"]?.toString()?.toLowerCase()?.capitalize() ?: "Random"; "convert $them to $t." }

			Type.SUMMON -> { val f = data["SUMMON"].toString(); "summon " + f.filename(false) + "." }

			Type.SPREADER -> {
				val spreader = data["SPREADER"] as Spreader
				"create " + spreader.nameKey
			}

			Type.SUPERCHARGE -> "Supercharge"

			Type.TEST -> "TEST"
		}
	}
}