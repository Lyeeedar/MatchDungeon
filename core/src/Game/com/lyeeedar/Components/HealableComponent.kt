package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.ciel

fun Entity.healable(): HealableComponent? = HealableComponent.mapper.get(this)
class HealableComponent : AbstractComponent()
{
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

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<HealableComponent> = ComponentMapper.getFor(HealableComponent::class.java)
		fun get(entity: Entity): HealableComponent? = mapper.get(entity)

		private val pool: Pool<HealableComponent> = object : Pool<HealableComponent>() {
			override fun newObject(): HealableComponent
			{
				return HealableComponent()
			}

		}

		@JvmStatic fun obtain(): HealableComponent
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
		hp = 1f
		lostHP = 0
		tookDamage = false
		maxhp = 1
	}
}