package com.lyeeedar.Board

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Ability.Permuter
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class Friendly(val desc: FriendlyDesc) : Creature(desc.hp, desc.size, desc.sprite.copy(), desc.death.copy())
{
	val abilities: Array<FriendlyAbility> = Array()
	var isSummon: Boolean = false

	init
	{
		abilities.addAll(desc.abilities.map { it.copy() }.toGdxArray())
	}

	override fun onTurn(grid: Grid)
	{
		val border = getBorderTiles(grid)
		for (tile in border)
		{
			if (tile.orb != null && tile.orb!!.hasAttack && !tile.orb!!.sealed)
			{
				grid.pop(tile, 0f)
				hp -= 1

				val closest = tiles.minBy { tile.euclideanDist2(it) }
				closest!!.effects.add(grid.hitEffect.copy())
			}
		}

		for (ability in abilities)
		{
			ability.cooldownTimer--
			if (ability.cooldownTimer <= 0)
			{
				ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)
				ability.activate(this, grid)
			}
		}

		if (isSummon)
		{
			hp--
		}
	}

	companion object
	{
		fun load(path: String, isSummon: Boolean): Friendly
		{
			val friendly = Friendly(FriendlyDesc.load(path))
			friendly.isSummon = isSummon
			return friendly
		}
	}
}

class FriendlyDesc
{
	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect
	var size: Int = 1
	var hp: Int = 25
	val abilities: Array<FriendlyAbility> = Array()

	companion object
	{
		val files: ObjectMap<String, FileHandle> by lazy { loadFriendlies() }

		private fun loadFriendlies(): ObjectMap<String, FileHandle>
		{
			val rootPath = "Friendlies"
			var root = Gdx.files.internal(rootPath)
			if (!root.exists()) root = Gdx.files.absolute(rootPath)

			val out = ObjectMap<String, FileHandle>()

			for (f in root.list())
			{
				out[f.nameWithoutExtension().toUpperCase()] = f
			}

			return out
		}

		fun load(path: String): FriendlyDesc
		{
			val xml = getXml(files[path.toUpperCase()])

			val desc = FriendlyDesc()

			desc.sprite = AssetManager.loadSprite(xml.getChildByName("Sprite")!!)
			desc.death = AssetManager.loadParticleEffect(xml.getChildByName("Death")!!)

			desc.size = xml.getInt("Size", 1)
			desc.hp = xml.getInt("HP", 10)

			val abilitiesEl = xml.getChildByName("Abilities")
			if (abilitiesEl != null)
			{
				for (i in 0..abilitiesEl.childCount-1)
				{
					val el = abilitiesEl.getChild(i)
					val ability = FriendlyAbility.load(el)
					desc.abilities.add(ability)
				}
			}

			return desc
		}
	}
}

abstract class FriendlyAbility
{
	var cooldownTimer: Int = 0
	var cooldownMin: Int = 1
	var cooldownMax: Int = 1

	abstract fun activate(friendly: Friendly, grid: Grid)
	abstract fun parse(xml: XmlData)
	abstract fun copy(): FriendlyAbility

	companion object
	{
		fun load(xml: XmlData): FriendlyAbility
		{
			val ability = when(xml.name)
			{
				"Attack" -> AttackAbility()
				"Move" -> MoveAbility()
				else -> throw NotImplementedError()
			}

			val cooldown = xml.get("Cooldown").split(",")
			ability.cooldownMin = cooldown[0].toInt()
			ability.cooldownMax = cooldown[1].toInt()
			ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)

			ability.parse(xml)

			return ability
		}
	}
}

class AttackAbility : FriendlyAbility()
{
	var range: Int = 1
	var targets: Int = 1
	lateinit var targetter: Targetter
	lateinit var permuter: Permuter
	val data: ObjectMap<String, String> = ObjectMap()

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = friendly.getBorderTiles(grid, range)
		val validTargets = availableTargets.filter { targetter.isValid(it, data) }

		val chosen = validTargets.random(targets)
		val final = Array<Tile>()

		for (c in chosen)
		{
			for (t in permuter.permute(c, grid, data))
			{
				if (!final.contains(t))
				{
					final.add(t)
				}
			}
		}

		for (tile in final)
		{
			grid.pop(tile, 0f)
		}
	}

	override fun parse(xml: XmlData)
	{
		range = xml.getInt("Range", 1)
		targets = xml.getInt("Count", 1)

		targetter = Targetter(Targetter.Type.valueOf(xml.get("TargetRestriction", "Orb")!!.toUpperCase()))
		permuter = Permuter(Permuter.Type.valueOf(xml.get("Permuter", "Single")!!.toUpperCase()))

		val dEl = xml.getChildByName("Data")
		if (dEl != null)
		{
			for (el in dEl.children())
			{
				data[el.name.toUpperCase()] = el.text.toUpperCase()
			}
		}
	}

	override fun copy(): FriendlyAbility
	{
		val out = AttackAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.targets = targets
		out.targetter = targetter
		out.permuter = permuter
		out.data.putAll(data)

		return out
	}
}

class MoveAbility : FriendlyAbility()
{
	enum class Target
	{
		NEIGHBOUR,
		RANDOM
	}

	enum class Destination
	{
		ATTACK,
		MONSTER,
		BLOCK,
		RANDOM
	}

	lateinit var target: Target
	lateinit var destination: Destination

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = Array<Tile>()

		if (target == Target.NEIGHBOUR)
		{
			availableTargets.addAll(friendly.getBorderTiles(grid))
		}
		else if (target == Target.RANDOM)
		{
			availableTargets.addAll(grid.grid)
		}
		else
		{
			throw NotImplementedError()
		}

		fun isValid(t: Tile): Boolean
		{
			for (x in 0..friendly.size-1)
			{
				for (y in 0..friendly.size-1)
				{
					val tile = grid.tile(t.x + x, t.y + y) ?: return false

					if (tile.orb != tile.contents && tile.friendly != friendly)
					{
						return false
					}

					if (!tile.canHaveOrb)
					{
						return false
					}
				}
			}

			return true
		}

		val validTargets = availableTargets.filter(::isValid)
		val destinations = when (destination)
		{
			Destination.ATTACK -> grid.grid.filter { it.orb != null && it.orb!!.hasAttack }
			Destination.MONSTER -> grid.grid.filter { it.monster != null }
			Destination.BLOCK -> grid.grid.filter { it.block != null }
			Destination.RANDOM -> grid.grid.asSequence()
			else -> return
		}

		if (destinations.count() == 0) return

		var chosen: Tile? = null

		if (destination == Destination.RANDOM)
		{
			chosen = validTargets.asSequence().random()
		}
		else
		{
			var current = Int.MAX_VALUE
			for (t in destinations)
			{
				for (tile in friendly.tiles)
				{
					val dist = tile.dist(t)
					if (dist < current) current = dist
				}
			}

			var chosenMin = Int.MAX_VALUE
			for (target in validTargets)
			{
				for (t in destinations)
				{
					val dist = target.dist(t)
					if (dist < chosenMin)
					{
						chosenMin = dist
						chosen = target
					}
				}
			}
		}

		if (chosen != null)
		{
			val start = friendly.tiles.first()
			friendly.setTile(chosen, grid)
			val end = friendly.tiles.first()

			if (this.target == Target.RANDOM)
			{
				val dst = chosen.euclideanDist(friendly.tiles[0, 0])
				val animDuration = 0.25f + dst * 0.025f

				friendly.sprite.animation = LeapAnimation.obtain().set(0.25f, chosen.getPosDiff(friendly.tiles[0, 0]), 1f + dst * 0.25f)
				friendly.sprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			}
			else
			{
				friendly.sprite.animation = MoveAnimation.obtain().set(0.25f, UnsmoothedPath(end.getPosDiff(start)), Interpolation.linear)
			}
		}
	}

	override fun parse(xml: XmlData)
	{
		target = Target.valueOf(xml.get("Target", "NEIGHBOUR")!!.toUpperCase())
		destination = Destination.valueOf(xml.get("Destination", "RANDOM")!!.toUpperCase())
	}

	override fun copy(): FriendlyAbility
	{
		val out = MoveAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.target = target
		out.destination = destination

		return out
	}
}