package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.MonsterAI
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.AlphaAnimation
import com.lyeeedar.Statistic

class DeletionSystem : AbstractSystem(Family.all(MarkedForDeletionComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		deletedEntities.clear()

		for (entity in entities)
		{
			processEntity(entity, deltaTime)
		}
	}

	val deletedEntities = ObjectSet<Entity>()

	fun processEntity(entity: Entity, deltaTime: Float)
	{
		val grid = grid ?: return

		if (deletedEntities.contains(entity)) return
		var doRemove = true

		val pos = entity.posOrNull()
		val renderable = entity.renderableOrNull()

		val matchableComponent = entity.matchable()
		if (pos != null && renderable != null && matchableComponent != null)
		{
			if (renderable.renderable.animation == null)
			{
				val tile = pos.tile!!

				grid.onPop(entity, matchableComponent.deletionEffectDelay)

				if (matchableComponent.deletionEffectDelay >= 0.2f)
				{
					val sprite = renderable.renderable.copy()
					sprite.renderDelay = matchableComponent.deletionEffectDelay - 0.2f
					sprite.showBeforeRender = true
					sprite.animation = AlphaAnimation.obtain().set(0.2f, 1f, 0f, sprite.colour)
					tile.effects.add(sprite)
				}
			}
			else
			{
				doRemove = false
			}
		}

		val damageableComponent = entity.damageable()
		if (pos != null && damageableComponent != null)
		{
			val tile = pos.tile!!

			val death = damageableComponent.deathEffect.copy()
			death.size[0] = pos.size
			death.size[1] = pos.size

			tile.effects.add(death)

			if (damageableComponent.isCreature)
			{
				val necroticAura = Global.player.getStat(Statistic.NECROTICAURA)
				if (necroticAura != 0f)
				{
					val die = grid.level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()
					if (die != null)
					{
						die.fractionalHp += necroticAura
					}
				}

				val monsterAI = entity.ai()?.ai as? MonsterAI
				if (monsterAI != null)
				{
					val rootDesc = monsterAI.desc.originalDesc ?: monsterAI.desc
					if (rootDesc.stages.size > 0)
					{
						val currentStage = if (monsterAI.desc.originalDesc == null) -1 else rootDesc.stages.indexOf(monsterAI.desc)

						if (currentStage < rootDesc.stages.size - 1)
						{
							val nextDesc = rootDesc.stages[currentStage + 1]
							val monster = nextDesc.getEntity(monsterAI.difficulty, false)
							monster.pos().setTile(monster, tile)
						}
					}
				}
			}
		}

		val containerComponent = entity.container()
		if (pos != null && containerComponent != null && containerComponent.containedEntity != null)
		{
			val tile = pos.tile!!

			val centity = containerComponent.containedEntity!!
			centity.pos().tile = tile
			centity.pos().addToTile(centity)

			engine.addEntity(centity)
		}

		val special = entity.special()
		if (special != null)
		{
			if (special.special.armed)
			{
				doRemove = false
			}
		}

		val monsterEffect = entity.monsterEffect()
		if (pos != null && renderable != null && monsterEffect != null)
		{
			val tile = pos.tile!!

			if (renderable.renderable.animation == null)
			{
				monsterEffect.monsterEffect.apply(grid, tile)
			}
			else
			{
				doRemove = false
			}
		}

		if (doRemove)
		{
			pos?.removeFromTile(entity)
			engine.removeEntity(entity)
			entity.free()

			deletedEntities.add(entity)
		}
	}
}
