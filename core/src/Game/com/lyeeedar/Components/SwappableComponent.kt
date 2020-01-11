package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

fun Entity.swappable(): SwappableComponent? = SwappableComponent.mapper.get(this)
class SwappableComponent : AbstractComponent()
{
	val movePoints = Array<Point>()
	var spawnCount = -1
	var cascadeCount = 0

	var canMove: Boolean = true
		get() = !sealed && field

	var sealCount = 0
		set(value)
		{
			field = value
		}
	val sealed: Boolean
		get() = sealCount > 0

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<SwappableComponent> = ComponentMapper.getFor(SwappableComponent::class.java)
		fun get(entity: Entity): SwappableComponent? = mapper.get(entity)

		private val pool: Pool<SwappableComponent> = object : Pool<SwappableComponent>() {
			override fun newObject(): SwappableComponent
			{
				return SwappableComponent()
			}

		}

		@JvmStatic fun obtain(): SwappableComponent
		{
			val obj = SwappableComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SwappableComponent.pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{
		movePoints.clear()
		spawnCount = -1
		cascadeCount = 0

		canMove = true

		sealCount = 0
	}
}