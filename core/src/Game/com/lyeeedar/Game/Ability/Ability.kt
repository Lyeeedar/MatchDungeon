package com.lyeeedar.Game.Ability

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Spreader
import com.lyeeedar.Board.Tile
import com.lyeeedar.Board.isMonster
import com.lyeeedar.Components.damageable
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.UI.Seperator
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray
import java.util.*

@DataClass(name = "Ability")
class AbilityData : XmlDataClass()
{
	@NeedsLocalisation(file = "Ability")
	lateinit var nameID: String

	@NeedsLocalisation(file = "Ability")
	lateinit var descriptionID: String

	var hitEffect: ParticleEffect? = null
	var flightEffect: ParticleEffect? = null

	var cost: Int = 2

	var maxUsages: Int = -1
	var resetUsagesPerLevel = false

	var targets = 1
	lateinit var targetter: Targetter
	lateinit var permuter: Permuter
	lateinit var effect: Effect

	override fun load(xmlData: XmlData)
	{

	}
}

class Ability(val data: AbilityData)
{
	val name: String
		get() = Localisation.getText(data.nameID, "Ability")

	var remainingUsages: Int = -1

	val selectedTargets = Array<Tile>()

	fun getCard(): CardWidget
	{
		return CardWidget.createCard(name, Localisation.getText("ability", "UI"), AssetManager.loadSprite("GUI/AbilityCardback"), createTable(), createTable())
	}

	fun createTable(): Table
	{
		val table = Table()
		table.defaults().pad(5f)

		var description = Localisation.getText(data.descriptionID, "Ability")

		if (data.effect.type == Effect.Type.POP)
		{
			val dam = data["DAMAGE", "0"].toString().evaluate(Global.getVariableMap())
			val bonusDam: Float
			if (data["DAMAGE", "0"].toString().length == 1)
			{
				// only add on ability dam if we havent used an equation
				bonusDam = dam + Global.player.getStat(Statistic.ABILITYDAMAGE)
			}
			else
			{
				bonusDam = dam
			}
			description = description.replace("{Damage}", bonusDam.toString())
		}
		else if (data.effect.type == Effect.Type.BUFF)
		{
			val buff = data["BUFF"] as Buff
			var turns = buff.remainingDuration.toString()

			if (Global.player.getStat(Statistic.BUFFDURATION, true) > 0f)
			{
				val bonus = (buff.remainingDuration.toFloat() * Global.player.getStat(Statistic.BUFFDURATION, true)).toInt()
				turns = "($turns + $bonus)"
			}

			description = description.replace("{Turns}", turns)
		}

		val descLabel = Label(description, Statics.skin, "card")
		descLabel.setWrap(true)

		table.add(descLabel).growX()
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard")).growX().pad(10f)
		table.row()

		if (data.effect.type == Effect.Type.BUFF)
		{
			table.add(Seperator(Statics.skin)).growX()
			table.row()

			val buff = data["BUFF"] as Buff
			table.add(buff.createTable(false)).growX()
			table.row()

			table.add(Seperator(Statics.skin)).growX()
			table.row()
		}

		table.add(Label(Localisation.getText("ability.powercost", "UI") + ": ${data.cost}", Statics.skin, "card")).growX()
		table.row()

		if (data.maxUsages > 0)
		{
			if (data.resetUsagesPerLevel)
			{
				table.add(Label(Localisation.getText("ability.usagesperencounter", "UI") + ": ${data.maxUsages}", Statics.skin, "card")).growX()
				table.row()
			}
			else
			{
				table.add(Label(Localisation.getText("ability.remainingusages", "UI") + ": $remainingUsages", Statics.skin, "card")).growX()
				table.row()
			}
		}

		return table
	}

	fun activate(grid: Grid)
	{
		if (data.maxUsages > 0)
		{
			if (remainingUsages == 0)
			{
				return
			}

			remainingUsages--
		}

		PowerBar.instance.pips -= cost

		if (data.effect.type == Effect.Type.BUFF)
		{
			data.effect.apply(Tile(0, 0, grid), grid, 0f, Array(), ObjectFloatMap())
			return
		}

		val finalTargets = Array<Tile>()

		if (data.permuter.type == PermuterType.RANDOM && data.targets == 0)
		{
			for (t in data.permuter.permute(grid.tile(grid.width/2, grid.height/2)!!, grid, selectedTargets, this, null))
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
			if (data.permuter.type == PermuterType.RANDOM && data.targets == 0)
			{
				finalTargets.add(target)
			}
			else
			{
				for (t in data.permuter.permute(target, grid, selectedTargets, this, null))
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
						finalTargets.removeRandom(grid.ran)
					}
				}
			}

			var delay = 0f
			if (data.flightEffect != null && !Global.resolveInstantly)
			{
				val fs = data.flightEffect!!.copy()
				fs.killOnAnimComplete = true

				val p1 = Vector2(Statics.stage.width / 2f, 0f)
				val p2 = GridWidget.instance.pointToScreenspace(target)

				val gridWidget = GridScreen.instance.grid!!
				p1.scl(1f / gridWidget.renderer.tileSize)
				p2.scl(1f / gridWidget.renderer.tileSize)

				val dist = p1.dst(p2)

				fs.animation = MoveAnimation.obtain().set((0.25f + dist * 0.025f) * (1.0f / fs.timeMultiplier), arrayOf(p1, p2), Interpolation.linear)

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
			variables[stat.toString().toUpperCase(Locale.ENGLISH)] = Global.player.getStat(stat, true)
		}
		val monsters = finalTargets.filter { it.contents?.isMonster() == true }.map { it.contents!! }.toGdxArray()
		variables["MONSTERCOUNT"] = monsters.size.toFloat()
		variables["MONSTERHP"] = monsters.map { it.damageable()!!.hp }.sum()
		variables["TILECOUNT"] = finalTargets.filter { it.canHaveOrb }.size.toFloat()

		val originalTargets = selectedTargets.toGdxArray()
		for (target in finalTargets)
		{
			val closest = selectedTargets.minBy { it.dist(target) }!!
			val dst = if (data.permuter.type == PermuterType.RANDOM) 0 else closest.dist(target)

			target.addDelayedAction(
					{ target ->
						var delay = 0.0f
						if (data.hitEffect != null && !Global.resolveInstantly)
						{
							val hs = data.hitEffect!!.copy()
							hs.renderDelay = delay + 0.1f * dst
							delay += hs.lifetime * 0.6f

							if (data.permuter.type == PermuterType.BLOCK || data.permuter.type == PermuterType.DIAMOND)
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

						data.effect.apply(target, grid, delay, data, originalTargets, variables)
					}, selectedDelays[closest] - 0.05f)

		}

		selectedTargets.clear()
	}

	fun hasValidTargets(grid: Grid): Boolean
	{
		if (data.maxUsages > 0 && remainingUsages == 0)
		{
			return false
		}
		else if (data.effect.type == Effect.Type.BUFF)
		{
			val buff = data["BUFF"] as Buff
			if (Global.player.levelbuffs.any { it.data.nameID == buff.data.nameID && it.remainingDuration > 1 })
			{
				return false
			}

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
			if (data.targetter.isValid(tile))
			{
				output.add(tile)
			}
		}

		return output
	}

	companion object
	{
		fun load(xml: XmlData): Ability
		{
			val data = AbilityData()
			data.load(xml)

			return Ability(data)
		}
	}
}
