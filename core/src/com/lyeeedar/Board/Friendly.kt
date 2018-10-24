package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class Friendly(val desc: FriendlyDesc) : Creature(desc.hp, desc.size, desc.sprite.copy(), desc.death.copy())
{
	val abilities: Array<FriendlyAbility> = Array()
	var isSummon: Boolean = false

	val sinkPathTiles = Array<Tile>()
	val monsterTiles = Array<Tile>()
	val blockTiles = Array<Tile>()
	val attackTiles = Array<Tile>()
	val namedOrbTiles = Array<Tile>()

	init
	{
		damageReduction = desc.dr
		abilities.addAll(desc.abilities.map { it.copy() }.toGdxArray())
	}

	override fun onTurn(grid: Grid)
	{
		sinkPathTiles.clear()
		monsterTiles.clear()
		blockTiles.clear()
		attackTiles.clear()
		namedOrbTiles.clear()

		val sinkables = Array<Tile>()
		for (tile in grid.grid)
		{
			if (tile.monster != null)
			{
				monsterTiles.add(tile)
			}
			else if (tile.block != null || tile.container != null)
			{
				blockTiles.add(tile)
			}
			else if (tile.monsterEffect != null)
			{
				attackTiles.add(tile)
			}
			else if (tile.sinkable != null)
			{
				sinkables.add(tile)
			}
			else if (tile.orb != null && Orb.isNamedOrb(tile.orb!!.desc))
			{
				namedOrbTiles.add(tile)
			}
		}

		for (sinkableTile in sinkables)
		{
			for (y in sinkableTile.y until grid.height)
			{
				sinkPathTiles.add(grid.tile(sinkableTile.x, y))
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
		fun load(xml: XmlData, isSummon: Boolean): Friendly
		{
			val friendly = Friendly(FriendlyDesc.load(xml))
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
	var dr: Int = 0
	val abilities: Array<FriendlyAbility> = Array()

	companion object
	{
		fun load(xml: XmlData): FriendlyDesc
		{
			val desc = FriendlyDesc()

			desc.sprite = AssetManager.loadSprite(xml.getChildByName("Sprite")!!)
			desc.death = AssetManager.loadParticleEffect(xml.getChildByName("Death")!!)

			desc.size = xml.getInt("Size", 1)
			desc.hp = xml.getInt("HP", 10)
			desc.dr = xml.getInt("DamageReduction", 0)

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

	var range: Int = 1

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
				"Break" -> BreakAbility()
				"Block" -> BlockAbility()
				"Pop" -> PopAbility()
				else -> throw NotImplementedError()
			}

			val cooldown = xml.get("Cooldown").split(",")
			ability.cooldownMin = cooldown[0].toInt()
			ability.cooldownMax = cooldown[1].toInt()
			ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)

			ability.range = xml.getInt("Range", 1)

			ability.parse(xml)

			return ability
		}
	}
}

class AttackAbility : FriendlyAbility()
{
	var targets: Int = 1
	var damage: Float = 0f
	var flightEffect: ParticleEffect? = null
	var hitEffect: ParticleEffect? = null

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = friendly.getBorderTiles(grid, range)
		val validTargets = availableTargets.filter { it.monster != null }

		val chosen = validTargets.random(targets).toList().toGdxArray()

		for (tile in chosen)
		{
			var delay = 0f

			if (flightEffect != null)
			{
				val diff = tile.getPosDiff(friendly.tiles[0, 0])
				diff[0].y *= -1
				friendly.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(friendly.tiles[0, 0])
				val animDuration = dst * 0.025f

				val particle = flightEffect!!.copy()
				particle.animation = MoveAnimation.obtain().set(animDuration, diff)
				particle.killOnAnimComplete = true

				tile.effects.add(particle)

				delay += animDuration
			}

			if (hitEffect != null)
			{
				val particle = hitEffect!!.copy()
				particle.renderDelay = delay

				delay += particle.lifetime / 2f

				tile.effects.add(particle)
			}

			grid.pop(tile, delay, bonusDam = damage, damSource = friendly)
		}
	}

	override fun parse(xml: XmlData)
	{
		range = xml.getInt("Range", 1)
		targets = xml.getInt("Count", 1)
		damage = xml.getFloat("Damage", 1f) - 1f

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null)
		{
			flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
		}

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null)
		{
			hitEffect = AssetManager.loadParticleEffect(hitEffectEl)
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
		out.damage = damage
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class BreakAbility : FriendlyAbility()
{
	var targets: Int = 1
	var damage: Float = 0f
	var flightEffect: ParticleEffect? = null
	var hitEffect: ParticleEffect? = null

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = friendly.getBorderTiles(grid, range)
		val validTargets = availableTargets.filter { it.block != null || it.container != null }

		val chosen = validTargets.random(targets).toList().toGdxArray()

		for (tile in chosen)
		{
			var delay = 0f

			if (flightEffect != null)
			{
				val diff = tile.getPosDiff(friendly.tiles[0, 0])
				diff[0].y *= -1
				friendly.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(friendly.tiles[0, 0])
				val animDuration = dst * 0.025f

				val particle = flightEffect!!.copy()
				particle.animation = MoveAnimation.obtain().set(animDuration, diff)
				particle.killOnAnimComplete = true

				tile.effects.add(particle)

				delay += animDuration
			}

			if (hitEffect != null)
			{
				val particle = hitEffect!!.copy()
				particle.renderDelay = delay

				delay += particle.lifetime / 2f

				tile.effects.add(particle)
			}

			grid.pop(tile, delay, bonusDam = damage, damSource = friendly)
		}
	}

	override fun parse(xml: XmlData)
	{
		range = xml.getInt("Range", 1)
		targets = xml.getInt("Count", 1)
		damage = xml.getFloat("Damage", 1f) - 1f

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null)
		{
			flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
		}

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null)
		{
			hitEffect = AssetManager.loadParticleEffect(hitEffectEl)
		}
	}

	override fun copy(): FriendlyAbility
	{
		val out = BreakAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.targets = targets
		out.damage = damage
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class BlockAbility : FriendlyAbility()
{
	var targets: Int = 1
	var flightEffect: ParticleEffect? = null
	var hitEffect: ParticleEffect? = null

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = friendly.getBorderTiles(grid, range)
		val validTargets = availableTargets.filter { it.monsterEffect != null }

		val chosen = validTargets.random(targets).toList().toGdxArray()

		for (tile in chosen)
		{
			var delay = 0f

			if (flightEffect != null)
			{
				val diff = tile.getPosDiff(friendly.tiles[0, 0])
				diff[0].y *= -1
				friendly.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(friendly.tiles[0, 0])
				val animDuration = dst * 0.025f

				val particle = flightEffect!!.copy()
				particle.animation = MoveAnimation.obtain().set(animDuration, diff)
				particle.killOnAnimComplete = true

				tile.effects.add(particle)

				delay += animDuration
			}

			if (hitEffect != null)
			{
				val particle = hitEffect!!.copy()
				particle.renderDelay = delay

				delay += particle.lifetime / 2f

				tile.effects.add(particle)
			}

			grid.pop(tile, delay)
		}
	}

	override fun parse(xml: XmlData)
	{
		range = xml.getInt("Range", 1)
		targets = xml.getInt("Count", 1)

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null)
		{
			flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
		}

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null)
		{
			hitEffect = AssetManager.loadParticleEffect(hitEffectEl)
		}
	}

	override fun copy(): FriendlyAbility
	{
		val out = BlockAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.targets = targets
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class PopAbility : FriendlyAbility()
{
	var targets: Int = 1
	var flightEffect: ParticleEffect? = null
	var hitEffect: ParticleEffect? = null

	override fun activate(friendly: Friendly, grid: Grid)
	{
		val availableTargets = friendly.getBorderTiles(grid, range).asGdxArray()

		for (r in 0 until targets)
		{
			val validTargets = Array<Tile>()

			for (target in availableTargets)
			{
				if (friendly.sinkPathTiles.contains(target) || friendly.namedOrbTiles.contains(target))
				{
					for (i in 0 until 5)
					{
						validTargets.add(target)
					}
				}
				else
				{
					validTargets.add(target)
				}
			}

			val tile = validTargets.random()!!
			availableTargets.removeValue(tile, true)

			var delay = 0f

			if (flightEffect != null)
			{
				val diff = tile.getPosDiff(friendly.tiles[0, 0])
				diff[0].y *= -1
				friendly.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

				val dst = tile.euclideanDist(friendly.tiles[0, 0])
				val animDuration = dst * 0.025f

				val particle = flightEffect!!.copy()
				particle.animation = MoveAnimation.obtain().set(animDuration, diff)
				particle.killOnAnimComplete = true

				tile.effects.add(particle)

				delay += animDuration
			}

			if (hitEffect != null)
			{
				val particle = hitEffect!!.copy()
				particle.renderDelay = delay

				delay += particle.lifetime / 2f

				tile.effects.add(particle)
			}

			grid.pop(tile, delay)
		}
	}

	override fun parse(xml: XmlData)
	{
		range = xml.getInt("Range", 1)
		targets = xml.getInt("Count", 1)

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null)
		{
			flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
		}

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null)
		{
			hitEffect = AssetManager.loadParticleEffect(hitEffectEl)
		}
	}

	override fun copy(): FriendlyAbility
	{
		val out = PopAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.targets = targets
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class MoveAbility : FriendlyAbility()
{
	override fun activate(friendly: Friendly, grid: Grid)
	{
		// check if there are any targets in range, if so stay put
		val srcTile = friendly.tiles[0, 0]

		for (ability in friendly.abilities)
		{
			when (ability)
			{
				is AttackAbility -> for (tile in friendly.monsterTiles)
				{
					if (tile.dist(srcTile) <= ability.range)
					{
						return
					}
				}
				is BreakAbility -> for (tile in friendly.blockTiles)
				{
					if (tile.dist(srcTile) <= ability.range)
					{
						return
					}
				}
				is BlockAbility -> for (tile in friendly.attackTiles)
				{
					if (tile.dist(srcTile) <= ability.range)
					{
						return
					}
				}
				is PopAbility ->
				{
					for (tile in friendly.sinkPathTiles)
					{
						if (tile.dist(srcTile) <= ability.range)
						{
							return
						}
					}

					for (tile in friendly.namedOrbTiles)
					{
						if (tile.dist(srcTile) <= ability.range)
						{
							return
						}
					}
				}
				is MoveAbility ->
				{

				}
				else -> throw Exception("Unknown friendly ability type!")
			}
		}

		// look within radius 2, if dest found move towards it, else widen search
		var searchRadius = 2
		val validTiles = Array<Tile>()

		while (true)
		{
			var possibleToHaveValidTiles = false

			for (ability in friendly.abilities)
			{
				when (ability)
				{
					is AttackAbility ->
					{
						if (friendly.monsterTiles.size > 0)
						{
							possibleToHaveValidTiles = true
						}

						for (tile in friendly.monsterTiles)
						{
							if (tile.dist(srcTile) <= searchRadius)
							{
								validTiles.add(tile)
							}
						}
					}
					is BreakAbility ->
					{
						if (friendly.blockTiles.size > 0)
						{
							possibleToHaveValidTiles = true
						}

						for (tile in friendly.blockTiles)
						{
							if (tile.dist(srcTile) <= searchRadius)
							{
								validTiles.add(tile)
							}
						}
					}
					is BlockAbility ->
					{
						if (friendly.attackTiles.size > 0)
						{
							possibleToHaveValidTiles = true
						}

						for (tile in friendly.attackTiles)
						{
							if (tile.dist(srcTile) <= searchRadius)
							{
								validTiles.add(tile)
							}
						}
					}
					is PopAbility ->
					{
						if (friendly.sinkPathTiles.size > 0 || friendly.namedOrbTiles.size > 0)
						{
							possibleToHaveValidTiles = true
						}

						for (tile in friendly.sinkPathTiles)
						{
							if (tile.dist(srcTile) <= searchRadius)
							{
								validTiles.add(tile)
							}
						}

						for (tile in friendly.namedOrbTiles)
						{
							if (tile.dist(srcTile) <= searchRadius)
							{
								validTiles.add(tile)
							}
						}
					}
					is MoveAbility ->
					{

					}
					else -> throw Exception("Unknown friendly ability type!")
				}
			}

			if (validTiles.size > 0)
			{
				break
			}
			else if (!possibleToHaveValidTiles)
			{
				break
			}

			searchRadius += 2
		}

		// if no valid tiles then move out of sinkables way, or randomly
		if (validTiles.size == 0)
		{
			val borderTiles = friendly.getBorderTiles(grid).filter { !friendly.sinkPathTiles.contains(it) }.asGdxArray()

			if (borderTiles.size > 0)
			{
				validTiles.add(borderTiles.random())
			}
		}
		else
		{
			// Weight tiles based on if they are in the path of a sinkable or not
			val tilesCopy = validTiles.toGdxArray()
			validTiles.clear()
			for (tile in tilesCopy)
			{
				if (!friendly.sinkPathTiles.contains(tile))
				{
					for (i in 0 until 3)
					{
						validTiles.add(tile)
					}
				}
				else
				{
					validTiles.add(tile)
				}
			}
		}

		// if still no valid, then bail
		if (validTiles.size == 0)
		{
			return
		}

		fun isValid(t: Tile): Boolean
		{
			for (x in 0 until friendly.size)
			{
				for (y in 0 until friendly.size)
				{
					val tile = grid.tile(t.x + x, t.y + y) ?: return false

					if (tile.orb == null && tile.friendly != friendly)
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

		// Remove invalid tiles
		val validTargets = validTiles.filter(::isValid).asGdxArray()

		if (validTargets.size == 0)
		{
			return
		}

		val chosen = validTargets.random()!!

		val borderTiles = friendly.getBorderTiles(grid, 1)
		val targetTile = borderTiles.minBy { it.dist(chosen) }!!

		val start = friendly.tiles.first()
		friendly.setTile(targetTile, grid)
		val end = friendly.tiles.first()

		friendly.sprite.animation = MoveAnimation.obtain().set(0.25f, UnsmoothedPath(end.getPosDiff(start)), Interpolation.linear)
	}

	override fun parse(xml: XmlData)
	{

	}

	override fun copy(): FriendlyAbility
	{
		val out = MoveAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)

		return out
	}
}