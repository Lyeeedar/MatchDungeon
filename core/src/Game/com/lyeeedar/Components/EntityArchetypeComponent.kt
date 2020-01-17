package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

enum class EntityArchetype private constructor(val letter: Char)
{
	BLOCK('='),
	CHEST('£'),
	CONTAINER('c'),
	FRIENDLY('?'),
	MONSTER('!'),
	ORB('o'),
	SINKABLE('c'),
	SPECIAL('B'),
	SPREADER('@'),
	CUSTOM('&')
}

fun Entity.archetype(): EntityArchetypeComponent? = EntityArchetypeComponent.mapper.get(this)
class EntityArchetypeComponent : AbstractComponent()
{
	lateinit var archetype: EntityArchetype

	fun set(archetype: EntityArchetype): EntityArchetypeComponent
	{
		this.archetype = archetype
		return this
	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<EntityArchetypeComponent> = ComponentMapper.getFor(EntityArchetypeComponent::class.java)
		fun get(entity: Entity): EntityArchetypeComponent? = mapper.get(entity)

		private val pool: Pool<EntityArchetypeComponent> = object : Pool<EntityArchetypeComponent>() {
			override fun newObject(): EntityArchetypeComponent
			{
				return EntityArchetypeComponent()
			}

		}

		@JvmStatic fun obtain(): EntityArchetypeComponent
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