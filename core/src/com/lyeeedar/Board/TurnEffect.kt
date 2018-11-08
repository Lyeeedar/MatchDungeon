package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Ability.Permuter
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

interface IHasTurnEffect
{
	val onTurnEffects: Array<TurnEffect>
}

class TurnEffect
{
	lateinit var xmlData: XmlData

	enum class TurnEffectType
	{
		ATTACK,
		SEALEDATTACK,
		SEAL,
		BLOCK,
		HEAL,
		SUMMON,
		DELAYEDSUMMON,
		SPREADER,
		DEBUFF,
		CUSTOMORB
	}
	lateinit var type: TurnEffectType

	val data = ObjectMap<String, Any>()

	var cooldownRange = Point()
	var currentCooldown = 0
	var repeats = -1

	lateinit var target: MonsterAbility.Target
	lateinit var targetRestriction: Targetter
	var targetCount: Int = 1
	lateinit var permuter: Permuter

	fun onTurn(grid: Grid, tile: Tile)
	{
		if (repeats == 0) return

		currentCooldown--
		if (currentCooldown <= 0)
		{
			currentCooldown = cooldownRange.x + ((cooldownRange.y - cooldownRange.x).toFloat() * Random.random()).toInt()
			currentCooldown += (currentCooldown * Global.player.getStat(Statistic.HASTE)).ciel()

			execute(grid, tile)

			if (repeats > 0)
			{
				repeats--
			}
		}
	}

	private fun execute(grid: Grid, tile: Tile)
	{
		if (!Global.release)
		{
			println("On turn effect triggering")
		}

		val availableTargets = Array<Tile>()

		if (target == MonsterAbility.Target.NEIGHBOUR)
		{
			val range = data["RANGE", "1"].toString().toInt()
			availableTargets.addAll(grid.grid.filter { it.taxiDist(tile) <= range })
		}
		else if (target == MonsterAbility.Target.RANDOM)
		{
			val minRange = data["MINRANGE", "0"].toString().toInt()
			availableTargets.addAll(grid.grid.filter { it.taxiDist(tile) >= minRange })
		}
		else
		{
			throw NotImplementedError()
		}

		var validTargets = availableTargets.filter { targetRestriction.isValid(it, data) }

		if (targetRestriction.type == Targetter.Type.ORB &&
			(type == TurnEffectType.ATTACK || type == TurnEffectType.SEALEDATTACK ||
			 type == TurnEffectType.HEAL ||
			 type == TurnEffectType.DELAYEDSUMMON || type == TurnEffectType.DEBUFF))
		{
			validTargets = validTargets.filter { validAttack(grid, it) }
		}

		val chosen = validTargets.asSequence().random(targetCount).toList().toGdxArray()

		val finalTargets = Array<Tile>()

		for (target in chosen)
		{
			val source = grid.grid.filter { it.taxiDist(tile) <= 1 }.minBy { it.dist(target) }
			for (t in permuter.permute(target, grid, data, chosen, null, source))
			{
				if (!finalTargets.contains(t, true))
				{
					finalTargets.add(t)
				}
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

		for (target in finalTargets)
		{
			val strength = data.get("STRENGTH", "1").toString().toInt()

			if (type == TurnEffectType.ATTACK || type == TurnEffectType.SEALEDATTACK || type == TurnEffectType.HEAL || type == TurnEffectType.DELAYEDSUMMON || type == TurnEffectType.DEBUFF)
			{
				var speed = data.get("NUMPIPS", 7).toString().toInt()
				speed += (Global.player.getStat(Statistic.HASTE) * speed).toInt()

				val monsterEffectType: MonsterEffectType
				if (type == TurnEffectType.HEAL)
				{
					monsterEffectType = MonsterEffectType.HEAL
				}
				else if (type == TurnEffectType.DEBUFF)
				{
					monsterEffectType = MonsterEffectType.DEBUFF
				}
				else if (type == TurnEffectType.DELAYEDSUMMON)
				{
					monsterEffectType = MonsterEffectType.SUMMON
				}
				else
				{
					val dam = data.get("DAMAGE", "1").toString().toInt()
					monsterEffectType = if (dam > 1) MonsterEffectType.BIGATTACK else MonsterEffectType.ATTACK
				}

				val monsterEffect: MonsterEffect
				if (target.orb == null)
				{
					target.effects.add(grid.hitEffect.copy())
					monsterEffect = MonsterEffect(monsterEffectType, data, Orb.getRandomOrb(grid.level), grid.level.theme)
					target.monsterEffect = monsterEffect
				}
				else
				{
					monsterEffect = MonsterEffect(monsterEffectType, data, target.orb!!.desc, grid.level.theme)
					target.monsterEffect = monsterEffect
				}

				monsterEffect.timer = speed
				val diff = target.getPosDiff(tile)
				diff[0].y *= -1

				val dst = target.euclideanDist(tile)
				var animDuration = dst * 0.025f

				if (data["SHOWATTACKLEAP", "true"].toString().toBoolean())
				{
					animDuration += 0.4f
					val attackSprite = monsterEffect.actualSprite.copy()
					attackSprite.colour = monsterEffect.sprite.colour
					attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
					target.effects.add(attackSprite)

					monsterEffect.delayDisplay = animDuration
				}
				else
				{
					val flightEffect = data["FLIGHTEFFECT", null]
					if (flightEffect is ParticleEffect)
					{
						animDuration += 0.4f
						val particle = flightEffect.copy()
						particle.animation = MoveAnimation.obtain().set(animDuration, diff)
						particle.killOnAnimComplete = true

						target.effects.add(particle)

						monsterEffect.delayDisplay = animDuration
					}
				}

				val hitEffect = data["HITEFFECT", null]
				if (hitEffect is ParticleEffect)
				{
					val particle = hitEffect.copy()
					particle.renderDelay = animDuration

					animDuration += particle.lifetime / 2f
					monsterEffect.delayDisplay = animDuration

					target.effects.add(particle)
				}

				if (monsterEffect.delayDisplay == 0f)
				{
					monsterEffect.sprite = monsterEffect.actualSprite
					monsterEffect.sprite.colour = monsterEffect.desc.sprite.colour
				}
			}
			if (type == TurnEffectType.CUSTOMORB)
			{
				target.orb = Orb(Orb.getNamedOrb(data.get("NAME").toString()), grid.level.theme)
			}
			if (type == TurnEffectType.SEAL || type == TurnEffectType.SEALEDATTACK)
			{
				target.swappable?.sealCount = strength
			}
			if (type == TurnEffectType.BLOCK)
			{
				target.block = Block(grid.level.theme)
				target.block!!.maxhp = strength

				val diff = target.getPosDiff(tile)
				diff[0].y *= -1

				val dst = target.euclideanDist(tile)
				val animDuration = 0.4f + dst * 0.025f
				val attackSprite = target.block!!.sprite.copy()
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				target.effects.add(attackSprite)
			}
			if (type == TurnEffectType.SUMMON)
			{
				var desc = data["MONSTERDESC", null] as? MonsterDesc
				if (desc == null)
				{
					val factionName = data["FACTION", null]?.toString()

					val faction: Faction
					if (!factionName.isNullOrBlank())
					{
						val factionPath = XmlData.enumeratePaths("Factions", "Faction").first { it.toUpperCase().endsWith("$factionName.XML") }.split("Factions/")[1]

						faction = Faction.load(factionPath)
					}
					else
					{
						faction = grid.level.chosenFaction!!
					}

					val name = data["NAME", null]?.toString() ?: ""
					desc = if (name.isBlank()) faction.get(1) else faction.get(name)
				}

				val difficulty = data["DIFFICULTY", "0"].toString().toInt()

				val summoned = Monster(desc!!, difficulty)
				summoned.isSummon = data["ISSUMMON", "false"].toString().toBoolean()

				summoned.setTile(target, grid)

				val spawnEffect = data["SPAWNEFFECT", null] as? ParticleEffect
				if (spawnEffect != null)
				{
					val spawnEffect = spawnEffect.copy()
					target.effects.add(spawnEffect)
				}
			}
			if (type == TurnEffectType.SPREADER)
			{
				val spreader = data["SPREADER"] as Spreader
				target.spreader = spreader.copy()
			}
		}
	}

	private fun parse(xmlData: XmlData)
	{
		val cooldown = xmlData.get("Cooldown").split(",")
		cooldownRange = Point(cooldown[0].toInt(), cooldown[1].toInt())
		currentCooldown = cooldownRange.x + ((cooldownRange.y - cooldownRange.x).toFloat() * Random.random()).toInt()

		repeats = xmlData.getInt("Repeats", -1)

		type = TurnEffectType.valueOf(xmlData.get("Effect", "Attack")!!.toUpperCase())

		target = MonsterAbility.Target.valueOf(xmlData.get("Target", "NEIGHBOUR")!!.toUpperCase())
		targetCount = xmlData.getInt("Count", 1)

		targetRestriction = Targetter(Targetter.Type.valueOf(xmlData.get("TargetRestriction", "Orb")!!.toUpperCase()))
		permuter = Permuter(Permuter.Type.valueOf(xmlData.get("Permuter", "Single")!!.toUpperCase()))

		val dEl = xmlData.getChildByName("Data")
		if (dEl != null)
		{
			val dBlock = loadDataBlock(dEl)
			data.putAll(dBlock)
		}
	}

	fun copy(): TurnEffect
	{
		return load(xmlData)
	}

	companion object
	{
		fun load(xmlData: XmlData): TurnEffect
		{
			val effect = TurnEffect()
			effect.xmlData = xmlData
			effect.parse(xmlData)
			return effect
		}

		fun loadFromElement(xmlData: XmlData?): Array<TurnEffect>
		{
			val effects = Array<TurnEffect>()

			if (xmlData != null)
			{
				for (el in xmlData.children)
				{
					effects.add(load(el))
				}
			}

			return effects
		}
	}
}