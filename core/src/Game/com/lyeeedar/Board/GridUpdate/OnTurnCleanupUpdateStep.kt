package com.lyeeedar.Board.GridUpdate

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.MonsterAI
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.AlphaAnimation
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import ktx.collections.toGdxArray

class OnTurnCleanupUpdateStep : AbstractUpdateStep()
{
	override fun doUpdateRealTile(grid: Grid, deltaTime: Float)
	{
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val contents = tile.contents ?: continue

				if (tile == contents.pos().tile && contents.isMarkedForDeletion())
				{
					processDeletion(contents, grid, deltaTime)
				}
			}
		}
	}

	private fun processDeletion(entity: Entity, grid: Grid, deltaTime: Float)
	{
		var doRemove = true

		val pos = entity.posOrNull()
		val renderable = entity.renderableOrNull()

		val damageableComponent = entity.damageable()
		if (pos != null && damageableComponent != null && doRemove)
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
		if (pos != null && containerComponent != null && containerComponent.containedEntity != null && doRemove)
		{
			val tile = pos.tile!!

			val centity = containerComponent.containedEntity!!
			centity.pos().tile = tile
			centity.pos().addToTile(centity)
		}

		val special = entity.special()
		if (special != null && doRemove)
		{
			if (special.special.armed)
			{
				doRemove = false
			}
		}

		val monsterEffect = entity.monsterEffect()
		if (pos != null && renderable != null && monsterEffect != null && doRemove)
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

		val matchableComponent = entity.matchable()
		if (pos != null && renderable != null && matchableComponent != null && doRemove)
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

		if (doRemove)
		{
			pos?.removeFromTile(entity)
			entity.free()
		}
	}

	override fun doUpdate(grid: Grid): Boolean
	{
		grid.animSpeedMultiplier = 1f

		return true
	}

	override fun doTurn(grid: Grid)
	{
		for (tile in grid.grid)
		{
			if (tile.contents != null)
			{
				val damageableComponent = tile.contents!!.damageable()
				val positionComponent = tile.contents!!.pos()

				if (damageableComponent != null && positionComponent.tile == tile)
				{
					damageableComponent.damSources.clear()
					damageableComponent.remainingReduction = damageableComponent.damageReduction
				}
			}
		}

		for (buff in Global.player.levelbuffs.toGdxArray())
		{
			buff.remainingDuration--
			if (buff.remainingDuration <= 0)
			{
				Global.player.levelbuffs.removeValue(buff, true)
			}
		}

		for (debuff in Global.player.leveldebuffs.toGdxArray())
		{
			debuff.remainingDuration--
			if (debuff.remainingDuration <= 0)
			{
				Global.player.leveldebuffs.removeValue(debuff, true)
			}
		}

		GridScreen.instance.updateBuffTable()

		grid.gainedBonusPower = false
		grid.poppedSpreaders.clear()
		grid.animSpeedMultiplier = 1f
		grid.matchCount = 0
		grid.noMatchTimer = 0f
		grid.inTurn = false
	}
}