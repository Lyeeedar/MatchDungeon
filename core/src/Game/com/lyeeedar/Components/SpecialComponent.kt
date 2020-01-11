package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.Special
import com.lyeeedar.Util.XmlData

fun Entity.special(): SpecialComponent? = SpecialComponent.mapper.get(this)
class SpecialComponent : AbstractComponent()
{
	lateinit var special: Special

	fun set(special: Special): SpecialComponent
	{
		this.special = special
		return this
	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<SpecialComponent> = ComponentMapper.getFor(SpecialComponent::class.java)
		fun get(entity: Entity): SpecialComponent? = mapper.get(entity)

		private val pool: Pool<SpecialComponent> = object : Pool<SpecialComponent>() {
			override fun newObject(): SpecialComponent
			{
				return SpecialComponent()
			}

		}

		@JvmStatic fun obtain(): SpecialComponent
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