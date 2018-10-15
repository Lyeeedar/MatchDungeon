package com.lyeeedar.Board

import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

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

	fun copy(): Spreader
	{
		val out = Spreader()
		out.nameKey = nameKey
		out.spriteWrapper = spriteWrapper?.copy()
		out.particleEffect = particleEffect?.copy()

		out.spreads = spreads
		out.damage = damage
		out.effect = effect

		out.attackCooldownMin = attackCooldownMin
		out.attackCooldownMax = attackCooldownMax
		out.attackEffect = attackEffect
		out.attackNumPips = attackNumPips

		return out
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
				spreader.particleEffect = AssetManager.loadParticleEffect(particleEl)
			}

			spreader.effect = SpreaderEffect.valueOf(xmlData.get("Effect", "seal")!!.toUpperCase())

			spreader.damage = xmlData.getFloat("Damage", 0f)

			spreader.spreads = xmlData.getBoolean("Spreads", true)

			spreader.attackCooldownMin = xmlData.getInt("AttackMin", 0)
			spreader.attackCooldownMax = xmlData.getInt("AttackMax", 0)
			spreader.attackNumPips = xmlData.getInt("AttackNumPips", 0)

			val attackEffectEl = xmlData.getChildByName("AttackEffect")
			if (attackEffectEl != null)
			{
				spreader.attackEffect = AssetManager.loadParticleEffect(attackEffectEl)
			}

			return spreader
		}
	}
}