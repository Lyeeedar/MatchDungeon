package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.tryGet

/**
 * Created by Philip on 08-Jul-16.
 */

fun createBlock(theme: Theme, hp: Int): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.BLOCK)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(theme.blockSprites.tryGet(0).copy())

	val damageableComponent = DamageableComponent.obtain()
	damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
	damageableComponent.alwaysShowHP = false
	damageableComponent.maxhp = hp

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(damageableComponent)

	return entity
}