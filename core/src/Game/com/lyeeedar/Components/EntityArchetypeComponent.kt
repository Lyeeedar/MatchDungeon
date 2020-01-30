package com.lyeeedar.Components

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

fun Entity.archetype(): EntityArchetypeComponent? = this.components[ComponentType.EntityArchetype] as EntityArchetypeComponent?
class EntityArchetypeComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.EntityArchetype

	lateinit var archetype: EntityArchetype

	fun set(archetype: EntityArchetype): EntityArchetypeComponent
	{
		this.archetype = archetype
		return this
	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}