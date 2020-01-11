package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.GridSystem
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random

class UpdateGridUpdateStep : AbstractUpdateStep()
{
	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		return true
	}

	val processedAIs = ObjectSet<AIComponent>()
	val processedSpreaders = ObjectSet<String>()

	val spreaderTiles = Array<Tile>()
	val aiTiles = Array<Tile>()
	val matchableTiles = Array<Tile>()
	val monsterEffectTiles = Array<Tile>()

	override fun doTurn(gridSystem: GridSystem)
	{
		val grid = gridSystem.grid!!

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

				val spreader = tile.spreader
				val matchable = tile.contents?.matchable()
				val ai = tile.contents?.ai()
				val monsterEffect = tile.contents?.monsterEffect()

				if (spreader != null)
				{
					spreaderTiles.add(tile)
				}
				if (matchable != null)
				{
					matchableTiles.add(tile)
				}
				if (ai != null && tile == tile.contents!!.pos().tile)
				{
					aiTiles.add(tile)
				}
				if (monsterEffect != null)
				{
					monsterEffectTiles.add(tile)
				}

				for (effect in tile.onTurnEffects)
				{
					effect.onTurn(grid, tile)
				}

				val onTurnEffect = tile.contents?.onTurnEffect()
				if (onTurnEffect != null)
				{
					for (effect in onTurnEffect.onTurnEffects)
					{
						effect.onTurn(grid, tile)
					}
				}
			}
		}

		for (tile in aiTiles)
		{
			val ai = tile.contents!!.ai()
			if (ai != null && !processedAIs.contains(ai))
			{
				processedAIs.add(ai)

				ai.ai.onTurn(grid)
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
						grid.damage(tile, damageable, 0f, spreader.nameKey, spreader.damage)
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
					matchable.desc = matchable.nextDesc!!
					matchable.nextDesc = Orb.getRandomOrb(grid.level, matchable.desc)

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
			}
		}
	}
}