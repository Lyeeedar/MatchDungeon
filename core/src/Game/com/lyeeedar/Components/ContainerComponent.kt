package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

fun Entity.container(): ContainerComponent? = ContainerComponent.mapper.get(this)
class ContainerComponent : AbstractComponent()
{
	var containedEntity: Entity? = null

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<ContainerComponent> = ComponentMapper.getFor(ContainerComponent::class.java)
		fun get(entity: Entity): ContainerComponent? = mapper.get(entity)

		private val pool: Pool<ContainerComponent> = object : Pool<ContainerComponent>() {
			override fun newObject(): ContainerComponent
			{
				return ContainerComponent()
			}

		}

		@JvmStatic fun obtain(): ContainerComponent
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
		containedEntity = null
	}
}