package com.lyeeedar.Board.GridUpdate

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random

class UpdateGridUpdateStep : AbstractUpdateStep()
{
	override fun doUpdate(grid: Grid): Boolean
	{
		return true
	}

	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{
		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val contents = tile.contents ?: continue

				if (tile == contents.pos().tile)
				{
					processEntityRealTime(contents, grid, deltaTime)
				}

				if (tile.delayedActions.size > 0)
				{
					val itr = tile.delayedActions.iterator()
					while (itr.hasNext())
					{
						val action = itr.next()
						action.delay -= deltaTime

						if (action.delay <= 0)
						{
							action.function.invoke()
							itr.remove()
						}
					}
				}
			}
		}
	}

	private fun processEntityRealTime(entity: Entity, grid: Grid, deltaTime: Float)
	{
		val renderable = entity.renderableOrNull()

		val monsterEffect = entity.monsterEffect()
		if (renderable != null && monsterEffect != null)
		{
			if (!monsterEffect.monsterEffect.addedSprite)
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
			if (damageableComponent.tookDamage)
			{
				damageableComponent.tookDamage = false

				renderable.renderable.animation = BlinkAnimation.obtain().set(Colour.RED, renderable.renderable.colour, 0.15f, true)
			}

			if (damageableComponent.hp <= 0 && !entity.isMarkedForDeletion())
			{
				entity.add(MarkedForDeletionComponent.obtain())
			}
		}

		val healableComponent = entity.healable()
		if (healableComponent != null && renderable != null)
		{
			if (healableComponent.tookDamage)
			{
				healableComponent.tookDamage = false

				renderable.renderable.animation = BlinkAnimation.obtain().set(Colour.RED, renderable.renderable.colour, 0.15f, true)
			}

			if (healableComponent.hp <= 0 && !entity.isMarkedForDeletion())
			{
				entity.add(MarkedForDeletionComponent.obtain())
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
	}

	val processedAIs = ObjectSet<AIComponent>()
	val processedSpreaders = ObjectSet<String>()

	val spreaderTiles = Array<Tile>()
	val aiTiles = Array<Tile>()
	val matchableTiles = Array<Tile>()
	val monsterEffectTiles = Array<Tile>()

	override fun doTurn(grid: Grid)
	{
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
				if (ai != null && tile == contents.pos().tile)
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
						contents.add(MarkedForDeletionComponent.obtain())
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
				// do spreading
				if (!processedSpreaders.contains(spreader.nameKey) && spreader.spreads)
				{
					processedSpreaders.add(spreader.nameKey)

					if (!grid.poppedSpreaders.contains(spreader.nameKey))
					{
						// spread

						// get borders tiles
						val border = ObjectSet<Tile>()
						for (t in grid.grid)
						{
							if (t.spreader != null && t.spreader!!.nameKey == spreader.nameKey)
							{
								for (dir in Direction.CardinalValues)
								{
									val nt = grid.tile(t + dir) ?: continue

									if (nt.spreader == null && nt.canHaveOrb)
									{
										border.add(nt)
									}
								}
							}
						}

						// select random
						if (border.size > 0)
						{
							val chosenTile = border.asSequence().random()!!

							val newspreader = spreader.copy()

							if (newspreader.particleEffect != null)
							{
								newspreader.particleEffect!!.animation = ExpandAnimation.obtain().set(grid.animSpeed)
							}

							if (newspreader.spriteWrapper != null)
							{
								if (newspreader.spriteWrapper!!.sprite != null)
								{
									newspreader.spriteWrapper!!.sprite!!.animation = ExpandAnimation.obtain().set(grid.animSpeed)
								}

								if (newspreader.spriteWrapper!!.tilingSprite != null)
								{
									newspreader.spriteWrapper!!.tilingSprite!!.animation = ExpandAnimation.obtain().set(grid.animSpeed)
								}
							}

							chosenTile.spreader = newspreader
						}
					}
				}

				// do fade
				if (spreader.fadeOut > 0)
				{
					spreader.fadeOut--
					if (spreader.fadeOut == 0)
					{
						tile.spreader = null
					}
				}

				// do on turn effects
				if (spreader.effect == Spreader.SpreaderEffect.POP)
				{
					grid.pop(tile, 0f, spreader, spreader.damage, 0f, true)
				}
				else if (spreader.effect == Spreader.SpreaderEffect.DAMAGE)
				{
					val damageable = tile.contents?.damageable()
					if (damageable != null)
					{
						grid.damage(tile, tile.contents!!, 0f, spreader.nameKey, spreader.damage)
					}
				}
				else if (spreader.effect == Spreader.SpreaderEffect.ATTACK)
				{
					if (spreader.attackCooldown <= 0)
					{
						spreader.attackCooldown = spreader.attackCooldownMin + ((spreader.attackCooldownMax - spreader.attackCooldownMin) * Random.random()).toInt()
						spreader.attackCooldown += (spreader.attackCooldown * Global.player.getStat(Statistic.HASTE, true)).toInt()
					}

					spreader.attackCooldown--
					if (spreader.attackCooldown <= 0)
					{
						val attackedTile: Tile
						if (tile.contents?.matchable() != null)
						{
							attackedTile = tile
						}
						else
						{
							attackedTile = grid.grid.filter { it.contents?.matchable() != null }.minBy { it.dist(tile) } ?: continue
						}

						val target = attackedTile.contents!!

						val attack = MonsterEffect(MonsterEffectType.ATTACK, ObjectMap())
						target.add(MonsterEffectComponent.obtain().set(attack))

						attack.timer = spreader.attackNumPips + (Global.player.getStat(Statistic.HASTE) * spreader.attackNumPips).toInt()
						val diff = attackedTile.getPosDiff(tile)
						diff[0].y *= -1

						val dst = attackedTile.euclideanDist(tile)
						val animDuration = 0.4f + attackedTile.euclideanDist(tile) * 0.025f
						val attackSprite = attack.actualSprite.copy()
						attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
						attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
						tile.effects.add(attackSprite)

						if (spreader.attackEffect != null)
						{
							val effect = spreader.attackEffect!!.copy()
							tile.effects.add(effect)
						}
					}
				}
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

					val effect = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.05f, matchable.desc.sprite.colour)
					tile.effects.add(effect)
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
					tile.contents!!.add(MarkedForDeletionComponent.obtain())
				}
			}
		}
	}
}