package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

fun Entity.onTurnEffect(): OnTurnEffectComponent? = OnTurnEffectComponent.mapper.get(this)
class OnTurnEffectComponent : AbstractComponent()
{
	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<OnTurnEffectComponent> = ComponentMapper.getFor(OnTurnEffectComponent::class.java)
		fun get(entity: Entity): OnTurnEffectComponent? = mapper.get(entity)

		private val pool: Pool<OnTurnEffectComponent> = object : Pool<OnTurnEffectComponent>() {
			override fun newObject(): OnTurnEffectComponent
			{
				return OnTurnEffectComponent()
			}

		}

		@JvmStatic fun obtain(): OnTurnEffectComponent
		{
			val obj = OnTurnEffectComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { OnTurnEffectComponent.pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{

	}
}