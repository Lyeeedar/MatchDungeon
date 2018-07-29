package com.lyeeedar.Game.Ability

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
		TEST
	}

	lateinit var apply: (tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>) -> Unit

	init
	{
		apply = when(type)
		{
			Type.POP -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>) { grid.pop(tile, delay, damSource = this, bonusDam = data["DAMAGE", "0"].toString().toInt() + grid.level.player.getStat(Statistic.ABILITYDAMAGE), skipPowerOrb = true) }

			Type.CONVERT -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>) { val orb = tile.orb ?: return; tile.orb = if(data["CONVERTTO"] == "RANDOM") Orb(Orb.getRandomOrb(grid.level), grid.level.theme) else Orb(Orb.getOrb(data["CONVERTTO"].toString()), grid.level.theme); tile.orb!!.setAttributes(orb) }

			Type.SUMMON ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>) { Friendly.load(data["SUMMON"].toString(), true).setTile(tile, grid) }

			Type.SPREADER -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>)
			{
				val spreader = data["SPREADER"] as Spreader
				spreader.damage += Global.player.getStat(Statistic.ABILITYDAMAGE) / 2f
				tile.spreader = spreader.copy()
			}

			Type.TEST ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>) { val orb = tile.orb ?: return; orb.special = Match5(orb) }
		}
	}

	fun toString(data: ObjectMap<String, Any>, them: String, popAction: String): String
	{
		return when(type)
		{
			Type.POP -> { val dam = data["DAMAGE", "0"].toString().toInt() + Global.player.getStat(Statistic.ABILITYDAMAGE) + 1; "$popAction $them, dealing $dam damage." }

			Type.CONVERT -> { val t = data["CONVERTTO"]; "convert $them to $t." }

			Type.SUMMON -> { val f = data["SUMMON"].toString(); "summon " + f.filename(false) + "." }

			Type.SPREADER -> {
				val spreader = data["SPREADER"] as Spreader
				"create " + spreader.nameKey
			}

			Type.TEST -> "TEST"
		}
	}
}