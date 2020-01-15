package com.lyeeedar.Board

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.MonsterEffectComponent
import com.lyeeedar.Components.damageable
import com.lyeeedar.Components.isBasicOrb
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.random

class Spreader
{
	enum class SpreaderEffect
	{
		POP,
		SEAL,
		DAMAGE,
		ATTACK
	}

	lateinit var nameKey: String

	var spriteWrapper: SpriteWrapper? = null
	var particleEffect: ParticleEffect? = null

	lateinit var effect: SpreaderEffect

	var damage: Float = 0f

	var attackCooldownMin = 0
	var attackCooldownMax = 0
	var attackCooldown = 0
	var attackNumPips = 0

	var attackEffect: ParticleEffect? = null

	var spreads = true
	var renderAbove = true
	var fadeOut = -1

	fun copy(): Spreader
	{
		val out = Spreader()
		out.nameKey = nameKey
		out.spriteWrapper = spriteWrapper?.copy()
		out.particleEffect = particleEffect?.copy()

		out.spreads = spreads
		out.renderAbove = renderAbove
		out.fadeOut = fadeOut
		out.damage = damage
		out.effect = effect

		out.attackCooldownMin = attackCooldownMin
		out.attackCooldownMax = attackCooldownMax
		out.attackEffect = attackEffect
		out.attackNumPips = attackNumPips

		return out
	}

	fun spread(grid: Grid, tile: Tile)
	{
		// do spreading
		if (!grid.poppedSpreaders.contains(nameKey))
		{
			// spread

			// get borders tiles
			val border = ObjectSet<Tile>()
			for (t in grid.grid)
			{
				if (t.spreader != null && t.spreader!!.nameKey == nameKey)
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
				val chosenTile = border.asSequence().random(grid.ran)!!

				val newspreader = copy()

				if (newspreader.particleEffect != null && !Global.resolveInstantly)
				{
					newspreader.particleEffect!!.animation = ExpandAnimation.obtain().set(grid.animSpeed)
				}

				if (newspreader.spriteWrapper != null && !Global.resolveInstantly)
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

	fun onTurn(grid: Grid, tile: Tile)
	{
		if (fadeOut > 0)
		{
			fadeOut--
			if (fadeOut == 0)
			{
				tile.spreader = null
			}
		}

		// do on turn effects
		if (effect == SpreaderEffect.POP)
		{
			grid.pop(tile, 0f, this, damage, 0f, true)
		}
		else if (effect == SpreaderEffect.DAMAGE)
		{
			val damageable = tile.contents?.damageable()
			if (damageable != null)
			{
				grid.damage(tile, tile.contents!!, 0f, nameKey, damage)
			}
		}
		else if (effect == SpreaderEffect.ATTACK)
		{
			if (attackCooldown <= 0)
			{
				attackCooldown = attackCooldownMin + grid.ran.nextInt(attackCooldownMax - attackCooldownMin)
				attackCooldown += (attackCooldown * Global.player.getStat(Statistic.HASTE, true)).toInt()
			}

			attackCooldown--
			if (attackCooldown <= 0)
			{
				val attackedTile: Tile
				if (tile.contents?.isBasicOrb() == true)
				{
					attackedTile = tile
				}
				else
				{
					attackedTile = grid.grid.filter { it.contents?.isBasicOrb() == true }.minBy { it.dist(tile) } ?: return
				}

				val target = attackedTile.contents!!

				val attack = MonsterEffect(MonsterEffectType.ATTACK, ObjectMap())
				target.add(MonsterEffectComponent.obtain().set(attack))

				attack.timer = attackNumPips + (Global.player.getStat(Statistic.HASTE) * attackNumPips).toInt()
				val diff = attackedTile.getPosDiff(tile)
				diff[0].y *= -1

				if (!Global.resolveInstantly)
				{
					val dst = attackedTile.euclideanDist(tile)
					val animDuration = 0.4f + attackedTile.euclideanDist(tile) * 0.025f
					val attackSprite = attack.actualSprite.copy()
					attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
					attackedTile.effects.add(attackSprite)

					if (attackEffect != null)
					{
						val effect = attackEffect!!.copy()
						tile.effects.add(effect)
					}

					attack.delayDisplay = animDuration
				}
			}
		}
	}

	companion object
	{
		fun load(xmlData: XmlData): Spreader
		{
			val spreader = Spreader()

			spreader.nameKey = xmlData.get("Name")

			val spriteEl = xmlData.getChildByName("Sprite")
			if (spriteEl != null)
			{
				spreader.spriteWrapper = SpriteWrapper.load(spriteEl)
			}

			val particleEl = xmlData.getChildByName("ParticleEffect")
			if (particleEl != null)
			{
				spreader.particleEffect = AssetManager.loadParticleEffect(particleEl).getParticleEffect()
			}

			spreader.effect = SpreaderEffect.valueOf(xmlData.get("Effect", "seal")!!.toUpperCase())

			spreader.damage = xmlData.getFloat("Damage", 0f)

			spreader.spreads = xmlData.getBoolean("Spreads", true)
			spreader.renderAbove = xmlData.getBoolean("RenderAbove", true)
			spreader.fadeOut = xmlData.getInt("FadeOut", -1)

			spreader.attackCooldownMin = xmlData.getInt("AttackCooldownMin", 3)
			spreader.attackCooldownMax = xmlData.getInt("AttackCooldownMax", 10)
			spreader.attackNumPips = xmlData.getInt("AttackNumPips", 7)

			val attackEffectEl = xmlData.getChildByName("AttackEffect")
			if (attackEffectEl != null)
			{
				spreader.attackEffect = AssetManager.loadParticleEffect(attackEffectEl).getParticleEffect()
			}

			return spreader
		}
	}
}