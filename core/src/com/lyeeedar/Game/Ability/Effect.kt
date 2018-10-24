package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.*
import com.lyeeedar.Game.Buff
import com.lyeeedar.Global
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.Util.XmlData
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
		BUFF,
		TEST
	}

	lateinit var apply: (tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>) -> Unit

	init
	{
		apply = when(type)
		{
			Type.POP -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val dam = data["DAMAGE", "0"].toString().evaluate(variables)

				val bonusDam: Float
				if (data["DAMAGE", "0"].toString().length == 1)
				{
					// only add on ability dam if we havent used an equation
					bonusDam = dam + grid.level.player.getStat(Statistic.ABILITYDAMAGE)
				}
				else
				{
					bonusDam = dam
				}

				grid.pop(tile, delay, damSource = this, bonusDam = bonusDam, pierce = grid.level.player.getStat(Statistic.PIERCE), skipPowerOrb = true)
			}

			Type.CONVERT -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
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

			Type.SUMMON ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>) { Friendly.load(data["SUMMON"] as XmlData, true).setTile(tile, grid) }

			Type.SPREADER -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val spreader = (data["SPREADER"] as Spreader).copy()
				spreader.damage += Global.player.getStat(Statistic.ABILITYDAMAGE) / 3f
				tile.spreader = spreader
			}

			Type.SUPERCHARGE -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val special = tile.special ?: return

				val mergeSpecial = DualMatch(special.desc, grid.level.theme)

				tile.special = special.merge(mergeSpecial) ?: mergeSpecial.merge(special) ?: special
				tile.special!!.armed = true
				tile.special!!.markedForDeletion = true
			}

			Type.BUFF -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val buff = (data["BUFF"] as Buff).copy()
				buff.remainingDuration += (Global.player.getStat(Statistic.BUFFDURATION, true) * buff.remainingDuration).toInt()

				val existing = Global.player.levelbuffs.firstOrNull { it.name == buff.name }
				if (existing != null)
				{
					existing.remainingDuration += buff.remainingDuration
				}
				else
				{
					Global.player.levelbuffs.add(buff)
				}

				GridScreen.instance.updateBuffTable()
			}

			Type.TEST ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>) { val orb = tile.orb ?: return; tile.special = Match5(orb.desc, grid.level.theme) }
		}
	}

	fun toString(data: ObjectMap<String, Any>, them: String, popAction: String, ability: Ability): String
	{
		return when(type)
		{
			Type.POP ->
			{
				val dam = data["DAMAGE", "0"].toString().toInt() + Global.player.getStat(Statistic.ABILITYDAMAGE) + 1

				if (ability.permuter.type == Permuter.Type.SINGLE && ability.targetter.type == Targetter.Type.ATTACK)
				{
					"$popAction $them"
				}
				else
				{
					"$popAction $them, dealing $dam damage."
				}
			}

			Type.CONVERT -> { val t = data["CONVERTTO"]?.toString()?.toLowerCase()?.capitalize() ?: "Random"; "convert $them to $t." }

			Type.SUMMON -> { val f = data["SUMMON"].toString(); "summon " + f.filename(false) + "." }

			Type.SPREADER -> {
				val spreader = data["SPREADER"] as Spreader
				"create " + spreader.nameKey
			}

			Type.SUPERCHARGE -> "Supercharge"

			Type.BUFF -> {
				throw Exception("Shouldnt ever hit this")
			}

			Type.TEST -> "TEST"
		}
	}
}