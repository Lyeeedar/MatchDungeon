package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

fun Entity.sinkable(): SinkableComponent? = SinkableComponent.mapper.get(this)
class SinkableComponent : AbstractComponent()
{
	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<SinkableComponent> = ComponentMapper.getFor(SinkableComponent::class.java)
		fun get(entity: Entity): SinkableComponent? = mapper.get(entity)

		private val pool: Pool<SinkableComponent> = object : Pool<SinkableComponent>() {
			override fun newObject(): SinkableComponent
			{
				return SinkableComponent()
			}

		}

		@JvmStatic fun obtain(): SinkableComponent
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

	}
}