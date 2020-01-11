package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

fun Entity.ai(): AIComponent? = AIComponent.mapper.get(this)
class AIComponent : AbstractComponent()
{
	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<AIComponent> = ComponentMapper.getFor(AIComponent::class.java)
		fun get(entity: Entity): AIComponent? = mapper.get(entity)

		private val pool: Pool<AIComponent> = object : Pool<AIComponent>() {
			override fun newObject(): AIComponent
			{
				return AIComponent()
			}

		}

		@JvmStatic fun obtain(): AIComponent
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