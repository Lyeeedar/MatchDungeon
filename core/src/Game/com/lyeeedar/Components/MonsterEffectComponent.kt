package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.MonsterEffect
import com.lyeeedar.Util.XmlData

fun Entity.monsterEffect(): MonsterEffectComponent? = MonsterEffectComponent.mapper.get(this)
class MonsterEffectComponent : AbstractComponent()
{
	lateinit var monsterEffect: MonsterEffect

	fun set(monsterEffect: MonsterEffect): MonsterEffectComponent
	{
		this.monsterEffect = monsterEffect
		return this
	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<MonsterEffectComponent> = ComponentMapper.getFor(MonsterEffectComponent::class.java)
		fun get(entity: Entity): MonsterEffectComponent? = mapper.get(entity)

		private val pool: Pool<MonsterEffectComponent> = object : Pool<MonsterEffectComponent>() {
			override fun newObject(): MonsterEffectComponent
			{
				return MonsterEffectComponent()
			}

		}

		@JvmStatic fun obtain(): MonsterEffectComponent
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