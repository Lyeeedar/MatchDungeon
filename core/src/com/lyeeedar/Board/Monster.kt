package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Permuter
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

/**
 * Created by Philip on 22-Jul-16.
 */

class Monster(val desc: MonsterDesc) : Creature(desc.hp, desc.size, desc.sprite.copy(), desc.death.copy())
{
	var isSummon = false

	var atkCooldown = 0

	val abilities = Array<MonsterAbility>()

	init
	{
		abilities.addAll(desc.abilities.map{ it.copy() }.toGdxArray())
		damageReduction = desc.damageReduction

		if (desc.attackNumPips > 0)
		{
			atkCooldown = (MathUtils.random() * desc.attackCooldown.max).toInt()
		}
		else
		{
			atkCooldown = Int.MAX_VALUE
		}
	}

	override fun onTurn(grid: Grid)
	{
		atkCooldown--
		if (atkCooldown <= 0)
		{
			atkCooldown = desc.attackCooldown.lerp()

			// do attack
			val tile = grid.grid.filter { validAttack(grid, it) }.random()

			if (tile != null)
			{
				val startTile = tiles.minBy { it.dist(tile) }!!

				tile.orb!!.attackTimer = desc.attackNumPips
				val diff = tile.getPosDiff(startTile)
				diff[0].y *= -1
				sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(startTile)
				val animDuration = 0.4f + tile.euclideanDist(startTile) * 0.025f
				val attackSprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_small", drawActualSize = true)
				attackSprite.colour = tile.orb!!.sprite.colour
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				tile.effects.add(attackSprite)

				tile.orb!!.delayDisplayAttack = animDuration
			}
		}

		for (ability in abilities)
		{
			ability.cooldownTimer--
			if (ability.cooldownTimer <= 0)
			{
				ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)
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
	if (tile.orb == null) return false
	if (tile.orb!!.special != null) return false
	if (tile.orb!!.hasAttack) return false

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
		SHIELD,
		SEAL,
		BLOCK,
		MOVE,
		HEAL,
		SUMMON
	}

	var cooldownTimer: Int = 0
	var cooldownMin: Int = 1
	var cooldownMax: Int = 1
	lateinit var target: Target
	lateinit var targetRestriction: Targetter
	var targetCount: Int = 1
	lateinit var permuter: Permuter
	lateinit var effect: Effect
	val data = ObjectMap<String, String>()

	fun copy(): MonsterAbility
	{
		val ability = MonsterAbility()
		ability.cooldownTimer = cooldownTimer
		ability.cooldownMin = cooldownMin
		ability.cooldownMax = cooldownMax
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
		println("Monster trying to use ability '$effect'")

		if (effect == Effect.HEAL)
		{
			monster.hp += data["AMOUNT"].toInt()
			if (monster.hp > monster.maxhp) monster.hp = monster.maxhp

			val sprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f, Colour(0f,1f,0f,1f))
			sprite.size[0] = monster.size
			sprite.size[1] = monster.size

			monster.tiles[0, 0].effects.add(sprite)

			return
		}

		val availableTargets = Array<Tile>()

		if (target == Target.NEIGHBOUR)
		{
			if (effect == Effect.MOVE)
			{
				val range = data["RANGE", "1"].toInt()
				availableTargets.addAll(grid.grid.filter { it != monster.tiles[0,0] && it.taxiDist(monster.tiles[0, 0]) <= range })
			}
			else
			{
				availableTargets.addAll(monster.getBorderTiles(grid, data["RANGE", "1"].toInt()))
			}
		}
		else if (target == Target.RANDOM)
		{
			availableTargets.addAll(grid.grid.filter { !monster.tiles.contains(it) })
		}
		else
		{
			throw NotImplementedError()
		}

		var validTargets = availableTargets.filter { targetRestriction.isValid(it, data) }

		if (targetRestriction.type == Targetter.Type.ORB && (effect == Effect.ATTACK || effect == Effect.SEALEDATTACK))
		{
			validTargets = validTargets.filter { validAttack(grid, it) }
		}

		val chosen = validTargets.asSequence().random(targetCount)

		val finalTargets = Array<Tile>()

		for (target in chosen)
		{
			for (t in permuter.permute(target, grid, data))
			{
				if (!finalTargets.contains(t, true))
				{
					finalTargets.add(t)
				}
			}
		}

		if (effect == Effect.MOVE)
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

							if (tile.orb != null && tile.orb!!.special != null)
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
				val animDuration = 0.25f + dst * 0.025f

				val start = monster.tiles.first()
				monster.setTile(target, grid, animDuration - 0.1f)
				val end = monster.tiles.first()

				val diff = end.getPosDiff(start)
				diff[0].y *= -1

				if (this.target == Target.RANDOM)
				{
					monster.sprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					monster.sprite.animation = ExpandAnimation.obtain().set(animDuration, 1f, 2f, false)
				}
				else
				{
					monster.sprite.animation = MoveAnimation.obtain().set(animDuration, UnsmoothedPath(diff), Interpolation.linear)
				}
			}

			return
		}

		for (target in finalTargets)
		{
			val strength = data.get("STRENGTH", "1").toInt()

			if (effect == Effect.ATTACK || effect == Effect.SEALEDATTACK)
			{
				val speed = data.get("SPEED", monster.desc.attackNumPips.toString()).toInt()

				if (target.orb == null)
				{
					target.effects.add(grid.hitEffect.copy())
					target.orb = Orb(Orb.getRandomOrb(grid.level), grid.level.theme)
				}

				target.orb!!.attackTimer = speed
				val diff = target.getPosDiff(monster.tiles[0, 0])
				diff[0].y *= -1
				monster.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = target.euclideanDist(monster.tiles[0, 0])
				val animDuration = 0.4f + dst * 0.025f
				val attackSprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_small", drawActualSize = true)
				attackSprite.colour = target.orb!!.sprite.colour
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				target.effects.add(attackSprite)

				target.orb!!.delayDisplayAttack = animDuration
			}
			if (effect == Effect.SHIELD)
			{
				target.shield = Shield(grid.level.theme)
				target.shield!!.count = strength
			}
			if (effect == Effect.SEAL || effect == Effect.SEALEDATTACK)
			{
				target.swappable?.sealCount = strength
			}
			if (effect == Effect.MOVE)
			{

			}
			if (effect == Effect.BLOCK)
			{
				target.block = Block(grid.level.theme)
				target.block!!.count = strength
			}
			if (effect == Effect.SUMMON)
			{
				val factionName = data["FACTION"]
				val name = data["NAME"]

				val faction = Faction.load(factionName)
				val summoned = Monster(faction.get(name)!!)
				summoned.isSummon = true

				summoned.setTile(target, grid)
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

			ability.target = Target.valueOf(xml.get("Target", "NEIGHBOUR")!!.toUpperCase())
			ability.targetCount = xml.getInt("Count", 1)

			ability.targetRestriction = Targetter(Targetter.Type.valueOf(xml.get("TargetRestriction", "Orb")!!.toUpperCase()))
			ability.permuter = Permuter(Permuter.Type.valueOf(xml.get("Permuter", "Single")!!.toUpperCase()))
			ability.effect = Effect.valueOf(xml.get("Effect", "Attack")!!.toUpperCase())

			val dEl = xml.getChildByName("Data")
			if (dEl != null)
			{
				for (i in 0..dEl.childCount-1)
				{
					val el = dEl.getChild(i)
					ability.data[el.name.toUpperCase()] = el.text.toUpperCase()
				}
			}

			return ability
		}
	}
}