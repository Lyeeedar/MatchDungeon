package com.lyeeedar.Board

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.damageable
import com.lyeeedar.Components.isBasicOrb
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import java.util.*

enum class SpreaderEffect
{
	POP,
	SEAL,
	DAMAGE,
	ATTACK
}

@DataClass(name = "SpreaderDef", global = true)
class SpreaderData : XmlDataClass()
{
	@DataValue(dataName = "Name")
	lateinit var nameKey: String
	var spriteWrapper: SpriteWrapper? = null
	var particleEffect: ParticleEffect? = null
	lateinit var effect: SpreaderEffect
	
	@DataValue(visibleIf = "Effect == POP || Effect == Damage")
	var damage: Float = 0f
	
	@DataValue(visibleIf = "Effect == Attack")
	@NumericRange(min = 3f)
	var attackCooldownMin: Int = 3
	
	@DataValue(visibleIf = "Effect == Attack")
	@NumericRange(min = 3f)
	var attackCooldownMax: Int = 10
	
	@DataValue(visibleIf = "Effect == Attack")
	@NumericRange(min = 3f)
	var attackNumPips: Int = 7
	
	@DataValue(visibleIf = "Effect == Attack")
	var attackEffect: ParticleEffect? = null
	var spreads: Boolean = true
	var renderAbove: Boolean = true
	
	@NumericRange(min = -1f)
	var fadeOut: Int = -1

	override fun load(xmlData: XmlData)
	{
		nameKey = xmlData.get("Name")
		spriteWrapper = AssetManager.tryLoadSpriteWrapper(xmlData.getChildByName("SpriteWrapper"))
		particleEffect = AssetManager.tryLoadParticleEffect(xmlData.getChildByName("ParticleEffect"))?.getParticleEffect()
		effect = SpreaderEffect.valueOf(xmlData.get("Effect").toUpperCase(Locale.ENGLISH))
		damage = xmlData.getFloat("Damage", 0f)
		attackCooldownMin = xmlData.getInt("AttackCooldownMin", 3)
		attackCooldownMax = xmlData.getInt("AttackCooldownMax", 10)
		attackNumPips = xmlData.getInt("AttackNumPips", 7)
		attackEffect = AssetManager.tryLoadParticleEffect(xmlData.getChildByName("AttackEffect"))?.getParticleEffect()
		spreads = xmlData.getBoolean("Spreads", true)
		renderAbove = xmlData.getBoolean("RenderAbove", true)
		fadeOut = xmlData.getInt("FadeOut", -1)
	}
}

class Spreader
{
	lateinit var data: SpreaderData
	var fadeOut = -1
	var attackCooldown = 0
	var spriteWrapper: SpriteWrapper? = null
	var particleEffect: ParticleEffect? = null

	fun copy(): Spreader
	{
		val out = Spreader()
		out.data = data
		out.spriteWrapper = data.spriteWrapper?.copy()
		out.particleEffect = data.particleEffect?.copy()
		out.fadeOut = data.fadeOut
		out.attackCooldown = data.attackCooldownMax

		return out
	}

	fun spread(grid: Grid, tile: Tile)
	{
		// do spreading
		if (!grid.poppedSpreaders.contains(data.nameKey))
		{
			// spread

			// get borders tiles
			val border = ObjectSet<Tile>()
			for (t in grid.grid)
			{
				if (t.spreader != null && t.spreader!!.data.nameKey == data.nameKey)
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
				val chosenTile = border.asSequence().sortedBy { it.toShortString() }.random(grid.ran)!!

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

				grid.replay.logAction("spreader ${data.nameKey} spreading to (${chosenTile.toShortString()})")
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
		if (data.effect == SpreaderEffect.POP)
		{
			grid.pop(tile, 0f, this, data.damage, 0f, true)
		}
		else if (data.effect == SpreaderEffect.DAMAGE)
		{
			val damageable = tile.contents?.damageable()
			if (damageable != null)
			{
				grid.damage(tile, tile.contents!!, 0f, data.nameKey, data.damage)
			}
		}
		else if (data.effect == SpreaderEffect.ATTACK)
		{
			if (attackCooldown <= 0)
			{
				attackCooldown = data.attackCooldownMin + grid.ran.nextInt(data.attackCooldownMax - data.attackCooldownMin)
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
				addMonsterEffect(target, attack)
				attack.timer = data.attackNumPips + (Global.player.getStat(Statistic.HASTE) * data.attackNumPips).toInt()

				grid.replay.logAction("spreader ${data.nameKey} attacking from (${tile.toShortString()}) to (${attackedTile.toShortString()})")

				if (!Global.resolveInstantly)
				{
					val diff = attackedTile.getPosDiff(tile)
					diff[0].y *= -1

					val dst = attackedTile.euclideanDist(tile)
					val animDuration = 0.4f + attackedTile.euclideanDist(tile) * 0.025f
					val attackSprite = attack.actualSprite.copy()
					attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
					attackedTile.effects.add(attackSprite)

					if (data.attackEffect != null)
					{
						val effect = data.attackEffect!!.copy()
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
			spreader.data = SpreaderData()
			spreader.data.load(xmlData)

			spreader.spriteWrapper = spreader.data.spriteWrapper?.copy()
			spreader.particleEffect = spreader.data.particleEffect?.copy()
			spreader.fadeOut = spreader.data.fadeOut
			spreader.attackCooldown = spreader.data.attackCooldownMax

			return spreader
		}
	}
}