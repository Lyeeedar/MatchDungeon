package com.lyeeedar.Systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.lyeeedar.Board.Grid
import kotlin.reflect.KClass

val systemList: Array<KClass<out AbstractSystem>> = arrayOf(
	GridSystem::class,
	DeletionSystem::class)

fun createEngine(): Engine
{
	val engine = Engine()

	for (system in systemList)
	{
		val instance: EntitySystem = ClassReflection.newInstance(system.java)
		engine.addSystem(instance)
	}

	return engine
}

var Engine.grid: Grid?
	get() = this.gridSystem().grid
	set(value)
	{
		for (system in systemList)
		{
			this.getSystem(system.java)?.grid = value
		}
	}

fun Engine.gridSystem() = this.getSystem(GridSystem::class.java)
fun Engine.render() = this.getSystem(RenderSystem::class.java)