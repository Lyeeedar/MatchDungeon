package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Renderable

fun getSinkable(renderable: Renderable): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.SINKABLE)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(renderable)

	val sinkable = SinkableComponent.obtain()

	val swappable = SwappableComponent.obtain()

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(sinkable)
	entity.add(swappable)

	return entity
}