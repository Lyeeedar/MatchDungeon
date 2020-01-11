package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.ciel

fun Entity.damageable(): DamageableComponent? = DamageableComponent.mapper.get(this)
class DamageableComponent : AbstractComponent()
{
	var immune = false

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

	var deathEffect: ParticleEffect = AssetManager.loadParticleEffect("death").getParticleEffect()

	var isCreature = false

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<DamageableComponent> = ComponentMapper.getFor(DamageableComponent::class.java)
		fun get(entity: Entity): DamageableComponent? = mapper.get(entity)

		private val pool: Pool<DamageableComponent> = object : Pool<DamageableComponent>() {
			override fun newObject(): DamageableComponent
			{
				return DamageableComponent()
			}

		}

		@JvmStatic fun obtain(): DamageableComponent
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{
		immune = false
		damageReduction = 0
		remainingReduction = 0
		hp = 1f
		lostHP = 0
		tookDamage = false
		maxhp = 1
		damSources.clear()
		deathEffect = AssetManager.loadParticleEffect("death").getParticleEffect()
		isCreature = false
	}
}