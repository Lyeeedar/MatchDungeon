package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Permuter
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Game.Buff
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

/**
 * Created by Philip on 22-Jul-16.
 */

class Monster(val desc: MonsterDesc, val difficulty: Int) : Creature(desc.hp, desc.size, desc.sprite.copy(), desc.death.copy())
{
	var isSummon = false

	var atkCooldown = 0

	val abilities = Array<MonsterAbility>()

	var immuneCooldown = -1

	var fastAttacks = -1
	var powerfulAttacks = -1

	var attackDamage = 1
	var attackNumPips = 7
	var attackCooldown: Point = Point(6, 6)

	init
	{
		attackDamage = desc.attackDamage
		attackNumPips = desc.attackNumPips
		attackCooldown = desc.attackCooldown.copy()

		maxhp += (maxhp.toFloat() * (difficulty / 10f)).ciel()

		abilities.addAll(desc.abilities.map{ it.copy() }.toGdxArray())
		damageReduction = desc.damageReduction

		if (difficulty >= 1)
		{
			atkCooldown -= (difficulty.toFloat() / 3f).ciel()
		}

		if (difficulty >= 2)
		{
			for (ability in abilities)
			{
				ability.cooldownMin -= (difficulty.toFloat() / 3f).ciel()
				ability.cooldownMax -= (difficulty.toFloat() / 3f).ciel()
			}

			attackNumPips -= (difficulty.toFloat() / 3f).ciel()
		}

		if (difficulty >= 3)
		{
			damageReduction += (difficulty.toFloat() / 5f).ciel()
			attackDamage += (difficulty.toFloat() / 4f).ciel()
		}

		if (desc.attackNumPips > 0)
		{
			var max = desc.attackCooldown.max
			max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

			atkCooldown = (MathUtils.random() * max).toInt()
		}
		else
		{
			atkCooldown = Int.MAX_VALUE
		}

		hp -= Global.player.getStat(Statistic.WEAKNESSAURA)
	}

	override fun onTurn(grid: Grid)
	{
		if (immune)
		{
			immuneCooldown--
			if (immuneCooldown <= 0)
			{
				immune = false
			}
		}

		if (fastAttacks > 0)
		{
			fastAttacks--

			atkCooldown = 0
		}

		if (powerfulAttacks > 0)
		{
			powerfulAttacks--
		}

		atkCooldown--
		if (atkCooldown <= 0)
		{
			var min = attackCooldown.min
			min += (Global.player.getStat(Statistic.HASTE) * min).toInt()

			var max = attackCooldown.max
			max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

			atkCooldown = min + MathUtils.random(max - min)

			// do attack
			val tile = grid.grid.filter { validAttack(grid, it) }.random()

			if (tile != null)
			{
				val startTile = tiles.minBy { it.dist(tile) }!!

				val damage = if (powerfulAttacks > 0) attackDamage + 2 else attackDamage

				val monsterEffectType = if (damage > 1) MonsterEffectType.BIGATTACK else MonsterEffectType.ATTACK
				val data = ObjectMap<String, Any>()
				data["DAMAGE"] = damage.toString()

				tile.monsterEffect = MonsterEffect(monsterEffectType, data, tile.orb!!.desc, grid.level.theme)

				tile.monsterEffect!!.timer = attackNumPips + (Global.player.getStat(Statistic.HASTE) * attackNumPips).toInt()
				val diff = tile.getPosDiff(startTile)
				diff[0].y *= -1
				sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(startTile)
				val animDuration = 0.4f + tile.euclideanDist(startTile) * 0.025f
				val attackSprite = tile.monsterEffect!!.sprite.copy()
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				tile.effects.add(attackSprite)

				tile.monsterEffect!!.delayDisplay = animDuration
			}
		}

		for (ability in abilities)
		{
			ability.cooldownTimer--
			if (ability.cooldownTimer <= 0)
			{
				var min = ability.cooldownMin
				min += (Global.player.getStat(Statistic.HASTE) * min).toInt()

				var max = ability.cooldownMax
				max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

				ability.cooldownTimer = min + MathUtils.random(max - min)
				ability.activate(grid, this)
			}
		}

		if (isSummon)
		{
			hp--
		}
	}
}

fun validAttack(grid: Grid, tile: Tile): Boolean
{
	if (!tile.canHaveOrb) return false
	if (tile.orb == null) return false
	if (tile.monsterEffect != null) return false
	if (tile.spreader != null) return false

	for (dir in Direction.CardinalValues)
	{
		val ntile = grid.tile(tile + dir) ?: return false
		if (!ntile.canHaveOrb) return false
	}

	return true
}

class MonsterAbility
{
	enum class Target
	{
		NEIGHBOUR,
		RANDOM
	}

	enum class Effect
	{
		ATTACK,
		SEALEDATTACK,
		CUSTOMORB,
		SEAL,
		BLOCK,
		MOVE,
		DASH,
		HEAL,
		SUMMON,
		DELAYEDSUMMON,
		SPREADER,
		DEBUFF,
		SELFBUFF
	}

	enum class MoveType
	{
		BASIC,
		LEAP,
		TELEPORT
	}

	var cooldownTimer: Int = 0
	var cooldownMin: Int = 1
	var cooldownMax: Int = 1
	lateinit var target: Target
	lateinit var targetRestriction: Targetter
	var targetCount: Int = 1
	lateinit var permuter: Permuter
	lateinit var effect: Effect
	var repeatable = true

	var hasBeenUsed = false
	val data = ObjectMap<String, Any>()

	fun copy(): MonsterAbility
	{
		val ability = MonsterAbility()
		ability.cooldownMin = cooldownMin
		ability.cooldownMax = cooldownMax
		ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)
		ability.repeatable = repeatable
		ability.target = target
		ability.targetRestriction = targetRestriction
		ability.targetCount = targetCount
		ability.permuter = permuter
		ability.effect = effect
		ability.data.putAll(data)

		return ability
	}

	fun activate(grid: Grid, monster: Monster)
	{
		if (!repeatable && hasBeenUsed)
		{
			return
		}
		hasBeenUsed = true

		if (!Global.release)
		{
			println("Monster trying to use ability '$effect'")
		}

		if (effect == Effect.SELFBUFF)
		{
			val type = data["BUFFTYPE", "Immunity"].toString()
			val duration =  data["DURATION", "1"].toString().toInt()

			when (type)
			{
				"Immunity" -> { monster.immune = true; monster.immuneCooldown = duration }
				"FastAttacks" -> monster.fastAttacks = duration
				"PowerfulAttacks" -> monster.powerfulAttacks = duration
				else -> throw Exception("Unknown monster selfbuff '$type'!")
			}

			val effect = data["PARTICLEEFFECT", null]
			if (effect is ParticleEffect)
			{
				val e = effect.copy()
				e.size[0] = monster.size
				e.size[1] = monster.size
				monster.tiles[0, 0].effects.add(e)
			}

			return
		}

		val availableTargets = Array<Tile>()

		if (target == Target.NEIGHBOUR)
		{
			if (effect == Effect.MOVE || effect == Effect.DASH)
			{
				val range = data["RANGE", "1"].toString().toInt()
				val minRange = data["MINRANGE", "0"].toString().toInt()

				availableTargets.addAll(grid.grid.filter { it != monster.tiles[0,0] && it.taxiDist(monster.tiles[0, 0]) <= range && it.taxiDist(monster.tiles[0, 0]) >= minRange })
			}
			else
			{
				availableTargets.addAll(monster.getBorderTiles(grid, data["RANGE", "1"].toString().toInt()))
			}
		}
		else if (target == Target.RANDOM)
		{
			val minRange = data["MINRANGE", "0"].toString().toInt()

			availableTargets.addAll(grid.grid.filter { !monster.tiles.contains(it) && it.taxiDist(monster.tiles[0, 0]) >= minRange })
		}
		else
		{
			throw NotImplementedError()
		}

		var validTargets = availableTargets.filter { targetRestriction.isValid(it, data) }

		if (targetRestriction.type == Targetter.Type.ORB && (effect == Effect.ATTACK || effect == Effect.SEALEDATTACK || effect == Effect.HEAL || effect == Effect.DELAYEDSUMMON || effect == Effect.DEBUFF))
		{
			validTargets = validTargets.filter { validAttack(grid, it) }
		}

		val chosen = validTargets.asSequence().random(targetCount).toList().toGdxArray()

		val finalTargets = Array<Tile>()

		for (target in chosen)
		{
			val source = monster.getBorderTiles(grid, 1).minBy { it.dist(target) }
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

		if (effect == Effect.MOVE || effect == Effect.DASH)
		{
			fun isValid(t: Tile): Boolean
			{
				for (x in 0 until monster.size)
				{
					for (y in 0 until monster.size)
					{
						val tile = grid.tile(t.x + x, t.y + y) ?: return false

						if (tile.monster != monster)
						{
							if (!tile.canHaveOrb)
							{
								return false
							}

							if (tile.contents != null && tile.contents !is Orb)
							{
								return false
							}
						}
					}
				}

				return true
			}

			val target = availableTargets.filter(::isValid).asSequence().random()

			monster.sprite.animation = null

			if (target != null)
			{
				val dst = monster.tiles[0,0].euclideanDist(target)
				var animDuration = 0.25f + dst * 0.025f

				val start = monster.tiles.first()
				monster.setTile(target, grid, animDuration - 0.1f)
				val end = monster.tiles.first()

				val diff = end.getPosDiff(start)
				diff[0].y *= -1

				val moveType: MoveType
				if (effect == Effect.DASH)
				{
					moveType = MoveType.BASIC
				}
				else if (data.containsKey("MOVETYPE"))
				{
					val moveTypeStr = data["MOVETYPE"]
					moveType = when(moveTypeStr)
					{
						"Basic" -> MoveType.BASIC
						"Leap" -> MoveType.LEAP
						"Teleport" -> MoveType.TELEPORT
						else -> throw Exception("Unknown move type '$moveTypeStr'!")
					}
				}
				else if (this.target == Target.RANDOM)
				{
					moveType = MoveType.LEAP
				}
				else
				{
					moveType = MoveType.BASIC
				}

				if (moveType == MoveType.LEAP)
				{
					monster.sprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					monster.sprite.animation = ExpandAnimation.obtain().set(animDuration, 1f, 2f, false)
				}
				else if (moveType == MoveType.TELEPORT)
				{
					animDuration = 0.2f
					monster.sprite.renderDelay = animDuration
					monster.sprite.showBeforeRender = false
				}
				else
				{
					monster.sprite.animation = MoveAnimation.obtain().set(animDuration, UnsmoothedPath(diff), Interpolation.linear)
				}

				val startParticle = data["STARTEFFECT", null]
				val endParticle = data["ENDEFFECT", null]

				if (startParticle is ParticleEffect)
				{
					val particle = startParticle.copy()
					particle.size[0] = monster.size
					particle.size[1] = monster.size

					start.effects.add(particle)
				}

				if (endParticle is ParticleEffect)
				{
					val particle = endParticle.copy()
					particle.size[0] = monster.size
					particle.size[1] = monster.size

					particle.renderDelay = animDuration

					end.effects.add(particle)
				}

				if (effect == Effect.DASH)
				{
					// get line from start to end
					val points = start.lineTo(end).toGdxArray()


					val maxDist = start.euclideanDist(end)

					val hitEffect = data["HITEFFECT", null]

					var timer = data["NUMPIPS", monster.attackNumPips.toString()].toString().toInt()
					timer += (Global.player.getStat(Statistic.HASTE) * timer).toInt()

					// make them all attacks in order
					for (point in points)
					{
						val tile = grid.tile(point)
						if (tile != null && validAttack(grid, tile))
						{
							val dist = start.euclideanDist(point)
							val alpha = dist / maxDist
							val delay = animDuration * alpha

							Future.call(
									{
										tile.monsterEffect = MonsterEffect(MonsterEffectType.ATTACK, ObjectMap(), tile.orb!!.desc, grid.level.theme)
										tile.monsterEffect!!.timer = timer

										if (hitEffect is ParticleEffect)
										{
											val particle = hitEffect.copy()
											tile.effects.add(particle)
										}

									}, delay)
						}
					}
				}
			}

			return
		}

		for (target in finalTargets)
		{
			val strength = data.get("STRENGTH", "1").toString().toInt()

			if (effect == Effect.ATTACK || effect == Effect.SEALEDATTACK || effect == Effect.HEAL || effect == Effect.DELAYEDSUMMON || effect == Effect.DEBUFF)
			{
				var speed = data.get("NUMPIPS", monster.attackNumPips.toString()).toString().toInt()
				speed += (Global.player.getStat(Statistic.HASTE) * speed).toInt()

				val monsterEffectType: MonsterEffectType
				if (effect == Effect.HEAL)
				{
					monsterEffectType = MonsterEffectType.HEAL
				}
				else if (effect == Effect.DEBUFF)
				{
					monsterEffectType = MonsterEffectType.DEBUFF
				}
				else if (effect == Effect.DELAYEDSUMMON)
				{
					monsterEffectType = MonsterEffectType.SUMMON
				}
				else
				{
					val dam = data.get("DAMAGE", "1").toString().toInt()
					monsterEffectType = if (dam > 1) MonsterEffectType.BIGATTACK else MonsterEffectType.ATTACK
				}

				if (target.orb == null)
				{
					target.effects.add(grid.hitEffect.copy())
					target.monsterEffect = MonsterEffect(monsterEffectType, data, Orb.getRandomOrb(grid.level), grid.level.theme)
				}
				else
				{
					target.monsterEffect = MonsterEffect(monsterEffectType, data, target.orb!!.desc, grid.level.theme)
				}

				target.monsterEffect!!.timer = speed
				val diff = target.getPosDiff(monster.tiles[0, 0])
				diff[0].y *= -1
				monster.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = target.euclideanDist(monster.tiles[0, 0])
				var animDuration = dst * 0.025f

				if (data["SHOWATTACKLEAP", "true"].toString().toBoolean())
				{
					animDuration += 0.4f
					val attackSprite = target.monsterEffect!!.actualSprite.copy()
					attackSprite.colour = target.monsterEffect!!.sprite.colour
					attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
					target.effects.add(attackSprite)

					target.monsterEffect!!.delayDisplay = animDuration
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

						target.monsterEffect!!.delayDisplay = animDuration
					}
				}

				val hitEffect = data["HITEFFECT", null]
				if (hitEffect is ParticleEffect)
				{
					val particle = hitEffect.copy()
					particle.renderDelay = animDuration

					animDuration += particle.lifetime / 2f
					target.monsterEffect!!.delayDisplay = animDuration

					target.effects.add(particle)
				}
			}
			if (effect == Effect.CUSTOMORB)
			{
				target.orb = Orb(Orb.getNamedOrb(data.get("NAME").toString()), grid.level.theme)
			}
			if (effect == Effect.SEAL || effect == Effect.SEALEDATTACK)
			{
				target.swappable?.sealCount = strength
			}
			if (effect == Effect.BLOCK)
			{
				target.block = Block(grid.level.theme)
				target.block!!.maxhp = strength

				val diff = target.getPosDiff(monster.tiles[0, 0])
				diff[0].y *= -1
				monster.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = target.euclideanDist(monster.tiles[0, 0])
				val animDuration = 0.4f + dst * 0.025f
				val attackSprite = target.block!!.sprite.copy()
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				target.effects.add(attackSprite)
			}
			if (effect == Effect.SUMMON)
			{
				var desc = data["MONSTERDESC", null] as? MonsterDesc
				if (desc == null)
				{
					val factionName = data["FACTION"].toString()
					val name = data["NAME"]?.toString() ?: ""

					val factionPath = XmlData.enumeratePaths("Factions", "Faction").first { it.toUpperCase().endsWith("$factionName.XML") }.split("Factions/")[1]

					val faction = Faction.load(factionPath)
					desc = if (name.isBlank()) faction.get(1) else faction.get(name)
				}

				val difficulty = data["DIFFICULTY", "0"].toString().toInt()

				val summoned = Monster(desc!!, difficulty)
				summoned.isSummon = data["ISSUMMON"].toString().toBoolean()

				summoned.setTile(target, grid)

				val spawnEffectEl = data["SPAWNEFFECT", null] as? XmlData
				if (spawnEffectEl != null)
				{
					val spawnEffect = AssetManager.loadParticleEffect(spawnEffectEl)
					target.effects.add(spawnEffect)
				}
			}
			if (effect == Effect.SPREADER)
			{
				val spreader = data["SPREADER"] as Spreader
				target.spreader = spreader.copy()
			}
		}
	}

	companion object
	{
		fun load(xml: XmlData) : MonsterAbility
		{
			val ability = MonsterAbility()

			val cooldown = xml.get("Cooldown").split(",")
			ability.cooldownMin = cooldown[0].toInt()
			ability.cooldownMax = cooldown[1].toInt()
			ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)

			ability.repeatable = xml.getBoolean("Repeatable", true)

			ability.target = Target.valueOf(xml.get("Target", "NEIGHBOUR")!!.toUpperCase())
			ability.targetCount = xml.getInt("Count", 1)

			ability.targetRestriction = Targetter(Targetter.Type.valueOf(xml.get("TargetRestriction", "Orb")!!.toUpperCase()))
			ability.permuter = Permuter(Permuter.Type.valueOf(xml.get("Permuter", "Single")!!.toUpperCase()))
			ability.effect = Effect.valueOf(xml.get("Effect", "Attack")!!.toUpperCase())

			val dEl = xml.getChildByName("Data")
			if (dEl != null)
			{
				for (i in 0 until dEl.childCount)
				{
					val el = dEl.getChild(i)

					if (el.name == "Spreader")
					{
						val spreader = Spreader.load(el)
						ability.data[el.name.toUpperCase()] = spreader
					}
					else if (el.name == "Debuff")
					{
						val buff = Buff.load(el)
						ability.data[el.name.toUpperCase()] = buff
					}
					else if (el.name == "SpawnEffect")
					{
						ability.data[el.name.toUpperCase()] = el
					}
					else if (el.name == "MonsterDesc")
					{
						ability.data[el.name.toUpperCase()] = MonsterDesc.load(el)
					}
					else if (el.name.contains("Effect"))
					{
						ability.data[el.name.toUpperCase()] = AssetManager.loadParticleEffect(el)
					}
					else if (el.name == "Sprite")
					{
						ability.data[el.name.toUpperCase()] = AssetManager.loadSprite(el)
					}
					else
					{
						ability.data[el.name.toUpperCase()] = el.text.toUpperCase()
					}
				}
			}

			return ability
		}
	}
}