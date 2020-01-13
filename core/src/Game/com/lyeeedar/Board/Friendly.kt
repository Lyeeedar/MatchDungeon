package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.filter
import ktx.collections.toGdxArray
import ktx.math.minus

class FriendlyDesc
{
	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect
	var size: Int = 1
	var hp: Int = 15
	lateinit var ai: FriendlyAI

	fun getEntity(isSummon: Boolean): Entity
	{
		val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.FRIENDLY)

		val position = PositionComponent.obtain()
		position.size = size

		val renderable = RenderableComponent.obtain().set(sprite.copy())

		val healable = HealableComponent.obtain()
		healable.deathEffect = death.copy()
		healable.maxhp = hp
		healable.isSummon = isSummon

		val ai = AIComponent.obtain()
		ai.ai = this.ai.copy()

		val tutorialComponent = TutorialComponent.obtain()
		tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
			if (!Statics.settings.get("Friendly", false) )
			{
				val tutorial = Tutorial("Friendly")
				tutorial.addPopup("This is a friendly ally. Match in the surrounding tiles to replenish its health.", gridWidget.getRect(entity))
				return tutorial
			}

			return null
		}

		val entity = EntityPool.obtain()
		entity.add(archetype)
		entity.add(position)
		entity.add(renderable)
		entity.add(healable)
		entity.add(ai)
		entity.add(tutorialComponent)

		return entity
	}

	companion object
	{
		fun load(xml: XmlData): FriendlyDesc
		{
			val desc = FriendlyDesc()

			desc.sprite = AssetManager.loadSprite(xml.getChildByName("Sprite")!!)
			desc.death = AssetManager.loadParticleEffect(xml.getChildByName("Death")!!).getParticleEffect()

			desc.size = xml.getInt("Size", 1)
			desc.hp = xml.getInt("HP", 10)

			val abilitiesEl = xml.getChildByName("Abilities")!!
			desc.ai = FriendlyAI.load(abilitiesEl)

			return desc
		}
	}
}

class FriendlyAI(val abilities: Array<FriendlyAbility>) : AbstractGridAI()
{
	override fun onTurn(entity: Entity, grid: Grid)
	{
		for (ability in abilities.toGdxArray())
		{
			ability.cooldownTimer--
			if (ability.cooldownTimer <= 0)
			{
				ability.cooldownTimer = ability.cooldownMin + MathUtils.random(ability.cooldownMax - ability.cooldownMin)
				ability.activate(entity, grid)
			}
		}
	}

	fun copy(): FriendlyAI
	{
		return FriendlyAI(abilities.map { it.copy() }.toGdxArray())
	}

	companion object
	{
		fun load(xml: XmlData): FriendlyAI
		{
			val abilities = Array<FriendlyAbility>()

			for (i in 0 until xml.childCount)
			{
				val el = xml.getChild(i)
				val ability = FriendlyAbility.load(el)
				abilities.add(ability)
			}

			return FriendlyAI(abilities)
		}
	}
}

abstract class FriendlyAbility
{
	var cooldownTimer: Int = 0
	var cooldownMin: Int = 1
	var cooldownMax: Int = 1

	var range: Int = 1

	abstract fun activate(entity: Entity, grid: Grid)
	abstract fun parse(xml: XmlData)
	abstract fun copy(): FriendlyAbility

	companion object
	{
		fun load(xml: XmlData): FriendlyAbility
		{
			val ability = when(xml.name)
			{
				"Attack" -> FriendlyAttackAbility()
				"Block" -> FriendlyBlockAbility()
				"Pop" -> FriendlyPopAbility()
				"Heal" -> FriendlyHealAbility()
				"Move" -> FriendlyMoveAbility()
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

abstract class FriendlyPopTileAbility : FriendlyAbility()
{
	var damage: Float = 0f
	var flightEffect: ParticleEffect? = null
	var hitEffect: ParticleEffect? = null

	abstract fun getTargets(entity: Entity, grid: Grid): Array<Tile>

	override fun activate(entity: Entity, grid: Grid)
	{
		val validTargets = getTargets(entity, grid).filter { entity.pos().position.taxiDist(it) <= range }

		val tile = validTargets.random() ?: return
		val srcTile = entity.pos().tile!!

		var delay = 0f

		val diff = tile.getPosDiff(srcTile)
		diff[0].y *= -1
		entity.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, diff)

		if (flightEffect != null)
		{
			val dst = tile.euclideanDist(srcTile)
			val animDuration = dst * 0.025f

			val particle = flightEffect!!.copy()
			particle.animation = MoveAnimation.obtain().set(animDuration, diff)
			particle.killOnAnimComplete = true

			particle.rotation = getRotation(srcTile, tile)

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

		grid.pop(tile, delay, bonusDam = damage, damSource = entity)
	}

	override fun parse(xml: XmlData)
	{
		damage = xml.getFloat("Damage", 1f) - 1f

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null)
		{
			flightEffect = AssetManager.loadParticleEffect(flightEffectEl).getParticleEffect()
		}

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null)
		{
			hitEffect = AssetManager.loadParticleEffect(hitEffectEl).getParticleEffect()
		}
	}
}

class FriendlyAttackAbility : FriendlyPopTileAbility()
{
	override fun getTargets(entity: Entity, grid: Grid): Array<Tile>
	{
		return grid.monsterTiles
	}

	override fun copy(): FriendlyAbility
	{
		val out = FriendlyAttackAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.damage = damage
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class FriendlyBlockAbility : FriendlyPopTileAbility()
{
	override fun getTargets(entity: Entity, grid: Grid): Array<Tile>
	{
		return grid.attackTiles
	}

	override fun copy(): FriendlyAbility
	{
		val out = FriendlyBlockAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class FriendlyPopAbility : FriendlyPopTileAbility()
{
	val tmpArray = Array<Tile>()

	override fun getTargets(entity: Entity, grid: Grid): Array<Tile>
	{
		tmpArray.clear()

		tmpArray.addAll(grid.sinkPathTiles)
		tmpArray.addAll(grid.breakableTiles)
		tmpArray.addAll(grid.attackTiles)
		tmpArray.addAll(grid.namedOrbTiles)

		return tmpArray
	}

	override fun copy(): FriendlyAbility
	{
		val out = FriendlyPopAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.flightEffect = flightEffect
		out.hitEffect = hitEffect

		return out
	}
}

class FriendlyHealAbility : FriendlyAbility()
{
	val heartSprite = AssetManager.loadSprite("Oryx/Custom/items/heart")
	var amount: Int = 1

	override fun activate(entity: Entity, grid: Grid)
	{
		val srcPos = entity.pos().tile!!

		for (tile in grid.friendlyTiles)
		{
			val friendly = tile.contents
			val healable = friendly?.healable() ?: continue

			val diff = tile.getPosDiff(srcPos)
			diff[0].y *= -1
			entity.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, diff)

			val dst = tile.euclideanDist(srcPos)
			var animDuration = dst * 0.025f

			animDuration += 0.4f
			val heartSprite = heartSprite.copy()
			heartSprite.colour = Colour.GREEN
			heartSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			heartSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			tile.effects.add(heartSprite)

			val healEffect = AssetManager.loadParticleEffect("Heal").getParticleEffect()
			healEffect.colour = Colour.GREEN
			healEffect.renderDelay = animDuration
			tile.effects.add(healEffect)


			Future.call({ healable.hp++ }, animDuration)
		}

		for (condition in grid.level.defeatConditions)
		{
			if (condition is CompletionConditionDie)
			{
				val sprite = heartSprite.copy()
				val dst = condition.table.localToStageCoordinates(Vector2(Random.random() * condition.table.width, Random.random() * condition.table.height))
				val moteDst = dst.cpy() - Vector2(GridWidget.instance.tileSize / 2f, GridWidget.instance.tileSize / 2f)
				val src = GridWidget.instance.pointToScreenspace(srcPos)

				Mote(src, moteDst, sprite, GridWidget.instance.tileSize,
					 {
						 condition.fractionalHp += 1f
						 condition.updateFractionalHp()
					 }, animSpeed = 0.35f, leap = true)
			}
		}
	}

	override fun parse(xml: XmlData)
	{
		amount = xml.getInt("Amount", 1)
	}

	override fun copy(): FriendlyAbility
	{
		val out = FriendlyHealAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)
		out.range = range
		out.amount = amount

		return out
	}
}

class FriendlyMoveAbility : FriendlyAbility()
{
	val tmpArray = Array<Tile>()

	override fun activate(entity: Entity, grid: Grid)
	{
		val srcPos = entity.pos().tile!!
		var targetTile: Tile? = null

		// move out of sinkable way
		if (grid.sinkPathTiles.contains(srcPos) && grid.notSinkPathTiles.size > 0)
		{
			targetTile = grid.notSinkPathTiles.minBy { it.taxiDist(srcPos) }
		}

		// else move to closest target
		if (targetTile == null)
		{
			tmpArray.clear()

			val parentAI = entity.ai()?.ai as FriendlyAI
			for (ability in parentAI.abilities)
			{
				if (ability is FriendlyPopTileAbility)
				{
					val targets = ability.getTargets(entity, grid)

					if (targets.any { it.taxiDist(srcPos) <= ability.range })
					{
						// we are close enough, return
						return
					}

					tmpArray.addAll(targets)
				}
			}

			targetTile = tmpArray.minBy { it.taxiDist(srcPos) }
		}

		if (targetTile == null)
		{
			return
		}

		val dir = Direction.getDirection(srcPos, targetTile)

		val nextTile = grid.getTile(srcPos, dir)

		if (nextTile != null && nextTile.canHaveOrb && nextTile.contents?.matchable() != null && nextTile.contents?.special() == null)
		{
			entity.pos().removeFromTile(entity)
			entity.pos().tile = nextTile
			entity.pos().addToTile(entity)

			val diff = nextTile.getPosDiff(srcPos)
			diff[0].y *= -1

			entity.renderable().renderable.animation = MoveAnimation.obtain().set(0.25f, UnsmoothedPath(diff), Interpolation.linear)
		}
	}

	override fun parse(xml: XmlData)
	{

	}

	override fun copy(): FriendlyAbility
	{
		val out = FriendlyMoveAbility()
		out.cooldownMin = cooldownMin
		out.cooldownMax = cooldownMax
		out.cooldownTimer = out.cooldownMin + MathUtils.random(out.cooldownMax - out.cooldownMin)

		return out
	}
}