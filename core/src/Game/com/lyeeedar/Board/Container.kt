package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.tryGet

fun createContainer(theme: Theme, hp: Int, contents: Entity): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.CONTAINER)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(theme.blockSprites.tryGet(0).copy())

	val damageableComponent = DamageableComponent.obtain()
	damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
	damageableComponent.alwaysShowHP = false
	damageableComponent.maxhp = hp

	val containerComponent = ContainerComponent.obtain()
	containerComponent.containedEntity = contents

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(damageableComponent)
	entity.add(containerComponent)

	return entity
}