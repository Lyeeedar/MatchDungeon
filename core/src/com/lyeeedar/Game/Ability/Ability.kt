package com.lyeeedar.Game.Ability

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Spreader
import com.lyeeedar.Board.Tile
import com.lyeeedar.Game.Buff
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.UI.Seperator
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

/**
 * Created by Philip on 20-Jul-16.
 */

class Ability
{
	lateinit var name: String
	lateinit var description: String

	var hitEffect: ParticleEffect? = null
	var flightEffect: ParticleEffect? = null

	var cost: Int = 2

	var targets = 1
	var targetter: Targetter = Targetter(Targetter.Type.ORB)
	var permuter: Permuter = Permuter(Permuter.Type.SINGLE)
	var effect: Effect = Effect(Effect.Type.TEST)
	val data = ObjectMap<String, Any>()

	val selectedTargets = Array<Tile>()

	fun createTable(): Table
	{
		val table = Table()
		table.defaults().pad(5f)

		val titleStack = Stack()
		val iconTable = Table()
		titleStack.add(iconTable)
		titleStack.add(Label(name, Global.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()

		val descLabel = Label(description, Global.skin, "card")
		descLabel.setWrap(true)

		table.add(descLabel).growX()
		table.row()

		table.add(Seperator(Global.skin, "horizontalcard")).growX().pad(10f)
		table.row()

		if (effect.type == Effect.Type.BUFF)
		{
			val buff = data["BUFF"] as Buff

			var turns = buff.remainingDuration.toString()

			if (Global.player.getStat(Statistic.BUFFDURATION, true) > 0f)
			{
				val bonus = (buff.remainingDuration.toFloat() * Global.player.getStat(Statistic.BUFFDURATION, true)).toInt()
				turns = "($turns + $bonus)"
			}

			val effectDesc = "For $turns gain buff:"

			val effectLabel = Label(effectDesc, Global.skin, "card")
			effectLabel.setWrap(true)

			table.add(effectLabel).growX()
			table.row()

			table.add(Seperator(Global.skin)).growX()
			table.row()

			table.add(buff.createTable(false)).growX()
			table.row()

			table.add(Seperator(Global.skin)).growX()
			table.row()
		}
		else
		{
			var effectDesc = "Target $targets " + targetter.type.toString().toLowerCase().capitalize().pluralize(targets)

			if (permuter.type != Permuter.Type.SINGLE)
			{
				effectDesc += " then " + permuter.toString(data)
			}

			val them = if (targets > 1 || permuter.type != Permuter.Type.SINGLE) "them" else "it"
			effectDesc += " and " + effect.toString(data, them, targetter.popAction())

			val effectLabel = Label(effectDesc, Global.skin, "card")
			effectLabel.setWrap(true)

			table.add(effectLabel).growX()
			table.row()
		}

		table.add(Label("Cost: $cost", Global.skin, "card")).growX()
		table.row()

		return table
	}

	fun activate(grid: Grid)
	{
		PowerBar.instance.pips -= cost

		if (effect.type == Effect.Type.BUFF)
		{
			effect.apply(Tile(0, 0), grid, 0f, data, Array(), ObjectFloatMap())
			return
		}

		val finalTargets = Array<Tile>()

		if (permuter.type == Permuter.Type.RANDOM && targets == 0)
		{
			for (t in permuter.permute(grid.tile(grid.width/2, grid.height/2)!!, grid, data, selectedTargets, this))
			{
				if (!selectedTargets.contains(t, true))
				{
					selectedTargets.add(t)
				}
			}
		}

		val selectedDelays = ObjectMap<Tile, Float>()

		for (target in selectedTargets)
		{
			if (permuter.type == Permuter.Type.RANDOM && targets == 0)
			{
				finalTargets.add(target)
			}
			else
			{
				for (t in permuter.permute(target, grid, data, selectedTargets, this))
				{
					if (!finalTargets.contains(t, true))
					{
						finalTargets.add(t)
					}
				}

				val coverage = data["COVERAGE", "1"]?.toString()?.toFloat() ?: 1f
				if (coverage < 1f)
				{
					val chosenCount = (finalTargets.size.toFloat() * coverage).ciel()
					while (finalTargets.size > chosenCount)
					{
						finalTargets.removeRandom(Random.random)
					}
				}
			}

			var delay = 0f
			if (flightEffect != null)
			{
				val fs = flightEffect!!.copy()
				fs.killOnAnimComplete = true

				val p1 = Vector2()
				val p2 = GridWidget.instance.pointToScreenspace(target)

				val gridWidget = GridScreen.instance.grid!!
				p1.scl(1f / gridWidget.ground.tileSize)
				p2.scl(1f / gridWidget.ground.tileSize)

				val dist = p1.dst(p2)

//				if (fs.moveType == ParticleEffect.MoveType.Leap)
//				{
//					val animDuration = (0.25f + dist * 0.025f) * fs.moveSpeed
//
//					val path = arrayOf(p1, p2)
//					for (point in path)
//					{
//						point.x -= path.last().x
//						point.y -= path.last().y
//					}
//
//					fs.animation = LeapAnimation.obtain().set(animDuration, path, 1f + dist * 0.25f)
//					fs.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
//				}
//				else
//				{
					fs.animation = MoveAnimation.obtain().set(0.25f + dist * 0.025f, arrayOf(p1, p2), Interpolation.linear)
//				}

				fs.rotation = getRotation(p1, p2)
				delay += fs.lifetime

				target.effects.add(fs)
			}

			selectedDelays[target] = delay
		}

		// make variables map
		val variables = ObjectFloatMap<String>()
		for (stat in Statistic.Values)
		{
			variables[stat.toString().toUpperCase()] = Global.player.getStat(stat, true)
		}
		val monsters = finalTargets.mapNotNull { it.monster }.toGdxArray()
		variables["MONSTERCOUNT"] = monsters.size.toFloat()
		variables["MONSTERHP"] = monsters.map { it.hp }.sum()
		variables["TILECOUNT"] = finalTargets.filter { it.canHaveOrb }.size.toFloat()

		val originalTargets = selectedTargets.toGdxArray()
		for (target in finalTargets)
		{
			val closest = selectedTargets.minBy { it.dist(target) }!!
			val dst = if (permuter.type == Permuter.Type.RANDOM) 0 else closest.dist(target)

			Future.call(
					{
						var delay = 0.0f
						if (hitEffect != null)
						{
							val hs = hitEffect!!.copy()
							hs.renderDelay = delay + 0.1f * dst
							delay += hs.lifetime * 0.6f

							if (permuter.type == Permuter.Type.BLOCK || permuter.type == Permuter.Type.DIAMOND)
							{
								// single sprite
								if (originalTargets.contains(target, true))
								{
									hs.size[0] = data["AOE"].toString().toInt() * 2 + 1
									hs.size[1] = hs.size[0]
									hs.isCentered = true
									target.effects.add(hs)
								}
							}
							else
							{
								target.effects.add(hs)
							}
						}

						effect.apply(target, grid, delay, data, originalTargets, variables)
					}, selectedDelays[closest] - 0.05f)

		}

		selectedTargets.clear()
	}

	fun hasValidTargets(grid: Grid): Boolean
	{
		if (effect.type == Effect.Type.BUFF)
		{
			return true
		}
		else
		{
			return getValidTargets(grid).size > 0
		}
	}

	fun getValidTargets(grid: Grid): Array<Point>
	{
		val output = Array<Point>()

		for (tile in grid.grid)
		{
			if (targetter.isValid(tile, data))
			{
				output.add(tile)
			}
		}

		return output
	}

	fun parse(xml: XmlData)
	{
		name = xml.get("Name")
		description = xml.get("Description")

		val dataEl = xml.getChildByName("EffectData")!!

		cost = dataEl.getInt("Cost", 1)

		val effectDesc = dataEl.get("Effect")
		val split = effectDesc.toUpperCase().split(",")

		targets = split[0].toInt()
		targetter = Targetter(Targetter.Type.valueOf(split[1]))
		permuter = Permuter(Permuter.Type.valueOf(split[2]))
		effect = Effect(Effect.Type.valueOf(split[3]))

		val dEl = dataEl.getChildByName("Data")
		if (dEl != null)
		{
			for (el in dEl.children)
			{
				if (el.name == "Spreader")
				{
					val spreader = Spreader.load(el)
					data[el.name.toUpperCase()] = spreader

				}
				else if (el.name == "Buff")
				{
					val buff = Buff.load(el)
					data[el.name.toUpperCase()] = buff
				}
				else
				{
					data[el.name.toUpperCase()] = el.text.toUpperCase()
				}
			}
		}

		val hitEffectData = dataEl.getChildByName("HitEffect")
		if (hitEffectData != null) hitEffect = AssetManager.loadParticleEffect(hitEffectData)
		val flightEffectData = dataEl.getChildByName("FlightEffect")
		if (flightEffectData != null) flightEffect = AssetManager.loadParticleEffect(flightEffectData)

		if (effect.type == Effect.Type.BUFF)
		{
			targets = 0
		}
	}

	companion object
	{
		fun load(xml: XmlData): Ability
		{
			val ability = Ability()
			ability.parse(xml)
			return ability
		}
	}
}
