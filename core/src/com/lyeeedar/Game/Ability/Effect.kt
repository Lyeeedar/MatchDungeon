package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.*

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
		TEST
	}

	lateinit var apply: (tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, String>) -> Unit

	init
	{
		apply = when(type)
		{
			Type.POP -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, String>) { grid.pop(tile, delay, damSource = this, bonusDam = data["DAMAGE", "0"].toInt() + grid.level.player.abilityDam, skipPowerOrb = true) }
			Type.CONVERT -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, String>) { val orb = tile.orb ?: return; tile.orb = if(data["CONVERTTO"] == "RANDOM") Orb(Orb.getRandomOrb(grid.level), grid.level.theme) else Orb(Orb.getOrb(data["CONVERTTO"]), grid.level.theme); tile.orb!!.setAttributes(orb) }
			Type.SUMMON ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, String>) { Friendly.load(data["SUMMON"], true).setTile(tile, grid) }
			Type.TEST ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, String>) { val orb = tile.orb ?: return; orb.special = Match5(orb) }
			else -> throw Exception("Invalid effect type $type")
		}
	}
}