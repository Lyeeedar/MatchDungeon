package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics

class UpdateGridUpdateStep : AbstractUpdateStep()
{
	override fun doUpdate(grid: Grid): Boolean
	{
		return true
	}

	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{
		val trace = Statics.performanceTracer.getTrace("UpdateGridRealTime")
		trace.start()

		if (grid.queuedActions.size > 0)
		{
			for (action in grid.queuedActions)
			{
				if (grid.delayedActions.size == 0)
				{
					grid.delayedActions.add(action)
				}
				else if (action.delay <= grid.delayedActions.first.delay)
				{
					grid.delayedActions.addFirst(action)
				}
				else if (action.delay >= grid.delayedActions.last.delay)
				{
					grid.delayedActions.addLast(action)
				}
				else
				{
					val mid = (grid.delayedActions.first.delay + grid.delayedActions.last.delay) * 0.5f
					if (action.delay <= mid)
					{
						grid.delayedActions.addFirst(action)
					}
					else
					{
						grid.delayedActions.addLast(action)
					}
				}
			}
			grid.queuedActions.clear()
		}
		if (grid.delayedActions.size > 0)
		{
			val itr = grid.delayedActions.iterator()
			while (itr.hasNext())
			{
				val action = itr.next()

				action.delay -= deltaTime
				if (action.delay <= 0)
				{
					action.function.invoke(action.target)
					itr.remove()

					action.free()
				}
			}
		}

		grid.hasAnim = false
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]

				if (!grid.hasAnim)
				{
					for (effect in tile.effects)
					{
						if (effect is ParticleEffect)
						{
							if (effect.isShortened)
							{
								if (effect.time < effect.lifetime * 0.5f)
								{
									grid.hasAnim = true
									break
								}
							}
							else if (effect.blocked())
							{
								grid.hasAnim = true
								break
							}
							else if (effect.animation != null)
							{
								grid.hasAnim = true
								break
							}
						}
						else
						{
							grid.hasAnim = true
							break
						}
					}
				}

				val contents = tile.contents ?: continue

				if (tile == contents.pos()?.tile)
				{
					processEntityRealTime(contents, grid, deltaTime)
				}
			}
		}

		trace.stop()
	}

	private fun processEntityRealTime(entity: Entity, grid: Grid, deltaTime: Float)
	{
		val renderable = entity.renderable()

		val monsterEffect = entity.monsterEffect()
		if (renderable != null && monsterEffect != null)
		{
			if (!monsterEffect.monsterEffect.addedSprite && !Global.resolveInstantly)
			{
				monsterEffect.monsterEffect.delayDisplay -= deltaTime

				if (monsterEffect.monsterEffect.delayDisplay <= 0)
				{
					val newSprite = monsterEffect.monsterEffect.actualSprite.copy()

					val matchable = entity.matchable()
					if (matchable != null)
					{
						newSprite.colour = matchable.desc.sprite.colour
					}

					renderable.renderable = newSprite

					monsterEffect.monsterEffect.addedSprite = true
				}
			}
		}

		val damageableComponent = entity.damageable()
		if (damageableComponent != null && renderable != null)
		{
			if (damageableComponent.tookDamage && !Global.resolveInstantly)
			{
				damageableComponent.tookDamage = false

				renderable.renderable.animation = BlinkAnimation.obtain().set(Colour.RED, renderable.renderable.colour, 0.15f, true)
			}

			if (damageableComponent.hp <= 0 && !entity.isMarkedForDeletion())
			{
				entity.markForDeletion(0f, "died")
			}
		}

		val healableComponent = entity.healable()
		if (healableComponent != null && renderable != null)
		{
			if (healableComponent.tookDamage && !Global.resolveInstantly)
			{
				healableComponent.tookDamage = false

				renderable.renderable.animation = BlinkAnimation.obtain().set(Colour.RED, renderable.renderable.colour, 0.15f, true)
			}

			if (healableComponent.hp <= 0 && !entity.isMarkedForDeletion())
			{
				entity.markForDeletion(0f, "died")
			}
		}

		val special = entity.special()
		if (special != null)
		{
			if (special.special.needsArming)
			{
				special.special.needsArming = false
				special.special.setArmed(true, entity)
			}
		}

		if (!grid.hasAnim)
		{
			val swappable = entity.swappable()
			if (swappable != null && renderable?.renderable?.animation != null)
			{
				grid.hasAnim = true
			}
		}
	}

	val processedAIs = ObjectSet<AIComponent>()
	val processedSpreaders = ObjectSet<String>()

	val spreaderTiles = Array<Tile>()
	val aiTiles = Array<Tile>()
	val matchableTiles = Array<Tile>()
	val monsterEffectTiles = Array<Tile>()

	override fun doTurn(grid: Grid)
	{
		val trace = Statics.performanceTracer.getTrace("UpdateGridTurn")
		trace.start()

		processedAIs.clear()
		processedSpreaders.clear()

		spreaderTiles.clear()
		aiTiles.clear()
		matchableTiles.clear()
		monsterEffectTiles.clear()

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]

				val contents = tile.contents

				val spreader = tile.spreader
				val matchable = contents?.matchable()
				val ai = contents?.ai()
				val monsterEffect = contents?.monsterEffect()
				val damageable = contents?.damageable()
				val healable = contents?.healable()
				val special = contents?.special()

				if (spreader != null)
				{
					spreaderTiles.add(tile)
				}
				if (matchable != null)
				{
					matchableTiles.add(tile)
				}
				if (ai != null && tile == contents.pos()?.tile)
				{
					aiTiles.add(tile)
				}
				if (monsterEffect != null)
				{
					monsterEffectTiles.add(tile)
				}
				if (special != null)
				{
					if (special.special.armed && !contents.isMarkedForDeletion())
					{
						contents.markForDeletion(0f, "armed")
					}
				}

				if (damageable != null)
				{
					if (damageable.isSummon)
					{
						damageable.hp--
					}

					if (damageable.immuneCooldown > 0)
					{
						damageable.immuneCooldown--

						if (damageable.immuneCooldown == 0)
						{
							damageable.immune = false
						}
					}
				}

				if (healable != null && healable.isSummon)
				{
					healable.hp--
				}
			}
		}

		for (tile in aiTiles)
		{
			val ai = tile.contents!!.ai()
			if (ai != null && !processedAIs.contains(ai))
			{
				processedAIs.add(ai)

				ai.ai.onTurn(tile.contents!!, grid)
			}
		}

		for (tile in spreaderTiles)
		{
			val spreader = tile.spreader
			if (spreader != null)
			{
				if (!processedSpreaders.contains(spreader.data.nameKey) && spreader.data.spreads)
				{
					processedSpreaders.add(spreader.data.nameKey)

					spreader.spread(grid, tile)
				}

				spreader.onTurn(grid, tile)
			}
		}

		for (tile in matchableTiles)
		{
			val matchable = tile.contents?.matchable()
			if (matchable != null)
			{
				if (matchable.isChanger)
				{
					matchable.setDesc(matchable.nextDesc!!, tile.contents!!)
					matchable.nextDesc = OrbDesc.getRandomOrb(grid.level, matchable.desc)

					if (!Global.resolveInstantly)
					{
						val effect = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.05f, matchable.desc.sprite.colour)
						tile.effects.add(effect)
					}
				}
			}
		}

		for (tile in monsterEffectTiles)
		{
			val monsterEffect = tile.contents?.monsterEffect()
			if (monsterEffect != null)
			{
				monsterEffect.monsterEffect.timer--

				if (monsterEffect.monsterEffect.timer <= 0)
				{
					tile.contents!!.markForDeletion(0f, "timer expired")
				}
			}
		}

		trace.stop()
	}
}