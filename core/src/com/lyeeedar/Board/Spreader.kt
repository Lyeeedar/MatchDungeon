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
		SEAL
	}

	lateinit var nameKey: String

	var spriteWrapper: SpriteWrapper? = null
	var particleEffect: ParticleEffect? = null

	lateinit var effect: SpreaderEffect

	fun copy(): Spreader
	{
		val out = Spreader()
		out.nameKey = nameKey
		out.spriteWrapper = spriteWrapper?.copy()
		out.particleEffect = particleEffect?.copy()

		out.effect = effect

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

			return spreader
		}
	}
}