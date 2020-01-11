package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.XmlData

fun Entity.onTurn(): OnTurnComponent? = OnTurnComponent.mapper.get(this)
class OnTurnComponent : AbstractComponent()
{
	var onTurn: ((grid: Grid, entity: Entity)->Unit)? = null

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<OnTurnComponent> = ComponentMapper.getFor(OnTurnComponent::class.java)
		fun get(entity: Entity): OnTurnComponent? = mapper.get(entity)

		private val pool: Pool<OnTurnComponent> = object : Pool<OnTurnComponent>() {
			override fun newObject(): OnTurnComponent
			{
				return OnTurnComponent()
			}

		}

		@JvmStatic fun obtain(): OnTurnComponent
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
		onTurn = null
	}
}