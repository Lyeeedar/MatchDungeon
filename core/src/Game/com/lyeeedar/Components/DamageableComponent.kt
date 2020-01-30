package com.lyeeedar.Components

import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.ciel

fun Entity.damageable(): DamageableComponent? = this.components[ComponentType.Damageable] as DamageableComponent?
class DamageableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Damageable

	var immune = false
	var immuneCooldown = 0

	var damageReduction: Int = 0
		set(value)
		{
			if (immune && value < field)
			{
				return
			}

			field = value
			remainingReduction = value
		}
	var remainingReduction: Int = 0

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
				//sprite.colourAnimation = BlinkAnimation.obtain().set(Colour(Color.RED), sprite.colour, 0.15f, true)
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

	val damSources = ObjectSet<Any?>()

	var deathEffect: ParticleEffect = AssetManager.loadParticleEffect("Death").getParticleEffect()

	var alwaysShowHP = true
	var isSummon = false

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		immune = false
		immuneCooldown = 0
		damageReduction = 0
		remainingReduction = 0
		hp = 1f
		lostHP = 0
		tookDamage = false
		maxhp = 1
		damSources.clear()
		deathEffect = AssetManager.loadParticleEffect("Death").getParticleEffect()
		alwaysShowHP = true
		isSummon = false
	}
}