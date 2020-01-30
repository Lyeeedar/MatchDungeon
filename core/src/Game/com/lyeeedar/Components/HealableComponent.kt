package com.lyeeedar.Components

import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.ciel

fun Entity.healable(): HealableComponent? = this.components[ComponentType.Healable] as HealableComponent?
class HealableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Healable

	var immune = false

	var hp: Float = 1f
		set(value)
		{
			if (immune && value < field)
			{
				return
			}

			if (value < field)
			{
				val loss = field.ciel() - value.ciel()
				lostHP += loss

				var delay = 1f
				for (i in 0 until loss)
				{
					Future.call({ lostHP-- }, delay)
					delay += 0.2f
				}

				tookDamage = true
			}

			field = value
			if (field < 0f) field = 0f
		}

	var lostHP: Int = 0
	var tookDamage = false

	var maxhp: Int = 1
		set(value)
		{
			field = value
			hp = value.toFloat()
		}

	var isSummon = false

	var deathEffect: ParticleEffect = AssetManager.loadParticleEffect("Death").getParticleEffect()

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		immune = false
		hp = 1f
		lostHP = 0
		tookDamage = false
		maxhp = 1
		isSummon = false
		deathEffect = AssetManager.loadParticleEffect("Death").getParticleEffect()
	}
}