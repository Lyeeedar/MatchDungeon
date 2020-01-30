package com.lyeeedar.Game.Ability

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.*
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Global
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.Util.XmlData

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
		BUFF
	}

	lateinit var apply: (tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>) -> Unit

	init
	{
		apply = when(type)
		{
			Type.POP -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val dam = data["DAMAGE", "0"].toString().evaluate(variables, grid.ran.nextLong())

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
				val contents = tile.contents
				val matchable = contents?.matchable() ?: return

				val convertTo = data["CONVERTTO"]?.toString() ?: "Random"
				val newDesc = when(convertTo)
				{
					"RANDOM" -> OrbDesc.getRandomOrb(grid.level)
					"SAME" -> originalTargets[0].contents!!.matchable()!!.desc
					else -> OrbDesc.getOrb(convertTo)
				}

				matchable.setDesc(newDesc, contents)
			}

			Type.SUMMON ->  fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val desc = FriendlyDesc.load(data["SUMMON"] as XmlData)
				val friendly = desc.getEntity(data["DEGENSUMMON", "true"].toString().toBoolean(), grid)
				friendly.pos().setTile(friendly, tile)
			}

			Type.SPREADER -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val spreader = (data["SPREADER"] as Spreader).copy()
				spreader.damage += Global.player.getStat(Statistic.ABILITYDAMAGE) / 3f
				tile.spreader = spreader
			}

			Type.SUPERCHARGE -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val contents = tile.contents
				val special = contents?.special() ?: return

				val specialEntity = EntityPool.obtain()
				specialEntity.addComponent(ComponentType.Special)
				val specialHolder = specialEntity.special()!!
				specialHolder.set(DualMatch())

				val merged = special.special.merge(specialEntity) ?: specialHolder.special.merge(contents) ?: special.special
				addSpecial(contents, merged)

				merged.setArmed(true, contents)

				contents.markForDeletion(0f, "merged")

				specialEntity.free()
			}

			Type.BUFF -> fun(tile: Tile, grid: Grid, delay: Float, data: ObjectMap<String, Any>, originalTargets: Array<Tile>, variables: ObjectFloatMap<String>)
			{
				val buff = (data["BUFF"] as Buff).copy()
				buff.remainingDuration += (Global.player.getStat(Statistic.BUFFDURATION, true) * buff.remainingDuration).toInt()

				val existing = Global.player.levelbuffs.firstOrNull { it.nameID == buff.nameID }
				if (existing != null)
				{
					existing.remainingDuration += buff.remainingDuration
				}
				else
				{
					Global.player.levelbuffs.add(buff)
				}

				if (!Global.resolveInstantly)
				{
					GridScreen.instance.updateBuffTable()
				}
			}
		}
	}
}